package io.github.jd1378.otphelper.ui.theme

enum class AppThemeMode(val storedValue: Int) {
  SYSTEM(0),
  LIGHT(1),
  DARK(2);

  fun isDark(systemIsDark: Boolean): Boolean =
      when (this) {
        SYSTEM -> systemIsDark
        LIGHT -> false
        DARK -> true
      }

  companion object {
    fun fromStoredValue(value: Int): AppThemeMode =
        entries.firstOrNull { it.storedValue == value } ?: SYSTEM
  }
}

enum class AppColorPalette(val storedValue: Int) {
  MATERIAL_YOU(0),
  VIVI_INSPIRED(1);

  companion object {
    fun fromStoredValue(value: Int): AppColorPalette =
        entries.firstOrNull { it.storedValue == value } ?: MATERIAL_YOU
  }
}
