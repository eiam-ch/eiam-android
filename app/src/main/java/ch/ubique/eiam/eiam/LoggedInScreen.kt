package ch.ubique.eiam.eiam

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ch.ubique.eiam.auth.data.IdpConfig
import ch.ubique.eiam.R
import ch.ubique.eiam.auth.AuthViewModelCallbacks
import ch.ubique.eiam.auth.ProfileData
import ch.ubique.eiam.auth.getProfileData
import ch.ubique.eiam.common.EiamEnvironment
import ch.ubique.eiam.common.compose.theme.FadingNight
import ch.ubique.eiam.common.compose.theme.PaperWhite
import ch.ubique.eiam.common.utils.DiagnoseViewState
import kotlinx.coroutines.launch
import net.openid.appauth.AuthState

@Composable
fun ContentBox(
    bubbleContent: @Composable () -> Unit,
    mainContent: @Composable () -> Unit,
    buttonContent: String,
    modifier: Modifier = Modifier,
    callback: () -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(20.dp)

    ) {
        Box {
            Surface(
                color = Color.White,
                shadowElevation = 2.dp,
                shape = RoundedCornerShape(15.dp),
                modifier = Modifier.padding(top = 30.dp, bottom = 25.dp)
            ) {
                Box(
                    modifier = Modifier
                        .padding(top = 40.dp, bottom = 40.dp, end = 20.dp, start = 20.dp)
                ) {
                    mainContent()
                }
            }
            Surface(
                color = Color.White,
                shadowElevation = 16.dp,
                shape = CircleShape,
                modifier = Modifier
                    .width(60.dp)
                    .height(60.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(10.dp)
                ) {
                    Column(modifier = Modifier.align(Alignment.Center)) {
                        bubbleContent()
                    }
                }
            }
            Button(
                colors = ButtonDefaults.buttonColors(containerColor = FadingNight),
                onClick = {
                    callback()
                }, modifier = Modifier
                    .height(50.dp)
                    .align(Alignment.BottomCenter)
            ) {
                Text(buttonContent, color = Color.White)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoggedInScreen(
    environment: EiamEnvironment,
    authState: AuthState,
    idpConfig: IdpConfig,
    popupViewState: DiagnoseViewState<PopupModel, PopupLoadingModel>,
    diagnoseCallbacks: AuthViewModelCallbacks,
    logout: () -> Unit
) {
    val profile: ProfileData? = authState.getProfileData()
    val viewSheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()
    var showDiagnoseSheet by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PaperWhite)
            .padding(bottom = 10.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(painterResource(id = R.drawable.ic_check), null, modifier = Modifier.padding(top = 30.dp))
        Text(stringResource(R.string.logged_in_scree_success), modifier = Modifier.padding(bottom = 30.dp))
        ContentBox(
            modifier = Modifier.padding(bottom = 30.dp),
            bubbleContent = {
                Text(environment.name)
            },
            mainContent = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "${profile?.firstName} ${profile?.lastName}",
                        fontWeight = FontWeight.Bold
                    )
                    Text("${profile?.email}")
                }
            },
            buttonContent = stringResource(R.string.logout_button)
        ) {
            logout()
        }
        ContentBox(
            bubbleContent = {
                Image(painterResource(id = R.drawable.ic_diagnose), null)
            },
            mainContent = {
                Text(stringResource(R.string.logged_in_diagnose_text))
            },
            buttonContent = stringResource(R.string.logged_in_diagnose_button)
        ) {
            showDiagnoseSheet = true
            scope.launch {
                viewSheetState.show()
            }
        }
    }
    if (showDiagnoseSheet) {
        ModalBottomSheet(
            containerColor = PaperWhite,
            onDismissRequest = { showDiagnoseSheet = false }) {
            DiagnoseSheet(
                profile?.email,
                authState,
                idpConfig,
                popupViewState,
                diagnoseCallbacks
            )
        }
    }
}