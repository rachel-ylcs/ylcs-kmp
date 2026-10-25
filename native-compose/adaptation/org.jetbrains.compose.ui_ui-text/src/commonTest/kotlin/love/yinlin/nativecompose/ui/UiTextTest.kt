package love.yinlin.nativecompose.ui

import androidx.compose.runtime.saveable.SaverScope
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.capitalize
import androidx.compose.ui.text.decapitalize
import androidx.compose.ui.text.input.CommitTextCommand
import androidx.compose.ui.text.input.DeleteSurroundingTextInCodePointsCommand
import androidx.compose.ui.text.input.EditProcessor
import androidx.compose.ui.text.input.SetSelectionCommand
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.toLowerCase
import androidx.compose.ui.text.toUpperCase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class UiTextTest {
    @Test
    fun localePreservesLanguageScriptRegionAndCanonicalIdentity() {
        val locale = Locale("zh-Hant-TW")
        assertEquals("zh", locale.language)
        assertEquals("Hant", locale.script)
        assertEquals("TW", locale.region)
        assertEquals("zh-Hant-TW", locale.toLanguageTag())
        assertEquals(Locale("en-US"), Locale("EN-us"))
        assertEquals(Locale("en-US").hashCode(), Locale("EN-us").hashCode())
    }

    @Test
    fun caseConversionHonorsTurkishAndLithuanianRules() {
        val turkish = Locale("tr-TR")
        assertEquals("İIIİ", "iıIİ".toUpperCase(turkish))
        assertEquals("iııi", "iıIİ".toLowerCase(turkish))
        assertEquals("i\u0307\u0301", "I\u0301".toLowerCase(Locale("lt-LT")))
    }

    @Test
    fun caseConversionHandlesExpansionAndContextualGreekSigma() {
        assertEquals("STRASSE", "straße".toUpperCase(Locale("de-DE")))
        assertEquals("ος", "ΟΣ".toLowerCase(Locale("el-GR")))
        assertEquals("A\u0000B", "a\u0000b".toUpperCase(Locale("en-US")))
        assertEquals("\uD801\uDC00", "\uD801\uDC28".toUpperCase(Locale("en-US")))
    }

    @Test
    fun capitalizationTouchesOnlyTheInitialCharacter() {
        val locale = Locale("de-DE")
        assertEquals("Sseta ABC", "ßeta ABC".capitalize(locale))
        assertEquals("iSTANBUL", "İSTANBUL".decapitalize(Locale("tr-TR")))
        assertEquals("", "".capitalize(locale))
        assertEquals("123 abc", "123 abc".capitalize(locale))
    }

    @Test
    fun annotatedTextAdjustsRangesWhenCaseMappingChangesLength() {
        val style = SpanStyle(color = Color.Red)
        val original = AnnotatedString("straße", spanStyles = [AnnotatedString.Range(style, 0, 6)])
        val transformed = original.toUpperCase(Locale("de-DE"))
        assertEquals("STRASSE", transformed.text)
        assertEquals(AnnotatedString.Range(style, 0, 7), transformed.spanStyles.single())
        assertEquals("RAS", transformed.subSequence(2, 5).text)
    }

    @Test
    fun reversedSelectionsAndInputEditsKeepUtf16Boundaries() {
        val selection = TextRange(5, 2)
        assertTrue(selection.reversed)
        assertEquals(3, selection.length)
        val processor = EditProcessor()
        processor.reset(TextFieldValue("A😀B", TextRange(3)), null)
        val deleted = processor.apply([DeleteSurroundingTextInCodePointsCommand(1, 0)])
        assertEquals("AB", deleted.text)
        assertEquals(TextRange(1), deleted.selection)
        val replaced = processor.apply([SetSelectionCommand(1, 2), CommitTextCommand("银临", 1)])
        assertEquals("A银临", replaced.text)
        assertEquals(TextRange(3), replaced.selection)
    }

    @Test
    fun textFieldSaverRestoresTextAndSelectionWithoutImeComposition() {
        val value = TextFieldValue("native compose", TextRange(9, 2), TextRange(0, 6))
        val saver = TextFieldValue.Saver
        val saved = with(saver) { SaverScope { true }.save(value) }
        val restored = assertNotNull(saver.restore(assertNotNull(saved)))
        assertEquals(value.annotatedString, restored.annotatedString)
        assertEquals(value.selection, restored.selection)
        assertEquals(null, restored.composition)
    }

    @Test
    fun textFieldValueConstrainsOutOfRangeSelections() {
        val value = TextFieldValue("ABC", TextRange(20, 1))
        assertEquals(TextRange(3, 1), value.selection)
        assertEquals(2, value.selection.length)
        assertFailsWith<IllegalArgumentException> { TextRange(-1, 0) }
    }
}
