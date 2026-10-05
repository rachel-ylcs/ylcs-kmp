package love.yinlin.foundation.parser.markdown

import kotlin.test.*

class TestMarkdownLimits {
    @Test
    fun parserAndWriterEnforceDepthAndNodeBudgets() {
        val small = Markdown(MarkdownConfiguration(maxDepth = 2, maxNodes = 3))
        assertEquals(MarkdownDocument([MarkdownParagraph("plain")]), small.parse("plain"))
        assertFailsWith<MarkdownParseException> { small.parse("*nested*") }
        assertFailsWith<MarkdownParseException> { small.parse("> > > deep") }
        assertFailsWith<MarkdownParseException> { small.parse("one\n\ntwo") }
        assertFailsWith<IllegalArgumentException> { small.encodeToString(markdown { p { em("nested") } }) }
        assertFailsWith<IllegalArgumentException> { small.encodeToString(markdown { p("one"); p("two") }) }
        val error = assertFailsWith<MarkdownParseException> {
            Markdown(MarkdownConfiguration(maxNodes = 3)).parse("ok\n\nextra")
        }
        assertEquals(3, error.line)
        assertEquals(1, error.column)
        assertEquals(4, error.offset)
    }

    @Test
    fun sourceLengthAndConfigurationValidation() {
        assertFailsWith<MarkdownParseException> { Markdown(MarkdownConfiguration(maxSourceLength = 4)).parse("12345") }
        assertFailsWith<IllegalArgumentException> { MarkdownConfiguration(maxDepth = 0) }
        assertFailsWith<IllegalArgumentException> { MarkdownConfiguration(maxDepth = 257) }
        assertFailsWith<IllegalArgumentException> { MarkdownConfiguration(maxNodes = 0) }
        assertFailsWith<IllegalArgumentException> { MarkdownConfiguration(maxSourceLength = 0) }
    }

    @Test
    fun extensionsCanBeDisabledConsistently() {
        val standard = Markdown(MarkdownConfiguration(tables = false, taskLists = false, strikethrough = false))
        val doc = standard.parse("~~text~~\n\n- [x] task\n\na | b\n--- | ---")
        assertEquals("~~text~~", doc[0].plainText)
        assertNull(doc[1].List[0].checked)
        assertEquals("[x] task", doc[1].List[0][0].plainText)
        assertTrue(doc.elements<MarkdownTable>().none())
        assertFailsWith<IllegalArgumentException> { standard.encodeToString(markdown { p { del("x") } }) }
        assertFailsWith<IllegalArgumentException> { standard.encodeToString(markdown { ul { li("x", checked = true) } }) }
        assertFailsWith<IllegalArgumentException> { standard.encodeToString(markdown { table { header("x") } }) }
    }

    @Test
    fun largeDocumentsAndUnmatchedMarkersRemainUsable() {
        val source = buildString { repeat(5000) { append("## Item ").append(it).append("\n\nText **bold**.\n\n") } }
        val doc = Markdown.parse(source)
        assertEquals(10000, doc.size)
        assertEquals(5000, doc.elements<MarkdownStrong>().count())
        assertEquals(doc, Markdown.parse(Markdown.encodeToString(doc)))
        val brackets = "[".repeat(30000) + "plain"
        assertEquals(brackets, Markdown.parse(brackets).plainText)
        val closed = "[".repeat(30000) + "plain" + "]".repeat(30000)
        assertEquals(closed, Markdown.parse(closed).plainText)
        val ticks = buildString { repeat(1000) { append("`".repeat(it % 23 + 1)).append(" text ") } }
        assertTrue(Markdown.parse(ticks).isNotEmpty())
    }
}
