package chat.revolt.composables.screens.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import chat.revolt.R
import chat.revolt.api.RevoltAPI
import chat.revolt.api.internals.ULID
import chat.revolt.api.routes.channel.SendMessageReply
import chat.revolt.api.schemas.Message
import chat.revolt.composables.chat.authorAvatarUrl
import chat.revolt.composables.chat.authorColour
import chat.revolt.composables.chat.authorName
import chat.revolt.composables.generic.UserAvatar
import chat.revolt.composables.markdown.RichMarkdown
import chat.revolt.markdown.jbm.JBMarkdownTreeState
import chat.revolt.markdown.jbm.LocalJBMarkdownTreeState
import androidx.compose.runtime.CompositionLocalProvider

fun processMarkdownToPlainText(content: String): String {
    return content
        .replace(Regex("<@([0-9A-HJKMNP-TV-Z]{26})>")) { match ->
            val userId = match.groupValues[1]
            val user = RevoltAPI.userCache[userId]
            "@${user?.username ?: "unknown"}"
        }
        .replace(Regex("<#([0-9A-HJKMNP-TV-Z]{26})>")) { match ->
            val channelId = match.groupValues[1]
            val channel = RevoltAPI.channelCache[channelId]
            "#${channel?.name ?: "unknown"}"
        }
        .replace(Regex("<%([0-9A-HJKMNP-TV-Z]{26})>")) { match ->
            val roleId = match.groupValues[1]
            "@role"
        }
        .replace(Regex("\\|\\|(.+?)\\|\\|")) { match ->
            "[spoiler]"
        }
        .replace(Regex("\\*\\*(.+?)\\*\\*")) { match ->
            match.groupValues[1]
        }
        .replace(Regex("__(.+?)__")) { match ->
            match.groupValues[1]
        }
        .replace(Regex("(?<!\\*)\\*([^*]+?)\\*(?!\\*)")) { match ->
            match.groupValues[1]
        }
        .replace(Regex("(?<!_)_([^_]+?)_(?!_)")) { match ->
            match.groupValues[1]
        }
        .replace(Regex("~~(.+?)~~")) { match ->
            match.groupValues[1]
        }
        .replace(Regex("`([^`]+?)`")) { match ->
            match.groupValues[1]
        }
        .replace(Regex("```[\\s\\S]*?```")) { match ->
            "[code block]"
        }
        .replace(Regex("\\s+"), " ")
        .trim()
}

@Composable
fun replyContentText(message: Message): String {
    return if (message.content.isNullOrBlank()) {
        stringResource(id = R.string.reply_message_empty_has_attachments)
    } else {
        message.content
            .replace(Regex("<@([0-9A-HJKMNP-TV-Z]{26})>")) { match ->
                val userId = match.groupValues[1]
                val user = RevoltAPI.userCache[userId]
                "@${user?.username ?: "unknown"}"
            }
            .replace(Regex("<#([0-9A-HJKMNP-TV-Z]{26})>")) { match ->
                val channelId = match.groupValues[1]
                val channel = RevoltAPI.channelCache[channelId]
                "#${channel?.name ?: "unknown"}"
            }
            .replace(Regex("<%([0-9A-HJKMNP-TV-Z]{26})>")) { match ->
                "@role"
            }
            .replace(Regex("\\|\\|(.+?)\\|\\|")) { match ->
                "[spoiler]"
            }
    }
}

@Composable
fun ManageableReply(reply: SendMessageReply, onToggleMention: () -> Unit, onRemove: () -> Unit) {
    val replyMessage = RevoltAPI.messageCache[reply.id] ?: return onRemove()
    val replyAuthor = RevoltAPI.userCache[replyMessage.author] ?: return onRemove()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(R.drawable.icn_close_24dp),
            contentDescription = stringResource(id = R.string.remove_reply_alt),
            modifier = Modifier
                .clip(MaterialTheme.shapes.small)
                .clickable {
                    onRemove()
                }
                .padding(4.dp)
                .size(16.dp)
        )

        Spacer(modifier = Modifier.width(8.dp))


        UserAvatar(
            username = authorName(message = replyMessage),
            userId = replyAuthor.id ?: ULID.makeSpecial(0),
            avatar = replyAuthor.avatar,
            rawUrl = authorAvatarUrl(message = replyMessage),
            size = 16.dp
        )

        Spacer(modifier = Modifier.width(4.dp))

        Text(
            text = authorName(message = replyMessage),
            modifier = Modifier
                .padding(4.dp),
            style = LocalTextStyle.current.copy(
                brush = authorColour(message = replyMessage),
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )
        )

        if (replyMessage.content.isNullOrBlank()) {
            Text(
                text = stringResource(id = R.string.reply_message_empty_has_attachments),
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .padding(4.dp)
                    .weight(1f)
            )
        } else {
            CompositionLocalProvider(
                LocalJBMarkdownTreeState provides JBMarkdownTreeState(
                    singleLine = true,
                    linksClickable = false,
                    embedded = true
                )
            ) {
                RichMarkdown(
                    input = replyMessage.content,
                    modifier = Modifier
                        .padding(4.dp)
                        .weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.width(4.dp))

        Text(
            text = if (reply.mention) {
                stringResource(id = R.string.reply_mention_on)
            } else {
                stringResource(id = R.string.reply_mention_off)
            },
            modifier = Modifier
                .clip(MaterialTheme.shapes.small)
                .clickable {
                    onToggleMention()
                }
                .padding(4.dp),
            color = if (reply.mention) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
            },
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun ReplyManager(
    replies: List<SendMessageReply>,
    onToggleMention: (SendMessageReply) -> Unit,
    onRemove: (SendMessageReply) -> Unit
) {
    Column {
        replies.forEach { reply ->
            ManageableReply(
                reply = reply,
                onToggleMention = { onToggleMention(reply) },
                onRemove = { onRemove(reply) }
            )
        }
    }
}
