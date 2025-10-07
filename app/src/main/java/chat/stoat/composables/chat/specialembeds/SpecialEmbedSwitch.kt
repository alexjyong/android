package chat.stoat.composables.chat.specialembeds

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import chat.stoat.api.schemas.Special
import chat.stoat.api.settings.LoadedSettings

@Composable
fun SpecialEmbedSwitch(special: Special, modifier: Modifier = Modifier) {
    when {
        (special.type == "YouTube") && LoadedSettings.specialEmbedSettings.embedYouTube -> YoutubeEmbedSwitch(
            special,
            modifier
        )

        (special.type == "AppleMusic") && LoadedSettings.specialEmbedSettings.embedAppleMusic -> AppleMusicEmbed(
            special,
            modifier
        )

        else -> {}
    }
}