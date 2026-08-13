package fr.antenia.settings

import com.intellij.openapi.options.SearchableConfigurable
import com.intellij.ui.dsl.builder.panel
import fr.antenia.MyMessageBundle.message
import javax.swing.JComponent

class AnteniaConfigurable : SearchableConfigurable {
    override fun getDisplayName(): String = message("configurable.antenia.display.name")
    override fun getId(): String = "fr.antenia"

    override fun createComponent(): JComponent = panel {
        group(message("settings.antenia.group")) {
            row { comment(message("settings.antenia.description")) }
            row { comment(message("settings.projects.description")) }
            row { comment(message("settings.commit.templates.description")) }
            row { comment(message("settings.run.configurations.description")) }
        }
    }

    override fun isModified(): Boolean = false
    override fun apply() = Unit

}
