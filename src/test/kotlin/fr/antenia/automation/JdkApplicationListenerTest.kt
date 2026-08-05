package fr.antenia.automation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.nio.file.Files
import java.nio.file.Path

class JdkApplicationListenerTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun `parses installed mise java paths`() {
        val paths = JdkAutoConfigurator.parseMiseInstallPaths(
            """
            [
              {"version":"temurin-17.0.19+10","install_path":"C:\\mise\\java\\temurin-17.0.19+10","installed":true},
              {"version":"temurin-21.0.11+10","install_path":"C:\\mise\\java\\temurin-21.0.11+10","installed":false}
            ]
            """.trimIndent(),
        )

        assertEquals(listOf(Path.of("C:\\mise\\java\\temurin-17.0.19+10")), paths)
    }

    @Test
    fun `resolves direct jdk home`() {
        val home = createJdkHome(temporaryFolder.newFolder("jdk-25").toPath())

        assertEquals(listOf(home.toAbsolutePath().normalize()), JdkAutoConfigurator.resolveJdkHomes(home))
    }

    @Test
    fun `resolves jdk nested below installation path`() {
        val installPath = temporaryFolder.newFolder("java-install").toPath()
        val home = createJdkHome(installPath.resolve("jdk-17"))

        assertEquals(listOf(home.toAbsolutePath().normalize()), JdkAutoConfigurator.resolveJdkHomes(installPath))
    }

    @Test
    fun `rejects jre without compiler`() {
        val candidate = temporaryFolder.newFolder("jre-17").toPath()
        Files.createDirectories(candidate.resolve("bin"))
        Files.createFile(candidate.resolve("bin/java.exe"))
        Files.createFile(candidate.resolve("release"))

        assertTrue(JdkAutoConfigurator.resolveJdkHomes(candidate).isEmpty())
    }

    @Test
    fun `adds mise suffix to suggested jdk name`() {
        assertEquals("temurin-25 (mise)", JdkAutoConfigurator.jdkName("temurin-25", true, emptySet()))
    }

    @Test
    fun `preserves standard name for program files jdk`() {
        assertEquals("openjdk-25", JdkAutoConfigurator.jdkName("openjdk-25", false, emptySet()))
    }

    @Test
    fun `migrates default mise jdk name`() {
        assertTrue(JdkAutoConfigurator.shouldRenameMiseJdk("temurin-25", "temurin-25"))
    }

    @Test
    fun `preserves custom mise jdk name`() {
        assertTrue(!JdkAutoConfigurator.shouldRenameMiseJdk("project-jdk", "temurin-25"))
    }

    private fun createJdkHome(home: Path): Path {
        Files.createDirectories(home.resolve("bin"))
        Files.createFile(home.resolve("bin/java.exe"))
        Files.createFile(home.resolve("bin/javac.exe"))
        Files.createFile(home.resolve("release"))
        return home
    }
}
