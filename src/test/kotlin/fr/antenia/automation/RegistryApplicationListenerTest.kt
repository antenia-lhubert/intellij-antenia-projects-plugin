package fr.antenia.automation

import org.junit.Assert.assertEquals
import org.junit.Test

class RegistryApplicationListenerTest {
    @Test
    fun `disables iterative merge conflict resolution`() {
        val configuredValues = mutableMapOf<String, Boolean>()

        RegistryAutoConfigurator.applyValues(configuredValues::put)

        assertEquals(
            mapOf("vcs.merge.conflict.iterative.resolution" to false),
            configuredValues,
        )
    }
}
