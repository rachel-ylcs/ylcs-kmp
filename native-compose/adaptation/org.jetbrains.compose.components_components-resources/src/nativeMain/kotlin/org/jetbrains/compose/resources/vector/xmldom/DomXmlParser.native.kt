/*
 * Derived from brahmkshatriya/compose-native, under the Apache License, Version 2.0.
 * The pinned source and local changes are recorded in upstream/compose-origins.json.
 * Changes: bounded parsing, DTD rejection, Unicode references and namespace handling.
 */
package org.jetbrains.compose.resources.vector.xmldom

internal fun parse(xml: String): Element = SimpleXmlParser(xml).parse()

private class SimpleXmlParser(private val source: String) {
    private var index = if (source.startsWith('\uFEFF')) 1 else 0
    private var nodes = 0
    private var attributeCount = 0
    private var namespaceCount = 0

    fun parse(): Element {
        if (source.length > 16 * 1024 * 1024) malformed("XML resource exceeds 16 MiB")
        skipMisc()
        val root = parseElement(parentNamespaces = emptyMap())
        skipMisc()
        if (index != source.length) malformed("Unexpected content after root element")
        return root
    }

    private fun parseElement(parentNamespaces: Map<String, String>, depth: Int = 0): SimpleElement {
        if (depth >= 256) malformed("XML nesting exceeds 256 elements")
        countNode()
        expect('<')
        if (peek('/') || peek('!') || peek('?')) malformed("Expected an element")
        val qualifiedName = readName()
        val rawAttributes = linkedMapOf<String, String>()

        while (true) {
            val beforeWhitespace = index
            skipWhitespace()

            when {
                consume("/>") -> {
                    val namespaces = namespaceMap(parentNamespaces, rawAttributes)
                    return SimpleElement(qualifiedName, rawAttributes, namespaces, [])
                }
                consume(">") -> break
                else -> {
                    if (beforeWhitespace == index) malformed("Attributes must be separated by whitespace")
                    val name = readName()
                    if (++attributeCount > 262144) malformed("XML contains too many attributes")
                    if ((name == "xmlns" || name.startsWith("xmlns:")) && ++namespaceCount > 1024) malformed("XML contains too many namespace declarations")
                    skipWhitespace()
                    expect('=')
                    skipWhitespace()
                    if (name in rawAttributes) malformed("Duplicate attribute $name")
                    rawAttributes[name] = readQuotedValue()
                }
            }
        }

        val namespaces = namespaceMap(parentNamespaces, rawAttributes)
        val children = mutableListOf<Node>()
        while (true) {
            if (consume("</")) {
                val closingName = readName()
                if (closingName != qualifiedName)
                    malformed("Expected </$qualifiedName>, found </$closingName>")
                skipWhitespace()
                expect('>')
                break
            }
            when {
                consume("<!--") -> skipUntil("-->")
                consume("<![CDATA[") -> children += textNode(readUntil("]]>"))
                consume("<?") -> skipUntil("?>")
                peek('<') -> children += parseElement(namespaces, depth + 1)
                index >= source.length -> malformed("Unclosed <$qualifiedName>")
                else -> {
                    val end = source.indexOf('<', index).takeIf { it >= 0 } ?: source.length
                    val text = source.substring(index, end)
                    index = end
                    if (text.isNotEmpty()) children += textNode(decodeEntities(text))
                }
            }
        }
        return SimpleElement(qualifiedName, rawAttributes, namespaces, children)
    }

    private fun namespaceMap(
        inherited: Map<String, String>,
        attributes: Map<String, String>,
    ): Map<String, String> = if (attributes.keys.none { it == "xmlns" || it.startsWith("xmlns:") }) inherited else buildMap {
        putAll(inherited)
        attributes.forEach { [name, value] ->
            when {
                name == "xmlns" -> put("", value)
                name.startsWith("xmlns:") -> put(name.substringAfter(':'), value)
            }
        }
    }

    private fun skipMisc() {
        while (true) {
            skipWhitespace()
            when {
                consume("<?") -> skipUntil("?>")
                consume("<!--") -> skipUntil("-->")
                consume("<!DOCTYPE") -> malformed("DTD declarations are not supported")
                else -> return
            }
        }
    }

    private fun readName(): String {
        val start = index
        if (index >= source.length || !(source[index].isLetter() || source[index] == '_' || source[index] == ':')) malformed("Invalid XML name")
        while (index < source.length) {
            val c = source[index]
            if (!(c.isLetterOrDigit() || c == '_' || c == '-' || c == ':' || c == '.')) break
            index++
        }
        if (index == start) malformed("Expected a name")
        return source.substring(start, index)
    }

