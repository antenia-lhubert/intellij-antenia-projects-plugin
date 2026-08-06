package fr.antenia.automation

import com.google.gson.JsonParser
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.configurations.PathEnvironmentVariableUtil
import com.intellij.execution.process.CapturingProcessHandler
import com.intellij.ide.AppLifecycleListener
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.projectRoots.JavaSdk
import com.intellij.openapi.projectRoots.ProjectJdkTable
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.util.io.FileUtil
import com.intellij.util.concurrency.AppExecutorUtil
import fr.antenia.notifications.AnteniaNotifications
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.io.path.name

class JdkApplicationListener : AppLifecycleListener {
    override fun appStarted() {
        AppExecutorUtil.getAppExecutorService().execute(JdkAutoConfigurator::configure)
    }
}

internal object JdkAutoConfigurator {
    private const val MISE_TIMEOUT_MS = 30_000
    private val logger = Logger.getInstance(JdkAutoConfigurator::class.java)
    private val started = AtomicBoolean()
    private val completion = CompletableFuture<Unit>()
    private val configurationLock = Any()

    fun configure() {
        if (!started.compareAndSet(false, true)) {
            completion.join()
            return
        }
        try {
            reapply()
        } finally {
            completion.complete(Unit)
        }
    }

    fun reapply() = synchronized(configurationLock) {
        try {
            configureJdks()
        } catch (exception: Exception) {
            logger.warn("Global JDK auto-configuration failed", exception)
            AnteniaNotifications.failure(
                null,
                "jdk-global-configuration",
                "JDK automatic configuration failed",
                "${exception.message ?: exception.javaClass.simpleName}. See the IDE log for details.",
            )
        }
    }

    private fun configureJdks() {
        val candidates = linkedMapOf<Path, Boolean>()
        discoverInstalledJdks(candidates)
        discoverMiseJdks(candidates)

        val javaSdk = JavaSdk.getInstance()
        val table = ProjectJdkTable.getInstance()
        val configuredSdks = table.allJdks.toMutableList()
        val configuredNames = table.allJdks.map { it.name }.toMutableSet()

        candidates.forEach { (home, isMise) ->
            val homeString = home.toString()
            val configuredSdk = configuredSdks.firstOrNull { FileUtil.pathsEqual(it.homePath, homeString) }
            if (configuredSdk != null) {
                if (isMise) ensureMiseName(configuredSdk, home, javaSdk, configuredNames)
                logger.info("JDK installation is already configured: $home")
                return@forEach
            }
            try {
                if (!javaSdk.isValidSdkHome(homeString)) error("IntelliJ does not recognize this directory as a JDK")
                val suggestedName = javaSdk.suggestSdkName(null, homeString).ifBlank { home.name }
                val name = jdkName(suggestedName, isMise, configuredNames)
                val sdk = javaSdk.createJdk(name, homeString, false)
                WriteAction.runAndWait<RuntimeException> { table.addJdk(sdk) }
                configuredSdks += sdk
                configuredNames += name
                logger.info("Configured JDK '$name' from $home (${sdk.versionString})")
            } catch (exception: Exception) {
                logger.warn("Could not configure JDK installation at $home", exception)
                AnteniaNotifications.failure(
                    null,
                    "jdk-registration-${home.normalizedKey()}",
                    "JDK installation could not be configured",
                    "$home: ${exception.message ?: exception.javaClass.simpleName}. See the IDE log for details.",
                )
            }
        }
    }

    private fun ensureMiseName(sdk: Sdk, home: Path, javaSdk: JavaSdk, names: MutableSet<String>) {
        val suggestedName = javaSdk.suggestSdkName(null, home.toString()).ifBlank { home.name }
        val oldName = sdk.name
        if (!shouldRenameMiseJdk(oldName, suggestedName)) return
        val name = jdkName(suggestedName, true, names - oldName)
        if (name == oldName) return
        WriteAction.runAndWait<RuntimeException> {
            sdk.sdkModificator.apply {
                this.name = name
                commitChanges()
            }
        }
        names -= oldName
        names += name
        logger.info("Renamed mise-managed JDK '$oldName' to '$name'")
    }

    private fun discoverInstalledJdks(candidates: MutableMap<Path, Boolean>) {
        val programFiles = System.getenv("PROGRAMFILES")
        if (programFiles.isNullOrBlank()) {
            logger.info("PROGRAMFILES is not available; skipping Program Files JDK discovery")
        } else {
            val root = Path.of(programFiles)
            discoverJdkDirectory(root.resolve("Java"), "Program Files Java", candidates)
            discoverJdkDirectory(root.resolve("Eclipse Adoptium"), "Program Files Eclipse Adoptium", candidates)
        }

        val localAppData = System.getenv("LOCALAPPDATA")
        if (localAppData.isNullOrBlank()) {
            logger.info("LOCALAPPDATA is not available; skipping local Eclipse Adoptium JDK discovery")
            return
        }
        discoverJdkDirectory(
            Path.of(localAppData).resolve("Programs").resolve("Eclipse Adoptium"),
            "Local Eclipse Adoptium",
            candidates,
        )
    }

