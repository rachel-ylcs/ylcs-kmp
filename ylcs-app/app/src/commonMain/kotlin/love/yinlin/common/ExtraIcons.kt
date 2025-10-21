package love.yinlin.common

import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import love.yinlin.compose.Colors

object ExtraIcons {
    val IOS: ImageVector by lazy {
        ImageVector.Builder(
            name = "IOS",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 960f,
            viewportHeight = 960f
        ).path(
            fill = SolidColor(Colors.Black),
            fillAlpha = 1.0f,
            stroke = null,
            strokeAlpha = 1.0f,
            strokeLineWidth = 1.0f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Miter,
            strokeLineMiter = 1.0f,
            pathFillType = PathFillType.NonZero
        ) {
            moveTo(160f, 360f)
            verticalLineToRelative(-80f)
            horizontalLineToRelative(80f)
            verticalLineToRelative(80f)
            close()
            moveToRelative(0f, 320f)
            verticalLineToRelative(-240f)
            horizontalLineToRelative(80f)
            verticalLineToRelative(240f)
            close()
            moveToRelative(280f, 0f)
            horizontalLineToRelative(-80f)
            quadToRelative(-33f, 0f, -56.5f, -23.5f)
            reflectiveQuadTo(280f, 600f)
            verticalLineToRelative(-240f)
            quadToRelative(0f, -33f, 23.5f, -56.5f)
            reflectiveQuadTo(360f, 280f)
            horizontalLineToRelative(80f)
            quadToRelative(33f, 0f, 56.5f, 23.5f)
            reflectiveQuadTo(520f, 360f)
            verticalLineToRelative(240f)
            quadToRelative(0f, 33f, -23.5f, 56.5f)
            reflectiveQuadTo(440f, 680f)
            moveToRelative(-80f, -80f)
            horizontalLineToRelative(80f)
            verticalLineToRelative(-240f)
            horizontalLineToRelative(-80f)
            close()
            moveToRelative(200f, 80f)
            verticalLineToRelative(-80f)
            horizontalLineToRelative(160f)
            verticalLineToRelative(-80f)
            horizontalLineToRelative(-80f)
            quadToRelative(-33f, 0f, -56.5f, -23.5f)
            reflectiveQuadTo(560f, 440f)
            verticalLineToRelative(-80f)
            quadToRelative(0f, -33f, 23.5f, -56.5f)
            reflectiveQuadTo(640f, 280f)
            horizontalLineToRelative(160f)
            verticalLineToRelative(80f)
            horizontalLineTo(640f)
            verticalLineToRelative(80f)
            horizontalLineToRelative(80f)
            quadToRelative(33f, 0f, 56.5f, 23.5f)
            reflectiveQuadTo(800f, 520f)
            verticalLineToRelative(80f)
            quadToRelative(0f, 33f, -23.5f, 56.5f)
            reflectiveQuadTo(720f, 680f)
            close()
        }.build()
    }

    val Windows: ImageVector by lazy {
        ImageVector.Builder(
            name = "Windows",
            defaultWidth = 16.dp,
            defaultHeight = 16.dp,
            viewportWidth = 16f,
            viewportHeight = 16f
        ).path(
            fill = SolidColor(Colors.Black),
            fillAlpha = 1.0f,
            stroke = null,
            strokeAlpha = 1.0f,
            strokeLineWidth = 1.0f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Miter,
            strokeLineMiter = 1.0f,
            pathFillType = PathFillType.NonZero
        ) {
            moveTo(6.555f, 1.375f)
            lineTo(0f, 2.237f)
            verticalLineToRelative(5.45f)
            horizontalLineToRelative(6.555f)
            close()
            moveTo(0f, 13.795f)
            lineToRelative(6.555f, 0.933f)
            verticalLineTo(8.313f)
            horizontalLineTo(0f)
            close()
            moveToRelative(7.278f, -5.4f)
            lineToRelative(0.026f, 6.378f)
            lineTo(16f, 16f)
            verticalLineTo(8.395f)
            close()
            moveTo(16f, 0f)
            lineTo(7.33f, 1.244f)
            verticalLineToRelative(6.414f)
            horizontalLineTo(16f)
            close()
        }.build()
    }

