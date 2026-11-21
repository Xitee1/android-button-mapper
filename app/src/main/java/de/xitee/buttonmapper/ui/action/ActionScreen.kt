package de.xitee.buttonmapper.ui.action

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.xitee.buttonmapper.R
import de.xitee.buttonmapper.model.Gesture
import de.xitee.buttonmapper.ui.MainViewModel
import de.xitee.buttonmapper.ui.components.InfoText
import de.xitee.buttonmapper.ui.components.SubPageTopBar
import de.xitee.buttonmapper.ui.labelRes

/** Settings page of one configured action. */
@Composable
fun ActionScreen(vm: MainViewModel, buttonId: String, gesture: Gesture, actionId: String, onBack: () -> Unit) {
    val config by vm.config.collectAsStateWithLifecycle()
    val cfg = config ?: return
    val action = cfg.button(buttonId)?.actions(gesture)?.firstOrNull { it.id == actionId }
    if (action == null) {
        LaunchedEffect(Unit) { onBack() }
        return
    }

    Scaffold(
        topBar = {
            SubPageTopBar(stringResource(action.config.labelRes), onBack) {
                IconButton(onClick = {
                    vm.removeAction(buttonId, gesture, actionId)
                    onBack()
                }) {
                    Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.action_remove))
                }
            }
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())) {
            if (action.config.hasSettings) {
                ActionSettings(action.config) { vm.updateAction(buttonId, gesture, actionId, it) }
            } else {
                InfoText(stringResource(R.string.action_no_settings))
            }
        }
    }
}
