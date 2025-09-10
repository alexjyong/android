package chat.revolt.composables.generic

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.isUnspecified
import chat.revolt.api.REVOLT_FILES
import chat.revolt.composables.generic.RemoteImage

/**
 * Text composable that automatically detects and renders custom emojis in the format :ULID:
 * This bypasses all parser-level emoji handling and works at the Compose rendering level.
 */
@Composable
fun EmojiAwareText(
    text: AnnotatedString,
    modifier: Modifier = Modifier,
    maxLines: Int = Int.MAX_VALUE,
    inlineContent: Map<String, InlineTextContent> = emptyMap(),
    onTextLayout: ((androidx.compose.ui.text.TextLayoutResult) -> Unit)? = null
) {
    val fontSize = LocalTextStyle.current.fontSize
    val emojiSize = if (fontSize.isUnspecified) 24.sp else (fontSize * 1.5f)
    
    val customEmojiRegex = Regex(":([0-9A-HJKMNP-TV-Z]{26}):")
    val matches = customEmojiRegex.findAll(text.text).toList()
    
    if (matches.isEmpty()) {
        Text(
            text = text,
            modifier = modifier,
            maxLines = maxLines,
            inlineContent = inlineContent,
            style = LocalTextStyle.current.copy(
                lineHeight = if (LocalTextStyle.current.lineHeight.isUnspecified) {
                    (LocalTextStyle.current.fontSize * 1.5f)
                } else {
                    val currentLineHeight = LocalTextStyle.current.lineHeight
                    val minLineHeight = LocalTextStyle.current.fontSize * 1.5f
                    if (currentLineHeight.value < minLineHeight.value) minLineHeight else currentLineHeight
                }
            ),
            onTextLayout = onTextLayout ?: {}
        )
        return
    }
    
    val processedText = buildAnnotatedString {
        val sourceText = text.text
        var lastIndex = 0
        
        matches.forEach { match ->
            val emojiId = match.groupValues[1]
            val placeholderKey = "emoji_$emojiId"
            
            if (match.range.first > lastIndex) {
                val beforeEmoji = sourceText.substring(lastIndex, match.range.first)
                append(beforeEmoji)
                
                text.spanStyles.forEach { spanStyle ->
                    if (spanStyle.start < match.range.first && spanStyle.end > lastIndex) {
                        val spanStart = maxOf(spanStyle.start - lastIndex, 0)
                        val spanEnd = minOf(spanStyle.end - lastIndex, beforeEmoji.length)
                        if (spanStart < spanEnd) {
                            addStyle(spanStyle.item, length - beforeEmoji.length + spanStart, length - beforeEmoji.length + spanEnd)
                        }
                    }
                }
            }
            
            appendInlineContent(placeholderKey, "[emoji]")
            
            lastIndex = match.range.last + 1
        }
        
        if (lastIndex < sourceText.length) {
            val remainingText = sourceText.substring(lastIndex)
            append(remainingText)
            
            text.spanStyles.forEach { spanStyle ->
                if (spanStyle.start < sourceText.length && spanStyle.end > lastIndex) {
                    val spanStart = maxOf(spanStyle.start - lastIndex, 0)
                    val spanEnd = minOf(spanStyle.end - lastIndex, remainingText.length)
                    if (spanStart < spanEnd) {
                        addStyle(spanStyle.item, length - remainingText.length + spanStart, length - remainingText.length + spanEnd)
                    }
                }
            }
        }
    }
    
    val emojiInlineContent = matches.associate { match ->
        val emojiId = match.groupValues[1]
        val placeholderKey = "emoji_$emojiId"
        
        placeholderKey to InlineTextContent(
            placeholder = Placeholder(
                width = emojiSize,
                height = emojiSize * 1.5f,
                placeholderVerticalAlign = PlaceholderVerticalAlign.Center
            )
        ) {
            RemoteImage(
                url = "$REVOLT_FILES/emojis/$emojiId",
                description = ":$emojiId:",
                modifier = Modifier
                    .size(width = emojiSize.value.dp, height = (emojiSize.value * 1.5f).dp)
            )
        }
    }
    
    val mergedInlineContent = inlineContent + emojiInlineContent
    
    Text(
        text = processedText,
        inlineContent = mergedInlineContent,
        modifier = modifier,
        maxLines = maxLines,
        style = LocalTextStyle.current.copy(
            lineHeight = if (LocalTextStyle.current.lineHeight.isUnspecified) {
                (LocalTextStyle.current.fontSize * 1.5f)
            } else {
                val currentLineHeight = LocalTextStyle.current.lineHeight
                val minLineHeight = LocalTextStyle.current.fontSize * 1.5f
                if (currentLineHeight.value < minLineHeight.value) minLineHeight else currentLineHeight
            }
        ),
        onTextLayout = onTextLayout ?: {}
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
    inlineContent: Map<String, InlineTextContent> = emptyMap(),
    onTextLayout: ((androidx.compose.ui.text.TextLayoutResult) -> Unit)? = null
) {
    EmojiAwareText(
        text = AnnotatedString(text),
        modifier = modifier,
        maxLines = maxLines,
        inlineContent = inlineContent,
        onTextLayout = onTextLayout
    )
}