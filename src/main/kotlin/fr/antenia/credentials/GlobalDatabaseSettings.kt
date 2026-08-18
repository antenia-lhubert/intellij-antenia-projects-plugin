package fr.antenia.credentials

import com.intellij.credentialStore.CredentialAttributes
import com.intellij.credentialStore.Credentials
import com.intellij.ide.passwordSafe.PasswordSafe
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import fr.antenia.notifications.AnteniaNotifications
import fr.antenia.MyMessageBundle.message

data class DatabaseCredentials(
    val username: String,
    val password: String,
)

@Service(Service.Level.APP)
class GlobalDatabaseSettings {
    private val logger = Logger.getInstance(GlobalDatabaseSettings::class.java)

    fun credentials(): DatabaseCredentials {
        val stored = PasswordSafe.instance.get(CREDENTIAL_ATTRIBUTES)
        return DatabaseCredentials(stored?.userName.orEmpty(), stored?.getPasswordAsString().orEmpty())
    }

    fun save(credentials: DatabaseCredentials) {
        try {
            PasswordSafe.instance.set(CREDENTIAL_ATTRIBUTES, Credentials(credentials.username, credentials.password))
            logger.info(
                "Saved global database credentials to Password Safe: " +
                    "usernamePresent=${credentials.username.isNotEmpty()}, passwordPresent=${credentials.password.isNotEmpty()}",
            )
            GlobalDatabaseCredentialsSynchronizer.updateOpenProjects(credentials)
        } catch (exception: Exception) {
            logger.error("Unable to save or synchronize global database credentials", exception)
            com.intellij.openapi.project.ProjectManager.getInstance().openProjects.forEach { project ->
                AnteniaNotifications.failure(
                    project,
                    "global-database-credentials",
                    message("notification.global.credentials.apply.failure.title"),
                    message("common.error.details", exception.message ?: exception.javaClass.simpleName),
                )
            }
            throw exception
        }
    }

    companion object {
        private val CREDENTIAL_ATTRIBUTES = CredentialAttributes("fr.antenia.antenia-projects.global-database")

        fun getInstance(): GlobalDatabaseSettings = ApplicationManager.getApplication().getService(GlobalDatabaseSettings::class.java)
    }
}

object ProjectDatabaseCredentials {
    private val logger = Logger.getInstance(ProjectDatabaseCredentials::class.java)

    fun credentials(project: Project): DatabaseCredentials? {
        val stored = PasswordSafe.instance.get(attributes(project)) ?: return null
        return DatabaseCredentials(stored.userName.orEmpty(), stored.getPasswordAsString().orEmpty())
    }

    fun save(project: Project, credentials: DatabaseCredentials) {
        try {
            PasswordSafe.instance.set(attributes(project), Credentials(credentials.username, credentials.password))
        } catch (exception: Exception) {
            logger.error("Unable to save project database credentials for '${project.name}'", exception)
            AnteniaNotifications.failure(
                project,
                "project-database-credentials",
                message("notification.project.credentials.save.failure.title"),
                message("common.error.details", exception.message ?: exception.javaClass.simpleName),
            )
            throw exception
        }
    }

    fun clear(project: Project) {
        PasswordSafe.instance.set(attributes(project), null)
        logger.info("Cleared project database credentials for '${project.name}'")
    }

    private fun attributes(project: Project): CredentialAttributes = CredentialAttributes(
        "fr.antenia.antenia-projects.project-database.${project.locationHash}",
    )
}

object ProfileDatabaseCredentials {
    private val logger = Logger.getInstance(ProfileDatabaseCredentials::class.java)

    fun credentials(profileId: String): DatabaseCredentials? {
        if (profileId.isBlank()) return null
        val stored = PasswordSafe.instance.get(attributes(profileId)) ?: return null
        return DatabaseCredentials(stored.userName.orEmpty(), stored.getPasswordAsString().orEmpty())
    }

    fun save(profileId: String, credentials: DatabaseCredentials) {
        require(profileId.isNotBlank()) { "A database profile ID is required" }
        try {
            PasswordSafe.instance.set(attributes(profileId), Credentials(credentials.username, credentials.password))
        } catch (exception: Exception) {
            logger.error("Unable to save database profile credentials", exception)
            throw exception
        }
    }

    fun clear(profileId: String) {
        if (profileId.isBlank()) return
        PasswordSafe.instance.set(attributes(profileId), null)
    }

    private fun attributes(profileId: String): CredentialAttributes = CredentialAttributes(
        "fr.antenia.antenia-projects.profile-database.$profileId",
    )
}
