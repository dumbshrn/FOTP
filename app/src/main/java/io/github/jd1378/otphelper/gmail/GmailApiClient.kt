package io.github.jd1378.otphelper.gmail

import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import org.json.JSONObject

class GmailApiClient(
  private val codeExtractor: GmailCodeExtractor,
  private val openConnection: (URL) -> HttpURLConnection = { it.openConnection() as HttpURLConnection },
  private val decodeBase64Url: (String) -> ByteArray = {
    android.util.Base64.decode(
        it,
        android.util.Base64.URL_SAFE or android.util.Base64.NO_WRAP or android.util.Base64.NO_PADDING,
    )
  },
) {
  data class FetchResult(val emailAddress: String, val code: String?)

  fun fetchLatestOtp(accessToken: String): FetchResult {
    val deadlineNanos = System.nanoTime() + MAX_FETCH_DURATION_MS * NANOS_PER_MILLISECOND
    val emailAddress = getEmailAddress(accessToken, deadlineNanos)
    val messages =
        request("users/me/messages?$INBOX_QUERY", accessToken, deadlineNanos)
            .optJSONArray("messages")
    var code: String? = null

    if (messages != null) {
      for (index in 0 until messages.length()) {
        val messageId =
            messages.optJSONObject(index)?.optString("id")?.takeIf(String::isNotBlank) ?: continue
        val payload =
            request("users/me/messages/$messageId?format=full", accessToken, deadlineNanos)
                .optJSONObject("payload") ?: continue
        val messageText = decodeMessage(payload)
        code = codeExtractor.extract(messageText)
        if (code != null) break
      }
    }

    return FetchResult(emailAddress, code)
  }

  fun getEmailAddress(accessToken: String): String =
      getEmailAddress(
          accessToken,
          System.nanoTime() + MAX_FETCH_DURATION_MS * NANOS_PER_MILLISECOND,
      )

  private fun getEmailAddress(accessToken: String, deadlineNanos: Long): String =
      request("users/me/profile", accessToken, deadlineNanos).optString("emailAddress")

  private fun request(path: String, accessToken: String, deadlineNanos: Long): JSONObject {
    val remainingMillis = ((deadlineNanos - System.nanoTime()) / NANOS_PER_MILLISECOND).toInt()
    if (remainingMillis <= 0) throw SocketTimeoutException("Gmail fetch exceeded its time limit")

    val connection = openConnection(URL("$GMAIL_API/$path"))
    try {
      connection.requestMethod = "GET"
      connection.connectTimeout = minOf(CONNECT_TIMEOUT_MS, remainingMillis)
      connection.readTimeout = minOf(READ_TIMEOUT_MS, remainingMillis)
      connection.setRequestProperty("Authorization", "Bearer $accessToken")
      connection.setRequestProperty("Accept", "application/json")
      val status = connection.responseCode
      val stream =
          (if (status in 200..299) connection.inputStream else connection.errorStream)
              ?: throw IOException("Gmail API returned an empty response")
      val body = stream.use(::readBoundedResponse)
      if (status !in 200..299) throw IllegalStateException("Gmail API returned HTTP $status")
      return JSONObject(body)
    } finally {
      connection.disconnect()
    }
  }

  private fun readBoundedResponse(stream: InputStream): String {
    val output = ByteArrayOutputStream()
    stream.use { input ->
      val buffer = ByteArray(8192)
      var totalBytes = 0
      while (true) {
        val count = input.read(buffer)
        if (count < 0) break
        totalBytes += count
        if (totalBytes > MAX_RESPONSE_BYTES) throw IOException("Gmail response exceeded its size limit")
        output.write(buffer, 0, count)
      }
    }
    return output.toString(Charsets.UTF_8.name())
  }

  internal fun decodeMimePayload(payload: JSONObject): String {
    if (payload.optString("filename").isNotBlank()) return ""
    val mimeType = payload.optString("mimeType")
    val output = StringBuilder()
    val bodyData = payload.optJSONObject("body")?.optString("data").orEmpty()
    if (bodyData.isNotEmpty() && (mimeType == "text/plain" || mimeType == "text/html")) {
      val decoded = String(decodeBase64Url(bodyData), Charsets.UTF_8)
      output.append(if (mimeType == "text/html") htmlToText(decoded) else decoded).append('\n')
    }

    val parts = payload.optJSONArray("parts")
    if (parts != null) {
      for (index in 0 until parts.length()) {
        val part = parts.optJSONObject(index) ?: continue
        output.append(decodeMimePayload(part)).append('\n')
      }
    }
    return output.toString()
  }

  internal fun decodeMessage(payload: JSONObject): String {
    val headers = payload.optJSONArray("headers")
    var subject = ""
    if (headers != null) {
      for (index in 0 until headers.length()) {
        val header = headers.optJSONObject(index) ?: continue
        if (header.optString("name").equals("subject", ignoreCase = true)) {
          subject = header.optString("value")
          break
        }
      }
    }
    return "$subject\n${decodeMimePayload(payload)}"
  }

  private fun htmlToText(html: String): String =
      html
          .replace(Regex("(?is)<(script|style)[^>]*>.*?</\\1>"), " ")
          .replace(Regex("(?i)<br\\s*/?>|</(p|div|tr|li)\\s*>"), "\n")
          .replace(Regex("<[^>]+>"), " ")
          .replace("&nbsp;", " ")
          .replace("&amp;", "&")
          .replace("&lt;", "<")
          .replace("&gt;", ">")
          .replace("&quot;", "\"")
          .replace("&#39;", "'")

  companion object {
    const val MAX_MESSAGES = 10
    const val GMAIL_READONLY_SCOPE = "https://www.googleapis.com/auth/gmail.readonly"
    const val INBOX_QUERY = "labelIds=INBOX&maxResults=$MAX_MESSAGES&q=newer_than%3A7d"
    const val MAX_FETCH_DURATION_MS = 30_000L
    private const val CONNECT_TIMEOUT_MS = 5_000
    private const val READ_TIMEOUT_MS = 8_000
    private const val MAX_RESPONSE_BYTES = 1_000_000
    private const val NANOS_PER_MILLISECOND = 1_000_000L
    private const val GMAIL_API = "https://gmail.googleapis.com/gmail/v1"
  }
}