    private fun discoverJdkDirectory(directory: Path, source: String, candidates: MutableMap<Path, Boolean>) {
        if (!Files.isDirectory(directory)) {
            logger.info("JDK directory does not exist; skipping discovery: $directory")
            return
        }
        try {
            Files.list(directory).use { entries ->
                entries.filter { it.name.startsWith("jdk", ignoreCase = true) }.forEach { candidate ->
                    addCandidate(candidate, false, source, candidates)
                }
            }
        } catch (exception: Exception) {
            discoveryFailure(source.lowercase().replace(' ', '-'), "Could not inspect $directory", exception)
        }
    }

    private fun discoverMiseJdks(candidates: MutableMap<Path, Boolean>) {
        val mise = PathEnvironmentVariableUtil.findExecutableInPathOnAnyOS("mise")
        if (mise == null) {
            logger.info("mise is not available; skipping mise-managed JDK discovery")
            return
        }
        try {
            val output = CapturingProcessHandler(
                GeneralCommandLine(mise.absolutePath, "ls", "java", "--json"),
            ).runProcess(MISE_TIMEOUT_MS)
            if (output.isTimeout) error("mise timed out after ${MISE_TIMEOUT_MS / 1000} seconds")
            if (output.exitCode != 0) error(output.stderr.trim().ifEmpty { "mise exited with code ${output.exitCode}" })
            parseMiseInstallPaths(output.stdout).forEach { addCandidate(it, true, "mise", candidates) }
        } catch (exception: Exception) {
            discoveryFailure("mise", "Could not discover JDK installations with mise", exception)
        }
    }

    private fun addCandidate(candidate: Path, isMise: Boolean, source: String, candidates: MutableMap<Path, Boolean>) {
        val resolved = resolveJdkHomes(candidate)
        if (resolved.isEmpty()) {
            discoveryFailure(source.lowercase().replace(' ', '-'), "$candidate is not a valid JDK installation", null)
            return
        }
        resolved.forEach { candidates.putIfAbsent(it, isMise) }
    }

    private fun discoveryFailure(source: String, message: String, exception: Exception?) {
        if (exception == null) logger.warn(message) else logger.warn(message, exception)
        val detail = exception?.let { ": ${it.message ?: it.javaClass.simpleName}" }.orEmpty()
        AnteniaNotifications.failure(
            null,
            "jdk-discovery-$source-${message.hashCode()}",
            "JDK installation detection failed",
            "$message$detail. See the IDE log for details.",
        )
    }

    internal fun parseMiseInstallPaths(json: String): List<Path> = JsonParser.parseString(json).asJsonArray
        .filter { entry ->
            val installed = entry.asJsonObject.get("installed")
            installed == null || installed.asBoolean
        }
        .mapNotNull { it.asJsonObject.get("install_path")?.asString }
        .map(Path::of)

    internal fun resolveJdkHomes(candidate: Path): List<Path> {
        val normalized = candidate.toAbsolutePath().normalize()
        if (isJdkHome(normalized)) return listOf(normalized)
        if (!Files.isDirectory(normalized)) return emptyList()
        return Files.list(normalized).use { children ->
            children.filter(::isJdkHome).map { it.toAbsolutePath().normalize() }.toList()
        }
    }

    internal fun jdkName(suggestedName: String, isMise: Boolean, existingNames: Set<String>): String {
        val baseName = if (isMise) "$suggestedName (mise)" else suggestedName
        if (baseName !in existingNames) return baseName
        return generateSequence(2) { it + 1 }
            .map { "$baseName ($it)" }
            .first { it !in existingNames }
    }

    internal fun shouldRenameMiseJdk(currentName: String, suggestedName: String): Boolean =
        currentName == suggestedName

    private fun isJdkHome(path: Path): Boolean =
        Files.isRegularFile(path.resolve("release")) &&
            (Files.isRegularFile(path.resolve("bin").resolve("java.exe")) ||
                Files.isRegularFile(path.resolve("bin").resolve("java"))) &&
            (Files.isRegularFile(path.resolve("bin").resolve("javac.exe")) ||
                Files.isRegularFile(path.resolve("bin").resolve("javac")))

    private fun Path.normalizedKey(): String = toAbsolutePath().normalize().toString().lowercase().hashCode().toString()
}
