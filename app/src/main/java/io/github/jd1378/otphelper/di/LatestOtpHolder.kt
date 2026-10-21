package io.github.jd1378.otphelper.di

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LatestOtpHolder @Inject constructor() {
  private val mutableLatestOtp = MutableStateFlow<LatestOtp?>(null)
  val latestOtp = mutableLatestOtp.asStateFlow()

  fun update(code: String, packageName: String, notificationTag: String, smsOrigin: String) {
    mutableLatestOtp.value =
        LatestOtp(code, packageName, notificationTag, smsOrigin, System.currentTimeMillis())
  }
}

data class LatestOtp(
    val code: String,
    val packageName: String,
    val notificationTag: String,
    val smsOrigin: String,
    val capturedAtMillis: Long,
)
