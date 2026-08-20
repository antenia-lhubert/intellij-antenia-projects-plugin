package fr.antenia.automation

import org.junit.Assert.assertEquals
import org.junit.Test

class SubversionRepositoryAutoConfiguratorTest {
    @Test
    fun `adds all missing Antenia repositories`() {
        val repositories = mutableListOf<String>()
        val typedUrls = mutableListOf<String>()

        SubversionRepositoryAutoConfigurator.addMissingRepositories(
            emptyList(),
            emptyList(),
            repositories::add,
            typedUrls::add,
        )

        val expected = listOf(
            "svn://svn.leaderinfo.com/novanet",
            "svn://svn.leaderinfo.com/projetged",
            "svn://svn.leaderinfo.com/owlnet",
            "svn://svn.leaderinfo.com/foxnet",
        )
        assertEquals(expected, repositories)
        assertEquals(expected, typedUrls)
    }

    @Test
    fun `preserves existing repositories and adds only missing entries`() {
        val repositories = mutableListOf<String>()
        val typedUrls = mutableListOf<String>()

        SubversionRepositoryAutoConfigurator.addMissingRepositories(
            listOf("svn://custom/repository", "svn://svn.leaderinfo.com/novanet/"),
            listOf("svn://svn.leaderinfo.com/projetged"),
            repositories::add,
            typedUrls::add,
        )

        assertEquals(
            listOf(
                "svn://svn.leaderinfo.com/projetged",
                "svn://svn.leaderinfo.com/owlnet",
                "svn://svn.leaderinfo.com/foxnet",
            ),
            repositories,
        )
        assertEquals(
            listOf(
                "svn://svn.leaderinfo.com/novanet",
                "svn://svn.leaderinfo.com/owlnet",
                "svn://svn.leaderinfo.com/foxnet",
            ),
            typedUrls,
        )
    }
}
