package de.xitee.buttonmapper.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.xitee.buttonmapper.input.InputDevice
import de.xitee.buttonmapper.model.Mapping
import de.xitee.buttonmapper.model.Timing
import de.xitee.buttonmapper.shizuku.ShizukuStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(vm: MainViewModel) {
    val config by vm.config.collectAsStateWithLifecycle()
    val shizuku by vm.shizukuStatus.collectAsStateWithLifecycle()
    val running by vm.serviceRunning.collectAsStateWithLifecycle()
    val connected by vm.readerConnected.collectAsStateWithLifecycle()
    val devices by vm.devices.collectAsStateWithLifecycle()
    val lastEvent by vm.lastEvent.collectAsStateWithLifecycle()
    val lastAction by vm.lastAction.collectAsStateWithLifecycle()
    val lastError by vm.lastError.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }

    val cfg = config ?: return

    Scaffold(
        topBar = { TopAppBar(title = { Text("Button Mapper") }) },
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = { showAddDialog = true }) { Text("Zuordnung hinzufügen") }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                StatusCard(
                    shizuku = shizuku,
                    serviceEnabled = cfg.serviceEnabled,
                    running = running,
                    connected = connected,
                    lastEvent = lastEvent?.let { "${it.keyName} ${it.action} · ${it.deviceName}" },
                    lastAction = lastAction,
                    lastError = lastError,
                    onRequestPermission = vm::requestShizukuPermission,
                    onServiceEnabled = vm::setServiceEnabled,
                )
            }

            item { SectionTitle("Zuordnungen") }
            if (cfg.mappings.isEmpty()) {
                item { Hint("Noch keine Zuordnungen. Tippe auf „Zuordnung hinzufügen“.") }
            }
            items(cfg.mappings, key = { it.id }) { mapping ->
                MappingCard(
                    mapping = mapping,
                    onEnabled = { vm.setMappingEnabled(mapping.id, it) },
                    onDelete = { vm.deleteMapping(mapping.id) },
                )
            }

            item { SectionTitle("Erkennung") }
            item { TimingCard(cfg.timing, vm::setTiming) }

            item { SectionTitle("Eingabegeräte") }
            if (devices.isEmpty()) {
                item { Hint("Geräte erscheinen, sobald der Dienst über Shizuku verbunden ist.") }
            }
            items(devices.filter { it.keys.isNotEmpty() }, key = { it.path }) { device ->
                DeviceCard(
                    device = device,
                    exclusive = device.name in cfg.exclusiveDevices,
                    onExclusive = { vm.setDeviceExclusive(device.name, it) },
                )
            }
        }
    }

    if (showAddDialog) {
        AddMappingDialog(
            vm = vm,
            config = cfg,
            onDismiss = { showAddDialog = false },
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
}

@Composable
private fun Hint(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun StatusCard(
    shizuku: ShizukuStatus,
    serviceEnabled: Boolean,
    running: Boolean,
    connected: Boolean,
    lastEvent: String?,
    lastAction: String?,
    lastError: String?,
    onRequestPermission: () -> Unit,
    onServiceEnabled: (Boolean) -> Unit,
) {
    val ok = shizuku == ShizukuStatus.READY && connected
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (ok) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Mapper aktiv", style = MaterialTheme.typography.titleMedium)
                    Text(
                        when {
                            !serviceEnabled -> "Dienst ausgeschaltet"
                            !running -> "Dienst startet …"
                            shizuku != ShizukuStatus.READY -> "Warte auf Shizuku"
                            connected -> "Tasten werden überwacht"
                            else -> "Verbinde mit Shizuku-Dienst …"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                Switch(checked = serviceEnabled, onCheckedChange = onServiceEnabled)
            }
            HorizontalDivider()
            Text(shizuku.label, style = MaterialTheme.typography.bodyMedium)
            if (shizuku == ShizukuStatus.NO_PERMISSION) {
                Button(onClick = onRequestPermission) { Text("Shizuku-Berechtigung anfordern") }
            }
            lastEvent?.let { Mono("Letzte Taste: $it") }
            lastAction?.let { Mono("Letzte Aktion: $it") }
            lastError?.let {
                Text("Hinweis: $it", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun Mono(text: String) {
    Text(text, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace)
}

@Composable
private fun MappingCard(mapping: Mapping, onEnabled: (Boolean) -> Unit, onDelete: () -> Unit) {
    Card {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(mapping.trigger.keyName, style = MaterialTheme.typography.titleSmall, fontFamily = FontFamily.Monospace)
                Text(
                    mapping.trigger.deviceName?.let { "Gerät: $it" } ?: "Gerät: alle",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text("${mapping.gesture.label} → ${mapping.action.label}", style = MaterialTheme.typography.bodyMedium)
            }
            Column(horizontalAlignment = Alignment.End) {
                Switch(checked = mapping.enabled, onCheckedChange = onEnabled)
                TextButton(onClick = onDelete) { Text("Löschen") }
            }
        }
    }
}

@Composable
private fun TimingCard(timing: Timing, onChange: (Timing) -> Unit) {
    Card {
        Column(Modifier.padding(16.dp)) {
            MsSlider("Zeitfenster Doppeldruck", timing.doublePressMs, 150f..800f) { onChange(timing.copy(doublePressMs = it)) }
            Spacer(Modifier.width(8.dp))
            MsSlider("Dauer Gedrückthalten", timing.holdMs, 250f..1500f) { onChange(timing.copy(holdMs = it)) }
        }
    }
}

@Composable
private fun MsSlider(label: String, value: Long, range: ClosedFloatingPointRange<Float>, onChange: (Long) -> Unit) {
    var current by remember(value) { mutableFloatStateOf(value.toFloat()) }
    Text("$label: ${current.toLong()} ms", style = MaterialTheme.typography.bodyMedium)
    Slider(
        value = current,
        onValueChange = { current = (it / 10).toInt() * 10f },
        onValueChangeFinished = { onChange(current.toLong()) },
        valueRange = range,
    )
}

@Composable
private fun DeviceCard(device: InputDevice, exclusive: Boolean, onExclusive: (Boolean) -> Unit) {
    Card {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(device.name, style = MaterialTheme.typography.titleSmall)
                    Text(device.path, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace)
                }
                Switch(
                    checked = exclusive,
                    enabled = device.canBeGrabbed || exclusive,
                    onCheckedChange = onExclusive,
                )
            }
            Text(
                device.keys.take(12).joinToString(", ") + if (device.keys.size > 12) ", …" else "",
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
            )
            Text(
                when {
                    !device.canBeGrabbed -> "Systemfunktion blockieren: nicht möglich (Gerät enthält die Power-Taste)"
                    exclusive && device.grabbed -> "Systemfunktion blockiert: Android empfängt keine Tasten dieses Geräts"
                    exclusive -> "Systemfunktion blockieren: aktiviert, wirkt sobald der Dienst verbunden ist"
                    else -> "Systemfunktion blockieren: aus (Android reagiert zusätzlich auf die Tasten)"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

