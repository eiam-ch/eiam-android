package ch.ubique.eiam.eiam

import android.content.Intent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ch.ubique.eiam.auth.data.IdpConfig
import ch.ubique.eiam.R
import ch.ubique.eiam.auth.AuthViewModelCallbacks
import ch.ubique.eiam.auth.LoginState
import ch.ubique.eiam.common.EiamEnvironment
import ch.ubique.eiam.common.utils.DiagnoseViewState
import ch.ubique.eiam.common.utils.ViewState
import net.openid.appauth.AuthState

@Composable
fun EiamScreen(
    loginState: ViewState<LoginState>,
    currentEnvironment: EiamEnvironment,
    currentAuthState: AuthState,
    currentConfig: IdpConfig,
    configLoadedState: State<Boolean>,
    launch: (Intent?) -> Unit,
    popupViewState: DiagnoseViewState<PopupModel, PopupLoadingModel>,
    diagnoseCallbacks: AuthViewModelCallbacks,
    logout: () -> Unit,
    switchConfig: (EiamEnvironment) -> Unit,
) {
    when (loginState) {
        is ViewState.Loading, is ViewState.Idle -> {
            Spinner()
        }

        is ViewState.Success -> {
            if (loginState.data.isLoggedIn) {
                LoggedInScreen(
                    currentEnvironment,
                    currentAuthState,
                    currentConfig,
                    popupViewState,
                    diagnoseCallbacks,
                    logout
                )
            } else {
                if (configLoadedState.value) {
                    Spinner()
                } else {
                    Column(modifier = Modifier.padding(20.dp)) {
                        SelectEnvScreen {
                            switchConfig(
                                it
                            )
                        }
                    }
                }
                LaunchedEffect(configLoadedState.value) {
                    if (configLoadedState.value) {
                        launch(loginState.data.authRequestIntent)
                    }
                }
            }
        }

        is ViewState.Error -> {
            Text("Error", color = Color.Red)
            Text("${loginState.throwable}", color = Color.Red)
            Text("${loginState.data}", color = Color.Red)
            val retry = loginState.retry
            if (retry != null) {
                Button(onClick = {
                    retry.invoke()
                }) {
                    Text(stringResource(R.string.retry_button))
                }
            }
            Button(onClick = {
                // give an option to reset
                logout()
                switchConfig(
                    currentEnvironment
                )
            }) {
                Text(stringResource(R.string.retry_button))
            }
        }
    }
}