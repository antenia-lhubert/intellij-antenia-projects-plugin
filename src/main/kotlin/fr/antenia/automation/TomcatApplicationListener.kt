package fr.antenia.automation

import com.google.gson.JsonParser
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.configurations.PathEnvironmentVariableUtil
import com.intellij.execution.process.CapturingProcessHandler
import com.intellij.ide.AppLifecycleListener
import com.intellij.javaee.appServers.appServerIntegrations.ApplicationServer
import com.intellij.javaee.appServers.serverInstances.ApplicationServersManager
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.io.FileUtil
import com.intellij.util.concurrency.AppExecutorUtil
import fr.antenia.notifications.AnteniaNotifications
import org.jetbrains.idea.tomcat.server.TomcatIntegration
import org.jetbrains.idea.tomcat.server.TomcatPersistentData
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.io.path.name

class TomcatApplicationListener : AppLifecycleListener {
    override fun appStarted() {
        if (!started.compareAndSet(false, true)) return
        AppExecutorUtil.getAppExecutorService().execute(::reapply)
    }

    private fun configureTomcatServers() {
        val homes = linkedSetOf<Path>()
        discoverToolsInstallations(Path.of(TOOLS_DIRECTORY), homes)
        discoverMiseInstallations(homes)

        val integration = TomcatIntegration.getInstance()
        val manager = ApplicationServersManager.getInstance()
        val configuredHomes = manager.getApplicationServers(integration)
            .mapNotNull { (it.persistentData as? TomcatPersistentData)?.HOME }
            .toMutableList()

        homes.forEach { home ->
            val homeString = home.toString()
            if (configuredHomes.any { FileUtil.pathsEqual(it, homeString) }) {
                logger.info("Tomcat installation is already configured: $home")
                return@forEach
            }
            try {
                val data = TomcatPersistentData().apply {
                    HOME = homeString
                    BASE = ""
                }
                val version = integration.getServerVersion(data)
                val server = createServer(manager, integration, data, version, home)
                configuredHomes += homeString
                logger.info("Configured ${server.name} from $home (version $version)")
            } catch (exception: Exception) {
                logger.warn("Could not configure Tomcat installation at $home", exception)
                AnteniaNotifications.failure(
                    null,
                    "tomcat-registration-${home.normalizedKey()}",
                    "Tomcat installation could not be configured",
                    "$home: ${exception.message ?: exception.javaClass.simpleName}. See the IDE log for details.",
                )
            }
        }
    }

    private fun createServer(
        manager: ApplicationServersManager,
        integration: TomcatIntegration,
        data: TomcatPersistentData,
        version: String,
        home: Path,
    ): ApplicationServer {
        val name = serverName(version, home, manager.applicationServers.map { it.name }.toSet())
        val libraries = integration.applicationServerHelper.getApplicationServerInfo(data).defaultLibraries
        val model = manager.createModifiableModel()
        try {
            val server = model.createNewApplicationServer(name, libraries, data)
            server.setIntegration(integration)
            WriteAction.runAndWait<RuntimeException>(model::commit)
            return server
        } catch (exception: Exception) {
            model.dispose()
            throw exception
        }
    }

    private fun discoverToolsInstallations(toolsDirectory: Path, homes: MutableSet<Path>) {
        if (!Files.isDirectory(toolsDirectory)) return
        try {
            Files.list(toolsDirectory).use { entries ->
                entries.filter { it.name.startsWith("apache-tomcat-", ignoreCase = true) }.forEach { candidate ->
                    addCandidate(candidate, "tools", homes)
                }
            }
        } catch (exception: Exception) {
            discoveryFailure("tools", "Could not inspect $toolsDirectory", exception)
        }
    }

