package io.github.jd1378.otphelper.ui.navigation

import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.compose.composable
import androidx.navigation.createGraph
import androidx.navigation.testing.TestNavHostController
import androidx.test.core.app.ApplicationProvider
import android.content.Context
import org.junit.Assert.assertEquals
import org.junit.Test

class TheNavControllerTest {
  @Test
  fun navigateToHomeReturnsFromHistoryToExistingHomeEntry() {
    val navController =
        TestNavHostController(ApplicationProvider.getApplicationContext<Context>()).apply {
          navigatorProvider.addNavigator(ComposeNavigator())
          graph =
              createGraph(startDestination = MainDestinations.HOME_ROUTE) {
                composable(MainDestinations.HOME_ROUTE) {}
                composable(MainDestinations.HISTORY_ROUTE) {}
              }
        }
    navController.navigate(MainDestinations.HISTORY_ROUTE)

    TheNavController(navController).navigateToHome()

    assertEquals(MainDestinations.HOME_ROUTE, navController.currentDestination?.route)
  }
}
