package love.yinlin

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.uikit.ComposeUIViewControllerDelegate
import androidx.compose.ui.window.ComposeUIViewController
import kotlinx.coroutines.delay
import love.yinlin.compose.*
import love.yinlin.platform.ActualAppContext
import love.yinlin.platform.ActualFloatingLyrics
import love.yinlin.platform.Coroutines
import love.yinlin.platform.app
import love.yinlin.platform.appNative
import love.yinlin.resources.Res
import love.yinlin.resources.xwwk
import platform.UIKit.UIViewController

lateinit var controller: UIViewController

fun MainViewController(): UIViewController {
    ActualAppContext().apply {
        app = this
        initialize()
    }

    return ComposeUIViewController(
        configure = {
            delegate = object : ComposeUIViewControllerDelegate {
                override fun viewDidAppear(animated: Boolean) {
                    ActualFloatingLyrics(controller).let {
                        appNative.musicFactory.floatingLyrics = it
                        if (appNative.config.enabledFloatingLyrics) {
                            Coroutines.startMain {
                                delay(1000L)
                                it.attach()
                            }
                        }
                    }
                }
            }
        }
    ) {
        App(
            deviceFactory = { maxWidth, maxHeight -> Device(maxWidth, maxHeight) },
            themeMode = app.config.themeMode,
            fontScale = app.config.fontScale,
            mainFontResource = Res.font.xwwk,
            modifier = Modifier.fillMaxSize()
        ) { _, _ ->
            AppUI()
        }
    }.apply {
        controller = this
    }
}