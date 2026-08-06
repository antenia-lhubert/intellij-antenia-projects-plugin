package fr.antenia.credentials

import com.intellij.openapi.options.SearchableConfigurable
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.panel
import fr.antenia.MyMessageBundle.message
import javax.swing.JComponent

class GlobalDatabaseConfigurable : SearchableConfigurable {
    private var form: DialogPanel? = null
    private var username = ""
    private var password = ""

    override fun getDisplayName(): String = message("configurable.projects.display.name")
    override fun getId(): String = "fr.antenia.globalDatabase"

    override fun createComponent(): JComponent {
        resetValues()
        return panel {
            group(message("settings.database.group")) {
                row(message("settings.database.username")) { textField().bindText({ username }, { username = it }) }
                row(message("settings.database.password")) { passwordField().bindText({ password }, { password = it }) }
                row { comment(message("settings.database.password.storage")) }
            }
        }.also { form = it }
    }

    override fun isModified(): Boolean = form?.isModified() == true

    override fun apply() {
        form?.apply()
        GlobalDatabaseSettings.getInstance().save(DatabaseCredentials(username, password))
    }

    override fun reset() {
        resetValues()
        form?.reset()
    }

    override fun disposeUIResources() {
        form = null
    }

    private fun resetValues() {
        GlobalDatabaseSettings.getInstance().credentials().also {
            username = it.username
            password = it.password
        }
    }
}
