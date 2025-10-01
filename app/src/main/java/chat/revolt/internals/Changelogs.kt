package chat.revolt.internals

import android.content.Context
import chat.revolt.persistence.KVStorage
import kotlinx.serialization.Serializable

@Serializable
data class ChangelogIndex(
    val changelogs: List<ChangelogData> = emptyList()
)

@Serializable
data class ChangelogData(
    val version: ChangelogVersion,
    val date: ChangelogDate,
    val summary: String
)

@Serializable
data class ChangelogDate(
    val publish: String
)

@Serializable
data class ChangelogVersion(
    val code: Long,
    val name: String,
    val title: String
)

@Serializable
data class Changelog(
    val id: String,
    val slug: String,
    val body: String,
    val collection: String,
    val data: ChangelogData,
    val rendered: String
)

class Changelogs(val context: Context, val kvStorage: KVStorage? = null) {

    companion object {

        private val changelog1003008 = ChangelogData(
            version = ChangelogVersion(
                code = 1003008,
                name = "1.3.7bj-forked",
                title = "Updates!"
            ),
            date = ChangelogDate(
                publish = "2025-10-01T12:00:00.000Z"
            ),
            summary = "Better invite code generation, and first pass at Stoat adjustments"
        )

        private val changelog1003007 = ChangelogData(
            version = ChangelogVersion(
                code = 1003007,
                name = "1.3.7bi-forked",
                title = "Bug Fixes & Improvements"
            ),
            date = ChangelogDate(
                publish = "2025-09-29T12:00:00.000Z"
            ),
            summary = "Fixed splash screen logo, notification popup behavior, and attachment upload issues"
        )

        private val changelog1003006 = ChangelogData(
            version = ChangelogVersion(
                code = 1003006,
                name = "1.3.6bh-forked",
                title = "Notification Support!!!!🎉🎉"
            ),
            date = ChangelogDate(
                publish = "2025-09-26T12:00:00.000Z"
            ),
            summary = "Comprehensive notification system"
        )

        private val allChangelogs = listOf(
            changelog1003008,
            changelog1003007,
            changelog1003006
        )

        private fun getChangelogContent(versionCode: Long): String {
            return when (versionCode) {
                1003008L -> """
                    <h2>Updates</h2>
                    <h3>What's New:</h3>
                    <ul>
                        <li><strong>First pass at Stoat link updates</strong></li>
                        <li><strong>Users can now generate invite links from the Server and Channel Context menus. (Long press a server or channel to see!)</strong></li>
                    </ul>
                """.trimIndent()
                1003007L -> """
                    <h2>Bug Fixes & Improvements</h2>
                    <h3>What's Fixed:</h3>
                    <ul>
                        <li><strong>Fixed splash screen logo</strong> - The app logo now displays correctly on the splash screen.</li>
                        <li><strong>Improved notification permission popup</strong> - The notification permission request will now only appear once instead of repeatedly showing up</li>
                        <li><strong>Fixed feedback link</strong> - The feedback link on the main page now works properly</li>
                        <li><strong>Prevented duplicate attachment uploads</strong> - Fixed an issue where files could be sent multiple times during the upload process</li>
                    </ul>
                """.trimIndent()
                1003006L -> """
                    <h2>Notification Support!!!!🎉🎉</h2>
                    <ul>
                        <li><strong>Revolt Forked is now Refork and it now supports notifications!</strong>
                    </ul>

                    <h3>Other updates:</h3>
                    <ol>
                        <li><strong>Adding support for role, @here, and @everyone mentions, fixing rendering of role, channel names in messages.</strong></li>
                        <li><strong>Enhanced handling of Revolt URLs, Revolt Urls will now navigate to the given channel or server. role mention support</strong></li>
                        <li><strong>Everyone/here mention, ( and suppression options on a server/channel basis) </strong></li>
                    </ol>

                    <h2>How to Use The New Notification System:</h2>
                    <ol>
                        <li><strong>Enable Notification Permissions</strong>: There should have been a pop-up asking you for notification permissions. If not, you'll need to enable permissions for the app.</li>
                        <li><strong>Enable Background Notifications</strong>: Go to Settings → Notifications → Enable "Background Notifications" (an option to enable permissions for the app will be available here)</li>
                        <li><strong>(Optional, but recommended for reliable notifications) Optimize Battery Settings</strong>: Toggle "Battery Optimization" in notification settings and follow the system prompts to ensure reliable delivery</li>
                        <li><strong>Click Notifications</strong>: Simply tap any notification to jump directly to the message that triggered it</li>
                        <li><strong>Known issues</strong>: If the app is completely closed (not just minimized), navigation away from the channel/DM where the notification is will be disabled. Also, sometimes the name of the user will not show up.</li>
                    </ol>
                """.trimIndent()
                else -> "<p>Changelog content not available</p>"
            }
        }
    }

    suspend fun fetchChangelogIndex(): ChangelogIndex {
        return ChangelogIndex(changelogs = allChangelogs)
    }

    suspend fun fetchChangelogByVersionCode(versionCode: Long): Changelog {
        val changelogData = allChangelogs.find { it.version.code == versionCode }
        return changelogData?.let {
            Changelog(
                id = versionCode.toString(),
                slug = it.version.name,
                body = getChangelogContent(versionCode),
                collection = "changelogs",
                data = it,
                rendered = getChangelogContent(versionCode)
            )
        } ?: Changelog(
            id = "",
            slug = "",
            body = "Changelog not found",
            collection = "",
            data = ChangelogData(
                version = ChangelogVersion(
                    code = 0L,
                    name = "",
                    title = "Changelog not found"
                ),
                date = ChangelogDate(
                    publish = "1970-01-01T00:00:00.000Z"
                ),
                summary = "Changelog not found"
            ),
            rendered = "Changelog not found"
        )
    }

    suspend fun getLatestChangelog(): ChangelogData {
        return fetchChangelogIndex().changelogs.maxByOrNull { it.version.code }
            ?: throw IllegalStateException("No changelogs available")
    }

    suspend fun getLatestChangelogCode(): String {
        return getLatestChangelog().version.code.toString()
    }

    suspend fun hasSeenCurrent(): Boolean {
        if (kvStorage == null) {
            throw IllegalStateException(
                "Not supported for non-KVStorage instances of Changelogs"
            )
        }

        val latest = getLatestChangelog().version.code
        val lastRead = kvStorage.get("latestChangelogRead")

        if (lastRead == null) {
            return false
        }

        // If the last read changelog is >= the latest, it has been read
        return lastRead.toLong() >= latest
    }

    suspend fun markAsSeen() {
        if (kvStorage == null) {
            throw IllegalStateException(
                "Not supported for non-KVStorage instances of Changelogs"
            )
        }

        val index = fetchChangelogIndex()
        val latest = index.changelogs.maxByOrNull { it.version.code }!!.version.code.toString()
        kvStorage.set("latestChangelogRead", latest)
    }
}
