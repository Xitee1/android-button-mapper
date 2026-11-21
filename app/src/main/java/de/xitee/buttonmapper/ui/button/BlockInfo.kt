package de.xitee.buttonmapper.ui.button

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import de.xitee.buttonmapper.R
import de.xitee.buttonmapper.input.InputDevice
import de.xitee.buttonmapper.model.KeyTrigger

/** What "block normal button function" means for a trigger. Blocking always grabs the whole input device. */
sealed interface BlockInfo {
    val possible: Boolean

    /** Trigger matches any device, so there is no single device to grab. */
    data object NeedsDevice : BlockInfo { override val possible = false }

    /** Device currently not reported by the reader (e.g. service not connected). */
    data class DeviceUnknown(val device: String) : BlockInfo { override val possible = true }

    data class PowerKey(val device: String) : BlockInfo { override val possible = false }

    data class OnlyThisKey(val device: String) : BlockInfo { override val possible = true }

    data class AlsoBlocks(val device: String, val otherKeys: List<String>) : BlockInfo { override val possible = true }
}

fun blockInfo(trigger: KeyTrigger, devices: List<InputDevice>): BlockInfo {
    val name = trigger.deviceName ?: return BlockInfo.NeedsDevice
    val device = devices.firstOrNull { it.name == name } ?: return BlockInfo.DeviceUnknown(name)
    if (!device.canBeGrabbed) return BlockInfo.PowerKey(name)
    val others = device.keys.filter { it != trigger.keyName }
    return if (others.isEmpty()) BlockInfo.OnlyThisKey(name) else BlockInfo.AlsoBlocks(name, others)
}

@Composable
fun BlockInfoText(info: BlockInfo) {
    val (text, warning) = when (info) {
        BlockInfo.NeedsDevice -> stringResource(R.string.block_needs_device) to false
        is BlockInfo.DeviceUnknown -> stringResource(R.string.block_device_unknown, info.device) to false
        is BlockInfo.PowerKey -> stringResource(R.string.block_power_key, info.device) to true
        is BlockInfo.OnlyThisKey -> stringResource(R.string.block_only_this_key, info.device) to false
        is BlockInfo.AlsoBlocks ->
            stringResource(R.string.block_also_blocks, info.device, info.otherKeys.joinToString(", ")) to true
    }
    if (warning) Text(text, color = MaterialTheme.colorScheme.error) else Text(text)
}
