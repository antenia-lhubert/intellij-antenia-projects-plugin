package fr.antenia.project

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class NeoProjectDetectorTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun `detects core project java version and react subproject`() {
        val root = temporaryFolder.newFolder("core").toPath()
        root.resolve("pom.xml").toFile().writeText(
            """
            <project>
              <artifactId>webapp-novanet</artifactId>
              <version>1.6.2</version>
              <properties>
                <java.version>17</java.version>
                <maven.compiler.release>25</maven.compiler.release>
              </properties>
            </project>
            """.trimIndent(),
        )
        root.resolve("novanet-react").toFile().mkdir()
        root.resolve("novanet-react/package.json").toFile().writeText("{}")

        val project = NeoProjectDetector.detect(root)!!

        assertEquals(NeoProjectType.CORE, project.type)
        assertEquals("1.6.2", project.version)
        assertEquals("11", project.tomcatVersion)
        assertEquals(17, project.javaVersion)
        assertTrue(project.hasReact)
    }

    @Test
    fun `detects legacy java notation and no react`() {
        val root = temporaryFolder.newFolder("ged").toPath()
        root.resolve("pom.xml").toFile().writeText(
            "<project><artifactId>webapp-ged</artifactId><properties><jdk.version>1.8</jdk.version></properties></project>",
        )

        val project = NeoProjectDetector.detect(root)!!

        assertEquals(NeoProjectType.GED, project.type)
        assertEquals("1.1-1.4", project.version)
        assertEquals("9", project.tomcatVersion)
        assertEquals(8, project.javaVersion)
        assertFalse(project.hasReact)
    }

    @Test
    fun `resolves referenced compiler property`() {
        val root = temporaryFolder.newFolder("selfcare").toPath()
        root.resolve("pom.xml").toFile().writeText(
            "<project><artifactId>webapp-owlnet</artifactId><properties><jdk.version>25</jdk.version><java.version>\${jdk.version}</java.version></properties></project>",
        )

        assertEquals(25, NeoProjectDetector.detect(root)!!.javaVersion)
    }

    @Test
    fun `infers project version from java for legacy placeholder version`() {
        val root = temporaryFolder.newFolder("placeholder-version").toPath()
        root.resolve("pom.xml").toFile().writeText(
            "<project><artifactId>webapp-owlnet</artifactId><version>1.0-SNAPSHOT</version><properties><java.version>25</java.version></properties></project>",
        )

        val project = NeoProjectDetector.detect(root)!!

        assertEquals("1.6+", project.version)
        assertEquals("11", project.tomcatVersion)
        assertEquals(25, project.javaVersion)
    }

    @Test
    fun `resolves project version property independently from java version`() {
        val root = temporaryFolder.newFolder("version-property").toPath()
        root.resolve("pom.xml").toFile().writeText(
            "<project><artifactId>webapp-ged</artifactId><version>\${revision}</version><properties><revision>1.5-SNAPSHOT</revision><java.version>8</java.version></properties></project>",
        )

        val project = NeoProjectDetector.detect(root)!!

        assertEquals("1.5-SNAPSHOT", project.version)
        assertEquals("10.1", project.tomcatVersion)
        assertEquals(8, project.javaVersion)
    }

    @Test
    fun `ignores non neo maven projects`() {
        val root = temporaryFolder.newFolder("other").toPath()
        root.resolve("pom.xml").toFile().writeText("<project><artifactId>something-else</artifactId></project>")

        assertNull(NeoProjectDetector.detect(root))
    }
}
