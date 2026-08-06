package fr.antenia.settings

import com.intellij.openapi.options.SearchableConfigurable
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.ProjectManager
import com.intellij.ui.dsl.builder.panel
import fr.antenia.commit.CommitTemplatesConfigurable
import fr.antenia.credentials.GlobalDatabaseConfigurable
import javax.swing.JComponent

class AnteniaConfigurable : SearchableConfigurable {
    override fun getDisplayName(): String = "Antenia"
    override fun getId(): String = "fr.antenia"

    override fun createComponent(): JComponent = panel {
        group("Antenia settings") {
            row {
                link("Projects") { open(GlobalDatabaseConfigurable::class.java) }
                comment("Global database credentials used by Antenia projects.")
            }
            row {
                link("Commit Templates") { open(CommitTemplatesConfigurable::class.java) }
                comment("Commit-message templates and validation formats.")
            }
            row {
                link("Run Configurations") { open(TomcatRunConfigurationConfigurable::class.java) }
                comment("Defaults for generated Tomcat run configurations.")
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
