package chat.revolt.composables.generic

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import chat.revolt.api.REVOLT_FILES

/**
 * Text composable that automatically detects and renders custom emojis in the format :ULID:
 * This bypasses all parser-level emoji handling and works at the Compose rendering level.
 */
@Composable
fun EmojiAwareText(
    text: AnnotatedString,
    modifier: Modifier = Modifier,
    maxLines: Int = Int.MAX_VALUE,
    onTextLayout: ((androidx.compose.ui.text.TextLayoutResult) -> Unit)? = null
) {
    val fontSize = LocalTextStyle.current.fontSize
    val emojiSize = if (fontSize.isUnspecified) 16.sp else fontSize
    
    // Find all custom emoji patterns :ULID:
    val customEmojiRegex = Regex(":([0-9A-HJKMNP-TV-Z]{26}):")
    val matches = customEmojiRegex.findAll(text.text).toList()
    
    if (matches.isEmpty()) {
        // No custom emojis, render normally
        Text(
            text = text,
            modifier = modifier,
            maxLines = maxLines,
            onTextLayout = onTextLayout
        )
        return
    }
    
    // Build new annotated string with inline content placeholders
    val processedText = buildAnnotatedString {
        // Copy all existing spans from original text
        append(text)
        
        // Replace emoji patterns with inline content placeholders
        var offset = 0
        matches.forEach { match ->
            val startIndex = match.range.first - offset
            val endIndex = match.range.last + 1 - offset
            val emojiId = match.groupValues[1]
            val placeholderKey = "emoji_$emojiId"
            
            // Remove the :ULID: text and add placeholder
            val beforeEmoji = subSequence(0, startIndex)
            val afterEmoji = subSequence(endIndex, length)
            
            clear()
            append(beforeEmoji)
            appendInlineContent(placeholderKey, "[emoji]")
            append(afterEmoji)
            
            offset += match.value.length - "[emoji]".length
        }
    }
    
    // Create inline content map for each emoji
    val inlineContent = matches.associate { match ->
        val emojiId = match.groupValues[1]
        val placeholderKey = "emoji_$emojiId"
        
        placeholderKey to InlineTextContent(
            placeholder = Placeholder(
                width = emojiSize,
                height = emojiSize,
                placeholderVerticalAlign = PlaceholderVerticalAlign.Center
            )
        ) {
            RemoteImage(
                url = "$REVOLT_FILES/emojis/$emojiId",
                description = ":$emojiId:",
                modifier = Modifier
                    .size(emojiSize.value.dp)
                    .clip(CircleShape)
            )
        }
    }
    
    Text(
        text = processedText,
        inlineContent = inlineContent,
        modifier = modifier,
        maxLines = maxLines,
        onTextLayout = onTextLayout
    )
}

/**
 * Overload for plain string input
 */
@Composable
fun EmojiAwareText(
    text: String,
    modifier: Modifier = Modifier,
    maxLines: Int = Int.MAX_VALUE,
    onTextLayout: ((androidx.compose.ui.text.TextLayoutResult) -> Unit)? = null
) {
    EmojiAwareText(
        text = AnnotatedString(text),
        modifier = modifier,
        maxLines = maxLines,
        onTextLayout = onTextLayout
    )
}