package ch.ubique.eiam.auth

import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.lifecycle.ViewModel
import ch.ubique.eiam.auth.data.IdpConfig
import ch.ubique.eiam.auth.storage.AuthStorage
import ch.ubique.eiam.BuildConfig
import ch.ubique.eiam.common.EiamEnvironment
import ch.ubique.eiam.common.utils.ViewModelFactory
import net.openid.appauth.AuthorizationService

class IdpConfigViewModel(private val authService : AuthorizationService, private val authStorage: AuthStorage, private val params: CustomTabColorSchemeParams) {
    private val idpRefConfig = IdpConfig(
        BuildConfig.IDP_REF_DISCOVERY_URL,
        BuildConfig.IDP_REF_REDIRECT_URL,
        BuildConfig.IDP_REF_CLIENT_ID,
        idpPromptParameter = "login",
        additionalOidcScopes = listOf("offline_access")
    )
    private val idpAbnConfig = IdpConfig(
        BuildConfig.IDP_ABN_DISCOVERY_URL,
        BuildConfig.IDP_ABN_REDIRECT_URL,
        BuildConfig.IDP_ABN_CLIENT_ID,
        idpPromptParameter = "login",
        additionalOidcScopes = listOf("offline_access")
    )
    private val idpProdConfig = IdpConfig(
        BuildConfig.IDP_PROD_DISCOVERY_URL,
        BuildConfig.IDP_PROD_REDIRECT_URL,
        BuildConfig.IDP_PROD_CLIENT_ID,
        idpPromptParameter = "login",
        additionalOidcScopes = listOf("offline_access")
    )

    fun getConfigForEnv(environment: EiamEnvironment) : IdpConfig {
        return when(environment) {
            EiamEnvironment.REF -> idpRefConfig
            EiamEnvironment.ABN -> idpAbnConfig
            EiamEnvironment.PROD -> idpProdConfig
        }
    }

    fun authRepository(environment: EiamEnvironment) : AuthRepository {
        return AuthRepository(getConfigForEnv(environment),authService,params,authStorage)
    }
}