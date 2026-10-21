package io.github.jd1378.otphelper.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.example.compose.md_theme_dark_background
import com.example.compose.md_theme_dark_error
import com.example.compose.md_theme_dark_errorContainer
import com.example.compose.md_theme_dark_inverseOnSurface
import com.example.compose.md_theme_dark_inversePrimary
import com.example.compose.md_theme_dark_inverseSurface
import com.example.compose.md_theme_dark_onBackground
import com.example.compose.md_theme_dark_onError
import com.example.compose.md_theme_dark_onErrorContainer
import com.example.compose.md_theme_dark_onPrimary
import com.example.compose.md_theme_dark_onPrimaryContainer
import com.example.compose.md_theme_dark_onSecondary
import com.example.compose.md_theme_dark_onSecondaryContainer
import com.example.compose.md_theme_dark_onSurface
import com.example.compose.md_theme_dark_onSurfaceVariant
import com.example.compose.md_theme_dark_onTertiary
import com.example.compose.md_theme_dark_onTertiaryContainer
import com.example.compose.md_theme_dark_outline
import com.example.compose.md_theme_dark_outlineVariant
import com.example.compose.md_theme_dark_primary
import com.example.compose.md_theme_dark_primaryContainer
import com.example.compose.md_theme_dark_scrim
import com.example.compose.md_theme_dark_secondary
import com.example.compose.md_theme_dark_secondaryContainer
import com.example.compose.md_theme_dark_surface
import com.example.compose.md_theme_dark_surfaceTint
import com.example.compose.md_theme_dark_surfaceVariant
import com.example.compose.md_theme_dark_tertiary
import com.example.compose.md_theme_dark_tertiaryContainer
import com.example.compose.md_theme_light_background
import com.example.compose.md_theme_light_error
import com.example.compose.md_theme_light_errorContainer
import com.example.compose.md_theme_light_inverseOnSurface
import com.example.compose.md_theme_light_inversePrimary
import com.example.compose.md_theme_light_inverseSurface
import com.example.compose.md_theme_light_onBackground
import com.example.compose.md_theme_light_onError
import com.example.compose.md_theme_light_onErrorContainer
import com.example.compose.md_theme_light_onPrimary
import com.example.compose.md_theme_light_onPrimaryContainer
import com.example.compose.md_theme_light_onSecondary
import com.example.compose.md_theme_light_onSecondaryContainer
import com.example.compose.md_theme_light_onSurface
import com.example.compose.md_theme_light_onSurfaceVariant
import com.example.compose.md_theme_light_onTertiary
import com.example.compose.md_theme_light_onTertiaryContainer
import com.example.compose.md_theme_light_outline
import com.example.compose.md_theme_light_outlineVariant
import com.example.compose.md_theme_light_primary
import com.example.compose.md_theme_light_primaryContainer
import com.example.compose.md_theme_light_scrim
import com.example.compose.md_theme_light_secondary
import com.example.compose.md_theme_light_secondaryContainer
import com.example.compose.md_theme_light_surface
import com.example.compose.md_theme_light_surfaceTint
import com.example.compose.md_theme_light_surfaceVariant
import com.example.compose.md_theme_light_tertiary
import com.example.compose.md_theme_light_tertiaryContainer

private val lightColors =
    lightColorScheme(
        primary = md_theme_light_primary,
        onPrimary = md_theme_light_onPrimary,
        primaryContainer = md_theme_light_primaryContainer,
        onPrimaryContainer = md_theme_light_onPrimaryContainer,
        secondary = md_theme_light_secondary,
        onSecondary = md_theme_light_onSecondary,
        secondaryContainer = md_theme_light_secondaryContainer,
        onSecondaryContainer = md_theme_light_onSecondaryContainer,
        tertiary = md_theme_light_tertiary,
        onTertiary = md_theme_light_onTertiary,
        tertiaryContainer = md_theme_light_tertiaryContainer,
        onTertiaryContainer = md_theme_light_onTertiaryContainer,
        error = md_theme_light_error,
        onError = md_theme_light_onError,
        errorContainer = md_theme_light_errorContainer,
        onErrorContainer = md_theme_light_onErrorContainer,
        outline = md_theme_light_outline,
        background = md_theme_light_background,
        onBackground = md_theme_light_onBackground,
        surface = md_theme_light_surface,
        onSurface = md_theme_light_onSurface,
        surfaceVariant = md_theme_light_surfaceVariant,
        onSurfaceVariant = md_theme_light_onSurfaceVariant,
        inverseSurface = md_theme_light_inverseSurface,
        inverseOnSurface = md_theme_light_inverseOnSurface,
        inversePrimary = md_theme_light_inversePrimary,
        surfaceTint = md_theme_light_surfaceTint,
        outlineVariant = md_theme_light_outlineVariant,
        scrim = md_theme_light_scrim,
    )

