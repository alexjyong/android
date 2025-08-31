package chat.revolt.composables.markdown

import android.content.Intent
import android.widget.Toast
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material3.LocalContentColor
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontSynthesis
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.core.net.toUri
import chat.revolt.R
import chat.revolt.activities.InviteActivity
import chat.revolt.api.REVOLT_FILES
import chat.revolt.api.RevoltAPI
import chat.revolt.api.routes.custom.fetchEmoji
import chat.revolt.api.schemas.isInviteUri
import chat.revolt.callbacks.Action
import chat.revolt.callbacks.ActionChannel
import chat.revolt.composables.generic.EmojiAwareText
import chat.revolt.composables.generic.RemoteImage
import chat.revolt.composables.utils.detectTapGesturesConditionalConsume
import chat.revolt.internals.EmojiRepository
import chat.revolt.internals.resolveTimestamp
import chat.revolt.ndk.AstNode
import chat.revolt.ui.theme.FragmentMono
import kotlinx.coroutines.launch


enum class Annotations(val tag: String, val clickable: Boolean) {
    URL("URL", true),
    UserMention("UserMention", true),
    ChannelMention("ChannelMention", true),
    CustomEmote("CustomEmote", true),
    Spoiler("Spoiler", true),
    Timestamp("Timestamp", false)
}

object MarkdownTextRegularExpressions {
    val Mention = Regex("<@([0-9A-Z]{26})>")
    val Channel = Regex("<#([0-9A-Z]{26})>")
    val CustomEmote = Regex(":([0-9A-Z]{26}):")
    val UnicodeEmote = Regex(":([a-zA-Z0-9_+-]+):")
    val Spoiler = Regex("\\|\\|(.+?)\\|\\|")
    val Timestamp = Regex("<t:([0-9]+?)(:[tTDfFR])?>")
    val UrlFallback =
        Regex("<?https?://(www\\.)?[-a-zA-Z0-9@:%._+~#=]{2,256}\\.[a-z]{2,4}\\b([-a-zA-Z0-9@:%_+.~#?&/=]*)>?")
}

/**
 * Visit the AST and its children and return an [AnnotatedString] with the appropriate annotations.
 */
