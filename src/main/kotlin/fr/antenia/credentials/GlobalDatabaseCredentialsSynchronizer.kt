package fr.antenia.credentials

import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import fr.antenia.config.ConfigurationFiles
import fr.antenia.config.ProjectConfigurationState
import fr.antenia.project.NeoProjectDetector
import fr.antenia.project.NeoSchema

object GlobalDatabaseCredentialsSynchronizer {
    fun updateOpenProjects(credentials: DatabaseCredentials) {
        ProjectManager.getInstance().openProjects.forEach { update(it, credentials) }
    }

    fun update(project: Project, credentials: DatabaseCredentials) {
        if (project.isDisposed || ProjectConfigurationState.getInstance(project).overrideGlobalCredentials) return
        val neoProject = NeoProjectDetector.detect(project) ?: return
        val keys = NeoSchema.forType(neoProject.type).database ?: return
        val path = ConfigurationFiles.ensureCreated(project, neoProject.type)
        val document = ConfigurationFiles.read(path)
        val updates = keys.usernames.associateWith { credentials.username } +
            keys.passwords.associateWith { credentials.password }
        if (updates.all { (key, value) -> document.value(key) == value }) return
        updates.forEach(document::setValue)
        document.regroup(buildSet {
            add(keys.url)
            keys.database?.let(::add)
            addAll(keys.usernames)
            addAll(keys.passwords)
        })
        ConfigurationFiles.write(project, path, document)
    }
}
