package fr.antenia.automation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.nio.file.Files
import java.nio.file.Path

class TomcatApplicationListenerTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun `parses installed mise tomcat paths`() {
        val paths = TomcatApplicationListener.parseMiseInstallPaths(
            """
            [
              {"version":"10.1.57","install_path":"C:\\mise\\tomcat\\10.1.57","installed":true},
              {"version":"11.0.0","install_path":"C:\\mise\\tomcat\\11.0.0","installed":false}
            ]
            """.trimIndent(),
        )

        assertEquals(listOf(Path.of("C:\\mise\\tomcat\\10.1.57")), paths)
    }

    @Test
    fun `resolves direct tomcat home`() {
        val home = createTomcatHome(temporaryFolder.newFolder("apache-tomcat-10.1.57").toPath())

        assertEquals(listOf(home.toAbsolutePath().normalize()), TomcatApplicationListener.resolveTomcatHomes(home))
    }

    @Test
    fun `resolves tomcat nested below mise install path`() {
        val installPath = temporaryFolder.newFolder("10.1.57").toPath()
        val home = createTomcatHome(installPath.resolve("apache-tomcat-10.1.57"))

        assertEquals(listOf(home.toAbsolutePath().normalize()), TomcatApplicationListener.resolveTomcatHomes(installPath))
    }

    @Test
    fun `rejects incomplete tomcat home`() {
        val candidate = temporaryFolder.newFolder("apache-tomcat-invalid").toPath()
        Files.createDirectories(candidate.resolve("bin"))
        Files.createFile(candidate.resolve("bin/catalina.bat"))

        assertTrue(TomcatApplicationListener.resolveTomcatHomes(candidate).isEmpty())
    }

    @Test
    fun `rejects tomcat archive`() {
        val archive = temporaryFolder.newFile("apache-tomcat-10.1.57.zip").toPath()

        assertTrue(TomcatApplicationListener.resolveTomcatHomes(archive).isEmpty())
    }

    @Test
    fun `adds mise suffix without changing version number`() {
        val home = Path.of("C:\\Users\\developer\\AppData\\Local\\mise\\installs\\tomcat\\10.1.57\\apache-tomcat-10.1.57")

        assertEquals(
            "Tomcat 10.1.57 (mise)",
            TomcatApplicationListener.serverName("10.1.57", home, emptySet()),
        )
    }

    @Test
    fun `increments mise suffix when application server library name already exists`() {
        val home = Path.of("C:\\Users\\developer\\AppData\\Local\\mise\\installs\\tomcat\\10.1.57\\apache-tomcat-10.1.57")

        assertEquals(
            "Tomcat 10.1.57 (mise) (2)",
            TomcatApplicationListener.serverName("10.1.57", home, setOf("Tomcat 10.1.57 (mise)")),
        )
    }

    private fun createTomcatHome(home: Path): Path {
        Files.createDirectories(home.resolve("bin"))
        Files.createDirectories(home.resolve("conf"))
        Files.createDirectories(home.resolve("lib"))
        Files.createFile(home.resolve("bin/catalina.bat"))
        Files.createFile(home.resolve("conf/server.xml"))
        Files.createFile(home.resolve("lib/catalina.jar"))
        return home
    }
}
