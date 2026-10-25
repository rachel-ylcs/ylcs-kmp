package love.yinlin.nativecompose.consumer

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.toUpperCase
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastDistinctBy
import androidx.compose.ui.util.fastRoundToInt
import androidx.compose.ui.util.trace
import kotlin.test.Test
import kotlin.test.assertEquals

/** Runs with only Maven artifacts and the published bridge plugin, without vendor sources. */
class ComposeUiPublicationTest {
    @Test
    fun geometryAndUnitsLinkAcrossThePublishedModules() {
        val rect = Rect(0f, 0f, 80f, 60f).translate(Offset(10f, 20f))
        assertEquals(Offset(50f, 50f), rect.center)
        with(Density(2f, 1.5f)) {
            assertEquals(48f, 16.sp.toPx())
            assertEquals(24, 12.dp.roundToPx())
        }
    }

    @Test
    fun nativeActualsAndTransitiveCollectionsExecute() {
        assertEquals(0, Float.NaN.fastRoundToInt())
        assertEquals(42, trace("maven-consumer") { 42 })
        val values: List<Int> = [1, 2, 1, 3]
        val expected: List<Int> = [1, 2, 3]
        assertEquals(expected, values.fastDistinctBy { it })
    }

    @Test
    fun publishedTextUsesTheNativeLocaleAndStaticIcuBridge() {
        val locale = Locale("tr-TR")
        assertEquals("tr", locale.language)
        assertEquals("TR", locale.region)
        assertEquals("İSTANBUL / STRASSE", AnnotatedString("istanbul / straße").toUpperCase(locale).text)
    }
}
