package love.yinlin.common

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

object ExtraIcons {
    val OrderMode: ImageVector by lazy {
        ImageVector.Builder(
            name = "OrderMode",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 16f,
            viewportHeight = 16f
        ). path(
            fill = SolidColor(Color(0xFF000000)),
            fillAlpha = 1.0f,
            stroke = null,
            strokeAlpha = 1.0f,
            strokeLineWidth = 1.0f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Miter,
            strokeLineMiter = 1.0f,
            pathFillType = PathFillType.NonZero
        ) {
            moveTo(11f, 5.466f)
            verticalLineTo(4f)
            horizontalLineTo(5f)
            arcToRelative(4f, 4f, 0f, isMoreThanHalf = false, isPositiveArc = false, -3.584f, 5.777f)
            arcToRelative(0.5f, 0.5f, 0f, isMoreThanHalf = true, isPositiveArc = true, -0.896f, 0.446f)
            arcTo(5f, 5f, 0f, isMoreThanHalf = false, isPositiveArc = true, 5f, 3f)
            horizontalLineToRelative(6f)
            verticalLineTo(1.534f)
            arcToRelative(0.25f, 0.25f, 0f, isMoreThanHalf = false, isPositiveArc = true, 0.41f, -0.192f)
            lineToRelative(2.36f, 1.966f)
            curveToRelative(0.12f, 0.1f, 0.12f, 0.284f, 0f, 0.384f)
            lineToRelative(-2.36f, 1.966f)
            arcToRelative(0.25f, 0.25f, 0f, isMoreThanHalf = false, isPositiveArc = true, -0.41f, -0.192f)
            moveToRelative(3.81f, 0.086f)
            arcToRelative(0.5f, 0.5f, 0f, isMoreThanHalf = false, isPositiveArc = true, 0.67f, 0.225f)
            arcTo(5f, 5f, 0f, isMoreThanHalf = false, isPositiveArc = true, 11f, 13f)
            horizontalLineTo(5f)
            verticalLineToRelative(1.466f)
            arcToRelative(0.25f, 0.25f, 0f, isMoreThanHalf = false, isPositiveArc = true, -0.41f, 0.192f)
            lineToRelative(-2.36f, -1.966f)
            arcToRelative(0.25f, 0.25f, 0f, isMoreThanHalf = false, isPositiveArc = true, 0f, -0.384f)
            lineToRelative(2.36f, -1.966f)
            arcToRelative(0.25f, 0.25f, 0f, isMoreThanHalf = false, isPositiveArc = true, 0.41f, 0.192f)
            verticalLineTo(12f)
            horizontalLineToRelative(6f)
            arcToRelative(4f, 4f, 0f, isMoreThanHalf = false, isPositiveArc = false, 3.585f, -5.777f)
            arcToRelative(0.5f, 0.5f, 0f, isMoreThanHalf = false, isPositiveArc = true, 0.225f, -0.67f)
            close()
        }.build()
    }

