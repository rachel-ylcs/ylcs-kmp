package love.yinlin.compose.ui.text

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import love.yinlin.compose.LocalStyle

@Composable
fun measureTextHeight(text: String, style: TextStyle = LocalStyle.current): Dp {
    val measurer = rememberTextMeasurer(cacheSize = 4)
    val density = LocalDensity.current
    val measureResult = remember(measurer, style, density) {
        measurer.measure(text, style, overflow = TextOverflow.Clip, maxLines = 1, density = density).size.height
    }
    return with(density) { measureResult.toDp() }
}

@Composable
fun measureTextHeight(text1: String, style1: TextStyle, text2: String, style2: TextStyle, calc: (Int, Int) -> Int): Dp {
    val measurer = rememberTextMeasurer(cacheSize = 4)
    val density = LocalDensity.current
    val measureResult = remember(measurer, style1, style2, density) {
        val result1 = measurer.measure(text1, style1, overflow = TextOverflow.Clip, maxLines = 1, density = density).size.height
        val result2 = measurer.measure(text2, style2, overflow = TextOverflow.Clip, maxLines = 1, density = density).size.height
        calc(result1, result2)
    }
    return with(density) { measureResult.toDp() }
}

@Composable
fun measureTextWidth(text: String, style: TextStyle = LocalStyle.current): Dp {
    val measurer = rememberTextMeasurer(cacheSize = 4)
    val density = LocalDensity.current
    val measureResult = remember(measurer, style, density) {
        measurer.measure(text, style, overflow = TextOverflow.Clip, maxLines = 1, density = density).size.width
    }
    return with(density) { measureResult.toDp() }
}

@Composable
fun measureTextWidth(items: List<String>, style: TextStyle = LocalStyle.current, standard: String = ""): Dp {
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val measureResult = remember(items, style, density, standard) {
        var maxText = items.maxByOrNull { it.length } ?: ""
        if (standard.length > maxText.length) maxText = standard
        measurer.measure(maxText, style, overflow = TextOverflow.Clip, maxLines = 1, density = density).size.width
    }
    return with(density) { measureResult.toDp() }
}