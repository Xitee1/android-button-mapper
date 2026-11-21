package de.xitee.buttonmapper.ui

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import de.xitee.buttonmapper.ui.action.ActionScreen
import de.xitee.buttonmapper.ui.add.AddButtonScreen
import de.xitee.buttonmapper.ui.button.ButtonScreen
import de.xitee.buttonmapper.ui.home.HomeScreen
import de.xitee.buttonmapper.ui.settings.SettingsScreen

@Composable
fun AppNavHost(vm: MainViewModel) {
    val nav = rememberNavController()
    NavHost(nav, startDestination = HomeRoute) {
        composable<HomeRoute> {
            HomeScreen(
                vm = vm,
                onAddButton = { nav.navigate(AddButtonRoute) },
                onOpenButton = { nav.navigate(ButtonRoute(it)) },
                onOpenSettings = { nav.navigate(SettingsRoute) },
            )
        }
        composable<AddButtonRoute> {
            AddButtonScreen(
                vm = vm,
                onBack = nav::popBackStack,
                onButtonReady = { id ->
                    nav.navigate(ButtonRoute(id)) { popUpTo<AddButtonRoute> { inclusive = true } }
                },
            )
        }
        composable<ButtonRoute> { entry ->
            val route = entry.toRoute<ButtonRoute>()
            ButtonScreen(
                vm = vm,
                buttonId = route.buttonId,
                onBack = nav::popBackStack,
                onOpenGesture = { nav.navigate(ActionRoute(route.buttonId, it)) },
            )
        }
        composable<ActionRoute> { entry ->
            val route = entry.toRoute<ActionRoute>()
            ActionScreen(vm = vm, buttonId = route.buttonId, gesture = route.gesture, onBack = nav::popBackStack)
        }
        composable<SettingsRoute> {
            SettingsScreen(vm = vm, onBack = nav::popBackStack)
        }
    }
}