    val Linux: ImageVector by lazy {
        ImageVector.Builder(
            name = "Linux",
            defaultWidth = 16.dp,
            defaultHeight = 16.dp,
            viewportWidth = 16f,
            viewportHeight = 16f
        ).path(
            fill = SolidColor(Colors.Black),
            fillAlpha = 1.0f,
            stroke = null,
            strokeAlpha = 1.0f,
            strokeLineWidth = 1.0f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Miter,
            strokeLineMiter = 1.0f,
            pathFillType = PathFillType.NonZero
        ) {
            moveTo(13.281f, 11.156f)
            arcToRelative(0.84f, 0.84f, 0f, isMoreThanHalf = false, isPositiveArc = true, 0.375f, 0.297f)
            curveToRelative(0.084f, 0.125f, 0.143f, 0.276f, 0.18f, 0.453f)
            curveToRelative(0.02f, 0.104f, 0.044f, 0.2f, 0.07f, 0.29f)
            arcToRelative(1.772f, 1.772f, 0f, isMoreThanHalf = false, isPositiveArc = false, 0.219f, 0.476f)
            curveToRelative(0.047f, 0.073f, 0.11f, 0.153f, 0.188f, 0.242f)
            curveToRelative(0.067f, 0.073f, 0.127f, 0.167f, 0.18f, 0.281f)
            arcToRelative(0.793f, 0.793f, 0f, isMoreThanHalf = false, isPositiveArc = true, 0.077f, 0.328f)
            arcToRelative(0.49f, 0.49f, 0f, isMoreThanHalf = false, isPositiveArc = true, -0.093f, 0.305f)
            arcToRelative(0.944f, 0.944f, 0f, isMoreThanHalf = false, isPositiveArc = true, -0.235f, 0.219f)
            curveToRelative(-0.12f, 0.083f, -0.245f, 0.156f, -0.375f, 0.219f)
            curveToRelative(-0.13f, 0.062f, -0.26f, 0.127f, -0.39f, 0.195f)
            arcToRelative(3.624f, 3.624f, 0f, isMoreThanHalf = false, isPositiveArc = false, -0.555f, 0.328f)
            curveToRelative(-0.156f, 0.115f, -0.313f, 0.26f, -0.469f, 0.438f)
            arcToRelative(2.815f, 2.815f, 0f, isMoreThanHalf = false, isPositiveArc = true, -0.625f, 0.523f)
            arcToRelative(1.471f, 1.471f, 0f, isMoreThanHalf = false, isPositiveArc = true, -0.383f, 0.172f)
            curveToRelative(-0.13f, 0.036f, -0.26f, 0.06f, -0.39f, 0.07f)
            curveToRelative(-0.302f, 0f, -0.552f, -0.052f, -0.75f, -0.156f)
            curveToRelative(-0.198f, -0.104f, -0.37f, -0.294f, -0.516f, -0.57f)
            curveToRelative(-0.042f, -0.079f, -0.083f, -0.128f, -0.125f, -0.149f)
            arcToRelative(0.774f, 0.774f, 0f, isMoreThanHalf = false, isPositiveArc = false, -0.203f, -0.055f)
            lineTo(8.67f, 15f)
            curveToRelative(-0.26f, -0.02f, -0.525f, -0.031f, -0.796f, -0.031f)
            arcToRelative(4.28f, 4.28f, 0f, isMoreThanHalf = false, isPositiveArc = false, -0.672f, 0.054f)
            curveToRelative(-0.229f, 0.037f, -0.456f, 0.081f, -0.68f, 0.133f)
            curveToRelative(-0.046f, 0.01f, -0.093f, 0.05f, -0.14f, 0.117f)
            arcToRelative(1.7f, 1.7f, 0f, isMoreThanHalf = false, isPositiveArc = true, -0.196f, 0.227f)
            arcToRelative(1.106f, 1.106f, 0f, isMoreThanHalf = false, isPositiveArc = true, -0.335f, 0.219f)
            arcToRelative(1.475f, 1.475f, 0f, isMoreThanHalf = false, isPositiveArc = true, -0.555f, 0.101f)
            curveToRelative(-0.172f, 0f, -0.357f, -0.018f, -0.555f, -0.054f)
            arcToRelative(1.82f, 1.82f, 0f, isMoreThanHalf = false, isPositiveArc = true, -0.531f, -0.18f)
            arcToRelative(3.578f, 3.578f, 0f, isMoreThanHalf = false, isPositiveArc = false, -0.953f, -0.328f)
            curveToRelative(-0.313f, -0.057f, -0.643f, -0.11f, -0.992f, -0.156f)
            arcToRelative(3.392f, 3.392f, 0f, isMoreThanHalf = false, isPositiveArc = true, -0.344f, -0.063f)
            arcToRelative(0.774f, 0.774f, 0f, isMoreThanHalf = false, isPositiveArc = true, -0.29f, -0.133f)
            arcToRelative(0.705f, 0.705f, 0f, isMoreThanHalf = false, isPositiveArc = true, -0.194f, -0.219f)
            arcToRelative(0.78f, 0.78f, 0f, isMoreThanHalf = false, isPositiveArc = true, -0.079f, -0.351f)
            curveToRelative(0f, -0.162f, 0.021f, -0.318f, 0.063f, -0.469f)
            curveToRelative(0.042f, -0.15f, 0.065f, -0.31f, 0.07f, -0.476f)
            curveToRelative(0f, -0.115f, -0.008f, -0.227f, -0.023f, -0.336f)
            arcToRelative(3.53f, 3.53f, 0f, isMoreThanHalf = false, isPositiveArc = true, -0.032f, -0.352f)
            curveToRelative(0f, -0.265f, 0.063f, -0.46f, 0.188f, -0.586f)
            curveToRelative(0.125f, -0.125f, 0.307f, -0.224f, 0.547f, -0.297f)
            arcToRelative(0.99f, 0.99f, 0f, isMoreThanHalf = false, isPositiveArc = false, 0.297f, -0.148f)
            arcToRelative(2.27f, 2.27f, 0f, isMoreThanHalf = false, isPositiveArc = false, 0.234f, -0.203f)
            arcToRelative(1.86f, 1.86f, 0f, isMoreThanHalf = false, isPositiveArc = false, 0.203f, -0.242f)
            curveToRelative(0.063f, -0.089f, 0.133f, -0.178f, 0.211f, -0.266f)
            arcToRelative(0.114f, 0.114f, 0f, isMoreThanHalf = false, isPositiveArc = false, 0.024f, -0.07f)
            curveToRelative(0f, -0.063f, -0.003f, -0.123f, -0.008f, -0.18f)
            lineToRelative(-0.016f, -0.188f)
            curveToRelative(0f, -0.354f, 0.055f, -0.71f, 0.164f, -1.07f)
            curveToRelative(0.11f, -0.36f, 0.253f, -0.71f, 0.43f, -1.055f)
            arcToRelative(9.08f, 9.08f, 0f, isMoreThanHalf = false, isPositiveArc = true, 0.594f, -0.992f)
            curveToRelative(0.218f, -0.317f, 0.435f, -0.612f, 0.648f, -0.883f)
            arcToRelative(4.35f, 4.35f, 0f, isMoreThanHalf = false, isPositiveArc = false, 0.68f, -1.203f)
            curveToRelative(0.15f, -0.416f, 0.229f, -0.87f, 0.234f, -1.36f)
            curveToRelative(0f, -0.207f, -0.01f, -0.413f, -0.031f, -0.616f)
            arcToRelative(6.122f, 6.122f, 0f, isMoreThanHalf = false, isPositiveArc = true, -0.031f, -0.625f)
            curveToRelative(0f, -0.417f, 0.047f, -0.792f, 0.14f, -1.125f)
            curveToRelative(0.094f, -0.334f, 0.24f, -0.62f, 0.438f, -0.86f)
            reflectiveCurveToRelative(0.456f, -0.419f, 0.773f, -0.539f)
            curveTo(7.474f, 0.075f, 7.854f, 0.01f, 8.296f, 0f)
            curveToRelative(0.527f, 0f, 0.946f, 0.104f, 1.259f, 0.313f)
            curveToRelative(0.312f, 0.208f, 0.552f, 0.481f, 0.718f, 0.82f)
            curveToRelative(0.167f, 0.338f, 0.274f, 0.716f, 0.32f, 1.133f)
            curveToRelative(0.048f, 0.416f, 0.074f, 0.838f, 0.079f, 1.265f)
            verticalLineToRelative(0.133f)
            curveToRelative(0f, 0.214f, 0.002f, 0.404f, 0.008f, 0.57f)
            arcToRelative(2.527f, 2.527f, 0f, isMoreThanHalf = false, isPositiveArc = false, 0.226f, 0.977f)
            curveToRelative(0.073f, 0.161f, 0.182f, 0.336f, 0.328f, 0.523f)
            curveToRelative(0.25f, 0.329f, 0.506f, 0.66f, 0.766f, 0.993f)
            curveToRelative(0.26f, 0.333f, 0.497f, 0.677f, 0.71f, 1.03f)
            curveToRelative(0.214f, 0.355f, 0.389f, 0.725f, 0.524f, 1.11f)
            curveToRelative(0.136f, 0.386f, 0.206f, 0.802f, 0.211f, 1.25f)
            arcToRelative(3.3f, 3.3f, 0f, isMoreThanHalf = false, isPositiveArc = true, -0.164f, 1.04f)
            close()
            moveToRelative(-6.554f, -8.14f)
            curveToRelative(0.072f, 0f, 0.132f, 0.018f, 0.18f, 0.054f)
            arcToRelative(0.357f, 0.357f, 0f, isMoreThanHalf = false, isPositiveArc = true, 0.109f, 0.149f)
            arcToRelative(0.85f, 0.85f, 0f, isMoreThanHalf = false, isPositiveArc = true, 0.054f, 0.187f)
            curveToRelative(0.01f, 0.063f, 0.016f, 0.128f, 0.016f, 0.196f)
            arcToRelative(0.282f, 0.282f, 0f, isMoreThanHalf = false, isPositiveArc = true, -0.024f, 0.125f)
            arcToRelative(0.27f, 0.27f, 0f, isMoreThanHalf = false, isPositiveArc = true, -0.07f, 0.086f)
            lineToRelative(-0.094f, 0.078f)
            arcToRelative(0.796f, 0.796f, 0f, isMoreThanHalf = false, isPositiveArc = false, -0.093f, 0.093f)
            arcToRelative(0.428f, 0.428f, 0f, isMoreThanHalf = false, isPositiveArc = true, -0.149f, 0.141f)
            arcToRelative(2.129f, 2.129f, 0f, isMoreThanHalf = false, isPositiveArc = false, -0.18f, 0.117f)
            arcToRelative(1.31f, 1.31f, 0f, isMoreThanHalf = false, isPositiveArc = false, -0.156f, 0.133f)
            arcToRelative(0.264f, 0.264f, 0f, isMoreThanHalf = false, isPositiveArc = false, -0.07f, 0.195f)
            curveToRelative(0f, 0.047f, 0.023f, 0.086f, 0.07f, 0.117f)
            arcToRelative(0.704f, 0.704f, 0f, isMoreThanHalf = false, isPositiveArc = true, 0.266f, 0.305f)
            curveToRelative(0.052f, 0.12f, 0.11f, 0.237f, 0.172f, 0.352f)
            curveToRelative(0.062f, 0.114f, 0.143f, 0.21f, 0.242f, 0.289f)
            curveToRelative(0.099f, 0.078f, 0.253f, 0.117f, 0.46f, 0.117f)
            horizontalLineToRelative(0.048f)
            curveToRelative(0.208f, -0.01f, 0.406f, -0.065f, 0.594f, -0.164f)
            curveToRelative(0.187f, -0.099f, 0.375f, -0.203f, 0.562f, -0.313f)
            arcToRelative(0.633f, 0.633f, 0f, isMoreThanHalf = false, isPositiveArc = true, 0.102f, -0.046f)
            arcToRelative(0.37f, 0.37f, 0f, isMoreThanHalf = false, isPositiveArc = false, 0.101f, -0.055f)
            lineToRelative(0.57f, -0.445f)
            arcToRelative(0.926f, 0.926f, 0f, isMoreThanHalf = false, isPositiveArc = false, 0.024f, -0.102f)
            arcToRelative(2.75f, 2.75f, 0f, isMoreThanHalf = false, isPositiveArc = false, 0.016f, -0.11f)
            arcToRelative(0.236f, 0.236f, 0f, isMoreThanHalf = false, isPositiveArc = false, -0.04f, -0.14f)
            arcToRelative(0.4f, 0.4f, 0f, isMoreThanHalf = false, isPositiveArc = false, -0.093f, -0.094f)
            arcToRelative(0.34f, 0.34f, 0f, isMoreThanHalf = false, isPositiveArc = false, -0.133f, -0.054f)
            arcToRelative(0.909f, 0.909f, 0f, isMoreThanHalf = false, isPositiveArc = true, -0.14f, -0.04f)
            arcToRelative(1.083f, 1.083f, 0f, isMoreThanHalf = false, isPositiveArc = true, -0.352f, -0.14f)
            arcToRelative(1.457f, 1.457f, 0f, isMoreThanHalf = false, isPositiveArc = false, -0.344f, -0.156f)
            curveToRelative(-0.02f, -0.006f, -0.036f, -0.021f, -0.047f, -0.047f)
            arcToRelative(0.983f, 0.983f, 0f, isMoreThanHalf = false, isPositiveArc = true, -0.031f, -0.094f)
            arcToRelative(0.23f, 0.23f, 0f, isMoreThanHalf = false, isPositiveArc = true, -0.008f, -0.102f)
            arcToRelative(0.126f, 0.126f, 0f, isMoreThanHalf = false, isPositiveArc = false, -0.008f, -0.078f)
            curveToRelative(0f, -0.062f, 0.005f, -0.127f, 0.016f, -0.195f)
            arcToRelative(0.551f, 0.551f, 0f, isMoreThanHalf = false, isPositiveArc = true, 0.07f, -0.195f)
            arcToRelative(0.417f, 0.417f, 0f, isMoreThanHalf = false, isPositiveArc = true, 0.125f, -0.14f)
            arcToRelative(0.411f, 0.411f, 0f, isMoreThanHalf = false, isPositiveArc = true, 0.203f, -0.056f)
            curveToRelative(0.162f, 0f, 0.279f, 0.06f, 0.352f, 0.18f)
            curveToRelative(0.073f, 0.12f, 0.112f, 0.25f, 0.117f, 0.39f)
            arcToRelative(0.397f, 0.397f, 0f, isMoreThanHalf = false, isPositiveArc = true, -0.039f, 0.18f)
            arcToRelative(0.379f, 0.379f, 0f, isMoreThanHalf = false, isPositiveArc = false, -0.04f, 0.172f)
            curveToRelative(0f, 0.042f, 0.014f, 0.07f, 0.04f, 0.086f)
            arcToRelative(0.26f, 0.26f, 0f, isMoreThanHalf = false, isPositiveArc = false, 0.102f, 0.031f)
            curveToRelative(0.12f, 0f, 0.197f, -0.028f, 0.234f, -0.085f)
            arcToRelative(0.533f, 0.533f, 0f, isMoreThanHalf = false, isPositiveArc = false, 0.062f, -0.258f)
            curveToRelative(0f, -0.12f, -0.01f, -0.253f, -0.03f, -0.399f)
            arcToRelative(1.32f, 1.32f, 0f, isMoreThanHalf = false, isPositiveArc = false, -0.126f, -0.406f)
            arcToRelative(0.969f, 0.969f, 0f, isMoreThanHalf = false, isPositiveArc = false, -0.242f, -0.313f)
            arcToRelative(0.574f, 0.574f, 0f, isMoreThanHalf = false, isPositiveArc = false, -0.383f, -0.124f)
            curveToRelative(-0.27f, 0f, -0.466f, 0.067f, -0.586f, 0.203f)
            curveToRelative(-0.12f, 0.135f, -0.182f, 0.338f, -0.187f, 0.609f)
            curveToRelative(0f, 0.078f, 0.005f, 0.156f, 0.015f, 0.234f)
            curveToRelative(0.01f, 0.079f, 0.016f, 0.157f, 0.016f, 0.235f)
            curveToRelative(0f, 0.026f, -0.003f, 0.039f, -0.008f, 0.039f)
            arcToRelative(0.218f, 0.218f, 0f, isMoreThanHalf = false, isPositiveArc = true, -0.047f, -0.016f)
            arcToRelative(4.263f, 4.263f, 0f, isMoreThanHalf = false, isPositiveArc = true, -0.093f, -0.039f)
            arcToRelative(0.774f, 0.774f, 0f, isMoreThanHalf = false, isPositiveArc = false, -0.118f, -0.039f)
            arcToRelative(0.514f, 0.514f, 0f, isMoreThanHalf = false, isPositiveArc = false, -0.203f, -0.008f)
            arcToRelative(1.007f, 1.007f, 0f, isMoreThanHalf = false, isPositiveArc = true, -0.125f, 0.008f)
            curveToRelative(-0.073f, 0f, -0.11f, -0.013f, -0.11f, -0.039f)
            curveToRelative(0f, -0.078f, -0.004f, -0.177f, -0.015f, -0.297f)
            curveToRelative(-0.01f, -0.12f, -0.036f, -0.24f, -0.078f, -0.36f)
            arcToRelative(0.995f, 0.995f, 0f, isMoreThanHalf = false, isPositiveArc = false, -0.156f, -0.296f)
            curveToRelative(-0.063f, -0.078f, -0.156f, -0.12f, -0.281f, -0.125f)
            arcToRelative(0.323f, 0.323f, 0f, isMoreThanHalf = false, isPositiveArc = false, -0.227f, 0.086f)
            arcToRelative(0.905f, 0.905f, 0f, isMoreThanHalf = false, isPositiveArc = false, -0.164f, 0.203f)
            arcToRelative(0.64f, 0.64f, 0f, isMoreThanHalf = false, isPositiveArc = false, -0.086f, 0.266f)
            arcToRelative(5.4f, 5.4f, 0f, isMoreThanHalf = false, isPositiveArc = true, -0.031f, 0.25f)
            arcToRelative(1.459f, 1.459f, 0f, isMoreThanHalf = false, isPositiveArc = false, 0.07f, 0.406f)
            curveToRelative(0.026f, 0.083f, 0.055f, 0.156f, 0.086f, 0.219f)
            curveToRelative(0.031f, 0.062f, 0.068f, 0.093f, 0.11f, 0.093f)
            curveToRelative(0.025f, 0f, 0.06f, -0.018f, 0.101f, -0.054f)
            curveToRelative(0.042f, -0.037f, 0.063f, -0.07f, 0.063f, -0.102f)
            curveToRelative(0f, -0.016f, -0.008f, -0.026f, -0.024f, -0.031f)
            arcToRelative(0.147f, 0.147f, 0f, isMoreThanHalf = false, isPositiveArc = false, -0.047f, -0.008f)
            curveToRelative(-0.036f, 0f, -0.068f, -0.018f, -0.094f, -0.055f)
            arcToRelative(0.468f, 0.468f, 0f, isMoreThanHalf = false, isPositiveArc = true, -0.062f, -0.125f)
            arcToRelative(5.144f, 5.144f, 0f, isMoreThanHalf = false, isPositiveArc = true, -0.047f, -0.148f)
            arcToRelative(0.564f, 0.564f, 0f, isMoreThanHalf = false, isPositiveArc = true, 0.055f, -0.398f)
            curveToRelative(0.047f, -0.084f, 0.133f, -0.128f, 0.258f, -0.133f)
            close()
            moveTo(5.023f, 15.18f)
            curveToRelative(0.125f, 0f, 0.248f, -0.01f, 0.368f, -0.032f)
            arcToRelative(0.97f, 0.97f, 0f, isMoreThanHalf = false, isPositiveArc = false, 0.336f, -0.125f)
            arcToRelative(0.614f, 0.614f, 0f, isMoreThanHalf = false, isPositiveArc = false, 0.234f, -0.242f)
            arcToRelative(0.943f, 0.943f, 0f, isMoreThanHalf = false, isPositiveArc = false, 0.094f, -0.375f)
            arcToRelative(0.816f, 0.816f, 0f, isMoreThanHalf = false, isPositiveArc = false, -0.047f, -0.273f)
            arcToRelative(0.963f, 0.963f, 0f, isMoreThanHalf = false, isPositiveArc = false, -0.133f, -0.25f)
            arcToRelative(2.763f, 2.763f, 0f, isMoreThanHalf = false, isPositiveArc = false, -0.203f, -0.281f)
            arcToRelative(2.763f, 2.763f, 0f, isMoreThanHalf = false, isPositiveArc = true, -0.203f, -0.282f)
            arcToRelative(62.93f, 62.93f, 0f, isMoreThanHalf = false, isPositiveArc = true, -0.29f, -0.43f)
            curveToRelative(-0.093f, -0.14f, -0.187f, -0.288f, -0.28f, -0.445f)
            arcToRelative(8.124f, 8.124f, 0f, isMoreThanHalf = false, isPositiveArc = true, -0.235f, -0.406f)
            arcToRelative(2.646f, 2.646f, 0f, isMoreThanHalf = false, isPositiveArc = false, -0.266f, -0.398f)
            arcToRelative(1.203f, 1.203f, 0f, isMoreThanHalf = false, isPositiveArc = false, -0.218f, -0.211f)
            arcToRelative(0.469f, 0.469f, 0f, isMoreThanHalf = false, isPositiveArc = false, -0.29f, -0.094f)
            arcToRelative(0.436f, 0.436f, 0f, isMoreThanHalf = false, isPositiveArc = false, -0.296f, 0.11f)
            arcToRelative(2.26f, 2.26f, 0f, isMoreThanHalf = false, isPositiveArc = false, -0.258f, 0.265f)
            arcToRelative(3.241f, 3.241f, 0f, isMoreThanHalf = false, isPositiveArc = true, -0.297f, 0.305f)
            curveToRelative(-0.11f, 0.099f, -0.25f, 0.177f, -0.422f, 0.234f)
            arcToRelative(0.744f, 0.744f, 0f, isMoreThanHalf = false, isPositiveArc = false, -0.312f, 0.172f)
            curveToRelative(-0.073f, 0.073f, -0.11f, 0.185f, -0.11f, 0.336f)
            curveToRelative(0f, 0.104f, 0.008f, 0.208f, 0.024f, 0.312f)
            curveToRelative(0.015f, 0.104f, 0.026f, 0.209f, 0.031f, 0.313f)
            curveToRelative(0f, 0.14f, -0.02f, 0.273f, -0.063f, 0.398f)
            arcToRelative(1.157f, 1.157f, 0f, isMoreThanHalf = false, isPositiveArc = false, -0.062f, 0.367f)
            curveToRelative(0f, 0.141f, 0.05f, 0.24f, 0.148f, 0.297f)
            curveToRelative(0.1f, 0.058f, 0.211f, 0.097f, 0.336f, 0.117f)
            curveToRelative(0.157f, 0.027f, 0.305f, 0.047f, 0.446f, 0.063f)
            curveToRelative(0.14f, 0.016f, 0.278f, 0.04f, 0.414f, 0.07f)
            curveToRelative(0.135f, 0.032f, 0.27f, 0.065f, 0.406f, 0.102f)
            curveToRelative(0.135f, 0.036f, 0.279f, 0.094f, 0.43f, 0.172f)
            curveToRelative(0.03f, 0.015f, 0.078f, 0.034f, 0.14f, 0.054f)
            lineToRelative(0.211f, 0.07f)
            curveToRelative(0.078f, 0.027f, 0.151f, 0.048f, 0.219f, 0.063f)
            arcToRelative(0.741f, 0.741f, 0f, isMoreThanHalf = false, isPositiveArc = false, 0.148f, 0.024f)
            close()
            moveToRelative(2.86f, -0.938f)
            curveToRelative(0.146f, 0f, 0.302f, -0.015f, 0.469f, -0.047f)
            arcToRelative(3.54f, 3.54f, 0f, isMoreThanHalf = false, isPositiveArc = false, 0.976f, -0.336f)
            arcToRelative(2.59f, 2.59f, 0f, isMoreThanHalf = false, isPositiveArc = false, 0.406f, -0.257f)
            arcToRelative(0.222f, 0.222f, 0f, isMoreThanHalf = false, isPositiveArc = false, 0.032f, -0.047f)
            arcToRelative(0.305f, 0.305f, 0f, isMoreThanHalf = false, isPositiveArc = false, 0.023f, -0.063f)
            verticalLineToRelative(-0.008f)
            curveToRelative(0.031f, -0.114f, 0.057f, -0.24f, 0.078f, -0.375f)
            arcToRelative(8.63f, 8.63f, 0f, isMoreThanHalf = false, isPositiveArc = false, 0.055f, -0.414f)
            arcToRelative(8.98f, 8.98f, 0f, isMoreThanHalf = false, isPositiveArc = true, 0.055f, -0.414f)
            curveToRelative(0.02f, -0.135f, 0.039f, -0.268f, 0.054f, -0.398f)
            curveToRelative(0.021f, -0.14f, 0.047f, -0.276f, 0.078f, -0.406f)
            curveToRelative(0.032f, -0.13f, 0.073f, -0.253f, 0.125f, -0.368f)
            arcToRelative(1.03f, 1.03f, 0f, isMoreThanHalf = false, isPositiveArc = true, 0.211f, -0.304f)
            arcToRelative(1.54f, 1.54f, 0f, isMoreThanHalf = false, isPositiveArc = true, 0.344f, -0.25f)
            verticalLineToRelative(-0.016f)
            lineToRelative(-0.008f, -0.023f)
            arcToRelative(0.29f, 0.29f, 0f, isMoreThanHalf = false, isPositiveArc = true, 0.047f, -0.149f)
            arcToRelative(1.4f, 1.4f, 0f, isMoreThanHalf = false, isPositiveArc = true, 0.117f, -0.164f)
            arcToRelative(0.582f, 0.582f, 0f, isMoreThanHalf = false, isPositiveArc = true, 0.149f, -0.133f)
            arcToRelative(0.946f, 0.946f, 0f, isMoreThanHalf = false, isPositiveArc = true, 0.164f, -0.078f)
            arcToRelative(9.837f, 9.837f, 0f, isMoreThanHalf = false, isPositiveArc = false, -0.102f, -0.375f)
            arcToRelative(4.938f, 4.938f, 0f, isMoreThanHalf = false, isPositiveArc = true, -0.094f, -0.375f)
            arcToRelative(7.126f, 7.126f, 0f, isMoreThanHalf = false, isPositiveArc = false, -0.093f, -0.476f)
            arcToRelative(2.954f, 2.954f, 0f, isMoreThanHalf = false, isPositiveArc = false, -0.11f, -0.36f)
            arcToRelative(1.317f, 1.317f, 0f, isMoreThanHalf = false, isPositiveArc = false, -0.18f, -0.32f)
            curveToRelative(-0.077f, -0.104f, -0.174f, -0.23f, -0.288f, -0.375f)
            arcToRelative(1.189f, 1.189f, 0f, isMoreThanHalf = false, isPositiveArc = true, -0.118f, -0.156f)
            arcToRelative(0.555f, 0.555f, 0f, isMoreThanHalf = false, isPositiveArc = true, -0.046f, -0.196f)
            arcToRelative(2.206f, 2.206f, 0f, isMoreThanHalf = false, isPositiveArc = false, -0.047f, -0.203f)
            arcToRelative(9.48f, 9.48f, 0f, isMoreThanHalf = false, isPositiveArc = false, -0.242f, -0.75f)
            arcToRelative(2.91f, 2.91f, 0f, isMoreThanHalf = false, isPositiveArc = false, -0.172f, -0.383f)
            arcToRelative(3.87f, 3.87f, 0f, isMoreThanHalf = false, isPositiveArc = false, -0.172f, -0.289f)
            curveToRelative(-0.052f, -0.078f, -0.107f, -0.117f, -0.164f, -0.117f)
            curveToRelative(-0.125f, 0f, -0.274f, 0.05f, -0.446f, 0.149f)
            curveToRelative(-0.171f, 0.099f, -0.354f, 0.208f, -0.546f, 0.328f)
            curveToRelative(-0.193f, 0.12f, -0.38f, 0.232f, -0.563f, 0.336f)
            curveToRelative(-0.182f, 0.104f, -0.346f, 0.153f, -0.492f, 0.148f)
            arcToRelative(0.7f, 0.7f, 0f, isMoreThanHalf = false, isPositiveArc = true, -0.43f, -0.148f)
            arcToRelative(2.236f, 2.236f, 0f, isMoreThanHalf = false, isPositiveArc = true, -0.36f, -0.344f)
            curveToRelative(-0.109f, -0.13f, -0.2f, -0.242f, -0.273f, -0.336f)
            curveToRelative(-0.073f, -0.094f, -0.127f, -0.146f, -0.164f, -0.156f)
            curveToRelative(-0.041f, 0f, -0.065f, 0.031f, -0.07f, 0.093f)
            arcToRelative(2.56f, 2.56f, 0f, isMoreThanHalf = false, isPositiveArc = false, -0.008f, 0.211f)
            verticalLineToRelative(0.133f)
            curveToRelative(0f, 0.032f, -0.005f, 0.052f, -0.016f, 0.063f)
            curveToRelative(-0.057f, 0.12f, -0.12f, 0.237f, -0.187f, 0.351f)
            curveToRelative(-0.068f, 0.115f, -0.135f, 0.232f, -0.203f, 0.352f)
            arcToRelative(1.611f, 1.611f, 0f, isMoreThanHalf = false, isPositiveArc = false, -0.219f, 0.758f)
            curveToRelative(0f, 0.078f, 0.005f, 0.156f, 0.016f, 0.234f)
            curveToRelative(0.01f, 0.078f, 0.036f, 0.154f, 0.078f, 0.227f)
            lineToRelative(-0.016f, 0.03f)
            arcToRelative(1.31f, 1.31f, 0f, isMoreThanHalf = false, isPositiveArc = true, -0.133f, 0.157f)
            arcToRelative(1.072f, 1.072f, 0f, isMoreThanHalf = false, isPositiveArc = false, -0.132f, 0.164f)
            arcToRelative(2.796f, 2.796f, 0f, isMoreThanHalf = false, isPositiveArc = false, -0.407f, 0.93f)
            curveToRelative(-0.078f, 0.333f, -0.12f, 0.672f, -0.125f, 1.015f)
            curveToRelative(0f, 0.089f, 0.006f, 0.178f, 0.016f, 0.266f)
            curveToRelative(0.01f, 0.089f, 0.016f, 0.177f, 0.016f, 0.266f)
            arcToRelative(0.526f, 0.526f, 0f, isMoreThanHalf = false, isPositiveArc = true, -0.008f, 0.086f)
            arcToRelative(0.525f, 0.525f, 0f, isMoreThanHalf = false, isPositiveArc = false, -0.008f, 0.086f)
            arcToRelative(0.75f, 0.75f, 0f, isMoreThanHalf = false, isPositiveArc = true, 0.313f, 0.109f)
            curveToRelative(0.12f, 0.068f, 0.25f, 0.154f, 0.39f, 0.258f)
            curveToRelative(0.14f, 0.104f, 0.274f, 0.224f, 0.399f, 0.36f)
            curveToRelative(0.125f, 0.135f, 0.244f, 0.267f, 0.359f, 0.398f)
            curveToRelative(0.115f, 0.13f, 0.198f, 0.26f, 0.25f, 0.39f)
            curveToRelative(0.052f, 0.13f, 0.086f, 0.237f, 0.101f, 0.32f)
            arcToRelative(0.444f, 0.444f, 0f, isMoreThanHalf = false, isPositiveArc = true, -0.125f, 0.329f)
            arcToRelative(0.955f, 0.955f, 0f, isMoreThanHalf = false, isPositiveArc = true, -0.312f, 0.203f)
            curveToRelative(0.089f, 0.156f, 0.198f, 0.289f, 0.328f, 0.398f)
            curveToRelative(0.13f, 0.11f, 0.271f, 0.198f, 0.422f, 0.266f)
            curveToRelative(0.151f, 0.068f, 0.315f, 0.117f, 0.492f, 0.148f)
            curveToRelative(0.177f, 0.032f, 0.35f, 0.047f, 0.516f, 0.047f)
            close()
            moveToRelative(3.133f, 1.11f)
            curveToRelative(0.109f, 0f, 0.216f, -0.016f, 0.32f, -0.047f)
            arcToRelative(1.65f, 1.65f, 0f, isMoreThanHalf = false, isPositiveArc = false, 0.445f, -0.203f)
            curveToRelative(0.136f, -0.089f, 0.26f, -0.198f, 0.375f, -0.329f)
            arcToRelative(3.07f, 3.07f, 0f, isMoreThanHalf = false, isPositiveArc = true, 0.977f, -0.75f)
            lineToRelative(0.258f, -0.117f)
            arcToRelative(2.18f, 2.18f, 0f, isMoreThanHalf = false, isPositiveArc = false, 0.257f, -0.133f)
            arcToRelative(0.962f, 0.962f, 0f, isMoreThanHalf = false, isPositiveArc = false, 0.165f, -0.132f)
            arcToRelative(0.256f, 0.256f, 0f, isMoreThanHalf = false, isPositiveArc = false, 0.078f, -0.188f)
            arcToRelative(0.295f, 0.295f, 0f, isMoreThanHalf = false, isPositiveArc = false, -0.024f, -0.117f)
            arcToRelative(0.58f, 0.58f, 0f, isMoreThanHalf = false, isPositiveArc = false, -0.07f, -0.117f)
            arcToRelative(5.136f, 5.136f, 0f, isMoreThanHalf = false, isPositiveArc = true, -0.203f, -0.305f)
            arcToRelative(1.978f, 1.978f, 0f, isMoreThanHalf = false, isPositiveArc = true, -0.149f, -0.297f)
            lineToRelative(-0.125f, -0.312f)
            arcToRelative(2.558f, 2.558f, 0f, isMoreThanHalf = false, isPositiveArc = true, -0.11f, -0.352f)
            arcToRelative(0.28f, 0.28f, 0f, isMoreThanHalf = false, isPositiveArc = false, -0.054f, -0.101f)
            arcToRelative(0.53f, 0.53f, 0f, isMoreThanHalf = false, isPositiveArc = false, -0.46f, -0.235f)
            arcToRelative(0.533f, 0.533f, 0f, isMoreThanHalf = false, isPositiveArc = false, -0.266f, 0.07f)
            lineToRelative(-0.266f, 0.149f)
            arcToRelative(7.335f, 7.335f, 0f, isMoreThanHalf = false, isPositiveArc = true, -0.281f, 0.148f)
            arcToRelative(0.656f, 0.656f, 0f, isMoreThanHalf = false, isPositiveArc = true, -0.297f, 0.07f)
            arcToRelative(0.411f, 0.411f, 0f, isMoreThanHalf = false, isPositiveArc = true, -0.258f, -0.077f)
            arcToRelative(0.636f, 0.636f, 0f, isMoreThanHalf = false, isPositiveArc = true, -0.172f, -0.211f)
            arcToRelative(2.218f, 2.218f, 0f, isMoreThanHalf = false, isPositiveArc = true, -0.117f, -0.258f)
            lineToRelative(-0.094f, -0.258f)
            arcToRelative(1.26f, 1.26f, 0f, isMoreThanHalf = false, isPositiveArc = true, -0.14f, 0.188f)
            arcToRelative(0.666f, 0.666f, 0f, isMoreThanHalf = false, isPositiveArc = false, -0.125f, 0.203f)
            curveToRelative(-0.068f, 0.156f, -0.11f, 0.33f, -0.125f, 0.523f)
            curveToRelative(-0.026f, 0.302f, -0.06f, 0.596f, -0.102f, 0.883f)
            arcToRelative(4.7f, 4.7f, 0f, isMoreThanHalf = false, isPositiveArc = true, -0.21f, 0.86f)
            arcToRelative(1.914f, 1.914f, 0f, isMoreThanHalf = false, isPositiveArc = false, -0.063f, 0.273f)
            arcToRelative(2.88f, 2.88f, 0f, isMoreThanHalf = false, isPositiveArc = false, -0.032f, 0.289f)
            curveToRelative(0f, 0.255f, 0.079f, 0.466f, 0.235f, 0.633f)
            curveToRelative(0.156f, 0.166f, 0.367f, 0.25f, 0.633f, 0.25f)
            close()
        }.build()
    }

