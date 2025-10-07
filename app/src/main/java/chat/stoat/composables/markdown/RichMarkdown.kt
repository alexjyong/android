package chat.stoat.composables.markdown

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import chat.stoat.api.settings.Experiments
import chat.stoat.markdown.jbm.JBM
import chat.stoat.markdown.jbm.JBMRenderer
import chat.stoat.ndk.Stendal

@OptIn(JBM::class)
@Composable
fun RichMarkdown(input: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        if (Experiments.useKotlinBasedMarkdownRenderer.isEnabled) {
            JBMRenderer(input)
        } else {
            MarkdownTree(node = Stendal.render(input))
        }
    }
}