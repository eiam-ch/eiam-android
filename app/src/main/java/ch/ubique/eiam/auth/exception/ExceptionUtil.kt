package ch.ubique.eiam.auth.exception

import net.openid.appauth.AuthorizationException
import retrofit2.HttpException

internal object ExceptionUtil {

	private const val HTTP_UNAUTHORIZED = 401
	private const val HTTP_FORBIDDEN = 403

	/**
	 * Check if the session is invalid or has expired based on the exception:
	 * - It's an AppAuthException (AuthorizationException) with error code "invalid_grant"
	 * - It's an HttpException with status code 401 or 403
	 *
	 * @return True if the exception indicates a session expiration, false otherwise
	 */
	fun doesExceptionIndicateSessionExpiration(t: Throwable): Boolean {
		return when {
			t is SessionExpiredException -> true
			t is AuthorizationException && t.code == AuthorizationException.TokenRequestErrors.INVALID_GRANT.code -> true
			t is AppAuthException && t.authorizationException.code == AuthorizationException.TokenRequestErrors.INVALID_GRANT.code -> true
			t is HttpException && (t.code() == HTTP_UNAUTHORIZED || t.code() == HTTP_FORBIDDEN) -> true
			else -> false
		}
	}

}