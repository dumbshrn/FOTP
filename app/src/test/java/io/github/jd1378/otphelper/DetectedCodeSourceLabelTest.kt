package io.github.jd1378.otphelper

import io.github.jd1378.otphelper.ui.screens.detectedCodeSourceLabel
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
}
