package org.regshield.ca.data

import org.junit.Assert.assertEquals
import org.junit.Test

class AskThreadsMetaTest {
  @Test
  fun buildAskThreadMeta_derivesTitleAndPreview() {
    val msgs =
      listOf(
        AskStoredMessage(role = "user", content = "  Hello   world  ", ts = 10L),
        AskStoredMessage(role = "assistant", content = "Answer\n\nline", ts = 20L),
      )

    val meta = buildAskThreadMeta(threadId = "t1", existingTitle = null, messages = msgs, nowMs = 1000L)
    assertEquals("t1", meta.id)
    assertEquals("Hello world", meta.title)
    assertEquals(10L, meta.createdAt)
    assertEquals(20L, meta.updatedAt)
    assertEquals(2, meta.messageCount)
    assertEquals("Answer line", meta.lastPreview)
  }

  @Test
  fun buildAskThreadMeta_prefersExistingTitle() {
    val msgs =
      listOf(
        AskStoredMessage(role = "user", content = "Question", ts = 10L),
        AskStoredMessage(role = "assistant", content = "Answer", ts = 20L),
      )

    val meta = buildAskThreadMeta(threadId = "t2", existingTitle = "Custom", messages = msgs, nowMs = 1000L)
    assertEquals("Custom", meta.title)
  }
}

