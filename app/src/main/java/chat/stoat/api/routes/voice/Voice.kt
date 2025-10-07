package chat.stoat.api.routes.voice

import chat.stoat.api.StoatAPIError
import chat.stoat.api.StoatHttp
import chat.stoat.api.StoatJson
import chat.stoat.api.api
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException

@Serializable
data class JoinCallResponse(
    val token: String,
    val url: String,
)

suspend fun joinCall(channelId: String, nodeName: String): JoinCallResponse {
    val response = StoatHttp.post("/channels/$channelId/join_call".api()) {
        contentType(ContentType.Application.Json)
        setBody(mapOf("node" to nodeName))
    }.bodyAsText()

    try {
        val error = StoatJson.decodeFromString(StoatAPIError.serializer(), response)
        throw Exception(error.type)
    } catch (e: SerializationException) {
        // Not an error
    }

    return StoatJson.decodeFromString(JoinCallResponse.serializer(), response)
}