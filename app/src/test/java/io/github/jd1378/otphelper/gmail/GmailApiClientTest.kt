package io.github.jd1378.otphelper.gmail

import java.util.Base64
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GmailApiClientTest {
  private val client =
      GmailApiClient(GmailCodeExtractor(), decodeBase64Url = { Base64.getUrlDecoder().decode(it) })

  @Test
  fun decodesNestedPlainTextParts() {
    val encoded =
        Base64.getUrlEncoder().withoutPadding().encodeToString("Your OTP is 483921".toByteArray())
    val payload =
        JSONObject(
            """{"mimeType":"multipart/alternative","parts":[{"mimeType":"text/plain","body":{"data":"$encoded"}}]}"""
        )

    assertTrue(client.decodeMimePayload(payload).contains("Your OTP is 483921"))
  }

  @Test
  fun decodesHtmlAndRemovesMarkup() {
    val encoded =
        Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString("<p>Your code is <b>483921</b></p>".toByteArray())
    val payload = JSONObject("""{"mimeType":"text/html","body":{"data":"$encoded"}}""")

    assertTrue(client.decodeMimePayload(payload).contains("Your code is  483921"))
  }

  @Test
  fun includesSubjectWhenScanningForOtp() {
    val payload =
        JSONObject(
            """{"mimeType":"text/plain","headers":[{"name":"Subject","value":"Your login code is 483921"}],"body":{"data":""}}"""
        )

    assertTrue(client.decodeMessage(payload).contains("Your login code is 483921"))
  }

  @Test
  fun capsInboxQueryToRecentMessages() {
    assertEquals(10, GmailApiClient.MAX_MESSAGES)
    assertEquals("labelIds=INBOX&maxResults=10&q=newer_than%3A7d", GmailApiClient.INBOX_QUERY)
    assertEquals("https://www.googleapis.com/auth/gmail.readonly", GmailApiClient.GMAIL_READONLY_SCOPE)
  }
}
