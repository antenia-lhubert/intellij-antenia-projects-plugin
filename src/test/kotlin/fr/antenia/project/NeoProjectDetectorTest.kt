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
        assertEquals("1.6.2", project.version.toString())
        assertEquals("11", project.tomcatVersion)
        assertEquals(17, project.javaVersion)
        assertTrue(project.hasReact)
    }

    @Test
    fun `detects legacy java notation and no react`() {
        val root = temporaryFolder.newFolder("ged").toPath()
        root.resolve("pom.xml").toFile().writeText(
            "<project><artifactId>webapp-ged</artifactId><version>1.4</version>" +
                "<properties><jdk.version>1.8</jdk.version></properties></project>",
        )

        val project = NeoProjectDetector.detect(root)!!

        assertEquals(NeoProjectType.GED, project.type)
        assertEquals("1.4.0", project.version.toString())
        assertEquals("9", project.tomcatVersion)
        assertEquals(8, project.javaVersion)
        assertFalse(project.hasReact)
    }

    @Test
    fun `resolves referenced compiler property`() {
        val root = temporaryFolder.newFolder("selfcare").toPath()
        root.resolve("pom.xml").toFile().writeText(
            "<project><artifactId>webapp-owlnet</artifactId><version>1.6.0</version>" +
                "<properties><jdk.version>25</jdk.version><java.version>\${jdk.version}</java.version></properties></project>",
        )

        assertEquals(25, NeoProjectDetector.detect(root)!!.javaVersion)
    }

    @Test
    fun `retains the pom snapshot project version`() {
        val root = temporaryFolder.newFolder("snapshot-version").toPath()
        root.resolve("pom.xml").toFile().writeText(
            "<project><artifactId>webapp-owlnet</artifactId><version>1.6.0-SNAPSHOT</version>" +
                "<properties><java.version>25</java.version></properties></project>",
        )

        val project = NeoProjectDetector.detect(root)!!

        assertEquals("1.6.0-SNAPSHOT", project.version.toString())
        assertEquals("11", project.tomcatVersion)
        assertEquals(25, project.javaVersion)
    }

    @Test
    fun `resolves project version property independently from java version`() {
        val root = temporaryFolder.newFolder("version-property").toPath()
        root.resolve("pom.xml").toFile().writeText(
            "<project><artifactId>webapp-ged</artifactId><version>\${revision}</version>" +
                "<properties><revision>1.5.0-SNAPSHOT</revision><java.version>8</java.version></properties></project>",
        )

        val project = NeoProjectDetector.detect(root)!!

        assertEquals("1.5.0-SNAPSHOT", project.version.toString())
        assertEquals("10.1", project.tomcatVersion)
        assertEquals(8, project.javaVersion)
    }

    @Test
    fun `uses legacy inference only when the project version is missing`() {
        val missing = temporaryFolder.newFolder("missing-version").toPath()
        missing.resolve("pom.xml").toFile().writeText("<project><artifactId>webapp-ged</artifactId></project>")

        assertEquals("1.4.0", NeoProjectDetector.detect(missing)!!.version.toString())
    }

    @Test
    fun `rejects a project version that semver4j cannot coerce`() {
        val invalid = temporaryFolder.newFolder("invalid-version").toPath()
        invalid.resolve("pom.xml").toFile().writeText(
            "<project><artifactId>webapp-ged</artifactId><version>invalid</version></project>",
        )

        assertNull(NeoProjectDetector.detect(invalid))
    }

    @Test
    fun `ignores non neo maven projects`() {
        val root = temporaryFolder.newFolder("other").toPath()
        root.resolve("pom.xml").toFile().writeText("<project><artifactId>something-else</artifactId></project>")

        assertNull(NeoProjectDetector.detect(root))
    }
}
