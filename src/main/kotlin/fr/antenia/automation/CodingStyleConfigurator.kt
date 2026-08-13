package fr.antenia.automation

import com.intellij.application.options.CodeStyle
import com.intellij.openapi.editor.ex.EditorSettingsExternalizable
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.encoding.EncodingManager
import com.intellij.openapi.vfs.encoding.EncodingProjectManager
import com.intellij.psi.codeStyle.CodeStyleSettingsManager
import com.intellij.psi.codeStyle.JavaCodeStyleSettings
import com.intellij.psi.codeStyle.JavaImportsLayoutSettings
import com.intellij.psi.codeStyle.PackageEntry
import com.intellij.psi.codeStyle.PackageEntryTable
import fr.antenia.project.NeoProjectDetector
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path

object CodingStyleConfigurator {
    private const val IMPORT_ON_DEMAND_THRESHOLD = 999
    private val WINDOWS_1252 = Charset.forName("windows-1252")

    fun configure(project: Project) {
        val projectRoot = project.basePath?.let(Path::of)
        val isNeoProject = projectRoot != null && isNeoProject(projectRoot)
        val encoding = encodingFor(projectRoot, isNeoProject)
        EncodingManager.getInstance().defaultCharsetName = encoding.name()
        val projectEncodingManager = EncodingProjectManager.getInstance(project)
        setProjectEncoding(encoding, projectEncodingManager::setDefaultCharsetName)
        if (!isNeoProject) return

        configureProjectCodeStyle(project)
        EditorSettingsExternalizable.getInstance().stripTrailingSpaces =
            EditorSettingsExternalizable.STRIP_TRAILING_SPACES_NONE
    }

    private fun configureProjectCodeStyle(project: Project) {
        val settingsManager = CodeStyleSettingsManager.getInstance(project)
        val settings = settingsManager.mainProjectCodeStyle
            ?: settingsManager.cloneSettings(CodeStyle.getProjectOrDefaultSettings(project))
        val javaSettings = settings.getCustomSettings(JavaCodeStyleSettings::class.java)
        configureJavaSettings(javaSettings)
        CodeStyle.setMainProjectSettings(project, settings)
        settingsManager.notifyCodeStyleSettingsChanged()
    }

    internal fun configureJavaSettings(javaSettings: JavaImportsLayoutSettings) {
        javaSettings.classCountToUseImportOnDemand = IMPORT_ON_DEMAND_THRESHOLD
        javaSettings.namesCountToUseImportOnDemand = IMPORT_ON_DEMAND_THRESHOLD
        javaSettings.isLayoutStaticImportsSeparately = true
        configureImportLayout(javaSettings.importLayoutTable)
    }

    internal fun configureImportLayout(table: PackageEntryTable) {
        while (table.entryCount > 0) table.removeEntryAt(0)
        listOf(
            PackageEntry.ALL_OTHER_STATIC_IMPORTS_ENTRY,
            PackageEntry.BLANK_LINE_ENTRY,
            PackageEntry(false, "java", true),
            PackageEntry.BLANK_LINE_ENTRY,
            PackageEntry(false, "javax", true),
            PackageEntry.BLANK_LINE_ENTRY,
            PackageEntry(false, "org", true),
            PackageEntry.BLANK_LINE_ENTRY,
            PackageEntry(false, "com", true),
            PackageEntry.BLANK_LINE_ENTRY,
            PackageEntry.ALL_OTHER_IMPORTS_ENTRY,
            PackageEntry.ALL_MODULE_IMPORTS,
        ).forEach(table::addEntry)
    }

    internal fun isNeoProject(projectRoot: Path): Boolean = NeoProjectDetector.detect(projectRoot) != null

    internal fun encodingFor(projectRoot: Path?, isNeoProject: Boolean): Charset =
        if (isNeoProject && projectRoot != null && !Files.exists(projectRoot.resolve(".editorconfig"))) {
            WINDOWS_1252
        } else {
            StandardCharsets.UTF_8
        }

    internal fun setProjectEncoding(encoding: Charset, setDefaultCharsetName: (String) -> Unit) {
        if (encoding == WINDOWS_1252) setDefaultCharsetName(StandardCharsets.US_ASCII.name())
        setDefaultCharsetName(encoding.name())
    }
}
