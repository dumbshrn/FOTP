package io.github.jd1378.otphelper.gmail

import java.io.ByteArrayInputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.Base64
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.runBlocking
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
  fun requestsRecentInboxMessagesInHundredMessagePages() {
    assertEquals(100, GmailApiClient.MAX_MESSAGES)
    assertEquals("labelIds=INBOX&maxResults=100&q=newer_than%3A7d", GmailApiClient.INBOX_QUERY)
    assertEquals(
        "https://www.googleapis.com/auth/gmail.readonly",
        GmailApiClient.GMAIL_READONLY_SCOPE,
    )
  }

  @Test
  fun paginatesRecentMessagesUntilItFindsTheNewestCode() = runBlocking {
    val responses =
        mapOf(
            "/gmail/v1/users/me/profile" to """{"emailAddress":"one@gmail.com"}""",
            "/gmail/v1/users/me/messages" to
                """{"messages":[{"id":"newest-no-code"}],"nextPageToken":"older-page"}""",
            "/gmail/v1/users/me/messages?pageToken=older-page" to
                """{"messages":[{"id":"older-code"}]}""",
            "/gmail/v1/users/me/messages/newest-no-code" to
                message("300", "Your account update is complete"),
            "/gmail/v1/users/me/messages/older-code" to
                message("200", "Your verification code is 654321"),
        )
    val apiClient =
        GmailApiClient(
            GmailCodeExtractor(),
            openConnection = { url ->
              val key =
                  if (url.query.orEmpty().contains("pageToken=older-page")) {
                    "${url.path}?pageToken=older-page"
                  } else {
                    url.path
                  }
              FakeHttpURLConnection(url, responses.getValue(key))
            },
            decodeBase64Url = { Base64.getUrlDecoder().decode(it) },
        )

    val result = apiClient.fetchLatestOtp("token")

    assertEquals("654321", result.code)
    assertEquals(200L, result.receivedAtMillis)
  }

  @Test
  fun requestsMessagesConcurrentlyWithinEachPage() = runBlocking {
    val messageCount = 4
    val remainingMessages = AtomicInteger(messageCount)
    val allMessagesStarted = CountDownLatch(messageCount)
    val responses =
        mapOf(
            "/gmail/v1/users/me/profile" to """{"emailAddress":"one@gmail.com"}""",
            "/gmail/v1/users/me/messages" to
                """{"messages":[{"id":"one"},{"id":"two"},{"id":"three"},{"id":"four"}]}""",
        )
    val apiClient =
        GmailApiClient(
            GmailCodeExtractor(),
            openConnection = { url ->
              val response =
                  responses[url.path]
                      ?: run {
                        allMessagesStarted.countDown()
                        check(allMessagesStarted.await(2, TimeUnit.SECONDS))
                        message("${remainingMessages.decrementAndGet()}", "No verification code here")
                      }
              FakeHttpURLConnection(url, response)
            },
            decodeBase64Url = { Base64.getUrlDecoder().decode(it) },
        )

    val result = apiClient.fetchLatestOtp("token")

    assertNull(result.code)
    assertEquals(0L, allMessagesStarted.count)
  }

  @Test
  fun selectsNewestMessageThatContainsCodeInsteadOfFirstListedMessage() = runBlocking {
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
  fun returnsNoTimestampWhenNoMessageContainsCode() = runBlocking {
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
  fun ignoresCodeMessagesWithoutValidReceivedTimestamp() = runBlocking {
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
