package fr.antenia.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import com.intellij.icons.AllIcons
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.JBUI
import fr.antenia.project.NeoProjectDetector
import fr.antenia.notifications.AnteniaNotifications
import fr.antenia.MyMessageBundle.message
import java.awt.BorderLayout
import javax.swing.JPanel

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
            val errorPanel = JPanel(BorderLayout()).apply {
                border = JBUI.Borders.empty(16)
                add(JBLabel(message("configuration.toolwindow.failure.message"), AllIcons.General.Warning, JBLabel.LEFT), BorderLayout.NORTH)
            }
            toolWindow.contentManager.addContent(ContentFactory.getInstance().createContent(errorPanel, null, false))
        }
    }
}
