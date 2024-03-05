package ch.ubique.eiam.auth

import android.content.Context
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.ubique.auth.storage.PreferencesAuthStorage
import ch.ubique.eiam.BuildConfig
import ch.ubique.eiam.auth.exception.UnauthorizedException
import ch.ubique.eiam.EiamApplication
import ch.ubique.eiam.R
import ch.ubique.eiam.common.EiamEnvironment
import ch.ubique.eiam.common.utils.DiagnoseViewState
import ch.ubique.eiam.common.utils.SingleEventFlow
import ch.ubique.eiam.common.utils.ViewModelFactory
import ch.ubique.eiam.common.utils.ViewState
import ch.ubique.eiam.common.utils.fetchAndUpdateStateFlow
import ch.ubique.eiam.eiam.PopupLoadingModel
import ch.ubique.eiam.eiam.PopupModel
import ch.ubique.eiam.eiam.ServicesViewModel
import ch.ubique.eiam.net.AuthorizedHttpClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.openid.appauth.AuthorizationException
import net.openid.appauth.AuthorizationResponse
import net.openid.appauth.AuthorizationService

class AuthViewModelFactory(private val context: Context) : ViewModelFactory<AuthViewModel>() {
    override fun create() = AuthViewModel(context)

}
class AuthViewModel(context: Context) : ViewModel(), AuthViewModelCallbacks {
    private val popupViewStateMutable : MutableStateFlow<DiagnoseViewState<PopupModel, PopupLoadingModel>> = MutableStateFlow(DiagnoseViewState.Idle())
    val popupViewState = popupViewStateMutable.asStateFlow()

    private val eiamAuthService = AuthorizationService(context)
    private val eiamAuthStorage = PreferencesAuthStorage(context)
    private val idpConfigViewModel = IdpConfigViewModel(eiamAuthService, eiamAuthStorage,
        CustomTabColorSchemeParams.Builder()
            .setToolbarColor(ContextCompat.getColor(context, R.color.white))
            .build())
    private val eiamAuthRepository = idpConfigViewModel.authRepository(eiamAuthStorage.getCurrentEnvState())
    private val authorizedHttpClient = AuthorizedHttpClient(
        idpConfigViewModel.authRepository(eiamAuthStorage.getCurrentEnvState()),
        eiamAuthService,
        eiamAuthStorage,
        { _ ->
            // we need a baseurl for retrofit, but the baseurl is overwritten with the URL annotation
            "https://eiam.admin.ch/invalid/"
        },
        { env ->
            when (env) {
                EiamEnvironment.REF -> BuildConfig.DEMO_REF_BACKEND
                EiamEnvironment.ABN -> BuildConfig.DEMO_ABN_BACKEND
                EiamEnvironment.PROD -> BuildConfig.DEMO_PROD_BACKEND
            }

        })

    private val servicesViewModel = ServicesViewModel(authorizedHttpClient, eiamAuthStorage)

    private val loginStateMutable = MutableStateFlow<ViewState<LoginState>>(ViewState.Loading())
    val loginState = loginStateMutable.asStateFlow()
    private var authRequestIntentJob: Job? = null
    private val applicationScope : CoroutineScope = EiamApplication.applicationScope
    private val configLoadedStateMutable = SingleEventFlow<Boolean>()
    val configLoadedState = configLoadedStateMutable.asFlow()

