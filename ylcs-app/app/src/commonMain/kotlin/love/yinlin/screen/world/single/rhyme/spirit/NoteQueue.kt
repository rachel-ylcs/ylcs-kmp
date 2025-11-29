package love.yinlin.screen.world.single.rhyme.spirit

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.util.fastForEach
import love.yinlin.compose.game.Drawer
import love.yinlin.compose.game.traits.BoxBody
import love.yinlin.compose.game.traits.Event
import love.yinlin.compose.game.traits.Spirit
import love.yinlin.compose.game.traits.Transform
import love.yinlin.data.music.RhymeAction
import love.yinlin.data.music.RhymeLyricsConfig
import love.yinlin.screen.world.single.rhyme.RhymeManager

@Stable
enum class DynamicActionState { Normal, Miss, Done }

@Stable
sealed class DynamicAction {
    abstract val action: RhymeAction
    abstract val duration: Long
    abstract val appearance: Long

    var state: DynamicActionState by mutableStateOf(DynamicActionState.Normal)



    @Stable
    class Note(start: Long, override val action: RhymeAction.Note) : DynamicAction() {
        companion object {
            private const val DURATION_BASE = 200L
            private const val PERFECT_RATIO = 0.25f
            private const val GOOD_RATIO = 0.5f
            private const val BAD_RATIO = 1f
            private const val MISS_RATIO = 3f
        }

        // 单音符时长与实际字符发音时长无关, 全部为固定值
        override val duration: Long = (DURATION_BASE / Track.TIP_RANGE).toLong()
        override val appearance: Long = start - (duration * Track.TIP_START_RATIO).toLong()
    }

    @Stable
    class FixedSlur(start: Long, end: Long, override val action: RhymeAction.Slur) : DynamicAction() {
        companion object {
            private const val DURATION_BASE_RATIO = 5L
            private const val LENGTH_RATIO = 0.7f
            private const val HEADER_RATIO = 0.1f
        }

        //                                                延音生命周期
        //    出现    ->    加首端    ->    加干路    ->    加尾端    ->    移动    ->    收干路    ->    收尾端    ->    消失
        //                 +2/3d          +4/3d          +2/3d         +4/3d         +2/3d         +1/3d
        //  appearance                                                               start          end
        //                 0%-10%         0%-60%         0%-70%        20%-90%      20%-90%       70%-90%
        override val duration: Long = (end - start) * DURATION_BASE_RATIO
        override val appearance: Long = start - duration * (DURATION_BASE_RATIO - 1) / DURATION_BASE_RATIO
    }

    @Stable
    class OffsetSlur(start: Long, end: Long, override val action: RhymeAction.Slur) : DynamicAction() {
        override val duration: Long = 0L
        override val appearance: Long = 0L
    }
}

@Stable
class NoteQueue(
    rhymeManager: RhymeManager,
    lyricsConfig: RhymeLyricsConfig,
    trackMap: TrackMap,
) : Spirit(rhymeManager), BoxBody {
    override val preTransform: List<Transform> = listOf(Transform.Translate(0f, -1080f * Track.VERTICES_TOP_RATIO))
    override val size: Size = Size(1920f, 1080f * (1 + Track.VERTICES_TOP_RATIO))

    private val lyrics = lyricsConfig.lyrics

    private val vertices = trackMap.vertices
    private val tracks = trackMap.tracks

    private val queue: List<DynamicAction> = buildList {
        lyrics.fastForEach { line ->
            val theme = line.theme
            for (i in theme.indices) {
                val action = theme[i]
                val start = (theme.getOrNull(i - 1)?.end ?: 0) + line.start
                val end = action.end + line.start
                add(when (action) {
                    is RhymeAction.Note -> DynamicAction.Note(start, action) // 单音
                    is RhymeAction.Slur -> {
                        val first = action.scale.firstOrNull()
                        if (action.scale.all { it == first }) DynamicAction.FixedSlur(start, end, action) // 延音
                        else DynamicAction.OffsetSlur(start, end, action) // 连音
                    }
                })
            }
        }
    }

    override fun onClientUpdate(tick: Long) {
        // 先处理音符消失

        // 再处理音符进入
    }

    override fun onClientEvent(event: Event): Boolean {
        return false
    }

    override fun Drawer.onClientDraw() {

    }
}