    private fun discoverMiseInstallations(homes: MutableSet<Path>) {
        val mise = PathEnvironmentVariableUtil.findExecutableInPathOnAnyOS("mise")
        if (mise == null) {
            logger.info("mise is not available; skipping mise-managed Tomcat discovery")
            return
        }
        try {
            val output = CapturingProcessHandler(
                GeneralCommandLine(mise.absolutePath, "ls", "tomcat", "--json"),
            ).runProcess(MISE_TIMEOUT_MS)
            if (output.isTimeout) error("mise timed out after ${MISE_TIMEOUT_MS / 1000} seconds")
            if (output.exitCode != 0) error(output.stderr.trim().ifEmpty { "mise exited with code ${output.exitCode}" })
            parseMiseInstallPaths(output.stdout).forEach { addCandidate(it, "mise", homes) }
        } catch (exception: Exception) {
            discoveryFailure("mise", "Could not discover Tomcat installations with mise", exception)
        }
    }

    private fun addCandidate(candidate: Path, source: String, homes: MutableSet<Path>) {
        val resolved = resolveTomcatHomes(candidate)
        if (resolved.isEmpty()) {
            discoveryFailure(source, "$candidate is not a valid Tomcat installation", null)
            return
        }
        homes += resolved
    }

    private fun discoveryFailure(source: String, message: String, exception: Exception?) {
        if (exception == null) logger.warn(message) else logger.warn(message, exception)
        val detail = exception?.let { ": ${it.message ?: it.javaClass.simpleName}" }.orEmpty()
        AnteniaNotifications.failure(
            null,
            "tomcat-discovery-$source-${message.hashCode()}",
            "Tomcat installation detection failed",
            "$message$detail. See the IDE log for details.",
        )
    }

    companion object {
        private const val TOOLS_DIRECTORY = "C:\\tools"
        private const val MISE_TIMEOUT_MS = 30_000
        private val logger = Logger.getInstance(TomcatApplicationListener::class.java)
        private val started = AtomicBoolean()
        private val configurationLock = Any()

        internal fun reapply() = synchronized(configurationLock) {
            TomcatApplicationListener().configureTomcatServers()
        }

        internal fun parseMiseInstallPaths(json: String): List<Path> = JsonParser.parseString(json).asJsonArray
            .filter { entry ->
                val installed = entry.asJsonObject.get("installed")
                installed == null || installed.asBoolean
            }
            .mapNotNull { it.asJsonObject.get("install_path")?.asString }
            .map(Path::of)

        internal fun resolveTomcatHomes(candidate: Path): List<Path> {
            val normalized = candidate.toAbsolutePath().normalize()
            if (isTomcatHome(normalized)) return listOf(normalized)
            if (!Files.isDirectory(normalized)) return emptyList()
            return Files.list(normalized).use { children ->
                children.filter(::isTomcatHome).map { it.toAbsolutePath().normalize() }.toList()
            }
        }

        internal fun serverName(version: String, home: Path, existingNames: Set<String>): String {
            val defaultName = "Tomcat $version"
            val normalizedHome = home.toAbsolutePath().normalize().toString().lowercase()
            if ("\\mise\\installs\\tomcat\\" in normalizedHome) {
                val miseName = "$defaultName (mise)"
                if (miseName !in existingNames) return miseName
                return generateSequence(2) { it + 1 }
                    .map { "$miseName ($it)" }
                    .first { it !in existingNames }
            }
            if (defaultName !in existingNames) return defaultName
            val source = when {
                normalizedHome.startsWith(TOOLS_DIRECTORY.lowercase()) -> TOOLS_DIRECTORY
                else -> home.parent?.fileName?.toString() ?: "local"
            }
            val qualifiedName = "$defaultName ($source)"
            if (qualifiedName !in existingNames) return qualifiedName
            return generateSequence(2) { it + 1 }
                .map { "$defaultName ($source $it)" }
                .first { it !in existingNames }
        }

        private fun isTomcatHome(path: Path): Boolean =
            Files.isRegularFile(path.resolve("lib").resolve("catalina.jar")) &&
                Files.isRegularFile(path.resolve("conf").resolve("server.xml")) &&
                (Files.isRegularFile(path.resolve("bin").resolve("catalina.bat")) ||
                    Files.isRegularFile(path.resolve("bin").resolve("catalina.sh")))

        private fun Path.normalizedKey(): String = toAbsolutePath().normalize().toString().lowercase().hashCode().toString()
    }
}
