package fr.antenia.database

import org.junit.Assert.assertEquals
import org.junit.Test
import java.awt.Component
import java.awt.Graphics
import javax.swing.Icon

class DatabaseEditorComponentsTest {
    @Test
    fun `changing action enablement requests a toolbar refresh`() {
        val action = DatabaseEditorAction("Save", "Save profile", EmptyIcon) {}
        var refreshes = 0
        action.refreshStateWith { refreshes++ }

        action.enabled = false
        action.enabled = false
        action.enabled = true

        assertEquals(2, refreshes)
    }

    private object EmptyIcon : Icon {
        override fun getIconWidth(): Int = 1
        override fun getIconHeight(): Int = 1
        override fun paintIcon(component: Component?, graphics: Graphics?, x: Int, y: Int) = Unit
    }
}
