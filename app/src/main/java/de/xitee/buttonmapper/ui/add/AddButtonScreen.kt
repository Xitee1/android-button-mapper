package de.xitee.buttonmapper.ui.add

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.xitee.buttonmapper.R
import de.xitee.buttonmapper.ui.MainViewModel
import de.xitee.buttonmapper.ui.components.SubPageTopBar
import kotlinx.coroutines.launch

/** Waits for a button press; "Continue" opens the (new or existing) button page. */
@Composable
fun AddButtonScreen(vm: MainViewModel, onBack: () -> Unit, onButtonReady: (String) -> Unit) {
    val learned by vm.learnedKey.collectAsStateWithLifecycle()
    val connected by vm.readerConnected.collectAsStateWithLifecycle()
    val config by vm.config.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    DisposableEffect(Unit) {
        vm.startLearning()
        onDispose { vm.stopLearning() }
    }

    val key = learned
    val alreadyConfigured = key != null && config?.buttons?.any {
        it.trigger.keyName == key.keyName && it.trigger.deviceName == key.deviceName
    } == true

    Scaffold(topBar = { SubPageTopBar(stringResource(R.string.add_button), onBack) }) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    when {
                        !connected -> Text(stringResource(R.string.add_not_connected), color = MaterialTheme.colorScheme.error)
                        key == null -> {
                            Text(stringResource(R.string.add_press_button), style = MaterialTheme.typography.titleMedium)
                            LinearProgressIndicator(Modifier.fillMaxWidth())
                            Text(
                                stringResource(R.string.add_waiting),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        else -> {
                            Text(
                                stringResource(R.string.add_detected_button),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(key.keyName, style = MaterialTheme.typography.headlineSmall, fontFamily = FontFamily.Monospace)
                            Text(
                                stringResource(R.string.add_detected_device, key.deviceName),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            if (alreadyConfigured) {
                                Text(stringResource(R.string.add_already_configured), style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }
            if (key != null) {
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { scope.launch { onButtonReady(vm.addButton(key)) } },
                ) { Text(stringResource(R.string.add_continue)) }
                OutlinedButton(modifier = Modifier.fillMaxWidth(), onClick = vm::startLearning) {
                    Text(stringResource(R.string.add_other_button))
                }
            }
        }
    }
}
