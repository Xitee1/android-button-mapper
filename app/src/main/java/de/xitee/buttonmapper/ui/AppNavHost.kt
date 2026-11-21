package de.xitee.buttonmapper.ui

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import de.xitee.buttonmapper.ui.action.ActionScreen
import de.xitee.buttonmapper.ui.add.AddButtonScreen
import de.xitee.buttonmapper.ui.button.ButtonScreen
import de.xitee.buttonmapper.ui.gesture.GestureScreen
import de.xitee.buttonmapper.ui.home.HomeScreen
import de.xitee.buttonmapper.ui.settings.SettingsScreen

@Composable
fun AppNavHost(vm: MainViewModel) {
    val nav = rememberNavController()
    NavHost(
        navController = nav,
        startDestination = HomeRoute,
        // Pages switch instantly, without the default cross-fade.
        enterTransition = { EnterTransition.None },
        exitTransition = { ExitTransition.None },
        popEnterTransition = { EnterTransition.None },
        popExitTransition = { ExitTransition.None },
    ) {
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
                onOpenGesture = { nav.navigate(GestureRoute(route.buttonId, it)) },
            )
        }
        composable<GestureRoute> { entry ->
            val route = entry.toRoute<GestureRoute>()
            GestureScreen(
                vm = vm,
                buttonId = route.buttonId,
                gesture = route.gesture,
                onBack = nav::popBackStack,
                onOpenAction = { nav.navigate(ActionRoute(route.buttonId, route.gesture, it)) },
            )
        }
        composable<ActionRoute> { entry ->
            val route = entry.toRoute<ActionRoute>()
            ActionScreen(
                vm = vm,
                buttonId = route.buttonId,
                gesture = route.gesture,
                actionId = route.actionId,
                onBack = nav::popBackStack,
            )
        }
        composable<SettingsRoute> {
            SettingsScreen(vm = vm, onBack = nav::popBackStack)
        }
    }
}
