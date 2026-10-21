package io.github.jd1378.otphelper

import androidx.work.WorkInfo
import io.github.jd1378.otphelper.ui.screens.isGmailFetchActive
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GmailFetchUiStateTest {
  @Test
  fun fetchIsActiveWhileEnqueuedBlockedOrRunning() {
    assertTrue(isGmailFetchActive(listOf(WorkInfo.State.ENQUEUED)))
    assertTrue(isGmailFetchActive(listOf(WorkInfo.State.BLOCKED)))
    assertTrue(isGmailFetchActive(listOf(WorkInfo.State.RUNNING)))
  }

  @Test
  fun fetchIsInactiveAfterWorkerFinishes() {
    assertFalse(isGmailFetchActive(listOf(WorkInfo.State.SUCCEEDED)))
    assertFalse(isGmailFetchActive(listOf(WorkInfo.State.FAILED)))
    assertFalse(isGmailFetchActive(emptyList()))
  }
}
