package de.xitee.buttonmapper.ui

import de.xitee.buttonmapper.model.Gesture
import kotlinx.serialization.Serializable

@Serializable
data object HomeRoute

@Serializable
data object AddButtonRoute

@Serializable
data class ButtonRoute(val buttonId: String)

/** List of actions of one gesture of one button. */
@Serializable
data class GestureRoute(val buttonId: String, val gesture: Gesture)

/** Settings of one configured action. */
@Serializable
data class ActionRoute(val buttonId: String, val gesture: Gesture, val actionId: String)

@Serializable
data object SettingsRoute
