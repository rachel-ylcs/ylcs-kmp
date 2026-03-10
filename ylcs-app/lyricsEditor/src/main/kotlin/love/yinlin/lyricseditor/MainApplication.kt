package love.yinlin.lyricseditor

import love.yinlin.compose.ComposeApplication

class MainApplication : ComposeApplication() {
    override fun buildInstance() = LyricsEditorApplication(this)
}