package io.github.jd1378.otphelper.ui.screens

import android.content.Context
import androidx.compose.runtime.Stable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jd1378.otphelper.UserSettings
import io.github.jd1378.otphelper.copy
import io.github.jd1378.otphelper.repository.UserSettingsRepository
import io.github.jd1378.otphelper.repository.DetectedCodeRepository
import io.github.jd1378.otphelper.data.local.entity.DetectedCode
import io.github.jd1378.otphelper.gmail.GmailFetchWorker
import io.github.jd1378.otphelper.utils.NotificationHelper
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@Stable
@HiltViewModel
class HomeViewModel
@Inject
constructor(
    private val savedStateHandle: SavedStateHandle,
    userSettingsRepository: UserSettingsRepository,
    detectedCodeRepository: DetectedCodeRepository,
) : ViewModel() {

  val userSettings =
      userSettingsRepository.userSettings.stateIn(
          scope = viewModelScope,
          started = SharingStarted.Eagerly,
          initialValue = UserSettings.getDefaultInstance().copy { isSetupFinished = true })

  val latestCodes =
      detectedCodeRepository.getLatest(6).stateIn(
          scope = viewModelScope,
          started = SharingStarted.WhileSubscribed(5_000),
          initialValue = emptyList<DetectedCode>(),
      )

  fun onSendTestNotifPressed(context: Context) {
    NotificationHelper.sendTestNotif(context)
  }

  fun fetchGmail(context: Context) {
    GmailFetchWorker.enqueue(context)
  }
}
