package chat.stoat.api.routes.invites

import chat.stoat.api.StoatAPIError
import chat.stoat.api.StoatHttp
import chat.stoat.api.StoatJson
import chat.stoat.api.api
import chat.stoat.api.schemas.Invite
import chat.stoat.api.schemas.InviteJoined
import chat.stoat.api.schemas.RsResult
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.SerializationException

suspend fun fetchInviteByCode(code: String): RsResult<Invite, StoatAPIError> {
    val response = StoatHttp.get("/invites/$code".api())
        .bodyAsText()

    try {
        val error = StoatJson.decodeFromString(StoatAPIError.serializer(), response)
        if (error.type != "Server") return RsResult.err(error)
    } catch (e: SerializationException) {
        // Not an error
    }

    val invite = StoatJson.decodeFromString(Invite.serializer(), response)
    return RsResult.ok(invite)
}

suspend fun joinInviteByCode(code: String): RsResult<InviteJoined, StoatAPIError> {
    val response = StoatHttp.post("/invites/$code".api())
        .bodyAsText()

    try {
        val error = StoatJson.decodeFromString(StoatAPIError.serializer(), response)
        if (error.type != "Server") return RsResult.err(error)
    } catch (e: SerializationException) {
        // Not an error
    }

    val invite = StoatJson.decodeFromString(InviteJoined.serializer(), response)
    return RsResult.ok(invite)
}
