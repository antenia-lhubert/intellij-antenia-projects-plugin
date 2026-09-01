package fr.antenia.database

import fr.antenia.project.NeoProjectType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DatabaseConnectionProfileSettingsTest {
    @Test
    fun `provides host suggestions without creating profiles`() {
        assertEquals(
            listOf(
                "antenia-dev-mysql5.leaderinfo.com",
                "antenia-dev-mysql8.leaderinfo.com",
                "mysql8-4-5-dev.antenia.com",
            ),
            DatabaseConnectionProfiles.defaultHosts(),
        )
        assertEquals("antenia-dev-mysql8.leaderinfo.com", DatabaseConnectionProfiles.preferredDefaultHost(8))
        assertEquals("mysql8-4-5-dev.antenia.com", DatabaseConnectionProfiles.preferredDefaultHost(17))
        assertTrue(DatabaseConnectionProfileSettings().profiles().isEmpty())
    }

    @Test
    fun `filters duplicate custom profile names case insensitively`() {
        val settings = DatabaseConnectionProfileSettings()
        settings.replaceProfiles(
            listOf(
                DatabaseConnectionProfile("Team", "team.example.com", 3307, "neo"),
                DatabaseConnectionProfile("team", "duplicate.example.com", 3308, "other"),
            ),
        )

        val profiles = settings.getState().profiles.map { it.toProfile() }
        assertEquals(
            listOf(DatabaseConnectionProfiles.normalized(DatabaseConnectionProfile("Team", "team.example.com", 3307, "neo"))),
            profiles.map { it.copy(id = "") },
        )
        assertTrue(profiles.single().id.isNotBlank())
    }

    @Test
    fun `loads only sanitized persisted custom profiles`() {
        val state = DatabaseConnectionProfileSettings.SettingsState().apply {
            profiles = mutableListOf(
                DatabaseConnectionProfileSettings.StoredProfile(
                    DatabaseConnectionProfile("Local", "localhost", 3307, "neo", overrideGlobalCredentials = true),
                ),
                DatabaseConnectionProfileSettings.StoredProfile(
                    DatabaseConnectionProfile("LOCAL", "duplicate", 3308, "other"),
                ),
            )
        }
        val settings = DatabaseConnectionProfileSettings()

        settings.loadState(state)

        val profiles = settings.profiles()
        assertEquals(
            listOf(
                DatabaseConnectionProfiles.normalized(
                    DatabaseConnectionProfile("Local", "localhost", 3307, "neo", overrideGlobalCredentials = true),
                ),
            ),
            profiles.map { it.copy(id = "") },
        )
        assertTrue(profiles.single().id.isNotBlank())
        assertEquals(1, settings.getState().profiles.size)
    }

    @Test
    fun `matches an exact or host only user profile`() {
        val profiles = listOf(
            DatabaseConnectionProfile("Host only", "mysql.example.com", 3306, ""),
            DatabaseConnectionProfile("Neo", "mysql.example.com", 3306, "neo"),
        )

        assertEquals(
            "Neo",
            DatabaseConnectionProfiles.matching(profiles, NeoProjectType.CORE, "MYSQL.EXAMPLE.COM", 3306, "neo")?.name,
        )
        assertEquals(
            "Host only",
            DatabaseConnectionProfiles.matching(profiles, NeoProjectType.CORE, "mysql.example.com", 3306, "other")?.name,
        )
    }

    @Test
    fun `matches profiles by connection details without comparing advanced values`() {
        val profiles = listOf(
            DatabaseConnectionProfile("Host only", "mysql.example.com", 3306, ""),
            DatabaseConnectionProfile(
                "Neo custom pool",
                "mysql.example.com",
                3306,
                "neo",
                projectType = NeoProjectType.CORE,
                advancedValues = DatabaseConnectionProfiles.defaultAdvancedValues(NeoProjectType.CORE) +
                    ("checkoutTimeout" to "20000"),
            ),
        )

        assertEquals(
            "Neo custom pool",
            DatabaseConnectionProfiles.matching(
                profiles,
                NeoProjectType.CORE,
                "mysql.example.com",
                3306,
                "neo",
            )?.name,
        )
    }

    @Test
    fun `persists typed advanced values`() {
        val settings = DatabaseConnectionProfileSettings()
        settings.replaceProfiles(
            listOf(
                DatabaseConnectionProfile(
                    "Neo EDI",
                    "mysql.example.com",
                    3306,
                    "neo",
                    projectType = NeoProjectType.CORE,
                    advancedValues = mapOf("databaseEdi" to "neo_edi", "checkoutTimeout" to "20000"),
                ),
            ),
        )

        val stored = settings.getState().profiles.single()
        assertEquals("CORE", stored.projectType)
        assertEquals("neo_edi", stored.toProfile().advancedValues["databaseEdi"])
        assertEquals("20000", stored.toProfile().advancedValues["checkoutTimeout"])
        assertFalse(stored.toProfile().advancedValues.containsKey("WebActionUrlDataBase"))
    }

    @Test
    fun `filters profiles by project type`() {
        val core = DatabaseConnectionProfile("Core", "mysql.example.com", 3306, "")
        val ged = core.copy(name = "GED", projectType = NeoProjectType.GED)

        assertEquals(listOf("Core"), DatabaseConnectionProfiles.applicable(listOf(core, ged), NeoProjectType.CORE).map { it.name })
        assertEquals(listOf("GED"), DatabaseConnectionProfiles.applicable(listOf(core, ged), NeoProjectType.GED).map { it.name })
    }

    @Test
    fun `migrates every untyped legacy profile to Core`() {
        val stored = DatabaseConnectionProfileSettings.StoredProfile().apply {
            name = "Legacy Core"
            host = "mysql.example.com"
        }

        val profile = stored.toProfile()

        assertEquals(NeoProjectType.CORE, profile.projectType)
        assertEquals("", profile.advancedValues["databaseEdi"])
        assertEquals("10000", profile.advancedValues["checkoutTimeout"])
    }

    @Test
    fun `preserves database EDI while migrating a legacy profile`() {
        val stored = DatabaseConnectionProfileSettings.StoredProfile().apply {
            databaseEdi = "neo_edi"
        }

        assertEquals("neo_edi", stored.toProfile().advancedValues["databaseEdi"])
    }

    @Test
    fun `migrates an unsupported persisted project type to Core`() {
        val stored = DatabaseConnectionProfileSettings.StoredProfile().apply {
            projectType = "SELFCARE"
        }

        val profile = stored.toProfile()

        assertEquals(NeoProjectType.CORE, profile.projectType)
        assertEquals("com.mysql.jdbc.Driver", profile.advancedValues["driver"])
    }

    @Test
    fun `merges provided and unique custom hosts`() {
        val settings = DatabaseConnectionProfileSettings()

        assertTrue(settings.addHost(" mysql.team.example.com "))
        assertFalse(settings.addHost("MYSQL.TEAM.EXAMPLE.COM"))
        assertFalse(settings.addHost("antenia-dev-mysql5.leaderinfo.com"))

        assertEquals(
            DatabaseConnectionProfiles.defaultHosts() + "mysql.team.example.com",
            settings.hosts(),
        )
    }

    @Test
    fun `sanitizes persisted custom hosts`() {
        val state = DatabaseConnectionProfileSettings.SettingsState().apply {
            hosts = mutableListOf(
                " mysql.team.example.com ",
                "MYSQL.TEAM.EXAMPLE.COM",
                "mysql8-4-5-dev.antenia.com",
                "",
            )
        }
        val settings = DatabaseConnectionProfileSettings()

        settings.loadState(state)

        assertEquals(listOf("mysql.team.example.com"), settings.getState().hosts)
    }

    @Test
    fun `profile state stores credential policy but no secrets`() {
        val settings = DatabaseConnectionProfileSettings()
        settings.replaceProfiles(
            listOf(DatabaseConnectionProfile("Team", "mysql.example.com", 3306, "neo", true)),
        )

        val stored = settings.getState().profiles.single()
        assertTrue(stored.overrideGlobalCredentials)
        assertTrue(stored.id.isNotBlank())
        assertFalse(
            DatabaseConnectionProfileSettings.StoredProfile::class.java.declaredFields.any {
                it.name.contains("username", ignoreCase = true) || it.name.contains("password", ignoreCase = true)
            },
        )
    }
}
