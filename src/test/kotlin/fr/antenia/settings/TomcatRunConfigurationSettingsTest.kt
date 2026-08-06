package fr.antenia.settings

import org.junit.Assert.assertEquals
import org.junit.Test

class TomcatRunConfigurationSettingsTest {
    @Test
    fun `maps frame deactivation actions to IntelliJ options`() {
        assertFrameAction(TomcatFrameDeactivationAction.NOTHING, false, false)
        assertFrameAction(TomcatFrameDeactivationAction.UPDATE_RESOURCES, true, false)
        assertFrameAction(TomcatFrameDeactivationAction.UPDATE_RESOURCES_AND_CLASSES, true, true)
    }

    @Test
    fun `offers independent update actions`() {
        assertEquals(
            listOf("update-resources", "update-classes-and-resources", "redeploy-artifacts", "restart-server"),
            TomcatUpdatingPolicy.entries.map { it.id },
        )
    }

    @Test
    fun `uses configured run configuration defaults`() {
        assertEquals(
            TomcatRunConfigurationOptions(
                openInBrowser = true,
                updateOnFrameDeactivation = true,
                updateClassesOnFrameDeactivation = false,
                updatingPolicy = "update-classes-and-resources",
            ),
            TomcatRunConfigurationSettings().options(),
        )
    }

    @Test
    fun `persists configured run configuration options`() {
        val settings = TomcatRunConfigurationSettings()
        val options = TomcatRunConfigurationOptions(
            openInBrowser = false,
            updateOnFrameDeactivation = false,
            updateClassesOnFrameDeactivation = false,
            updatingPolicy = "update-resources",
        )

        settings.save(options)

        assertEquals(options, settings.options())
    }

    private fun assertFrameAction(action: TomcatFrameDeactivationAction, update: Boolean, updateClasses: Boolean) {
        assertEquals(update, action.updateOnFrameDeactivation)
        assertEquals(updateClasses, action.updateClassesOnFrameDeactivation)
        assertEquals(action, TomcatFrameDeactivationAction.fromOptions(update, updateClasses))
    }
}
