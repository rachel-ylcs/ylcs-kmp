package love.yinlin.screen.world.single.rhyme

import androidx.compose.runtime.Stable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import love.yinlin.data.music.RhymeLyricsConfig

// 场景
@Stable
internal class Scene(
    lyrics: RhymeLyricsConfig,
    imageSet: ImageSet
) : RhymeDynamic(), RhymeContainer.Rectangle, RhymeEvent {
    override val position: Offset = Offset.Zero
    override val size: Size = Size.Game

    private val missEnvironment = MissEnvironment()
    private val lyricsBoard = LyricsBoard(lyrics)
    private val scoreBoard = ScoreBoard()
    private val comboBoard = ComboBoard()
    private val noteBoard = NoteBoard(lyrics, imageSet, missEnvironment, scoreBoard, comboBoard)
    private val progressBoard = ProgressBoard(imageSet, lyrics.duration)

    override fun onUpdate(position: Long) {
        lyricsBoard.onUpdate(position)
        scoreBoard.onUpdate(position)
        comboBoard.onUpdate(position)
        noteBoard.onUpdate(position)
        progressBoard.onUpdate(position)
        missEnvironment.onUpdate(position)
    }

    override fun onEvent(pointer: Pointer): Boolean = progressBoard.onEvent(pointer) || noteBoard.onEvent(pointer)

    override fun DrawScope.onDraw(textManager: RhymeTextManager) {
        missEnvironment.run { draw(textManager) }
        lyricsBoard.run { draw(textManager) }
        scoreBoard.run { draw(textManager) }
        comboBoard.run { draw(textManager) }
        noteBoard.run { draw(textManager) }
        progressBoard.run { draw(textManager) }
    }
}