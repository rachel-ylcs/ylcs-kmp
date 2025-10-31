package love.yinlin.compose

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

internal val DefaultIcon = ImageVector.Builder(
    name = "ComposeMultiplatform",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 64f,
    viewportHeight = 64f
).path(fill = SolidColor(Colors(0xFF6075F2))) {
    moveTo(56.25f, 18f)
    verticalLineTo(46f)
    lineTo(32f, 60f)
    lineTo(7.75f, 46f)
    verticalLineTo(18f)
    lineTo(32f, 4f)
    close()
}.path(fill = SolidColor(Colors(0xFF6B57FF))) {
    moveToRelative(41.5f, 26.5f)
    verticalLineToRelative(11f)
    lineTo(32f, 43f)
    verticalLineTo(60f)
    lineTo(56.25f, 46f)
    verticalLineTo(18f)
    close()
}.path(
    fill = Brush.radialGradient(
        colorStops = arrayOf(
            0f to Color(0xFF5383EC),
            0.867f to Color(0xFF7F52FF)
        ),
        center = Offset(23.131f, 18.441f),
        radius = 42.132f
    )
) {
    moveToRelative(32f, 43f)
    lineToRelative(-9.5f, -5.5f)
    verticalLineToRelative(-11f)
    lineTo(7.75f, 18f)
    verticalLineTo(46f)
    lineTo(32f, 60f)
    close()
}.path(
    fill = Brush.linearGradient(
        colorStops = arrayOf(
            0f to Color(0xFF33C3FF),
            0.878f to Color(0xFF5383EC)
        ),
        start = Offset(44.172f, 4.377f),
        end = Offset(17.973f, 34.035f)
    )
) {
    moveTo(22.5f, 26.5f)
    lineTo(32f, 21f)
    lineTo(41.5f, 26.5f)
    lineTo(56.25f, 18f)
    lineTo(32f, 4f)
    lineTo(7.75f, 18f)
    close()
}.path(fill = SolidColor(Color.Black)) {
    moveToRelative(32f, 21f)
    lineToRelative(9.526f, 5.5f)
    verticalLineToRelative(11f)
    lineTo(32f, 43f)
    lineTo(22.474f, 37.5f)
    verticalLineToRelative(-11f)
    close()
}.build()