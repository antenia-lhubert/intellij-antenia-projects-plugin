package fr.antenia.database

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage

@Service(Service.Level.APP)
@State(name = "AnteniaDatabaseConnectionProfiles", storages = [Storage("antenia.xml")])
class DatabaseConnectionProfileSettings :
    PersistentStateComponent<DatabaseConnectionProfileSettings.SettingsState> {
    private var settingsState = SettingsState()

    override fun getState(): SettingsState = settingsState

    override fun loadState(state: SettingsState) {
        settingsState = SettingsState()
        replaceProfiles(state.profiles.map(StoredProfile::toProfile))
        replaceHosts(state.hosts)
    }

    fun profiles(): List<DatabaseConnectionProfile> = DatabaseConnectionProfiles.customOnly(
        settingsState.profiles.map(StoredProfile::toProfile),
    )

    fun replaceProfiles(profiles: List<DatabaseConnectionProfile>) {
        val ids = mutableSetOf<String>()
        settingsState.profiles = DatabaseConnectionProfiles.customOnly(profiles).map { profile ->
            val id = profile.id.takeIf { it.isNotBlank() && ids.add(it) }
                ?: DatabaseConnectionProfiles.newId().also(ids::add)
            profile.copy(id = id)
        }
            .map(::StoredProfile)
            .toMutableList()
    }

    fun hosts(): List<String> = DatabaseConnectionProfiles.defaultHosts() + settingsState.hosts

    fun addHost(host: String): Boolean {
        val value = host.trim()
        if (value.isEmpty() || hosts().any { it.equals(value, ignoreCase = true) }) return false
        settingsState.hosts.add(value)
        return true
    }

    fun replaceHosts(hosts: List<String>) {
        val provided = DatabaseConnectionProfiles.defaultHosts()
        val seen = mutableSetOf<String>()
        settingsState.hosts = hosts.map(String::trim).filter {
            it.isNotEmpty() && provided.none { providedHost -> providedHost.equals(it, ignoreCase = true) } &&
                seen.add(it.lowercase(java.util.Locale.ROOT))
        }.toMutableList()
    }

    class SettingsState {
        var profiles: MutableList<StoredProfile> = mutableListOf()
        var hosts: MutableList<String> = mutableListOf()
    }

    class StoredProfile() {
        var name: String = ""
        var host: String = ""
        var port: Int = 3306
        var database: String = ""
        var overrideGlobalCredentials: Boolean = false
        var id: String = ""

        constructor(profile: DatabaseConnectionProfile) : this() {
            name = profile.name
            host = profile.host
            port = profile.port
            database = profile.database
            overrideGlobalCredentials = profile.overrideGlobalCredentials
            id = profile.id
        }

        fun toProfile(): DatabaseConnectionProfile = DatabaseConnectionProfile(
            name = name,
            host = host,
            port = port,
            database = database,
            overrideGlobalCredentials = overrideGlobalCredentials,
            id = id,
        )
    }

    companion object {
        fun getInstance(): DatabaseConnectionProfileSettings =
            ApplicationManager.getApplication().getService(DatabaseConnectionProfileSettings::class.java)
    }
}
