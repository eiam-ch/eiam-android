package ch.ubique.eiam.eiam

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ch.ubique.eiam.R
import ch.ubique.eiam.common.EiamEnvironment
import ch.ubique.eiam.common.compose.theme.FadingNight

@Composable
fun Logo() {
    Column(modifier = Modifier
        .fillMaxWidth(2.0f / 3.0f)
        .padding(bottom = 50.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.SpaceBetween) {
        Image(painterResource(id = R.drawable.ic_logo), contentDescription = null, modifier = Modifier.height(90.dp))
        Text(stringResource(R.string.Information_url_title), modifier = Modifier.align(Alignment.CenterHorizontally))
        Text(stringResource(R.string.home_subtitle), fontWeight = FontWeight.Bold, textAlign = TextAlign.Center , modifier = Modifier.align(Alignment.CenterHorizontally))
    }
}

@Composable
private fun EnvironmentButton(env: EiamEnvironment, environmentSelected: (EiamEnvironment) -> Unit) {
    Button(
        shape = RoundedCornerShape(15.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color.White),
        onClick = { environmentSelected(env) },
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
            .padding(bottom = 10.dp)
    ) {
        Text(env.name, color = FadingNight, fontSize = 20.sp)
    }
}

@Composable
fun SelectEnvScreen(environmentSelected: (EiamEnvironment) -> Unit) {
    Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement =  Arrangement.SpaceBetween) {
        Logo()
        Text(stringResource(R.string.information_text_top), textAlign = TextAlign.Center)
        Column(modifier = Modifier
            .weight(1.0f)
            .padding(top = 30.dp)) {
            EnvironmentButton(EiamEnvironment.REF, environmentSelected)
            EnvironmentButton(EiamEnvironment.ABN, environmentSelected)
            EnvironmentButton(EiamEnvironment.PROD, environmentSelected)
        }
    }
}