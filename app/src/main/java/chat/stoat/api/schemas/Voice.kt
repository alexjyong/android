package chat.stoat.api.schemas

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ChannelVoiceState(
    val id: String,
    val participants: List<UserVoiceState> = emptyList(),
)

@Serializable
data class UserVoiceState(
    val id: String,
    @SerialName("is_receiving") val isReceiving: Boolean,
    @SerialName("is_publishing") val isPublishing: Boolean,
    val screensharing: Boolean,
    val camera: Boolean,
    @SerialName(value = "joined_at") val joinedAt: String? = null,
)