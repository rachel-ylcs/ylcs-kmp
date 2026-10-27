package org.jetbrains.compose.resources.vector.xmldom

import kotlin.test.*

class NativeXmlTest {
    @Test
    fun resolvesNamespaceShadowingAndSupplementaryCharacters() {
        val root = parse("<r xmlns='urn:default' xmlns:a='urn:a'><x xmlns:a='urn:b' xmlns:q='urn:a' q:v='&#x1f600;' a:v='b' plain='p'/></r>")
        val child = root.childNodes.item(0) as Element
        assertEquals("urn:default", child.namespaceURI)
        assertEquals("😀", child.getAttributeNS("urn:a", "v"))
        assertEquals("b", child.getAttributeNS("urn:b", "v"))
        assertEquals("p", child.getAttributeNS("", "plain"))
        assertEquals("q", child.lookupPrefix("urn:a"))
    }

    @Test
    fun rejectsDtdAndMalformedAttributes() {
        for (xml in ["<!DOCTYPE r [<!ENTITY x SYSTEM 'file:///unreachable'>]><r>&x;</r>", "<r a='x'b='y'/>", "<1bad/>", "<r a='<bad'/>", "<r>&#xD800;</r>", "<r>&unknown;</r>"]) {
            assertFailsWith<MalformedXMLException>(xml) { parse(xml) }
        }
    }

    @Test
    fun boundsDepthAndTotalNodeCount() {
        assertFailsWith<MalformedXMLException> { parse("<r>".repeat(257) + "</r>".repeat(257)) }
        assertFailsWith<MalformedXMLException> { parse("<r>" + "<x/>".repeat(65536) + "</r>") }
    }

    @Test
    fun aggregatesTextWithoutKeepingAWholeCopyAtEveryAncestor() {
        val text = "abc😀".repeat(32768)
        val root = parse("<r>".repeat(64) + text + "<![CDATA[<tail>]]>" + "</r>".repeat(64))
        assertEquals("$text<tail>", root.textContent)
    }
}
