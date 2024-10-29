package chat.revolt.api.settings.experiments

import android.util.Log
import chat.revolt.api.RevoltAPI
import chat.revolt.api.internals.ULID
import chat.revolt.api.schemas.Message
import chat.revolt.api.schemas.RsResult
import com.google.mlkit.nl.smartreply.SmartReply
import com.google.mlkit.nl.smartreply.TextMessage

object SmartReplyImpl {
    val client = SmartReply.getClient()

    fun forMessages(
        messages: List<Message>,
        onResult: (RsResult<List<String>, Exception>) -> Unit
    ) {
        if (messages.size > 10) {
            onResult(RsResult.err(IllegalArgumentException("Too many messages")))
            return
        }

        Log.d("SmartReplyImpl", "Creating conversation for ${messages.size} messages")

        val conversation = messages.map {
            if (it.author == RevoltAPI.selfId) {
                if (it.id == null) {
                    Log.w("SmartReplyImpl", "Message ID is null in local user message, skipping")
                    return@map null
                }
                if (it.content?.isEmpty() == true || it.content == null) {
                    Log.w(
                        "SmartReplyImpl",
                        "Message content is null or empty in local user message, skipping"
                    )
                    return@map null
                }
                TextMessage.createForLocalUser(it.content, ULID.asTimestamp(it.id))
            } else {
                if (it.id == null || it.author == null) {
                    Log.w(
                        "SmartReplyImpl",
                        "Message ID or author is null in remote user message, skipping"
                    )
                    return@map null
                }
                if (it.content?.isEmpty() == true || it.content == null) {
                    Log.w(
                        "SmartReplyImpl",
                        "Message content is null or empty in remote user message, skipping"
                    )
                    return@map null
                }
                TextMessage.createForRemoteUser(it.content, ULID.asTimestamp(it.id), it.author)
            }
        }.filterNotNull().reversed()

        Log.d("SmartReplyImpl", "Suggesting replies for ${conversation.size} messages")

        client.suggestReplies(conversation).addOnSuccessListener {
            onResult(RsResult.ok(it.suggestions.map { s -> s.text }))
        }.addOnFailureListener {
            onResult(RsResult.err(it))
        }
    }
}