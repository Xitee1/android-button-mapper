package de.xitee.buttonmapper.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.xitee.buttonmapper.R
import de.xitee.buttonmapper.input.InputKeyEvent
import de.xitee.buttonmapper.model.ActionConfig
import de.xitee.buttonmapper.model.AppConfig
import de.xitee.buttonmapper.model.Gesture
import de.xitee.buttonmapper.model.KeyTrigger
import de.xitee.buttonmapper.model.Mapping

/**
 * Step-by-step dialog: the app listens for a button press as soon as it opens. Gesture, action and
 * the blocking option only appear once a button has been detected.
 */
@Composable
fun AddMappingDialog(vm: MainViewModel, config: AppConfig, onDismiss: () -> Unit) {
    val learned by vm.learnedKey.collectAsStateWithLifecycle()
    val connected by vm.readerConnected.collectAsStateWithLifecycle()
    val devices by vm.devices.collectAsStateWithLifecycle()
    var gesture by remember { mutableStateOf(Gesture.SINGLE) }
    var action by remember { mutableStateOf<ActionConfig>(ActionConfig.all.first()) }
    var block by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        vm.startLearning()
        onDispose { vm.stopLearning() }
    }

    val key = learned
    val trigger = key?.let { KeyTrigger(it.keyName, it.deviceName) }
    val info = trigger?.let { blockInfo(it, devices) }
    val duplicate = trigger != null && config.mappings.any {
        it.gesture == gesture && it.trigger.keyName == trigger.keyName &&
            (it.trigger.deviceName == null || it.trigger.deviceName == trigger.deviceName)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.dialog_title)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                StepTitle(R.string.dialog_step_button)
                ButtonStep(connected = connected, key = key, onRetry = vm::startLearning)

                if (trigger != null && info != null) {
                    StepTitle(R.string.dialog_step_gesture)
                    Column(Modifier.selectableGroup()) {
                        Gesture.entries.forEach { g ->
                            ChoiceRow(
                                selected = gesture == g,
                                label = stringResource(g.labelRes),
                                description = stringResource(g.descriptionRes),
                            ) { gesture = g }
                        }
                    }

                    StepTitle(R.string.dialog_step_action)
                    Column(Modifier.selectableGroup()) {
                        ActionConfig.all.forEach { a ->
                            ChoiceRow(selected = action == a, label = stringResource(a.labelRes)) { action = a }
                        }
                    }

                    StepTitle(R.string.dialog_step_system)
                    SwitchRow(
                        stringResource(R.string.block_system_function),
                        checked = block && info.possible,
                        enabled = info.possible,
                    ) { block = it }
                    BlockInfoText(info)

                    if (duplicate) {
                        Text(
                            stringResource(R.string.dialog_duplicate),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = trigger != null,
                onClick = {
                    trigger ?: return@TextButton
                    vm.saveMapping(
                        Mapping(
                            trigger = trigger,
                            gesture = gesture,
                            action = action,
                            blockSystemFunction = block && info?.possible == true,
                        ),
                    )
                    onDismiss()
                },
            ) { Text(stringResource(R.string.dialog_save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.dialog_cancel)) } },
    )
}

@Composable
private fun StepTitle(text: Int) {
    Text(stringResource(text), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
}

@Composable
private fun ButtonStep(connected: Boolean, key: InputKeyEvent?, onRetry: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            when {
                !connected -> Text(stringResource(R.string.dialog_not_connected), color = MaterialTheme.colorScheme.error)
                key == null -> {
                    Text(stringResource(R.string.dialog_press_button), style = MaterialTheme.typography.bodyLarge)
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                    Text(
                        stringResource(R.string.dialog_waiting),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                else -> {
                    Text(
                        stringResource(R.string.dialog_detected_button),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(key.keyName, style = MaterialTheme.typography.titleMedium, fontFamily = FontFamily.Monospace)
                    Text(
                        stringResource(R.string.dialog_detected_device, key.deviceName),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    TextButton(onClick = onRetry) { Text(stringResource(R.string.dialog_other_button)) }
                }
            }
        }
    }
}

@Composable
private fun ChoiceRow(selected: Boolean, label: String, description: String? = null, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null)
        Column(Modifier.padding(start = 12.dp)) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            description?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
