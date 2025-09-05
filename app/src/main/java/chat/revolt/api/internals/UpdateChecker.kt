package chat.revolt.api.internals

import android.content.Context
import chat.revolt.BuildConfig
import chat.revolt.persistence.KVStorage
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class GitHubRelease(
    @SerialName("tag_name") val tagName: String,
    @SerialName("name") val name: String,
    @SerialName("body") val body: String?,
    @SerialName("html_url") val htmlUrl: String,
    @SerialName("assets") val assets: List<GitHubAsset>
)

@Serializable
data class GitHubAsset(
    @SerialName("name") val name: String,
    @SerialName("browser_download_url") val browserDownloadUrl: String
)

@Serializable
data class UpdateInfo(
    val version: String,
    val downloadUrl: String?,
    val releaseNotesUrl: String,
    val releaseNotes: String?
)

class UpdateChecker(
    private val context: Context,
    private val kvStorage: KVStorage
) {
    private val httpClient = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
    }

    private suspend fun fetchLatestRelease(): GitHubRelease? {
        return try {
            withContext(Dispatchers.IO) {
                val response = httpClient.get("https://api.github.com/repos/alexjyong/android/releases/latest")
                response.body<GitHubRelease>()
            }
        } catch (e: Exception) {
            android.util.Log.e("UpdateChecker", "Failed to fetch latest release", e)
            null
        }
    }

    suspend fun checkForUpdates(): UpdateInfo? {
        return try {
            val release = fetchLatestRelease() ?: return null
            val currentVersion = BuildConfig.VERSION_NAME
            val latestVersion = release.tagName

            // Simple string comparison - if they're different, there's an update
            if (currentVersion != latestVersion) {
                val apkAsset = release.assets.find { it.name.endsWith(".apk") }
                
                UpdateInfo(
                    version = latestVersion,
                    downloadUrl = apkAsset?.browserDownloadUrl,
                    releaseNotesUrl = release.htmlUrl,
                    releaseNotes = release.body
                )
            } else {
                null
            }
        } catch (e: Exception) {
            android.util.Log.e("UpdateChecker", "Failed to check for updates", e)
            null
        }
    }

    suspend fun shouldCheckForUpdates(): Boolean {
        val isEnabled = kvStorage.getBoolean("updateChecker/enabled") ?: true // Default enabled
        if (!isEnabled) return false

        val lastCheckTimeStr = kvStorage.get("updateChecker/lastCheck")
        val lastCheckTime = lastCheckTimeStr?.toLongOrNull() ?: 0
        val now = System.currentTimeMillis()
        val dayInMillis = 24 * 60 * 60 * 1000L
        
        return (now - lastCheckTime) > dayInMillis
    }

    suspend fun markUpdateCheckDone() {
        kvStorage.set("updateChecker/lastCheck", System.currentTimeMillis().toString())
    }

    suspend fun isUpdateCheckerEnabled(): Boolean {
        return kvStorage.getBoolean("updateChecker/enabled") ?: true
    }

    suspend fun setUpdateCheckerEnabled(enabled: Boolean) {
        kvStorage.set("updateChecker/enabled", enabled)
    }

    suspend fun dismissUpdate(version: String) {
        kvStorage.set("updateChecker/dismissed/$version", true)
    }

    suspend fun isUpdateDismissed(version: String): Boolean {
        return kvStorage.getBoolean("updateChecker/dismissed/$version") ?: false
    }
}