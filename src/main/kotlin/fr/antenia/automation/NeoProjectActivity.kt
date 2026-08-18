package fr.antenia.automation

import com.intellij.compiler.CompilerConfiguration
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
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
import fr.antenia.notifications.AnteniaNotifications
import fr.antenia.MyMessageBundle.message
import fr.antenia.project.NeoProject
import fr.antenia.project.NeoProjectDetector
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.idea.maven.project.MavenProjectsManager

class NeoProjectActivity : ProjectActivity {
    private val logger = Logger.getInstance(NeoProjectActivity::class.java)

    override suspend fun execute(project: Project) {
        CodingStyleConfigurator.configure(project)
        SubversionRepositoryAutoConfigurator.configure()
        withContext(Dispatchers.IO) {
            JdkAutoConfigurator.configure()
            TomcatApplicationListener.configure()
        }
        logger.info("Installing Neo project automation for '${project.name}'")
        MavenProjectsManager.getInstance(project).addManagerListener(object : MavenProjectsManager.Listener {
            override fun projectImportCompleted() = scheduleConfiguration(project, "Maven import completed")
        }, project)
        scheduleConfiguration(project, "project opened")
    }

    internal fun scheduleConfiguration(project: Project, reason: String, completed: ((Boolean) -> Unit)? = null) {
        logger.info("Scheduling Neo project configuration for '${project.name}': $reason")
        ApplicationManager.getApplication().invokeLater {
            if (project.isDisposed) {
                logger.info("Neo project configuration cancelled for '${project.name}': project is disposed")
                completed?.invoke(false)
                return@invokeLater
            }
            val neoProject = NeoProjectDetector.detect(project)
            if (neoProject == null) {
                logger.info("Neo project configuration skipped for '${project.name}': no supported root artifact detected")
                completed?.invoke(false)
                return@invokeLater
            }
            logger.info(
                "Detected ${neoProject.type.displayName} project '${project.name}': " +
                    "artifactId=${neoProject.type.artifactId}, version=${neoProject.version}, " +
                    "java=${neoProject.javaVersion}, react=${neoProject.hasReact}",
            )
            var succeeded = false
            try {
                ToolWindowManager.getInstance(project).getToolWindow("Neo Configuration")?.setAvailable(true)
                logger.info("Enabled Neo Configuration tool window for '${project.name}'")
                ConfigurationFiles.ensureCreated(project, neoProject.type)
                GlobalDatabaseCredentialsSynchronizer.update(project, GlobalDatabaseSettings.getInstance().credentials())
                DatabaseProfileSynchronizer.update(project, neoProject)
                configureProject(project, neoProject)
                NeoRunConfigurationManager.configure(project, neoProject)
                logger.info("Completed Neo project configuration for '${project.name}'")
                succeeded = true
            } catch (exception: Exception) {
                logger.error("Neo project configuration failed for '${project.name}'", exception)
                AnteniaNotifications.failure(
                    project,
                    "automatic-configuration",
                    message("notification.automatic.configuration.failure.title"),
                    message("common.error.details", exception.message ?: exception.javaClass.simpleName),
                )
                throw exception
            } finally {
                completed?.invoke(succeeded)
            }
        }
    }

    private fun configureProject(project: Project, neoProject: NeoProject) {
        CompilerConfiguration.getInstance(project).setBuildProcessHeapSize(4096)
        logger.info("Set compiler build process heap to 4096 MB for '${project.name}'")
        val sdk = chooseSdk(neoProject.javaVersion)
        if (sdk == null) {
            logger.warn("No Java ${neoProject.javaVersion} SDK found for '${project.name}'; project SDK was not changed")
            AnteniaNotifications.failure(
                project,
                "missing-java-sdk-${neoProject.javaVersion}",
                message("notification.java.sdk.missing.title"),
                message("notification.java.sdk.missing.message", neoProject.javaVersion),
            )
            return
        }
        if (ProjectRootManager.getInstance(project).projectSdk == sdk) {
            logger.info("Project SDK already matches Java ${neoProject.javaVersion} for '${project.name}': ${sdk.name}")
            return
        }
        ApplicationManager.getApplication().runWriteAction {
            ProjectRootManager.getInstance(project).projectSdk = sdk
        }
        logger.info("Selected project SDK for '${project.name}': ${sdk.name} (${sdk.versionString})")
    }

    internal fun chooseSdk(javaVersion: Int): Sdk? = ProjectJdkTable.getInstance().allJdks
        .asSequence()
        .filter { it.sdkType is JavaSdk }
        .filter { sdkMajor(it) == javaVersion }
        .sortedWith { first, second ->
            val vendorComparison = isTemurin(second).compareTo(isTemurin(first))
            if (vendorComparison != 0) vendorComparison else compareVersions(sdkVersionNumbers(second), sdkVersionNumbers(first))
        }
        .firstOrNull()

    private fun sdkMajor(sdk: Sdk): Int = Regex("(?:1\\.)?(\\d+)").find(sdk.versionString.orEmpty())?.groupValues?.get(1)?.toIntOrNull() ?: 0

    private fun isTemurin(sdk: Sdk): Boolean {
        val description = "${sdk.name} ${sdk.versionString}".lowercase()
        return "temurin" in description || "adoptium" in description
    }

    private fun sdkVersionNumbers(sdk: Sdk): List<Int> = Regex("\\d+").findAll(sdk.versionString.orEmpty())
        .map { it.value.toInt() }
        .toList()

    private fun compareVersions(first: List<Int>, second: List<Int>): Int {
        for (index in 0 until maxOf(first.size, second.size)) {
            val comparison = first.getOrElse(index) { 0 }.compareTo(second.getOrElse(index) { 0 })
            if (comparison != 0) return comparison
        }
        return 0
    }
}
