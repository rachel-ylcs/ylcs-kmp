package love.yinlin.screen.world.single.rhyme.spirit

import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Size
import love.yinlin.compose.game.Drawer
import love.yinlin.compose.game.traits.*
import love.yinlin.data.music.RhymeAction
import love.yinlin.data.music.RhymeLyricsConfig
import love.yinlin.screen.world.single.rhyme.RhymeManager
import love.yinlin.screen.world.single.rhyme.RhymePlayConfig
import love.yinlin.screen.world.single.rhyme.RhymeSound
import love.yinlin.screen.world.single.rhyme.data.ActionCallback
import love.yinlin.screen.world.single.rhyme.data.ActionResult
import love.yinlin.screen.world.single.rhyme.data.DynamicAction
import love.yinlin.screen.world.single.rhyme.data.FixedSlurAction
import love.yinlin.screen.world.single.rhyme.data.NoteAction
import love.yinlin.screen.world.single.rhyme.data.OffsetSlurAction
import love.yinlin.screen.world.single.rhyme.data.Tracks

@Stable
class NoteQueue(
    rhymeManager: RhymeManager,
    playConfig: RhymePlayConfig,
    private val lyricsConfig: RhymeLyricsConfig,
    private val scoreBoard: ScoreBoard,
    private val comboBoard: ComboBoard,
    private val trackMap: TrackMap,
    private val screenEnvironment: ScreenEnvironment,
) : Spirit(rhymeManager), BoxBody {
    override val preTransform: List<Transform> = trackMap.preTransform
    override val size: Size = trackMap.size

    private val audioDelay = rhymeManager.config.audioDelay

    // 预编译队列
    private val queue: List<DynamicAction> = buildList(lyricsConfig.lyrics.size) {
        for (line in lyricsConfig.lyrics) {
            val theme = line.theme
            val lineStart = line.start + lyricsConfig.offset // 偏移补偿
            for (i in theme.indices) {
                val action = theme[i]
                val start = (theme.getOrNull(i - 1)?.end ?: 0) + lineStart
                val end = action.end + lineStart
                val dynamicAction = when (action) {
                    is RhymeAction.Note -> { // 单音
                        NoteAction(rhymeManager.assets, playConfig, start, end, action)
                    }
                    is RhymeAction.Slur -> {
                        // 不同音级但同音高的仍然算做延音
                        val first = DynamicAction.mapTrackIndex(action.scale.first())
                        if (action.scale.all { first == DynamicAction.mapTrackIndex(it) }) { // 延音
                            FixedSlurAction(rhymeManager.assets, playConfig, start, end, action)
                        }
                        else { // 连音
                            OffsetSlurAction(rhymeManager.assets, playConfig, start, end, action)
                        }
                    }
                }
                add(dynamicAction)
            }
        }
    }

    // 入场指针索引
    private var actionIndex by mutableIntStateOf(-1)

    // 遍历场内音符
    private inline fun foreachAction(block: (DynamicAction) -> Boolean) {
        if (actionIndex >= 0) {
            for (index in 0 .. actionIndex) {
                val action = queue[index]
                // 只有未消失的字符才需要更新或渲染
                if (!action.isDismiss && block(action)) break
            }
        }
    }

    // 查找指定音符
    private fun findAction(pointerId: Long): DynamicAction? {
        if (actionIndex >= 0) {
            for (index in 0 .. actionIndex) {
                val action = queue[index]
                if (!action.isDismiss && action.bindId == pointerId) return action
            }
        }
        return null
    }

    // 音符行为回调
    private val callback = object : ActionCallback {
        override fun updateResult(result: ActionResult, scoreRatio: Float) {
            // 更新连击和分数
            val score = comboBoard.updateAction(result, scoreRatio)
            scoreBoard.addScore(score)
            // 更新环境
            if (result == ActionResult.MISS) screenEnvironment.missEnvironment.animation.start()
        }

        override fun playSound(type: RhymeSound) = rhymeManager.playSound(type)
    }

    override fun onClientUpdate(tick: Long) {
        val compensateTick = tick - audioDelay // 延时补偿
        // 处理音符进入 (预编译队列入场顺序严格按照时间顺序)
        queue.getOrNull(actionIndex + 1)?.let { nextAction ->
            if (compensateTick > nextAction.appearance) { // 到达出现时间
                // 入场
                nextAction.onAdmission()
                actionIndex++
            }
        }
        // 更新音符
        foreachAction {
            it.onUpdate(compensateTick, callback)
            false
        }
    }

    override fun onClientEvent(tick: Long, event: Event): Boolean {
        val compensateTick = tick - audioDelay // 延时补偿
        val activeTracks = trackMap.activeTracks
        return when (event) {
            is PointerDownEvent -> {
                // 按下时根据当前位置判定轨道
                val (track, inTracks) = trackMap.calcTrackIndex(event.position)
                if (track != null && inTracks) {
                    val trackIndex = track.index
                    // 防止多指按下统一轨道
                    if (activeTracks[trackIndex] == null) {
                        // 标记该轨道被按下
                        activeTracks[trackIndex] = event.id
                        // 寻找相匹配的音符并处理
                        foreachAction { action ->
                            if (action.bindId != null) false
                            else action.onTrackDown(track, compensateTick, callback).also {
                                if (it) action.bindId = event.id // 绑定指针ID
                            }
                        }
                    }
                }
                // 音符轨道事件区域本身就覆盖全屏, 并且位于事件触发最底层, 后续不需要再处理
                true
            }
            is PointerUpEvent -> {
                // 查找指针原始轨道
                val rawTrackIndex = activeTracks.indexOfFirst { it == event.id }
                if (rawTrackIndex != -1) {
                    val (track, inTracks) = trackMap.calcTrackIndex(event.position)
                    // 此时 event.position 一定位于当前轨道, 因为其他情况被移动排除了
                    require(inTracks && (track == null || track.index == rawTrackIndex))
                    val action = findAction(event.id)
                    if (action != null) {
                        action.onTrackUp(Tracks[rawTrackIndex], compensateTick, callback)
                        // 防止意外被再次唤起按下
                        action.bindId = -1L
                    }
                    // 抬起时回调先于置空
                    activeTracks[rawTrackIndex] = null
                }
                true
            }
            is PointerMoveEvent -> {
                // 查找指针原始轨道
                val rawTrackIndex = activeTracks.indexOfFirst { it == event.id }
                if (rawTrackIndex != -1) {
                    val rawTrack = Tracks[rawTrackIndex]
                    // 移动时根据当前位置判定轨道
                    val (track, inTracks) = trackMap.calcTrackIndex(event.position)
                    val action = findAction(event.id)
                    if (track != null) {
                        val trackIndex = track.index
                        if (rawTrackIndex != trackIndex) { // 轨道发生变化, 但绑定 ID 未发生变化无需处理
                            // 标记原始轨道被抬起
                            if (action != null) {
                                // 变轨
                                if (action.onTrackTransfer(rawTrack, track, compensateTick, callback)) {
                                    // 标记当前轨道被按下
                                    activeTracks[trackIndex] = event.id
                                }
                                else {
                                    // 不支持变轨则等价于被抬起
                                    action.onTrackUp(rawTrack, compensateTick, callback)
                                    action.bindId = -1L
                                }
                            }
                            activeTracks[rawTrackIndex] = null
                        }
                    }
                    else if (!inTracks) { // 指针移出轨道区域则移除原始轨道
                        if (action != null) {
                            action.onTrackUp(rawTrack, compensateTick, callback)
                            action.bindId = -1L
                        }
                        activeTracks[rawTrackIndex] = null
                    }
                }
                true
            }
        }
    }

    override fun Drawer.onClientDraw() {
        foreachAction {
            it.apply { onDraw() }
            false
        }
    }
}