    val currentEnvState = eiamAuthStorage.getEnvState().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), eiamAuthStorage.getCurrentEnvState())
    val currentAuthState = eiamAuthStorage.getAuthState().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), eiamAuthStorage.getCurrentAuthState())
    val currentConfig = MutableStateFlow(idpConfigViewModel.getConfigForEnv(eiamAuthStorage.getCurrentEnvState()))

    init {
        viewModelScope.launch {
            eiamAuthRepository.getAuthorizedState().collect { isLoggedIn ->
                loginStateMutable.update { viewState ->
                    viewState.updateData { it?.copy(isLoggedIn = isLoggedIn) }
                }
            }
        }
        loadAuthRequestIntent()
    }

    fun switchConfig(env: EiamEnvironment) {
        val config = idpConfigViewModel.getConfigForEnv(env)
        currentConfig.value = config
        eiamAuthStorage.replaceEnvState(env)
        eiamAuthRepository.switchConfig(config)
        authorizedHttpClient.switchConfig()
        loadAuthRequestIntent(forceFetchConfig = true)
    }
    private fun loadAuthRequestIntent(forceFetchConfig: Boolean = false) {
        authRequestIntentJob?.cancel()
        authRequestIntentJob = viewModelScope.fetchAndUpdateStateFlow(loginStateMutable) {
            withContext(Dispatchers.IO) {
                val authRequestIntent = eiamAuthRepository.createAuthRequestIntent(forceFetchConfig)
                val isLoggedIn = eiamAuthRepository.isAuthorized()
                LoginState(authRequestIntent, isLoggedIn, forceFetchConfig).also {
                    configLoadedStateMutable.emit(forceFetchConfig)
                }
            }
        }
    }

    fun logout(endSession : Boolean = false) {
        if (!eiamAuthRepository.isAuthorized()) return

        // Run on application scope to ensure this is not cancelled when the AuthViewModel is cleared
        applicationScope.launch(Dispatchers.IO) {

            // Logout the auth repository, which in turn resets the auth state in the encrypted shared preferences
            // Must run after the deregister call, since that call requires the access token
            eiamAuthRepository.invalidateAuthState(preserveServiceConfiguration = true)
        }
    }

    fun onAuthenticationResult(authResponse: AuthorizationResponse?, authException: AuthorizationException?) {
        loginStateMutable.update { it.toLoading() }

        // Run on application scope to ensure this is not cancelled when the AuthViewModel is cleared
        // During onboarding, the "exchangeAuthorizationCode" method will exchange the code for a token and persist the auth state
        // That auth state change triggers the navigation graph to change its start destination (in the BottomNavigationHost),
        // which navigates to another screen and therefore clears this ViewModel. Since the register call (authorized only) is
        // required to happen, viewModelScope is not suitable
        applicationScope.launch {

            eiamAuthRepository.updateFromAuthResponse(authResponse, authException)
            when {
                authResponse?.authorizationCode != null -> {
                    try {
                        val token = eiamAuthRepository.exchangeAuthorizationCode(authResponse)
                        if (token != null) {
                            loginStateMutable.update { it.toSuccess(it.data ?: LoginState(null, true)) }
                        } else {
                            // Should never happen, but if the OIDC flow returned neither an exception nor a token, treat it as a failed authorization
                            throw UnauthorizedException()
                        }
                    } catch (e: Exception) {
                        eiamAuthRepository.invalidateAuthState(preserveServiceConfiguration = true)
                        loginStateMutable.update { it.toError(e) }
                    }
                }
                authException != null -> loginStateMutable.update { it.toError(authException) }
                else -> loginStateMutable.update { it.toError(IllegalStateException("Authorization flow failed")) }
            }
        }
    }

    fun resetLoadedState() {
        this.configLoadedStateMutable.emit(false)
    }
    override fun performTokenRefresh() {
        popupViewStateMutable.value =
            DiagnoseViewState.Loading(loadingState = PopupLoadingModel(isRefreshTokenLoading = true))
        val currentAuthState = eiamAuthStorage.getCurrentAuthState()
        if (currentAuthState.refreshToken == null) {
            this.showPopup(R.string.no_refresh_token,
                R.string.could_not_refresh_token)
        } else {
            viewModelScope.launch {
                try {
                    eiamAuthRepository.refreshToken()
                    popupViewStateMutable.value =
                        DiagnoseViewState.Success(PopupModel(R.string.refreshed_token_title, R.string.refreshed_token_success_text))
                } catch(ex : Exception) {
                    popupViewStateMutable.value = DiagnoseViewState.Error(ex, PopupModel(R.string.refreshed_token_title, R.string.error_dialog_text))
                }
            }
        }
    }
    override fun loadUserInfo() {
        popupViewStateMutable.value =
            DiagnoseViewState.Loading(loadingState = PopupLoadingModel(isUserInfoLoading = true))
        viewModelScope.launch {
            popupViewStateMutable.value = servicesViewModel.loadUserInfo()
        }
    }
    override fun loadApi() {
        popupViewStateMutable.value =
            DiagnoseViewState.Loading(loadingState = PopupLoadingModel(isSampleApiLoading = true))
        viewModelScope.launch {
            popupViewStateMutable.value = servicesViewModel.loadApi()
        }
    }

    override fun showPopup(dialogTitle: Int, dialogText: Int) {
        popupViewStateMutable.value = DiagnoseViewState.Success(PopupModel(dialogTitle, dialogText, showPopup = true))
    }

    override fun dismissPopup() {
        popupViewStateMutable.value = DiagnoseViewState.Idle()
    }
}

interface AuthViewModelCallbacks {
    fun loadUserInfo()
    fun loadApi()
    fun performTokenRefresh()
    fun dismissPopup()
    fun showPopup(dialogTitle : Int, dialogText: Int)
}