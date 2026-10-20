package io.github.jd1378.otphelper.gmail

import java.io.ByteArrayInputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.Base64
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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

  @Test
  fun selectsNewestMessageThatContainsCodeInsteadOfFirstListedMessage() {
    val responses =
        mapOf(
            "/gmail/v1/users/me/profile" to """{"emailAddress":"one@gmail.com"}""",
            "/gmail/v1/users/me/messages" to
                """{"messages":[{"id":"no-code"},{"id":"older-code"},{"id":"newer-code"}]}""",
            "/gmail/v1/users/me/messages/no-code" to
                message("100", "Your account update is complete"),
            "/gmail/v1/users/me/messages/older-code" to
                message("200", "Your verification code is 123456"),
            "/gmail/v1/users/me/messages/newer-code" to
                message("300", "Your verification code is 654321"),
        )
    val apiClient =
        GmailApiClient(
            GmailCodeExtractor(),
            openConnection = { url -> FakeHttpURLConnection(url, responses.getValue(url.path)) },
            decodeBase64Url = { Base64.getUrlDecoder().decode(it) },
        )

    val result = apiClient.fetchLatestOtp("token")

    assertEquals("one@gmail.com", result.emailAddress)
    assertEquals("654321", result.code)
    assertEquals(300L, result.receivedAtMillis)
  }

  @Test
  fun returnsNoTimestampWhenNoMessageContainsCode() {
    val responses =
        mapOf(
            "/gmail/v1/users/me/profile" to """{"emailAddress":"one@gmail.com"}""",
            "/gmail/v1/users/me/messages" to """{"messages":[{"id":"no-code"}]}""",
            "/gmail/v1/users/me/messages/no-code" to
                message("100", "Your account update is complete"),
        )
    val apiClient =
        GmailApiClient(
            GmailCodeExtractor(),
            openConnection = { url -> FakeHttpURLConnection(url, responses.getValue(url.path)) },
            decodeBase64Url = { Base64.getUrlDecoder().decode(it) },
        )

    val result = apiClient.fetchLatestOtp("token")

    assertNull(result.code)
    assertNull(result.receivedAtMillis)
  }

  @Test
  fun ignoresCodeMessagesWithoutValidReceivedTimestamp() {
    val responses =
        mapOf(
            "/gmail/v1/users/me/profile" to """{"emailAddress":"one@gmail.com"}""",
            "/gmail/v1/users/me/messages" to
                """{"messages":[{"id":"missing-date"},{"id":"invalid-date"}]}""",
            "/gmail/v1/users/me/messages/missing-date" to
                message(null, "Your verification code is 123456"),
            "/gmail/v1/users/me/messages/invalid-date" to
                message("not-a-date", "Your verification code is 654321"),
        )
    val apiClient =
        GmailApiClient(
            GmailCodeExtractor(),
            openConnection = { url -> FakeHttpURLConnection(url, responses.getValue(url.path)) },
            decodeBase64Url = { Base64.getUrlDecoder().decode(it) },
        )

    val result = apiClient.fetchLatestOtp("token")

    assertNull(result.code)
    assertNull(result.receivedAtMillis)
  }

  private fun message(internalDate: String?, body: String): String {
    val dateField = internalDate?.let { "\"internalDate\":\"$it\"," }.orEmpty()
    return """{$dateField"payload":{"mimeType":"text/plain","body":{"data":"${encoded(body)}"}}}"""
  }

  private fun encoded(body: String): String =
      Base64.getUrlEncoder().withoutPadding().encodeToString(body.toByteArray())

  private class FakeHttpURLConnection(
    url: URL,
    private val responseBody: String,
  ) : HttpURLConnection(url) {
    override fun disconnect() = Unit

    override fun usingProxy(): Boolean = false

    override fun connect() = Unit

    override fun getResponseCode(): Int = HTTP_OK

    override fun getInputStream() = ByteArrayInputStream(responseBody.toByteArray())
  }
}
