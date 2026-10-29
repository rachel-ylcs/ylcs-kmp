package love.yinlin

import androidx.compose.runtime.Composable
import love.yinlin.compose.screen.ScreenManager
import love.yinlin.gallery.screen.GalleryRoutes
import love.yinlin.gallery.screen.OverviewScreen
import love.yinlin.gallery.screen.registerGallery

@Composable
fun App() {
    ScreenManager.Navigation<OverviewScreen> {
        registerGallery(GalleryRoutes())
    }
}
