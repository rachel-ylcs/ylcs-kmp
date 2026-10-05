package love.yinlin.foundation.parser.markdown

import kotlin.test.*

class TestMarkdownWriter {
    private fun roundTrip(doc: MarkdownDocument) {
        val encoded = Markdown.encodeToString(doc)
        val parsed = Markdown.parse(encoded)
        assertEquals(doc, parsed, encoded)
        assertEquals(encoded, Markdown.encodeToString(parsed), "Canonical output must be stable")
    }

    @Test
    fun mixedDocumentRoundTrip() {
        roundTrip(Markdown.parse("# Heading\n\nText *em* and **strong**, [link](https://example.com \"title\") and ![alt](a.png).\n\n> quote\n>\n> - first\n> - second\n\n3. one\n   - [x] nested\n4. two\n\n---\n\n``` kotlin\nval x = 1\n```"))
    }

    @Test
    fun literalTextCannotInjectBlockOrInlineSyntax() {
        val literal = "# title\n- item\n1. numbered\n---\n> quote\n**bold** [link](url) &amp; <html> | table |"
        val doc = MarkdownDocument([MarkdownParagraph(literal)])
        roundTrip(doc)
        assertEquals(literal, Markdown.parse(doc.toString()).plainText)
        roundTrip(MarkdownDocument([MarkdownParagraph("  leading and trailing  ")]))
    }

    @Test
    fun codeSpansAndFencesAvoidDelimiterCollisions() {
        for (code in ["`start", "end`", "a `` b", " spaced ", "   "]) {
            roundTrip(markdown { p { code(code) } })
        }
        val doc = markdown {
            codeBlock("before\n```\n````\nafter", "kotlin")
            codeBlock("~~~\ntext", "language`name")
            codeBlock("")
            codeBlock("trimmed info", " kotlin ")
        }
        roundTrip(doc)
        assertTrue(doc.toString().startsWith("````` kotlin"))
        assertEquals("kotlin", assertIs<MarkdownCodeBlock>(doc.last()).info)
    }

    @Test
    fun nestedAndAdjacentFormattingRoundTrip() {
        roundTrip(markdown {
            p {
                +"before"
                em { +"a"; em { +"b" }; +"c" }
                strong { +"one"; strong { +"two" }; +"three" }
                +"after"
            }
        })
        roundTrip(markdown { p { em("one"); strong("two"); em("three") } })
        roundTrip(markdown { p { em(" spaced "); del("old") } })
    }

    @Test
    fun linksAndTitlesEscapeSpecialCharacters() {
        roundTrip(markdown { p { a("a>b\\c&d<e path", "label [x]", "a\"b\\c&d\nnext") } })
    }

    @Test
    fun listsQuotesAndAdjacentListsPreserveStructure() {
        roundTrip(markdown {
            ul { li("one"); li { p("two"); ul { li("nested") } } }
            ul { li("separate") }
            ul { li("third list") }
            ol(start = 999_999_999) { li("large"); li("next") }
            ol { li("other list") }
            blockquote { p("line one\nline two"); codeBlock("a\nb") }
        })
        roundTrip(Markdown.parse("- first\n\n  second paragraph\n\n- last"))
        roundTrip(Markdown.parse("-\n\n  foo"))
        roundTrip(markdown { ul(tight = false) { li("single loose item") } })
        roundTrip(Markdown.parse("- first\n\n      before\n\n\n      after"))
    }

    @Test
    fun tablesEscapePipesIncludingCodeAndLinks() {
        roundTrip(markdown {
            table {
                header("Name", "Value")
                align(MarkdownAlignment.Left, MarkdownAlignment.Right)
                tr { td("a|b"); td { code("c|d") } }
                tr { td { a("path|part", "a|b", "title|text") }; td { strong("bold") } }
            }
        })
    }

    @Test
    fun emptyDocumentsAndInlineFragments() {
        assertEquals("", MarkdownDocument(emptyList()).toString())
        assertEquals("*word*", MarkdownEmphasis("word").toString())
        assertEquals("`code`", MarkdownCode("code").toString())
        assertFailsWith<IllegalArgumentException> { MarkdownCode("").toString() }
        assertFailsWith<IllegalArgumentException> { MarkdownEmphasis(emptyList()).toString() }
        assertFailsWith<IllegalArgumentException> { markdown { p { a("outer") { a("inner", "bad") } } }.toString() }
    }
}
