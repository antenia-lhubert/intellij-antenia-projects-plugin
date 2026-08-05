package fr.antenia.commit

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CommitMessageValidatorTest {
    @Test
    fun `accepts every specified commit format`() {
        val messages = listOf(
            "[EVO] - Mantis : 123 : Add customer export",
            "[BUG] - Mantis : 456 : Fix invoice calculation",
            "[BUG_TRANSVERSAL] - Mantis : 789 : Fix shared date parsing",
            "[STRUCT] - Mantis : 12 : Reorganize persistence module",
            "[CODE_REVIEW] - Apply review feedback",
            "[MERGE] r222180 | lhubert | 2026-08-05 15:13:00 CEST",
        )

        messages.forEach { message ->
            assertTrue("Expected valid message: $message", CommitMessageValidator.validate(message).isValid)
        }
    }

    @Test
    fun `validates the format on the first line and counts the complete message`() {
        val result = CommitMessageValidator.validate(
            "[BUG] - Mantis : 456 : Fix invoice calculation\n\n> Detailed implementation message",
        )

        assertTrue(result.isValid)
        assertEquals(81, result.characterCount)
    }

    @Test
    fun `rejects placeholders invalid ticket numbers and unknown formats`() {
        val messages = listOf(
            "[EVO] - Mantis : MANTIS_NO : TITRE",
            "[BUG] - Mantis : 0 : Fix invoice calculation",
            "[BUG] - Mantis : -1 : Fix invoice calculation",
            "[MERGE] rREVISION | AUTHOR | DATE",
            "[FEATURE] - A long but unsupported commit message",
        )

        messages.forEach { message ->
            assertFalse("Expected invalid message: $message", CommitMessageValidator.validate(message).isValid)
        }
    }

    @Test
    fun `uses the complete message for the twenty five character minimum`() {
        val result = CommitMessageValidator.validate("[CODE_REVIEW] - Short")
        val resultWithBody = CommitMessageValidator.validate("[CODE_REVIEW] - Short\n\n> Details")

        assertFalse(result.isValid)
        assertEquals(21, result.characterCount)
        assertTrue(resultWithBody.isValid)
    }

    @Test
    fun `provides all built in templates`() {
        val defaults = CommitTemplates.defaults()

        assertEquals(
            listOf("Evolution", "Bug", "Transversal bug", "Structure", "Code review", "Merge"),
            defaults.map { it.name },
        )
        assertTrue(defaults.all { it.isDefault })
    }

    @Test
    fun `keeps defaults immutable and persists only custom templates`() {
        val custom = CommitTemplate("Release", "[CODE_REVIEW] - Prepare release notes")
        val allTemplates = CommitTemplates.withDefaults(CommitTemplates.defaults() + custom)

        assertEquals(7, allTemplates.size)
        assertEquals(listOf(custom), CommitTemplates.customOnly(allTemplates))
    }
}
