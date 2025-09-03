package chat.revolt.composables.markdown

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import chat.revolt.api.settings.Experiments
import chat.revolt.markdown.jbm.JBM
import chat.revolt.markdown.jbm.JBMRenderer
import chat.revolt.markdown.jbm.JBMEnhancedRenderer
import chat.revolt.ndk.Stendal

@OptIn(JBM::class)
@Composable
fun RichMarkdown(input: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        when {
            Experiments.useFinalMarkdownRenderer.isEnabled -> {
                JBMRenderer(input)
            }
            Experiments.useEnhancedMarkdownRenderer.isEnabled -> {
                JBMEnhancedRenderer(input)
            }
            Experiments.useKotlinBasedMarkdownRenderer.isEnabled -> {
                JBMRenderer(input)
            }
            else -> {
                JBMEnhancedRenderer(input)
            }
        }
    }
}