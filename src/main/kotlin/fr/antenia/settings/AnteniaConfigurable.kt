package fr.antenia.settings

import com.intellij.openapi.options.SearchableConfigurable
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.ProjectManager
import com.intellij.ui.dsl.builder.panel
import fr.antenia.MyMessageBundle.message
import fr.antenia.commit.CommitTemplatesConfigurable
import fr.antenia.credentials.GlobalDatabaseConfigurable
import javax.swing.JComponent

class AnteniaConfigurable : SearchableConfigurable {
    override fun getDisplayName(): String = message("configurable.antenia.display.name")
    override fun getId(): String = "fr.antenia"

    override fun createComponent(): JComponent = panel {
        group(message("settings.antenia.group")) {
            row {
                link(message("configurable.projects.display.name")) { open(GlobalDatabaseConfigurable::class.java) }
                comment(message("settings.projects.description"))
            }
            row {
                link(message("configurable.commit.templates.display.name")) { open(CommitTemplatesConfigurable::class.java) }
                comment(message("settings.commit.templates.description"))
            }
            row {
                link(message("configurable.run.configurations.display.name")) { open(TomcatRunConfigurationConfigurable::class.java) }
                comment(message("settings.run.configurations.description"))
            }
        }
    }

    override fun isModified(): Boolean = false
    override fun apply() = Unit

    private fun open(configurable: Class<out SearchableConfigurable>) {
        val projectManager = ProjectManager.getInstance()
        val project = projectManager.openProjects.firstOrNull() ?: projectManager.defaultProject
        ShowSettingsUtil.getInstance().showSettingsDialog(project, configurable)
    }
}
