package de.xitee.buttonmapper.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.xitee.buttonmapper.R
import de.xitee.buttonmapper.model.ButtonConfig
import de.xitee.buttonmapper.model.Gesture
import de.xitee.buttonmapper.shizuku.ShizukuStatus
import de.xitee.buttonmapper.ui.MainViewModel
import de.xitee.buttonmapper.ui.components.InfoText
import de.xitee.buttonmapper.ui.components.SectionHeader
import de.xitee.buttonmapper.ui.actionLabels
import de.xitee.buttonmapper.ui.labelRes
import de.xitee.buttonmapper.ui.message
import de.xitee.buttonmapper.ui.shortLabelRes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    vm: MainViewModel,
    onAddButton: () -> Unit,
    onOpenButton: (String) -> Unit,
    onOpenSettings: () -> Unit,
) {
    val config by vm.config.collectAsStateWithLifecycle()
    val shizuku by vm.shizukuStatus.collectAsStateWithLifecycle()
    val running by vm.serviceRunning.collectAsStateWithLifecycle()
    val connected by vm.readerConnected.collectAsStateWithLifecycle()
    val lastError by vm.lastError.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val cfg = config ?: return

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = stringResource(R.string.settings_title))
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddButton,
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.add_button)) },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 96.dp),
        ) {
            item {
                StatusCard(
                    shizuku = shizuku,
                    serviceEnabled = cfg.serviceEnabled,
                    running = running,
                    connected = connected,
                    error = lastError?.message(context),
                    onRequestPermission = vm::requestShizukuPermission,
                    onServiceEnabled = vm::setServiceEnabled,
                )
            }
            item { SectionHeader(stringResource(R.string.section_buttons)) }
            if (cfg.buttons.isEmpty()) {
                item { InfoText(stringResource(R.string.buttons_empty)) }
            }
            items(cfg.buttons, key = { it.id }) { button ->
                ButtonCard(button, onClick = { onOpenButton(button.id) })
            }
        }
    }
}

@Composable
private fun StatusCard(
    shizuku: ShizukuStatus,
    serviceEnabled: Boolean,
    running: Boolean,
    connected: Boolean,
    error: String?,
    onRequestPermission: () -> Unit,
    onServiceEnabled: (Boolean) -> Unit,
) {
    val ok = serviceEnabled && shizuku == ShizukuStatus.READY && connected
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (ok) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.status_title), style = MaterialTheme.typography.titleLarge)
                    Text(
                        stringResource(
                            when {
                                !serviceEnabled -> R.string.status_service_off
                                !running -> R.string.status_service_starting
                                shizuku != ShizukuStatus.READY -> shizuku.labelRes
                                connected -> R.string.status_monitoring
                                else -> R.string.status_connecting
                            },
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                Switch(checked = serviceEnabled, onCheckedChange = onServiceEnabled)
            }
            if (serviceEnabled && shizuku == ShizukuStatus.NO_PERMISSION) {
                Button(onClick = onRequestPermission) { Text(stringResource(R.string.shizuku_request_permission)) }
            }
            if (serviceEnabled && error != null) {
                Text(
                    stringResource(R.string.status_error, error),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
private fun ButtonCard(button: ButtonConfig, onClick: () -> Unit) {
    val summary = when {
        !button.enabled -> stringResource(R.string.button_off)
        button.mappedGestures.isEmpty() -> stringResource(R.string.button_no_actions)
        else -> Gesture.entries.mapNotNull { g ->
            actionLabels(button.actions(g))?.let { stringResource(R.string.gesture_summary, stringResource(g.shortLabelRes), it) }
        }.joinToString(" · ")
    }
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
    ) {
        ListItem(
            headlineContent = { Text(button.trigger.keyName, fontFamily = FontFamily.Monospace) },
            overlineContent = { Text(button.trigger.deviceName ?: stringResource(R.string.device_any)) },
            supportingContent = { Text(summary) },
            trailingContent = { Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.edit)) },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        )
    }
}
