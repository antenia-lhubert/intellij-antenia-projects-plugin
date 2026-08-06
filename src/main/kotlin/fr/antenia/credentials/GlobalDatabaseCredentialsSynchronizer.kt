package fr.antenia.credentials

import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.diagnostic.Logger
import fr.antenia.config.ConfigurationFiles
import fr.antenia.config.ProjectConfigurationState
import fr.antenia.project.NeoProjectDetector
import fr.antenia.project.NeoSchema
import fr.antenia.notifications.AnteniaNotifications
import fr.antenia.MyMessageBundle.message

object GlobalDatabaseCredentialsSynchronizer {
    private val logger = Logger.getInstance(GlobalDatabaseCredentialsSynchronizer::class.java)

    fun updateOpenProjects(credentials: DatabaseCredentials) {
        logger.info("Synchronizing global database credentials to ${ProjectManager.getInstance().openProjects.size} open project(s)")
        ProjectManager.getInstance().openProjects.forEach { project ->
            try {
                update(project, credentials)
            } catch (exception: Exception) {
                logger.error("Unable to synchronize global database credentials for '${project.name}'", exception)
                AnteniaNotifications.failure(
                    project,
                    "global-database-credential-synchronization",
                    message("notification.global.credentials.sync.failure.title"),
                    message("common.error.details", exception.message ?: exception.javaClass.simpleName),
                )
            }
        }
    }

    fun update(project: Project, credentials: DatabaseCredentials) {
        if (project.isDisposed) {
            logger.debug("Global database credential synchronization skipped for '${project.name}': project is disposed")
            return
        }
        if (ProjectConfigurationState.getInstance(project).overrideGlobalCredentials) {
            logger.info("Global database credential synchronization skipped for '${project.name}': project override is enabled")
            return
        }
        val neoProject = NeoProjectDetector.detect(project)
        if (neoProject == null) {
            logger.debug("Global database credential synchronization skipped for '${project.name}': unsupported project")
            return
        }
        val keys = NeoSchema.forType(neoProject.type).database
        if (keys == null) {
            logger.info("Global database credential synchronization skipped for '${project.name}': no database schema")
            return
        }
        val path = ConfigurationFiles.ensureCreated(project, neoProject.type)
        val document = ConfigurationFiles.read(path)
        val updates = keys.usernames.associateWith { credentials.username } +
            keys.passwords.associateWith { credentials.password }
        if (updates.all { (key, value) -> document.value(key) == value }) {
            logger.info("Global database credentials already synchronized for '${project.name}'")
            return
        }
        updates.forEach(document::setValue)
        document.regroup(buildSet {
            add(keys.url)
            keys.database?.let(::add)
            addAll(keys.usernames)
            addAll(keys.passwords)
        })
        ConfigurationFiles.write(project, path, document)
        logger.info(
            "Synchronized global database credentials for '${project.name}': " +
                "usernamePresent=${credentials.username.isNotEmpty()}, passwordPresent=${credentials.password.isNotEmpty()}, keys=${updates.keys.joinToString()}",
        )
    }
}
