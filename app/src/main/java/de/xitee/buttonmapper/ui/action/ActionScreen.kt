package de.xitee.buttonmapper.ui.action

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.xitee.buttonmapper.R
import de.xitee.buttonmapper.model.ActionConfig
import de.xitee.buttonmapper.model.Gesture
import de.xitee.buttonmapper.ui.MainViewModel
import de.xitee.buttonmapper.ui.components.InfoText
import de.xitee.buttonmapper.ui.components.RadioListItem
import de.xitee.buttonmapper.ui.components.SectionHeader
import de.xitee.buttonmapper.ui.components.SubPageTopBar
import de.xitee.buttonmapper.ui.descriptionRes
import de.xitee.buttonmapper.ui.labelRes

/** Chooses the action of one gesture of one button and shows that action's own settings. */
@Composable
fun ActionScreen(vm: MainViewModel, buttonId: String, gesture: Gesture, onBack: () -> Unit) {
    val config by vm.config.collectAsStateWithLifecycle()
    val cfg = config ?: return
    val button = cfg.button(buttonId)
    if (button == null) {
        LaunchedEffect(Unit) { onBack() }
        return
    }
    val current = button.actions[gesture]

    Scaffold(topBar = { SubPageTopBar(stringResource(gesture.labelRes), onBack) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())) {
            InfoText(
                stringResource(R.string.action_page_intro, stringResource(gesture.descriptionRes), button.trigger.keyName),
            )

            SectionHeader(stringResource(R.string.section_action))
            Column(Modifier.selectableGroup()) {
                RadioListItem(
                    headline = stringResource(R.string.action_none),
                    selected = current == null,
                    onClick = { vm.setAction(button.id, gesture, null) },
                )
                ActionConfig.defaults.forEach { default ->
                    val selected = current != null && current::class == default::class
                    RadioListItem(
                        headline = stringResource(default.labelRes),
                        selected = selected,
                        // Keep the existing settings when the same action type is selected again.
                        onClick = { if (!selected) vm.setAction(button.id, gesture, default) },
                    )
                }
            }

            if (current != null && current.hasSettings) {
                SectionHeader(stringResource(R.string.section_action_settings))
                ActionSettings(current) { vm.setAction(button.id, gesture, it) }
            }
        }
    }
}
