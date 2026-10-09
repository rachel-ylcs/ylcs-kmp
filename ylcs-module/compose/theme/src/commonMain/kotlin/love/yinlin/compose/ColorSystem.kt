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
            primary = Color(0xff356b82),
            primaryContainer = Color(0xff2d5c70),
            secondary = Color(0xff527039),
            secondaryContainer = Color(0xff476232),
            tertiary = Color(0xff745c8b),
            tertiaryContainer = Color(0xff655178),

            onContainer = Color(0xffffffff),
            onContainerVariant = Color(0xffe2e9ec),

            background = Color(0xffeef2f4),
            backgroundVariant = Color(0xffe1e8ec),
            onBackground = Color(0xff24323a),
            onBackgroundVariant = Color(0xff52636d),

            surface = Color(0xffffffff),
            onSurface = Color(0xff24323a),
            onSurfaceVariant = Color(0xff52636d),

            error = Color(0xffb43845),
            onError = Color(0xffffffff),
            warning = Color(0xff8b590a),
            onWarning = Color(0xffffffff),

            outline = Color(0xff76858d),
            disabledContent = Color(0x6124323a),
            disabledContainer = Color(0xffdee4e7),
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