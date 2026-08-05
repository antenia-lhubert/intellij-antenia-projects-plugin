package fr.antenia.settings

import com.intellij.openapi.options.SearchableConfigurable
import com.intellij.ui.dsl.builder.panel
import javax.swing.JComponent

class AnteniaConfigurable : SearchableConfigurable {
    override fun getDisplayName(): String = "Antenia"
    override fun getId(): String = "fr.antenia"

    override fun createComponent(): JComponent = panel {
        row { label("Select an Antenia settings page.") }
    }

    override fun isModified(): Boolean = false
    override fun apply() = Unit
}
