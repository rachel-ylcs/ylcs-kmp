package love.yinlin

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import love.yinlin.compose.ColorSystem
import love.yinlin.compose.Theme
import love.yinlin.compose.ThemeMode
import love.yinlin.compose.screen.ScreenManager
import love.yinlin.gallery.screen.GalleryRoutes
import love.yinlin.gallery.screen.OverviewScreen
import love.yinlin.gallery.screen.registerGallery

@Composable
fun App() {
    val colors = remember {
        ColorSystem.Default.copy(
            light = ColorSystem.Default.light.copy(
                primary = Color(0xff137c83),
                primaryContainer = Color(0xff126b73),
                secondaryContainer = Color(0xff405675),
                background = Color(0xfff3f6fa),
                backgroundVariant = Color(0xffeaf0f5),
                surface = Color(0xffffffff),
                onBackground = Color(0xff172b43),
                onBackgroundVariant = Color(0xff52657a),
                onSurface = Color(0xff172b43),
                onSurfaceVariant = Color(0xff52657a),
                outline = Color(0xffbdcbd8),
            ),
            dark = ColorSystem.Default.dark.copy(
                primary = Color(0xff7bd7d9),
                primaryContainer = Color(0xff126b73),
                background = Color(0xff101b2b),
                backgroundVariant = Color(0xff203044),
                surface = Color(0xff19283b),
                onBackgroundVariant = Color(0xffb0bfd0),
                onSurfaceVariant = Color(0xffb0bfd0),
                outline = Color(0xff53677f),
            ),
        )
    }
    Theme(themeMode = ThemeMode.LIGHT, colorSystem = colors) {
        ScreenManager.Navigation<OverviewScreen> {
            registerGallery(GalleryRoutes())
        }
    }
}
