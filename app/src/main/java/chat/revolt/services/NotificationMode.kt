package chat.revolt.services

enum class NotificationMode(val value: String) {
    INSTANT("instant"),
    BATTERY_SAVER("battery_saver"),
    OFF("off");

    companion object {
        fun fromString(value: String?): NotificationMode {
            return values().find { it.value == value } ?: OFF
        }
    }
}