package love.yinlin.media

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import love.yinlin.compose.data.media.MediaInfo
import love.yinlin.compose.data.media.MediaPlayMode
import love.yinlin.extension.replaceAll
import kotlin.random.Random

@Stable
abstract class CommonMusicPlayer<Info : MediaInfo>(fetcher: MediaMetadataFetcher<Info>) : MusicPlayer<Info>(fetcher) {
    private var shuffledList = ShuffledOrder()
    protected var currentIndex: Int by mutableIntStateOf(-1)

    abstract fun innerStop()
    abstract fun innerGotoIndex(index: Int, playing: Boolean = true)

    protected val loopPreviousIndex: Int get() = (currentIndex + musicList.size - 1) % musicList.size

    protected val loopNextIndex: Int get() = (currentIndex + 1) % musicList.size

    protected val randomPreviousIndex: Int? get() {
        val indices = shuffledList.indices
        val indexInShuffled = indices.indexOf(currentIndex)
        return if (indexInShuffled != -1 && indices.size == musicList.size) {
            val actualIndex = indices[(indexInShuffled + indices.size - 1) % indices.size]
            // 播放一轮结束, 重新洗牌
            if (actualIndex == shuffledList.begin) null else actualIndex
        }
        else -1
    }

    protected val randomNextIndex: Int? get() {
        val indices = shuffledList.indices
        val indexInShuffled = indices.indexOf(currentIndex)
        return if (indexInShuffled != -1 && indices.size == musicList.size) {
            val actualIndex = indices[(indexInShuffled + 1) % indices.size]
            // 播放一轮结束, 重新洗牌
            if (actualIndex == shuffledList.begin) null else actualIndex
        } else -1
    }

    protected fun reshuffled(size: Int? = null, start: Int? = null): Int {
        shuffledList = ShuffledOrder(size = size ?: shuffledList.indices.size, start = start)
        return shuffledList.begin
    }

    protected fun resetShuffled() {
        shuffledList = ShuffledOrder()
    }

    final override suspend fun updatePlayMode(mode: MediaPlayMode) {
        playMode = mode
        listener?.onPlayModeChanged(mode)
        // 重新换模式要重设洗牌顺序
        if (mode == MediaPlayMode.Random) reshuffled(start = currentIndex)
    }

    final override suspend fun stop() { if (isReady) innerStop() }

    final override suspend fun gotoPrevious() {
        if (isReady) innerGotoIndex(if (playMode == MediaPlayMode.Random) randomPreviousIndex ?: reshuffled() else loopPreviousIndex)
    }

    final override suspend fun gotoNext() {
        if (isReady) innerGotoIndex(if (playMode == MediaPlayMode.Random) randomNextIndex ?: reshuffled() else loopNextIndex)
    }

    final override suspend fun gotoIndex(index: Int) {
        if (isReady) innerGotoIndex(index)
    }

    final override suspend fun prepareMedias(medias: List<Info>, startIndex: Int?, playing: Boolean) {
        val index = startIndex ?: 0
        if (index >= 0 && index < medias.size) {
            musicList.replaceAll(medias)
            reshuffled(size = medias.size, start = index)
            innerGotoIndex(index, playing)
        }
    }

    final override suspend fun addMedias(medias: List<Info>) {
        if (isReady) {
            musicList.addAll(medias)

            /*
            * 将当前播放位置随机序前面的索引接到随机序末尾
            * 保持第一个元素是始终是当前播放索引
            * 然后将新导入的媒体随机插入其中
            * 得到新的随机序索引表
            *  */
            val indices = shuffledList.indices
            if (playMode == MediaPlayMode.Random) {
                val indexInShuffled = indices.indexOf(currentIndex)
                val rotated = indices.subList(indexInShuffled, indices.size) + indices.subList(0, indexInShuffled)
                val head = rotated.first()
                val rest = rotated.drop(1).toMutableList()
                repeat(medias.size) {
                    // 生成0到当前rest长度的随机位置
                    val position = Random.nextInt(rest.size + 1)
                    rest.add(position, indices.size + it)
                }
                shuffledList.internalSet(listOf(head) + rest)
            }
            else reshuffled(size = indices.size + medias.size)
        }
    }

    final override suspend fun removeMedia(index: Int) {
        if (isReady) {
            val size = musicList.size
            if (index in musicList.indices) {
                if (size == 1) innerStop()
                else {
                    musicList.removeAt(index)
                    if (playMode == MediaPlayMode.Random) {
                        if (currentIndex == index) innerGotoIndex(reshuffled(size = size - 1))
                        else {
                            // 如果删除了随机序的起点则重新令当前播放的为起点
                            val rest = shuffledList.indices.toMutableList()
                            rest.removeAll { it == index }
                            // 随机序中超过被删除索引的索引都要自减1
                            for ((i, item) in rest.withIndex()) {
                                if (item > index) rest[i] -= 1
                            }
                            shuffledList.internalSet(rest, if (shuffledList.begin == index) currentIndex else null)
                        }
                    }
                    else {
                        if (currentIndex == index) innerGotoIndex(if (index == size - 1) index - 1 else index)
                        reshuffled(size = size - 1)
                    }
                }
            }
        }
    }
}