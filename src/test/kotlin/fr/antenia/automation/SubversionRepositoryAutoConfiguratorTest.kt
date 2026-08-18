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
            "svn://leader-svn.leaderinfo.com/novanet",
            "svn://leader-svn.leaderinfo.com/projetged",
            "svn://leader-svn.leaderinfo.com/owlnet",
            "svn://leader-svn.leaderinfo.com/foxnet",
        )
        assertEquals(expected, repositories)
        assertEquals(expected, typedUrls)
    }

    @Test
    fun `preserves existing repositories and adds only missing entries`() {
        val repositories = mutableListOf<String>()
        val typedUrls = mutableListOf<String>()

        SubversionRepositoryAutoConfigurator.addMissingRepositories(
            listOf("svn://custom/repository", "svn://leader-svn.leaderinfo.com/novanet/"),
            listOf("svn://leader-svn.leaderinfo.com/projetged"),
            repositories::add,
            typedUrls::add,
        )

        assertEquals(
            listOf(
                "svn://leader-svn.leaderinfo.com/projetged",
                "svn://leader-svn.leaderinfo.com/owlnet",
                "svn://leader-svn.leaderinfo.com/foxnet",
            ),
            repositories,
        )
        assertEquals(
            listOf(
                "svn://leader-svn.leaderinfo.com/novanet",
                "svn://leader-svn.leaderinfo.com/owlnet",
                "svn://leader-svn.leaderinfo.com/foxnet",
            ),
            typedUrls,
        )
    }
}
