package ch.ubique.eiam.auth

import android.content.Intent
import android.net.Uri
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.core.os.LocaleListCompat
import ch.ubique.eiam.auth.data.IdpConfig
import ch.ubique.eiam.auth.exception.ExceptionUtil
import ch.ubique.eiam.auth.exception.SessionExpiredException
import ch.ubique.eiam.auth.extensions.copy
import ch.ubique.eiam.auth.storage.AuthStorage
import ch.ubique.eiam.common.utils.SingleEventFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import net.openid.appauth.AuthState
import net.openid.appauth.AuthState.AuthStateAction
import net.openid.appauth.AuthorizationException
import net.openid.appauth.AuthorizationRequest
import net.openid.appauth.AuthorizationResponse
import net.openid.appauth.AuthorizationService
import net.openid.appauth.AuthorizationService.TokenResponseCallback
import net.openid.appauth.AuthorizationServiceConfiguration
import net.openid.appauth.ClientSecretBasic
import net.openid.appauth.EndSessionRequest
import net.openid.appauth.ResponseTypeValues
import net.openid.appauth.TokenResponse
import net.openid.appauth.connectivity.ConnectionBuilder
import net.openid.appauth.connectivity.DefaultConnectionBuilder
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

/**
 * Central authorization repository that handles the OIDC flow with the IDP and provides access to the most common information regarding the auth state
 */
