package io.github.jd1378.otphelper.gmail

import io.github.jd1378.otphelper.utils.CodeExtractor
import org.junit.Assert.assertEquals
import org.junit.Test

class GmailCodeExtractorTest {
  @Test
  fun ignoresCodexDateWithoutTreatingItAsAnOtp() {
    val message = "Your Codex update was sent on 2026-10-20."

    assertEquals(null, GmailCodeExtractor().extract(message))
  }

  @Test
  fun codexIsIgnoredEvenWhenUserRemovedTheDefaultIgnorePhrase() {
    val extractor = GmailCodeExtractor(CodeExtractor(ignoredPhrases = emptyList()))

    assertEquals(null, extractor.extract("Codex update: your code is 2026-10-20"))
  }

  @Test
  fun ignoresCodexEvenWhenTheUserRemovedTheDefaultIgnorePhrase() {
    val extractor = GmailCodeExtractor(CodeExtractor(ignoredPhrases = emptyList()))

    assertEquals(null, extractor.extract("Codex update: your OTP is 483921"))
  }

  @Test
  fun extractsOtpWhenMessageAlsoContainsDate() {
    val message = "Your verification code is 483921. This link expires on 2026-10-20."

    assertEquals("483921", GmailCodeExtractor().extract(message))
  }
}
