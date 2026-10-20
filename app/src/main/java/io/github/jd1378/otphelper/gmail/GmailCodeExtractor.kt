package io.github.jd1378.otphelper.gmail

import io.github.jd1378.otphelper.utils.CodeExtractor

class GmailCodeExtractor(
  private val codeExtractor: CodeExtractor = CodeExtractor(),
) {
  fun extract(message: String): String? {
    if (codeExtractor.shouldIgnore(message) || CODEX_WORD.containsMatchIn(message)) return null

    val safeMessage = ISO_DATE.replace(CODEX_WORD.replace(message, " "), " ")
    return codeExtractor.getCode(codeExtractor.cleanup(safeMessage), doCleanup = false)
  }

  private companion object {
    val CODEX_WORD =
        Regex("(?<![\\p{L}\\p{N}_])codex(?![\\p{L}\\p{N}_])", RegexOption.IGNORE_CASE)
    val ISO_DATE = Regex("\\b(?:19|20)\\d{2}[-/.]\\d{1,2}[-/.]\\d{1,2}\\b")
  }
}
