package fr.antenia.run

import org.junit.Assert.assertEquals
import org.junit.Test

class NeoEnvironmentExtensionTest {
    @Test
    fun `injects rmi hostname option`() {
        assertEquals(RMI_HOSTNAME_OPTION, appendRmiHostname(null))
    }

    @Test
    fun `preserves existing java tool options`() {
        assertEquals("-Xmx1g $RMI_HOSTNAME_OPTION", appendRmiHostname("-Xmx1g"))
    }

    @Test
    fun `does not duplicate rmi hostname option`() {
        assertEquals(RMI_HOSTNAME_OPTION, appendRmiHostname(RMI_HOSTNAME_OPTION))
    }
}
