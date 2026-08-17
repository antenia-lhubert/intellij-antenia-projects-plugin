package fr.antenia.automation

import com.intellij.ide.AppLifecycleListener
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.registry.Registry
import fr.antenia.MyMessageBundle.message
import fr.antenia.notifications.AnteniaNotifications

class RegistryApplicationListener : AppLifecycleListener {
    override fun appStarted() {
        RegistryAutoConfigurator.configure()
    }
}

internal object RegistryAutoConfigurator {
    private val logger = Logger.getInstance(RegistryAutoConfigurator::class.java)

    fun configure(): Boolean = try {
        applyValues { key, value -> Registry.get(key).setValue(value) }
        true
    } catch (exception: Exception) {
        logger.warn("Global registry auto-configuration failed", exception)
        AnteniaNotifications.failure(
            null,
            "registry-global-configuration",
            message("notification.registry.configuration.failure.title"),
            message("common.error.details", exception.message ?: exception.javaClass.simpleName),
        )
        false
    }

    internal fun applyValues(setValue: (String, Boolean) -> Unit) {
        setValue("vcs.merge.conflict.iterative.resolution", false)
    }
}