    private fun readQuotedValue(): String {
        if (index >= source.length || source[index] != '\'' && source[index] != '"') malformed("Expected quoted value")
        val quote = source[index++]
        val start = index

        while (index < source.length && source[index] != quote) index++

        if (index >= source.length) malformed("Unclosed attribute value")
        val value = source.substring(start, index)
        index++
        if ('<' in value) malformed("Unescaped '<' in attribute value")
        return decodeEntities(value)
    }

    private fun countNode() {
        if (++nodes > 65536) malformed("XML contains too many nodes")
    }

    private fun textNode(text: String): SimpleText {
        countNode()
        return SimpleText(text)
    }

    private fun decodeEntities(value: String): String = buildString {
        var position = 0
        while (position < value.length) {
            val character = value[position++]
            if (character != '&') {
                append(character)
                continue
            }
            val end = value.indexOf(';', position)
            if (end < 0) malformed("Unclosed entity")
            val entity = value.substring(position, end)
            position = end + 1

            when (entity) {
                "amp" -> append('&')
                "lt" -> append('<')
                "gt" -> append('>')
                "quot" -> append('"')
                "apos" -> append('\'')
                else -> {
                    val codePoint = when {
                        entity.startsWith("#x") -> entity.substring(2).toIntOrNull(16)
                        entity.startsWith('#') -> entity.substring(1).toIntOrNull()
                        else -> null
                    } ?: malformed("Unsupported entity &$entity;")

                    if (codePoint != 9 && codePoint != 10 && codePoint != 13 && codePoint !in 0x20 .. 0xD7FF && codePoint !in 0xE000 .. 0xFFFD && codePoint !in 0x10000 .. 0x10FFFF) malformed("Invalid XML character reference")
                    if (codePoint <= 0xFFFF) append(codePoint.toChar())
                    else {
                        val scalar = codePoint - 0x10000
                        append((0xD800 + (scalar shr 10)).toChar())
                        append((0xDC00 + (scalar and 0x3FF)).toChar())
                    }
                }
            }
        }
    }

    private fun readUntil(token: String): String {
        val end = source.indexOf(token, index)
        if (end < 0) malformed("Expected $token")
        val result = source.substring(index, end)
        index = end + token.length
        return result
    }

    private fun skipUntil(token: String) {
        readUntil(token)
    }

    private fun skipWhitespace() {
        while (index < source.length && source[index].isWhitespace()) index++
    }

    private fun expect(c: Char) {
        if (index >= source.length || source[index] != c) malformed("Expected '$c'")
        index++
    }

    private fun consume(token: String): Boolean {
        if (!source.startsWith(token, index)) return false
        index += token.length
        return true
    }

    private fun peek(c: Char): Boolean = index < source.length && source[index] == c

    private fun malformed(message: String): Nothing =
        throw MalformedXMLException("$message at character $index")

}

private class SimpleNodeList(private val nodes: List<Node>) : NodeList {
    override fun item(i: Int): Node = nodes[i]

    override val length: Int
        get() = nodes.size
}

private class SimpleText(value: String) : Node {
    override val textContent: String = value
    override val nodeName: String = "#text"
    override val localName: String = nodeName
    override val childNodes: NodeList = SimpleNodeList([])
    override val namespaceURI: String = ""

    override fun lookupPrefix(namespaceURI: String): String = ""
}

private class SimpleElement(
    private val qualifiedName: String,
    private val attributes: Map<String, String>,
    private val namespaces: Map<String, String>,
    children: List<Node>,
) : Element {
    override val nodeName: String
        get() = qualifiedName

    override val localName: String
        get() = qualifiedName.substringAfter(':', qualifiedName)

    override val namespaceURI: String
        get() = namespaces[qualifiedName.substringBefore(':', "")].orEmpty()

    override val childNodes: NodeList = SimpleNodeList(children)
    override val textContent: String?
        get() = buildString { appendDescendantText(childNodes) }.ifEmpty { null }

    override fun lookupPrefix(namespaceURI: String): String =
        namespaces.entries.firstOrNull { it.value == namespaceURI }?.key.orEmpty()

    override fun getAttribute(name: String): String = attributes[name].orEmpty()

    override fun getAttributeNS(nameSpaceURI: String, localName: String): String {
        return attributes.entries
            .firstOrNull { [qualifiedName, _] ->
                if (qualifiedName == "xmlns" || qualifiedName.startsWith("xmlns:"))
                    return@firstOrNull false
                val prefix = qualifiedName.substringBefore(':', "")
                val attributeLocalName = qualifiedName.substringAfter(':', qualifiedName)
                attributeLocalName == localName && (if (prefix.isEmpty()) "" else namespaces[prefix].orEmpty()) == nameSpaceURI
            }
            ?.value
            .orEmpty()
    }
}

private fun StringBuilder.appendDescendantText(nodes: NodeList) {
    for (index in 0 ..< nodes.length) {
        val node = nodes.item(index)
        if (node is SimpleText) append(node.textContent)
        else appendDescendantText(node.childNodes)
    }
}
