package chat.revolt.markdown.jbm

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier

/**
 * Enhanced JBM Renderer that provides additional markdown features.
 * This renderer inherits all functionality from the original JBMRenderer while adding support
 * for spoilers and other enhancements through a simple preprocessing approach.
 */

@JBM
@Composable  
fun JBMEnhancedRenderer(content: String, modifier: Modifier = Modifier) {
    // Create a custom CompositionLocalProvider that tells JBMRenderer to use enhanced flavor
    CompositionLocalProvider(
        LocalJBMarkdownTreeState provides LocalJBMarkdownTreeState.current.copy(
            enhanced = true
        )
    ) {
        JBMRenderer(content, modifier)
    }
}