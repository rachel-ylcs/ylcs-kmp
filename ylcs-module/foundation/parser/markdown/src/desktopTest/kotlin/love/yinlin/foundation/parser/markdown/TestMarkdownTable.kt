package love.yinlin.foundation.parser.markdown

import kotlin.test.*

class TestMarkdownTable {
    @Test
    fun tableAlignmentInlineContentAndRowPadding() {
        val doc = Markdown.parse("| Left | Center | Right |\n| :--- | :---: | ---: |\n| *one* | **two** | `three` |\n| short |\n| a | b | c | ignored |\n\nafter")
        val table = assertIs<MarkdownTable>(doc[0])
        assertEquals([MarkdownAlignment.Left, MarkdownAlignment.Center, MarkdownAlignment.Right], table.alignments)
        assertEquals(3, table.rows.size)
        assertIs<MarkdownEmphasis>(table.rows[0][0].children.single())
        assertIs<MarkdownStrong>(table.rows[0][1].children.single())
        assertIs<MarkdownCode>(table.rows[0][2].children.single())
        assertEquals("", table.rows[1][1].plainText)
        assertEquals("c", table.rows[2][2].plainText)
        assertEquals("after", doc[1].plainText)
    }

    @Test
    fun escapedPipesWorkInsideCodeAndText() {
        val table = assertIs<MarkdownTable>(Markdown.parse("a | b\n--- | ---\nx\\|y | `a\\|b`")[0])
        assertEquals("x|y", table.rows[0][0].plainText)
        assertEquals("a|b", assertIs<MarkdownCode>(table.rows[0][1].children.single()).content)
        val trailing = assertIs<MarkdownTable>(Markdown.parse("a | b\n--- | ---\nx | y\\|\nshort")[0])
        assertEquals("y|", trailing.rows[0][1].plainText)
        assertEquals("short", trailing.rows[1][0].plainText)
        assertEquals("", trailing.rows[1][1].plainText)
    }

    @Test
    fun invalidSeparatorsRemainParagraphs() {
        assertTrue(Markdown.parse("a | b\n--- | invalid").elements<MarkdownTable>().none())
        assertTrue(Markdown.parse("a | b\n---").elements<MarkdownTable>().none())
        assertTrue(Markdown.parse("a | b\n--- | --- | ---").elements<MarkdownTable>().none())
    }
}
