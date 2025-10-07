package chat.stoat.api.routes.custom

import chat.stoat.api.StoatHttp
import chat.stoat.api.StoatJson
import chat.stoat.api.api
import chat.stoat.api.schemas.Emoji
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText

suspend fun fetchEmoji(id: String): Emoji {
    val response = StoatHttp.get("/custom/emoji/$id".api()).bodyAsText()
    return StoatJson.decodeFromString(
        Emoji.serializer(),
        response
    )
}
