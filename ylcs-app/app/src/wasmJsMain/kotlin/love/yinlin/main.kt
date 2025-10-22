package love.yinlin

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.ComposeViewport
import kotlinx.browser.document
import love.yinlin.compose.*
import love.yinlin.compose.ui.floating.localBalloonTipEnabled
import love.yinlin.platform.ActualAppContext
import love.yinlin.platform.app
import love.yinlin.resources.Res
import love.yinlin.resources.xwwk

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    ActualAppContext().apply {
        app = this
        initialize()
    }
    ComposeViewport(document.body!!) {
        App(
            deviceFactory = { maxWidth, maxHeight -> Device(maxWidth, maxHeight) },
            themeMode = app.config.themeMode,
            fontScale = app.config.fontScale,
            mainFontResource = Res.font.xwwk,
            modifier = Modifier.fillMaxSize(),
            localProvider = arrayOf(
                LocalAnimationSpeed provides app.config.animationSpeed,
                localBalloonTipEnabled provides app.config.enabledTip
            ),
        ) { _, _ ->
            AppUI()
        }
    }
}