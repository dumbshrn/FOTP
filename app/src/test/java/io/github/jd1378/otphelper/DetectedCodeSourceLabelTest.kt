package io.github.jd1378.otphelper

import io.github.jd1378.otphelper.ui.screens.detectedCodeSourceLabel
import io.github.jd1378.otphelper.ui.screens.detectedCodeIconPackageName
import org.junit.Assert.assertEquals
import org.junit.Test

class DetectedCodeSourceLabelTest {
  @Test
  fun gmailFetchIsIdentifiedAsGmailInsteadOfOtpHelper() {
    assertEquals("Gmail", detectedCodeSourceLabel("gmail", "", "OTP Helper"))
  }

  @Test
  fun smsOriginTakesPrecedenceOverAppLabel() {
    assertEquals("Bank", detectedCodeSourceLabel("", "Bank", "Messaging"))
  }

  @Test
  fun appLabelIsUsedForOtherNotifications() {
    assertEquals("SecureOTP", detectedCodeSourceLabel("", "", "SecureOTP"))
  }

  @Test
  fun gmailHistoryUsesGmailAppIconPackage() {
    assertEquals("com.google.android.gm", detectedCodeIconPackageName("gmail", "com.fotp"))
  }

  @Test
  fun otherHistoryUsesDetectedAppIconPackage() {
    assertEquals("com.example.sms", detectedCodeIconPackageName("", "com.example.sms"))
  }
}