    val MacOS: ImageVector by lazy {
        ImageVector.Builder(
            name = "MacOS",
            defaultWidth = 16.dp,
            defaultHeight = 16.dp,
            viewportWidth = 16f,
            viewportHeight = 16f
        ).path(
            fill = SolidColor(Colors.Black),
            fillAlpha = 1.0f,
            stroke = null,
            strokeAlpha = 1.0f,
            strokeLineWidth = 1.0f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Miter,
            strokeLineMiter = 1.0f,
            pathFillType = PathFillType.NonZero
        ) {
            moveTo(11.182f, 0.008f)
            curveTo(11.148f, -0.03f, 9.923f, 0.023f, 8.857f, 1.18f)
            curveToRelative(-1.066f, 1.156f, -0.902f, 2.482f, -0.878f, 2.516f)
            reflectiveCurveToRelative(1.52f, 0.087f, 2.475f, -1.258f)
            reflectiveCurveToRelative(0.762f, -2.391f, 0.728f, -2.43f)
            moveToRelative(3.314f, 11.733f)
            curveToRelative(-0.048f, -0.096f, -2.325f, -1.234f, -2.113f, -3.422f)
            reflectiveCurveToRelative(1.675f, -2.789f, 1.698f, -2.854f)
            reflectiveCurveToRelative(-0.597f, -0.79f, -1.254f, -1.157f)
            arcToRelative(3.7f, 3.7f, 0f, isMoreThanHalf = false, isPositiveArc = false, -1.563f, -0.434f)
            curveToRelative(-0.108f, -0.003f, -0.483f, -0.095f, -1.254f, 0.116f)
            curveToRelative(-0.508f, 0.139f, -1.653f, 0.589f, -1.968f, 0.607f)
            curveToRelative(-0.316f, 0.018f, -1.256f, -0.522f, -2.267f, -0.665f)
            curveToRelative(-0.647f, -0.125f, -1.333f, 0.131f, -1.824f, 0.328f)
            curveToRelative(-0.49f, 0.196f, -1.422f, 0.754f, -2.074f, 2.237f)
            curveToRelative(-0.652f, 1.482f, -0.311f, 3.83f, -0.067f, 4.56f)
            reflectiveCurveToRelative(0.625f, 1.924f, 1.273f, 2.796f)
            curveToRelative(0.576f, 0.984f, 1.34f, 1.667f, 1.659f, 1.899f)
            reflectiveCurveToRelative(1.219f, 0.386f, 1.843f, 0.067f)
            curveToRelative(0.502f, -0.308f, 1.408f, -0.485f, 1.766f, -0.472f)
            curveToRelative(0.357f, 0.013f, 1.061f, 0.154f, 1.782f, 0.539f)
            curveToRelative(0.571f, 0.197f, 1.111f, 0.115f, 1.652f, -0.105f)
            curveToRelative(0.541f, -0.221f, 1.324f, -1.059f, 2.238f, -2.758f)
            quadToRelative(0.52f, -1.185f, 0.473f, -1.282f)
        }.build()
    }

    val WasmJs: ImageVector by lazy {
        ImageVector.Builder(
            name = "WasmJs",
            defaultWidth = 16.dp,
            defaultHeight = 16.dp,
            viewportWidth = 16f,
            viewportHeight = 16f
        ).path(
            fill = SolidColor(Colors.Black),
            fillAlpha = 1.0f,
            stroke = null,
            strokeAlpha = 1.0f,
            strokeLineWidth = 1.0f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Miter,
            strokeLineMiter = 1.0f,
            pathFillType = PathFillType.NonZero
        ) {
            moveTo(9.482f, 9.341f)
            curveToRelative(-0.069f, 0.062f, -0.17f, 0.153f, -0.17f, 0.309f)
            curveToRelative(0f, 0.162f, 0.107f, 0.325f, 0.3f, 0.456f)
            curveToRelative(0.877f, 0.613f, 2.521f, 0.54f, 2.592f, 0.538f)
            horizontalLineToRelative(0.002f)
            curveToRelative(0.667f, 0f, 1.32f, -0.18f, 1.894f, -0.519f)
            arcTo(3.84f, 3.84f, 0f, isMoreThanHalf = false, isPositiveArc = false, 16f, 6.819f)
            curveToRelative(0.018f, -1.316f, -0.44f, -2.218f, -0.666f, -2.664f)
            lineToRelative(-0.04f, -0.08f)
            curveTo(13.963f, 1.487f, 11.106f, 0f, 8f, 0f)
            arcTo(8f, 8f, 0f, isMoreThanHalf = false, isPositiveArc = false, 0.473f, 5.29f)
            curveTo(1.488f, 4.048f, 3.183f, 3.262f, 5f, 3.262f)
            curveToRelative(2.83f, 0f, 5.01f, 1.885f, 5.01f, 4.797f)
            horizontalLineToRelative(-0.004f)
            verticalLineToRelative(0.002f)
            curveToRelative(0f, 0.338f, -0.168f, 0.832f, -0.487f, 1.244f)
            lineToRelative(0.006f, -0.006f)
            close()
        }.path(
            fill = SolidColor(Colors.Black),
            fillAlpha = 1.0f,
            stroke = null,
            strokeAlpha = 1.0f,
            strokeLineWidth = 1.0f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Miter,
            strokeLineMiter = 1.0f,
            pathFillType = PathFillType.NonZero
        ) {
            moveTo(0.01f, 7.753f)
            arcToRelative(8.14f, 8.14f, 0f, isMoreThanHalf = false, isPositiveArc = false, 0.753f, 3.641f)
            arcToRelative(8f, 8f, 0f, isMoreThanHalf = false, isPositiveArc = false, 6.495f, 4.564f)
            arcToRelative(5f, 5f, 0f, isMoreThanHalf = false, isPositiveArc = true, -0.785f, -0.377f)
            horizontalLineToRelative(-0.01f)
            lineToRelative(-0.12f, -0.075f)
            arcToRelative(5.5f, 5.5f, 0f, isMoreThanHalf = false, isPositiveArc = true, -1.56f, -1.463f)
            arcTo(5.543f, 5.543f, 0f, isMoreThanHalf = false, isPositiveArc = true, 6.81f, 5.8f)
            lineToRelative(0.01f, -0.004f)
            lineToRelative(0.025f, -0.012f)
            curveToRelative(0.208f, -0.098f, 0.62f, -0.292f, 1.167f, -0.285f)
            quadToRelative(0.194f, 0.001f, 0.384f, 0.033f)
            arcToRelative(4f, 4f, 0f, isMoreThanHalf = false, isPositiveArc = false, -0.993f, -0.698f)
            lineToRelative(-0.01f, -0.005f)
            curveTo(6.348f, 4.282f, 5.199f, 4.263f, 5f, 4.263f)
            curveToRelative(-2.44f, 0f, -4.824f, 1.634f, -4.99f, 3.49f)
            moveToRelative(10.263f, 7.912f)
            quadToRelative(0.133f, -0.04f, 0.265f, -0.084f)
            quadToRelative(-0.153f, 0.047f, -0.307f, 0.086f)
            close()
        }.path(
            fill = SolidColor(Colors.Black),
            fillAlpha = 1.0f,
            stroke = null,
            strokeAlpha = 1.0f,
            strokeLineWidth = 1.0f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Miter,
            strokeLineMiter = 1.0f,
            pathFillType = PathFillType.NonZero
        ) {
            moveTo(10.228f, 15.667f)
            arcToRelative(5f, 5f, 0f, isMoreThanHalf = false, isPositiveArc = false, 0.303f, -0.086f)
            lineToRelative(0.082f, -0.025f)
            arcToRelative(8.02f, 8.02f, 0f, isMoreThanHalf = false, isPositiveArc = false, 4.162f, -3.3f)
            arcToRelative(0.25f, 0.25f, 0f, isMoreThanHalf = false, isPositiveArc = false, -0.331f, -0.35f)
            quadToRelative(-0.322f, 0.168f, -0.663f, 0.294f)
            arcToRelative(6.4f, 6.4f, 0f, isMoreThanHalf = false, isPositiveArc = true, -2.243f, 0.4f)
            curveToRelative(-2.957f, 0f, -5.532f, -2.031f, -5.532f, -4.644f)
            quadToRelative(0.003f, -0.203f, 0.046f, -0.399f)
            arcToRelative(4.54f, 4.54f, 0f, isMoreThanHalf = false, isPositiveArc = false, -0.46f, 5.898f)
            lineToRelative(0.003f, 0.005f)
            curveToRelative(0.315f, 0.441f, 0.707f, 0.821f, 1.158f, 1.121f)
            horizontalLineToRelative(0.003f)
            lineToRelative(0.144f, 0.09f)
            curveToRelative(0.877f, 0.55f, 1.721f, 1.078f, 3.328f, 0.996f)
        }.build()
    }

    val Github: ImageVector by lazy {
        ImageVector.Builder(
            name = "Github",
            defaultWidth = 16.dp,
            defaultHeight = 16.dp,
            viewportWidth = 16f,
            viewportHeight = 16f
        ).path(
            fill = SolidColor(Colors.Black),
            fillAlpha = 1.0f,
            stroke = null,
            strokeAlpha = 1.0f,
            strokeLineWidth = 1.0f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Miter,
            strokeLineMiter = 1.0f,
            pathFillType = PathFillType.NonZero
        ) {
            moveTo(8f, 0f)
            curveTo(3.58f, 0f, 0f, 3.58f, 0f, 8f)
            curveToRelative(0f, 3.54f, 2.29f, 6.53f, 5.47f, 7.59f)
            curveToRelative(0.4f, 0.07f, 0.55f, -0.17f, 0.55f, -0.38f)
            curveToRelative(0f, -0.19f, -0.01f, -0.82f, -0.01f, -1.49f)
            curveToRelative(-2.01f, 0.37f, -2.53f, -0.49f, -2.69f, -0.94f)
            curveToRelative(-0.09f, -0.23f, -0.48f, -0.94f, -0.82f, -1.13f)
            curveToRelative(-0.28f, -0.15f, -0.68f, -0.52f, -0.01f, -0.53f)
            curveToRelative(0.63f, -0.01f, 1.08f, 0.58f, 1.23f, 0.82f)
            curveToRelative(0.72f, 1.21f, 1.87f, 0.87f, 2.33f, 0.66f)
            curveToRelative(0.07f, -0.52f, 0.28f, -0.87f, 0.51f, -1.07f)
            curveToRelative(-1.78f, -0.2f, -3.64f, -0.89f, -3.64f, -3.95f)
            curveToRelative(0f, -0.87f, 0.31f, -1.59f, 0.82f, -2.15f)
            curveToRelative(-0.08f, -0.2f, -0.36f, -1.02f, 0.08f, -2.12f)
            curveToRelative(0f, 0f, 0.67f, -0.21f, 2.2f, 0.82f)
            curveToRelative(0.64f, -0.18f, 1.32f, -0.27f, 2f, -0.27f)
            reflectiveCurveToRelative(1.36f, 0.09f, 2f, 0.27f)
            curveToRelative(1.53f, -1.04f, 2.2f, -0.82f, 2.2f, -0.82f)
            curveToRelative(0.44f, 1.1f, 0.16f, 1.92f, 0.08f, 2.12f)
            curveToRelative(0.51f, 0.56f, 0.82f, 1.27f, 0.82f, 2.15f)
            curveToRelative(0f, 3.07f, -1.87f, 3.75f, -3.65f, 3.95f)
            curveToRelative(0.29f, 0.25f, 0.54f, 0.73f, 0.54f, 1.48f)
            curveToRelative(0f, 1.07f, -0.01f, 1.93f, -0.01f, 2.2f)
            curveToRelative(0f, 0.21f, 0.15f, 0.46f, 0.55f, 0.38f)
            arcTo(8.01f, 8.01f, 0f, isMoreThanHalf = false, isPositiveArc = false, 16f, 8f)
            curveToRelative(0f, -4.42f, -3.58f, -8f, -8f, -8f)
        }.build()
    }

