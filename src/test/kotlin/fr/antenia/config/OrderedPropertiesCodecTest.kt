package fr.antenia.config

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OrderedPropertiesCodecTest {
    @Test
    fun `preserves order comments blank lines and values`() {
        val source = "# database\r\nurl=jdbc\\:mysql\\://localhost/test?one\\=two\r\n\r\nname=Neo \\u00e9\r\n"

        val document = OrderedPropertiesCodec.parse(source)

        assertEquals("jdbc:mysql://localhost/test?one=two", document.value("url"))
        assertEquals("Neo é", document.value("name"))
        assertTrue(document.lines[0] is PropertyLine.Comment)
        assertTrue(document.lines[2] is PropertyLine.Blank)
        assertEquals("\r\n", document.newline)
        assertTrue(document.finalNewline)
        assertEquals(listOf("url", "name"), document.lines.filterIsInstance<PropertyLine.Entry>().map { it.key })
    }

    @Test
    fun `parses escaped keys separators and continuations`() {
        val source = "escaped\\ key\\:part : first\\\n  second\nempty=\n"

        val document = OrderedPropertiesCodec.parse(source)

        assertEquals("firstsecond", document.value("escaped key:part"))
        assertEquals("", document.value("empty"))
    }

    @Test
    fun `regroups special keys at first key`() {
        val document = OrderedPropertiesCodec.parse("a=1\nuser=x\nb=2\npassword=y\nc=3\n")

        document.regroup(setOf("user", "password"))

        assertEquals(listOf("a", "user", "password", "b", "c"), document.lines.filterIsInstance<PropertyLine.Entry>().map { it.key })
    }

    @Test
    fun `renders changed values as valid properties`() {
        val document = OrderedPropertiesCodec.parse("key=value\n")
        document.setValue("space key", " leading=value")

        val rendered = OrderedPropertiesCodec.render(document)
        val reparsed = OrderedPropertiesCodec.parse(rendered)

        assertEquals(" leading=value", reparsed.value("space key"))
    }

    @Test
    fun `normalizes line separators for IntelliJ documents`() {
        val document = OrderedPropertiesCodec.parse("# database\r\nkey=value\r\n")

        assertEquals("# database\nkey=value\n", renderForEditor(document))
    }
}
