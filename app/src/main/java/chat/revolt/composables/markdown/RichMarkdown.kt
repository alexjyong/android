package chat.revolt.composables.markdown

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
        when {
            // Future: FinalMarkdown renderer (when available)
            Experiments.useFinalMarkdownRenderer.isEnabled -> {
                // TODO: Implement FinalMarkdown rendering when ready
                // For now, fallback to JBM
                JBMRenderer(input)
            }
            // Default: Always use JBM renderer (Stendal/C++ path removed)
            else -> {
                JBMRenderer(input)
            }
        }
        
        // Stendal C++ path completely removed - unreachable:
        // MarkdownTree(node = Stendal.render(input))
    }
}