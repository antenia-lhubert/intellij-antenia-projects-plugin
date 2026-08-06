package fr.antenia.settings

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import fr.antenia.MyMessageBundle.message

data class TomcatRunConfigurationOptions(
    val openInBrowser: Boolean,
    val updateOnFrameDeactivation: Boolean,
    val updateClassesOnFrameDeactivation: Boolean,
    val updatingPolicy: String,
)

enum class TomcatFrameDeactivationAction(
    private val label: String,
    val updateOnFrameDeactivation: Boolean,
    val updateClassesOnFrameDeactivation: Boolean,
) {
    NOTHING("settings.tomcat.action.nothing", false, false),
    UPDATE_RESOURCES("settings.tomcat.action.update.resources", true, false),
    UPDATE_RESOURCES_AND_CLASSES("settings.tomcat.action.update.resources.classes", true, true),
    ;

    override fun toString(): String = message(label)

    companion object {
        fun fromOptions(update: Boolean, updateClasses: Boolean): TomcatFrameDeactivationAction = when {
            !update -> NOTHING
            updateClasses -> UPDATE_RESOURCES_AND_CLASSES
            else -> UPDATE_RESOURCES
        }
    }
}

enum class TomcatUpdatingPolicy(val id: String, private val label: String) {
    UPDATE_RESOURCES("update-resources", "settings.tomcat.action.update.resources"),
    UPDATE_RESOURCES_AND_CLASSES("update-classes-and-resources", "settings.tomcat.action.update.resources.classes"),
    REDEPLOY("redeploy-artifacts", "settings.tomcat.action.redeploy"),
    RESTART_SERVER("restart-server", "settings.tomcat.action.restart"),
    ;

    override fun toString(): String = message(label)

    companion object {
        fun fromId(id: String): TomcatUpdatingPolicy = entries.firstOrNull { it.id == id } ?: UPDATE_RESOURCES_AND_CLASSES
    }
}

@Service(Service.Level.APP)
@State(name = "AnteniaTomcatRunConfigurations", storages = [Storage("antenia.xml")])
class TomcatRunConfigurationSettings : PersistentStateComponent<TomcatRunConfigurationSettings.SettingsState> {
    private var settingsState = SettingsState()

    override fun getState(): SettingsState = settingsState

    override fun loadState(state: SettingsState) {
        settingsState = state
    }

    fun options(): TomcatRunConfigurationOptions = TomcatRunConfigurationOptions(
        openInBrowser = settingsState.openInBrowser,
        updateOnFrameDeactivation = settingsState.updateOnFrameDeactivation,
        updateClassesOnFrameDeactivation = settingsState.updateClassesOnFrameDeactivation,
        updatingPolicy = settingsState.updatingPolicy,
    )

    fun save(options: TomcatRunConfigurationOptions) {
        settingsState.openInBrowser = options.openInBrowser
        settingsState.updateOnFrameDeactivation = options.updateOnFrameDeactivation
        settingsState.updateClassesOnFrameDeactivation = options.updateClassesOnFrameDeactivation
        settingsState.updatingPolicy = options.updatingPolicy
    }

    class SettingsState {
        var openInBrowser: Boolean = true
        var updateOnFrameDeactivation: Boolean = true
        var updateClassesOnFrameDeactivation: Boolean = false
        var updatingPolicy: String = TomcatUpdatingPolicy.UPDATE_RESOURCES_AND_CLASSES.id
    }

    companion object {
        fun getInstance(): TomcatRunConfigurationSettings =
            ApplicationManager.getApplication().getService(TomcatRunConfigurationSettings::class.java)
    }
}
