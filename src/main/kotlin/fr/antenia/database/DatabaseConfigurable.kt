package fr.antenia.database

import com.intellij.openapi.options.SearchableConfigurable
import com.intellij.ui.dsl.builder.panel
import fr.antenia.MyMessageBundle.message
import javax.swing.JComponent

class DatabaseConfigurable : SearchableConfigurable {
    override fun getDisplayName(): String = message("configurable.database.display.name")
    override fun getId(): String = "fr.antenia.database"

    override fun createComponent(): JComponent = panel {
        group(message("settings.database.category.group")) {
            row { comment(message("settings.database.category.description")) }
            row { comment(message("settings.projects.description")) }
            row { comment(message("settings.database.profiles.description")) }
            row { comment(message("settings.database.hosts.description")) }
        }
    }

    override fun isModified(): Boolean = false
    override fun apply() = Unit
}
