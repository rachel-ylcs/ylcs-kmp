package love.yinlin.media

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import love.yinlin.compose.data.media.MediaPlayMode
import love.yinlin.extension.moveItem
import love.yinlin.extension.replaceAll

@Stable
abstract class CommonMusicPlayer(fetcher: MediaMetadataFetcher) : MusicPlayer(fetcher) {
    private var shuffledList = ShuffledOrder()
    protected var currentIndex: Int by mutableIntStateOf(-1)

    abstract fun innerStop()
    abstract fun innerGotoIndex(path: String, playing: Boolean = true): Boolean

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

    protected fun internalStop() {
        musicList.clear()
        duration = 0L
        currentId = null
        currentIndex = -1
        resetShuffled()
        innerStop()
        listener?.onPlayerStop()
    }

    protected fun internalGotoIndex(index: Int, playing: Boolean = true) {
        var success = false
        if (index in musicList.indices) {
            val path = fetcher.extractAudioUri(musicList[index])
            if (path != null) {
                currentIndex = index
                success = innerGotoIndex(path, playing)
            }
        }
        if (!success) internalStop()
    }

    final override suspend fun stop() { if (isReady) internalStop() }

    final override suspend fun gotoPrevious() {
        if (isReady) internalGotoIndex(if (playMode == MediaPlayMode.Random) randomPreviousIndex ?: reshuffled() else loopPreviousIndex)
    }

    final override suspend fun gotoNext() {
        if (isReady) internalGotoIndex(if (playMode == MediaPlayMode.Random) randomNextIndex ?: reshuffled() else loopNextIndex)
    }

    final override suspend fun gotoIndex(index: Int) {
        if (isReady) internalGotoIndex(index)
    }

    final override suspend fun prepareMedias(medias: List<String>, startIndex: Int?, playing: Boolean) {
        val index = startIndex ?: 0
        if (index >= 0 && index < medias.size) {
            musicList.replaceAll(medias)
            reshuffled(size = medias.size, start = index)
            internalGotoIndex(index, playing)
        }
    }

    final override suspend fun updateNewMedias(medias: List<String>) {
        val currentPlayingId = musicList.getOrNull(currentIndex) ?: return
        val newIndex = medias.indexOf(currentPlayingId)
        // 自己维护的列表可以直接替换
        musicList.replaceAll(medias)
        // 更新当前媒体索引
        currentIndex = newIndex
        // 重置随机索引列表
        reshuffled(size = medias.size, start = newIndex)
    }

    final override suspend fun removeMedia(index: Int) {
        if (isReady) {
            val size = musicList.size
            if (index in musicList.indices) {
                if (size == 1) internalStop()
                else {
                    musicList.removeAt(index)
                    if (playMode == MediaPlayMode.Random) {
                        if (currentIndex == index) internalGotoIndex(reshuffled(size = size - 1))
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
                        if (currentIndex == index) internalGotoIndex(if (index == size - 1) index - 1 else index)
                        reshuffled(size = size - 1)
                    }
                }
            }
        }
    }

    final override suspend fun moveMedia(fromIndex: Int, toIndex: Int) {
        if (isReady) {
            // 移动媒体只需要移动媒体列表后并更新当前索引即可
            musicList.moveItem(fromIndex, toIndex)
            currentIndex = toIndex
            // 如果是随机播放模式还需要调整索引表
            if (playMode == MediaPlayMode.Random) {
                // 确定移动的两个媒体在随机索引表的位置
                val indices = shuffledList.indices
                val fromRandomIndex = indices.indexOf(fromIndex)
                val toRandomIndex = indices.indexOf(toIndex)
                // 交换两个媒体在索引表中的位置
                val newIndices = indices.toMutableList()
                val tmp = newIndices[fromRandomIndex]
                newIndices[fromRandomIndex] = newIndices[toRandomIndex]
                newIndices[toRandomIndex] = tmp
                // 检查当前移走的媒体是否是随机播放的起点并更新
                val start = if (shuffledList.begin == fromIndex) toIndex else null
                shuffledList.internalSet(newIndices, start)
            }
        }
    }
}