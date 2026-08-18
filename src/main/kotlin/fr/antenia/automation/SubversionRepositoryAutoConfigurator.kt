package fr.antenia.automation

import com.intellij.openapi.diagnostic.Logger
import fr.antenia.MyMessageBundle.message
import fr.antenia.notifications.AnteniaNotifications
import org.jetbrains.idea.svn.SvnApplicationSettings

internal object SubversionRepositoryAutoConfigurator {
    private val logger = Logger.getInstance(SubversionRepositoryAutoConfigurator::class.java)
    private val repositories = listOf(
        "svn://leader-svn.leaderinfo.com/novanet",
        "svn://leader-svn.leaderinfo.com/projetged",
        "svn://leader-svn.leaderinfo.com/owlnet",
        "svn://leader-svn.leaderinfo.com/foxnet",
    )

    fun configure(): Boolean = try {
        val settings = SvnApplicationSettings.getInstance()
        addMissingRepositories(
            settings.checkoutURLs,
            settings.typedUrlsListCopy,
            settings::addCheckoutURL,
            settings::addTypedUrl,
        )
        true
    } catch (exception: Exception) {
        logger.warn("Global Subversion repository auto-configuration failed", exception)
        AnteniaNotifications.failure(
            null,
            "subversion-repository-global-configuration",
            message("notification.subversion.repositories.configuration.failure.title"),
            message("common.error.details", exception.message ?: exception.javaClass.simpleName),
        )
        false
    }

    internal fun addMissingRepositories(
        existingRepositories: Collection<String>,
        typedUrls: Collection<String>,
        addRepository: (String) -> Unit,
        addTypedUrl: (String) -> Unit,
    ) {
        val repositoryKeys = existingRepositories.mapTo(mutableSetOf()) { it.repositoryKey() }
        val typedUrlKeys = typedUrls.mapTo(mutableSetOf()) { it.repositoryKey() }
        repositories.forEach { repository ->
            val key = repository.repositoryKey()
            if (repositoryKeys.add(key)) addRepository(repository)
            if (typedUrlKeys.add(key)) addTypedUrl(repository)
        }
    }

    private fun String.repositoryKey(): String = trim().trimEnd('/')
}