    val LoopMode: ImageVector by lazy {
        ImageVector.Builder(
            name = "LoopMode",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 16f,
            viewportHeight = 16f
        ).path(
            fill = SolidColor(Color(0xFF000000)),
            fillAlpha = 1.0f,
            stroke = null,
            strokeAlpha = 1.0f,
            strokeLineWidth = 1.0f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Miter,
            strokeLineMiter = 1.0f,
            pathFillType = PathFillType.NonZero
        ) {
            moveTo(11f, 4f)
            verticalLineToRelative(1.466f)
            arcToRelative(0.25f, 0.25f, 0f, isMoreThanHalf = false, isPositiveArc = false, 0.41f, 0.192f)
            lineToRelative(2.36f, -1.966f)
            arcToRelative(0.25f, 0.25f, 0f, isMoreThanHalf = false, isPositiveArc = false, 0f, -0.384f)
            lineToRelative(-2.36f, -1.966f)
            arcToRelative(0.25f, 0.25f, 0f, isMoreThanHalf = false, isPositiveArc = false, -0.41f, 0.192f)
            verticalLineTo(3f)
            horizontalLineTo(5f)
            arcToRelative(5f, 5f, 0f, isMoreThanHalf = false, isPositiveArc = false, -4.48f, 7.223f)
            arcToRelative(0.5f, 0.5f, 0f, isMoreThanHalf = false, isPositiveArc = false, 0.896f, -0.446f)
            arcTo(4f, 4f, 0f, isMoreThanHalf = false, isPositiveArc = true, 5f, 4f)
            close()
            moveToRelative(4.48f, 1.777f)
            arcToRelative(0.5f, 0.5f, 0f, isMoreThanHalf = false, isPositiveArc = false, -0.896f, 0.446f)
            arcTo(4f, 4f, 0f, isMoreThanHalf = false, isPositiveArc = true, 11f, 12f)
            horizontalLineTo(5.001f)
            verticalLineToRelative(-1.466f)
            arcToRelative(0.25f, 0.25f, 0f, isMoreThanHalf = false, isPositiveArc = false, -0.41f, -0.192f)
            lineToRelative(-2.36f, 1.966f)
            arcToRelative(0.25f, 0.25f, 0f, isMoreThanHalf = false, isPositiveArc = false, 0f, 0.384f)
            lineToRelative(2.36f, 1.966f)
            arcToRelative(0.25f, 0.25f, 0f, isMoreThanHalf = false, isPositiveArc = false, 0.41f, -0.192f)
            verticalLineTo(13f)
            horizontalLineToRelative(6f)
            arcToRelative(5f, 5f, 0f, isMoreThanHalf = false, isPositiveArc = false, 4.48f, -7.223f)
            close()
        }.path(
            fill = SolidColor(Color(0xFF000000)),
            fillAlpha = 1.0f,
            stroke = null,
            strokeAlpha = 1.0f,
            strokeLineWidth = 1.0f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Miter,
            strokeLineMiter = 1.0f,
            pathFillType = PathFillType.NonZero
        ) {
            moveTo(9f, 5.5f)
            arcToRelative(0.5f, 0.5f, 0f, isMoreThanHalf = false, isPositiveArc = false, -0.854f, -0.354f)
            lineToRelative(-1.75f, 1.75f)
            arcToRelative(0.5f, 0.5f, 0f, isMoreThanHalf = true, isPositiveArc = false, 0.708f, 0.708f)
            lineTo(8f, 6.707f)
            verticalLineTo(10.5f)
            arcToRelative(0.5f, 0.5f, 0f, isMoreThanHalf = false, isPositiveArc = false, 1f, 0f)
            close()
        }.build()
    }

