package fr.antenia.credentials

import com.intellij.credentialStore.CredentialAttributes
import com.intellij.credentialStore.Credentials
import com.intellij.ide.passwordSafe.PasswordSafe
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service

data class DatabaseCredentials(
    val username: String,
    val password: String,
)

@Service(Service.Level.APP)
class GlobalDatabaseSettings {
    fun credentials(): DatabaseCredentials {
        val stored = PasswordSafe.instance.get(CREDENTIAL_ATTRIBUTES)
        return DatabaseCredentials(stored?.userName.orEmpty(), stored?.getPasswordAsString().orEmpty())
    }

    fun save(credentials: DatabaseCredentials) {
        PasswordSafe.instance.set(CREDENTIAL_ATTRIBUTES, Credentials(credentials.username, credentials.password))
        GlobalDatabaseCredentialsSynchronizer.updateOpenProjects(credentials)
    }

    companion object {
        private val CREDENTIAL_ATTRIBUTES = CredentialAttributes("fr.antenia.antenia-projects.global-database")

        fun getInstance(): GlobalDatabaseSettings = ApplicationManager.getApplication().getService(GlobalDatabaseSettings::class.java)
    }
}
