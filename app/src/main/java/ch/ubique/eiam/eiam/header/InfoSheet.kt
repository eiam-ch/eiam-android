package ch.ubique.eiam.eiam.header

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ch.ubique.eiam.R
import ch.ubique.eiam.eiam.Link

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EiamInfoSheet(showInfoSheet: Boolean, onDismiss: () -> Unit) {
    if(showInfoSheet) {
        ModalBottomSheet(
            containerColor = Color.White,
            onDismissRequest = {
                onDismiss()
            }) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxSize()
            ) {
                Text(
                    stringResource(R.string.information_title),
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
                    textAlign = TextAlign.Center
                )
                Text(
                    stringResource(R.string.information_text_top),
                    modifier = Modifier.padding(top = 10.dp, bottom = 10.dp)
                )
                YoutubeScreen(
                    videoId = stringResource(R.string.Information_youtubevideo_url), modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp, bottom = 10.dp)
                )
                Text(
                    stringResource(R.string.information_text_bottom),
                    modifier = Modifier.padding(top = 10.dp, bottom = 10.dp)
                )
//                Text(
//                    stringResource(R.string.info_explain_paragraph_2),
//                    modifier = Modifier.padding(top = 10.dp, bottom = 10.dp)
//                )
//                Text(
//                    stringResource(R.string.info_explain_paragraph_3),
//                    modifier = Modifier.padding(top = 10.dp, bottom = 10.dp)
//                )
                Link(stringResource(R.string.Information_url_link)) {
                    Text(stringResource(R.string.Information_url_title))
                }
            }
        }
    }
}