package fr.antenia.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import fr.antenia.project.NeoProjectDetector
import fr.antenia.notifications.AnteniaNotifications
import fr.antenia.MyMessageBundle.message

class ConfigurationToolWindowFactory : ToolWindowFactory {
    private val logger = Logger.getInstance(ConfigurationToolWindowFactory::class.java)

    override fun shouldBeAvailable(project: Project): Boolean = NeoProjectDetector.detect(project) != null

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val neoProject = NeoProjectDetector.detect(project) ?: return
        try {
            val panel = ConfigurationPanel(project, neoProject)
            val content = ContentFactory.getInstance().createContent(panel.component, null, false)
            content.setDisposer(panel)
            toolWindow.contentManager.addContent(content)
        } catch (exception: Exception) {
            logger.error("Unable to open the Neo Configuration tool window for '${project.name}'", exception)
            AnteniaNotifications.failure(
                project,
                "configuration-tool-window",
                message("configuration.toolwindow.failure.title"),
                message("common.error.details", exception.message ?: exception.javaClass.simpleName),
            )
            throw exception
        }
    }
}
