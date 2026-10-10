package love.yinlin.foundation.parser.markdown

import kotlin.test.*

class TestMarkdownParser {
    @Test
    fun headingsParagraphsAndLineEndings() {
        val doc = Markdown.parse("# Title ###\r\n\r\nSubtitle\r---\r\rFirst\nsecond\n\n###### Sixth\n\n####### plain")
        assertEquals(5, doc.size)
        assertEquals(MarkdownHeading(1, "Title"), doc[0])
        assertEquals(MarkdownHeading(2, "Subtitle"), doc[1])
        assertEquals([MarkdownText("First"), MarkdownSoftBreak, MarkdownText("second")], doc[2].Paragraph.children)
        assertEquals(6, doc[3].Heading.level)
        assertEquals("####### plain", doc[4].plainText)
        assertEquals(0, Markdown.parse("\n \t\n").size)
        assertEquals("a\ufffdb", Markdown.parse("a\u0000b").plainText)
    }

    @Test
    fun thematicBreakPrecedenceAndEscapedMarkers() {
        val doc = Markdown.parse("---\n\n* * *\n\n___\n\n\\# literal\n\\- literal\n\nabc\n---")
        assertEquals(5, doc.size)
        assertTrue(doc.take(3).all { it == MarkdownThematicBreak })
        assertEquals("# literal\n- literal", doc[3].plainText)
        assertEquals(MarkdownHeading(2, "abc"), doc[4])
        assertEquals(MarkdownParagraph("#no space"), Markdown.parse("#no space")[0])
    }

    @Test
    fun quotesSupportNestingAndLazyParagraphContinuation() {
        val doc = Markdown.parse("> first\nlazy\n>\n> > nested\n>\n> - one\n> - two\n\noutside")
        val quote = assertIs<MarkdownBlockQuote>(doc[0])
        assertEquals(3, quote.size)
        assertEquals("first\nlazy", quote[0].plainText)
        assertEquals("nested", assertIs<MarkdownBlockQuote>(quote[1])[0].plainText)
        assertEquals(2, quote[2].List.size)
        assertEquals("outside", doc[1].plainText)
    }

    @Test
    fun nestedListsTasksAndOrderedStart() {
        val doc = Markdown.parse("3. first\n   - [x] done\n   - [ ] todo\n4. second\n\n- separate")
        val ordered = doc[0].List
        assertEquals(3, ordered.start)
        assertTrue(ordered.ordered)
        assertTrue(ordered.tight)
        val tasks = ordered[0][1].List
        assertEquals(true, tasks[0].checked)
        assertEquals(false, tasks[1].checked)
        assertEquals("done", tasks[0][0].plainText)
        assertEquals("second", ordered[1][0].plainText)
        assertFalse(doc[1].List.ordered)
    }

    @Test
    fun looseListsAndMarkerChanges() {
        val doc = Markdown.parse("- first\n\n  another paragraph\n\n- second\n\n+ third\n\n1) ordered")
        assertEquals(3, doc.size)
        val list = doc[0].List
        assertFalse(list.tight)
        assertEquals(2, list[0].size)
        assertEquals("another paragraph", list[0][1].plainText)
        assertEquals("third", doc[1].List[0][0].plainText)
        assertTrue(doc[2].List.ordered)
    }

    @Test
    fun listInterruptionsAndEmptyItems() {
        assertEquals(1, Markdown.parse("paragraph\n2. continuation").size)
        assertEquals(2, Markdown.parse("paragraph\n1. item").size)
        val doc = Markdown.parse("-\n- value\n- [ ]")
        assertEquals(3, doc[0].List.size)
        assertTrue(doc[0].List[0].isEmpty())
        assertEquals(false, doc[0].List[2].checked)
        assertTrue(doc[0].List[2].isEmpty())
    }

    @Test
    fun varyingMarkerIndentationRemainsOneList() {
        val doc = Markdown.parse("- a\n - b\n  - c\n   - d")
        assertEquals(1, doc.size)
        assertEquals(["a", "b", "c", "d"], doc[0].List.map { it[0].plainText })
        val ordered = Markdown.parse("1. a\n\n  2. b\n\n   3. c")[0].List
        assertEquals(3, ordered.size)
        assertFalse(ordered.tight)
    }

    @Test
    fun blankLinesInsideCodeDoNotLoosenListsOrChangeCode() {
        val doc = Markdown.parse("- a\n- ```\n  code\n\n\n  ```\n- c")
        assertTrue(doc[0].List.tight)
        assertEquals("code\n\n\n", assertIs<MarkdownCodeBlock>(doc[0].List[1][0]).content)
        assertEquals(doc, Markdown.parse(doc.toString()))
    }

    @Test
    fun fencedCodeHandlesIndentationAndUnclosedFences() {
        val source = "  ```` kotlin\n  val x = `code`\n  ```\n  ````\n\n~~~ text\nunclosed"
        val doc = Markdown.parse(source)
        val code = assertIs<MarkdownCodeBlock>(doc[0])
        assertEquals("kotlin", code.info)
        assertEquals("val x = `code`\n```\n", code.content)
        assertEquals("unclosed\n", assertIs<MarkdownCodeBlock>(doc[1]).content)
        assertEquals("", assertIs<MarkdownCodeBlock>(Markdown.parse("```\n```")[0]).content)
    }

    @Test
    fun indentedCodeAndTabsDoNotInterruptParagraphs() {
        val doc = Markdown.parse("    one\n\n\ttwo\n\nparagraph\n    continuation")
        assertEquals("one\n\ntwo\n", assertIs<MarkdownCodeBlock>(doc[0]).content)
        assertEquals("paragraph\ncontinuation", doc[1].plainText)
        val list = Markdown.parse("- item\n\n      code")[0].List
        assertEquals("code\n", assertIs<MarkdownCodeBlock>(list[0][1]).content)
    }

    @Test
    fun commonRawHtmlBlocksArePreserved() {
        val source = "<!-- comment\ncontinued -->\n\n<div>\n**raw**\n</div>\n\nend"
        val doc = Markdown.parse(source)
        assertEquals("<!-- comment\ncontinued -->", assertIs<MarkdownHtmlBlock>(doc[0]).content)
        assertEquals("<div>\n**raw**\n</div>", assertIs<MarkdownHtmlBlock>(doc[1]).content)
        assertEquals("end", doc[2].plainText)
    }

    @Test
    fun referenceDefinitionsResolveForwardAndAcrossContainers() {
        val source = "[shown][ ref ] and [REF][] and [ref]\n\n> [ref]: /first \"Title\"\n\n[REF]: /ignored"
        val doc = Markdown.parse(source)
        val links = doc.elements<MarkdownLink>().toList()
        assertEquals(3, links.size)
        assertTrue(links.all { it.destination == "/first" && it.title == "Title" })
        assertEquals("shown", links[0].plainText)
        assertEquals("REF", links[1].plainText)
        assertEquals("[missing]", Markdown.parse("[missing]").plainText)
        assertEquals("[a]: invalid target extra", Markdown.parse("[a]: invalid target extra").plainText)
    }
}
