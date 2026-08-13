package fr.antenia.automation

import com.intellij.psi.codeStyle.JavaImportsLayoutSettings
import com.intellij.psi.codeStyle.PackageEntry
import com.intellij.psi.codeStyle.PackageEntryTable
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.nio.charset.StandardCharsets
import java.nio.file.Path

class CodingStyleConfiguratorTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun `configures requested java import settings`() {
        val settings = TestJavaImportsLayoutSettings()

        CodingStyleConfigurator.configureJavaSettings(settings)

        assertEquals(999, settings.classCountToUseImportOnDemand)
        assertEquals(999, settings.namesCountToUseImportOnDemand)
        assertTrue(settings.isLayoutStaticImportsSeparately)
        val entries = settings.importLayoutTable.entries
        assertEquals(12, entries.size)
        assertSame(PackageEntry.ALL_OTHER_STATIC_IMPORTS_ENTRY, entries[0])
        assertSame(PackageEntry.BLANK_LINE_ENTRY, entries[1])
        assertPackage(entries[2], "java")
        assertSame(PackageEntry.BLANK_LINE_ENTRY, entries[3])
        assertPackage(entries[4], "javax")
        assertSame(PackageEntry.BLANK_LINE_ENTRY, entries[5])
        assertPackage(entries[6], "org")
        assertSame(PackageEntry.BLANK_LINE_ENTRY, entries[7])
        assertPackage(entries[8], "com")
        assertSame(PackageEntry.BLANK_LINE_ENTRY, entries[9])
        assertSame(PackageEntry.ALL_OTHER_IMPORTS_ENTRY, entries[10])
        assertSame(PackageEntry.ALL_MODULE_IMPORTS, entries[11])
    }

    @Test
    fun `uses windows 1252 for neo project without editorconfig`() {
        val root = temporaryFolder.newFolder("neo").toPath()
        writePom(root, "webapp-novanet")

        assertTrue(CodingStyleConfigurator.isNeoProject(root))
        assertEquals("windows-1252", CodingStyleConfigurator.encodingFor(root, true).name())
    }

    @Test
    fun `uses utf 8 when neo project has editorconfig`() {
        val root = temporaryFolder.newFolder("configured-neo").toPath()
        writePom(root, "webapp-ged")
        root.resolve(".editorconfig").toFile().writeText("root = true")

        assertTrue(CodingStyleConfigurator.isNeoProject(root))
        assertEquals(StandardCharsets.UTF_8, CodingStyleConfigurator.encodingFor(root, true))
    }

    @Test
    fun `does not apply neo style and uses utf 8 for non neo project without editorconfig`() {
        val root = temporaryFolder.newFolder("other").toPath()
        writePom(root, "something-else")

        assertFalse(CodingStyleConfigurator.isNeoProject(root))
        assertEquals(StandardCharsets.UTF_8, CodingStyleConfigurator.encodingFor(root, false))
    }

    @Test
    fun `sets project encoding through us ascii before windows 1252`() {
        val assignments = mutableListOf<String>()

        CodingStyleConfigurator.setProjectEncoding(java.nio.charset.Charset.forName("windows-1252"), assignments::add)

        assertEquals(listOf("US-ASCII", "windows-1252"), assignments)
    }

    @Test
    fun `sets utf 8 project encoding directly`() {
        val assignments = mutableListOf<String>()

        CodingStyleConfigurator.setProjectEncoding(StandardCharsets.UTF_8, assignments::add)

        assertEquals(listOf("UTF-8"), assignments)
    }

    private fun writePom(root: Path, artifactId: String) {
        root.resolve("pom.xml").toFile().writeText("<project><artifactId>$artifactId</artifactId></project>")
    }

    private fun assertPackage(entry: PackageEntry, name: String) {
        assertEquals(name, entry.packageName)
        assertTrue(entry.isWithSubpackages)
        assertFalse(entry.isStatic)
    }

    private class TestJavaImportsLayoutSettings : JavaImportsLayoutSettings {
        private val layoutTable = PackageEntryTable().apply { addEntry(PackageEntry.ALL_OTHER_IMPORTS_ENTRY) }
        private val onDemandTable = PackageEntryTable()
        private var staticImportsSeparately = false
        private var namesCount = 3
        private var classCount = 5
        private var insertInnerClassImports = false
        private var useSingleClassImports = true
        private var useFqClassNames = false

        override fun getImportLayoutTable() = layoutTable
        override fun getPackagesToUseImportOnDemand() = onDemandTable
        override fun isLayoutStaticImportsSeparately() = staticImportsSeparately
        override fun setLayoutStaticImportsSeparately(value: Boolean) { staticImportsSeparately = value }
        override fun getNamesCountToUseImportOnDemand() = namesCount
        override fun setNamesCountToUseImportOnDemand(value: Int) { namesCount = value }
        override fun getClassCountToUseImportOnDemand() = classCount
        override fun setClassCountToUseImportOnDemand(value: Int) { classCount = value }
        override fun isInsertInnerClassImports() = insertInnerClassImports
        override fun setInsertInnerClassImports(value: Boolean) { insertInnerClassImports = value }
        override fun isUseSingleClassImports() = useSingleClassImports
        override fun setUseSingleClassImports(value: Boolean) { useSingleClassImports = value }
        override fun isUseFqClassNames() = useFqClassNames
        override fun setUseFqClassNames(value: Boolean) { useFqClassNames = value }
    }
}
