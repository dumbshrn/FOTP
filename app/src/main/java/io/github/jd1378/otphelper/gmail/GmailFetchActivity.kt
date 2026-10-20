package io.github.jd1378.otphelper.gmail

import android.accounts.Account
import android.app.Activity
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
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
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@AndroidEntryPoint
class GmailFetchActivity : AppCompatActivity() {
  @Inject lateinit var userSettingsRepository: UserSettingsRepository

  private val authorizationClient by lazy { Identity.getAuthorizationClient(this) }
  private val accountStore by lazy { GmailAccountStore(this) }
  private val fetchOutcomes = mutableListOf<AccountFetchOutcome>()
  private lateinit var progressIndicator: ProgressBar
  private lateinit var statusText: TextView
  private lateinit var resultText: TextView
  private lateinit var doneButton: Button
  private var accountsToFetch: List<String> = emptyList()
  private var fetchIndex = 0
  private var authorizationEmail: String? = null
  private var authorizationPurpose = AuthorizationPurpose.CONNECT
  private var awaitingAuthorizationResult = false
  private var showingResult = false

  private val authorizationLauncher =
      registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        awaitingAuthorizationResult = false
        if (result.resultCode != Activity.RESULT_OK || result.data == null) {
          handleAuthorizationFailure()
          return@registerForActivityResult
        }
        try {
          handleAuthorization(authorizationClient.getAuthorizationResultFromIntent(result.data))
        } catch (error: Exception) {
          handleAuthorizationFailure()
        }
      }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    if (intent.action == ACTION_FETCH) {
      GmailFetchWorker.enqueue(applicationContext)
      finish()
      return
    }
    setContentView(createContent())
    if (savedInstanceState != null) {
      restoreState(savedInstanceState)
      when {
        showingResult -> showResult(resultText.text.toString())
        awaitingAuthorizationResult -> showProgress(statusText.text.toString())
        else -> resumePendingOperation()
      }
      return
    }
    when (intent.action) {
      ACTION_DISCONNECT -> disconnectGmail(intent.getStringExtra(EXTRA_EMAIL_ADDRESS))
      ACTION_CONNECT -> {
        showProgress(getString(R.string.gmail_select_account))
        requestAuthorization(AuthorizationPurpose.CONNECT, forceAccountSelection = true)
      }
      else -> startFetching()
    }
  }

  override fun onSaveInstanceState(outState: Bundle) {
    super.onSaveInstanceState(outState)
    outState.putStringArrayList(STATE_ACCOUNTS, ArrayList(accountsToFetch))
    outState.putInt(STATE_FETCH_INDEX, fetchIndex)
    outState.putString(STATE_AUTHORIZATION_EMAIL, authorizationEmail)
    outState.putString(STATE_AUTHORIZATION_PURPOSE, authorizationPurpose.name)
    outState.putBoolean(STATE_AWAITING_AUTHORIZATION, awaitingAuthorizationResult)
    outState.putBoolean(STATE_SHOWING_RESULT, showingResult)
    outState.putString(STATE_STATUS_TEXT, statusText.text.toString())
    outState.putString(STATE_RESULT_TEXT, resultText.text.toString())
    outState.putString(STATE_FETCH_OUTCOMES, encodeOutcomes())
  }

  private fun restoreState(state: Bundle) {
    accountsToFetch = state.getStringArrayList(STATE_ACCOUNTS).orEmpty()
    fetchIndex = state.getInt(STATE_FETCH_INDEX).coerceIn(0, accountsToFetch.size)
    authorizationEmail = state.getString(STATE_AUTHORIZATION_EMAIL)
    authorizationPurpose =
        runCatching {
              AuthorizationPurpose.valueOf(
                  state.getString(STATE_AUTHORIZATION_PURPOSE).orEmpty()
              )
            }
            .getOrDefault(AuthorizationPurpose.CONNECT)
    awaitingAuthorizationResult = state.getBoolean(STATE_AWAITING_AUTHORIZATION)
    showingResult = state.getBoolean(STATE_SHOWING_RESULT)
    statusText.text = state.getString(STATE_STATUS_TEXT).orEmpty()
    resultText.text = state.getString(STATE_RESULT_TEXT).orEmpty()
    decodeOutcomes(state.getString(STATE_FETCH_OUTCOMES).orEmpty())
  }

  private fun resumePendingOperation() {
    when (authorizationPurpose) {
      AuthorizationPurpose.CONNECT ->
          requestAuthorization(AuthorizationPurpose.CONNECT, forceAccountSelection = true)
      AuthorizationPurpose.FETCH_ACCOUNT ->
          requestAuthorization(AuthorizationPurpose.FETCH_ACCOUNT, authorizationEmail)
      AuthorizationPurpose.FETCH_FIRST_ACCOUNT ->
          requestAuthorization(
              AuthorizationPurpose.FETCH_FIRST_ACCOUNT,
              forceAccountSelection = true,
          )
      AuthorizationPurpose.DISCONNECT -> disconnectGmail(authorizationEmail)
    }
  }

  private fun encodeOutcomes(): String =
      JSONArray().apply {
        fetchOutcomes.forEach { outcome ->
          put(
              JSONObject()
                  .put("emailAddress", outcome.emailAddress)
                  .put("failed", outcome.failed)
                  .apply {
                    outcome.result?.let { result ->
                      put("code", result.code ?: JSONObject.NULL)
                      put("receivedAtMillis", result.receivedAtMillis ?: JSONObject.NULL)
                    }
                  }
          )
        }
      }
          .toString()

  private fun decodeOutcomes(serialized: String) {
    fetchOutcomes.clear()
    runCatching {
      val values = JSONArray(serialized)
      for (index in 0 until values.length()) {
        val value = values.optJSONObject(index) ?: continue
        val emailAddress = value.optString("emailAddress")
        val hasResult = value.has("code") || value.has("receivedAtMillis")
        val result =
            if (hasResult) {
              GmailApiClient.FetchResult(
                  emailAddress,
                  if (value.isNull("code")) null else value.optString("code"),
                  if (value.isNull("receivedAtMillis")) null
                  else value.optLong("receivedAtMillis"),
              )
            } else {
              null
            }
        fetchOutcomes.add(
            AccountFetchOutcome(
                emailAddress,
                result,
                value.optBoolean("failed"),
            )
        )
      }
    }
  }

  private fun createContent(): View {
    val container =
        LinearLayout(this).apply {
          orientation = LinearLayout.VERTICAL
          gravity = Gravity.CENTER
          setPadding(24.dp, 24.dp, 24.dp, 24.dp)
        }
    val title = TextView(this).apply {
      text = getString(R.string.gmail_otp_title)
      textSize = 22f
      gravity = Gravity.CENTER
    }
    progressIndicator = ProgressBar(this)
    statusText = TextView(this).apply { gravity = Gravity.CENTER }
    resultText = TextView(this).apply {
      gravity = Gravity.CENTER
      textSize = 18f
      visibility = View.GONE
    }
    doneButton = Button(this).apply {
      text = getString(R.string.gmail_done)
      visibility = View.GONE
      setOnClickListener { finish() }
    }
    container.addView(title, centeredLayoutParams(bottomMargin = 24))
    container.addView(progressIndicator, centeredLayoutParams(bottomMargin = 16))
    container.addView(statusText, centeredLayoutParams(bottomMargin = 16))
    container.addView(resultText, centeredLayoutParams(bottomMargin = 16))
    container.addView(doneButton, centeredLayoutParams())
    return container
  }

  private fun startFetching() {
    accountsToFetch = accountStore.getAccounts()
    fetchIndex = 0
    fetchOutcomes.clear()
    if (accountsToFetch.isEmpty()) {
      showProgress(getString(R.string.gmail_select_account))
      requestAuthorization(AuthorizationPurpose.FETCH_FIRST_ACCOUNT, forceAccountSelection = true)
    } else {
      authorizeNextAccount()
    }
  }

  private fun authorizeNextAccount() {
    if (fetchIndex >= accountsToFetch.size) {
      finishFetching()
      return
    }
    val email = accountsToFetch[fetchIndex]
    showProgress(
        getString(
            R.string.gmail_checking_accounts,
            fetchIndex + 1,
            accountsToFetch.size,
            email,
        )
    )
    requestAuthorization(AuthorizationPurpose.FETCH_ACCOUNT, email)
  }

  private fun requestAuthorization(
      purpose: AuthorizationPurpose,
      emailAddress: String? = null,
      forceAccountSelection: Boolean = false,
  ) {
    authorizationPurpose = purpose
    authorizationEmail = emailAddress
    awaitingAuthorizationResult = false
    val requestBuilder =
        AuthorizationRequest.builder()
            .setRequestedScopes(listOf(Scope(GmailApiClient.GMAIL_READONLY_SCOPE)))
    if (emailAddress != null) {
      requestBuilder.setAccount(Account(emailAddress, GOOGLE_ACCOUNT_TYPE))
    }
    if (forceAccountSelection) {
      requestBuilder.setPrompt(AuthorizationRequest.Prompt.SELECT_ACCOUNT)
    }
    authorizationClient
        .authorize(requestBuilder.build())
        .addOnSuccessListener(::handleAuthorization)
        .addOnFailureListener { handleAuthorizationFailure() }
  }

  private fun handleAuthorization(
      result: com.google.android.gms.auth.api.identity.AuthorizationResult
  ) {
    if (result.hasResolution()) {
      val pendingIntent = result.pendingIntent ?: run {
        handleAuthorizationFailure()
        return
      }
      awaitingAuthorizationResult = true
      authorizationLauncher.launch(
          androidx.activity.result.IntentSenderRequest.Builder(pendingIntent.intentSender).build()
      )
      return
    }
    awaitingAuthorizationResult = false

    val accessToken = result.accessToken
    if (accessToken.isNullOrBlank()) {
      handleAuthorizationFailure()
      return
    }
    when (authorizationPurpose) {
      AuthorizationPurpose.CONNECT -> connect(accessToken)
      AuthorizationPurpose.FETCH_ACCOUNT -> fetchAccount(accessToken, authorizationEmail)
      AuthorizationPurpose.FETCH_FIRST_ACCOUNT -> fetchAccount(accessToken, null)
      AuthorizationPurpose.DISCONNECT -> disconnectGmail(authorizationEmail)
    }
  }

  private fun handleAuthorizationFailure() {
    when (authorizationPurpose) {
      AuthorizationPurpose.CONNECT -> showResult(getString(R.string.gmail_authorization_cancelled))
      AuthorizationPurpose.FETCH_ACCOUNT -> {
        fetchOutcomes.add(AccountFetchOutcome(authorizationEmail.orEmpty(), failed = true))
        fetchIndex++
        authorizeNextAccount()
      }
      AuthorizationPurpose.FETCH_FIRST_ACCOUNT ->
          showResult(getString(R.string.gmail_authorization_cancelled))
      AuthorizationPurpose.DISCONNECT ->
          showResult(getString(R.string.gmail_disconnect_failed))
    }
  }

  private fun connect(accessToken: String) {
    lifecycleScope.launch {
      try {
        val emailAddress =
            kotlinx.coroutines.withContext(Dispatchers.IO) {
              GmailApiClient(GmailCodeExtractor()).getEmailAddress(accessToken)
            }
        if (emailAddress.isBlank()) throw IllegalStateException("No Gmail account address returned")
        if (!accountStore.addAccount(emailAddress)) {
          showResult(getString(R.string.gmail_account_save_failed))
          return@launch
        }
        showResult(getString(R.string.gmail_connected_success))
      } catch (error: Exception) {
        showResult(getString(R.string.gmail_fetch_failed))
      } finally {
        clearToken(accessToken)
      }
    }
  }

  private fun fetchAccount(accessToken: String, expectedEmail: String?) {
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
        val emailAddress = result.emailAddress.ifBlank { expectedEmail.orEmpty() }
        if (emailAddress.isBlank()) throw IllegalStateException("No Gmail account address returned")
        val accountSaved = expectedEmail != null || accountStore.addAccount(emailAddress)
        fetchOutcomes.add(
            AccountFetchOutcome(
                emailAddress,
                result.copy(emailAddress = emailAddress),
                failed = !accountSaved,
            )
        )
      } catch (error: Exception) {
        fetchOutcomes.add(
            AccountFetchOutcome(
                expectedEmail ?: getString(R.string.gmail_selected_account),
                failed = true,
            )
        )
      } finally {
        clearToken(accessToken)
      }

      if (expectedEmail == null) {
        accountsToFetch = accountStore.getAccounts()
        finishFetching()
      } else {
        fetchIndex++
        authorizeNextAccount()
      }
    }
  }

  private fun finishFetching() {
    val summary = GmailFetchCoordinator.summarize(fetchOutcomes)
    val newestResult = summary.newestResult
    if (newestResult == null) {
      val message =
          if (summary.failedAccountEmails.isEmpty()) {
            getString(R.string.gmail_no_code_found)
          } else {
            getString(
                R.string.gmail_no_code_accounts_failed,
                summary.failedAccountEmails.joinToString(),
            )
          }
      showResult(message)
      return
    }
    val code = newestResult.code ?: return showResult(getString(R.string.gmail_no_code_found))

    val data =
        workDataOf(
            "packageName" to BuildConfig.APPLICATION_ID,
            "notificationId" to "gmail-fetch",
            "notificationTag" to "gmail",
            "text" to getString(R.string.gmail_history_entry, code),
            "code" to code,
            "is_gmail" to true,
        )
    val work = OneTimeWorkRequestBuilder<CodeDetectedWorker>().setInputData(data).build()
    WorkManager.getInstance(applicationContext).enqueue(work)
    val resultMessage =
        if (summary.failedAccountEmails.isEmpty()) {
          getString(R.string.gmail_code_source, code, newestResult.emailAddress)
        } else {
          getString(
              R.string.gmail_code_source_partial,
              code,
              newestResult.emailAddress,
              summary.failedAccountEmails.joinToString(),
          )
        }
    showResult(resultMessage)
  }

  private fun disconnectGmail(emailAddress: String?) {
    if (emailAddress.isNullOrBlank()) {
      showResult(getString(R.string.gmail_disconnect_failed))
      return
    }
    authorizationPurpose = AuthorizationPurpose.DISCONNECT
    authorizationEmail = emailAddress
    showProgress(getString(R.string.gmail_disconnect_in_progress))
    val request =
        RevokeAccessRequest.builder()
            .setAccount(Account(emailAddress, GOOGLE_ACCOUNT_TYPE))
            .setScopes(listOf(Scope(GmailApiClient.GMAIL_READONLY_SCOPE)))
            .build()
    authorizationClient
        .revokeAccess(request)
        .addOnSuccessListener {
          if (accountStore.removeAccount(emailAddress)) {
            finish()
          } else {
            showResult(getString(R.string.gmail_account_remove_failed))
          }
        }
        .addOnFailureListener { showResult(getString(R.string.gmail_disconnect_failed)) }
  }

  private fun clearToken(accessToken: String) {
    runCatching {
      authorizationClient.clearToken(ClearTokenRequest.builder().setToken(accessToken).build())
    }
  }

  private fun showProgress(message: String) {
    showingResult = false
    progressIndicator.visibility = View.VISIBLE
    resultText.visibility = View.GONE
    doneButton.visibility = View.GONE
    statusText.visibility = View.VISIBLE
    statusText.text = message
  }

  private fun showResult(message: String) {
    showingResult = true
    progressIndicator.visibility = View.GONE
    statusText.visibility = View.GONE
    resultText.visibility = View.VISIBLE
    resultText.text = message
    doneButton.visibility = View.VISIBLE
  }

  private fun centeredLayoutParams(bottomMargin: Int = 0) =
      LinearLayout.LayoutParams(
          LinearLayout.LayoutParams.MATCH_PARENT,
          LinearLayout.LayoutParams.WRAP_CONTENT,
      ).apply {
        gravity = Gravity.CENTER
        if (bottomMargin > 0) this.bottomMargin = bottomMargin.dp
      }

  private val Int.dp: Int
    get() = (this * resources.displayMetrics.density).toInt()

  private enum class AuthorizationPurpose {
    CONNECT,
    FETCH_ACCOUNT,
    FETCH_FIRST_ACCOUNT,
    DISCONNECT,
  }

  companion object {
    const val PREFS_NAME = "gmail_connection"
    const val PREF_EMAIL = "connected_email"
    const val EXTRA_EMAIL_ADDRESS = "email_address"
    const val ACTION_FETCH = "com.dumbshrn.otphelper.action.FETCH_GMAIL_OTP"
    const val ACTION_CONNECT = "com.dumbshrn.otphelper.action.CONNECT_GMAIL"
    const val ACTION_DISCONNECT = "com.dumbshrn.otphelper.action.DISCONNECT_GMAIL"
    private const val GOOGLE_ACCOUNT_TYPE = "com.google"
    private const val STATE_ACCOUNTS = "gmail_accounts"
    private const val STATE_FETCH_INDEX = "gmail_fetch_index"
    private const val STATE_AUTHORIZATION_EMAIL = "gmail_authorization_email"
    private const val STATE_AUTHORIZATION_PURPOSE = "gmail_authorization_purpose"
    private const val STATE_AWAITING_AUTHORIZATION = "gmail_awaiting_authorization"
    private const val STATE_SHOWING_RESULT = "gmail_showing_result"
    private const val STATE_STATUS_TEXT = "gmail_status_text"
    private const val STATE_RESULT_TEXT = "gmail_result_text"
    private const val STATE_FETCH_OUTCOMES = "gmail_fetch_outcomes"
  }
}