@Composable
fun annotateText(node: AstNode, revealedSpoilers: Set<String> = emptySet()): AnnotatedString {
    return buildAnnotatedString {
        when (node.stringType) {
            "text" -> {
                var text = node.text ?: ""
                
                text = MarkdownTextRegularExpressions.UnicodeEmote.replace(text) { matchResult ->
                    val shortcodeName = matchResult.groupValues[1]
                    EmojiRepository.unicodeByShortcode(shortcodeName) ?: matchResult.value
                }

                val mentions = MarkdownTextRegularExpressions.Mention.findAll(text)
                val channels = MarkdownTextRegularExpressions.Channel.findAll(text)
                val customEmotes = MarkdownTextRegularExpressions.CustomEmote.findAll(text)
                val spoilers = MarkdownTextRegularExpressions.Spoiler.findAll(text)
                val timestamps = MarkdownTextRegularExpressions.Timestamp.findAll(text)
                val urls = MarkdownTextRegularExpressions.UrlFallback.findAll(text)

                var lastIndex = 0
                for (mention in mentions) {
                    try {
                        append(text.substring(lastIndex, mention.range.first))
                    } catch (e: Exception) {
                        // no-op
                    }
                    pushStringAnnotation(
                        tag = Annotations.UserMention.tag,
                        annotation = mention.groupValues[1]
                    )
                    pushStyle(
                        LocalTextStyle.current.toSpanStyle()
                            .copy(
                                color = MaterialTheme.colorScheme.primary,
                                background = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                            )
                    )
                    val member = LocalMarkdownTreeConfig.current.currentServer?.let { serverId ->
                        RevoltAPI.members.getMember(serverId, mention.groupValues[1])
                    }
                    val content = member?.nickname?.let { nick -> "@$nick" }
                        ?: RevoltAPI.userCache[mention.groupValues[1]]?.username?.let { username -> "@$username" }
                        ?: "<@${mention.groupValues[1]}>"
                    append(content)
                    pop()
                    pop()
                    lastIndex = mention.range.last + 1
                }

                for (channel in channels) {
                    try {
                        append(text.substring(lastIndex, channel.range.first))
                    } catch (e: Exception) {
                        // no-op
                    }
                    pushStringAnnotation(
                        tag = Annotations.ChannelMention.tag,
                        annotation = channel.groupValues[1]
                    )
                    pushStyle(
                        LocalTextStyle.current.toSpanStyle()
                            .copy(
                                color = MaterialTheme.colorScheme.primary,
                                background = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                            )
                    )
                    val content =
                        RevoltAPI.channelCache[channel.groupValues[1]]?.name?.let { chId -> "#$chId" }
                            ?: "<#${channel.groupValues[1]}>"
                    append(content)
                    pop()
                    pop()
                    lastIndex = channel.range.last + 1
                }

                for (emote in customEmotes) {
                    try {
                        append(text.substring(lastIndex, emote.range.first))
                    } catch (e: Exception) {
                        // no-op
                    }
                    pushStringAnnotation(
                        tag = Annotations.CustomEmote.tag,
                        annotation = emote.groupValues[1]
                    )
                    appendInlineContent("CustomEmote", emote.groupValues[1])
                    pop()
                    lastIndex = emote.range.last + 1
                }

                for (spoiler in spoilers) {
                    try {
                        append(text.substring(lastIndex, spoiler.range.first))
                    } catch (e: Exception) {
                        // no-op
                    }
                    val spoilerContent = spoiler.groupValues[1]
                    val spoilerId = "spoiler_${spoiler.range.first}_${spoilerContent.hashCode()}"
                    val isRevealed = revealedSpoilers.contains(spoilerId)
                    
                    pushStringAnnotation(
                        tag = Annotations.Spoiler.tag,
                        annotation = spoilerId
                    )
                    pushStyle(
                        LocalTextStyle.current.toSpanStyle().copy(
                            background = if (isRevealed) MaterialTheme.colorScheme.surfaceContainer else MaterialTheme.colorScheme.onSurface,
                            color = if (isRevealed) LocalContentColor.current else MaterialTheme.colorScheme.onSurface
                        )
                    )
                    append(spoilerContent)
                    pop()
                    pop()
                    lastIndex = spoiler.range.last + 1
                }

                for (timestamp in timestamps) {
                    try {
                        append(text.substring(lastIndex, timestamp.range.first))
                    } catch (e: Exception) {
                        // no-op
                    }
                    pushStringAnnotation(
                        tag = Annotations.Timestamp.tag,
                        annotation = timestamp.groupValues[1]
                    )
                    pushStyle(
                        LocalTextStyle.current.toSpanStyle()
                            .copy(
                                fontFamily = FragmentMono,
                                background = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                            )
                    )
                    append(
                        resolveTimestamp(
                            try {
                                timestamp.groupValues[1].toLong()
                            } catch (e: NumberFormatException) {
                                -1
                            },
                            timestamp.groupValues.getOrNull(2)
                        )
                    )
                    pop()
                    lastIndex = timestamp.range.last + 1
                }

                // Yes, cmark should handle this, but for gTLDs like .chat it doesn't.
                // As a service with a .chat TLD, this is a problem. Duct tape fix, their fault.
                for (url in urls) {
                    try {
                        append(text.substring(lastIndex, url.range.first))
                    } catch (e: Exception) {
                        // no-op
                    }
                    pushStringAnnotation(
                        tag = Annotations.URL.tag,
                        annotation = url.value
                    )
                    pushStyle(
                        LocalTextStyle.current.toSpanStyle()
                            .copy(
                                color = MaterialTheme.colorScheme.primary
                            )
                    )
                    append(url.value)
                    pop()
                    pop()
                    lastIndex = url.range.last + 1
                }

                append(text.substring(lastIndex, text.length))
            }

            "emph" -> {
                pushStyle(
                    LocalTextStyle.current.toSpanStyle()
                        .copy(
                            fontStyle = FontStyle.Italic,
                            fontSynthesis = FontSynthesis.All
                        )
                )
                node.children?.forEach { append(annotateText(it, revealedSpoilers)) }
                pop()
            }

            "strong" -> {
                pushStyle(
                    LocalTextStyle.current.toSpanStyle()
                        .copy(
                            fontWeight = FontWeight.Bold,
                            fontSynthesis = FontSynthesis.All
                        )
                )
                node.children?.forEach { append(annotateText(it, revealedSpoilers)) }
                pop()
            }

            "del" -> {
                pushStyle(
                    LocalTextStyle.current.toSpanStyle()
                        .copy(
                            textDecoration = TextDecoration.LineThrough,
                            fontSynthesis = FontSynthesis.All
                        )
                )
                node.children?.forEach { append(annotateText(it, revealedSpoilers)) }
                pop()
            }

            "link" -> {
                pushStringAnnotation(
                    tag = Annotations.URL.tag,
                    annotation = node.url ?: ""
                )
                pushStyle(
                    LocalTextStyle.current.toSpanStyle()
                        .copy(
                            color = MaterialTheme.colorScheme.primary
                        )
                )
                node.children?.forEach { append(annotateText(it, revealedSpoilers)) }
                pop()
                pop()
            }

            "code" -> {
                pushStyle(
                    LocalTextStyle.current.toSpanStyle()
                        .copy(
                            fontFamily = FragmentMono,
                            fontSynthesis = FontSynthesis.All,
                            background = MaterialTheme.colorScheme.surfaceContainer
                        )
                )
                append(node.text ?: "")
                pop()
            }

            "spoiler" -> {
                pushStyle(
                    LocalTextStyle.current.toSpanStyle().copy(
                        background = MaterialTheme.colorScheme.onSurface,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )
                node.children?.forEach { append(annotateText(it, revealedSpoilers)) }
                pop()
            }

            "softbreak" -> {
                append("\n")
            }

            else -> {
                node.children?.forEach { append(annotateText(it, revealedSpoilers)) }
            }
        }
    }
}

@Composable
fun MarkdownText(textNode: AstNode, modifier: Modifier = Modifier) {
    var layoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
    var revealedSpoilers by remember { mutableStateOf(setOf<String>()) }
    val annotatedText = annotateText(textNode, revealedSpoilers)
    val context = LocalContext.current
    val background = MaterialTheme.colorScheme.background
    val scope = rememberCoroutineScope()
    val markdownConfig = LocalMarkdownTreeConfig.current

    val shouldConsumeTap = handler@{ offset: Int ->
        Annotations.entries.filter { it.clickable }.map { it.tag }.forEach { tag ->
            if (annotatedText.getStringAnnotations(
                    tag = tag,
                    start = offset,
                    end = offset
                ).isNotEmpty()
            ) {
                return@handler true
            }
        }

        return@handler false
    }

    val onClick = handler@{ offset: Int ->
        if (markdownConfig.linksClickable) {
            annotatedText.getStringAnnotations(
                tag = Annotations.URL.tag,
                start = offset,
                end = offset
            ).firstOrNull()?.let { annotation ->
                val url = annotation.item

                try {
                    val uri = url.toUri()
                    if (uri.isInviteUri()) {
                        scope.launch {
                            Intent(context, InviteActivity::class.java).apply {
                                data = uri
                                context.startActivity(this)
                            }
                        }
                        return@handler true
                    }
                } catch (e: Exception) {
                    // no-op
                }

                val customTab = CustomTabsIntent.Builder()
                    .setShowTitle(true)
                    .setDefaultColorSchemeParams(
                        CustomTabColorSchemeParams.Builder()
                            .setToolbarColor(background.toArgb())
                            .build()
                    )
                    .build()

                try {
                    customTab.launchUrl(context, url.toUri())
                } catch (e: Exception) {
                    Toast.makeText(
                        context,
                        context.getString(R.string.link_type_no_intent),
                        Toast.LENGTH_SHORT
                    ).show()
                }

                return@handler true
            }

            annotatedText.getStringAnnotations(
                tag = Annotations.UserMention.tag,
                start = offset,
                end = offset
            ).firstOrNull()?.let { annotation ->
                scope.launch {
                    ActionChannel.send(
                        Action.OpenUserSheet(
                            annotation.item,
                            markdownConfig.currentServer
                        )
                    )
                }

                return@handler true
            }

            annotatedText.getStringAnnotations(
                tag = Annotations.ChannelMention.tag,
                start = offset,
                end = offset
            ).firstOrNull()?.let { annotation ->
                scope.launch {
                    ActionChannel.send(Action.SwitchChannel(annotation.item))
                }

                return@handler true
            }

            annotatedText.getStringAnnotations(
                tag = Annotations.CustomEmote.tag,
                start = offset,
                end = offset
            ).firstOrNull()?.let { annotation ->
                scope.launch {
                    ActionChannel.send(Action.EmoteInfo(annotation.item))
                }

                return@handler true
            }

            annotatedText.getStringAnnotations(
                tag = Annotations.Spoiler.tag,
                start = offset,
                end = offset
            ).firstOrNull()?.let { annotation ->
                val spoilerId = annotation.item
                revealedSpoilers = if (revealedSpoilers.contains(spoilerId)) {
                    revealedSpoilers - spoilerId
                } else {
                    revealedSpoilers + spoilerId
                }
                return@handler true
            }
        }

        false
    }

    val onLongClick = handler@{ offset: Int ->
        if (markdownConfig.linksClickable) {
            annotatedText.getStringAnnotations(
                tag = Annotations.URL.tag,
                start = offset,
                end = offset
            ).firstOrNull()?.let { annotation ->
                scope.launch {
                    ActionChannel.send(Action.LinkInfo(annotation.item))
                }

                return@handler true
            }
        }

        false
    }

    EmojiAwareText(
        text = annotatedText,
        onTextLayout = { layoutResult = it },
        modifier = modifier.pointerInput(onClick, onLongClick) {
            detectTapGesturesConditionalConsume(
                onTap = { pos ->
                    val index =
                        layoutResult?.getOffsetForPosition(pos)
                            ?: return@detectTapGesturesConditionalConsume
                    onClick(index)
                },
                onLongPress = { pos ->
                    val index =
                        layoutResult?.getOffsetForPosition(pos)
                            ?: return@detectTapGesturesConditionalConsume
                    onLongClick(index)
                },
                shouldConsumeTap = { pos ->
                    val index =
                        layoutResult?.getOffsetForPosition(pos)
                            ?: return@detectTapGesturesConditionalConsume false
                    shouldConsumeTap(index)
                }
            )
        }
    )
}