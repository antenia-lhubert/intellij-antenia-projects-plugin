package fr.antenia.automation

import com.intellij.compiler.CompilerConfiguration
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.JavaSdk
import com.intellij.openapi.projectRoots.ProjectJdkTable
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.openapi.wm.ToolWindowManager
import fr.antenia.config.ConfigurationFiles
import fr.antenia.credentials.GlobalDatabaseCredentialsSynchronizer
import fr.antenia.credentials.GlobalDatabaseSettings
import fr.antenia.database.DatabaseProfileSynchronizer
import fr.antenia.project.NeoProject
import fr.antenia.project.NeoProjectDetector
import org.jetbrains.idea.maven.project.MavenProjectsManager

class NeoProjectActivity : ProjectActivity {
    override suspend fun execute(project: Project) {
        MavenProjectsManager.getInstance(project).addManagerListener(object : MavenProjectsManager.Listener {
            override fun projectImportCompleted() = scheduleConfiguration(project)
        }, project)
        scheduleConfiguration(project)
    }

    private fun scheduleConfiguration(project: Project) {
        ApplicationManager.getApplication().invokeLater {
            if (project.isDisposed) return@invokeLater
            val neoProject = NeoProjectDetector.detect(project) ?: return@invokeLater
            ToolWindowManager.getInstance(project).getToolWindow("Neo Configuration")?.setAvailable(true)
            ConfigurationFiles.ensureCreated(project, neoProject.type)
            GlobalDatabaseCredentialsSynchronizer.update(project, GlobalDatabaseSettings.getInstance().credentials())
            DatabaseProfileSynchronizer.update(project, neoProject)
            configureProject(project, neoProject)
            NeoRunConfigurationManager.configure(project, neoProject)
        }
    }

    private fun configureProject(project: Project, neoProject: NeoProject) {
        CompilerConfiguration.getInstance(project).setBuildProcessHeapSize(4096)
        val sdk = chooseSdk(neoProject.javaVersion) ?: return
        if (ProjectRootManager.getInstance(project).projectSdk == sdk) return
        ApplicationManager.getApplication().runWriteAction {
            ProjectRootManager.getInstance(project).projectSdk = sdk
        }
    }

    internal fun chooseSdk(javaVersion: Int): Sdk? = ProjectJdkTable.getInstance().allJdks
        .asSequence()
        .filter { it.sdkType is JavaSdk }
        .filter { sdkMajor(it) == javaVersion }
        .sortedWith(compareByDescending<Sdk> { isTemurin(it) }.thenByDescending { sdkMajor(it) })
        .firstOrNull()

    private fun sdkMajor(sdk: Sdk): Int = Regex("(?:1\\.)?(\\d+)").find(sdk.versionString.orEmpty())?.groupValues?.get(1)?.toIntOrNull() ?: 0

    private fun isTemurin(sdk: Sdk): Boolean {
        val description = "${sdk.name} ${sdk.versionString}".lowercase()
        return "temurin" in description || "adoptium" in description
    }
}
