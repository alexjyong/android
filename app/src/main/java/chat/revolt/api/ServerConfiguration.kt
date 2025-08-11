package chat.revolt.api

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import chat.revolt.RevoltApplication
import chat.revolt.api.routes.misc.Root
import chat.revolt.api.routes.misc.getRootRoute
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

@Serializable
data class ServerConfig(
    val name: String = "Revolt Chat",
    val apiBase: String = "https://api.revolt.chat/0.8",
    val filesBase: String = "https://cdn.revoltusercontent.com",
    val januaryBase: String = "https://jan.revolt.chat",
    val websocketBase: String = "wss://ws.revolt.chat",
    val appBase: String = "https://app.revolt.chat",
    val vapidKey: String = ""
) {
    companion object {
        fun getDefault() = ServerConfig()
    }
}

object ServerConfiguration {
    private const val PREFS_NAME = "server_config"
    private const val KEY_SERVER_CONFIG = "current_server"
    
    private val prefs: SharedPreferences by lazy {
        RevoltApplication.instance.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
    
    var current by mutableStateOf(loadFromPreferences())
        private set
    
    init {
        // Load saved configuration on startup
        current = loadFromPreferences()
    }
    
    fun updateServer(config: ServerConfig) {
        current = config
        saveToPreferences(config)
        
        // Only trigger WebSocket reconnection if user is already logged in
        // During login flow, this will be handled naturally by the login process
        if (RevoltAPI.sessionToken.isNotEmpty() && RevoltAPI.selfId != null) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    RevoltAPI.reconnectWS()
                } catch (e: Exception) {
                    // Ignore reconnection errors - user can manually reconnect
                }
            }
        }
    }
    
    fun resetToDefault() {
        updateServer(ServerConfig.getDefault())
    }
    
    private fun saveToPreferences(config: ServerConfig) {
        val json = RevoltJson.encodeToString(ServerConfig.serializer(), config)
        prefs.edit().putString(KEY_SERVER_CONFIG, json).apply()
    }
    
    private fun loadFromPreferences(): ServerConfig {
        val json = prefs.getString(KEY_SERVER_CONFIG, null)
        return if (json != null) {
            try {
                RevoltJson.decodeFromString(ServerConfig.serializer(), json)
            } catch (e: Exception) {
                ServerConfig.getDefault()
            }
        } else {
            ServerConfig.getDefault()
        }
    }
}

/**
 * Discovers server configuration from a base URL
 */
suspend fun discoverServerConfig(baseUrl: String): Result<ServerConfig> {
    return try {
        // Create a temporary HTTP client for discovery
        val client = HttpClient(OkHttp) {
            install(ContentNegotiation) {
                json(RevoltJson)
            }
        }
        
        // Clean up the URL
        val cleanUrl = baseUrl.trimEnd('/')
        
        // Query the root endpoint
        val response = client.get("$cleanUrl/") {
            header("User-Agent", buildUserAgent("Discovery"))
        }
        
        val root = response.body<Root>()
        
        val config = ServerConfig(
            name = extractServerName(cleanUrl),
            apiBase = cleanUrl,
            filesBase = root.features.autumn.url,
            januaryBase = root.features.january.url,
            websocketBase = root.ws,
            appBase = root.app,
            vapidKey = root.vapid
        )
        
        client.close()
        Result.success(config)
    } catch (e: Exception) {
        Result.failure(e)
    }
}

private fun extractServerName(url: String): String {
    return try {
        val domain = url.removePrefix("https://").removePrefix("http://")
            .split("/")[0] // Get just the domain part
        when {
            domain.contains("revolt.chat") -> "Revolt Chat"
            domain.contains("localhost") -> "Local Server"
            else -> domain.split(".").firstOrNull()?.replaceFirstChar { it.uppercase() } ?: "Custom Server"
        }
    } catch (e: Exception) {
        "Custom Server"
    }
}
