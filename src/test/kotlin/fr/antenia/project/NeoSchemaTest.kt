package fr.antenia.project

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NeoSchemaTest {
    @Test
    fun `exposes database EDI only as a Core database field`() {
        val core = NeoSchema.forType(NeoProjectType.CORE)

        assertEquals("databaseEdi", core.database?.databaseEdi)
        assertTrue("databaseEdi" in core.specialKeys)
        assertNull(NeoSchema.forType(NeoProjectType.GED).database?.databaseEdi)
        assertNull(NeoSchema.forType(NeoProjectType.SELFCARE).database)
    }
}
