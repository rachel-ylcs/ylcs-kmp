package love.yinlin.foundation.parser.markdown

import kotlin.test.*

class TestMarkdownInline {
    private fun inline(source: String): List<MarkdownInline> = Markdown.parse(source)[0].Paragraph.children

    @Test
    fun emphasisStrongAndNestedDelimiters() {
        val nodes = inline("plain *em* **strong** ***both*** ~~old~~")
        assertEquals("em", assertIs<MarkdownEmphasis>(nodes[1]).plainText)
        assertEquals("strong", assertIs<MarkdownStrong>(nodes[3]).plainText)
        assertIs<MarkdownStrong>(assertIs<MarkdownEmphasis>(nodes[5])[0])
        assertEquals("old", assertIs<MarkdownStrikethrough>(nodes[7]).plainText)
        val nested = assertIs<MarkdownStrong>(inline("**bold *inner* bold**").single())
        assertEquals("inner", assertIs<MarkdownEmphasis>(nested[1]).plainText)
    }

    @Test
    fun intrawordUnderscoresWhitespaceAndUnmatchedDelimiters() {
        assertEquals([MarkdownText("foo_bar_baz")], inline("foo_bar_baz"))
        assertEquals([MarkdownText("literal * no* **open and ~~~literal~~~")], inline("literal * no* **open and ~~~literal~~~"))
        assertEquals("word", assertIs<MarkdownEmphasis>(inline("_word_").single()).plainText)
        assertEquals("b", assertIs<MarkdownEmphasis>(inline("a*b*c")[1]).plainText)
        assertEquals([MarkdownText("*"), MarkdownEmphasis("a")], inline("**a*"))
    }

    @Test
    fun codeSpansNormalizeSpacesAndChooseMatchingRunLength() {
        val nodes = inline("` *literal* ` and `` a ` b `` and `one\ntwo` and ```unmatched")
        val codes = nodes.filterIsInstance<MarkdownCode>()
        assertEquals(["*literal*", "a ` b", "one two"], codes.map { it.content })
        assertEquals(" and ```unmatched", assertIs<MarkdownText>(nodes.last()).content)
        assertEquals("   ", assertIs<MarkdownCode>(inline("`   `").single()).content)
    }

    @Test
    fun punctuationEscapesAndEntitiesDoNotBecomeSyntax() {
        val nodes = inline("\\*literal\\* &amp; &lt; &#65; &#x1F600; &#0; &unknown; \\q")
        assertEquals([MarkdownText("*literal* & < A 😀 \ufffd &unknown; \\q")], nodes)
        assertEquals([MarkdownText("*")], inline("&#42;"))
    }

    @Test
    fun inlineLinksImagesBalancedDestinationsAndTitles() {
        val doc = Markdown.parse("[**label**](path_(part) \"a \\\"title\\\"\") ![*alt*](<image name.png> 'photo')")
        val link = doc.elements<MarkdownLink>().single()
        assertEquals("path_(part)", link.destination)
        assertEquals("a \"title\"", link.title)
        assertIs<MarkdownStrong>(link[0])
        val image = doc.elements<MarkdownImage>().single()
        assertEquals("image name.png", image.destination)
        assertEquals("alt", image.alt)
        assertEquals("photo", image.title)
        assertEquals("", Markdown.parse("[empty]()").elements<MarkdownLink>().single().destination)
    }

    @Test
    fun autolinksAndInlineHtml() {
        val doc = Markdown.parse("Go <https://example.com/a> <user@example.com> <em>html</em>")
        val links = doc.elements<MarkdownLink>().toList()
        assertEquals(["https://example.com/a", "mailto:user@example.com"], links.map { it.destination })
        assertEquals(["<em>", "</em>"], doc.elements<MarkdownHtmlInline>().map { it.content }.toList())
    }

    @Test
    fun hardAndSoftBreaks() {
        val nodes = inline("first  \nsecond\\\nthird\nfourth")
        assertEquals([MarkdownText("first"), MarkdownHardBreak, MarkdownText("second"), MarkdownHardBreak,
            MarkdownText("third"), MarkdownSoftBreak, MarkdownText("fourth")], nodes)
    }

    @Test
    fun linksCannotNestButImageLabelsCanContainLinks() {
        val doc = Markdown.parse("[outer [inner](a)](b) ![image [link](c)](d)")
        assertEquals(["a", "c"], doc.elements<MarkdownLink>().map { it.destination }.toList())
        assertEquals("image link", doc.elements<MarkdownImage>().single().alt)
    }

    @Test
    fun referenceDefinitionWithFollowingTitle() {
        val doc = Markdown.parse("[a]\n\n[a]:\n  <some path>\n  'caption'")
        val link = doc.elements<MarkdownLink>().single()
        assertEquals("some path", link.destination)
        assertEquals("caption", link.title)
    }
}
