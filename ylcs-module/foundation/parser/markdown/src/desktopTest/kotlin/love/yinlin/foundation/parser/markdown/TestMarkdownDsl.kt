package love.yinlin.foundation.parser.markdown

import kotlin.test.*

class TestMarkdownDsl {
    @Test
    fun htmlStyleDslCreatesAccessibleNodes() {
        val doc = markdown {
            h1 { +"Markdown" }
            p { +"Write "; strong { +"Kotlin" }; +" with "; a("https://kotlinlang.org") { +"links" }; br(); code("val x = 1") }
            blockquote { p("A quote") }
            ul { li("done", checked = true); li { p("todo"); ol(start = 2) { li("nested") } } }
            table { header("Feature", "State"); tr("DSL", "ready") }
            hr()
        }
        assertEquals("Markdown", doc[0].Heading[0].Text)
        assertEquals(2, doc[3].List.size)
        assertEquals(true, doc[3].List[0].checked)
        assertEquals(2, doc[3].List[1][1].List.start)
        assertEquals("DSL", assertIs<MarkdownTable>(doc[4]).rows[0][0].plainText)
        assertEquals(doc, Markdown.parse(Markdown.encodeToString(doc)))
        assertEquals(1, doc.elements<MarkdownHeading>().count())
        assertEquals(doc, doc.Document)
        assertNull(doc[0].ParagraphNull)
    }

    @Test
    fun inputCollectionsAndBuildersAreSnapshots() {
        val nodes: MutableList<MarkdownInline> = [MarkdownText("one")]
        val paragraph = MarkdownParagraph(nodes)
        nodes.clear()
        assertEquals("one", paragraph.plainText)
        val blocks: MutableList<MarkdownBlock> = [paragraph]
        val doc = MarkdownDocument(blocks)
        blocks.clear()
        assertEquals(1, doc.size)
        lateinit var blockBuilder: MarkdownBlockBuilder
        lateinit var inlineBuilder: MarkdownInlineBuilder
        val built = markdown { blockBuilder = this; p { inlineBuilder = this; +"old" } }
        blockBuilder.p("later")
        inlineBuilder.text("later")
        assertEquals(1, built.size)
        assertEquals("old", built[0].plainText)
    }

    @Test
    fun nodeEqualityAndTextNormalization() {
        val one = MarkdownParagraph([MarkdownText("a"), MarkdownText("b"), MarkdownText("")])
        assertEquals(MarkdownParagraph("ab"), one)
        assertEquals(MarkdownParagraph("ab").hashCode(), one.hashCode())
        assertNotEquals<MarkdownElement>(MarkdownEmphasis("x"), MarkdownStrong("x"))
        assertNotEquals(MarkdownHeading(1, "x"), MarkdownHeading(2, "x"))
        assertNotEquals(MarkdownLink("a", "x"), MarkdownLink("b", "x"))
        val doc = markdown { h2("x"); p("y") }
        assertEquals(["x", "y"], doc.elements<MarkdownText>().map { it.content }.toList())
        assertEquals("x\ny", doc.plainText)
    }

    @Test
    fun tablesValidateDimensionsAndSnapshotInputs() {
        assertFailsWith<IllegalArgumentException> { markdown { table { tr("missing header") } } }
        assertFailsWith<IllegalArgumentException> { markdown { table { header("a", "b"); tr("short") } } }
        assertFailsWith<IllegalArgumentException> { MarkdownHeading(7, "wrong") }
        assertFailsWith<IllegalArgumentException> { MarkdownList([]) }
        assertFailsWith<IllegalArgumentException> { MarkdownList([MarkdownListItem([])], start = 3) }
        assertFailsWith<IllegalArgumentException> { MarkdownCodeBlock("code", "bad\ninfo") }
        val align = [MarkdownAlignment.Center]
        val rows = [MarkdownTableRow([MarkdownTableCell("a")])]
        val table = MarkdownTable(rows[0], align, rows)
        assertEquals(1, table.rows.size)
        assertEquals([MarkdownAlignment.Center], table.alignments)
    }
}
