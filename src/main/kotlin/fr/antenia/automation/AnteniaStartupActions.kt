package fr.antenia.automation

import com.intellij.openapi.project.Project
import com.intellij.openapi.diagnostic.Logger
import com.intellij.util.concurrency.AppExecutorUtil

object AnteniaStartupActions {
    private val logger = Logger.getInstance(AnteniaStartupActions::class.java)

    fun reapply(project: Project, completed: ((Boolean) -> Unit)? = null) {
        AppExecutorUtil.getAppExecutorService().execute {
            val jdkResult = runCatching(JdkAutoConfigurator::reapply)
                .onFailure { logger.error("Unable to reapply JDK startup actions", it) }
            val tomcatResult = runCatching(TomcatApplicationListener::reapply)
                .onFailure { logger.error("Unable to reapply Tomcat startup actions", it) }
            val registrySucceeded = RegistryAutoConfigurator.configure()
            val subversionSucceeded = SubversionRepositoryAutoConfigurator.configure()
            NeoProjectActivity().scheduleConfiguration(project, "startup actions manually reapplied") { projectSucceeded ->
                completed?.invoke(
                    jdkResult.isSuccess && tomcatResult.isSuccess && registrySucceeded && subversionSucceeded && projectSucceeded,
                )
            }
        }
    }
}