class AuthRepository(
    private var idpConfig: IdpConfig,
    private val authService: AuthorizationService,
    private val customTabColorSchemeParams: CustomTabColorSchemeParams,
    private val authStorage: AuthStorage,
    private val connectionBuilder: ConnectionBuilder = DefaultConnectionBuilder.INSTANCE,
) : AuthStateProvider, AuthStateHandler {

    companion object {
        private const val OIDC_SCOPE_DEFAULT = "openid"
        private const val OIDC_SCOPE_REFRESH_TOKEN = "offline_access"
    }

    private val oidcScopeValue = listOf(
        OIDC_SCOPE_DEFAULT,
        OIDC_SCOPE_REFRESH_TOKEN,
        *idpConfig.additionalOidcScopes.toTypedArray()
    ).joinToString(" ")

    private val sessionExpiredMutable = SingleEventFlow<Boolean>()
    private val sessionExpired = sessionExpiredMutable.asFlow()

    override fun isAuthorized() = authStorage.getCurrentAuthState().isAuthorized

    override fun getAuthorizedState() =
        authStorage.getAuthState().map { it.isAuthorized }.distinctUntilChanged()

    override fun getSessionExpiration() = sessionExpired

    override fun sessionHasExpired() {
        sessionExpiredMutable.emit(true)
    }

    fun endSession(idpConfig: IdpConfig): Intent? {
        val serviceConfig = authStorage.getCurrentAuthState().authorizationServiceConfiguration
            ?: return null
        return authService.getEndSessionRequestIntent(
            EndSessionRequest.Builder(serviceConfig)
                .setIdTokenHint(authStorage.getCurrentAuthState().idToken)
                .setPostLogoutRedirectUri(Uri.parse(idpConfig.idpRedirectUrl)).build()
        )
    }

    /**
     * Invalidate the auth state and replace it in the storage, optionally preserving the previous service configuration
     */
    fun invalidateAuthState(preserveServiceConfiguration: Boolean) {
        val invalidatedAuthState = if (preserveServiceConfiguration) {
            val serviceConfig = authStorage.getCurrentAuthState().authorizationServiceConfiguration
            serviceConfig?.let { AuthState(it) } ?: AuthState()
        } else {
            AuthState()
        }
        replaceAuthState(invalidatedAuthState)
    }

    /**
     * Trigger a manual one-time token refresh if the current auth state is authorized
     */
    suspend fun refreshToken() = suspendCoroutine { continuation ->
        val currentAuthState = authStorage.getCurrentAuthState().copy()
        if (currentAuthState.isAuthorized) {
            currentAuthState.needsTokenRefresh = true

            val clientAuth = idpConfig.idpClientSecret?.let { ClientSecretBasic(it) }

            val callback = AuthStateAction { _, _, ex ->
                if (ex == null) {
                    replaceAuthState(currentAuthState)
                    continuation.resume(Unit)
                } else {
                    // Check if the AuthorizationException should trigger the session expiration flow
                    val sessionExpired = ExceptionUtil.doesExceptionIndicateSessionExpiration(ex)
                    if (sessionExpired) {
                        sessionHasExpired()
                        continuation.resumeWithException(SessionExpiredException())
                    } else {
                        continuation.resumeWithException(ex)
                    }
                }
            }

            if (clientAuth != null) {
                currentAuthState.performActionWithFreshTokens(authService, clientAuth, callback)
            } else {
                currentAuthState.performActionWithFreshTokens(authService, callback)
            }
        } else {
            continuation.resume(Unit)
        }
    }

    /**
     * Create the intent required to start the OIDC AppAuth process
     *
     * @param forceFetchConfig True to load the service configuration again, even if it was already loaded before
     * @param languageKey The app language key to be passed to the auth service
     * @param loginHint The email address to be prefilled in the OIDC login form
     */
    suspend fun createAuthRequestIntent(
        forceFetchConfig: Boolean,
        languageKey: String? = null,
        loginHint: String? = null,
    ): Intent {
        val authRequest = createAuthRequest(forceFetchConfig, languageKey, loginHint)
        val authIntent = authService.createCustomTabsIntentBuilder(authRequest.toUri())
            .setDefaultColorSchemeParams(customTabColorSchemeParams)
            .build()

        return authService.getAuthorizationRequestIntent(authRequest, authIntent)
    }

    /**
     * Update the auth state from the authorization response (which happens after the user entered their credentials)
     */
    fun updateFromAuthResponse(
        authResponse: AuthorizationResponse?,
        authException: AuthorizationException?
    ) {
        val updatedAuthState = authStorage.getCurrentAuthState().copy()
        updatedAuthState.update(authResponse, authException)
        replaceAuthState(updatedAuthState)
    }



    /**
     * Exchange the authorization code contained in the authorization response for a token
     */
    suspend fun exchangeAuthorizationCode(authResponse: AuthorizationResponse): String? =
        suspendCoroutine { continuation ->
            val clientAuth = idpConfig.idpClientSecret?.let { ClientSecretBasic(it) }

            val tokenRequest = authResponse.createTokenExchangeRequest()
            val callback = TokenResponseCallback { tokenResponse, ex ->
                updateFromTokenResponse(tokenResponse, ex)

                if (ex == null) {
                    continuation.resume(tokenResponse?.accessToken)
                } else {
                    continuation.resumeWithException(ex)
                }
            }

            if (clientAuth != null) {
                authService.performTokenRequest(tokenRequest, clientAuth, callback)
            } else {
                authService.performTokenRequest(tokenRequest, callback)
            }
        }
    /**
     * Update the auth state from the token response (which happens after the authorization code exchange)
     */
    private fun updateFromTokenResponse(
        tokenResponse: TokenResponse?,
        tokenException: AuthorizationException?
    ) {
        val updatedAuthState = authStorage.getCurrentAuthState().copy()
        updatedAuthState.update(tokenResponse, tokenException)
        replaceAuthState(updatedAuthState)
    }

    /**
     * Replaces the auth state in the storage with [newAuthState] and also resets the session expiration flag because we don't know
     * whether or not the new auth state is still expired
     */
    private fun replaceAuthState(newAuthState: AuthState) {
        authStorage.replaceAuthState(newAuthState)
        sessionExpiredMutable.emit(false)
    }

    private suspend fun createAuthRequest(
        forceFetchConfig: Boolean,
        languageKey: String? = null,
        loginHint: String? = null,
    ): AuthorizationRequest {
        val serviceConfig = if (forceFetchConfig) {
            fetchServiceConfiguration()
        } else {
            authStorage.getCurrentAuthState().authorizationServiceConfiguration
                ?: fetchServiceConfiguration()
        }
        return createAuthRequestFromConfig(serviceConfig, languageKey, loginHint)
    }

    private suspend fun fetchServiceConfiguration() = suspendCoroutine { continuation ->
        val issuerUrl = Uri.parse(idpConfig.idpDiscoveryUrl)
        AuthorizationServiceConfiguration.fetchFromIssuer(
            issuerUrl,
            { serviceConfiguration, ex ->
                when {
                    serviceConfiguration != null -> {
                        val newAuthState = AuthState(serviceConfiguration)
                        replaceAuthState(newAuthState)
                        continuation.resume(serviceConfiguration)
                    }

                    ex != null -> continuation.resumeWithException(ex)
                    else -> continuation.resumeWithException(IllegalStateException("Loading authorization service config failed without an exception"))
                }
            },
            connectionBuilder
        )
    }

    private fun createAuthRequestFromConfig(
        authServiceConfiguration: AuthorizationServiceConfiguration,
        languageKey: String? = null,
        loginHint: String? = null,
    ): AuthorizationRequest {
        var builder = AuthorizationRequest.Builder(
            authServiceConfiguration,
            idpConfig.idpClientId,
            ResponseTypeValues.CODE,
            Uri.parse(idpConfig.idpRedirectUrl)
        )
            .setScope(oidcScopeValue)
            .setPrompt(idpConfig.idpPromptParameter)
            .setUiLocales(LocaleListCompat.getDefault().toLanguageTags().replace(",", " "))

        if (languageKey != null) {
            // Pass the language key as an additional parameter
            val additionalParameters = mapOf("language" to languageKey)
            builder = builder.setAdditionalParameters(additionalParameters)
        }

        if (loginHint != null) {
            // Pass the optional login hint (e.g. prefilled email address)
            builder = builder.setLoginHint(loginHint)
        }

        return builder.build()
    }

    fun switchConfig(idpConfig: IdpConfig) {
        this.idpConfig = idpConfig
    }

}