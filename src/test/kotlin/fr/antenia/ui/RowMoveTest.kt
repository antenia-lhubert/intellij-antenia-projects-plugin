package fr.antenia.ui

import fr.antenia.config.PropertyLine
import org.junit.Assert.assertEquals
import org.junit.Test

class RowMoveTest {
    @Test
    fun `moves a row to an insertion point below it`() {
        val rows = mutableListOf("a", "b", "c", "d")

        val destination = RowMove.move(rows, source = 1, insertion = 4)

        assertEquals(3, destination)
        assertEquals(listOf("a", "c", "d", "b"), rows)
    }

    @Test
    fun `moves a row to an insertion point above it`() {
        val rows = mutableListOf("a", "b", "c", "d")

        val destination = RowMove.move(rows, source = 3, insertion = 1)

        assertEquals(1, destination)
        assertEquals(listOf("a", "d", "b", "c"), rows)
    }

    @Test
    fun `keeps grouped property lines together`() {
        val database = listOf(PropertyLine.Entry("url", "jdbc:mysql://host/db"), PropertyLine.Entry("username", "neo"))
        val rows = mutableListOf(
            listOf<PropertyLine>(PropertyLine.Comment("# database")),
            database,
            listOf<PropertyLine>(PropertyLine.Blank()),
            listOf<PropertyLine>(PropertyLine.Entry("smtp", "localhost")),
        )

        RowMove.move(rows, source = 1, insertion = 4)

        assertEquals(database, rows.last())
        assertEquals(listOf("url", "username"), rows.last().filterIsInstance<PropertyLine.Entry>().map { it.key })
    }

    @Test
    fun `rejects no-op and disallowed moves`() {
        val rows = mutableListOf("built-in", "custom")

        assertEquals(-1, RowMove.move(rows, source = 1, insertion = 2))
        assertEquals(-1, RowMove.move(rows, source = 1, insertion = 0) { _, destination -> destination > 0 })
        assertEquals(listOf("built-in", "custom"), rows)
    }
}
