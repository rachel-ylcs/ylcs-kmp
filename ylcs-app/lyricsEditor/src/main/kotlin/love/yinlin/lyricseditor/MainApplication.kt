package love.yinlin.lyricseditor

import love.yinlin.compose.ComposeApplication

class MainApplication : ComposeApplication() {
    override val instance = LyricsEditorApplication(this)
}