package fr.antenia.project

import fr.antenia.config.OrderedPropertiesCodec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NeoSchemaTest {
    @Test
    fun `exposes only the supported advanced database fields for each project type`() {
        val core = requireNotNull(NeoSchema.forType(NeoProjectType.CORE).database)
        val ged = requireNotNull(NeoSchema.forType(NeoProjectType.GED).database)

        assertEquals(
            listOf(
                "databaseEdi", "driver", "hibernate.show_sql", "hibernate.dialect", "checkoutTimeout",
                "maxIdleTime", "maxConnectionAge", "acquireIncrement", "maxStatements", "propertyCycle",
                "unreturnedConnectionTimeout", "autoCommitOnClose", "preferredTestQuery", "switchToManual",
                "minPoolSizeNormal", "maxPoolSizeNormal", "minPoolSizeSpring", "maxPoolSizeSpring", "SGBD",
                "liquibaseEnabled",
            ),
            core.advanced.map { it.key },
        )
        assertEquals(
            listOf("jdbc.driverClassName", "hibernate.show_sql", "hibernate.dialect"),
            ged.advanced.map { it.key },
        )
        assertTrue(core.advanced.all { it.key in core.allKeys })
        assertEquals(core.allKeys, core.layoutGroups.flatMap { it.keys }.toSet())
        assertEquals(ged.allKeys, ged.layoutGroups.flatMap { it.keys }.toSet())
        listOf(
            "WebActionUrlDataBase",
            "WebActionDriverDataBase",
            "WebActionLoginDataBase",
            "WebActionPasswordDataBase",
            "WebActionUrlServerApplication",
            "WebActionUrlServerApplicationTest",
            "WebActionUrlServerApplicationInterne",
        ).forEach {
            assertFalse(it in core.allKeys)
            assertFalse(it in NeoSchema.forType(NeoProjectType.CORE).specialKeys)
        }
        assertFalse("CouperRequetes" in core.allKeys)
        assertFalse("PropertyCycle" in core.allKeys)
        assertNull(NeoSchema.forType(NeoProjectType.SELFCARE).database)
    }

    @Test
    fun `advanced database defaults match the project templates`() {
        mapOf(
            NeoProjectType.CORE to "/templates/novanet.properties",
            NeoProjectType.GED to "/templates/configuration.properties",
        ).forEach { (type, resource) ->
            val database = requireNotNull(NeoSchema.forType(type).database)
            val template = requireNotNull(javaClass.getResource(resource)).readText()
            val document = OrderedPropertiesCodec.parse(template)

            assertEquals(
                database.advancedDefaults,
                database.advanced.associate { it.key to document.value(it.key).orEmpty() },
            )
            val beforeRegroup = OrderedPropertiesCodec.render(document)
            document.regroupPreservingLayout(database.layoutGroups)
            val afterRegroup = OrderedPropertiesCodec.render(document)
            assertEquals(beforeRegroup, afterRegroup)
            document.regroupPreservingLayout(database.layoutGroups)
            assertEquals(afterRegroup, OrderedPropertiesCodec.render(document))
        }
    }
}
