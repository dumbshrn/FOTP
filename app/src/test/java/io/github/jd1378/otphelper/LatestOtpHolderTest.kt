package io.github.jd1378.otphelper

import io.github.jd1378.otphelper.di.LatestOtpHolder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LatestOtpHolderTest {
  @Test
  fun startsEmptyAndKeepsLatestCodeInMemory() {
    val holder = LatestOtpHolder()

    assertNull(holder.latestOtp.value)
    holder.update("45THTA12", "com.example.app", "", "")
    assertEquals("45THTA12", holder.latestOtp.value?.code)
    holder.update("829104", "", "gmail", "")
    assertEquals("829104", holder.latestOtp.value?.code)
    assertEquals("gmail", holder.latestOtp.value?.notificationTag)
  }
}
