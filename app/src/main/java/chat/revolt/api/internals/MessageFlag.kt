package chat.revolt.api.internals

enum class MessageFlag(val value: Int) {
    // Message will not send push / desktop notifications
    SuppressNotifications(1 shl 0),

    // Message will mention all users who can see the channel
    MentionsEveryone(1 shl 1),

    // Message will mention all users who are online and can see the channel.
    // This cannot be true if MentionsEveryone is true
    MentionsOnline(1 shl 2)
}

operator fun Int.plus(other: MessageFlag): Int {
    return this or other.value
}

fun Int.hasMessageFlag(flag: MessageFlag): Boolean {
    return this and flag.value == flag.value
}

infix fun Int?.hasMessageFlag(flag: MessageFlag): Boolean {
    return this != null && this.hasMessageFlag(flag)
}