    val Pictures: ImageVector by lazy {
        ImageVector.Builder(
            name = "Pictures",
            defaultWidth = 200.0.dp,
            defaultHeight = 200.0.dp,
            viewportWidth = 1024.0f,
            viewportHeight = 1024.0f
        ).path(
            fill = SolidColor(Colors(0xFF96F5FF)),
            stroke = null,
            strokeLineWidth = 0.0f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Miter,
            strokeLineMiter = 4.0f,
            pathFillType = PathFillType.NonZero
        ) {
            moveTo(125.2f, 288.0f)
            curveToRelative(0.8f, 1.4f, 1.5f, 2.8f, 2.3f, 4.1f)
            lineTo(270.1f, 530.6f)
            lineToRelative(122.0f, -211.3f)
            lineToRelative(135.7f, -241.9f)
            lineToRelative(-5.0f, -5.0f)
            curveToRelative(-2.9f, -0.1f, -5.8f, -0.1f, -8.7f, -0.1f)
            curveToRelative(-164.0f, 0.0f, -307.9f, 86.2f, -388.9f, 215.8f)
            close()
        }.path(
            fill = SolidColor(Colors(0xFF6E6E96)),
            stroke = null,
            strokeLineWidth = 0.0f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Miter,
            strokeLineMiter = 4.0f,
            pathFillType = PathFillType.NonZero
        ) {
            moveTo(270.1f, 547.1f)
            arcToRelative(16.5f, 16.5f, 0.0f, isMoreThanHalf = false, isPositiveArc = true, -14.2f, -8.0f)
            lineTo(113.3f, 300.6f)
            arcToRelative(93.5f, 93.5f, 0.0f, isMoreThanHalf = false, isPositiveArc = true, -1.9f, -3.3f)
            lineToRelative(-0.7f, -1.3f)
            arcToRelative(16.5f, 16.5f, 0.0f, isMoreThanHalf = false, isPositiveArc = true, 0.4f, -16.8f)
            curveTo(198.6f, 139.3f, 349.2f, 55.7f, 514.1f, 55.7f)
            curveToRelative(3.0f, 0.0f, 6.0f, 0.1f, 9.0f, 0.1f)
            arcToRelative(16.5f, 16.5f, 0.0f, isMoreThanHalf = false, isPositiveArc = true, 11.4f, 4.8f)
            lineToRelative(5.0f, 5.0f)
            arcToRelative(16.5f, 16.5f, 0.0f, isMoreThanHalf = false, isPositiveArc = true, 2.7f, 19.8f)
            lineTo(406.5f, 327.4f)
            lineTo(284.4f, 538.8f)
            arcToRelative(16.5f, 16.5f, 0.0f, isMoreThanHalf = false, isPositiveArc = true, -14.2f, 8.3f)
            horizontalLineToRelative(-0.1f)
            close()
            moveTo(144.5f, 288.4f)
            lineToRelative(125.4f, 209.5f)
            lineToRelative(107.9f, -186.9f)
            lineToRelative(124.6f, -222.1f)
            curveToRelative(-145.5f, 3.8f, -277.9f, 77.5f, -357.9f, 199.5f)
            close()
        }.path(
            fill = SolidColor(Colors(0xFFBFFFCE)),
            stroke = null,
            strokeLineWidth = 0.0f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Miter,
            strokeLineMiter = 4.0f,
            pathFillType = PathFillType.NonZero
        ) {
            moveTo(920.9f, 319.3f)
            curveToRelative(-72.7f, -139.6f, -215.0f, -237.1f, -381.0f, -246.3f)
            lineToRelative(-12.1f, 4.4f)
            lineToRelative(-135.7f, 241.9f)
            horizontalLineToRelative(528.8f)
            close()
        }.path(
            fill = SolidColor(Colors(0xFF6E6E96)),
            stroke = null,
            strokeLineWidth = 0.0f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Miter,
            strokeLineMiter = 4.0f,
            pathFillType = PathFillType.NonZero
        ) {
            moveTo(920.9f, 335.8f)
            lineTo(392.1f, 335.8f)
            arcToRelative(16.5f, 16.5f, 0.0f, isMoreThanHalf = false, isPositiveArc = true, -14.4f, -24.6f)
            lineToRelative(135.7f, -241.9f)
            arcToRelative(16.5f, 16.5f, 0.0f, isMoreThanHalf = false, isPositiveArc = true, 8.8f, -7.4f)
            lineToRelative(12.1f, -4.4f)
            arcToRelative(16.2f, 16.2f, 0.0f, isMoreThanHalf = false, isPositiveArc = true, 6.6f, -1.0f)
            curveToRelative(166.4f, 9.3f, 317.6f, 107.0f, 394.7f, 255.2f)
            arcToRelative(16.5f, 16.5f, 0.0f, isMoreThanHalf = false, isPositiveArc = true, -14.6f, 24.1f)
            close()
            moveTo(420.3f, 302.8f)
            horizontalLineToRelative(472.4f)
            curveToRelative(-74.4f, -123.3f, -206.1f, -204.0f, -350.3f, -213.1f)
            lineToRelative(-3.2f, 1.2f)
            lineToRelative(-118.9f, 212.0f)
            close()
        }.path(
            fill = SolidColor(Colors(0xFFFEFF73)),
            stroke = null,
            strokeLineWidth = 0.0f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Miter,
            strokeLineMiter = 4.0f,
            pathFillType = PathFillType.NonZero
        ) {
            moveTo(636.0f, 319.3f)
            lineToRelative(122.0f, 211.3f)
            lineToRelative(143.6f, 244.8f)
            horizontalLineToRelative(4.1f)
            curveToRelative(44.8f, -70.8f, 70.8f, -154.8f, 70.8f, -244.8f)
            curveToRelative(0.0f, -76.5f, -18.8f, -148.6f, -51.9f, -212.1f)
            lineToRelative(-3.7f, 0.8f)
            horizontalLineToRelative(-284.8f)
            close()
        }.path(
            fill = SolidColor(Colors(0xFF6E6E96)),
            stroke = null,
            strokeLineWidth = 0.0f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Miter,
            strokeLineMiter = 4.0f,
            pathFillType = PathFillType.NonZero
        ) {
            moveTo(905.7f, 791.9f)
            horizontalLineToRelative(-4.1f)
            arcToRelative(16.5f, 16.5f, 0.0f, isMoreThanHalf = false, isPositiveArc = true, -14.2f, -8.2f)
            lineToRelative(-143.6f, -244.8f)
            lineToRelative(-122.0f, -211.4f)
            arcToRelative(16.5f, 16.5f, 0.0f, isMoreThanHalf = false, isPositiveArc = true, 14.3f, -24.8f)
            horizontalLineToRelative(283.1f)
            lineToRelative(2.0f, -0.4f)
            arcToRelative(16.5f, 16.5f, 0.0f, isMoreThanHalf = false, isPositiveArc = true, 18.1f, 8.5f)
            curveToRelative(35.2f, 67.3f, 53.8f, 143.3f, 53.8f, 219.7f)
            curveToRelative(0.0f, 90.1f, -25.4f, 177.8f, -73.4f, 253.6f)
            arcToRelative(16.5f, 16.5f, 0.0f, isMoreThanHalf = false, isPositiveArc = true, -14.0f, 7.7f)
            close()
            moveTo(664.6f, 335.8f)
            lineToRelative(107.7f, 186.5f)
            lineToRelative(131.4f, 224.1f)
            curveToRelative(36.9f, -65.7f, 56.3f, -139.9f, 56.3f, -215.8f)
            curveToRelative(0.0f, -67.5f, -15.6f, -134.6f, -45.2f, -194.8f)
            horizontalLineToRelative(-250.2f)
            close()
        }.path(
            fill = SolidColor(Colors(0xFFFFCC5E)),
            stroke = null,
            strokeLineWidth = 0.0f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Miter,
            strokeLineMiter = 4.0f,
            pathFillType = PathFillType.NonZero
        ) {
            moveTo(901.6f, 775.4f)
            lineToRelative(-143.6f, -244.8f)
            lineToRelative(-122.0f, 211.3f)
            lineToRelative(-147.6f, 246.1f)
            curveToRelative(9.8f, 0.6f, 19.8f, 1.0f, 29.8f, 1.0f)
            curveToRelative(160.8f, 0.0f, 302.3f, -82.9f, 384.1f, -208.2f)
            lineToRelative(-0.7f, -5.3f)
            close()
        }.path(
            fill = SolidColor(Colors(0xFF6E6E96)),
            stroke = null,
            strokeLineWidth = 0.0f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Miter,
            strokeLineMiter = 4.0f,
            pathFillType = PathFillType.NonZero
        ) {
            moveTo(518.2f, 1005.4f)
            curveToRelative(-9.9f, 0.0f, -20.2f, -0.3f, -30.8f, -1.0f)
            arcToRelative(16.5f, 16.5f, 0.0f, isMoreThanHalf = false, isPositiveArc = true, -13.1f, -25.0f)
            lineToRelative(147.6f, -246.1f)
            lineToRelative(121.8f, -211.0f)
            arcToRelative(16.5f, 16.5f, 0.0f, isMoreThanHalf = false, isPositiveArc = true, 28.5f, -0.1f)
            lineToRelative(143.6f, 244.8f)
            curveToRelative(1.1f, 1.9f, 1.8f, 4.0f, 2.1f, 6.2f)
            lineToRelative(0.7f, 5.3f)
            arcToRelative(16.5f, 16.5f, 0.0f, isMoreThanHalf = false, isPositiveArc = true, -2.5f, 11.2f)
            curveTo(828.0f, 924.8f, 679.2f, 1005.4f, 518.2f, 1005.4f)
            close()
            moveTo(517.0f, 972.4f)
            horizontalLineToRelative(1.2f)
            curveToRelative(147.2f, 0.0f, 283.4f, -72.4f, 365.9f, -194.2f)
            lineToRelative(-126.0f, -214.8f)
            lineToRelative(-107.8f, 186.7f)
            lineToRelative(-133.3f, 222.3f)
            close()
        }.path(
            fill = SolidColor(Colors(0xFFFF9EAF)),
            stroke = null,
            strokeLineWidth = 0.0f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Miter,
            strokeLineMiter = 4.0f,
            pathFillType = PathFillType.NonZero
        ) {
            moveTo(636.0f, 741.9f)
            horizontalLineTo(111.4f)
            curveToRelative(72.1f, 138.5f, 212.8f, 235.5f, 377.0f, 246.1f)
            lineToRelative(147.6f, -246.1f)
            close()
        }.path(
            fill = SolidColor(Colors(0xFF6E6E96)),
            stroke = null,
            strokeLineWidth = 0.0f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Miter,
            strokeLineMiter = 4.0f,
            pathFillType = PathFillType.NonZero
        ) {
            moveTo(488.4f, 1004.4f)
            curveToRelative(-0.4f, 0.0f, -0.7f, -0.0f, -1.1f, -0.0f)
            curveToRelative(-164.6f, -10.6f, -314.3f, -108.2f, -390.6f, -254.9f)
            arcToRelative(16.5f, 16.5f, 0.0f, isMoreThanHalf = false, isPositiveArc = true, 14.6f, -24.1f)
            horizontalLineToRelative(524.6f)
            arcToRelative(16.5f, 16.5f, 0.0f, isMoreThanHalf = false, isPositiveArc = true, 14.2f, 25.0f)
            lineToRelative(-147.6f, 246.1f)
            arcToRelative(16.5f, 16.5f, 0.0f, isMoreThanHalf = false, isPositiveArc = true, -14.2f, 8.0f)
            close()
            moveTo(139.6f, 758.4f)
            curveToRelative(72.6f, 120.2f, 199.9f, 200.1f, 339.9f, 212.3f)
            lineToRelative(127.4f, -212.3f)
            lineTo(139.6f, 758.4f)
            close()
        }.path(
            fill = SolidColor(Colors(0xFFEABDFF)),
            stroke = null,
            strokeLineWidth = 0.0f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Miter,
            strokeLineMiter = 4.0f,
            pathFillType = PathFillType.NonZero
        ) {
            moveTo(127.5f, 292.2f)
            horizontalLineToRelative(-4.9f)
            curveTo(80.2f, 361.6f, 55.7f, 443.3f, 55.7f, 530.6f)
            curveToRelative(0.0f, 76.2f, 18.6f, 148.0f, 51.5f, 211.3f)
            horizontalLineToRelative(284.8f)
            lineTo(127.5f, 292.2f)
            close()
        }.path(
            fill = SolidColor(Colors(0xFF6E6E96)),
            stroke = null,
            strokeLineWidth = 0.0f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Miter,
            strokeLineMiter = 4.0f,
            pathFillType = PathFillType.NonZero
        ) {
            moveTo(392.1f, 758.4f)
            lineTo(107.3f, 758.4f)
            arcToRelative(16.5f, 16.5f, 0.0f, isMoreThanHalf = false, isPositiveArc = true, -14.6f, -8.9f)
            curveTo(57.7f, 682.4f, 39.2f, 606.7f, 39.2f, 530.6f)
            curveToRelative(0.0f, -87.4f, 23.9f, -172.8f, 69.2f, -247.0f)
            arcToRelative(16.5f, 16.5f, 0.0f, isMoreThanHalf = false, isPositiveArc = true, 14.1f, -7.9f)
            horizontalLineToRelative(4.9f)
            arcToRelative(16.5f, 16.5f, 0.0f, isMoreThanHalf = false, isPositiveArc = true, 14.2f, 8.1f)
            lineToRelative(264.6f, 449.7f)
            arcToRelative(16.5f, 16.5f, 0.0f, isMoreThanHalf = false, isPositiveArc = true, -14.2f, 24.9f)
            close()
            moveTo(117.4f, 725.3f)
            horizontalLineToRelative(245.8f)
            lineTo(125.2f, 320.8f)
            curveTo(90.5f, 385.0f, 72.3f, 457.0f, 72.3f, 530.6f)
            curveToRelative(0.0f, 68.5f, 15.2f, 133.9f, 45.2f, 194.8f)
            close()
        }.path(
            fill = SolidColor(Colors(0xFF6E6E96)),
            stroke = null,
            fillAlpha = 0.15f,
            strokeAlpha = 0.15f,
            strokeLineWidth = 0.0f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Miter,
            strokeLineMiter = 4.0f,
            pathFillType = PathFillType.NonZero
        ) {
            moveTo(767.7f, 161.5f)
            curveTo(867.2f, 240.5f, 931.1f, 362.6f, 931.1f, 499.6f)
            curveToRelative(0.0f, 238.3f, -193.2f, 431.5f, -431.5f, 431.5f)
            curveToRelative(-216.2f, 0.0f, -395.2f, -159.0f, -426.6f, -366.4f)
            curveTo(86.4f, 801.2f, 282.4f, 988.9f, 522.3f, 988.9f)
            curveToRelative(248.6f, 0.0f, 450.1f, -201.5f, 450.1f, -450.1f)
            curveToRelative(0.0f, -158.1f, -81.5f, -297.1f, -204.7f, -377.3f)
            close()
        }.path(
            fill = SolidColor(Colors(0xFF6E6E96)),
            stroke = null,
            fillAlpha = 0.15f,
            strokeAlpha = 0.15f,
            strokeLineWidth = 0.0f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Miter,
            strokeLineMiter = 4.0f,
            pathFillType = PathFillType.NonZero
        ) {
            moveTo(115.1f, 300.4f)
            lineTo(307.6f, 693.0f)
            horizontalLineTo(99.4f)
            lineToRelative(7.9f, 48.9f)
            horizontalLineToRelative(284.8f)
            close()
        }.path(
            fill = SolidColor(Colors(0xFF6E6E96)),
            stroke = null,
            fillAlpha = 0.15f,
            strokeAlpha = 0.15f,
            strokeLineWidth = 0.0f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Miter,
            strokeLineMiter = 4.0f,
            pathFillType = PathFillType.NonZero
        ) {
            moveTo(453.6f, 928.6f)
            lineToRelative(159.5f, -186.8f)
            lineToRelative(-90.3f, 188.6f)
            close()
        }.path(
            fill = SolidColor(Colors(0xFF6E6E96)),
            stroke = null,
            fillAlpha = 0.15f,
            strokeAlpha = 0.15f,
            strokeLineWidth = 0.0f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Miter,
            strokeLineMiter = 4.0f,
            pathFillType = PathFillType.NonZero
        ) {
            moveTo(662.7f, 332.4f)
            lineToRelative(237.9f, 326.9f)
            lineToRelative(-23.1f, 48.7f)
            close()
        }.path(
            fill = SolidColor(Colors(0xFF6E6E96)),
            stroke = null,
            fillAlpha = 0.15f,
            strokeAlpha = 0.15f,
            strokeLineWidth = 0.0f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Miter,
            strokeLineMiter = 4.0f,
            pathFillType = PathFillType.NonZero
        ) {
            moveTo(418.1f, 272.9f)
            horizontalLineToRelative(448.7f)
            lineToRelative(25.0f, 46.4f)
            horizontalLineTo(392.1f)
            close()
        }.path(
            fill = SolidColor(Colors(0xFF6E6E96)),
            stroke = null,
            fillAlpha = 0.15f,
            strokeAlpha = 0.15f,
            strokeLineWidth = 0.0f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Miter,
            strokeLineMiter = 4.0f,
            pathFillType = PathFillType.NonZero
        ) {
            moveTo(244.3f, 483.8f)
            lineToRelative(236.1f, -410.3f)
            lineToRelative(42.3f, -1.1f)
            lineToRelative(-258.8f, 465.2f)
            close()
        }.path(
            fill = SolidColor(Colors(0xFF6E6E96)),
            stroke = null,
            fillAlpha = 0.15f,
            strokeAlpha = 0.15f,
            strokeLineWidth = 0.0f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Miter,
            strokeLineMiter = 4.0f,
            pathFillType = PathFillType.NonZero
        ) {
            moveTo(739.7f, 562.4f)
            lineToRelative(96.3f, 207.5f)
            lineToRelative(23.5f, -32.1f)
            lineToRelative(-101.4f, -207.3f)
            close()
        }.build()
    }

