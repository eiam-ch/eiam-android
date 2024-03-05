package ch.ubique.eiam.eiam

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ch.ubique.eiam.auth.data.IdpConfig
import ch.ubique.eiam.R
import ch.ubique.eiam.auth.AuthViewModelCallbacks
import ch.ubique.eiam.common.compose.theme.FadingNight
import ch.ubique.eiam.common.utils.DiagnoseViewState
import ch.ubique.eiam.eiam.dialog.EiamAlertDialog
import net.openid.appauth.AuthState

@Composable
fun ThinDivider() {
    Row(modifier = Modifier
        .fillMaxWidth()
        .background(Color.White)
        ) {
        Divider(thickness = Dp.Hairline, modifier = Modifier.padding(start = 20.dp, end = 20.dp))
    }
}
@Composable
fun DiagnoseSheet(
    email: String?,
    authState: AuthState,
    idpConfig: IdpConfig,
    popupViewState: DiagnoseViewState<PopupModel, PopupLoadingModel>,
    diagnoseCallbacks: AuthViewModelCallbacks
) {
    when(popupViewState) {
        is DiagnoseViewState.Success -> {
            EiamAlertDialog(
                onDismissRequest = { diagnoseCallbacks.dismissPopup() },
                onConfirmation = { diagnoseCallbacks.dismissPopup() },
                popupViewState.result,
            )
        }
        is DiagnoseViewState.Error -> {
            EiamAlertDialog(
                onDismissRequest = { diagnoseCallbacks.dismissPopup() },
                onConfirmation = { diagnoseCallbacks.dismissPopup() },
                popupViewState.result,
                popupViewState.throwable
            )
        }
        else -> {}
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Text(stringResource(R.string.diagnose_sheet_title), modifier = Modifier.align(Alignment.CenterHorizontally))
        Text(stringResource(R.string.diagnose_sheet_actions_title),modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 10.dp))
        Column(modifier = Modifier.padding(bottom = 20.dp)) {
            Box(
                modifier = Modifier
                    .background(Color.White)
                    .fillMaxWidth()
                    .padding(start = 10.dp, end = 10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = {
                        diagnoseCallbacks.loadUserInfo()
                    }) { Text("UserInfo Request", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = FadingNight) }

                    if(popupViewState is DiagnoseViewState.Loading && popupViewState.loadingState.isUserInfoLoading) {
                        CircularProgressIndicator()
                    }
                }
            }
            ThinDivider()
            Box(
                modifier = Modifier
                    .background(Color.White)
                    .fillMaxWidth()
                    .padding(start = 10.dp, end = 10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically)  {
                    TextButton(onClick = {
                        diagnoseCallbacks.performTokenRefresh()
                    }) {
                        Text("Refresh Token", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = FadingNight)
                    }
                    if (popupViewState is DiagnoseViewState.Loading && popupViewState.loadingState.isRefreshTokenLoading) {
                        CircularProgressIndicator()
                    }
                }
            }
            ThinDivider()
            Box(
                modifier = Modifier
                    .background(Color.White)
                    .fillMaxWidth()
                    .padding(start = 10.dp, end = 10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically)  {
                    TextButton(onClick = {
                        diagnoseCallbacks.loadApi()
                    }) { Text("Test Request", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = FadingNight) }
                    if(popupViewState is DiagnoseViewState.Loading && popupViewState.loadingState.isSampleApiLoading) {
                        CircularProgressIndicator()
                    }
                }

            }
        }
        Text("Information", modifier = Modifier.align(Alignment.CenterHorizontally))
        Box(
            modifier = Modifier
                .background(Color.White)
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text("User: $email")
        }
        Text("TOKENS", modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 10.dp))
        Column(modifier = Modifier.padding(bottom = 20.dp)) {
            Box(
                modifier = Modifier
                    .background(Color.White)
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Column {
                    Text("Access-Token")
                    Text(
                        authState.accessToken ?: "N/A",
                        maxLines = 5,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            ThinDivider()
            Box(
                modifier = Modifier
                    .background(Color.White)
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Column {
                    Text("Refresh-Token")
                    Text(
                        authState.refreshToken ?: "N/A",
                        maxLines = 5,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            ThinDivider()
            Box(
                modifier = Modifier
                    .background(Color.White)
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Column {
                    Text("ID-Token")
                    Text(authState.idToken ?: "N/A", maxLines = 5, overflow = TextOverflow.Ellipsis)
                }
            }
        }
        Text("CONFIGURATION", modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 10.dp))
        Column(modifier = Modifier.padding(bottom = 20.dp)) {
            Box(
                modifier = Modifier
                    .background(Color.White)
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Column {
                    Text("Client ID")
                    Text(idpConfig.idpClientId)
                }
            }
            ThinDivider()
            Box(
                modifier = Modifier
                    .background(Color.White)
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Column {
                    Text("Client Secret")
                    Text(idpConfig.idpClientSecret ?: "N/A")
                }
            }
            ThinDivider()
            Box(
                modifier = Modifier
                    .background(Color.White)
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Column {
                    Text("Discovery")
                    Text(idpConfig.idpDiscoveryUrl)
                }
            }
            ThinDivider()
            Box(
                modifier = Modifier
                    .background(Color.White)
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Column {
                    Text("Redirect URI")
                    Text(idpConfig.idpRedirectUrl)
                }
            }
        }
    }
}