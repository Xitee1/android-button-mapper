package de.xitee.buttonmapper.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.xitee.buttonmapper.R
import de.xitee.buttonmapper.input.InputDevice
import de.xitee.buttonmapper.ui.MainViewModel
import de.xitee.buttonmapper.ui.components.InfoText
import de.xitee.buttonmapper.ui.components.SectionHeader
import de.xitee.buttonmapper.ui.components.SubPageTopBar
import de.xitee.buttonmapper.ui.components.ValueSlider
import de.xitee.buttonmapper.ui.labelRes
import de.xitee.buttonmapper.ui.message

/** Rarely changed settings plus diagnostics. */
@Composable
fun SettingsScreen(vm: MainViewModel, onBack: () -> Unit) {
    val config by vm.config.collectAsStateWithLifecycle()
    val shizuku by vm.shizukuStatus.collectAsStateWithLifecycle()
    val devices by vm.devices.collectAsStateWithLifecycle()
    val lastEvent by vm.lastEvent.collectAsStateWithLifecycle()
    val lastAction by vm.lastAction.collectAsStateWithLifecycle()
    val lastError by vm.lastError.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val cfg = config ?: return
    val none = stringResource(R.string.diagnostics_none)

    Scaffold(topBar = { SubPageTopBar(stringResource(R.string.settings_title), onBack) }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(bottom = 24.dp)) {
            item { SectionHeader(stringResource(R.string.section_detection)) }
            item {
                ValueSlider(
                    label = { stringResource(R.string.timing_double_press, it) },
                    value = cfg.timing.doublePressMs,
                    range = 150f..800f,
                    step = 10,
                ) { vm.setTiming(cfg.timing.copy(doublePressMs = it)) }
            }
            item {
                ValueSlider(
                    label = { stringResource(R.string.timing_hold, it) },
                    value = cfg.timing.holdMs,
                    range = 250f..1500f,
                    step = 10,
                ) { vm.setTiming(cfg.timing.copy(holdMs = it)) }
            }

            item { SectionHeader(stringResource(R.string.section_diagnostics)) }
            item {
                Column {
                    InfoText(stringResource(R.string.diagnostics_shizuku, stringResource(shizuku.labelRes)))
                    InfoText(
                        stringResource(
                            R.string.diagnostics_last_button,
                            lastEvent?.let { "${it.keyName} ${it.action} · ${it.deviceName}" } ?: none,
                        ),
                    )
                    InfoText(stringResource(R.string.diagnostics_last_action, lastAction ?: none))
                    InfoText(stringResource(R.string.diagnostics_last_error, lastError?.message(context) ?: none))
                }
            }

            item { SectionHeader(stringResource(R.string.section_devices)) }
            item { InfoText(stringResource(R.string.section_devices_desc)) }
            if (devices.isEmpty()) {
                item { InfoText(stringResource(R.string.devices_empty)) }
            }
            items(devices.filter { it.keys.isNotEmpty() }, key = { it.path }) { DeviceCard(it) }
        }
    }
}

@Composable
private fun DeviceCard(device: InputDevice) {
    Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
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
