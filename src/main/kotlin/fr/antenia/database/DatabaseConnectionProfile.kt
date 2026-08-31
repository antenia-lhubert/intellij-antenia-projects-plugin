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
    val databaseEdi: String = "",
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
        databaseEdi: String = "",
    ): DatabaseConnectionProfile? = profiles.withIndex()
        .filter { (_, profile) ->
            profile.host.equals(host, ignoreCase = true) && profile.port == port &&
                (profile.database.isEmpty() || profile.database == database) &&
                (profile.databaseEdi.isEmpty() || profile.databaseEdi == databaseEdi)
        }
        .maxWithOrNull(
            compareBy<IndexedValue<DatabaseConnectionProfile>> {
                (if (it.value.database.isNotEmpty()) 1 else 0) + (if (it.value.databaseEdi.isNotEmpty()) 1 else 0)
            }.thenByDescending { it.index },
        )
        ?.value

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
