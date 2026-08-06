package fr.antenia.settings

import com.intellij.openapi.options.SearchableConfigurable
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.dsl.builder.bindItem
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.panel
import fr.antenia.MyMessageBundle.message
import javax.swing.JComponent

class TomcatRunConfigurationConfigurable : SearchableConfigurable {
    private var form: DialogPanel? = null
    private var openInBrowser = true
    private var frameDeactivationAction = TomcatFrameDeactivationAction.UPDATE_RESOURCES
    private var updatingPolicy = TomcatUpdatingPolicy.UPDATE_RESOURCES_AND_CLASSES

    override fun getDisplayName(): String = message("configurable.run.configurations.display.name")
    override fun getId(): String = "fr.antenia.runConfigurations"

    override fun createComponent(): JComponent {
        resetValues()
        return panel {
            group(message("settings.tomcat.group")) {
                row {
                    checkBox(message("settings.tomcat.open.browser"))
                        .bindSelected({ openInBrowser }, { openInBrowser = it })
                }
                row(message("settings.tomcat.frame.deactivation")) {
                    comboBox(TomcatFrameDeactivationAction.entries)
                        .bindItem(
                            { frameDeactivationAction },
                            { frameDeactivationAction = it ?: TomcatFrameDeactivationAction.UPDATE_RESOURCES },
                        )
                }
                row(message("settings.tomcat.update.action")) {
                    comboBox(TomcatUpdatingPolicy.entries)
                        .bindItem({ updatingPolicy }, { updatingPolicy = it ?: TomcatUpdatingPolicy.UPDATE_RESOURCES_AND_CLASSES })
                }
                row { comment(message("settings.tomcat.description")) }
            }
        }.also { form = it }
    }

    override fun isModified(): Boolean = form?.isModified() == true

    override fun apply() {
        form?.apply()
        TomcatRunConfigurationSettings.getInstance().save(
            TomcatRunConfigurationOptions(
                openInBrowser = openInBrowser,
                updateOnFrameDeactivation = frameDeactivationAction.updateOnFrameDeactivation,
                updateClassesOnFrameDeactivation = frameDeactivationAction.updateClassesOnFrameDeactivation,
                updatingPolicy = updatingPolicy.id,
            ),
        )
    }

    override fun reset() {
        resetValues()
        form?.reset()
    }

    override fun disposeUIResources() {
        form = null
    }

    private fun resetValues() {
        TomcatRunConfigurationSettings.getInstance().options().also {
            openInBrowser = it.openInBrowser
            frameDeactivationAction = TomcatFrameDeactivationAction.fromOptions(
                it.updateOnFrameDeactivation,
                it.updateClassesOnFrameDeactivation,
            )
            updatingPolicy = TomcatUpdatingPolicy.fromId(it.updatingPolicy)
        }
    }
}
