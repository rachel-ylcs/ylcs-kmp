package love.yinlin.screen.world.single.rhyme

import androidx.compose.runtime.Stable

@Stable
enum class RhymeSound(val title: String, val version: Int? = null) {
    NoteClick("soundNoteClick");
}