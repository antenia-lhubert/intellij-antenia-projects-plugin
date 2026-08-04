package fr.antenia.config

sealed interface PropertyLine {
    data class Blank(val raw: String = "") : PropertyLine
    data class Comment(val raw: String) : PropertyLine
    data class Entry(var key: String, var value: String) : PropertyLine
}

data class OrderedProperties(
    val lines: MutableList<PropertyLine>,
    val newline: String = System.lineSeparator(),
    val finalNewline: Boolean = true,
) {
    fun value(key: String): String? = lines.filterIsInstance<PropertyLine.Entry>().firstOrNull { it.key == key }?.value

    fun setValue(key: String, value: String) {
        val entry = lines.filterIsInstance<PropertyLine.Entry>().firstOrNull { it.key == key }
        if (entry != null) entry.value = value else lines.add(PropertyLine.Entry(key, value))
    }

    fun regroup(keys: Set<String>) {
        val indexed = lines.withIndex().filter { (_, line) -> line is PropertyLine.Entry && line.key in keys }
        if (indexed.size < 2) return
        val first = indexed.first().index
        val entries = indexed.map { it.value }
        lines.removeAll(entries.toSet())
        lines.addAll(first.coerceAtMost(lines.size), entries)
    }
}

object OrderedPropertiesCodec {
    fun parse(text: String): OrderedProperties {
        val newline = when {
            "\r\n" in text -> "\r\n"
            "\r" in text -> "\r"
            else -> "\n"
        }
        val finalNewline = text.endsWith("\n") || text.endsWith("\r")
        val physical = text.split(Regex("\r\n|\r|\n")).let { if (finalNewline) it.dropLast(1) else it }
        val result = mutableListOf<PropertyLine>()
        var index = 0
        while (index < physical.size) {
            val first = physical[index]
            val trimmed = first.trimStart()
            if (trimmed.isEmpty()) {
                result += PropertyLine.Blank(first)
                index++
                continue
            }
            if (trimmed.startsWith('#') || trimmed.startsWith('!')) {
                result += PropertyLine.Comment(first)
                index++
                continue
            }
            val logical = StringBuilder(first)
            while (hasContinuation(logical.toString()) && index + 1 < physical.size) {
                logical.setLength(logical.length - 1)
                logical.append(physical[++index].trimStart())
            }
            val (rawKey, rawValue) = splitEntry(logical.toString())
            result += PropertyLine.Entry(unescape(rawKey), unescape(rawValue))
            index++
        }
        return OrderedProperties(result, newline, finalNewline)
    }

    fun render(document: OrderedProperties): String {
        val rendered = document.lines.joinToString(document.newline) { line ->
            when (line) {
                is PropertyLine.Blank -> line.raw
                is PropertyLine.Comment -> line.raw
                is PropertyLine.Entry -> "${escape(line.key, true)}=${escape(line.value, false)}"
            }
        }
        return rendered + if (document.finalNewline) document.newline else ""
    }

    private fun hasContinuation(line: String): Boolean {
        var slashes = 0
        for (index in line.indices.reversed()) {
            if (line[index] != '\\') break
            slashes++
        }
        return slashes % 2 == 1
    }

    private fun splitEntry(line: String): Pair<String, String> {
        var escaped = false
        var separator = -1
        for (index in line.indices) {
            val character = line[index]
            if (!escaped && (character == '=' || character == ':' || character.isWhitespace())) {
                separator = index
                break
            }
            escaped = !escaped && character == '\\'
            if (character != '\\') escaped = false
        }
        if (separator < 0) return line to ""
        var valueStart = separator
        while (valueStart < line.length && line[valueStart].isWhitespace()) valueStart++
        if (valueStart < line.length && (line[valueStart] == '=' || line[valueStart] == ':')) valueStart++
        while (valueStart < line.length && line[valueStart].isWhitespace()) valueStart++
        return line.substring(0, separator) to line.substring(valueStart)
    }

    private fun unescape(value: String): String {
        val result = StringBuilder()
        var index = 0
        while (index < value.length) {
            val character = value[index++]
            if (character != '\\' || index >= value.length) {
                result.append(character)
                continue
            }
            when (val escaped = value[index++]) {
                't' -> result.append('\t')
                'n' -> result.append('\n')
                'r' -> result.append('\r')
                'f' -> result.append('\u000C')
                'u' -> {
                    val hex = value.substring(index, (index + 4).coerceAtMost(value.length))
                    if (hex.length == 4) {
                        hex.toIntOrNull(16)?.let { result.append(it.toChar()) } ?: result.append("\\u").append(hex)
                        index += 4
                    } else result.append("\\u").append(hex)
                }
                else -> result.append(escaped)
            }
        }
        return result.toString()
    }

    private fun escape(value: String, key: Boolean): String = buildString {
        value.forEachIndexed { index, character ->
            when (character) {
                '\\' -> append("\\\\")
                '\t' -> append("\\t")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\u000C' -> append("\\f")
                '=', ':' -> if (key) append('\\').append(character) else append(character)
                '#', '!' -> if (key || index == 0) append('\\').append(character) else append(character)
                ' ' -> if (key || index == 0) append("\\ ") else append(' ')
                else -> append(character)
            }
        }
    }
}
