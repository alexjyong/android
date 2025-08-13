package chat.revolt.screens.login

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import chat.revolt.R
import chat.revolt.api.*
import kotlinx.coroutines.launch

class ServerSelectionViewModel : ViewModel() {
    var serverUrl by mutableStateOf("https://revolt.chat")
    var isDiscovering by mutableStateOf(false)
    var discoveryError by mutableStateOf<String?>(null)
    var discoveredConfig by mutableStateOf<ServerConfig?>(null)
    var showAdvancedMode by mutableStateOf(false)
    
    var apiUrl by mutableStateOf("")
    var filesUrl by mutableStateOf("")
    var websocketUrl by mutableStateOf("")
    var serverName by mutableStateOf("")
    
    fun discoverServer() {
        if (serverUrl.isBlank()) return
        
        viewModelScope.launch {
            isDiscovering = true
            discoveryError = null
            discoveredConfig = null
            
            try {
                var cleanUrl = serverUrl.trim()
                if (!cleanUrl.startsWith("http://") && !cleanUrl.startsWith("https://")) {
                    cleanUrl = "https://$cleanUrl"
                }
                
                val potentialEndpoints = listOf(
                    cleanUrl,
                    if (cleanUrl.endsWith("/api")) cleanUrl else "$cleanUrl/api", 
                    cleanUrl.replace("://", "://api.")
                )
                
                var success = false
                for (endpoint in potentialEndpoints) {
                    val result = discoverServerConfig(endpoint)
                    if (result.isSuccess) {
                        discoveredConfig = result.getOrNull()
                        discoveryError = null
                        success = true
                        break
                    }
                }
                
                if (!success) {
                    discoveryError = "Could not discover server configuration. Try advanced mode for manual setup."
                    discoveredConfig = null
                }
            } catch (e: Exception) {
                discoveryError = e.message ?: "Unknown error occurred"
                discoveredConfig = null
            } finally {
                isDiscovering = false
            }
        }
    }
    
    fun populateAdvancedFromDiscovered() {
        discoveredConfig?.let { config ->
            serverName = config.name
            apiUrl = config.apiBase
            filesUrl = config.filesBase
            websocketUrl = config.websocketBase
        }
    }
    
    fun createManualConfig(): ServerConfig? {
        if (apiUrl.isBlank()) return null
        
        return ServerConfig(
            name = serverName.ifBlank { "Custom Server" },
            apiBase = apiUrl.trim(),
            filesBase = filesUrl.ifBlank { "$apiUrl/files" },
            websocketBase = websocketUrl.ifBlank { apiUrl.replace("http", "ws") },
            appBase = serverUrl.trim(),
            januaryBase = "",
            vapidKey = ""
        )
    }
    
    fun applyServerConfig(): Boolean {
        return discoveredConfig?.let { config ->
            ServerConfiguration.updateServer(config)
            true
        } ?: false
    }
    
    fun useDefaultServer() {
        ServerConfiguration.resetToDefault()
        serverUrl = "https://revolt.chat"
        discoveredConfig = ServerConfiguration.current
        discoveryError = null
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServerSelectionScreen(
    navController: NavController,
    viewModel: ServerSelectionViewModel = viewModel()
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    
    LaunchedEffect(Unit) {
        if (viewModel.discoveredConfig == null) {
            viewModel.useDefaultServer()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Select Server") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Choose which Revolt server to connect to",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            OutlinedTextField(
                value = viewModel.serverUrl,
                onValueChange = { 
                    viewModel.serverUrl = it
                    viewModel.discoveryError = null
                    viewModel.discoveredConfig = null
                },
                label = { Text("Server URL") },
                placeholder = { Text("https://revolt.example.com") },
                supportingText = { 
                    if (viewModel.discoveryError != null) {
                        Text(
                            text = viewModel.discoveryError!!,
                            color = MaterialTheme.colorScheme.error
                        )
                    } else {
                        Text("Enter the URL of your Revolt server")
                    }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                isError = viewModel.discoveryError != null,
                modifier = Modifier.fillMaxWidth()
            )
            
            Button(
                onClick = { viewModel.discoverServer() },
                enabled = !viewModel.isDiscovering && viewModel.serverUrl.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (viewModel.isDiscovering) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(Modifier.width(8.dp))
                }
                Text("Discover Server")
            }
            
            TextButton(
                onClick = { viewModel.showAdvancedMode = !viewModel.showAdvancedMode },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (viewModel.showAdvancedMode) "Hide Advanced Options" else "Advanced Options")
            }
            
            if (viewModel.showAdvancedMode) {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Manual Configuration",
                            style = MaterialTheme.typography.titleMedium
                        )
                        
                        viewModel.discoveredConfig?.let {
                            OutlinedButton(
                                onClick = { viewModel.populateAdvancedFromDiscovered() },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Populate from Discovered Configuration")
                            }
                        }
                        
                        OutlinedTextField(
                            value = viewModel.serverName,
                            onValueChange = { viewModel.serverName = it },
                            label = { Text("Server Name") },
                            placeholder = { Text("My Revolt Server") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        OutlinedTextField(
                            value = viewModel.apiUrl,
                            onValueChange = { viewModel.apiUrl = it },
                            label = { Text("API URL *") },
                            placeholder = { Text("https://api.revolt.example.com") },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri)
                        )
                        
                        OutlinedTextField(
                            value = viewModel.filesUrl,
                            onValueChange = { viewModel.filesUrl = it },
                            label = { Text("Files URL (optional)") },
                            placeholder = { Text("https://files.revolt.example.com") },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri)
                        )
                        
                        OutlinedTextField(
                            value = viewModel.websocketUrl,
                            onValueChange = { viewModel.websocketUrl = it },
                            label = { Text("WebSocket URL (optional)") },
                            placeholder = { Text("wss://ws.revolt.example.com") },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri)
                        )
                        
                        Button(
                            onClick = {
                                viewModel.createManualConfig()?.let { config ->
                                    viewModel.discoveredConfig = config
                                    viewModel.discoveryError = null
                                }
                            },
                            enabled = viewModel.apiUrl.isNotBlank(),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Use Manual Configuration")
                        }
                    }
                }
            }
            
            viewModel.discoveredConfig?.let { config ->
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = config.name,
                            style = MaterialTheme.typography.titleMedium
                        )
                        
                        Text(
                            text = "API: ${config.apiBase}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        Text(
                            text = "Files: ${config.filesBase}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        Spacer(Modifier.height(8.dp))
                        
                        Button(
                            onClick = { 
                                viewModel.applyServerConfig()
                                navController.navigate("login/login") {
                                    popUpTo("login/greeting") { inclusive = false }
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Continue to Login")
                        }
                    }
                }
            }
            
            Spacer(Modifier.weight(1f))
            
            if (viewModel.serverUrl != "https://revolt.chat") {
                OutlinedButton(
                    onClick = { 
                        viewModel.useDefaultServer()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Use Official Revolt Server")
                }
            }
        }
    }
}
