package chat.revolt.screens.settings

import android.content.Context
import android.content.Intent
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ListItem
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ToggleButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import chat.revolt.BuildConfig
import chat.revolt.RevoltApplication
import chat.revolt.api.settings.Experiments
import chat.revolt.api.settings.FeatureFlags
import chat.revolt.api.settings.LoadedSettings
import chat.revolt.persistence.KVStorage
import chat.revolt.settings.dsl.SettingsPage
import chat.revolt.settings.dsl.SubcategoryContentInsets
import kotlinx.coroutines.launch

enum class MarkdownRenderer {
    JetBrains, FinalMarkdown
}

class ExperimentsSettingsScreenViewModel : ViewModel() {
    private val kv = KVStorage(RevoltApplication.instance)

    fun init() {
        viewModelScope.launch {
            when {
                Experiments.useKotlinBasedMarkdownRenderer.isEnabled -> {
                    mdRenderer.value = MarkdownRenderer.JetBrains
                }

                Experiments.useFinalMarkdownRenderer.isEnabled -> {
                    mdRenderer.value = MarkdownRenderer.FinalMarkdown
                }

                else -> {
                    mdRenderer.value = MarkdownRenderer.JetBrains
                }
            }
            usePolarChecked.value = Experiments.usePolar.isEnabled
            enableServerIdentityOptionsChecked.value =
                Experiments.enableServerIdentityOptions.isEnabled
        }
    }

    val showNeedsRestartAlert = mutableStateOf(false)

    // cf. https://stackoverflow.com/a/46848226
    fun restartApp(context: Context) {
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        if (intent == null) {
            return
        }

        val componentName = intent.component
        val mainIntent = Intent.makeRestartActivityTask(componentName)
        mainIntent.`package` = context.packageName
        context.startActivity(mainIntent)
        Runtime.getRuntime().exit(0)
    }

    fun disableExperiments(then: () -> Unit = {}) {
        viewModelScope.launch {
            kv.remove("experimentsEnabled")
            LoadedSettings.experimentsEnabled = false
            then()
        }
    }

    val mdRenderer = mutableStateOf(MarkdownRenderer.JetBrains)

    fun setMdRenderer(value: MarkdownRenderer) {
        viewModelScope.launch {
            when (value) {
                MarkdownRenderer.JetBrains -> {
                    kv.set("exp/useKotlinBasedMarkdownRenderer", true)
                    Experiments.useKotlinBasedMarkdownRenderer.setEnabled(true)
                    kv.set("exp/useFinalMarkdownRenderer", false)
                    Experiments.useFinalMarkdownRenderer.setEnabled(false)
                }

                MarkdownRenderer.FinalMarkdown -> {
                    kv.set("exp/useKotlinBasedMarkdownRenderer", false)
                    Experiments.useKotlinBasedMarkdownRenderer.setEnabled(false)
                    kv.set("exp/useFinalMarkdownRenderer", true)
                    Experiments.useFinalMarkdownRenderer.setEnabled(true)
                }
            }
            mdRenderer.value = value
        }
    }

    val usePolarChecked = mutableStateOf(false)

    fun setUsePolarChecked(value: Boolean) {
        viewModelScope.launch {
            kv.set("exp/usePolar", value)
            Experiments.usePolar.setEnabled(value)
            showNeedsRestartAlert.value = true
            usePolarChecked.value = value
        }
    }

    val enableServerIdentityOptionsChecked = mutableStateOf(false)

    fun setEnableServerIdentityOptionsChecked(value: Boolean) {
        viewModelScope.launch {
            kv.set("exp/enableServerIdentityOptions", value)
            Experiments.enableServerIdentityOptions.setEnabled(value)
            enableServerIdentityOptionsChecked.value = value
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ExperimentsSettingsScreen(
    navController: NavController,
    viewModel: ExperimentsSettingsScreenViewModel = viewModel()
) {
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.init()
    }

    if (viewModel.showNeedsRestartAlert.value) {
        AlertDialog(
            onDismissRequest = {
                viewModel.showNeedsRestartAlert.value = false
            },
            title = {
                Text("Restart Required")
            },
            text = {
                Text("The changes you made require a restart to take effect.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.restartApp(context)
                    }
                ) {
                    Text("Restart")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        viewModel.showNeedsRestartAlert.value = false
                    }
                ) {
                    Text("Later")
                }
            }
        )
    }

    SettingsPage(
        navController,
        title = {
            Text("Experiments", maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            ListItem(
                headlineContent = {
                    Text("Markdown Renderer")
                },
                supportingContent = {
                    when (viewModel.mdRenderer.value) {
                        MarkdownRenderer.JetBrains -> Text("Use the Kotlin-based JetBrains Markdown renderer for messages. This renderer is feature-complete with support for strikethrough, spoilers, and more. (Default)")
                        MarkdownRenderer.FinalMarkdown -> Text("Use a new blazingly fast markdown renderer for messages. This renderer is experimental and may have missing features.")
                    }
                },
                modifier = Modifier
                    .animateContentSize()
                    .weight(1f)
            )
            Column(
                modifier = Modifier
                    .width(IntrinsicSize.Max)
                    .padding(end = 8.dp),
                verticalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
            ) {
                ToggleButton(
                    checked = viewModel.mdRenderer.value == MarkdownRenderer.JetBrains,
                    onCheckedChange = { viewModel.setMdRenderer(MarkdownRenderer.JetBrains) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics { role = Role.RadioButton }
                ) {
                    Text("JetBrains")
                }
                if (FeatureFlags.finalMarkdownGranted || viewModel.mdRenderer.value == MarkdownRenderer.FinalMarkdown) {
                    ToggleButton(
                        checked = viewModel.mdRenderer.value == MarkdownRenderer.FinalMarkdown,
                        onCheckedChange = { viewModel.setMdRenderer(MarkdownRenderer.FinalMarkdown) },
                        enabled = FeatureFlags.finalMarkdownGranted,
                        modifier = Modifier
                            .fillMaxWidth()
                            .semantics { role = Role.RadioButton }
                    ) {
                        Text("Final")
                    }
                }
            }
        }

        ListItem(
            headlineContent = {
                Text("Threefold Root User Interface")
            },
            supportingContent = {
                Text("Polar")
            },
            trailingContent = {
                Switch(
                    checked = viewModel.usePolarChecked.value,
                    onCheckedChange = null
                )
            },
            modifier = Modifier.clickable { viewModel.setUsePolarChecked(!viewModel.usePolarChecked.value) }
        )

        ListItem(
            headlineContent = {
                Text("Server Identity Options")
            },
            supportingContent = {
                Text("Enable options to control what parts of others' server identities you want to see.")
            },
            trailingContent = {
                Switch(
                    checked = viewModel.enableServerIdentityOptionsChecked.value,
                    onCheckedChange = null
                )
            },
            modifier = Modifier.clickable { viewModel.setEnableServerIdentityOptionsChecked(!viewModel.enableServerIdentityOptionsChecked.value) }
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