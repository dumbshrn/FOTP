package io.github.jd1378.otphelper.gmail

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