    val ShuffleMode: ImageVector by lazy {
        ImageVector.Builder(
            name = "ShuffleMode",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 15f,
            viewportHeight = 15f
        ).path(
            fill = SolidColor(Color(0xFF000000)),
            fillAlpha = 1.0f,
            stroke = null,
            strokeAlpha = 1.0f,
            strokeLineWidth = 1.0f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Miter,
            strokeLineMiter = 1.0f,
            pathFillType = PathFillType.EvenOdd
        ) {
            moveTo(12.3536f, 1.14645f)
            curveTo(12.1583f, 0.9512f, 11.8417f, 0.9512f, 11.6464f, 1.1464f)
            curveTo(11.4512f, 1.3417f, 11.4512f, 1.6583f, 11.6464f, 1.8536f)
            lineTo(12.7929f, 3f)
            horizontalLineTo(12f)
            curveTo(10.7037f, 3f, 9.7111f, 3.5842f, 8.8725f, 4.3893f)
            curveTo(8.2006f, 5.0343f, 7.5935f, 5.8568f, 6.9946f, 6.6682f)
            curveTo(6.8629f, 6.8467f, 6.7315f, 7.0246f, 6.6f, 7.2f)
            curveTo(5.1087f, 9.1883f, 3.4904f, 11f, 0.5f, 11f)
            curveTo(0.2239f, 11f, 0f, 11.2239f, 0f, 11.5f)
            curveTo(0f, 11.7761f, 0.2239f, 12f, 0.5f, 12f)
            curveTo(4.0096f, 12f, 5.8913f, 9.8117f, 7.4f, 7.8f)
            curveTo(7.5437f, 7.6085f, 7.6832f, 7.4196f, 7.82f, 7.2345f)
            lineTo(7.82005f, 7.23443f)
            lineTo(7.82006f, 7.23441f)
            curveTo(8.4167f, 6.4269f, 8.9607f, 5.6909f, 9.565f, 5.1107f)
            curveTo(10.2889f, 4.4158f, 11.0463f, 4f, 12f, 4f)
            horizontalLineTo(12.7929f)
            lineTo(11.6464f, 5.14645f)
            curveTo(11.4512f, 5.3417f, 11.4512f, 5.6583f, 11.6464f, 5.8536f)
            curveTo(11.8417f, 6.0488f, 12.1583f, 6.0488f, 12.3536f, 5.8536f)
            lineTo(14.3536f, 3.85355f)
            curveTo(14.5488f, 3.6583f, 14.5488f, 3.3417f, 14.3536f, 3.1465f)
            lineTo(12.3536f, 1.14645f)
            close()
            moveTo(0.5f, 3f)
            curveTo(3.3528f, 3f, 5.1299f, 4.4459f, 6.5055f, 6.0675f)
            lineTo(6.3762f, 6.24266f)
            curveTo(6.2483f, 6.4161f, 6.1229f, 6.5861f, 6f, 6.75f)
            curveTo(5.964f, 6.798f, 5.928f, 6.8458f, 5.892f, 6.8933f)
            curveTo(4.5735f, 5.2931f, 3.0264f, 4f, 0.5f, 4f)
            curveTo(0.2239f, 4f, 0f, 3.7761f, 0f, 3.5f)
            curveTo(0f, 3.2239f, 0.2239f, 3f, 0.5f, 3f)
            close()
            moveTo(8.87248f, 10.6107f)
            curveTo(8.3728f, 10.131f, 7.909f, 9.5531f, 7.4577f, 8.9547f)
            curveTo(7.6469f, 8.7169f, 7.827f, 8.4806f, 8f, 8.25f)
            lineTo(8.08987f, 8.12987f)
            curveTo(8.5841f, 8.794f, 9.0529f, 9.3977f, 9.565f, 9.8893f)
            curveTo(10.2889f, 10.5842f, 11.0463f, 11f, 12f, 11f)
            horizontalLineTo(12.7929f)
            lineTo(11.6464f, 9.85355f)
            curveTo(11.4512f, 9.6583f, 11.4512f, 9.3417f, 11.6464f, 9.1464f)
            curveTo(11.8417f, 8.9512f, 12.1583f, 8.9512f, 12.3536f, 9.1464f)
            lineTo(14.3536f, 11.1464f)
            curveTo(14.5488f, 11.3417f, 14.5488f, 11.6583f, 14.3536f, 11.8536f)
            lineTo(12.3536f, 13.8536f)
            curveTo(12.1583f, 14.0488f, 11.8417f, 14.0488f, 11.6464f, 13.8536f)
            curveTo(11.4512f, 13.6583f, 11.4512f, 13.3417f, 11.6464f, 13.1464f)
            lineTo(12.7929f, 12f)
            horizontalLineTo(12f)
            curveTo(10.7037f, 12f, 9.7111f, 11.4158f, 8.8725f, 10.6107f)
            close()
        }.build()
    }

    val Play: ImageVector by lazy {
        ImageVector.Builder(
            name = "Play",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 1024f,
            viewportHeight = 1024f
        ).path(
            fill = SolidColor(Color(0xFF000000)),
            stroke = null,
            strokeLineWidth = 0f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Miter,
            strokeLineMiter = 4f,
            pathFillType = PathFillType.NonZero
        ) {
            moveTo(891.2f, 512.0f)
            lineToRelative(-750.0f, 450.0f)
            verticalLineToRelative(-900.0f)
            lineToRelative(750.0f, 450.0f)
            close()
        }.build()
    }

    val Pause: ImageVector by lazy {
        ImageVector.Builder(
            name = "Pause",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 1024f,
            viewportHeight = 1024f
        ).path(
            fill = SolidColor(Color(0xFF000000)),
            stroke = null,
            strokeLineWidth = 0f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Miter,
            strokeLineMiter = 4f,
            pathFillType = PathFillType.NonZero
        ) {
            moveTo(209.4f, 62.0f)
            horizontalLineToRelative(150.0f)
            verticalLineToRelative(900.0f)
            horizontalLineToRelative(-150.0f)
            verticalLineToRelative(-900.0f)
            close()
        }.path(
            fill = SolidColor(Color(0xFF000000)),
            stroke = null,
            strokeLineWidth = 0f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Miter,
            strokeLineMiter = 4f,
            pathFillType = PathFillType.NonZero
        ) {
            moveTo(659.4f, 62.0f)
            horizontalLineToRelative(150.0f)
            verticalLineToRelative(900.0f)
            horizontalLineToRelative(-150.0f)
            verticalLineToRelative(-900.0f)
            close()
        }.build()
    }

