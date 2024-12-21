package chat.revolt.api.internals

import chat.revolt.api.REVOLT_FILES
import chat.revolt.api.api
import chat.revolt.api.schemas.User

object ResourceLocations {
    fun userAvatarUrl(user: User?): String {
        if (user?.avatar != null) {
            return "$REVOLT_FILES/avatars/${user.avatar.id}"
        }
        return "/users/${(user?.id ?: "").ifBlank { "0".repeat(26) }}/default_avatar".api()
    }
}