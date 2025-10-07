package chat.stoat.sheets

import androidx.compose.runtime.Composable
import chat.stoat.api.StoatAPI
import chat.stoat.composables.emoji.EmojiPicker

@Composable
fun ReactSheet(messageId: String, onSelect: (String?) -> Unit) {
    val message = StoatAPI.messageCache[messageId]

    if (message == null) {
        onSelect(null)
        return
    }

    EmojiPicker {
        onSelect(it.removeSurrounding(":"))
    }

}