    val Weibo: ImageVector by lazy {
        ImageVector.Builder(
            name = "Weibo",
            defaultWidth = 200.0.dp,
            defaultHeight = 200.0.dp,
            viewportWidth = 1024.0f,
            viewportHeight = 1024.0f
        ).path(
            fill = SolidColor(Colors(0xFFE71F19)),
            stroke = null,
            strokeLineWidth = 0.0f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Miter,
            strokeLineMiter = 4.0f,
            pathFillType = PathFillType.NonZero
        ) {
            moveTo(851.4f, 590.2f)
            curveToRelative(-22.2f, -66.2f, -90.4f, -90.4f, -105.9f, -91.9f)
            curveToRelative(-15.5f, -1.4f, -29.6f, -9.9f, -19.3f, -27.5f)
            curveToRelative(10.3f, -17.6f, 29.3f, -68.7f, -7.2f, -104.7f)
            curveToRelative(-36.6f, -36.1f, -116.5f, -22.5f, -173.1f, 0.9f)
            curveToRelative(-56.4f, 23.3f, -53.4f, 7.1f, -51.7f, -8.9f)
            curveToRelative(1.9f, -16.8f, 32.4f, -111.0f, -60.8f, -122.4f)
            curveTo(311.4f, 220.9f, 154.9f, 370.8f, 99.6f, 457.1f)
            curveTo(16.0f, 587.6f, 29.2f, 675.9f, 29.2f, 675.9f)
            horizontalLineToRelative(0.6f)
            curveToRelative(10.0f, 121.8f, 190.8f, 218.9f, 412.3f, 218.9f)
            curveToRelative(190.5f, 0.0f, 351.0f, -71.9f, 398.4f, -169.5f)
            curveToRelative(0.0f, 0.0f, 0.1f, -0.4f, 0.6f, -1.2f)
            curveToRelative(4.9f, -10.5f, 8.7f, -21.2f, 11.0f, -32.3f)
            curveToRelative(6.7f, -26.2f, 11.8f, -64.2f, -0.7f, -101.7f)
            close()
            moveTo(414.7f, 841.5f)
            curveToRelative(-157.7f, 0.0f, -285.7f, -84.1f, -285.7f, -187.8f)
            curveToRelative(0.0f, -103.7f, 127.8f, -187.8f, 285.7f, -187.8f)
            curveToRelative(157.7f, 0.0f, 285.7f, 84.1f, 285.7f, 187.8f)
            curveToRelative(0.0f, 103.8f, -128.0f, 187.8f, -285.7f, 187.8f)
            close()
        }.path(
            fill = SolidColor(Colors(0xFFF5AA15)),
            stroke = null,
            strokeLineWidth = 0.0f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Miter,
            strokeLineMiter = 4.0f,
            pathFillType = PathFillType.NonZero
        ) {
            moveTo(803.1f, 425.3f)
            curveToRelative(2.9f, 1.3f, 5.9f, 1.9f, 9.0f, 1.9f)
            curveToRelative(9.0f, 0.0f, 17.7f, -5.3f, 21.3f, -14.1f)
            curveToRelative(5.9f, -14.0f, 9.0f, -28.8f, 9.0f, -44.2f)
            curveToRelative(0.0f, -62.5f, -51.2f, -113.3f, -114.2f, -113.3f)
            curveToRelative(-15.4f, 0.0f, -30.3f, 3.0f, -44.4f, 8.9f)
            curveToRelative(-11.8f, 4.9f, -17.3f, 18.4f, -12.3f, 30.2f)
            curveToRelative(4.9f, 11.7f, 18.6f, 17.1f, 30.5f, 12.2f)
            curveToRelative(8.4f, -3.5f, 17.3f, -5.3f, 26.4f, -5.3f)
            curveToRelative(37.4f, 0.0f, 67.8f, 30.2f, 67.8f, 67.2f)
            curveToRelative(0.0f, 9.1f, -1.7f, 17.9f, -5.4f, 26.2f)
            arcToRelative(22.8f, 22.8f, 0.0f, isMoreThanHalf = false, isPositiveArc = false, dx1 = 12.3f, dy1 = 30.2f)
            lineToRelative(0.0f, 0.0f)
            close()
        }.path(
            fill = SolidColor(Colors(0xFFF5AA15)),
            stroke = null,
            strokeLineWidth = 0.0f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Miter,
            strokeLineMiter = 4.0f,
            pathFillType = PathFillType.NonZero
        ) {
            moveTo(726.9f, 114.2f)
            curveToRelative(-26.0f, 0.0f, -51.7f, 3.7f, -76.3f, 10.9f)
            curveToRelative(-18.4f, 5.5f, -28.9f, 24.6f, -23.5f, 42.9f)
            curveToRelative(5.5f, 18.3f, 24.8f, 28.7f, 43.2f, 23.3f)
            arcToRelative(201.9f, 201.9f, 0.0f, isMoreThanHalf = false, isPositiveArc = true, dx1 = 56.6f, dy1 = -8.1f)
            curveToRelative(109.3f, 0.0f, 198.2f, 88.3f, 198.2f, 196.7f)
            curveToRelative(0.0f, 19.4f, -2.9f, 38.7f, -8.4f, 57.2f)
            curveToRelative(-5.5f, 18.3f, 4.8f, 37.6f, 23.2f, 43.1f)
            curveToRelative(3.3f, 1.0f, 6.8f, 1.4f, 10.2f, 1.4f)
            curveToRelative(14.9f, 0.0f, 28.7f, -9.6f, 33.4f, -24.5f)
            curveToRelative(7.5f, -24.9f, 11.5f, -50.8f, 11.5f, -77.2f)
            curveToRelative(-0.1f, -146.6f, -120.3f, -265.8f, -268.0f, -265.8f)
            close()
        }.path(
            fill = SolidColor(Colors(0xFFF5AA15)),
            stroke = null,
            strokeLineWidth = 0.0f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Miter,
            strokeLineMiter = 4.0f,
            pathFillType = PathFillType.NonZero
        ) {
            moveTo(388.3f, 534.5f)
            curveToRelative(-84.2f, 0.0f, -152.3f, 59.2f, -152.3f, 132.3f)
            curveToRelative(0.0f, 73.1f, 68.2f, 132.3f, 152.3f, 132.3f)
            curveToRelative(84.1f, 0.0f, 152.3f, -59.2f, 152.3f, -132.3f)
            curveToRelative(0.0f, -73.2f, -68.2f, -132.3f, -152.3f, -132.3f)
            close()
            moveTo(338.5f, 752.8f)
            curveToRelative(-29.5f, 0.0f, -53.4f, -23.8f, -53.4f, -53.0f)
            curveToRelative(0.0f, -29.2f, 23.9f, -53.0f, 53.4f, -53.0f)
            curveToRelative(29.5f, 0.0f, 53.4f, 23.8f, 53.4f, 53.0f)
            curveToRelative(0.0f, 29.2f, -23.9f, 53.0f, -53.4f, 53.0f)
            close()
            moveTo(438.4f, 657.3f)
            curveToRelative(-6.4f, 11.1f, -19.3f, 15.7f, -28.7f, 10.2f)
            curveToRelative(-9.4f, -5.3f, -11.8f, -18.7f, -5.4f, -29.8f)
            curveToRelative(6.4f, -11.1f, 19.3f, -15.7f, 28.7f, -10.2f)
            curveToRelative(9.4f, 5.5f, 11.8f, 18.9f, 5.4f, 29.8f)
            close()
        }.build()
    }

    val Chaohua: ImageVector by lazy {
        ImageVector.Builder(
            name = "Chaohua",
            defaultWidth = 200.0.dp,
            defaultHeight = 200.0.dp,
            viewportWidth = 1024.0f,
            viewportHeight = 1024.0f
        ).path(
            fill = SolidColor(Colors(0xFF1296db)),
            stroke = null,
            strokeLineWidth = 0.0f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Miter,
            strokeLineMiter = 4.0f,
            pathFillType = PathFillType.NonZero
        ) {
            moveTo(275.0f, 170.5f)
            arcTo(32.0f, 32.0f, 0.0f, isMoreThanHalf = false, isPositiveArc = true, x1 = 298.7f, y1 = 160.0f)
            horizontalLineToRelative(426.7f)
            arcToRelative(32.0f, 32.0f, 0.0f, isMoreThanHalf = false, isPositiveArc = true, dx1 = 23.7f, dy1 = 10.5f)
            lineToRelative(213.3f, 234.7f)
            arcToRelative(32.0f, 32.0f, 0.0f, isMoreThanHalf = false, isPositiveArc = true, dx1 = -1.6f, dy1 = 44.8f)
            lineToRelative(-426.7f, 405.3f)
            arcToRelative(32.0f, 32.0f, 0.0f, isMoreThanHalf = false, isPositiveArc = true, dx1 = -44.1f, dy1 = -46.4f)
            lineToRelative(404.0f, -383.7f)
            lineToRelative(-182.7f, -201.0f)
            lineTo(312.8f, 224.0f)
            lineTo(130.3f, 424.7f)
            lineTo(384.0f, 660.3f)
            lineToRelative(64.5f, -59.9f)
            lineTo(297.4f, 449.3f)
            arcToRelative(32.0f, 32.0f, 0.0f, isMoreThanHalf = false, isPositiveArc = true, dx1 = -3.0f, dy1 = -41.8f)
            lineToRelative(64.0f, -85.3f)
            arcToRelative(32.0f, 32.0f, 0.0f, isMoreThanHalf = false, isPositiveArc = true, dx1 = 25.6f, dy1 = -12.8f)
            horizontalLineToRelative(234.7f)
            arcToRelative(32.0f, 32.0f, 0.0f, isMoreThanHalf = false, isPositiveArc = true, dx1 = 25.6f, dy1 = 12.8f)
            lineToRelative(64.0f, 85.3f)
            arcToRelative(32.0f, 32.0f, 0.0f, isMoreThanHalf = false, isPositiveArc = true, -3.8f, 42.7f)
            lineToRelative(-298.7f, 277.3f)
            arcToRelative(32.0f, 32.0f, 0.0f, isMoreThanHalf = false, isPositiveArc = true, -43.5f, 0.0f)
            lineToRelative(-298.7f, -277.3f)
            arcToRelative(32.0f, 32.0f, 0.0f, isMoreThanHalf = false, isPositiveArc = true, -1.9f, -45.0f)
            lineToRelative(213.3f, -234.7f)
            close()
            moveTo(495.4f, 556.8f)
            lineToRelative(144.3f, -134.0f)
            lineToRelative(-37.1f, -49.5f)
            horizontalLineToRelative(-202.7f)
            lineTo(362.2f, 423.7f)
            lineToRelative(133.2f, 133.2f)
            close()
        }.build()
    }

    val Douyin: ImageVector by lazy {
        ImageVector.Builder(
            name = "Douyin",
            defaultWidth = 200.dp,
            defaultHeight = 200.dp,
            viewportWidth = 1024f,
            viewportHeight = 1024f
        ).path(
            fill = SolidColor(Colors(0xFF111111)),
            stroke = null,
            strokeLineWidth = 0.0f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Miter,
            strokeLineMiter = 4.0f,
            pathFillType = PathFillType.NonZero
        ) {
            moveTo(0.0f, 0.0f)
            moveToRelative(184.3f, 0.0f)
            lineToRelative(655.4f, 0.0f)
            quadToRelative(184.3f, 0.0f, 184.3f, 184.3f)
            lineToRelative(0.0f, 655.4f)
            quadToRelative(0.0f, 184.3f, -184.3f, 184.3f)
            lineToRelative(-655.4f, 0.0f)
            quadToRelative(-184.3f, 0.0f, -184.3f, -184.3f)
            lineToRelative(0.0f, -655.4f)
            quadToRelative(0.0f, -184.3f, 184.3f, -184.3f)
            close()
        }.path(
            fill = SolidColor(Colors(0xFFFF4040)),
            stroke = null,
            strokeLineWidth = 0.0f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Miter,
            strokeLineMiter = 4.0f,
            pathFillType = PathFillType.NonZero
        ) {
            moveTo(204.3f, 670.6f)
            arcToRelative(246.3f, 246.3f, 0.0f, isMoreThanHalf = false, isPositiveArc = true, dx1 = 246.0f, dy1 = -246.0f)
            verticalLineToRelative(147.6f)
            arcToRelative(98.5f, 98.5f, 0.0f, isMoreThanHalf = false, isPositiveArc = false, dx1 = -98.4f, dy1 = 98.4f)
            curveToRelative(0.0f, 48.3f, 26.1f, 100.4f, 83.5f, 100.4f)
            curveToRelative(3.8f, 0.0f, 93.6f, -0.9f, 93.6f, -77.2f)
            verticalLineTo(134.4f)
            horizontalLineToRelative(157.3f)
            arcToRelative(133.3f, 133.3f, 0.0f, isMoreThanHalf = false, isPositiveArc = false, dx1 = 133.1f, dy1 = 133.0f)
            lineToRelative(-0.1f, 147.3f)
            arcToRelative(273.2f, 273.2f, 0.0f, isMoreThanHalf = false, isPositiveArc = true, dx1 = -142.6f, dy1 = -38.9f)
            lineToRelative(-0.1f, 318.0f)
            curveToRelative(0.0f, 146.0f, -124.2f, 224.8f, -241.1f, 224.8f)
            curveToRelative(-131.7f, 0.0f, -231.1f, -106.6f, -231.1f, -247.9f)
            close()
        }.path(
            fill = SolidColor(Colors(0xFF00F5FF)),
            stroke = null,
            strokeLineWidth = 0.0f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Miter,
            strokeLineMiter = 4.0f,
            pathFillType = PathFillType.NonZero
        ) {
            moveTo(164.9f, 631.2f)
            arcToRelative(246.3f, 246.3f, 0.0f, isMoreThanHalf = false, isPositiveArc = true, dx1 = 246.0f, dy1 = -246.0f)
            verticalLineToRelative(147.6f)
            arcToRelative(98.5f, 98.5f, 0.0f, isMoreThanHalf = false, isPositiveArc = false, dx1 = -98.4f, dy1 = 98.4f)
            curveToRelative(0.0f, 48.3f, 26.1f, 100.4f, 83.5f, 100.4f)
            curveToRelative(3.8f, 0.0f, 93.6f, -0.9f, 93.6f, -77.2f)
            verticalLineTo(95.0f)
            horizontalLineToRelative(157.3f)
            arcToRelative(133.3f, 133.3f, 0.0f, isMoreThanHalf = false, isPositiveArc = false, dx1 = 133.1f, dy1 = 133.0f)
            lineToRelative(-0.1f, 147.3f)
            arcToRelative(273.2f, 273.2f, 0.0f, isMoreThanHalf = false, isPositiveArc = true, dx1 = -142.6f, dy1 = -38.9f)
            lineToRelative(-0.1f, 318.0f)
            curveToRelative(0.0f, 146.0f, -124.2f, 224.8f, -241.1f, 224.8f)
            curveToRelative(-131.7f, 0.0f, -231.1f, -106.6f, -231.1f, -247.9f)
            close()
        }.path(
            fill = SolidColor(Colors(0xFFFFFFFF)),
            stroke = null,
            strokeLineWidth = 0.0f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Miter,
            strokeLineMiter = 4.0f,
            pathFillType = PathFillType.NonZero
        ) {
            moveTo(410.9f, 427.6f)
            curveToRelative(-158.8f, 20.2f, -284.4f, 222.7f, -154.1f, 405.0f)
            curveToRelative(120.4f, 98.5f, 373.7f, 41.2f, 380.7f, -171.9f)
            lineToRelative(-0.2f, -324.1f)
            arcToRelative(280.7f, 280.7f, 0.0f, isMoreThanHalf = false, isPositiveArc = false, dx1 = 142.9f, dy1 = 38.6f)
            verticalLineTo(261.2f)
            arcToRelative(145.0f, 145.0f, 0.0f, isMoreThanHalf = false, isPositiveArc = true, dx1 = -72.8f, dy1 = -54.8f)
            arcToRelative(135.2f, 135.2f, 0.0f, isMoreThanHalf = false, isPositiveArc = true, dx1 = -54.7f, dy1 = -72.5f)
            horizontalLineToRelative(-123.7f)
            lineToRelative(-0.1f, 561.4f)
            curveToRelative(-0.1f, 78.5f, -131.0f, 106.4f, -164.2f, 30.3f)
            curveToRelative(-83.2f, -39.8f, -64.4f, -190.9f, 46.3f, -192.6f)
            close()
        }.build()
    }

