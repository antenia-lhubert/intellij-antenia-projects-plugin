package fr.antenia.credentials

import org.junit.Assert.assertEquals
import org.junit.Test

class GlobalDatabaseCredentialsSynchronizerTest {
    private val global = DatabaseCredentials("global-user", "global-password")
    private val project = DatabaseCredentials("project-user", "project-password")

    @Test
    fun `restores global credentials when project override is disabled`() {
        assertEquals(global, restoredDatabaseCredentials(false, project, global))
    }

    @Test
    fun `restores project credentials when project override is enabled`() {
        assertEquals(project, restoredDatabaseCredentials(true, project, global))
    }

    @Test
    fun `does not expose global credentials when an override has no saved credentials`() {
        assertEquals(DatabaseCredentials("", ""), restoredDatabaseCredentials(true, null, global))
    }
}