private val darkColors =
    darkColorScheme(
        primary = md_theme_dark_primary,
        onPrimary = md_theme_dark_onPrimary,
        primaryContainer = md_theme_dark_primaryContainer,
        onPrimaryContainer = md_theme_dark_onPrimaryContainer,
        secondary = md_theme_dark_secondary,
        onSecondary = md_theme_dark_onSecondary,
        secondaryContainer = md_theme_dark_secondaryContainer,
        onSecondaryContainer = md_theme_dark_onSecondaryContainer,
        tertiary = md_theme_dark_tertiary,
        onTertiary = md_theme_dark_onTertiary,
        tertiaryContainer = md_theme_dark_tertiaryContainer,
        onTertiaryContainer = md_theme_dark_onTertiaryContainer,
        error = md_theme_dark_error,
        onError = md_theme_dark_onError,
        errorContainer = md_theme_dark_errorContainer,
        onErrorContainer = md_theme_dark_onErrorContainer,
        outline = md_theme_dark_outline,
        background = md_theme_dark_background,
        onBackground = md_theme_dark_onBackground,
        surface = md_theme_dark_surface,
        onSurface = md_theme_dark_onSurface,
        surfaceVariant = md_theme_dark_surfaceVariant,
        onSurfaceVariant = md_theme_dark_onSurfaceVariant,
        inverseSurface = md_theme_dark_inverseSurface,
        inverseOnSurface = md_theme_dark_inverseOnSurface,
        inversePrimary = md_theme_dark_inversePrimary,
        surfaceTint = md_theme_dark_surfaceTint,
        outlineVariant = md_theme_dark_outlineVariant,
        scrim = md_theme_dark_scrim,
    )

data class CustomColors(val codeHighlight: Color, val phraseHighlight: Color)

val LocalCustomColors = compositionLocalOf {
  CustomColors(
      phraseHighlight = Color.Unspecified,
      codeHighlight = Color.Unspecified,
  )
}

@Composable
fun OtpHelperTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    amoledTheme: Boolean = false,
    colorPalette: AppColorPalette = AppColorPalette.MATERIAL_YOU,
    content: @Composable () -> Unit,
) {
  val colorScheme =
      when {
        colorPalette != AppColorPalette.MATERIAL_YOU && darkTheme ->
            customAccent(darkColors, colorPalette, true)
        colorPalette != AppColorPalette.MATERIAL_YOU -> customAccent(lightColors, colorPalette, false)
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
          val context = LocalContext.current
          if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> darkColors
        else -> lightColors
      }
  val resolvedColorScheme =
      if (amoledTheme) {
        colorScheme.copy(
            background = Color.Black,
            surface = Color.Black,
            surfaceVariant = Color(0xFF171717),
        )
      } else colorScheme
  val view = LocalView.current
  if (!view.isInEditMode) {
    SideEffect {
      val window = (view.context as Activity).window

      window.statusBarColor = resolvedColorScheme.background.toArgb()
      window.navigationBarColor = resolvedColorScheme.background.toArgb()
      window.decorView.setBackgroundColor(resolvedColorScheme.background.toArgb())
      WindowCompat.getInsetsController(window, view).apply {
        isAppearanceLightStatusBars = !darkTheme
        isAppearanceLightNavigationBars = !darkTheme
      }
    }
  }

  val customColors =
      if (darkTheme) {
        CustomColors(
            phraseHighlight = Color(0.384f, 0.788f, 0.969f, 1.0f),
            codeHighlight = Color(0.357f, 0.859f, 0.541f, 1.0f),
        )
      } else {
        CustomColors(
            phraseHighlight = Color(0.227f, 0.565f, 0.714f, 1.0f),
            codeHighlight = Color(0.231f, 0.639f, 0.38f, 1.0f),
        )
      }

  CompositionLocalProvider(LocalCustomColors provides customColors) {
    MaterialTheme(colorScheme = resolvedColorScheme, typography = Typography, content = content)
  }
}

private fun customAccent(
    scheme: androidx.compose.material3.ColorScheme,
    palette: AppColorPalette,
    isDark: Boolean,
): androidx.compose.material3.ColorScheme {
  val (primary, container, onContainer, secondary, tertiary) =
      when (palette) {
        AppColorPalette.BLUE ->
            if (isDark) listOf(Color(0xFFA8C7FA), Color(0xFF304A68), Color(0xFFD7E3FF), Color(0xFFB9C8E8), Color(0xFFB5D0F3))
            else listOf(Color(0xFF315B91), Color(0xFFD7E3FF), Color(0xFF0B1C34), Color(0xFF52627A), Color(0xFF4B6378))
        AppColorPalette.VIOLET ->
            if (isDark) listOf(Color(0xFFD0BCFF), Color(0xFF493B66), Color(0xFFEADDFF), Color(0xFFCBC0E8), Color(0xFFE2BBDD))
            else listOf(Color(0xFF62538C), Color(0xFFEADDFF), Color(0xFF21143F), Color(0xFF625A71), Color(0xFF7B526F))
        AppColorPalette.GREEN ->
            if (isDark) listOf(Color(0xFF8AD6B2), Color(0xFF20513B), Color(0xFFB8F2D0), Color(0xFFB0D0BE), Color(0xFFB6D2E5))
            else listOf(Color(0xFF176B49), Color(0xFFB8F2D0), Color(0xFF002114), Color(0xFF50665A), Color(0xFF416474))
        AppColorPalette.ROSE ->
            if (isDark) listOf(Color(0xFFFFB1C8), Color(0xFF633344), Color(0xFFFFD9E3), Color(0xFFE0BDC5), Color(0xFFE5BFD9))
            else listOf(Color(0xFF8D4058), Color(0xFFFFD9E3), Color(0xFF390A1C), Color(0xFF70565D), Color(0xFF78536B))
        AppColorPalette.MATERIAL_YOU -> return scheme
      }
  return scheme.copy(
      primary = primary,
      primaryContainer = container,
      onPrimaryContainer = onContainer,
      secondary = secondary,
      tertiary = tertiary,
      surfaceTint = primary,
  )
}
