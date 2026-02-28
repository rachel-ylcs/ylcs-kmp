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
            secondary = Colors(0xff76c1c6),
            secondaryContainer = Colors(0xff1c8d95),
            tertiary = Colors(0xffef91a1),
            tertiaryContainer = Colors(0xffc48b92),
            onContainer = Colors(0xfffbfbfb),
            onContainerVariant = Colors(0xffe0e0e0),
            background = Colors.Ghost,
            backgroundVariant = Colors(0xfff2f2f2),
            onBackground = Colors.Black,
            onBackgroundVariant = Colors(0xff444444),
            surface = Colors(0xfff9f9f9),
            onSurface = Colors(0xff1a1a1a),
            onSurfaceVariant = Colors.Gray5,
            error = Colors.Red5,
            onError = Colors.Ghost,
            warning = Colors.Yellow4,
            onWarning = Colors.Ghost,
            outline = Colors(0xff79747e),
            disabledContent = Colors(0x611c1b1f),
            disabledContainer = Colors(0xffe0dddd),
            scrim = Colors.Dark,
        )

        internal val DefaultDark = ColorTheme(
            primary = Colors(0xffb0d5de),
            primaryContainer = Colors(0xff7da1aa),
            secondary = Colors(0xff9ac84b),
            secondaryContainer = Colors(0xff608c46),
            tertiary = Colors(0xffd6c8ff),
            tertiaryContainer = Colors(0xff7a89ce),
            onContainer = Colors(0xffe8e8e8),
            onContainerVariant = Colors(0xffd5d5d5),
            background = Colors.Dark,
            backgroundVariant = Colors(0xff3a3a3a),
            onBackground = Colors.White,
            onBackgroundVariant = Colors(0xffe2e2e2),
            surface = Colors(0xff292929),
            onSurface = Colors.White,
            onSurfaceVariant = Colors.Gray4,
            error = Colors.Red4,
            onError = Colors.Ghost,
            warning = Colors.Yellow5,
            onWarning = Colors.Ghost,
            outline = Colors(0xff938f99),
            disabledContent = Colors(0x61e6e1e5),
            disabledContainer = Colors(0xff2f3232),
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