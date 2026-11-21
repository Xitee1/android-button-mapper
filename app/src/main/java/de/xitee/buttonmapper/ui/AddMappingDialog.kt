package de.xitee.buttonmapper.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.xitee.buttonmapper.model.ActionConfig
import de.xitee.buttonmapper.model.AppConfig
import de.xitee.buttonmapper.model.Gesture
import de.xitee.buttonmapper.model.KeyTrigger
import de.xitee.buttonmapper.model.Mapping

@Composable
fun AddMappingDialog(vm: MainViewModel, config: AppConfig, onDismiss: () -> Unit) {
    val learned by vm.learnedKey.collectAsStateWithLifecycle()
    val connected by vm.readerConnected.collectAsStateWithLifecycle()
    val devices by vm.devices.collectAsStateWithLifecycle()
    var onlyThisDevice by remember { mutableStateOf(true) }
    var block by remember { mutableStateOf(false) }
    var gesture by remember { mutableStateOf(Gesture.SINGLE) }
    var action by remember { mutableStateOf<ActionConfig>(ActionConfig.all.first()) }

    DisposableEffect(Unit) {
        vm.startLearning()
        onDispose { vm.stopLearning() }
    }

    val key = learned
    val duplicate = key != null && config.mappings.any {
        it.gesture == gesture && it.trigger.keyName == key.keyName &&
            (it.trigger.deviceName == null || !onlyThisDevice || it.trigger.deviceName == key.deviceName)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Neue Zuordnung") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("1. Taste", style = MaterialTheme.typography.titleSmall)
                when {
                    !connected -> Text(
                        "Der Dienst ist nicht mit Shizuku verbunden. Schalte „Mapper aktiv“ ein und erteile die Shizuku-Berechtigung.",
                        color = MaterialTheme.colorScheme.error,
                    )
                    key == null -> Text("Drücke jetzt die gewünschte Taste …")
                    else -> {
                        Text("${key.keyName}  (${key.deviceName})", fontFamily = FontFamily.Monospace)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = onlyThisDevice, onCheckedChange = { onlyThisDevice = it })
                            Text("Nur auf diesem Gerät")
                        }
                        val info = blockInfo(KeyTrigger(key.keyName, key.deviceName.takeIf { onlyThisDevice }), devices)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = block && info.possible,
                                enabled = info.possible,
                                onCheckedChange = { block = it },
                            )
                            Text("Systemfunktion der Taste blockieren")
                        }
                        BlockInfoText(info)
                        OutlinedButton(onClick = vm::startLearning) { Text("Andere Taste") }
                    }
                }

                Text("2. Geste", style = MaterialTheme.typography.titleSmall)
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    Gesture.entries.forEachIndexed { i, g ->
                        SegmentedButton(
                            selected = gesture == g,
                            onClick = { gesture = g },
                            shape = SegmentedButtonDefaults.itemShape(i, Gesture.entries.size),
                        ) { Text(g.shortLabel) }
                    }
                }

                Text("3. Aktion", style = MaterialTheme.typography.titleSmall)
                ActionConfig.all.forEach { a ->
                    Row(
                        Modifier.fillMaxWidth().selectable(selected = action == a, onClick = { action = a }),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = action == a, onClick = { action = a })
                        Text(a.label)
                    }
                }
                if (duplicate) {
                    Text(
                        "Für diese Taste und Geste gibt es bereits eine Zuordnung; beide werden ausgeführt.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = key != null,
                onClick = {
                    key ?: return@TextButton
                    val trigger = KeyTrigger(key.keyName, key.deviceName.takeIf { onlyThisDevice })
                    vm.saveMapping(
                        Mapping(
                            trigger = trigger,
                            gesture = gesture,
                            action = action,
                            blockSystemFunction = block && blockInfo(trigger, devices).possible,
                        ),
                    )
                    onDismiss()
                },
            ) { Text("Speichern") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Abbrechen") } },
    )
}

private val Gesture.shortLabel: String
    get() = when (this) {
        Gesture.SINGLE -> "Einfach"
        Gesture.DOUBLE -> "Doppelt"
        Gesture.HOLD -> "Halten"
    }
