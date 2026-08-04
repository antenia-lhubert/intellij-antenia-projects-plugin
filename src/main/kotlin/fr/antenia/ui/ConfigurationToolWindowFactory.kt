package fr.antenia.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import fr.antenia.project.NeoProjectDetector

class ConfigurationToolWindowFactory : ToolWindowFactory {
    override fun shouldBeAvailable(project: Project): Boolean = NeoProjectDetector.detect(project) != null

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val neoProject = NeoProjectDetector.detect(project) ?: return
        val panel = ConfigurationPanel(project, neoProject)
        val content = ContentFactory.getInstance().createContent(panel.component, null, false)
        content.setDisposer(panel)
        toolWindow.contentManager.addContent(content)
    }
}
