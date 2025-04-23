package love.yinlin.common

import love.yinlin.AppModel
import love.yinlin.ui.screen.community.ScreenUserCard
import love.yinlin.ui.screen.music.ScreenImportMusic

class DeepLink(private val model: AppModel) {
    private fun schemeContent(uri: Uri) {
        model.navigate(ScreenImportMusic.Args(uri.toString()))
    }

    private fun schemeRachel(uri: Uri) {
        when (uri.path) {
            "/openProfile" -> {
                uri.params["uid"]?.toIntOrNull()?.let { uid ->
                    model.navigate(ScreenUserCard.Args(uid))
                }
            }
        }
    }

    fun process(uri: Uri) {
        when (uri.scheme) {
            Scheme.Content -> schemeContent(uri)
            Scheme.Rachel -> schemeRachel(uri)
        }
    }
}