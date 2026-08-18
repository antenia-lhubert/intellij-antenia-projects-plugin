package fr.antenia.database

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
            listOf(DatabaseConnectionProfile("Team", "team.example.com", 3307, "neo")),
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
            listOf(DatabaseConnectionProfile("Local", "localhost", 3307, "neo", overrideGlobalCredentials = true)),
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

        assertEquals("Neo", DatabaseConnectionProfiles.matching(profiles, "MYSQL.EXAMPLE.COM", 3306, "neo")?.name)
        assertEquals("Host only", DatabaseConnectionProfiles.matching(profiles, "mysql.example.com", 3306, "other")?.name)
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
