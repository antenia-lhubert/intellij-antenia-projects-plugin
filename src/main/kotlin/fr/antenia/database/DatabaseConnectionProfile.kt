package fr.antenia.database

import java.util.Locale
import java.util.UUID

data class DatabaseConnectionProfile(
    val name: String,
    val host: String,
    val port: Int,
    val database: String,
    val overrideGlobalCredentials: Boolean = false,
    val id: String = "",
)

object DatabaseConnectionProfiles {
    private val providedHosts = listOf(
        "antenia-dev-mysql5.leaderinfo.com",
        "antenia-dev-mysql8.leaderinfo.com",
        "mysql8-4-5-dev.antenia.com",
    )

    fun defaultHosts(): List<String> = providedHosts

    fun preferredDefaultHost(javaVersion: Int): String = providedHosts[if (javaVersion >= 17) 2 else 1]

    fun newId(): String = UUID.randomUUID().toString()

    fun matching(
        profiles: List<DatabaseConnectionProfile>,
        host: String,
        port: Int,
        database: String,
    ): DatabaseConnectionProfile? = profiles.firstOrNull {
        it.host.equals(host, ignoreCase = true) && it.port == port && it.database.isNotEmpty() && it.database == database
    } ?: profiles.firstOrNull {
        it.host.equals(host, ignoreCase = true) && it.port == port && it.database.isEmpty()
    }

    fun customOnly(profiles: List<DatabaseConnectionProfile>): List<DatabaseConnectionProfile> {
        val customNames = mutableSetOf<String>()
        return profiles.mapNotNull { profile ->
            val normalizedName = profile.name.normalizedName()
            if (!customNames.add(normalizedName)) {
                null
            } else {
                profile
            }
        }
    }

    private fun String.normalizedName(): String = lowercase(Locale.ROOT)
}
