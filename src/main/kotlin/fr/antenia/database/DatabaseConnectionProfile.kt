package fr.antenia.database

import fr.antenia.project.NeoProjectType
import fr.antenia.project.NeoSchema
import java.util.Locale
import java.util.UUID
import org.semver4j.Semver

data class DatabaseConnectionProfile(
    val name: String,
    val host: String,
    val port: Int,
    val database: String,
    val overrideGlobalCredentials: Boolean = false,
    val id: String = "",
    val projectType: NeoProjectType = NeoProjectType.CORE,
    val advancedValues: Map<String, String> = emptyMap(),
)

object DatabaseConnectionProfiles {
    private val mysql8MinVersion = Semver("1.3.0-0")
    private val currentMysqlMinVersion = Semver("1.5.0-0")
    private val providedHosts = listOf(
        "antenia-dev-mysql5.leaderinfo.com",
        "antenia-dev-mysql8.leaderinfo.com",
        "mysql8-4-5-dev.antenia.com",
    )

    fun defaultHosts(): List<String> = providedHosts

    fun preferredDefaultHost(projectVersion: Semver): String {
        return providedHosts[when {
            projectVersion < mysql8MinVersion -> 0
            projectVersion < currentMysqlMinVersion -> 1
            else -> 2
        }]
    }

    fun newId(): String = UUID.randomUUID().toString()

    fun defaultAdvancedValues(type: NeoProjectType): Map<String, String> =
        NeoSchema.forType(type).database?.advancedDefaults.orEmpty()

    fun applicable(profiles: List<DatabaseConnectionProfile>, type: NeoProjectType): List<DatabaseConnectionProfile> =
        profiles.filter { it.projectType == type }

    fun normalized(profile: DatabaseConnectionProfile): DatabaseConnectionProfile {
        val type = profile.projectType.takeIf { it == NeoProjectType.GED } ?: NeoProjectType.CORE
        val defaults = defaultAdvancedValues(type)
        return profile.copy(
            projectType = type,
            advancedValues = defaults + profile.advancedValues.filterKeys(defaults::containsKey),
        )
    }

    fun matching(
        profiles: List<DatabaseConnectionProfile>,
        type: NeoProjectType,
        host: String,
        port: Int,
        database: String,
    ): DatabaseConnectionProfile? = profiles.withIndex()
        .filter { (_, profile) ->
            profile.projectType == type &&
                profile.host.equals(host, ignoreCase = true) && profile.port == port &&
                (profile.database.isEmpty() || profile.database == database)
        }
        .maxWithOrNull(
            compareBy<IndexedValue<DatabaseConnectionProfile>> {
                if (it.value.database.isNotEmpty()) 1 else 0
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
