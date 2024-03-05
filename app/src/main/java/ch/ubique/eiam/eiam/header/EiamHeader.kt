package ch.ubique.eiam.eiam.header

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import ch.ubique.eiam.R
import ch.ubique.eiam.common.compose.theme.TomatoRed
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EiamHeader(onInfoRequest: () -> Unit) {
    val scope = rememberCoroutineScope()
    Row(
        modifier = Modifier
            .height(50.dp)
            .fillMaxWidth(), Arrangement.SpaceBetween
    ) {
        Spacer(modifier = Modifier.weight(1.0f))
        Column(
            modifier = Modifier
                .align(Alignment.CenterVertically)
                .weight(1.0f)
        ) {
            Image(
                painterResource(R.drawable.ic_navbar_schweiz_wappen),
                contentDescription = "logo",
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
        Column(
            modifier = Modifier
                .align(Alignment.CenterVertically)
                .fillMaxHeight()
                .weight(1.0f)
        ) {
            Image(
                painterResource(id = R.drawable.ic_info), null, modifier = Modifier
                    .padding(end = 20.dp, top = 10.dp, bottom = 10.dp)
                    .fillMaxHeight()
                    .align(Alignment.End)
                    .clickable {
                        scope.launch {
                            onInfoRequest()
                        }
                    })
        }

    }
    Divider(color = TomatoRed, thickness = 3.dp)
}

@Composable
fun YoutubeScreen(
    videoId: String,
    modifier: Modifier
) {
    AndroidView(factory = {
        val view = YouTubePlayerView(it)
        view.addYouTubePlayerListener(
            object : AbstractYouTubePlayerListener() {
                override fun onReady(youTubePlayer: YouTubePlayer) {
                    super.onReady(youTubePlayer)
                    youTubePlayer.cueVideo(videoId, 0f)
                }
            }
        )
        view
    })
}

