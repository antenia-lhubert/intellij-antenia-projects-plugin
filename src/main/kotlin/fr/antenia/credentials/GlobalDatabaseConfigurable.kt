package fr.antenia.credentials

import com.intellij.openapi.options.SearchableConfigurable
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.panel
import javax.swing.JComponent

class GlobalDatabaseConfigurable : SearchableConfigurable {
    private var form: DialogPanel? = null
    private var username = ""
    private var password = ""

    override fun getDisplayName(): String = "Projects"
    override fun getId(): String = "fr.antenia.globalDatabase"

    override fun createComponent(): JComponent {
        resetValues()
        return panel {
            group("Global database credentials") {
                row("Username:") { textField().bindText({ username }, { username = it }) }
                row("Password:") { passwordField().bindText({ password }, { password = it }) }
                row { comment("The password is stored in IntelliJ Password Safe, never in project files or settings XML.") }
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
