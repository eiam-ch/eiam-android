package ch.ubique.eiam.auth.network

import ch.ubique.eiam.auth.AuthStateHandler
import ch.ubique.eiam.auth.exception.AppAuthException
import ch.ubique.eiam.auth.exception.ExceptionUtil
import ch.ubique.eiam.auth.exception.SessionExpiredException
import ch.ubique.eiam.auth.exception.UnauthorizedException
import ch.ubique.eiam.auth.extensions.copy
import ch.ubique.eiam.auth.storage.AuthStorage
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import net.openid.appauth.AuthState
import net.openid.appauth.AuthorizationService
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import retrofit2.Invocation
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Interceptor that takes care of the OAuth token handling:
 * - Performs every request with a fresh access token
 * - Adds the access token as the Authorization Header
 * - Notifies the [authStateHandler] about a session expiration when the IDP responds with an INVALID_GRANT error
 * - Force refreshes the access token and retries the request once if the server responds with a 401 or 403
 * - Ensures only one token refresh is happening at once and waits for it to finish when multiple requests are happening at the same time
 */
class AuthorizationHeaderInterceptor(
	private val authStorage: AuthStorage,
	private val authStateHandler: AuthStateHandler,
	private val authService: AuthorizationService,
	private val authorizedBaseUrls: Set<String> = emptySet(),
	private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
	private val throwOnUnauthorized: Boolean = true,
) : Interceptor {

	companion object {
		private const val HEADER_AUTHORIZATION = "Authorization"
		private const val BEARER_TOKEN_PREFIX = "Bearer"

		private const val HTTP_UNAUTHORIZED = 401
		private const val HTTP_FORBIDDEN = 403

		private const val MAX_TOKEN_REFRESH_RETRY_COUNT = 1
	}

	/**
	 * A Mutex lock that is used to ensure requests are waiting for fresh tokens if another request is already doing a token refresh
	 */
	private val tokenRefreshLock = Mutex()

	override fun intercept(chain: Interceptor.Chain): Response {
		val request = chain.request()

		// If the request url doesn't match any of the authorized base urls, proceed without sending the bearer token
		val requestUrl = request.url.toString()
		if (authorizedBaseUrls.none { requestUrl.startsWith(it) }) {
			return chain.proceed(request)
		}

		val response = runBlocking(dispatcher) {
			// Copy the current auth state to ensure we don't modify the instance that is currently in the StateFlow
			val authState = authStorage.getCurrentAuthState().copy()
			executeRequestWithFreshToken(chain, authState, request)
		}

		return response
	}

	/**
	 * Execute a request
	 */
	private suspend fun executeRequestWithFreshToken(
		chain: Interceptor.Chain,
		authState: AuthState,
		request: Request,
		retryCount: Int = 0,
	): Response = suspendCancellableCoroutine { continuation ->
		if (authState.isAuthorized) {
			val lockOwner = request.url.toString()

			// Only acquire the lock if this is the initial request, as retry requests run within the lock of the initial request
			if (retryCount == 0) {
				// Acquire the token refresh lock (or wait for it to unlock, in case another request is currently refreshing the token)
				runBlocking { tokenRefreshLock.lock(lockOwner) }

				// If the auth state needs no token refresh, directly release the token refresh lock again
				if (!authState.needsTokenRefresh) {
					tokenRefreshLock.unlock(lockOwner)
				}
			}

			// Remember the old access token to know if a token refresh actually happened
			val oldAccessToken = authState.accessToken

			// If the user is logged in, always try to perform a request with a fresh access token
			authState.performActionWithFreshTokens(authService) { freshAccessToken, _, ex ->
				// The callback might be called on the main thread, so we switch again to the IO dispatcher
				runBlocking(dispatcher) {
					if (ex == null) {
						// Check if the new access token has actually changed
						val didRefreshToken = freshAccessToken != oldAccessToken

						// Build a new request that has the fresh access token added as the Authorization Header
						val newRequest = request.newBuilder()
							.addHeader(HEADER_AUTHORIZATION, "$BEARER_TOKEN_PREFIX $freshAccessToken")
							.build()

						// Replace the auth state in the repository
						authStorage.replaceAuthState(authState)

						try {
							val response = chain.proceed(newRequest)

							if (response.isSuccessful) {
								// Access token was accepted by the server
								continuation.resume(response)
							} else {
								if (
									response.code in setOf(HTTP_UNAUTHORIZED, HTTP_FORBIDDEN)
									&& !didRefreshToken
									&& retryCount < MAX_TOKEN_REFRESH_RETRY_COUNT
								) {
									// Server returned an authentication error code, the token was not refreshed and the request can still be retried, so we should force refresh the token and try again
									authState.needsTokenRefresh = true

									// Acquire the token refresh lock before attempting to retry the request with a forced token refresh
									tokenRefreshLock.lock(lockOwner)

									try {
										val retryResponse = runBlocking {
											executeRequestWithFreshToken(chain, authState, request, retryCount = retryCount + 1)
										}
										continuation.resume(retryResponse)
									} catch (e: Exception) {
										continuation.resumeWithException(e)
									}
								} else {
									continuation.resume(response)
								}
							}
						} catch (e: Exception) {
							continuation.resumeWithException(e)
						}
					} else {
						// Check if this is an exception that should trigger the session expiration flow
						val sessionExpired = ExceptionUtil.doesExceptionIndicateSessionExpiration(ex)
						if (sessionExpired) {
							authStateHandler.sessionHasExpired()
							continuation.resumeWithException(SessionExpiredException())
						} else {
							continuation.resumeWithException(AppAuthException(ex))
						}
					}

					// Release the token refresh lock after the callback was handled (to make sure the authState in the storage was replaced)
					// Retry requests don't acquire the lock themselves, but the original request does, so don't try to unlock here (to prevent a race condition)
					if (retryCount == 0 && tokenRefreshLock.isLocked && tokenRefreshLock.holdsLock(lockOwner)) {
						tokenRefreshLock.unlock(lockOwner)
					}
				}
			}
		} else {
			// If the user is not logged in, either continue with an UnauthorizedException or normally execute the request and let the caller handle any 401/403 responses
			if (throwOnUnauthorized) {
				continuation.resumeWithException(UnauthorizedException())
			} else {
				continuation.resume(chain.proceed(request))
			}
		}
	}

}