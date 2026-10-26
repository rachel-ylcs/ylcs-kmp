package love.yinlin.nativecompose.consumer

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.clearSkikoComposeImplementation
import androidx.compose.ui.platform.registerSkikoComposeImplementation
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.Paragraph
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.createFontFamilyResolver
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.text.toUpperCase
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastDistinctBy
import androidx.compose.ui.util.fastRoundToInt
import androidx.compose.ui.util.trace
import androidx.window.core.layout.WindowSizeClass
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Runs with only Maven artifacts and the published bridge plugin, without vendor sources. */
class ComposeUiPublicationTest {
    @Test
    fun adaptiveNavigationPublishesTheWindowCoreNativeDependency() {
        // WindowSizeClass is exported through adaptive-navigation -> adaptive-layout -> adaptive.
        val window = WindowSizeClass(900, 620)
        assertTrue(window.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND))
        assertTrue(window.isHeightAtLeastBreakpoint(WindowSizeClass.HEIGHT_DP_MEDIUM_LOWER_BOUND))
    }

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

    @Test
    @OptIn(androidx.compose.ui.InternalComposeUiApi::class)
    fun publishedSkikoBackendRendersThroughCommonComposeApis() {
        registerSkikoComposeImplementation()
        try {
            val image = ImageBitmap(32, 32)
            Canvas(image).drawRect(0f, 0f, 32f, 32f, Paint().apply { color = Color.Blue })
            val pixels = IntArray(32 * 32)
            image.readPixels(pixels)
            assertEquals(Color.Blue.toArgb(), pixels[16 * 32 + 16])
            val paragraph = Paragraph(
                text = "Native Maven text",
                style = TextStyle(fontSize = 16.sp),
                constraints = Constraints(maxWidth = 240),
                density = Density(1f),
                fontFamilyResolver = createFontFamilyResolver(),
                defaultLocaleList = LocaleList(Locale("en-US"))
            )
            assertTrue(paragraph.height > 0f)
            assertTrue(paragraph.lineCount > 0)
        }
        finally {
            clearSkikoComposeImplementation()
        }
    }
}
