package fr.antenia.project

import com.intellij.openapi.project.Project
import com.intellij.openapi.diagnostic.Logger
import java.nio.file.Files
import java.nio.file.Path
import fr.antenia.notifications.AnteniaNotifications
import fr.antenia.MyMessageBundle.message
import javax.xml.XMLConstants
import javax.xml.parsers.DocumentBuilderFactory

enum class NeoProjectType(
    private val displayNameKey: String,
    val artifactId: String,
    val directoryName: String,
    val configurationFile: String,
    val environmentVariable: String,
    val contextPath: String,
    val httpPort: Int,
    val httpsPort: Int,
    val jmxPort: Int,
) {
    CORE("project.type.core", "webapp-novanet", "novanet", "novanet.properties", "NOVANET_DIR", "novanet", 8080, 8443, 1999),
    GED("project.type.ged", "webapp-ged", "ged", "configuration.properties", "GED_DIR", "ged", 8081, 8444, 2000),
    SELFCARE("project.type.selfcare", "webapp-owlnet", "owlnet", "owlnet.properties", "OWLNET_DIR", "owlnet", 8082, 8445, 2001);

    val displayName: String get() = message(displayNameKey)
    val explodedArtifactName: String get() = "$artifactId:war exploded"
}

data class NeoProject(val type: NeoProjectType, val version: String, val javaVersion: Int, val hasReact: Boolean) {
    val tomcatVersion: String get() = NeoProjectDetector.tomcatVersion(version)
}

object NeoProjectDetector {
    private val logger = Logger.getInstance(NeoProjectDetector::class.java)
    private val javaProperties = listOf(
        "java.version",
        "jdk.version",
        "version.compiler",
        "maven.compiler.release",
        "maven.compiler.target",
        "maven.compiler.source",
    )

    fun detect(project: Project): NeoProject? {
        val basePath = project.basePath
        if (basePath == null) {
            logger.debug("Neo project detection skipped for '${project.name}': no base path")
            return null
        }
        return detect(Path.of(basePath)) { exception ->
            AnteniaNotifications.failure(
                project,
                "project-detection",
                message("project.detection.failure.title"),
                message("project.detection.failure.message", exception.message ?: exception.javaClass.simpleName),
            )
        }
    }

    fun detect(projectRoot: Path): NeoProject? = detect(projectRoot, null)

    private fun detect(projectRoot: Path, reportFailure: ((Throwable) -> Unit)?): NeoProject? {
        val pom = projectRoot.resolve("pom.xml")
        if (!Files.isRegularFile(pom)) {
            logger.debug("Neo project detection skipped: no root pom.xml at $pom")
            return null
        }
        return runCatching {
            val factory = DocumentBuilderFactory.newInstance().apply {
                setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
                setFeature("http://xml.org/sax/features/external-general-entities", false)
                setFeature("http://xml.org/sax/features/external-parameter-entities", false)
                setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "")
                setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "")
            }
            val document = Files.newInputStream(pom).use { factory.newDocumentBuilder().parse(it) }
            val root = document.documentElement
            val artifactId = root.childText("artifactId")
            if (artifactId == null) {
                logger.debug("Neo project detection skipped for $projectRoot: root artifactId is absent")
                return@runCatching null
            }
            val type = NeoProjectType.entries.firstOrNull { it.artifactId == artifactId }
            if (type == null) {
                logger.debug("Neo project detection skipped for $projectRoot: unsupported root artifactId=$artifactId")
                return@runCatching null
            }
            val properties = root.child("properties")
            val propertyValues = properties?.children()?.associate { it.tagName to it.textContent.trim() }.orEmpty()
            val rawJavaVersion = javaProperties.firstNotNullOfOrNull { propertyValues[it] }?.let { resolveProperty(it, propertyValues) }
            val javaVersion = rawJavaVersion?.let(::parseJavaVersion) ?: 8
            val rawProjectVersion = root.childText("version")?.let { resolveProperty(it, propertyValues) }
            val version = rawProjectVersion
                ?.takeUnless { it == UNDETECTED_PROJECT_VERSION }
                ?.takeIf { PROJECT_VERSION_PATTERN.matches(it) }
                ?: inferProjectVersion(javaVersion)
            val reactDirectory = when (type) {
                NeoProjectType.CORE -> "novanet-react"
                else -> null
            }
            NeoProject(type, version, javaVersion, reactDirectory != null && Files.isRegularFile(projectRoot.resolve(reactDirectory).resolve("package.json"))).also {
                logger.debug(
                    "Neo project detected at $projectRoot: type=${it.type}, artifactId=$artifactId, " +
                        "version=${it.version}, rawVersion=${rawProjectVersion ?: "undetected"}, " +
                        "java=${it.javaVersion}, rawJavaVersion=${rawJavaVersion ?: "default"}, react=${it.hasReact}",
                )
            }
        }.onFailure {
            logger.warn("Unable to detect Neo project from $pom", it)
            reportFailure?.invoke(it)
        }.getOrNull()
    }

    internal fun parseJavaVersion(value: String): Int {
        val normalized = value.trim().removePrefix("1.")
        return normalized.takeWhile(Char::isDigit).toIntOrNull() ?: 8
    }

    internal fun tomcatVersion(projectVersion: String): String {
        val match = PROJECT_VERSION_PATTERN.matchEntire(projectVersion)
            ?: error("Unsupported Neo project version: $projectVersion")
        val major = match.groupValues[1].toInt()
        val minor = match.groupValues[2].toInt()
        return when {
            major > 1 || major == 1 && minor >= 6 -> "11"
            major == 1 && minor >= 5 -> "10.1"
            else -> "9"
        }
    }

    // Temporary gap fill for projects that still expose the legacy placeholder version.
    private fun inferProjectVersion(javaVersion: Int): String = when {
        javaVersion >= 25 -> "1.6+"
        javaVersion >= 17 -> "1.5"
        else -> "1.1-1.4"
    }

    private fun resolveProperty(value: String, properties: Map<String, String>): String {
        var resolved = value
        repeat(10) {
            val reference = Regex("^\\$\\{([^}]+)}$").matchEntire(resolved)?.groupValues?.get(1) ?: return resolved
            resolved = properties[reference] ?: return resolved
        }
        return resolved
    }

    private const val UNDETECTED_PROJECT_VERSION = "1.0-SNAPSHOT"
    private val PROJECT_VERSION_PATTERN = Regex("^(\\d+)\\.(\\d+)(?:[.+-].*)?$")
}

private fun org.w3c.dom.Element.child(name: String): org.w3c.dom.Element? =
    (0 until childNodes.length)
        .map { childNodes.item(it) }
        .filterIsInstance<org.w3c.dom.Element>()
        .firstOrNull { it.localName == name || it.tagName == name }

private fun org.w3c.dom.Element.childText(name: String): String? = child(name)?.textContent?.trim()?.takeIf { it.isNotEmpty() }

private fun org.w3c.dom.Element.children(): List<org.w3c.dom.Element> =
    (0 until childNodes.length).map { childNodes.item(it) }.filterIsInstance<org.w3c.dom.Element>()
