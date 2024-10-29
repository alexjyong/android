package chat.revolt.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import chat.revolt.BuildConfig
import chat.revolt.RevoltApplication
import chat.revolt.api.settings.Experiments
import chat.revolt.api.settings.LoadedSettings
import chat.revolt.persistence.KVStorage
import chat.revolt.settings.dsl.SettingsPage
import chat.revolt.settings.dsl.SubcategoryContentInsets
import kotlinx.coroutines.launch

class ExperimentsSettingsScreenViewModel : ViewModel() {
    private val kv = KVStorage(RevoltApplication.instance)

    fun init() {
        viewModelScope.launch {
            useKotlinMdRendererChecked.value = Experiments.useKotlinBasedMarkdownRenderer.isEnabled
            useMlKitSmartReplyInAppChecked.value = Experiments.useMlKitSmartReplyInApp.isEnabled
        }
    }

    fun disableExperiments(then: () -> Unit = {}) {
        viewModelScope.launch {
            kv.remove("experimentsEnabled")
            LoadedSettings.experimentsEnabled = false
            then()
        }
    }

    val useKotlinMdRendererChecked = mutableStateOf(false)
    val useMlKitSmartReplyInAppChecked = mutableStateOf(false)

    fun setUseKotlinMdRendererChecked(value: Boolean) {
        viewModelScope.launch {
            kv.set("exp/useKotlinBasedMarkdownRenderer", value)
            Experiments.useKotlinBasedMarkdownRenderer.setEnabled(value)
            useKotlinMdRendererChecked.value = value
        }
    }

    fun setUseMlKitSmartReplyInAppChecked(value: Boolean) {
        viewModelScope.launch {
            kv.set("exp/useMlKitSmartReplyInApp", value)
            Experiments.useMlKitSmartReplyInApp.setEnabled(value)
            useMlKitSmartReplyInAppChecked.value = value
        }
    }
}

@Composable
fun ExperimentsSettingsScreen(
    navController: NavController,
    viewModel: ExperimentsSettingsScreenViewModel = viewModel()
) {
    LaunchedEffect(Unit) {
        viewModel.init()
    }

    SettingsPage(
        navController,
        title = {
            Text("Experiments", maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    ) {
        ListItem(
            headlineContent = {
                Text("New Message Markdown Renderer")
            },
            supportingContent = {
                Text("Use a Kotlin-based Markdown renderer for messages rather than the C++ one. Missing features may be present.")
            },
            trailingContent = {
                Switch(
                    checked = viewModel.useKotlinMdRendererChecked.value,
                    onCheckedChange = viewModel::setUseKotlinMdRendererChecked
                )
            },
            modifier = Modifier.clickable { viewModel.setUseKotlinMdRendererChecked(!viewModel.useKotlinMdRendererChecked.value) }
        )

        ListItem(
            headlineContent = {
                Text("Smart Reply Suggestions (In-App)")
            },
            supportingContent = {
                Text("Use a machine learning model to suggest replies to messages from the message sheet.")
            },
            trailingContent = {
                Switch(
                    checked = viewModel.useMlKitSmartReplyInAppChecked.value,
                    onCheckedChange = viewModel::setUseMlKitSmartReplyInAppChecked
                )
            },
            modifier = Modifier.clickable { viewModel.setUseMlKitSmartReplyInAppChecked(!viewModel.useMlKitSmartReplyInAppChecked.value) }
        )

        Subcategory(
            title = {
                Text("Disable experiments")
            },
            contentInsets = SubcategoryContentInsets
        ) {
            ElevatedButton(
                onClick = {
                    viewModel.disableExperiments {
                        navController.popBackStack()
                    }
                },
                enabled = !BuildConfig.DEBUG,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (BuildConfig.DEBUG) {
                    Text("Experiments are always enabled in debug builds")
                } else {
                    Text("Disable")
                }
            }
        }
    }
}