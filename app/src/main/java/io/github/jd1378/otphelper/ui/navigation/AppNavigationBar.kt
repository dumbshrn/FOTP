package io.github.jd1378.otphelper.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import io.github.jd1378.otphelper.R

@Composable
fun AppNavigationBar(currentRoute: String?, onNavigate: (String) -> Unit) {
  val currentPrimaryRoute =
      when (currentRoute) {
        MainDestinations.HISTORY_ROUTE -> MainDestinations.HISTORY_ROUTE
        MainDestinations.SETTINGS_ROUTE -> MainDestinations.SETTINGS_ROUTE
        else -> MainDestinations.HOME_ROUTE
      }

  NavigationBar {
    NavigationBarItem(
        selected = currentPrimaryRoute == MainDestinations.HOME_ROUTE,
        onClick = { onNavigate(MainDestinations.HOME_ROUTE) },
        icon = { Icon(Icons.Filled.Home, contentDescription = null) },
        label = { Text(stringResource(R.string.codes)) },
    )
    NavigationBarItem(
        selected = currentPrimaryRoute == MainDestinations.HISTORY_ROUTE,
        onClick = { onNavigate(MainDestinations.HISTORY_ROUTE) },
        icon = { Icon(Icons.Filled.History, contentDescription = null) },
        label = { Text(stringResource(R.string.history)) },
    )
    NavigationBarItem(
        selected = currentPrimaryRoute == MainDestinations.SETTINGS_ROUTE,
        onClick = { onNavigate(MainDestinations.SETTINGS_ROUTE) },
        icon = { Icon(Icons.Filled.Settings, contentDescription = null) },
        label = { Text(stringResource(R.string.settings)) },
    )
  }
}
