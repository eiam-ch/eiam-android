package ch.ubique.eiam.eiam

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import ch.ubique.eiam.auth.AuthViewModel
import ch.ubique.eiam.auth.AuthViewModelFactory
import ch.ubique.eiam.auth.LoginState
import ch.ubique.eiam.common.compose.theme.PaperWhite
import ch.ubique.eiam.common.compose.theme.TemplateAndroidTheme
import ch.ubique.eiam.common.utils.ViewState
import ch.ubique.eiam.databinding.FragmentComposeBinding
import ch.ubique.eiam.eiam.header.EiamHeader
import ch.ubique.eiam.eiam.header.EiamInfoSheet
import kotlinx.coroutines.flow.MutableStateFlow
import net.openid.appauth.AuthorizationException
import net.openid.appauth.AuthorizationResponse

class EiamFragment : Fragment() {

    companion object {
        fun newInstance() = EiamFragment()
    }

    private var _binding: FragmentComposeBinding? = null
    private val binding get() = _binding!!

    private val authViewModel by viewModels<AuthViewModel> { AuthViewModelFactory(requireContext()) }

    private val isLogout = MutableStateFlow(false)
    private val launcher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        authViewModel.resetLoadedState()
        if (result.resultCode != Activity.RESULT_CANCELED) {
            result.data?.let {
                val authResponse = AuthorizationResponse.fromIntent(it)
                val authException = AuthorizationException.fromIntent(it)
                if (authException == null && authResponse == null && isLogout.value) {
                    isLogout.value = false
                } else {
                    authViewModel.onAuthenticationResult(
                        authResponse,
                        authException
                    )
                }
            }
        }
    }
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentComposeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.composeView.setContent {
            TemplateAndroidTheme {
                val showInfoSheet = remember { mutableStateOf(false) }
                val loginState: ViewState<LoginState> =
                    authViewModel.loginState.collectAsState().value
//                val currentEnvironment = remember {
//                    mutableStateOf(authViewModel.currentEnvState.collectAsState(initial = ))
//                }
                val configLoadedState =
                    authViewModel.configLoadedState.collectAsState(
                        initial = false
                    )
//                val currentAuthState = authViewModel.eiamAuthStorage.getAuthState().collectAsState(initial = AuthState())

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(PaperWhite)
                ) {
                    EiamHeader {
                        showInfoSheet.value = true
                    }
                    EiamScreen(
                        loginState,
                        authViewModel.currentEnvState.collectAsState().value,
                        authViewModel.currentAuthState.collectAsState().value,
                        authViewModel.currentConfig.collectAsState().value,
                        configLoadedState,
                        { intent -> launcher.launch(intent) },
                        authViewModel.popupViewState.collectAsState().value,
                        authViewModel,
                        {
                            authViewModel.logout()
                            // use if we want to call the end session endpoint and logout of the browser
//                             val endSession = authViewModel.eiamAuthRepository.endSession(
//                             idpConfigViewModel.getConfigForEnv(currentEnvironment.value)
//                            )
//                             isLogout.value = true
//                             launcher.launch(endSession)
                        },
                        { env ->
                            authViewModel.switchConfig(env)
                        }
                    )
                }
                EiamInfoSheet(showInfoSheet.value) {
                    showInfoSheet.value = false
                }
            }
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}