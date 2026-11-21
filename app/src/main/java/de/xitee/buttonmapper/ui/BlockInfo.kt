package de.xitee.buttonmapper.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import de.xitee.buttonmapper.input.InputDevice
import de.xitee.buttonmapper.model.KeyTrigger

/** What "block system function" means for a trigger. Blocking always grabs the whole input device. */
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
        BlockInfo.NeedsDevice ->
            "Nur möglich, wenn die Zuordnung auf ein Gerät beschränkt ist." to false
        is BlockInfo.DeviceUnknown ->
            "Sperrt das Gerät „${info.device}“. Das Gerät ist gerade nicht verbunden." to false
        is BlockInfo.PowerKey ->
            "Nicht möglich: Das Gerät „${info.device}“ enthält die Power-Taste." to true
        is BlockInfo.OnlyThisKey ->
            "Sperrt das Gerät „${info.device}“. Es enthält nur diese Taste." to false
        is BlockInfo.AlsoBlocks ->
            ("Sperrt das ganze Gerät „${info.device}“. Damit reagiert Android auch auf diese Tasten nicht mehr: " +
                info.otherKeys.joinToString(", ")) to true
    }
    Text(
        text,
        style = MaterialTheme.typography.bodySmall,
        color = if (warning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
