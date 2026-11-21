package de.xitee.buttonmapper.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.xitee.buttonmapper.R
import de.xitee.buttonmapper.input.InputDevice
import de.xitee.buttonmapper.model.FlashlightSettings
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
    val context = LocalContext.current
    var showAddDialog by remember { mutableStateOf(false) }

    val cfg = config ?: return

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.app_name)) }) },
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = { showAddDialog = true }) { Text(stringResource(R.string.add_mapping)) }
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
                    lastError = lastError?.message(context),
                    onRequestPermission = vm::requestShizukuPermission,
                    onServiceEnabled = vm::setServiceEnabled,
                )
            }

            item { SectionTitle(R.string.section_mappings, R.string.section_mappings_desc) }
            if (cfg.mappings.isEmpty()) {
                item { Hint(stringResource(R.string.mappings_empty)) }
            }
            items(cfg.mappings, key = { it.id }) { mapping ->
                MappingCard(
                    mapping = mapping,
                    blockInfo = blockInfo(mapping.trigger, devices),
                    onEnabled = { vm.setMappingEnabled(mapping.id, it) },
                    onBlock = { vm.setMappingBlocksSystemFunction(mapping.id, it) },
                    onDelete = { vm.deleteMapping(mapping.id) },
                )
            }

            item { SectionTitle(R.string.section_flashlight, R.string.section_flashlight_desc) }
            item { FlashlightCard(cfg.flashlight, vm::setFlashlightSettings) }

            item { SectionTitle(R.string.section_detection, R.string.section_detection_desc) }
            item { TimingCard(cfg.timing, vm::setTiming) }

            item { SectionTitle(R.string.section_devices, R.string.section_devices_desc) }
            if (devices.isEmpty()) {
                item { Hint(stringResource(R.string.devices_empty)) }
            }
            items(devices.filter { it.keys.isNotEmpty() }, key = { it.path }) { device ->
                DeviceCard(device)
            }
        }
    }

    if (showAddDialog) {
        AddMappingDialog(vm = vm, config = cfg, onDismiss = { showAddDialog = false })
    }
}

@Composable
private fun SectionTitle(title: Int, description: Int) {
    Column(Modifier.padding(top = 8.dp)) {
        Text(stringResource(title), style = MaterialTheme.typography.titleMedium)
        Hint(stringResource(description))
    }
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
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (ok) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.status_title), style = MaterialTheme.typography.titleMedium)
                    Text(
                        stringResource(
                            when {
                                !serviceEnabled -> R.string.status_service_off
                                !running -> R.string.status_service_starting
                                shizuku != ShizukuStatus.READY -> R.string.status_waiting_shizuku
                                connected -> R.string.status_monitoring
                                else -> R.string.status_connecting
                            },
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                Switch(checked = serviceEnabled, onCheckedChange = onServiceEnabled)
            }
            HorizontalDivider()
            Text(stringResource(shizuku.labelRes), style = MaterialTheme.typography.bodyMedium)
            if (shizuku == ShizukuStatus.NO_PERMISSION) {
                Button(onClick = onRequestPermission) { Text(stringResource(R.string.shizuku_request_permission)) }
            }
            lastEvent?.let { Mono(stringResource(R.string.status_last_key, it)) }
            lastAction?.let { Mono(stringResource(R.string.status_last_action, it)) }
            lastError?.let {
                Text(
                    stringResource(R.string.status_error, it),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
private fun Mono(text: String) {
    Text(text, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace)
}

@Composable
private fun MappingCard(
    mapping: Mapping,
    blockInfo: BlockInfo,
    onEnabled: (Boolean) -> Unit,
    onBlock: (Boolean) -> Unit,
    onDelete: () -> Unit,
) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(mapping.trigger.keyName, style = MaterialTheme.typography.titleSmall, fontFamily = FontFamily.Monospace)
                    Text(
                        mapping.trigger.deviceName?.let { stringResource(R.string.mapping_device, it) }
                            ?: stringResource(R.string.mapping_device_any),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        "${stringResource(mapping.gesture.labelRes)} → ${stringResource(mapping.action.labelRes)}",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                TextButton(onClick = onDelete) { Text(stringResource(R.string.mapping_delete)) }
            }
            HorizontalDivider()
            SwitchRow(stringResource(R.string.mapping_active), mapping.enabled, enabled = true, onEnabled)
            SwitchRow(
                stringResource(R.string.block_system_function),
                mapping.blockSystemFunction,
                enabled = blockInfo.possible || mapping.blockSystemFunction,
                onBlock,
            )
            BlockInfoText(blockInfo)
        }
    }
}

@Composable
fun SwitchRow(label: String, checked: Boolean, enabled: Boolean, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Switch(checked = checked, enabled = enabled, onCheckedChange = onChange)
    }
}

@Composable
private fun FlashlightCard(settings: FlashlightSettings, onChange: (FlashlightSettings) -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            SwitchRow(stringResource(R.string.flashlight_auto_off), settings.autoOffEnabled, enabled = true) {
                onChange(settings.copy(autoOffEnabled = it))
            }
            if (settings.autoOffEnabled) {
                ValueSlider(
                    label = { stringResource(R.string.flashlight_auto_off_after, it) },
                    value = settings.autoOffMinutes.toLong(),
                    range = 1f..60f,
                    step = 1,
                ) { onChange(settings.copy(autoOffMinutes = it.toInt())) }
            }
        }
    }
}

@Composable
private fun TimingCard(timing: Timing, onChange: (Timing) -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            ValueSlider(
                label = { stringResource(R.string.timing_double_press, it) },
                value = timing.doublePressMs,
                range = 150f..800f,
                step = 10,
            ) { onChange(timing.copy(doublePressMs = it)) }
            Spacer(Modifier.height(8.dp))
            ValueSlider(
                label = { stringResource(R.string.timing_hold, it) },
                value = timing.holdMs,
                range = 250f..1500f,
                step = 10,
            ) { onChange(timing.copy(holdMs = it)) }
        }
    }
}

@Composable
private fun ValueSlider(
    label: @Composable (Long) -> String,
    value: Long,
    range: ClosedFloatingPointRange<Float>,
    step: Int,
    onChange: (Long) -> Unit,
) {
    var current by remember(value) { mutableFloatStateOf(value.toFloat()) }
    Text(label(current.toLong()), style = MaterialTheme.typography.bodyMedium)
    Slider(
        value = current,
        onValueChange = { current = (Math.round(it / step) * step).toFloat() },
        onValueChangeFinished = { onChange(current.toLong()) },
        valueRange = range,
    )
}

@Composable
private fun DeviceCard(device: InputDevice) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(device.name, style = MaterialTheme.typography.titleSmall)
            Text(device.path, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace)
            Text(
                device.keys.take(12).joinToString(", ") + if (device.keys.size > 12) ", …" else "",
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
            )
            Text(
                stringResource(if (device.grabbed) R.string.device_blocked else R.string.device_not_blocked),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
