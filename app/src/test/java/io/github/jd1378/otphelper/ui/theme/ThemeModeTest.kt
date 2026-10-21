package io.github.jd1378.otphelper.ui.theme

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemeModeTest {
  @Test
  fun systemModeFollowsSystemDarkTheme() {
    assertTrue(AppThemeMode.SYSTEM.isDark(systemIsDark = true))
    assertFalse(AppThemeMode.SYSTEM.isDark(systemIsDark = false))
  }

  @Test
  fun lightModeStaysLightWhenSystemIsDark() {
    assertFalse(AppThemeMode.LIGHT.isDark(systemIsDark = true))
  }

  @Test
  fun darkModeStaysDarkWhenSystemIsLight() {
    assertTrue(AppThemeMode.DARK.isDark(systemIsDark = false))
    assertTrue(AppThemeMode.AMOLED.isDark(systemIsDark = false))
  }
}
