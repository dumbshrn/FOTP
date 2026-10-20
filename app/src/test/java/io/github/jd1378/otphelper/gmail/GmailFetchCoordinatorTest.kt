package io.github.jd1378.otphelper.gmail

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GmailFetchCoordinatorTest {
  @Test
  fun choosesCandidateWithNewestReceivedTimestampAcrossAccounts() {
    val summary =
        GmailFetchCoordinator.summarize(
            listOf(
                AccountFetchOutcome("one@gmail.com", result("one@gmail.com", "123456", 100)),
                AccountFetchOutcome("two@gmail.com", result("two@gmail.com", "654321", 200)),
            )
        )

    assertEquals("654321", summary.newestResult?.code)
    assertEquals("two@gmail.com", summary.newestResult?.emailAddress)
  }

  @Test
  fun ignoresResultsWithoutCodeOrTimestamp() {
    val summary =
        GmailFetchCoordinator.summarize(
            listOf(
                AccountFetchOutcome("one@gmail.com", result("one@gmail.com", "123456", 100)),
                AccountFetchOutcome("two@gmail.com", result("two@gmail.com", "999999", null)),
                AccountFetchOutcome("three@gmail.com", result("three@gmail.com", null, 300)),
            )
        )

    assertEquals("123456", summary.newestResult?.code)
  }

  @Test
  fun preservesFirstCandidateWhenReceivedTimestampsTie() {
    val summary =
        GmailFetchCoordinator.summarize(
            listOf(
                AccountFetchOutcome("one@gmail.com", result("one@gmail.com", "123456", 100)),
                AccountFetchOutcome("two@gmail.com", result("two@gmail.com", "654321", 100)),
            )
        )

    assertEquals("one@gmail.com", summary.newestResult?.emailAddress)
  }

  @Test
  fun keepsSuccessfulCandidateWhenAnotherAccountFails() {
    val summary =
        GmailFetchCoordinator.summarize(
            listOf(
                AccountFetchOutcome("one@gmail.com", failed = true),
                AccountFetchOutcome("two@gmail.com", result("two@gmail.com", "654321", 200)),
            )
        )

    assertEquals("654321", summary.newestResult?.code)
    assertEquals(listOf("one@gmail.com"), summary.failedAccountEmails)
  }

  @Test
  fun returnsNoCandidateWhenEveryAccountHasNoCode() {
    val summary =
        GmailFetchCoordinator.summarize(
            listOf(AccountFetchOutcome("one@gmail.com", result("one@gmail.com", null, null)))
        )

    assertNull(summary.newestResult)
  }

  private fun result(email: String, code: String?, receivedAtMillis: Long?) =
      GmailApiClient.FetchResult(email, code, receivedAtMillis)
}