    val OrderMode: ImageVector by lazy {
        ImageVector.Builder(
            name = "OrderMode",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 16f,
            viewportHeight = 16f
        ). path(
            fill = SolidColor(Colors.Black),
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
            fill = SolidColor(Colors.Black),
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
            fill = SolidColor(Colors.Black),
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
            fill = SolidColor(Colors.Black),
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
            fill = SolidColor(Colors.Black),
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
            fill = SolidColor(Colors.Black),
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
            fill = SolidColor(Colors.Black),
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
            fill = SolidColor(Colors.Black),
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
            fill = SolidColor(Colors.Black),
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
            fill = SolidColor(Colors.Black),
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
            fill = SolidColor(Colors.Black),
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
            fill = SolidColor(Colors.Black),
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
            fill = SolidColor(Colors.Black),
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
            fill = SolidColor(Colors.Black),
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
            fill = SolidColor(Colors.Black),
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

    val Artist by lazy {
        ImageVector.Builder(
            name = "Artist",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 960f,
            viewportHeight = 960f
        ).path(
            fill = SolidColor(Colors.Black),
            fillAlpha = 1f,
            stroke = null,
            strokeAlpha = 1f,
            strokeLineWidth = 1f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Miter,
            strokeLineMiter = 1f,
            pathFillType = PathFillType.NonZero
        ) {
            moveTo(740f, 400f)
            horizontalLineToRelative(140f)
            verticalLineToRelative(80f)
            horizontalLineToRelative(-80f)
            verticalLineToRelative(220f)
            quadToRelative(0f, 42f, -29f, 71f)
            reflectiveQuadToRelative(-71f, 29f)
            reflectiveQuadToRelative(-71f, -29f)
            reflectiveQuadToRelative(-29f, -71f)
            reflectiveQuadToRelative(29f, -71f)
            reflectiveQuadToRelative(71f, -29f)
            quadToRelative(8f, 0f, 18f, 1.5f)
            reflectiveQuadToRelative(22f, 6.5f)
            close()
            moveTo(120f, 800f)
            verticalLineToRelative(-112f)
            quadToRelative(0f, -35f, 17.5f, -63f)
            reflectiveQuadToRelative(46.5f, -43f)
            quadToRelative(62f, -31f, 126f, -46.5f)
            reflectiveQuadTo(440f, 520f)
            quadToRelative(42f, 0f, 83.5f, 6.5f)
            reflectiveQuadTo(607f, 546f)
            quadToRelative(-20f, 12f, -36f, 29f)
            reflectiveQuadToRelative(-28f, 37f)
            quadToRelative(-26f, -6f, -51.5f, -9f)
            reflectiveQuadToRelative(-51.5f, -3f)
            quadToRelative(-57f, 0f, -112f, 14f)
            reflectiveQuadToRelative(-108f, 40f)
            quadToRelative(-9f, 5f, -14.5f, 14f)
            reflectiveQuadToRelative(-5.5f, 20f)
            verticalLineToRelative(32f)
            horizontalLineToRelative(321f)
            quadToRelative(2f, 20f, 9.5f, 40f)
            reflectiveQuadToRelative(20.5f, 40f)
            close()
            moveToRelative(320f, -320f)
            quadToRelative(-66f, 0f, -113f, -47f)
            reflectiveQuadToRelative(-47f, -113f)
            reflectiveQuadToRelative(47f, -113f)
            reflectiveQuadToRelative(113f, -47f)
            reflectiveQuadToRelative(113f, 47f)
            reflectiveQuadToRelative(47f, 113f)
            reflectiveQuadToRelative(-47f, 113f)
            reflectiveQuadToRelative(-113f, 47f)
            moveToRelative(0f, -80f)
            quadToRelative(33f, 0f, 56.5f, -23.5f)
            reflectiveQuadTo(520f, 320f)
            reflectiveQuadToRelative(-23.5f, -56.5f)
            reflectiveQuadTo(440f, 240f)
            reflectiveQuadToRelative(-56.5f, 23.5f)
            reflectiveQuadTo(360f, 320f)
            reflectiveQuadToRelative(23.5f, 56.5f)
            reflectiveQuadTo(440f, 400f)
            moveToRelative(0f, 320f)
        }.build()
    }
    
    val QQ by lazy {
        ImageVector.Builder(
            name = "QQ",
            defaultWidth = 16.dp,
            defaultHeight = 16.dp,
            viewportWidth = 16f,
            viewportHeight = 16f
        ).path(
            fill = SolidColor(Colors.Black),
            fillAlpha = 1f,
            stroke = null,
            strokeAlpha = 1f,
            strokeLineWidth = 1f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Miter,
            strokeLineMiter = 1f,
            pathFillType = PathFillType.NonZero
        ) {
            moveTo(6.048f, 3.323f)
            curveToRelative(0.022f, 0.277f, -0.13f, 0.523f, -0.338f, 0.55f)
            curveToRelative(-0.21f, 0.026f, -0.397f, -0.176f, -0.419f, -0.453f)
            reflectiveCurveToRelative(0.13f, -0.523f, 0.338f, -0.55f)
            curveToRelative(0.21f, -0.026f, 0.397f, 0.176f, 0.42f, 0.453f)
            close()
            moveToRelative(2.265f, -0.24f)
            curveToRelative(-0.603f, -0.146f, -0.894f, 0.256f, -0.936f, 0.333f)
            curveToRelative(-0.027f, 0.048f, -0.008f, 0.117f, 0.037f, 0.15f)
            curveToRelative(0.045f, 0.035f, 0.092f, 0.025f, 0.119f, -0.003f)
            curveToRelative(0.361f, -0.39f, 0.751f, -0.172f, 0.829f, -0.129f)
            lineToRelative(0.011f, 0.007f)
            curveToRelative(0.053f, 0.024f, 0.147f, 0.028f, 0.193f, -0.098f)
            curveToRelative(0.023f, -0.063f, 0.017f, -0.11f, -0.006f, -0.142f)
            curveToRelative(-0.016f, -0.023f, -0.089f, -0.08f, -0.247f, -0.118f)
        }.path(
            fill = SolidColor(Colors.Black),
            fillAlpha = 1f,
            stroke = null,
            strokeAlpha = 1f,
            strokeLineWidth = 1f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Miter,
            strokeLineMiter = 1f,
            pathFillType = PathFillType.NonZero
        ) {
            moveTo(11.727f, 6.719f)
            curveToRelative(0f, -0.022f, 0.01f, -0.375f, 0.01f, -0.557f)
            curveToRelative(0f, -3.07f, -1.45f, -6.156f, -5.015f, -6.156f)
            reflectiveCurveTo(1.708f, 3.092f, 1.708f, 6.162f)
            curveToRelative(0f, 0.182f, 0.01f, 0.535f, 0.01f, 0.557f)
            lineToRelative(-0.72f, 1.795f)
            arcToRelative(26f, 26f, 0f, isMoreThanHalf = false, isPositiveArc = false, -0.534f, 1.508f)
            curveToRelative(-0.68f, 2.187f, -0.46f, 3.093f, -0.292f, 3.113f)
            curveToRelative(0.36f, 0.044f, 1.401f, -1.647f, 1.401f, -1.647f)
            curveToRelative(0f, 0.979f, 0.504f, 2.256f, 1.594f, 3.179f)
            curveToRelative(-0.408f, 0.126f, -0.907f, 0.319f, -1.228f, 0.556f)
            curveToRelative(-0.29f, 0.213f, -0.253f, 0.43f, -0.201f, 0.518f)
            curveToRelative(0.228f, 0.386f, 3.92f, 0.246f, 4.985f, 0.126f)
            curveToRelative(1.065f, 0.12f, 4.756f, 0.26f, 4.984f, -0.126f)
            curveToRelative(0.052f, -0.088f, 0.088f, -0.305f, -0.2f, -0.518f)
            curveToRelative(-0.322f, -0.237f, -0.822f, -0.43f, -1.23f, -0.557f)
            curveToRelative(1.09f, -0.922f, 1.594f, -2.2f, 1.594f, -3.178f)
            curveToRelative(0f, 0f, 1.041f, 1.69f, 1.401f, 1.647f)
            curveToRelative(0.168f, -0.02f, 0.388f, -0.926f, -0.292f, -3.113f)
            arcToRelative(26f, 26f, 0f, isMoreThanHalf = false, isPositiveArc = false, -0.534f, -1.508f)
            lineToRelative(-0.72f, -1.795f)
            close()
            moveTo(9.773f, 5.53f)
            arcToRelative(0.1f, 0.1f, 0f, isMoreThanHalf = false, isPositiveArc = true, -0.009f, 0.096f)
            curveToRelative(-0.109f, 0.159f, -1.554f, 0.943f, -3.033f, 0.943f)
            horizontalLineToRelative(-0.017f)
            curveToRelative(-1.48f, 0f, -2.925f, -0.784f, -3.034f, -0.943f)
            arcToRelative(0.1f, 0.1f, 0f, isMoreThanHalf = false, isPositiveArc = true, -0.018f, -0.055f)
            quadToRelative(0f, -0.022f, 0.01f, -0.04f)
            curveToRelative(0.13f, -0.287f, 1.43f, -0.606f, 3.042f, -0.606f)
            horizontalLineToRelative(0.017f)
            curveToRelative(1.611f, 0f, 2.912f, 0.319f, 3.042f, 0.605f)
            moveToRelative(-4.32f, -0.989f)
            curveToRelative(-0.483f, 0.022f, -0.896f, -0.529f, -0.922f, -1.229f)
            reflectiveCurveToRelative(0.344f, -1.286f, 0.828f, -1.308f)
            curveToRelative(0.483f, -0.022f, 0.896f, 0.529f, 0.922f, 1.23f)
            curveToRelative(0.027f, 0.7f, -0.344f, 1.286f, -0.827f, 1.307f)
            close()
            moveToRelative(2.538f, 0f)
            curveToRelative(-0.484f, -0.022f, -0.854f, -0.607f, -0.828f, -1.308f)
            curveToRelative(0.027f, -0.7f, 0.44f, -1.25f, 0.923f, -1.23f)
            curveToRelative(0.483f, 0.023f, 0.853f, 0.608f, 0.827f, 1.309f)
            curveToRelative(-0.026f, 0.7f, -0.439f, 1.251f, -0.922f, 1.23f)
            close()
            moveTo(2.928f, 8.99f)
            quadToRelative(0.32f, 0.063f, 0.639f, 0.117f)
            verticalLineToRelative(2.336f)
            reflectiveCurveToRelative(1.104f, 0.222f, 2.21f, 0.068f)
            verticalLineTo(9.363f)
            quadToRelative(0.49f, 0.027f, 0.937f, 0.023f)
            horizontalLineToRelative(0.017f)
            curveToRelative(1.117f, 0.013f, 2.474f, -0.136f, 3.786f, -0.396f)
            curveToRelative(0.097f, 0.622f, 0.151f, 1.386f, 0.097f, 2.284f)
            curveToRelative(-0.146f, 2.45f, -1.6f, 3.99f, -3.846f, 4.012f)
            horizontalLineToRelative(-0.091f)
            curveToRelative(-2.245f, -0.023f, -3.7f, -1.562f, -3.846f, -4.011f)
            curveToRelative(-0.054f, -0.9f, 0f, -1.663f, 0.097f, -2.285f)
        }.build()
    }

    val Taobao by lazy {
        ImageVector.Builder(
            name = "Taobao",
            defaultWidth = 200.dp,
            defaultHeight = 200.dp,
            viewportWidth = 1024f,
            viewportHeight = 1024f
        ).path(
            fill = SolidColor(Colors.Black),
            stroke = null,
            strokeLineWidth = 0f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Miter,
            strokeLineMiter = 4f,
            pathFillType = PathFillType.NonZero
        ) {
            moveTo(959.8f, 312.7f)
            curveToRelative(-0.2f, -14.3f, -0.2f, -20.1f, -0.3f, -26.8f)
            curveToRelative(-0.3f, -20.1f, -1.7f, -40.4f, -5.2f, -58.7f)
            curveToRelative(-3.8f, -19.8f, -8.8f, -37.8f, -17.9f, -55.9f)
            curveToRelative(-9.3f, -18.2f, -22.0f, -34.1f, -35.8f, -48.2f)
            curveToRelative(-14.1f, -13.8f, -29.9f, -26.5f, -48.2f, -35.8f)
            curveToRelative(-18.1f, -9.1f, -36.0f, -14.1f, -55.7f, -17.9f)
            curveToRelative(-18.1f, -3.4f, -38.5f, -4.6f, -58.7f, -5.0f)
            curveToRelative(-6.7f, -0.2f, -12.6f, -0.2f, -26.8f, -0.3f)
            curveToRelative(-13.6f, -0.2f, -24.6f, -0.2f, -32.9f, -0.2f)
            curveToRelative(-41.6f, 0.0f, -291.0f, 0.0f, -332.7f, 0.0f)
            curveToRelative(-8.3f, 0.0f, -19.3f, 0.0f, -32.9f, 0.2f)
            curveToRelative(-14.3f, 0.2f, -20.1f, 0.2f, -26.8f, 0.3f)
            curveToRelative(-20.1f, 0.3f, -40.4f, 1.7f, -58.7f, 5.2f)
            curveToRelative(-19.8f, 3.8f, -37.8f, 8.8f, -55.9f, 17.9f)
            curveToRelative(-18.2f, 9.3f, -34.1f, 22.0f, -48.2f, 35.8f)
            curveToRelative(-13.8f, 14.1f, -26.5f, 29.9f, -35.6f, 48.2f)
            curveToRelative(-9.1f, 18.1f, -14.1f, 36.0f, -17.9f, 55.9f)
            curveToRelative(-3.4f, 18.1f, -4.8f, 38.5f, -5.0f, 58.7f)
            curveToRelative(-0.2f, 6.7f, -0.2f, 12.6f, -0.3f, 26.8f)
            curveToRelative(-0.2f, 13.6f, -0.2f, 24.8f, -0.2f, 33.0f)
            lineToRelative(0.0f, 332.7f)
            curveToRelative(0.0f, 8.3f, 0.0f, 19.3f, 0.2f, 32.9f)
            curveToRelative(0.2f, 14.3f, 0.2f, 20.1f, 0.3f, 26.8f)
            curveToRelative(0.3f, 20.1f, 1.5f, 40.4f, 5.0f, 58.7f)
            curveToRelative(3.8f, 19.8f, 8.8f, 37.8f, 17.9f, 55.9f)
            curveToRelative(9.3f, 18.2f, 21.8f, 34.1f, 35.6f, 48.2f)
            curveToRelative(14.1f, 13.8f, 29.9f, 26.5f, 48.2f, 35.8f)
            curveToRelative(18.1f, 9.1f, 36.0f, 14.1f, 55.7f, 17.9f)
            curveToRelative(18.2f, 3.4f, 38.5f, 4.6f, 58.7f, 5.2f)
            curveToRelative(6.7f, 0.2f, 12.6f, 0.2f, 26.8f, 0.3f)
            curveToRelative(13.6f, 0.2f, 24.8f, 0.2f, 32.9f, 0.2f)
            curveToRelative(41.6f, 0.0f, 291.0f, 0.0f, 332.7f, 0.0f)
            curveToRelative(8.3f, 0.0f, 19.3f, 0.0f, 32.9f, -0.2f)
            curveToRelative(14.3f, -0.2f, 20.1f, -0.2f, 26.8f, -0.3f)
            curveToRelative(20.1f, -0.3f, 40.4f, -1.7f, 58.7f, -5.2f)
            curveToRelative(19.8f, -3.8f, 37.8f, -8.8f, 55.7f, -17.9f)
            curveToRelative(18.2f, -9.3f, 34.1f, -22.0f, 48.2f, -35.8f)
            curveToRelative(13.8f, -14.1f, 26.5f, -29.9f, 35.8f, -48.2f)
            curveToRelative(9.1f, -18.1f, 14.1f, -36.0f, 17.9f, -55.9f)
            curveToRelative(3.4f, -18.1f, 4.6f, -38.5f, 5.2f, -58.7f)
            curveToRelative(0.2f, -6.7f, 0.2f, -12.6f, 0.3f, -26.8f)
            curveToRelative(0.2f, -13.6f, 0.2f, -24.8f, 0.2f, -32.9f)
            lineTo(959.8f, 345.6f)
            curveTo(960.0f, 337.5f, 960.0f, 326.3f, 959.8f, 312.7f)
            close()
            moveTo(280.6f, 239.1f)
            curveToRelative(35.1f, 0.0f, 63.6f, 26.0f, 63.6f, 58.1f)
            reflectiveCurveToRelative(-28.6f, 58.1f, -63.6f, 58.1f)
            curveToRelative(-35.4f, 0.0f, -64.0f, -26.1f, -64.0f, -58.1f)
            curveTo(216.7f, 264.9f, 245.1f, 239.1f, 280.6f, 239.1f)
            close()
            moveTo(333.7f, 588.5f)
            curveToRelative(-16.9f, 53.0f, -12.6f, 33.4f, -79.5f, 181.1f)
            lineToRelative(-96.7f, -61.4f)
            curveToRelative(0.0f, 0.0f, 108.2f, -99.9f, 130.2f, -145.7f)
            curveToRelative(23.6f, -48.2f, -26.1f, -73.8f, -26.1f, -73.8f)
            lineToRelative(-74.3f, -47.0f)
            lineToRelative(40.4f, -63.0f)
            curveToRelative(55.9f, 42.3f, 60.0f, 45.8f, 97.7f, 84.3f)
            curveTo(354.7f, 493.2f, 350.9f, 534.4f, 333.7f, 588.5f)
            close()
            moveTo(860.6f, 657.4f)
            curveToRelative(-18.9f, 180.1f, -249.6f, 113.0f, -249.6f, 113.0f)
            lineToRelative(12.6f, -51.6f)
            lineToRelative(53.5f, 11.5f)
            curveToRelative(98.7f, 6.4f, 89.1f, -81.2f, 89.1f, -81.2f)
            lineTo(766.2f, 394.4f)
            curveToRelative(0.7f, -96.8f, -89.6f, -107.0f, -252.0f, -48.0f)
            lineToRelative(37.7f, 10.5f)
            curveToRelative(-3.1f, 11.2f, -15.3f, 28.9f, -31.1f, 48.2f)
            lineTo(737.9f, 405.1f)
            lineToRelative(0.0f, 44.7f)
            lineToRelative(-122.1f, 0.0f)
            lineToRelative(0.0f, 55.7f)
            lineToRelative(121.8f, 0.0f)
            lineToRelative(0.0f, 44.7f)
            lineTo(615.8f, 550.3f)
            lineToRelative(0.0f, 93.4f)
            curveToRelative(18.4f, -6.0f, 35.3f, -14.4f, 49.9f, -25.6f)
            lineToRelative(-10.7f, -40.6f)
            lineToRelative(57.5f, -18.2f)
            lineToRelative(48.0f, 119.0f)
            lineTo(689.8f, 708.2f)
            lineToRelative(-12.6f, -47.5f)
            curveToRelative(-31.7f, 24.6f, -97.2f, 60.2f, -211.9f, 56.9f)
            curveToRelative(-122.3f, 3.3f, -90.8f, -140.2f, -90.8f, -140.2f)
            lineToRelative(3.1f, -1.5f)
            lineToRelative(86.2f, 0.0f)
            curveToRelative(-0.7f, 18.4f, -8.1f, 48.5f, 2.1f, 64.8f)
            curveToRelative(8.4f, 13.4f, 29.9f, 15.7f, 43.5f, 16.5f)
            curveToRelative(1.5f, 0.2f, 3.3f, 0.2f, 4.6f, 0.2f)
            lineTo(514.0f, 550.4f)
            lineToRelative(-124.5f, 0.0f)
            lineToRelative(0.0f, -44.7f)
            lineToRelative(124.5f, 0.0f)
            lineToRelative(0.0f, -55.7f)
            lineTo(481.6f, 450.0f)
            curveToRelative(-28.0f, 30.1f, -53.7f, 55.2f, -53.7f, 55.2f)
            lineToRelative(-37.7f, -33.4f)
            curveToRelative(26.8f, -28.7f, 53.5f, -74.1f, 70.0f, -104.2f)
            curveToRelative(-13.4f, 5.5f, -27.2f, 11.5f, -41.5f, 17.7f)
            curveToRelative(-13.8f, 18.1f, -29.8f, 36.3f, -47.6f, 54.5f)
            curveToRelative(0.7f, 0.9f, -61.8f, -35.6f, -61.8f, -35.6f)
            curveToRelative(64.3f, -55.7f, 100.5f, -175.3f, 100.5f, -175.3f)
            lineToRelative(89.6f, 25.5f)
            curveToRelative(0.0f, 0.0f, -7.2f, 17.7f, -22.7f, 44.7f)
            curveToRelative(358.1f, -103.0f, 379.3f, 63.1f, 379.3f, 63.1f)
            reflectiveCurveTo(879.3f, 477.2f, 860.6f, 657.4f)
            close()
        }.build()
    }

    val QQMusic by lazy {
        ImageVector.Builder(
            name = "QQMusic",
            defaultWidth = 200.0.dp,
            defaultHeight = 200.0.dp,
            viewportWidth = 1024.0f,
            viewportHeight = 1024.0f
        ).path(
            fill = SolidColor(Colors(0xFFF8C913)),
            stroke = null,
            strokeLineWidth = 0.0f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Miter,
            strokeLineMiter = 4.0f,
            pathFillType = PathFillType.NonZero
        ) {
            moveTo(42.7f, 516.7f)
            arcTo(469.3f, 469.3f, 0.0f, isMoreThanHalf = true, isPositiveArc = false, 512.0f, 52.1f)
            arcToRelative(469.3f, 469.3f, 0.0f, isMoreThanHalf = false, isPositiveArc = false, -469.3f, 464.6f)
            close()
        }.path(
            fill = SolidColor(Colors(0xFF02B053)),
            stroke = null,
            strokeLineWidth = 0.0f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Miter,
            strokeLineMiter = 4.0f,
            pathFillType = PathFillType.NonZero
        ) {
            moveTo(654.9f, 12.4f)
            arcToRelative(217.2f, 217.2f, 0.0f, isMoreThanHalf = false, isPositiveArc = true, dx1 = -105.4f, dy1 = 49.5f)
            arcToRelative(409.6f, 409.6f, 0.0f, isMoreThanHalf = false, isPositiveArc = false, dx1 = -128.0f, dy1 = 39.7f)
            arcToRelative(184.7f, 184.7f, 0.0f, isMoreThanHalf = false, isPositiveArc = false, dx1 = -42.7f, dy1 = 29.9f)
            curveToRelative(-20.1f, 17.5f, -35.0f, 49.5f, -30.3f, 59.7f)
            reflectiveCurveToRelative(50.3f, 72.1f, 107.9f, 154.0f)
            reflectiveCurveTo(569.6f, 504.3f, 582.0f, 521.4f)
            arcToRelative(269.2f, 269.2f, 0.0f, isMoreThanHalf = false, isPositiveArc = true, 20.1f, 32.4f)
            curveToRelative(0.0f, 2.6f, -10.2f, 0.0f, -20.1f, -2.6f)
            arcTo(256.0f, 256.0f, 0.0f, isMoreThanHalf = false, isPositiveArc = false, 421.5f, 576.0f)
            arcToRelative(260.3f, 260.3f, 0.0f, isMoreThanHalf = false, isPositiveArc = false, -97.7f, 96.9f)
            arcToRelative(170.7f, 170.7f, 0.0f, isMoreThanHalf = false, isPositiveArc = false, -7.7f, 131.8f)
            arcToRelative(183.9f, 183.9f, 0.0f, isMoreThanHalf = false, isPositiveArc = false, 90.5f, 94.3f)
            arcToRelative(146.3f, 146.3f, 0.0f, isMoreThanHalf = false, isPositiveArc = false, 82.8f, 20.1f)
            arcToRelative(221.9f, 221.9f, 0.0f, isMoreThanHalf = false, isPositiveArc = false, 75.1f, -5.1f)
            arcToRelative(216.3f, 216.3f, 0.0f, isMoreThanHalf = false, isPositiveArc = false, 165.5f, -183.9f)
            curveToRelative(2.6f, -47.4f, -2.6f, -62.3f, -65.3f, -170.7f)
            curveToRelative(-97.7f, -170.7f, -185.6f, -323.0f, -185.6f, -325.5f)
            lineToRelative(40.1f, -5.1f)
            arcToRelative(166.0f, 166.0f, 0.0f, isMoreThanHalf = false, isPositiveArc = false, dx1 = 145.5f, dy1 = -85.3f)
            curveToRelative(12.4f, -22.2f, 12.4f, -29.9f, 12.4f, -77.2f)
            arcTo(221.4f, 221.4f, 0.0f, isMoreThanHalf = false, isPositiveArc = false, x1 = 672.0f, y1 = 6.4f)
            curveToRelative(-2.1f, -9.0f, -4.3f, -6.4f, -17.1f, 6.0f)
            close()
        }.build()
    }

    val NetEaseCloudMusic by lazy {
        ImageVector.Builder(
            name = "NetEaseCloudMusic",
            defaultWidth = 200.0.dp,
            defaultHeight = 200.0.dp,
            viewportWidth = 1024.0f,
            viewportHeight = 1024.0f
        ).path(
            fill = SolidColor(Colors(0xFFEA3E3C)),
            stroke = null,
            strokeLineWidth = 0.0f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Miter,
            strokeLineMiter = 4.0f,
            pathFillType = PathFillType.NonZero
        ) {
            moveTo(0.0f, 0.0f)
            moveToRelative(184.3f, 0.0f)
            lineToRelative(655.4f, 0.0f)
            quadToRelative(184.3f, 0.0f, 184.3f, 184.3f)
            lineToRelative(0.0f, 655.4f)
            quadToRelative(0.0f, 184.3f, -184.3f, 184.3f)
            lineToRelative(-655.4f, 0.0f)
            quadToRelative(-184.3f, 0.0f, -184.3f, -184.3f)
            lineToRelative(0.0f, -655.4f)
            quadToRelative(0.0f, -184.3f, 184.3f, -184.3f)
            close()
        }.path(
            fill = SolidColor(Colors.White),
            stroke = null,
            strokeLineWidth = 0.0f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Miter,
            strokeLineMiter = 4.0f,
            pathFillType = PathFillType.NonZero
        ) {
            moveTo(527.6f, 849.4f)
            arcToRelative(373.6f, 373.6f, 0.0f, isMoreThanHalf = false, isPositiveArc = true, -162.5f, -39.0f)
            curveToRelative(-112.4f, -55.2f, -180.0f, -176.3f, -172.6f, -308.7f)
            curveToRelative(7.4f, -130.3f, 85.1f, -237.5f, 202.8f, -279.6f)
            arcToRelative(35.9f, 35.9f, 0.0f, isMoreThanHalf = false, isPositiveArc = true, 24.2f, 67.5f)
            curveToRelative(-107.7f, 38.5f, -150.8f, 136.9f, -155.3f, 216.1f)
            curveToRelative(-5.9f, 103.5f, 46.1f, 197.8f, 132.3f, 240.1f)
            curveToRelative(124.7f, 60.3f, 216.9f, 22.4f, 260.8f, -5.6f)
            curveToRelative(59.8f, -38.2f, 97.9f, -100.0f, 97.0f, -157.6f)
            curveToRelative(-1.0f, -63.7f, -24.1f, -121.0f, -63.3f, -157.1f)
            arcToRelative(145.4f, 145.4f, 0.0f, isMoreThanHalf = false, isPositiveArc = false, -65.6f, -35.3f)
            quadToRelative(2.8f, 9.8f, 5.6f, 19.3f)
            curveToRelative(13.4f, 45.6f, 24.9f, 85.1f, 25.6f, 114.4f)
            arcToRelative(134.3f, 134.3f, 0.0f, isMoreThanHalf = false, isPositiveArc = true, -37.7f, 97.8f)
            arcToRelative(139.1f, 139.1f, 0.0f, isMoreThanHalf = false, isPositiveArc = true, -100.7f, 40.5f)
            arcToRelative(140.1f, 140.1f, 0.0f, isMoreThanHalf = false, isPositiveArc = true, -100.5f, -42.2f)
            arcToRelative(169.1f, 169.1f, 0.0f, isMoreThanHalf = false, isPositiveArc = true, -46.3f, -122.8f)
            curveToRelative(1.2f, -85.1f, 80.1f, -153.3f, 162.8f, -175.1f)
            arcToRelative(324.8f, 324.8f, 0.0f, isMoreThanHalf = false, isPositiveArc = true, -6.7f, -67.1f)
            arcToRelative(92.1f, 92.1f, 0.0f, isMoreThanHalf = false, isPositiveArc = true, 69.2f, -91.8f)
            curveToRelative(46.2f, -12.5f, 104.4f, 5.2f, 124.7f, 37.9f)
            arcToRelative(35.8f, 35.8f, 0.0f, isMoreThanHalf = false, isPositiveArc = true, -11.7f, 49.3f)
            arcToRelative(35.8f, 35.8f, 0.0f, isMoreThanHalf = false, isPositiveArc = true, -49.3f, -11.7f)
            arcToRelative(62.3f, 62.3f, 0.0f, isMoreThanHalf = false, isPositiveArc = false, -48.5f, -5.2f)
            curveToRelative(-4.3f, 1.7f, -12.4f, 4.9f, -12.8f, 23.1f)
            arcToRelative(270.9f, 270.9f, 0.0f, isMoreThanHalf = false, isPositiveArc = false, 6.7f, 58.5f)
            arcToRelative(217.5f, 217.5f, 0.0f, isMoreThanHalf = false, isPositiveArc = true, 133.6f, 57.7f)
            curveToRelative(53.6f, 49.4f, 85.0f, 125.5f, 86.4f, 208.7f)
            curveToRelative(1.3f, 81.9f, -49.8f, 167.9f, -130.0f, 219.1f)
            arcToRelative(310.1f, 310.1f, 0.0f, isMoreThanHalf = false, isPositiveArc = true, -168.2f, 48.7f)
            close()
            moveTo(551.3f, 391.9f)
            curveToRelative(-56.8f, 15.7f, -107.5f, 63.0f, -108.1f, 106.4f)
            arcToRelative(98.3f, 98.3f, 0.0f, isMoreThanHalf = false, isPositiveArc = false, 25.7f, 71.4f)
            arcToRelative(68.0f, 68.0f, 0.0f, isMoreThanHalf = false, isPositiveArc = false, 49.4f, 20.9f)
            arcToRelative(67.2f, 67.2f, 0.0f, isMoreThanHalf = false, isPositiveArc = false, 49.4f, -18.9f)
            arcToRelative(63.2f, 63.2f, 0.0f, isMoreThanHalf = false, isPositiveArc = false, 17.2f, -46.1f)
            curveToRelative(-0.4f, -19.8f, -11.7f, -58.4f, -22.7f, -95.7f)
            curveToRelative(-3.6f, -12.4f, -7.4f, -25.1f, -11.0f, -38.0f)
            close()
        }.build()
    }

    val KugouMusic by lazy {
        ImageVector.Builder(
            name = "KugouMusic",
            defaultWidth = 200.0.dp,
            defaultHeight = 200.0.dp,
            viewportWidth = 1024.0f,
            viewportHeight = 1024.0f
        ).path(
            fill = SolidColor(Colors(0xFF3D8BFA)),
            stroke = null,
            strokeLineWidth = 0.0f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Miter,
            strokeLineMiter = 4.0f,
            pathFillType = PathFillType.NonZero
        ) {
            moveTo(849.9f, 51.2f)
            lineTo(174.1f, 51.2f)
            curveToRelative(-67.9f, 0.0f, -122.9f, 55.0f, -122.9f, 122.9f)
            verticalLineToRelative(675.8f)
            curveToRelative(0.0f, 67.9f, 55.0f, 122.9f, 122.9f, 122.9f)
            horizontalLineToRelative(675.8f)
            curveToRelative(67.9f, 0.0f, 122.9f, -55.0f, 122.9f, -122.9f)
            lineTo(972.8f, 174.1f)
            curveToRelative(0.0f, -67.9f, -55.0f, -122.9f, -122.9f, -122.9f)
            close()
            moveTo(512.0f, 819.2f)
            curveToRelative(-169.4f, 0.0f, -307.2f, -137.8f, -307.2f, -307.2f)
            reflectiveCurveToRelative(137.8f, -307.2f, 307.2f, -307.2f)
            reflectiveCurveToRelative(307.2f, 137.8f, 307.2f, 307.2f)
            reflectiveCurveToRelative(-137.8f, 307.2f, -307.2f, 307.2f)
            close()
        }.path(
            fill = SolidColor(Colors(0xFF3D8BFA)),
            stroke = null,
            strokeLineWidth = 0.0f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Miter,
            strokeLineMiter = 4.0f,
            pathFillType = PathFillType.NonZero
        ) {
            moveTo(512.0f, 235.5f)
            curveToRelative(-152.5f, 0.0f, -276.5f, 124.0f, -276.5f, 276.5f)
            reflectiveCurveToRelative(124.0f, 276.5f, 276.5f, 276.5f)
            reflectiveCurveToRelative(276.5f, -124.0f, 276.5f, -276.5f)
            reflectiveCurveToRelative(-124.0f, -276.5f, -276.5f, -276.5f)
            close()
            moveTo(555.0f, 529.9f)
            lineToRelative(103.4f, 132.6f)
            horizontalLineToRelative(-94.2f)
            lineTo(485.4f, 549.4f)
            curveToRelative(-13.2f, -20.0f, -16.7f, -35.3f, -16.1f, -46.5f)
            lineToRelative(-14.8f, 159.7f)
            horizontalLineToRelative(-70.7f)
            lineToRelative(27.7f, -298.0f)
            horizontalLineToRelative(70.7f)
            lineToRelative(-12.5f, 134.6f)
            curveToRelative(1.9f, -14.1f, 10.2f, -20.5f, 10.2f, -20.5f)
            lineToRelative(114.2f, -114.2f)
            horizontalLineToRelative(87.0f)
            reflectiveCurveTo(581.6f, 464.9f, 559.6f, 486.9f)
            reflectiveCurveToRelative(-4.6f, 43.0f, -4.6f, 43.0f)
            close()
        }.build()
    }

    val ResetPicture by lazy {
        ImageVector.Builder(
            name = "ResetPicture",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 960f,
            viewportHeight = 960f
        ).path(
            fill = SolidColor(Colors.Black),
            fillAlpha = 1f,
            stroke = null,
            strokeAlpha = 1f,
            strokeLineWidth = 1f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Miter,
            strokeLineMiter = 1f,
            pathFillType = PathFillType.NonZero
        ) {
            moveTo(120f, 360f)
            verticalLineToRelative(-240f)
            horizontalLineToRelative(80f)
            verticalLineToRelative(134f)
            quadToRelative(50f, -62f, 122.5f, -98f)
            reflectiveQuadTo(480f, 120f)
            quadToRelative(118f, 0f, 210.5f, 67f)
            reflectiveQuadTo(820f, 360f)
            horizontalLineToRelative(-87f)
            quadToRelative(-34f, -72f, -101f, -116f)
            reflectiveQuadToRelative(-152f, -44f)
            quadToRelative(-57f, 0f, -107.5f, 21f)
            reflectiveQuadTo(284f, 280f)
            horizontalLineToRelative(76f)
            verticalLineToRelative(80f)
            close()
            moveToRelative(120f, 360f)
            horizontalLineToRelative(480f)
            lineTo(570f, 520f)
            lineTo(450f, 680f)
            lineToRelative(-90f, -120f)
            close()
            moveTo(200f, 880f)
            quadToRelative(-33f, 0f, -56.5f, -23.5f)
            reflectiveQuadTo(120f, 800f)
            verticalLineToRelative(-320f)
            horizontalLineToRelative(80f)
            verticalLineToRelative(320f)
            horizontalLineToRelative(560f)
            verticalLineToRelative(-320f)
            horizontalLineToRelative(80f)
            verticalLineToRelative(320f)
            quadToRelative(0f, 33f, -23.5f, 56.5f)
            reflectiveQuadTo(760f, 880f)
            close()
        }.build()
    }

    val RewardCup by lazy {
        ImageVector.Builder(
            name = "RewardCup",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 960f,
            viewportHeight = 960f
        ).path(
            fill = SolidColor(Colors.Black)
        ) {
            moveTo(480f, 440f)
            quadToRelative(33f, 0f, 56.5f, -23.5f)
            reflectiveQuadTo(560f, 360f)
            reflectiveQuadToRelative(-23.5f, -56.5f)
            reflectiveQuadTo(480f, 280f)
            reflectiveQuadToRelative(-56.5f, 23.5f)
            reflectiveQuadTo(400f, 360f)
            reflectiveQuadToRelative(23.5f, 56.5f)
            reflectiveQuadTo(480f, 440f)
            moveTo(280f, 840f)
            verticalLineToRelative(-80f)
            horizontalLineToRelative(160f)
            verticalLineToRelative(-124f)
            quadToRelative(-49f, -11f, -87.5f, -41.5f)
            reflectiveQuadTo(296f, 518f)
            quadToRelative(-75f, -9f, -125.5f, -65.5f)
            reflectiveQuadTo(120f, 320f)
            verticalLineToRelative(-40f)
            quadToRelative(0f, -33f, 23.5f, -56.5f)
            reflectiveQuadTo(200f, 200f)
            horizontalLineToRelative(80f)
            verticalLineToRelative(-80f)
            horizontalLineToRelative(400f)
            verticalLineToRelative(80f)
            horizontalLineToRelative(80f)
            quadToRelative(33f, 0f, 56.5f, 23.5f)
            reflectiveQuadTo(840f, 280f)
            verticalLineToRelative(40f)
            quadToRelative(0f, 76f, -50.5f, 132.5f)
            reflectiveQuadTo(664f, 518f)
            quadToRelative(-18f, 46f, -56.5f, 76.5f)
            reflectiveQuadTo(520f, 636f)
            verticalLineToRelative(124f)
            horizontalLineToRelative(160f)
            verticalLineToRelative(80f)
            close()
            moveToRelative(0f, -408f)
            verticalLineToRelative(-152f)
            horizontalLineToRelative(-80f)
            verticalLineToRelative(40f)
            quadToRelative(0f, 38f, 22f, 68.5f)
            reflectiveQuadToRelative(58f, 43.5f)
            moveToRelative(200f, 128f)
            quadToRelative(50f, 0f, 85f, -35f)
            reflectiveQuadToRelative(35f, -85f)
            verticalLineToRelative(-240f)
            horizontalLineTo(360f)
            verticalLineToRelative(240f)
            quadToRelative(0f, 50f, 35f, 85f)
            reflectiveQuadToRelative(85f, 35f)
            moveToRelative(200f, -128f)
            quadToRelative(36f, -13f, 58f, -43.5f)
            reflectiveQuadToRelative(22f, -68.5f)
            verticalLineToRelative(-40f)
            horizontalLineToRelative(-80f)
            close()
            moveToRelative(-200f, -52f)
        }.build()
    }

    val Rank1 by lazy {
        ImageVector.Builder(
            name = "Rank1",
            defaultWidth = 200.0.dp,
            defaultHeight = 200.0.dp,
            viewportWidth = 1024.0f,
            viewportHeight = 1024.0f
        ).path(
            fill = SolidColor(Colors(0xFFE64A19)),
            stroke = null,
            strokeLineWidth = 0.0f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Miter,
            strokeLineMiter = 4.0f,
            pathFillType = PathFillType.NonZero
        ) {
            moveTo(337.2f, 0.0f)
            horizontalLineTo(162.4f)
            lineToRelative(249.8f, 449.6f)
            horizontalLineToRelative(174.8f)
            lineTo(337.2f, 0.0f)
            close()
        }.path(
            fill = SolidColor(Colors(0xFFFF754C)),
            stroke = null,
            strokeLineWidth = 0.0f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Miter,
            strokeLineMiter = 4.0f,
            pathFillType = PathFillType.NonZero
        ) {
            moveTo(686.9f, 0.0f)
            lineTo(437.1f, 449.6f)
            horizontalLineTo(611.9f)
            lineTo(861.7f, 0.0f)
            horizontalLineTo(686.9f)
            close()
        }.path(
            fill = SolidColor(Colors(0xFFC93D18)),
            stroke = null, strokeLineWidth = 0.0f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Miter,
            strokeLineMiter = 4.0f,
            pathFillType = PathFillType.NonZero
        ) {
            moveTo(587.0f, 449.6f)
            lineToRelative(-1.7f, -3.3f)
            lineToRelative(-1.7f, 3.3f)
            horizontalLineToRelative(3.4f)
            close()
            moveTo(482.8f, 260.7f)
            lineToRelative(-86.0f, 160.4f)
            lineToRelative(27.9f, 54.4f)
            lineToRelative(87.0f, -160.6f)
            lineToRelative(-28.9f, -54.2f)
            close()
        }.path(
            fill = SolidColor(Colors(0xFFFFBA57)),
            stroke = null,
            strokeLineWidth = 0.0f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Miter,
            strokeLineMiter = 4.0f,
            pathFillType = PathFillType.NonZero
        ) {
            moveTo(512.0f, 724.3f)
            moveToRelative(-299.7f, 0.0f)
            arcToRelative(299.7f, 299.7f, 0.0f, isMoreThanHalf = true, isPositiveArc = false, dx1 = 599.4f, dy1 = 0.0f)
            arcToRelative(299.7f, 299.7f, 0.0f, isMoreThanHalf = true, isPositiveArc = false, dx1 = -599.4f, dy1 = 0.0f)
            close()
        }.path(
            fill = SolidColor(Colors(0xFFFFE082)),
            stroke = null,
            strokeLineWidth = 0.0f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Miter,
            strokeLineMiter = 4.0f,
            pathFillType = PathFillType.NonZero
        ) {
            moveTo(512.0f, 974.0f)
            curveToRelative(-137.9f, 0.0f, -249.8f, -111.8f, -249.8f, -249.8f)
            reflectiveCurveToRelative(111.8f, -249.8f, 249.8f, -249.8f)
            reflectiveCurveToRelative(249.8f, 111.8f, 249.8f, 249.8f)
            curveToRelative(-0.2f, 138.0f, -111.9f, 249.7f, -249.8f, 249.8f)
            close()
            moveTo(437.1f, 799.2f)
            verticalLineToRelative(50.0f)
            horizontalLineToRelative(149.9f)
            verticalLineToRelative(-50.0f)
            horizontalLineToRelative(-50.0f)
            lineTo(537.0f, 649.4f)
            horizontalLineToRelative(50.0f)
            verticalLineToRelative(-50.0f)
            lineTo(437.1f, 599.4f)
            verticalLineToRelative(50.0f)
            horizontalLineToRelative(50.0f)
            verticalLineToRelative(149.9f)
            horizontalLineToRelative(-50.0f)
            close()
        }.build()
    }

    val Rank2 by lazy {
        ImageVector.Builder(
            name = "Rank2",
            defaultWidth = 200.0.dp,
            defaultHeight = 200.0.dp,
            viewportWidth = 1024.0f,
            viewportHeight = 1024.0f
        ).path(
            fill = SolidColor(Colors(0xFFE64A19)),
            stroke = null,
            strokeLineWidth = 0.0f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Miter,
            strokeLineMiter = 4.0f,
            pathFillType = PathFillType.NonZero
        ) {
            moveTo(337.2f, 0.0f)
            horizontalLineTo(162.3f)
            lineToRelative(249.8f, 449.6f)
            horizontalLineToRelative(174.8f)
            lineTo(337.2f, 0.0f)
            close()
        }.path(
            fill = SolidColor(Colors(0xFFFF754C)),
            stroke = null,
            strokeLineWidth = 0.0f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Miter,
            strokeLineMiter = 4.0f,
            pathFillType = PathFillType.NonZero
        ) {
            moveTo(686.8f, 0.0f)
            lineTo(437.1f, 449.6f)
            horizontalLineToRelative(174.8f)
            lineTo(861.7f, 0.0f)
            horizontalLineTo(686.8f)
            close()
        }.path(
            fill = SolidColor(Colors(0xFFC93D18)),
            stroke = null,
            strokeLineWidth = 0.0f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Miter,
            strokeLineMiter = 4.0f,
            pathFillType = PathFillType.NonZero
        ) {
            moveTo(586.9f, 449.6f)
            lineToRelative(-1.7f, -3.3f)
            lineToRelative(-1.7f, 3.3f)
            horizontalLineToRelative(3.4f)
            close()
            moveTo(482.7f, 260.7f)
            lineToRelative(-86.0f, 160.4f)
            lineToRelative(27.9f, 54.4f)
            lineToRelative(87.0f, -160.6f)
            lineToRelative(-28.9f, -54.2f)
            close()
        }.path(
            fill = SolidColor(Colors(0xFF9FA8DA)),
            stroke = null,
            strokeLineWidth = 0.0f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Miter,
            strokeLineMiter = 4.0f,
            pathFillType = PathFillType.NonZero
        ) {
            moveTo(512.0f, 724.3f)
            moveToRelative(-299.7f, 0.0f)
            arcToRelative(299.7f, 299.7f, 0.0f, isMoreThanHalf = true, isPositiveArc = false, dx1 = 599.4f, dy1 = 0.0f)
            arcToRelative(299.7f, 299.7f, 0.0f, isMoreThanHalf = true, isPositiveArc = false, dx1 = -599.4f, dy1 = 0.0f)
            close()
        }.path(
            fill = SolidColor(Colors(0xFFC5CAE9)),
            stroke = null,
            strokeLineWidth = 0.0f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Miter,
            strokeLineMiter = 4.0f,
            pathFillType = PathFillType.NonZero
        ) {
            moveTo(487.0f, 649.4f)
            horizontalLineToRelative(50.0f)
            verticalLineToRelative(149.9f)
            horizontalLineToRelative(-50.0f)
            close()
        }.path(
            fill = SolidColor(Colors(0xFFC5CAE9)),
            stroke = null,
            strokeLineWidth = 0.0f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Miter,
            strokeLineMiter = 4.0f,
            pathFillType = PathFillType.NonZero
        ) {
            moveTo(512.0f, 974.0f)
            curveToRelative(-137.9f, 0.0f, -249.8f, -111.8f, -249.8f, -249.8f)
            reflectiveCurveTo(374.1f, 474.5f, 512.0f, 474.5f)
            reflectiveCurveToRelative(249.8f, 111.8f, 249.8f, 249.8f)
            curveTo(761.6f, 862.2f, 649.9f, 973.9f, 512.0f, 974.0f)
            close()
            moveTo(387.1f, 799.2f)
            verticalLineToRelative(50.0f)
            horizontalLineToRelative(249.8f)
            verticalLineToRelative(-50.0f)
            horizontalLineToRelative(-50.0f)
            verticalLineTo(649.4f)
            horizontalLineToRelative(50.0f)
            verticalLineToRelative(-50.0f)
            horizontalLineTo(387.1f)
            verticalLineToRelative(50.0f)
            horizontalLineToRelative(50.0f)
            verticalLineToRelative(149.9f)
            horizontalLineToRelative(-50.0f)
            close()
        }.build()
    }

    val Rank3 by lazy {
        ImageVector.Builder(
            name = "Rank3",
            defaultWidth = 200.0.dp,
            defaultHeight = 200.0.dp,
            viewportWidth = 1024.0f,
            viewportHeight = 1024.0f
        ).path(
            fill = SolidColor(Colors(0xFFE64A19)),
            stroke = null,
            strokeLineWidth = 0.0f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Miter,
            strokeLineMiter = 4.0f,
            pathFillType = PathFillType.NonZero
        ) {
            moveTo(337.2f, 0.0f)
            horizontalLineTo(162.3f)
            lineToRelative(249.8f, 449.6f)
            horizontalLineToRelative(174.8f)
            lineTo(337.2f, 0.0f)
            close()
        }.path(
            fill = SolidColor(Colors(0xFFFF754C)),
            stroke = null,
            strokeLineWidth = 0.0f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Miter,
            strokeLineMiter = 4.0f,
            pathFillType = PathFillType.NonZero
        ) {
            moveTo(686.8f, 0.0f)
            lineTo(437.1f, 449.6f)
            horizontalLineToRelative(174.8f)
            lineTo(861.7f, 0.0f)
            horizontalLineTo(686.8f)
            close()
        }.path(
            fill = SolidColor(Colors(0xFFC93D18)),
            stroke = null,
            strokeLineWidth = 0.0f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Miter,
            strokeLineMiter = 4.0f,
            pathFillType = PathFillType.NonZero
        ) {
            moveTo(586.9f, 449.6f)
            lineToRelative(-1.7f, -3.3f)
            lineToRelative(-1.7f, 3.3f)
            horizontalLineToRelative(3.4f)
            close()
            moveTo(482.7f, 260.7f)
            lineToRelative(-86.0f, 160.4f)
            lineToRelative(27.9f, 54.4f)
            lineToRelative(87.0f, -160.6f)
            lineToRelative(-28.9f, -54.2f)
            close()
        }.path(
            fill = SolidColor(Colors(0xFFAF8A77)),
            stroke = null,
            strokeLineWidth = 0.0f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Miter,
            strokeLineMiter = 4.0f,
            pathFillType = PathFillType.NonZero
        ) {
            moveTo(512.0f, 724.3f)
            moveToRelative(-299.7f, 0.0f)
            arcToRelative(299.7f, 299.7f, 0.0f, isMoreThanHalf = true, isPositiveArc = false, dx1 = 599.4f, dy1 = 0.0f)
            arcToRelative(299.7f, 299.7f, 0.0f, isMoreThanHalf = true, isPositiveArc = false, dx1 = -599.4f, dy1 = 0.0f)
            close()
        }.path(
            fill = SolidColor(Colors(0xFFCEB1A1)),
            stroke = null,
            strokeLineWidth = 0.0f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Miter,
            strokeLineMiter = 4.0f,
            pathFillType = PathFillType.NonZero
        ) {
            moveTo(512.0f, 974.0f)
            curveToRelative(-137.9f, 0.0f, -249.8f, -111.8f, -249.8f, -249.8f)
            reflectiveCurveTo(374.1f, 474.5f, 512.0f, 474.5f)
            reflectiveCurveToRelative(249.8f, 111.8f, 249.8f, 249.8f)
            curveTo(761.6f, 862.2f, 649.9f, 973.9f, 512.0f, 974.0f)
            close()
            moveTo(337.2f, 799.2f)
            verticalLineToRelative(50.0f)
            horizontalLineToRelative(349.7f)
            verticalLineToRelative(-50.0f)
            horizontalLineToRelative(-50.0f)
            verticalLineTo(649.4f)
            horizontalLineToRelative(50.0f)
            verticalLineToRelative(-50.0f)
            horizontalLineTo(337.2f)
            verticalLineToRelative(50.0f)
            horizontalLineToRelative(50.0f)
            verticalLineToRelative(149.9f)
            horizontalLineToRelative(-50.0f)
            close()
        }.path(
            fill = SolidColor(Colors(0xFFCEB1A1)),
            stroke = null,
            strokeLineWidth = 0.0f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Miter,
            strokeLineMiter = 4.0f,
            pathFillType = PathFillType.NonZero
        ) {
            moveTo(537.0f, 649.4f)
            horizontalLineToRelative(50.0f)
            verticalLineToRelative(149.9f)
            horizontalLineToRelative(-50.0f)
            close()
            moveTo(437.1f, 649.4f)
            horizontalLineToRelative(50.0f)
            verticalLineToRelative(149.9f)
            horizontalLineToRelative(-50.0f)
            close()
        }.build()
    }

    val Disconnect by lazy {
        ImageVector.Builder(
            name = "Disconnect",
            defaultWidth = 16.dp,
            defaultHeight = 16.dp,
            viewportWidth = 16f,
            viewportHeight = 16f
        ).path(fill = SolidColor(Colors.Black)) {
            moveTo(13.617f, 3.844f)
            arcToRelative(2.87f, 2.87f, 0f, isMoreThanHalf = false, isPositiveArc = false, dx1 = -0.451f, dy1 = -0.868f)
            lineToRelative(1.354f, -1.36f)
            lineTo(13.904f, 1f)
            lineToRelative(-1.36f, 1.354f)
            arcToRelative(2.877f, 2.877f, 0f, isMoreThanHalf = false, isPositiveArc = false, dx1 = -0.868f, dy1 = -0.452f)
            arcToRelative(3.073f, 3.073f, 0f, isMoreThanHalf = false, isPositiveArc = false, dx1 = -2.14f, dy1 = 0.075f)
            arcToRelative(3.03f, 3.03f, 0f, isMoreThanHalf = false, isPositiveArc = false, dx1 = -0.991f, dy1 = 0.664f)
            lineTo(7f, 4.192f)
            lineToRelative(4.327f, 4.328f)
            lineToRelative(1.552f, -1.545f)
            curveToRelative(0.287f, -0.287f, 0.508f, -0.618f, 0.663f, -0.992f)
            arcToRelative(3.074f, 3.074f, 0f, isMoreThanHalf = false, isPositiveArc = false, dx1 = 0.075f, dy1 = -2.14f)
            close()
            moveToRelative(-0.889f, 1.804f)
            arcToRelative(2.15f, 2.15f, 0f, isMoreThanHalf = false, isPositiveArc = true, dx1 = -0.471f, dy1 = 0.705f)
            lineToRelative(-0.93f, 0.93f)
            lineToRelative(-3.09f, -3.09f)
            lineToRelative(0.93f, -0.93f)
            arcToRelative(2.15f, 2.15f, 0f, isMoreThanHalf = false, isPositiveArc = true, dx1 = 0.704f, dy1 = -0.472f)
            arcToRelative(2.134f, 2.134f, 0f, isMoreThanHalf = false, isPositiveArc = true, dx1 = 1.689f, dy1 = 0.007f)
            curveToRelative(0.264f, 0.114f, 0.494f, 0.271f, 0.69f, 0.472f)
            curveToRelative(0.2f, 0.195f, 0.358f, 0.426f, 0.472f, 0.69f)
            arcToRelative(2.134f, 2.134f, 0f, isMoreThanHalf = false, isPositiveArc = true, dx1 = 0.007f, dy1 = 1.688f)
            close()
            moveToRelative(-4.824f, 4.994f)
            lineToRelative(1.484f, -1.545f)
            lineToRelative(-0.616f, -0.622f)
            lineToRelative(-1.49f, 1.551f)
            lineToRelative(-1.86f, -1.859f)
            lineToRelative(1.491f, -1.552f)
            lineTo(6.291f, 6f)
            lineTo(4.808f, 7.545f)
            lineToRelative(-0.616f, -0.615f)
            lineToRelative(-1.551f, 1.545f)
            arcToRelative(3f, 3f, 0f, isMoreThanHalf = false, isPositiveArc = false, dx1 = -0.663f, dy1 = 0.998f)
            arcToRelative(3.023f, 3.023f, 0f, isMoreThanHalf = false, isPositiveArc = false, dx1 = -0.233f, dy1 = 1.169f)
            curveToRelative(0f, 0.332f, 0.05f, 0.656f, 0.15f, 0.97f)
            curveToRelative(0.105f, 0.31f, 0.258f, 0.597f, 0.459f, 0.862f)
            lineTo(1f, 13.834f)
            lineToRelative(0.615f, 0.615f)
            lineToRelative(1.36f, -1.353f)
            curveToRelative(0.265f, 0.2f, 0.552f, 0.353f, 0.862f, 0.458f)
            curveToRelative(0.314f, 0.1f, 0.638f, 0.15f, 0.97f, 0.15f)
            curveToRelative(0.406f, 0f, 0.796f, -0.077f, 1.17f, -0.232f)
            curveToRelative(0.378f, -0.155f, 0.71f, -0.376f, 0.998f, -0.663f)
            lineToRelative(1.545f, -1.552f)
            lineToRelative(-0.616f, -0.615f)
            close()
            moveToRelative(-2.262f, 2.023f)
            arcToRelative(2.16f, 2.16f, 0f, isMoreThanHalf = false, isPositiveArc = true, dx1 = -0.834f, dy1 = 0.164f)
            curveToRelative(-0.301f, 0f, -0.586f, -0.057f, -0.855f, -0.17f)
            arcToRelative(2.278f, 2.278f, 0f, isMoreThanHalf = false, isPositiveArc = true, dx1 = -0.697f, dy1 = -0.466f)
            arcToRelative(2.28f, 2.28f, 0f, isMoreThanHalf = false, isPositiveArc = true, dx1 = -0.465f, dy1 = -0.697f)
            arcToRelative(2.167f, 2.167f, 0f, isMoreThanHalf = false, isPositiveArc = true, dx1 = -0.17f, dy1 = -0.854f)
            arcToRelative(2.16f, 2.16f, 0f, isMoreThanHalf = false, isPositiveArc = true, dx1 = 0.642f, dy1 = -1.545f)
            lineToRelative(0.93f, -0.93f)
            lineToRelative(3.09f, 3.09f)
            lineToRelative(-0.93f, 0.93f)
            arcToRelative(2.22f, 2.22f, 0f, isMoreThanHalf = false, isPositiveArc = true, dx1 = -0.711f, dy1 = 0.478f)
            close()
        }.build()
    }
}