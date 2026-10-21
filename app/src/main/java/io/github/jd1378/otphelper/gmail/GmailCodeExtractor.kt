package io.github.jd1378.otphelper.gmail

import io.github.jd1378.otphelper.utils.CodeExtractor

class GmailCodeExtractor(
  private val codeExtractor: CodeExtractor = CodeExtractor(),
) {
  fun extract(message: String): String? {
    if (
        codeExtractor.shouldIgnore(message) ||
            CODEX_WORD.containsMatchIn(message) ||
            TRANSIT_TICKET_FIELD.containsMatchIn(message)
    ) {
      return null
    }

    val safeMessage = ISO_DATE.replace(CODEX_WORD.replace(message, " "), " ")
    val lines = codeExtractor.cleanup(safeMessage).lineSequence().map(String::trim).toList()
    lines.forEachIndexed { index, line ->
      if (line.isEmpty()) return@forEachIndexed
      codeExtractor.getCode(line, doCleanup = false)?.let { return it }
      if (STANDALONE_OTP_LABEL.matches(line)) {
        val nextLine = lines.drop(index + 1).firstOrNull(String::isNotEmpty) ?: return@forEachIndexed
        codeExtractor.getCode("$line $nextLine", doCleanup = false)?.let { return it }
      }
    }
    return null
  }

  private companion object {
    val CODEX_WORD =
        Regex("(?<![\\p{L}\\p{N}_])codex(?![\\p{L}\\p{N}_])", RegexOption.IGNORE_CASE)
    val ISO_DATE = Regex("\\b(?:19|20)\\d{2}[-/.]\\d{1,2}[-/.]\\d{1,2}\\b")
    val TRANSIT_TICKET_FIELD =
        Regex(
            "\\b(?:PNR\\s*:|Ticket\\s*(?:No\\.?|Number)\\s*:|Trip\\s*Code\\s*:|Bus\\s*Type\\s*:)",
            RegexOption.IGNORE_CASE,
        )
    val STANDALONE_OTP_LABEL =
        Regex(
            "(?:your\\s+)?(?:(?:verification|login|security|authentication|one[-\\s]?time)\\s+)?(?:code|otp|2fa|password)(?:\\s+is)?\\s*:?\\s*",
            RegexOption.IGNORE_CASE,
        )
  }
}