    val GotoPrevious: ImageVector by lazy {
        ImageVector. Builder(
            name = "GotoPrevious",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 1024f,
            viewportHeight = 1024f
        ).path(
            fill = SolidColor(Color(0xFF000000)),
            stroke = null,
            strokeLineWidth = 0f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Miter,
            strokeLineMiter = 4f,
            pathFillType = PathFillType.NonZero
        ) {
            moveTo(98.3f, 483.1f)
            lineTo(654.0f, 162.2f)
            curveToRelative(22.4f, -12.9f, 50.5f, 3.2f, 50.5f, 29.1f)
            verticalLineToRelative(641.7f)
            curveToRelative(0.0f, 25.9f, -28.0f, 42.1f, -50.5f, 29.1f)
            lineTo(98.3f, 541.3f)
            curveToRelative(-22.4f, -12.9f, -22.4f, -45.3f, 0.0f, -58.3f)
            close()
        }.path(
            fill = SolidColor(Color(0xFF000000)),
            stroke = null,
            strokeLineWidth = 0f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Miter,
            strokeLineMiter = 4f,
            pathFillType = PathFillType.NonZero
        ) {
            moveTo(653.4f, 505.9f)
            lineTo(929.9f, 156.0f)
            curveToRelative(6.0f, -7.6f, 18.1f, -3.3f, 18.1f, 6.3f)
            verticalLineToRelative(699.8f)
            curveToRelative(0.0f, 9.6f, -12.1f, 13.9f, -18.1f, 6.3f)
            lineTo(653.4f, 518.5f)
            arcToRelative(10.2f, 10.2f, 0.0f, isMoreThanHalf = false, isPositiveArc = true, 0.0f, -12.6f)
            close()
        }.build()
    }

    val GotoNext: ImageVector by lazy {
        ImageVector.Builder(
            name = "GotoNext",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 1024f,
            viewportHeight = 1024f
        ).path(
            fill = SolidColor(Color(0xFF000000)),
            stroke = null,
            strokeLineWidth = 0f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Miter,
            strokeLineMiter = 4f,
            pathFillType = PathFillType.NonZero
        ) {
            moveTo(925.6f, 483.1f)
            lineTo(369.9f, 162.2f)
            curveToRelative(-22.4f, -12.9f, -50.5f, 3.2f, -50.5f, 29.1f)
            verticalLineToRelative(641.7f)
            curveToRelative(0.0f, 25.9f, 28.0f, 42.1f, 50.5f, 29.1f)
            lineToRelative(555.7f, -320.9f)
            curveToRelative(22.4f, -12.9f, 22.4f, -45.3f, -0.0f, -58.3f)
            close()
        }.path(
            fill = SolidColor(Color(0xFF000000)),
            stroke = null,
            strokeLineWidth = 0f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Miter,
            strokeLineMiter = 4f,
            pathFillType = PathFillType.NonZero
        ) {
            moveTo(370.5f, 505.9f)
            lineTo(94.0f, 156.0f)
            curveToRelative(-6.0f, -7.6f, -18.1f, -3.3f, -18.1f, 6.3f)
            verticalLineToRelative(699.8f)
            curveToRelative(0.0f, 9.6f, 12.1f, 13.9f, 18.1f, 6.3f)
            lineTo(370.5f, 518.5f)
            curveToRelative(2.9f, -3.7f, 2.9f, -8.9f, 0.0f, -12.6f)
            close()
        }.build()
    }

