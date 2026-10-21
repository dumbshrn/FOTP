package io.github.jd1378.otphelper.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.jd1378.otphelper.R

@Composable
fun AppNavigationBar(
    currentRoute: String?,
    floating: Boolean,
    onNavigate: (String) -> Unit,
) {
  val currentPrimaryRoute =
      when (currentRoute) {
        MainDestinations.HISTORY_ROUTE -> MainDestinations.HISTORY_ROUTE
        MainDestinations.SETTINGS_ROUTE -> MainDestinations.SETTINGS_ROUTE
        else -> MainDestinations.HOME_ROUTE
      }
  val shape = RoundedCornerShape(28.dp)
  val modifier =
      if (floating) {
        Modifier.fillMaxWidth().padding(horizontal = 16.dp).shadow(8.dp, shape).clip(shape)
      } else {
        Modifier.fillMaxWidth()
      }

  if (floating) {
    Box(
        modifier = Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.navigationBars),
        contentAlignment = Alignment.Center,
    ) {
      NavigationBarContent(
          currentPrimaryRoute = currentPrimaryRoute,
          onNavigate = onNavigate,
          modifier = modifier,
          windowInsets = WindowInsets(0, 0, 0, 0),
      )
    }
  } else {
    NavigationBarContent(
        currentPrimaryRoute = currentPrimaryRoute,
        onNavigate = onNavigate,
        modifier = modifier,
        windowInsets = NavigationBarDefaults.windowInsets,
    )
  }
}

@Composable
private fun NavigationBarContent(
    currentPrimaryRoute: String,
    onNavigate: (String) -> Unit,
    modifier: Modifier,
    windowInsets: WindowInsets,
) {
  NavigationBar(
      modifier = modifier,
      containerColor = MaterialTheme.colorScheme.surface,
      tonalElevation = 0.dp,
      windowInsets = windowInsets,
  ) {
    NavigationBarItem(
        selected = currentPrimaryRoute == MainDestinations.HOME_ROUTE,
        onClick = { onNavigate(MainDestinations.HOME_ROUTE) },
        icon = { Icon(Icons.Filled.Home, contentDescription = null) },
        label = { Text(stringResource(R.string.codes)) },
    )
    NavigationBarItem(
        selected = currentPrimaryRoute == MainDestinations.HISTORY_ROUTE,
        onClick = { onNavigate(MainDestinations.HISTORY_ROUTE) },
        icon = { Icon(Icons.Filled.DateRange, contentDescription = null) },
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
