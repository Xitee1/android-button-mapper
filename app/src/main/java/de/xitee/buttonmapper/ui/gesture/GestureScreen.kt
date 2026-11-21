package de.xitee.buttonmapper.ui.gesture

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.xitee.buttonmapper.R
import de.xitee.buttonmapper.model.ActionConfig
import de.xitee.buttonmapper.model.Gesture
import de.xitee.buttonmapper.ui.MainViewModel
import de.xitee.buttonmapper.ui.actionSummary
import de.xitee.buttonmapper.ui.components.InfoText
import de.xitee.buttonmapper.ui.components.SectionHeader
import de.xitee.buttonmapper.ui.components.SubPageTopBar
import de.xitee.buttonmapper.ui.descriptionRes
import de.xitee.buttonmapper.ui.labelRes

/** The actions of one gesture of one button, run in list order. Each action opens its own settings page. */
@Composable
fun GestureScreen(
    vm: MainViewModel,
    buttonId: String,
    gesture: Gesture,
    onBack: () -> Unit,
    onOpenAction: (String) -> Unit,
) {
    val config by vm.config.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }
    val cfg = config ?: return
    val button = cfg.button(buttonId)
    if (button == null) {
        LaunchedEffect(Unit) { onBack() }
        return
    }
    val actions = button.actions(gesture)
    val itemColors = ListItemDefaults.colors(containerColor = Color.Transparent)

    Scaffold(topBar = { SubPageTopBar(stringResource(gesture.labelRes), onBack) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())) {
            InfoText(
                stringResource(R.string.gesture_page_intro, stringResource(gesture.descriptionRes), button.trigger.keyName),
            )

            SectionHeader(stringResource(R.string.section_actions))
            if (actions.isEmpty()) InfoText(stringResource(R.string.actions_empty))
            actions.forEach { action ->
                ListItem(
                    headlineContent = { Text(stringResource(action.config.labelRes)) },
                    supportingContent = { Text(actionSummary(action.config)) },
                    trailingContent = {
                        IconButton(onClick = { onOpenAction(action.id) }) {
                            Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.edit))
                        }
                    },
                    colors = itemColors,
                    modifier = Modifier.clickable { onOpenAction(action.id) },
                )
            }
            ListItem(
                headlineContent = { Text(stringResource(R.string.add_action), color = MaterialTheme.colorScheme.primary) },
                leadingContent = { Icon(Icons.Filled.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                colors = itemColors,
                modifier = Modifier.clickable { showAddDialog = true },
            )
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text(stringResource(R.string.add_action)) },
            text = {
                Column {
                    ActionConfig.defaults.forEach { default ->
                        ListItem(
                            headlineContent = { Text(stringResource(default.labelRes)) },
                            colors = itemColors,
                            modifier = Modifier.clickable {
                                vm.addAction(button.id, gesture, default)
                                showAddDialog = false
                            },
                        )
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { showAddDialog = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}
