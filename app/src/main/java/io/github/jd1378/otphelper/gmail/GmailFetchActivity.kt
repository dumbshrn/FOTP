package io.github.jd1378.otphelper.gmail

import android.app.Activity
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.ClearTokenRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.identity.RevokeAccessRequest
import com.google.android.gms.common.api.Scope
import dagger.hilt.android.AndroidEntryPoint
import io.github.jd1378.otphelper.BuildConfig
import io.github.jd1378.otphelper.R
import io.github.jd1378.otphelper.repository.UserSettingsRepository
import io.github.jd1378.otphelper.utils.CodeExtractor
import io.github.jd1378.otphelper.worker.CodeDetectedWorker
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@AndroidEntryPoint
class GmailFetchActivity : AppCompatActivity() {
  @Inject lateinit var userSettingsRepository: UserSettingsRepository

  private val authorizationClient by lazy { Identity.getAuthorizationClient(this) }
  private val authorizationLauncher =
      registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        if (result.resultCode != Activity.RESULT_OK || result.data == null) {
          finishWithMessage(R.string.gmail_authorization_cancelled)
          return@registerForActivityResult
        }
        try {
          handleAuthorization(authorizationClient.getAuthorizationResultFromIntent(result.data))
        } catch (error: Exception) {
          finishWithMessage(R.string.gmail_fetch_failed)
        }
      }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    if (savedInstanceState != null) return
    when (intent.action) {
      ACTION_DISCONNECT -> disconnectGmail()
      else -> requestAuthorization()
    }
  }

  private fun requestAuthorization() {
    val request =
        AuthorizationRequest.builder()
            .setRequestedScopes(listOf(Scope(GmailApiClient.GMAIL_READONLY_SCOPE)))
            .build()
    authorizationClient
        .authorize(request)
        .addOnSuccessListener(::handleAuthorization)
        .addOnFailureListener { finishWithMessage(R.string.gmail_fetch_failed) }
  }

  private fun handleAuthorization(
      result: com.google.android.gms.auth.api.identity.AuthorizationResult
  ) {
    if (result.hasResolution()) {
      val pendingIntent =
          result.pendingIntent ?: run {
            finishWithMessage(R.string.gmail_fetch_failed)
            return
          }
      authorizationLauncher.launch(
          androidx.activity.result.IntentSenderRequest.Builder(pendingIntent.intentSender)
              .build()
      )
      return
    }

    val accessToken = result.accessToken
    if (accessToken.isNullOrBlank()) {
      finishWithMessage(R.string.gmail_fetch_failed)
      return
    }
    if (intent.action == ACTION_CONNECT) connect(accessToken) else fetch(accessToken)
  }

  private fun connect(accessToken: String) {
    lifecycleScope.launch {
      try {
        val emailAddress =
            kotlinx.coroutines.withContext(Dispatchers.IO) {
              GmailApiClient(GmailCodeExtractor()).getEmailAddress(accessToken)
            }
        if (emailAddress.isBlank()) throw IllegalStateException("No Gmail account address returned")
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            .edit()
            .putString(PREF_EMAIL, emailAddress)
            .apply()
        finishWithMessage(R.string.gmail_connected_success)
      } catch (error: Exception) {
        finishWithMessage(R.string.gmail_fetch_failed)
      } finally {
        runCatching {
          authorizationClient.clearToken(ClearTokenRequest.builder().setToken(accessToken).build())
        }
      }
    }
  }

  private fun fetch(accessToken: String) {
    lifecycleScope.launch {
      try {
        val settings = userSettingsRepository.fetchSettings()
        val extractor =
            GmailCodeExtractor(
                CodeExtractor(
                    settings.sensitivePhrasesList,
                    settings.ignoredPhrasesList,
                    settings.cleanupPhrasesList,
                )
            )
        val result =
            kotlinx.coroutines.withContext(Dispatchers.IO) {
              GmailApiClient(extractor).fetchLatestOtp(accessToken)
            }
        if (result.emailAddress.isNotBlank()) {
          getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
              .edit()
              .putString(PREF_EMAIL, result.emailAddress)
              .apply()
        }
        if (result.code == null) {
          finishWithMessage(R.string.gmail_no_code_found)
        } else {
          val data =
              workDataOf(
                  "packageName" to BuildConfig.APPLICATION_ID,
                  "notificationId" to "gmail-fetch",
                  "notificationTag" to "gmail",
                  "text" to getString(R.string.gmail_history_entry, result.code),
                  "code" to result.code,
                  "is_gmail" to true,
              )
          val work = OneTimeWorkRequestBuilder<CodeDetectedWorker>().setInputData(data).build()
          WorkManager.getInstance(applicationContext).enqueue(work)
          finishWithMessage(R.string.gmail_code_fetched)
        }
      } catch (error: Exception) {
        finishWithMessage(R.string.gmail_fetch_failed)
      } finally {
        runCatching {
          authorizationClient.clearToken(ClearTokenRequest.builder().setToken(accessToken).build())
        }
      }
    }
  }

  private fun disconnectGmail() {
    authorizationClient
        .revokeAccess(RevokeAccessRequest.builder().build())
        .addOnSuccessListener {
          getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit().remove(PREF_EMAIL).apply()
          finish()
        }
        .addOnFailureListener { finishWithMessage(R.string.gmail_disconnect_failed) }
  }

  private fun finishWithMessage(message: Int) {
    Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    finish()
  }

  companion object {
    const val PREFS_NAME = "gmail_connection"
    const val PREF_EMAIL = "connected_email"
    const val ACTION_FETCH = "com.dumbshrn.otphelper.action.FETCH_GMAIL_OTP"
    const val ACTION_CONNECT = "com.dumbshrn.otphelper.action.CONNECT_GMAIL"
    const val ACTION_DISCONNECT = "com.dumbshrn.otphelper.action.DISCONNECT_GMAIL"
  }
}
