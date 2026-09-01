package fr.antenia.config

sealed interface PropertyLine {
    data class Blank(val raw: String = "") : PropertyLine
    data class Comment(val raw: String) : PropertyLine
    data class Entry(var key: String, var value: String) : PropertyLine
}

data class PropertyLayoutGroup(
    val keys: List<String>,
    val heading: String? = null,
)

data class OrderedProperties(
    val lines: MutableList<PropertyLine>,
    val newline: String = System.lineSeparator(),
    val finalNewline: Boolean = true,
) {
    fun value(key: String): String? = lines.filterIsInstance<PropertyLine.Entry>().lastOrNull { it.key == key }?.value

    fun setValue(key: String, value: String) {
        val entry = lines.filterIsInstance<PropertyLine.Entry>().lastOrNull { it.key == key }
        if (entry != null) entry.value = value else lines.add(PropertyLine.Entry(key, value))
    }

    fun setValueInGroup(key: String, value: String, group: List<String>) {
        val entry = lines.filterIsInstance<PropertyLine.Entry>().lastOrNull { it.key == key }
        if (entry != null) {
            entry.value = value
            return
        }

        val keyPosition = group.indexOf(key)
        require(keyPosition >= 0) { "Database layout group does not contain '$key'" }
        val next = lines.indices.firstOrNull { index ->
            val line = lines[index]
            line is PropertyLine.Entry && line.key in group.drop(keyPosition + 1)
        }
        val previous = lines.indices.lastOrNull { index ->
            val line = lines[index]
            line is PropertyLine.Entry && line.key in group.take(keyPosition)
        }
        val insertion = next ?: previous?.plus(1) ?: (lines.indexOfLast { it is PropertyLine.Entry } + 1)
        lines.add(insertion, PropertyLine.Entry(key, value))
    }

    fun regroup(keys: Set<String>) {
        val indexed = lines.withIndex().filter { (_, line) -> line is PropertyLine.Entry && line.key in keys }
        if (indexed.size < 2) return
        val first = indexed.first().index
        val entries = indexed.map { it.value }
        indexed.asReversed().forEach { lines.removeAt(it.index) }
        lines.addAll(first.coerceAtMost(lines.size), entries)
    }

    fun groupedLines(groups: List<PropertyLayoutGroup>): List<PropertyLine> =
        groupedIndexBlocks(groups).flatten().map(lines::get)

    fun regroupPreservingLayout(groups: List<PropertyLayoutGroup>) {
        val blocks = groupedIndexBlocks(groups).filter(List<Int>::isNotEmpty)
        val indices = blocks.flatten()
        if (indices.size < 2) return
        val insertion = indices.min()
        val grouped = buildList {
            blocks.forEach { block ->
                val blockLines = block.map(lines::get)
                if (isNotEmpty() && last() !is PropertyLine.Blank && blockLines.first() !is PropertyLine.Blank) {
                    add(PropertyLine.Blank())
                }
                addAll(blockLines)
            }
        }
        indices.sortedDescending().forEach(lines::removeAt)
        lines.addAll(insertion.coerceAtMost(lines.size), grouped)
    }

    private fun groupedIndexBlocks(groups: List<PropertyLayoutGroup>): List<List<Int>> {
        val duplicateKeys = groups.flatMap { it.keys }.groupingBy { it }.eachCount().filterValues { it > 1 }.keys
        require(duplicateKeys.isEmpty()) { "Property layout groups overlap: ${duplicateKeys.joinToString()}" }
        val ownedByGroup = mutableMapOf<Int, Int>()
        val groupEntries = groups.mapIndexed { groupIndex, group ->
            lines.indices.filter { index ->
                val line = lines[index]
                line is PropertyLine.Entry && line.key in group.keys
            }.also { entries -> entries.forEach { ownedByGroup[it] = groupIndex } }
        }
        val groupHeadings = groups.mapIndexed { groupIndex, group ->
            group.heading?.takeIf { groupEntries[groupIndex].isNotEmpty() }?.let { heading ->
                groupEntries[groupIndex].firstNotNullOfOrNull { entry ->
                    (entry - 1 downTo 0).firstOrNull { lines[it] !is PropertyLine.Blank }?.takeIf { index ->
                        val line = lines[index]
                        line is PropertyLine.Comment && line.raw.trim() == heading.trim()
                    }
                }?.also { ownedByGroup[it] = groupIndex }
            }
        }
        val groupBlanks = groups.indices.associateWith { mutableListOf<Int>() }
        lines.indices.filter { lines[it] is PropertyLine.Blank }.forEach { blank ->
            val previous = (blank - 1 downTo 0).firstOrNull { lines[it] !is PropertyLine.Blank }
            val next = (blank + 1 until lines.size).firstOrNull { lines[it] !is PropertyLine.Blank }
            if (previous in ownedByGroup && next in ownedByGroup) {
                groupBlanks.getValue(requireNotNull(ownedByGroup[next])).add(blank)
            }
        }
        return groups.indices.map { groupIndex ->
            val heading = groupHeadings[groupIndex]
            val entries = groupEntries[groupIndex]
            val blanks = groupBlanks.getValue(groupIndex)
            if (heading == null) {
                (entries + blanks).sorted()
            } else {
                val leadingBlanks = blanks.filter { it < heading }
                buildList {
                    addAll(leadingBlanks)
                    add(heading)
                    addAll((entries + blanks.filter { it >= heading }).sorted())
                }
            }
        }
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

    fun comment(entry: PropertyLine.Entry): PropertyLine.Comment =
        PropertyLine.Comment("# ${escape(entry.key, true)}=${escape(entry.value, false)}")

    fun uncomment(comment: PropertyLine.Comment): PropertyLine.Entry? {
        val raw = comment.raw.trimStart().drop(1).trimStart()
        if (!hasExplicitSeparator(raw)) return null
        val (rawKey, rawValue) = splitEntry(raw)
        if (rawKey.isEmpty()) return null
        return PropertyLine.Entry(unescape(rawKey), unescape(rawValue))
    }

    private fun hasContinuation(line: String): Boolean {
        var slashes = 0
        for (index in line.indices.reversed()) {
            if (line[index] != '\\') break
            slashes++
        }
        return slashes % 2 == 1
    }

    private fun hasExplicitSeparator(line: String): Boolean {
        var escaped = false
        for (index in line.indices) {
            val character = line[index]
            if (!escaped && (character == '=' || character == ':')) return index > 0
            if (!escaped && character.isWhitespace()) {
                val separator = line.drop(index).trimStart().firstOrNull()
                return index > 0 && (separator == '=' || separator == ':')
            }
            escaped = !escaped && character == '\\'
            if (character != '\\') escaped = false
        }
        return false
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
