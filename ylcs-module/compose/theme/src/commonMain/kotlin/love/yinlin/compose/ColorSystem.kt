package love.yinlin.compose

import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.Color

@Stable
data class ColorTheme(
    val primary: Color, // 第一主题色
    val secondary: Color, // 第二主题色
    val tertiary: Color, // 第三主题色
    val primaryContainer: Color, // 第一主题容器色
    val secondaryContainer: Color, // 第二主题容器色
    val tertiaryContainer: Color, // 第三主题容器色
    val onContainer: Color, // 容器内容色
    val onContainerVariant: Color, // 容器内容变体色
    val background: Color, // 背景色
    val backgroundVariant: Color, // 背景变体
    val onBackground: Color, // 背景内容色
    val onBackgroundVariant: Color, // 背景内容变体色
    val surface: Color, // 表面色
    val onSurface: Color, // 表面内容色
    val onSurfaceVariant: Color, // 表面内容变体色
    val error: Color, // 错误色
    val onError: Color, // 错误内容色
    val warning: Color, // 警告色
    val onWarning: Color, // 警告内容色
    val outline: Color, // 轮廓色
    val disabledContent: Color, // 禁止内容色
    val disabledContainer: Color, // 禁止容器色
    val scrim: Color, // 遮罩色
) {
    companion object {
        internal val DefaultLight = ColorTheme(
            primary = Colors.Steel4,
            primaryContainer = Colors.Steel6,
            secondary = Color(0xff76c1c6),
            secondaryContainer = Color(0xff1c8d95),
            tertiary = Color(0xffef91a1),
            tertiaryContainer = Color(0xffc48b92),
            onContainer = Color(0xfffbfbfb),
            onContainerVariant = Color(0xffe0e0e0),
            background = Colors.Ghost,
            backgroundVariant = Color(0xfff2f2f2),
            onBackground = Colors.Black,
            onBackgroundVariant = Color(0xff444444),
            surface = Color(0xfff9f9f9),
            onSurface = Color(0xff1a1a1a),
            onSurfaceVariant = Colors.Gray5,
            error = Colors.Red5,
            onError = Colors.Ghost,
            warning = Colors.Yellow4,
            onWarning = Colors.Ghost,
            outline = Color(0xff79747e),
            disabledContent = Color(0x611c1b1f),
            disabledContainer = Color(0xffe0dddd),
            scrim = Colors.Dark,
        )

        internal val DefaultDark = ColorTheme(
            primary = Color(0xffb0d5de),
            primaryContainer = Color(0xff7da1aa),
            secondary = Color(0xff9ac84b),
            secondaryContainer = Color(0xff608c46),
            tertiary = Color(0xffd6c8ff),
            tertiaryContainer = Color(0xff7a89ce),
            onContainer = Color(0xffe8e8e8),
            onContainerVariant = Color(0xffd5d5d5),
            background = Colors.Dark,
            backgroundVariant = Color(0xff3a3a3a),
            onBackground = Colors.White,
            onBackgroundVariant = Color(0xffe2e2e2),
            surface = Color(0xff292929),
            onSurface = Colors.White,
            onSurfaceVariant = Colors.Gray4,
            error = Colors.Red4,
            onError = Colors.Ghost,
            warning = Colors.Yellow5,
            onWarning = Colors.Ghost,
            outline = Color(0xff938f99),
            disabledContent = Color(0x61e6e1e5),
            disabledContainer = Color(0xff2f3232),
            scrim = Colors.Black,
        )
    }
}

@Stable
data class ColorSystem(val light: ColorTheme, val dark: ColorTheme) {
    companion object {
        val Default = ColorSystem(light = ColorTheme.DefaultLight, dark = ColorTheme.DefaultDark)
    }
}