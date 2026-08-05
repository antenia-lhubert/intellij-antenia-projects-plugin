package fr.antenia.commit

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage

@Service(Service.Level.APP)
@State(name = "AnteniaCommitTemplates", storages = [Storage("antenia.xml")])
class CommitTemplateSettings : PersistentStateComponent<CommitTemplateSettings.SettingsState> {
    private var settingsState = SettingsState()

    override fun getState(): SettingsState = settingsState

    override fun loadState(state: SettingsState) {
        settingsState = state
    }

    fun templates(): List<CommitTemplate> = CommitTemplates.withDefaults(
        settingsState.templates.map { CommitTemplate(it.name, it.content) },
    )

    fun replaceTemplates(templates: List<CommitTemplate>) {
        settingsState.templates = CommitTemplates.customOnly(templates)
            .map { StoredTemplate(it.name, it.content) }
            .toMutableList()
    }

    class SettingsState {
        var templates: MutableList<StoredTemplate> = mutableListOf()
    }

    class StoredTemplate() {
        var name: String = ""
        var content: String = ""

        constructor(name: String, content: String) : this() {
            this.name = name
            this.content = content
        }
    }

    companion object {
        fun getInstance(): CommitTemplateSettings =
            ApplicationManager.getApplication().getService(CommitTemplateSettings::class.java)
    }
}
