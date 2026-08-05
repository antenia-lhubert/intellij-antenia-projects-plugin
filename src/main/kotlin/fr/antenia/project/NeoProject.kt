package fr.antenia.project

import com.intellij.openapi.project.Project
import com.intellij.openapi.diagnostic.Logger
import java.nio.file.Files
import java.nio.file.Path
import fr.antenia.notifications.AnteniaNotifications
import javax.xml.XMLConstants
import javax.xml.parsers.DocumentBuilderFactory

enum class NeoProjectType(
    val displayName: String,
    val artifactId: String,
    val directoryName: String,
    val configurationFile: String,
    val environmentVariable: String,
    val contextPath: String,
    val httpPort: Int,
    val httpsPort: Int,
    val jmxPort: Int,
) {
    CORE("Neo Core", "webapp-novanet", "novanet", "novanet.properties", "NOVANET_DIR", "novanet", 8080, 8443, 1999),
    GED("Neo GED", "webapp-ged", "ged", "configuration.properties", "GED_DIR", "ged", 8081, 8444, 2000),
    SELFCARE("Neo Selfcare", "webapp-owlnet", "owlnet", "owlnet.properties", "OWLNET_DIR", "owlnet", 8082, 8445, 2001);

    val explodedArtifactName: String get() = "$artifactId:war exploded"
}

data class NeoProject(val type: NeoProjectType, val javaVersion: Int, val hasReact: Boolean)

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
                "Neo project detection failed",
                "The root pom.xml could not be inspected: ${exception.message ?: exception.javaClass.simpleName}. See the IDE log for details.",
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
            val reactDirectory = when (type) {
                NeoProjectType.CORE -> "novanet-react"
                else -> null
            }
            NeoProject(type, javaVersion, reactDirectory != null && Files.isRegularFile(projectRoot.resolve(reactDirectory).resolve("package.json"))).also {
                logger.debug(
                    "Neo project detected at $projectRoot: type=${it.type}, artifactId=$artifactId, " +
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

    private fun resolveProperty(value: String, properties: Map<String, String>): String {
        var resolved = value
        repeat(10) {
            val reference = Regex("^\\$\\{([^}]+)}$").matchEntire(resolved)?.groupValues?.get(1) ?: return resolved
            resolved = properties[reference] ?: return resolved
        }
        return resolved
    }
}

private fun org.w3c.dom.Element.child(name: String): org.w3c.dom.Element? =
    (0 until childNodes.length)
        .map { childNodes.item(it) }
        .filterIsInstance<org.w3c.dom.Element>()
        .firstOrNull { it.localName == name || it.tagName == name }

private fun org.w3c.dom.Element.childText(name: String): String? = child(name)?.textContent?.trim()?.takeIf { it.isNotEmpty() }

private fun org.w3c.dom.Element.children(): List<org.w3c.dom.Element> =
    (0 until childNodes.length).map { childNodes.item(it) }.filterIsInstance<org.w3c.dom.Element>()
