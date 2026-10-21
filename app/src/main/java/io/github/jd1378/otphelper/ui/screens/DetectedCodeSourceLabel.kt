package io.github.jd1378.otphelper.ui.screens

internal fun detectedCodeSourceLabel(notificationTag: String, smsOrigin: String, appLabel: String): String =
    when {
      notificationTag == "gmail" -> "Gmail"
      smsOrigin.isNotBlank() -> smsOrigin
      else -> appLabel
    }
