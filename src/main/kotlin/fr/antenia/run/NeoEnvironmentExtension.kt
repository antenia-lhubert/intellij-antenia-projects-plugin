package fr.antenia.run

import com.intellij.execution.ExecutionException
import com.intellij.execution.RunConfigurationExtension
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.configurations.JavaParameters
import com.intellij.execution.configurations.RunConfigurationBase
import com.intellij.execution.configurations.RunnerSettings
import fr.antenia.config.ProjectConfigurationState
import fr.antenia.project.NeoProjectDetector

class NeoEnvironmentExtension : RunConfigurationExtension() {
    override fun isApplicableFor(configuration: RunConfigurationBase<*>): Boolean =
        NeoProjectDetector.detect(configuration.project) != null

    override fun isEnabledFor(configuration: RunConfigurationBase<*>, runnerSettings: RunnerSettings?): Boolean = true

    @Throws(ExecutionException::class)
    override fun <T : RunConfigurationBase<*>> updateJavaParameters(
        configuration: T,
        params: JavaParameters,
        runnerSettings: RunnerSettings?,
    ) {
        val environment = environment(configuration, params.env[JAVA_TOOL_OPTIONS]) ?: return
        params.env = params.env + environment
    }

    @Throws(ExecutionException::class)
    override fun patchCommandLine(
        configuration: RunConfigurationBase<*>,
        runnerSettings: RunnerSettings?,
        cmdLine: GeneralCommandLine,
        runnerId: String,
    ) {
        val environment = environment(configuration, cmdLine.environment[JAVA_TOOL_OPTIONS]) ?: return
        cmdLine.environment.putAll(environment)
    }

    private fun environment(configuration: RunConfigurationBase<*>, existingJavaToolOptions: String?): Map<String, String>? {
        val neoProject = NeoProjectDetector.detect(configuration.project) ?: return null
        val root = ProjectConfigurationState.getInstance(configuration.project)
            .root(configuration.project)
            .toAbsolutePath()
            .normalize()
            .toString()
            .replace('\\', '/') + "/"
        return mapOf(
            neoProject.type.environmentVariable to root,
            JAVA_TOOL_OPTIONS to appendRmiHostname(existingJavaToolOptions),
        )
    }
}

internal const val JAVA_TOOL_OPTIONS = "JAVA_TOOL_OPTIONS"
internal const val RMI_HOSTNAME_OPTION = "-Djava.rmi.server.hostname=127.0.0.1"

internal fun appendRmiHostname(existing: String?): String {
    val options = existing.orEmpty().trim()
    return when {
        RMI_HOSTNAME_OPTION in options.split(Regex("\\s+")).filter(String::isNotEmpty) -> options
        options.isEmpty() -> RMI_HOSTNAME_OPTION
        else -> "$options $RMI_HOSTNAME_OPTION"
    }
}
