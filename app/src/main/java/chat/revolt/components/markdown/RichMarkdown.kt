package chat.revolt.components.markdown

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import chat.revolt.api.settings.Experiments
import chat.revolt.markdown.jbm.JBM
import chat.revolt.markdown.jbm.JBMRenderer
import chat.revolt.ndk.Stendal

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