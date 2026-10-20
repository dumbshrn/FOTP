package io.github.jd1378.otphelper.gmail

import android.accounts.Account
import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.ClearTokenRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope
import com.google.android.gms.tasks.Tasks
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import io.github.jd1378.otphelper.BuildConfig
import io.github.jd1378.otphelper.R
import io.github.jd1378.otphelper.repository.UserSettingsRepository
import io.github.jd1378.otphelper.utils.CodeExtractor
import io.github.jd1378.otphelper.utils.NotificationHelper
import io.github.jd1378.otphelper.worker.CodeDetectedWorker
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.withContext

@HiltWorker
class GmailFetchWorker
@AssistedInject
constructor(
    @Assisted context: Context,
    @Assisted workerParameters: WorkerParameters,
    private val userSettingsRepository: UserSettingsRepository,
) : CoroutineWorker(context, workerParameters) {

  override suspend fun doWork(): Result =
      try {
        fetchAndNotify()
      } catch (error: CancellationException) {
        NotificationHelper.clearGmailFetchProgress(applicationContext)
        throw error
      } catch (error: Exception) {
        NotificationHelper.sendGmailFetchResult(
            applicationContext,
            applicationContext.getString(R.string.gmail_fetch_failed_result),
        )
        Result.failure()
      }

  private suspend fun fetchAndNotify(): Result {
    val context = applicationContext
    val accounts = GmailAccountStore(context).getAccounts()
    if (accounts.isEmpty()) {
      NotificationHelper.sendGmailFetchResult(
          context,
          context.getString(R.string.gmail_fetch_no_accounts),
      )
      return Result.success()
    }

    val authorizationClient = Identity.getAuthorizationClient(context)
    val requestSemaphore = Semaphore(MAX_CONCURRENT_MESSAGE_REQUESTS)
    val settings = userSettingsRepository.fetchSettings()
    val extractor =
        GmailCodeExtractor(
            CodeExtractor(
                settings.sensitivePhrasesList,
                settings.ignoredPhrasesList,
                settings.cleanupPhrasesList,
            )
        )

    NotificationHelper.sendGmailFetchProgress(
        context,
        context.getString(R.string.gmail_fetch_progress, 0, accounts.size),
    )

    val outcomes =
        GmailFetchCoordinator.fetchAccounts(
            accounts = accounts,
            maxConcurrent = MAX_CONCURRENT_ACCOUNTS,
            onAccountCompleted = { completed, _ ->
              NotificationHelper.sendGmailFetchProgress(
                  context,
                  context.getString(R.string.gmail_fetch_progress, completed, accounts.size),
              )
            },
        ) { emailAddress ->
          var accessToken: String? = null
          try {
            val request =
                AuthorizationRequest.builder()
                    .setRequestedScopes(listOf(Scope(GmailApiClient.GMAIL_READONLY_SCOPE)))
                    .setAccount(Account(emailAddress, GOOGLE_ACCOUNT_TYPE))
                    .build()
            val authorizationResult =
                withContext(Dispatchers.IO) {
                  Tasks.await(authorizationClient.authorize(request))
                }
            if (authorizationResult.hasResolution()) {
              return@fetchAccounts AccountFetchOutcome(emailAddress, failed = true)
            }
            val token = authorizationResult.accessToken?.takeIf(String::isNotBlank)
                ?: return@fetchAccounts AccountFetchOutcome(emailAddress, failed = true)
            accessToken = token

            val result =
                GmailApiClient(extractor).fetchLatestOtp(
                    accessToken = token,
                    emailAddressOverride = emailAddress,
                    requestSemaphore = requestSemaphore,
                )
            AccountFetchOutcome(emailAddress, result)
          } finally {
            accessToken?.let { token ->
              runCatching {
                withContext(Dispatchers.IO) {
                  Tasks.await(
                      authorizationClient.clearToken(
                          ClearTokenRequest.builder().setToken(token).build()
                      )
                  )
                }
              }
            }
          }
        }

    val summary = GmailFetchCoordinator.summarize(outcomes)
    val newestResult = summary.newestResult
    if (newestResult == null) {
      val message =
          if (summary.failedAccountEmails.isEmpty()) {
            context.getString(R.string.gmail_fetch_no_code_result)
          } else {
            context.getString(R.string.gmail_fetch_failed_result)
          }
      NotificationHelper.sendGmailFetchResult(context, message)
      return Result.success()
    }

    val code = newestResult.code ?: return Result.success()
    val workData =
        workDataOf(
            "packageName" to BuildConfig.APPLICATION_ID,
            "notificationId" to "gmail-fetch",
            "notificationTag" to "gmail",
            "text" to context.getString(R.string.gmail_history_entry, code),
            "code" to code,
            "is_gmail" to true,
        )
    WorkManager.getInstance(context)
        .enqueue(OneTimeWorkRequestBuilder<CodeDetectedWorker>().setInputData(workData).build())
    NotificationHelper.clearGmailFetchProgress(context)
    return Result.success()
  }

  companion object {
    private const val UNIQUE_WORK_NAME = "gmail-otp-fetch"
    private const val GOOGLE_ACCOUNT_TYPE = "com.google"
    private const val MAX_CONCURRENT_ACCOUNTS = 4
    private const val MAX_CONCURRENT_MESSAGE_REQUESTS = 8

    fun enqueue(context: Context) {
      val request = OneTimeWorkRequestBuilder<GmailFetchWorker>().build()
      WorkManager.getInstance(context.applicationContext)
          .enqueueUniqueWork(UNIQUE_WORK_NAME, ExistingWorkPolicy.KEEP, request)
    }
  }
}