    val Playlist: ImageVector by lazy {
        ImageVector.Builder(
            name = "Playlist",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 1024f,
            viewportHeight = 1024f
        ).path(
            fill = SolidColor(Color(0xFF000000)),
            stroke = null,
            strokeLineWidth = 0f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Miter,
            strokeLineMiter = 4f,
            pathFillType = PathFillType.NonZero
        ) {
            moveTo(938.8f, 555.5f)
            curveToRelative(-12.8f, -27.7f, -30.4f, -52.7f, -51.7f, -73.9f)
            curveToRelative(-2.4f, -2.2f, -4.8f, -4.4f, -7.1f, -6.8f)
            curveToRelative(-15.5f, -16.0f, -26.8f, -35.9f, -32.1f, -58.1f)
            curveToRelative(-0.9f, -3.3f, -1.5f, -6.6f, -2.0f, -10.0f)
            curveToRelative(-1.6f, -9.6f, -2.1f, -19.7f, -1.3f, -29.8f)
            curveToRelative(1.7f, -22.4f, 9.2f, -43.0f, 20.8f, -60.4f)
            curveToRelative(-47.7f, 6.3f, -87.8f, 39.3f, -103.2f, 84.6f)
            curveToRelative(-1.1f, 3.4f, -2.2f, 6.9f, -3.0f, 10.5f)
            lineToRelative(-22.2f, 290.0f)
            curveToRelative(-6.4f, -2.6f, -12.9f, -4.9f, -19.8f, -6.7f)
            curveToRelative(-11.7f, -3.3f, -24.1f, -5.7f, -37.0f, -7.1f)
            curveToRelative(-14.9f, -1.6f, -30.5f, -1.7f, -46.6f, -0.1f)
            curveToRelative(-34.1f, 3.2f, -65.2f, 13.3f, -90.8f, 27.9f)
            curveToRelative(-28.9f, 16.5f, -50.7f, 39.0f, -61.9f, 64.4f)
            curveToRelative(-6.5f, 14.8f, -9.5f, 30.5f, -7.9f, 46.5f)
            curveToRelative(6.3f, 67.7f, 88.6f, 115.3f, 183.6f, 106.4f)
            curveToRelative(23.3f, -2.2f, 45.3f, -7.6f, 65.0f, -15.5f)
            curveToRelative(26.6f, -10.7f, 49.1f, -25.9f, 65.7f, -43.9f)
            curveToRelative(14.6f, -15.8f, 24.5f, -33.9f, 28.3f, -52.9f)
            curveToRelative(0.7f, -3.6f, 1.2f, -7.4f, 1.6f, -11.1f)
            curveToRelative(0.2f, 0.0f, 22.0f, -282.3f, 22.0f, -282.3f)
            curveToRelative(2.3f, 0.1f, 4.8f, 0.1f, 7.1f, 0.3f)
            curveToRelative(30.1f, 2.3f, 57.0f, 14.9f, 77.4f, 34.3f)
            curveToRelative(16.4f, 15.5f, 28.5f, 35.3f, 34.8f, 57.5f)
            curveToRelative(-3.6f, -22.3f, -10.3f, -43.7f, -19.7f, -63.8f)
            close()
            moveTo(839.3f, 527.2f)
            close()
            moveTo(105.0f, 169.0f)
            horizontalLineToRelative(798.2f)
            curveToRelative(22.1f, 0.0f, 40.0f, -17.9f, 40.0f, -40.0f)
            reflectiveCurveToRelative(-17.9f, -40.0f, -40.0f, -40.0f)
            lineTo(105.0f, 89.0f)
            curveToRelative(-22.1f, 0.0f, -40.0f, 17.9f, -40.0f, 40.0f)
            reflectiveCurveToRelative(17.9f, 40.0f, 40.0f, 40.0f)
            close()
        }.path(
            fill = SolidColor(Color(0xFF000000)),
            stroke = null,
            strokeLineWidth = 0f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Miter,
            strokeLineMiter = 4f,
            pathFillType = PathFillType.NonZero
        ) {
            moveTo(105.0f, 551.8f)
            horizontalLineToRelative(568.2f)
            curveToRelative(22.1f, 0.0f, 40.0f, -17.9f, 40.0f, -40.0f)
            reflectiveCurveToRelative(-17.9f, -40.0f, -40.0f, -40.0f)
            horizontalLineTo(105.0f)
            curveToRelative(-22.1f, 0.0f, -40.0f, 17.9f, -40.0f, 40.0f)
            reflectiveCurveToRelative(17.9f, 40.0f, 40.0f, 40.0f)
            close()
            moveTo(417.2f, 854.6f)
            horizontalLineTo(105.0f)
            curveToRelative(-22.1f, 0.0f, -40.0f, 17.9f, -40.0f, 40.0f)
            reflectiveCurveToRelative(17.9f, 40.0f, 40.0f, 40.0f)
            horizontalLineToRelative(312.2f)
            curveToRelative(22.1f, 0.0f, 40.0f, -17.9f, 40.0f, -40.0f)
            reflectiveCurveToRelative(-17.9f, -40.0f, -40.0f, -40.0f)
            close()
        }.build()
    }

