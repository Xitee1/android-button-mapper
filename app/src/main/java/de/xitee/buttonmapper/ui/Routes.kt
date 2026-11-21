package de.xitee.buttonmapper.ui

import de.xitee.buttonmapper.model.Gesture
import kotlinx.serialization.Serializable

@Serializable
data object HomeRoute

@Serializable
data object AddButtonRoute

@Serializable
data class ButtonRoute(val buttonId: String)

@Serializable
data class ActionRoute(val buttonId: String, val gesture: Gesture)

@Serializable
data object SettingsRoute
