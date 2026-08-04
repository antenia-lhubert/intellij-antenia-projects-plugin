package fr.antenia.config

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.project.Project
import com.intellij.util.xmlb.XmlSerializerUtil
import java.nio.file.Path

@Service(Service.Level.PROJECT)
@State(name = "AnteniaProjectConfiguration", storages = [Storage("antenia.xml")])
class ProjectConfigurationState : PersistentStateComponent<ProjectConfigurationState> {
    var overrideGlobalCredentials: Boolean = false

    override fun getState(): ProjectConfigurationState = this
    override fun loadState(state: ProjectConfigurationState) = XmlSerializerUtil.copyBean(state, this)

    fun root(project: Project): Path {
        return Path.of(requireNotNull(project.basePath)).resolve(".local")
    }

    companion object {
        fun getInstance(project: Project): ProjectConfigurationState = project.getService(ProjectConfigurationState::class.java)
    }
}
