package io.github.jd1378.otphelper.gmail

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.util.concurrent.atomic.AtomicInteger

data class AccountFetchOutcome(
  val emailAddress: String,
  val result: GmailApiClient.FetchResult? = null,
  val failed: Boolean = false,
)

data class GmailFetchSummary(
  val newestResult: GmailApiClient.FetchResult?,
  val failedAccountEmails: List<String>,
)

object GmailFetchCoordinator {
  suspend fun fetchAccounts(
      accounts: List<String>,
      maxConcurrent: Int,
      onAccountCompleted: (completedCount: Int, emailAddress: String) -> Unit = { _, _ -> },
      fetchAccount: suspend (emailAddress: String) -> AccountFetchOutcome,
  ): List<AccountFetchOutcome> = coroutineScope {
    require(maxConcurrent > 0)
    val semaphore = Semaphore(maxConcurrent)
    val completedCount = AtomicInteger()
    accounts
        .map { emailAddress ->
          async {
            val outcome =
                try {
                  semaphore.withPermit { fetchAccount(emailAddress) }
                } catch (error: CancellationException) {
                  throw error
                } catch (error: Exception) {
                  AccountFetchOutcome(emailAddress, failed = true)
                }
            onAccountCompleted(completedCount.incrementAndGet(), emailAddress)
            outcome
          }
        }
        .awaitAll()
  }

  fun summarize(outcomes: List<AccountFetchOutcome>): GmailFetchSummary {
    var newestResult: GmailApiClient.FetchResult? = null
    for (outcome in outcomes) {
      val candidate = outcome.result ?: continue
      val receivedAtMillis = candidate.receivedAtMillis ?: continue
      if (candidate.code.isNullOrBlank()) continue
      val newestTimestamp = newestResult?.receivedAtMillis
      if (newestTimestamp == null || receivedAtMillis > newestTimestamp) {
        newestResult = candidate
      }
    }
    return GmailFetchSummary(
        newestResult,
        outcomes.filter(AccountFetchOutcome::failed).map(AccountFetchOutcome::emailAddress),
    )
  }
}
