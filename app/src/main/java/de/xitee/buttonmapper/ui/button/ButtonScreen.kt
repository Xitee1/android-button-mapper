package de.xitee.buttonmapper.ui.button

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.xitee.buttonmapper.R
import de.xitee.buttonmapper.model.Gesture
import de.xitee.buttonmapper.ui.MainViewModel
import de.xitee.buttonmapper.ui.components.InfoText
import de.xitee.buttonmapper.ui.components.NavigationListItem
import de.xitee.buttonmapper.ui.components.SectionHeader
import de.xitee.buttonmapper.ui.components.SubPageTopBar
import de.xitee.buttonmapper.ui.components.SwitchListItem
import de.xitee.buttonmapper.ui.actionLabels
import de.xitee.buttonmapper.ui.labelRes

/** Settings of one button: general switches and one entry per gesture. */
@Composable
fun ButtonScreen(vm: MainViewModel, buttonId: String, onBack: () -> Unit, onOpenGesture: (Gesture) -> Unit) {
    val config by vm.config.collectAsStateWithLifecycle()
    val devices by vm.devices.collectAsStateWithLifecycle()
    var confirmDelete by remember { mutableStateOf(false) }

    val cfg = config ?: return
    val button = cfg.button(buttonId)
    if (button == null) {
        LaunchedEffect(Unit) { onBack() }
        return
    }
    val info = blockInfo(button.trigger, devices)

    Scaffold(
        topBar = {
            SubPageTopBar(button.trigger.keyName, onBack) {
                IconButton(onClick = { confirmDelete = true }) {
                    Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.button_delete))
                }
            }
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())) {
            InfoText(
                button.trigger.deviceName?.let { stringResource(R.string.button_device, it) }
                    ?: stringResource(R.string.device_any),
            )

            SectionHeader(stringResource(R.string.section_general))
            SwitchListItem(
                headline = stringResource(R.string.button_active),
                checked = button.enabled,
                onCheckedChange = { vm.setButtonEnabled(button.id, it) },
                supporting = { Text(stringResource(R.string.button_active_desc)) },
            )
            SwitchListItem(
                headline = stringResource(R.string.block_system_function),
                checked = button.blockSystemFunction,
                enabled = info.possible || button.blockSystemFunction,
                onCheckedChange = { vm.setButtonBlocksSystemFunction(button.id, it) },
                supporting = { BlockInfoText(info) },
            )

            SectionHeader(stringResource(R.string.section_gestures))
            Gesture.entries.forEach { gesture ->
                NavigationListItem(
                    headline = stringResource(gesture.labelRes),
                    supporting = actionLabels(button.actions(gesture)) ?: stringResource(R.string.action_none),
                    onClick = { onOpenGesture(gesture) },
                )
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.button_delete_title)) },
            text = { Text(stringResource(R.string.button_delete_text, button.trigger.keyName)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    vm.deleteButton(button.id)
                }) { Text(stringResource(R.string.button_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }
}
