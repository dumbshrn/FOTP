package io.github.jd1378.otphelper

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import io.github.jd1378.otphelper.repository.UserSettingsRepository
import io.github.jd1378.otphelper.ui.navigation.MainDestinations
import io.github.jd1378.otphelper.ui.navigation.AppNavigationBar
import io.github.jd1378.otphelper.ui.navigation.otphelperNavGraph
import io.github.jd1378.otphelper.ui.navigation.rememberTheNavController
import io.github.jd1378.otphelper.ui.theme.OtpHelperTheme
import io.github.jd1378.otphelper.ui.theme.AppColorPalette
import io.github.jd1378.otphelper.ui.theme.AppThemeMode

@Composable
fun OtpHelperApp(
    deepLinkHandler: DeepLinkHandler,
    userSettingsRepository: UserSettingsRepository,
) {
  val userSettings by
      userSettingsRepository.userSettings.collectAsStateWithLifecycle(
          initialValue = io.github.jd1378.otphelper.UserSettings.getDefaultInstance())
  val colorPalette = AppColorPalette.fromStoredValue(userSettings.colorPalette)
  OtpHelperTheme(
      darkTheme = AppThemeMode.fromStoredValue(userSettings.themeMode).isDark(isSystemInDarkTheme()),
      amoledTheme = AppThemeMode.fromStoredValue(userSettings.themeMode) == AppThemeMode.AMOLED,
      dynamicColor = colorPalette == AppColorPalette.MATERIAL_YOU,
      colorPalette = colorPalette,
  ) {
    val theNavController = rememberTheNavController()
    val event by deepLinkHandler.event.collectAsState()
    val currentRoute by
        theNavController.currentRouteFlow.collectAsStateWithLifecycle(
            initialValue = MainDestinations.HOME_ROUTE)

    LaunchedEffect(event) {
      when (val currentEvent = event) {
        is Event.NavigateWithDeepLink ->
            theNavController.navController.handleDeepLink(currentEvent.intent)
        Event.None -> Unit
      }
      deepLinkHandler.consumeEvent()
    }

    Scaffold(
        bottomBar = {
          if (currentRoute in setOf(
                  MainDestinations.HOME_ROUTE,
                  MainDestinations.HISTORY_ROUTE,
                  MainDestinations.SETTINGS_ROUTE,
              )) {
            AppNavigationBar(currentRoute) { route ->
              theNavController.navigateToRoute(route, popToStart = true)
            }
          }
        },
    ) { contentPadding ->
      NavHost(
          navController = theNavController.navController,
          modifier = Modifier.padding(contentPadding),
          startDestination = MainDestinations.HOME_ROUTE,
          enterTransition = {
            slideInHorizontally(
                initialOffsetX = { 300 },
                animationSpec = tween(300, easing = FastOutSlowInEasing),
            ) + fadeIn(animationSpec = tween(300))
          },
          exitTransition = {
            slideOutHorizontally(
                targetOffsetX = { -300 },
                animationSpec = tween(300, easing = FastOutSlowInEasing),
            ) + fadeOut(animationSpec = tween(300))
          },
          popEnterTransition = {
            slideInHorizontally(
                initialOffsetX = { -300 },
                animationSpec = tween(300, easing = FastOutSlowInEasing),
            ) + fadeIn(animationSpec = tween(300))
          },
          popExitTransition = {
            slideOutHorizontally(
                targetOffsetX = { 300 },
                animationSpec = tween(300, easing = FastOutSlowInEasing),
            ) + fadeOut(animationSpec = tween(300))
          },
      ) {
        otphelperNavGraph(
            upPress = theNavController::upPress,
            onNavigateToRoute = theNavController::navigateToRoute)
      }
    }
  }
}
