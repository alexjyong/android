package chat.stoat.api.schemas

import chat.stoat.api.StoatAPI
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Session(
    @SerialName("_id") val id: String,
    val name: String
) {
    fun isCurrent(): Boolean {
        return id == StoatAPI.sessionId
    }
}
