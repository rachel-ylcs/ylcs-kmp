package love.yinlin.compose

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Stable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Stable
data class SizeTheme(
    val box1: Dp,
    val box2: Dp,
    val box3: Dp,
    val box4: Dp,
    val icon: Dp,
    val image1: Dp,
    val image2: Dp,
    val image3: Dp,
    val image4: Dp,
    val image5: Dp,
    val image6: Dp,
    val image7: Dp,
    val image8: Dp,
    val image9: Dp,
    val image10: Dp,
    val input1: Dp,
    val input2: Dp,
    val input3: Dp,
    val input4: Dp,
    val input5: Dp,
    val input6: Dp,
    val input7: Dp,
    val input8: Dp,
    val input9: Dp,
    val input10: Dp,
    val cell1: Dp,
    val cell2: Dp,
    val cell3: Dp,
    val cell4: Dp,
    val cell5: Dp,
    val cell6: Dp,
    val cell7: Dp,
    val cell8: Dp,
    val cell9: Dp,
    val cell10: Dp,
)

/**
 * @param h: 横向间距
 * @param v: 纵向间距
 * @param g: 小间隙
 */
@Stable
data class PaddingTheme(
    val h: Dp,
    val v: Dp,
    val g: Dp,
) {
    val h1: Dp = h * 10f
    val h2: Dp = h * 7.5f
    val h3: Dp = h * 5f
    val h4: Dp = h * 4f
    val h5: Dp = h * 3f
    val h6: Dp = h * 2.5f
    val h7: Dp = h * 2f
    val h8: Dp = h * 1.75f
    val h9: Dp = h * 1.5f
    val h10: Dp = h * 1.25f

    val v1: Dp = v * 10f
    val v2: Dp = v * 7.5f
    val v3: Dp = v * 5f
    val v4: Dp = v * 4f
    val v5: Dp = v * 3f
    val v6: Dp = v * 2.5f
    val v7: Dp = v * 2f
    val v8: Dp = v * 1.75f
    val v9: Dp = v * 1.5f
    val v10: Dp = v * 1.25f

    val g1: Dp = g * 4f
    val g2: Dp = g * 3.5f
    val g3: Dp = g * 3f
    val g4: Dp = g * 2.75f
    val g5: Dp = g * 2.5f
    val g6: Dp = g * 2.25f
    val g7: Dp = g * 2f
    val g8: Dp = g * 1.75f
    val g9: Dp = g * 1.5f
    val g10: Dp = g * 1.25f

    val e: Dp = (h + v) / 2
    val e1: Dp = (h1 + v1) / 2
    val e2: Dp = (h2 + v2) / 2
    val e3: Dp = (h3 + v3) / 2
    val e4: Dp = (h4 + v4) / 2
    val e5: Dp = (h5 + v5) / 2
    val e6: Dp = (h6 + v6) / 2
    val e7: Dp = (h7 + v7) / 2
    val e8: Dp = (h8 + v8) / 2
    val e9: Dp = (h9 + v9) / 2
    val e10: Dp = (h10 + v10) / 2

    val value: PaddingValues = PaddingValues(horizontal = h, vertical = v)
    val value1: PaddingValues = PaddingValues(horizontal = h1, vertical = v1)
    val value2: PaddingValues = PaddingValues(horizontal = h2, vertical = v2)
    val value3: PaddingValues = PaddingValues(horizontal = h3, vertical = v3)
    val value4: PaddingValues = PaddingValues(horizontal = h4, vertical = v4)
    val value5: PaddingValues = PaddingValues(horizontal = h5, vertical = v5)
    val value6: PaddingValues = PaddingValues(horizontal = h6, vertical = v6)
    val value7: PaddingValues = PaddingValues(horizontal = h7, vertical = v7)
    val value8: PaddingValues = PaddingValues(horizontal = h8, vertical = v8)
    val value9: PaddingValues = PaddingValues(horizontal = h9, vertical = v9)
    val value10: PaddingValues = PaddingValues(horizontal = h10, vertical = v10)

    val eValue: PaddingValues = PaddingValues(all = e)
    val eValue1: PaddingValues = PaddingValues(all = e1)
    val eValue2: PaddingValues = PaddingValues(all = e2)
    val eValue3: PaddingValues = PaddingValues(all = e3)
    val eValue4: PaddingValues = PaddingValues(all = e4)
    val eValue5: PaddingValues = PaddingValues(all = e5)
    val eValue6: PaddingValues = PaddingValues(all = e6)
    val eValue7: PaddingValues = PaddingValues(all = e7)
    val eValue8: PaddingValues = PaddingValues(all = e8)
    val eValue9: PaddingValues = PaddingValues(all = e9)
    val eValue10: PaddingValues = PaddingValues(all = e10)
}

@Stable
data class BorderTheme(
    val v1: Dp,
    val v2: Dp,
    val v3: Dp,
    val v4: Dp,
    val v5: Dp,
    val v6: Dp,
    val v7: Dp,
    val v8: Dp,
    val v9: Dp,
    val v10: Dp,
)

@Stable
data class ShadowTheme(
    val v1: Dp,
    val v2: Dp,
    val v3: Dp,
    val v4: Dp,
    val v5: Dp,
    val v6: Dp,
    val v7: Dp,
    val v8: Dp,
    val v9: Dp,
    val v10: Dp,
)

@Stable
data class GeometryTheme(
    val size: SizeTheme,
    val padding: PaddingTheme,
    val border: BorderTheme,
    val shadow: ShadowTheme,
) {
    companion object {
        val Default = GeometryTheme(
            size = SizeTheme(
                icon = 24.dp,
                box1 = 16.dp,
                box2 = 12.dp,
                box3 = 8.dp,
                box4 = 4.dp,
                image1 = 256.dp,
                image2 = 196.dp,
                image3 = 144.dp,
                image4 = 128.dp,
                image5 = 96.dp,
                image6 = 81.dp,
                image7 = 64.dp,
                image8 = 48.dp,
                image9 = 32.dp,
                image10 = 24.dp,
                input1 = 196.dp,
                input2 = 144.dp,
                input3 = 128.dp,
                input4 = 100.dp,
                input5 = 81.dp,
                input6 = 64.dp,
                input7 = 48.dp,
                input8 = 36.dp,
                input9 = 28.dp,
                input10 = 20.dp,
                cell1 = 300.dp,
                cell2 = 250.dp,
                cell3 = 200.dp,
                cell4 = 150.dp,
                cell5 = 125.dp,
                cell6 = 100.dp,
                cell7 = 81.dp,
                cell8 = 75.dp,
                cell9 = 64.dp,
                cell10 = 48.dp,
            ),
            padding = PaddingTheme(
                h = 10.dp,
                v = 5.dp,
                g = 1.dp,
            ),
            border = BorderTheme(
                v1 = 8.dp,
                v2 = 6.dp,
                v3 = 4.dp,
                v4 = 3.dp,
                v5 = 2.dp,
                v6 = 1.5.dp,
                v7 = 1.dp,
                v8 = 0.75.dp,
                v9 = 0.5.dp,
                v10 = 0.25.dp,
            ),
            shadow = ShadowTheme(
                v1 = 8.dp,
                v2 = 6.dp,
                v3 = 4.dp,
                v4 = 3.dp,
                v5 = 2.dp,
                v6 = 1.5.dp,
                v7 = 1.dp,
                v8 = 0.75.dp,
                v9 = 0.5.dp,
                v10 = 0.25.dp,
            ),
        )
    }
}