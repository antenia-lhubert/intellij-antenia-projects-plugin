package fr.antenia.config

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Document
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import fr.antenia.project.NeoProjectType
import fr.antenia.MyMessageBundle.message
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

object ConfigurationFiles {
    private val logger = Logger.getInstance(ConfigurationFiles::class.java)

    fun propertyPath(project: Project, type: NeoProjectType): Path =
        ProjectConfigurationState.getInstance(project).root(project).resolve(type.directoryName).resolve(type.configurationFile)

    fun ensureCreated(project: Project, type: NeoProjectType): Path {
        val root = ProjectConfigurationState.getInstance(project).root(project)
        val directory = root.resolve(type.directoryName)
        Files.createDirectories(directory)
        copyTemplateIfMissing("/templates/${type.configurationFile}", directory.resolve(type.configurationFile))
        when (type) {
            NeoProjectType.CORE -> copyTemplateIfMissing("/templates/novaLog.xml", directory.resolve("novaLog.xml"))
            NeoProjectType.GED -> copyTemplateIfMissing("/templates/gedLog.xml", directory.resolve("gedLog.xml"))
            NeoProjectType.SELFCARE -> Unit
        }
        LocalFileSystem.getInstance().refreshAndFindFileByNioFile(directory)
        logger.debug("Ensured ${type.displayName} configuration directory exists: $directory")
        return directory.resolve(type.configurationFile)
    }

    fun read(path: Path): OrderedProperties = OrderedPropertiesCodec.parse(Files.readString(path))

    fun write(project: Project, path: Path, document: OrderedProperties) {
        val virtualFile = LocalFileSystem.getInstance().refreshAndFindFileByNioFile(path)
        if (virtualFile == null) {
            Files.writeString(path, OrderedPropertiesCodec.render(document))
            logger.debug("Wrote Neo configuration directly to $path")
            return
        }
        val fileDocument = ReadAction.compute<Document?, RuntimeException> {
            FileDocumentManager.getInstance().getDocument(virtualFile)
        }
        if (fileDocument == null) {
            logger.warn("Neo configuration was not written because no editor document was available: $path")
            error(message("configuration.document.unavailable", path))
        }
        WriteCommandAction.runWriteCommandAction(project, message("configuration.update.command"), null, Runnable {
            fileDocument.setText(renderForEditor(document))
            FileDocumentManager.getInstance().saveDocument(fileDocument)
        })
        logger.debug("Wrote Neo configuration through the editor document: $path")
    }

    fun reset(project: Project, type: NeoProjectType): Path {
        val path = propertyPath(project, type)
        Files.createDirectories(path.parent)
        javaClass.getResourceAsStream("/templates/${type.configurationFile}")!!.use { input ->
            Files.copy(input, path, StandardCopyOption.REPLACE_EXISTING)
        }
        ApplicationManager.getApplication().invokeLater {
            LocalFileSystem.getInstance().refreshAndFindFileByNioFile(path)?.refresh(false, false)
        }
        logger.info("Reset ${type.displayName} configuration to template: $path")
        return path
    }

    fun deleteManaged(project: Project, type: NeoProjectType) {
        val directory = propertyPath(project, type).parent
        val fileNames = when (type) {
            NeoProjectType.CORE -> listOf(type.configurationFile, "novaLog.xml")
            NeoProjectType.GED -> listOf(type.configurationFile, "gedLog.xml")
            NeoProjectType.SELFCARE -> listOf(type.configurationFile)
        }
        fileNames.forEach { fileName ->
            val path = directory.resolve(fileName)
            val virtualFile = LocalFileSystem.getInstance().findFileByNioFile(path)
            if (virtualFile != null) {
                ApplicationManager.getApplication().runWriteAction { virtualFile.delete(ConfigurationFiles) }
            } else {
                Files.deleteIfExists(path)
            }
        }
        logger.info("Deleted managed ${type.displayName} configuration files from $directory")
    }

    private fun copyTemplateIfMissing(resource: String, destination: Path) {
        if (Files.exists(destination)) return
        javaClass.getResourceAsStream(resource)!!.use { Files.copy(it, destination) }
        logger.info("Created Neo configuration file from $resource: $destination")
    }
}

internal fun renderForEditor(document: OrderedProperties): String =
    OrderedPropertiesCodec.render(document).replace("\r\n", "\n").replace('\r', '\n')