    val ShowLyrics: ImageVector by lazy {
        ImageVector.Builder(
            name = "ShowLyrics",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 1024f,
            viewportHeight = 1024f
        ).path(
            fill = SolidColor(Color(0xFF000000)),
            stroke = null,
            strokeLineWidth = 0f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Miter,
            strokeLineMiter = 4f,
            pathFillType = PathFillType.NonZero
        ) {
            moveTo(826.3f, 963.2f)
            horizontalLineTo(196.3f)
            curveToRelative(-74.4f, 0.0f, -135.0f, -60.6f, -135.0f, -135.0f)
            verticalLineTo(198.2f)
            curveToRelative(0.0f, -74.4f, 60.6f, -135.0f, 135.0f, -135.0f)
            horizontalLineToRelative(630.0f)
            curveToRelative(74.4f, 0.0f, 135.0f, 60.6f, 135.0f, 135.0f)
            verticalLineToRelative(630.0f)
            curveToRelative(0.0f, 74.4f, -60.6f, 135.0f, -135.0f, 135.0f)
            close()
            moveTo(196.3f, 133.2f)
            curveToRelative(-35.8f, 0.0f, -65.0f, 29.2f, -65.0f, 65.0f)
            verticalLineToRelative(630.0f)
            curveToRelative(0.0f, 35.8f, 29.2f, 65.0f, 65.0f, 65.0f)
            horizontalLineToRelative(630.0f)
            curveToRelative(35.8f, 0.0f, 65.0f, -29.2f, 65.0f, -65.0f)
            verticalLineTo(198.2f)
            curveToRelative(0.0f, -35.8f, -29.2f, -65.0f, -65.0f, -65.0f)
            horizontalLineTo(196.3f)
            close()
        }.path(
            fill = SolidColor(Color(0xFF000000)),
            stroke = null,
            strokeLineWidth = 0f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Miter,
            strokeLineMiter = 4f,
            pathFillType = PathFillType.NonZero
        ) {
            moveTo(243.7f, 452.4f)
            curveToRelative(-20.6f, 0.0f, -30.9f, -12.0f, -30.9f, -36.1f)
            curveToRelative(0.0f, -24.0f, 10.3f, -36.9f, 30.9f, -38.6f)
            horizontalLineToRelative(61.8f)
            curveToRelative(29.2f, 0.0f, 43.8f, 18.0f, 43.8f, 54.1f)
            verticalLineToRelative(239.6f)
            curveToRelative(-1.7f, 6.9f, -1.7f, 10.3f, 0.0f, 10.3f)
            curveToRelative(1.7f, 1.7f, 5.2f, -0.8f, 10.3f, -7.7f)
            curveToRelative(1.7f, -1.7f, 4.3f, -6.0f, 7.7f, -12.9f)
            curveToRelative(1.7f, -3.4f, 4.3f, -7.7f, 7.7f, -12.9f)
            curveToRelative(10.3f, -15.5f, 24.9f, -17.1f, 43.8f, -5.2f)
            curveToRelative(15.5f, 13.8f, 19.7f, 29.2f, 12.9f, 46.4f)
            curveToRelative(-8.6f, 18.9f, -25.8f, 43.0f, -51.5f, 72.1f)
            curveToRelative(-27.5f, 30.9f, -52.4f, 44.6f, -74.7f, 41.2f)
            curveToRelative(-18.9f, -5.2f, -28.3f, -24.9f, -28.3f, -59.3f)
            verticalLineToRelative(-283.4f)
            curveToRelative(-1.7f, -3.4f, -4.3f, -6.0f, -7.7f, -7.7f)
            horizontalLineToRelative(-25.8f)
            close()
            moveTo(341.7f, 341.6f)
            curveToRelative(-25.8f, -18.9f, -51.5f, -42.1f, -77.3f, -69.6f)
            curveToRelative(-17.2f, -13.7f, -18.9f, -29.2f, -5.2f, -46.4f)
            curveToRelative(18.9f, -18.9f, 37.8f, -21.5f, 56.7f, -7.7f)
            curveToRelative(1.7f, 3.5f, 7.7f, 9.5f, 18.0f, 18.0f)
            curveToRelative(29.2f, 25.8f, 49.8f, 43.8f, 61.8f, 54.1f)
            curveToRelative(13.7f, 17.2f, 12.9f, 34.4f, -2.6f, 51.5f)
            curveToRelative(-17.2f, 13.8f, -34.4f, 13.8f, -51.5f, 0.0f)
            close()
            moveTo(434.4f, 310.7f)
            curveToRelative(-20.6f, 0.0f, -30.9f, -12.0f, -30.9f, -36.1f)
            curveToRelative(0.0f, -22.3f, 10.3f, -34.3f, 30.9f, -36.1f)
            horizontalLineToRelative(304.0f)
            curveToRelative(36.1f, 1.7f, 55.0f, 16.3f, 56.7f, 43.8f)
            verticalLineToRelative(461.2f)
            curveToRelative(0.0f, 48.1f, -34.4f, 70.4f, -103.1f, 67.0f)
            curveToRelative(-24.1f, 0.0f, -44.7f, -3.5f, -61.8f, -10.3f)
            curveToRelative(-24.1f, -8.6f, -34.4f, -24.9f, -30.9f, -49.0f)
            curveToRelative(6.8f, -18.9f, 22.3f, -26.6f, 46.4f, -23.2f)
            curveToRelative(5.2f, 3.5f, 15.5f, 6.0f, 30.9f, 7.7f)
            horizontalLineToRelative(20.6f)
            curveToRelative(17.1f, 3.5f, 25.8f, -2.6f, 25.8f, -18.0f)
            lineTo(723.0f, 318.4f)
            curveToRelative(-1.7f, -3.4f, -4.3f, -6.0f, -7.7f, -7.7f)
            lineTo(434.4f, 310.7f)
            close()
            moveTo(460.2f, 413.8f)
            curveToRelative(-18.9f, 0.0f, -29.2f, -11.2f, -30.9f, -33.5f)
            curveToRelative(1.7f, -22.3f, 12.0f, -34.3f, 30.9f, -36.1f)
            horizontalLineToRelative(211.3f)
            curveToRelative(18.9f, 1.7f, 29.2f, 13.8f, 30.9f, 36.1f)
            curveToRelative(0.0f, 22.3f, -9.5f, 33.5f, -28.3f, 33.5f)
            lineTo(460.2f, 413.8f)
            close()
            moveTo(493.7f, 702.3f)
            curveToRelative(-36.1f, 0.0f, -54.1f, -17.1f, -54.1f, -51.5f)
            lineTo(439.6f, 501.4f)
            curveToRelative(-1.7f, -34.3f, 13.7f, -51.5f, 46.4f, -51.5f)
            horizontalLineToRelative(152.0f)
            curveToRelative(34.3f, 1.7f, 51.5f, 16.3f, 51.5f, 43.8f)
            verticalLineToRelative(157.2f)
            curveToRelative(0.0f, 36.1f, -15.5f, 53.3f, -46.4f, 51.5f)
            lineTo(493.7f, 702.3f)
            close()
            moveTo(511.7f, 627.6f)
            curveToRelative(0.0f, 5.2f, 2.6f, 7.7f, 7.7f, 7.7f)
            horizontalLineToRelative(90.2f)
            curveToRelative(3.4f, 0.0f, 6.0f, -2.6f, 7.7f, -7.7f)
            lineTo(617.3f, 527.1f)
            curveToRelative(0.0f, -5.2f, -2.6f, -7.7f, -7.7f, -7.7f)
            horizontalLineToRelative(-90.2f)
            curveToRelative(-5.2f, 1.7f, -7.7f, 4.3f, -7.7f, 7.7f)
            verticalLineToRelative(100.5f)
            close()
        }.build()
    }
}