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
    fun `uses the last duplicate value and preserves every occurrence`() {
        val document = OrderedPropertiesCodec.parse("key=first\nother=value\nkey=last\n")

        assertEquals("last", document.value("key"))

        document.setValue("key", "updated")

        assertEquals("key=first\nother=value\nkey=updated\n", OrderedPropertiesCodec.render(document))
    }

    @Test
    fun `regroups identical duplicate entries without losing either line`() {
        val document = OrderedPropertiesCodec.parse("a=1\nkey=value\nb=2\nkey=value\nc=3\n")

        document.regroup(setOf("key"))

        assertEquals("a=1\nkey=value\nkey=value\nb=2\nc=3\n", OrderedPropertiesCodec.render(document))
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
    fun `comments and uncomments key value properties`() {
        val entry = PropertyLine.Entry("space key", " leading=value")

        val comment = OrderedPropertiesCodec.comment(entry)
        val uncommented = OrderedPropertiesCodec.uncomment(comment)

        assertEquals("# space\\ key=\\ leading=value", comment.raw)
        assertEquals(entry, uncommented)
        assertEquals(PropertyLine.Entry("key", "value"), OrderedPropertiesCodec.uncomment(PropertyLine.Comment("! key : value")))
    }

    @Test
    fun `does not interpret prose comments as properties`() {
        assertEquals(null, OrderedPropertiesCodec.uncomment(PropertyLine.Comment("# database settings")))
        assertEquals(null, OrderedPropertiesCodec.uncomment(PropertyLine.Comment("# URL uses x=y")))
        assertEquals(null, OrderedPropertiesCodec.uncomment(PropertyLine.Comment("# =missing key")))
    }

    @Test
    fun `normalizes line separators for IntelliJ documents`() {
        val document = OrderedPropertiesCodec.parse("# database\r\nkey=value\r\n")

        assertEquals("# database\nkey=value\n", renderForEditor(document))
    }
}
