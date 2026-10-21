package io.github.jd1378.otphelper

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FloatingNavigationPreferenceTest {

  @Test
  fun floatingNavigationDefaultsOffForExistingSettings() {
    assertFalse(UserSettings.parseFrom(byteArrayOf()).useFloatingNavigationBar)
  }

  @Test
  fun floatingNavigationPreferenceSurvivesSerialization() {
    val settings = UserSettings.newBuilder().setUseFloatingNavigationBar(true).build()

    assertTrue(UserSettings.parseFrom(settings.toByteArray()).useFloatingNavigationBar)
  }
}
