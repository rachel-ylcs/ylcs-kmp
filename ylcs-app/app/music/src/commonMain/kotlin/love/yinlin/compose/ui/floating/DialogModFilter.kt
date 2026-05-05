package love.yinlin.compose.ui.floating

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import love.yinlin.app
import love.yinlin.compose.Theme
import love.yinlin.compose.extension.rememberFalse
import love.yinlin.compose.ui.animation.ExpandableContent
import love.yinlin.compose.ui.icon.Icons
import love.yinlin.compose.ui.image.Icon
import love.yinlin.compose.ui.input.Filter
import love.yinlin.compose.ui.input.PrimaryTextButton
import love.yinlin.compose.ui.input.Switch
import love.yinlin.compose.ui.input.TextButton
import love.yinlin.compose.ui.node.dashBorder
import love.yinlin.compose.ui.node.fastAnimateRotate
import love.yinlin.compose.ui.text.SimpleEllipsisText
import love.yinlin.coroutines.Coroutines
import love.yinlin.data.mod.ModResourceType
import love.yinlin.data.music.MusicInfo
import love.yinlin.extension.replaceAll

@Stable
internal class DialogModFilter : DialogTemplate<DialogModFilter.ModFilter>() {
    @Stable
    abstract class ModFilter {
        open val isSuspend: Boolean = false

        open fun check(musicInfo: MusicInfo): Boolean = true
        open suspend fun checkSuspend(musicInfo: MusicInfo): Boolean = true
    }

    @Stable
    class MultiModFilter(val filters: List<ModFilter>): ModFilter() {
        override val isSuspend: Boolean = true
        override suspend fun checkSuspend(musicInfo: MusicInfo): Boolean {
            for (filter in filters) {
                if (filter.isSuspend) {
                    if (!filter.checkSuspend(musicInfo)) return false
                }
                else {
                    if (!filter.check(musicInfo)) return false
                }
            }
            return true
        }
    }

    @Stable
    class NameModFilter(val set: Set<String>, val callback: (MusicInfo) -> String) : ModFilter() {
        override fun check(musicInfo: MusicInfo): Boolean = callback(musicInfo) in set
    }

    @Stable
    class MultiNameModFilter(val set: Set<String>, val callback: (MusicInfo) -> String) : ModFilter() {
        override fun check(musicInfo: MusicInfo): Boolean = SplitChain(callback(musicInfo)).any { it in set }
    }

    @Stable
    class ResourceModFilter(val resourceType: ModResourceType): ModFilter() {
        override val isSuspend: Boolean = true
        override suspend fun checkSuspend(musicInfo: MusicInfo): Boolean = musicInfo.path(app.modPath, resourceType).exists()
    }

    @Stable
    data class FilterInfo(val name: String, val num: Int, val selected: Boolean)

    @Stable
    data class FilterResult(
        val albumList: List<FilterInfo>,
        val singerList: List<FilterInfo>,
        val lyricistList: List<FilterInfo>,
        val composerList: List<FilterInfo>,
    )

    companion object {
        val SplitChain = { raw: String ->
            raw.splitToSequence(',').map(String::trim)
        }

        val FilterChain = { raw: MutableMap<String, Int> ->
            raw.asSequence().map { FilterInfo(it.key, it.value, false) }.sortedByDescending(FilterInfo::num).toList()
        }

        val InfoSetChain = { raw: List<FilterInfo> ->
            raw.asSequence().filter { it.selected }.mapTo(mutableSetOf(), FilterInfo::name)
        }
    }

    override val icon: ImageVector = Icons.Filter

    var albumList = mutableStateListOf<FilterInfo>()
    var singerList = mutableStateListOf<FilterInfo>()
    var lyricistList = mutableStateListOf<FilterInfo>()
    var composerList = mutableStateListOf<FilterInfo>()
    var useAnimation = mutableStateOf(false)
    var useVideo = mutableStateOf(false)
    var useRhyme = mutableStateOf(false)
    var useAccompaniment = mutableStateOf(false)

    suspend fun open(library: MutableCollection<MusicInfo>): ModFilter? {
        val result = Coroutines.cpu {
            val tmpAlbum = mutableMapOf<String, Int>()
            val tmpSinger = mutableMapOf<String, Int>()
            val tmpLyricist = mutableMapOf<String, Int>()
            val tempComposer = mutableMapOf<String, Int>()

            for (info in library) {
                Coroutines.catching {
                    // 专辑
                    tmpAlbum[info.album] = (tmpAlbum[info.album] ?: 0) + 1
                    // 歌手
                    for (singer in SplitChain(info.singer)) tmpSinger[singer] = (tmpSinger[singer] ?: 0) + 1
                    // 作词
                    for (lyricist in SplitChain(info.lyricist)) tmpLyricist[lyricist] = (tmpLyricist[lyricist] ?: 0) + 1
                    // 作曲
                    for (composer in SplitChain(info.composer)) tempComposer[composer] = (tempComposer[composer] ?: 0) + 1
                }
            }

            FilterResult(
                albumList = FilterChain(tmpAlbum),
                singerList = FilterChain(tmpSinger),
                lyricistList = FilterChain(tmpLyricist),
                composerList = FilterChain(tempComposer)
            )
        }
        albumList.replaceAll(result.albumList)
        singerList.replaceAll(result.singerList)
        lyricistList.replaceAll(result.lyricistList)
        composerList.replaceAll(result.composerList)
        useAnimation.value = false
        useVideo.value = false
        useRhyme.value = false
        useAccompaniment.value = false

        return awaitResult()
    }

    override val actions: @Composable (RowScope.() -> Unit) = {
        PrimaryTextButton(text = Theme.value.dialogOkText, onClick = {
            val albumSet = InfoSetChain(albumList)
            val singerSet = InfoSetChain(singerList)
            val lyricistSet = InfoSetChain(lyricistList)
            val composerSet = InfoSetChain(composerList)

            val filters = mutableListOf<ModFilter>()
            if (albumSet.isNotEmpty()) filters += NameModFilter(albumSet, MusicInfo::album)
            if (singerSet.isNotEmpty()) filters += MultiNameModFilter(singerSet, MusicInfo::singer)
            if (lyricistSet.isNotEmpty()) filters += MultiNameModFilter(lyricistSet, MusicInfo::lyricist)
            if (composerSet.isNotEmpty()) filters += MultiNameModFilter(composerSet, MusicInfo::composer)
            if (useAnimation.value) filters += ResourceModFilter(ModResourceType.Animation)
            if (useVideo.value) filters += ResourceModFilter(ModResourceType.Video)
            if (useRhyme.value) filters += ResourceModFilter(ModResourceType.Rhyme)
            if (useAccompaniment.value) filters += ResourceModFilter(ModResourceType.Accompaniment)

            future?.send(MultiModFilter(filters))
        })
        TextButton(text = Theme.value.dialogCancelText, onClick = ::close)
    }

    @Composable
    fun FilterLayout(title: String, items: SnapshotStateList<FilterInfo>) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .dashBorder(Theme.border.v7, Theme.color.primary, Theme.shape.v7)
                .padding(Theme.padding.value),
            verticalArrangement = Arrangement.spacedBy(Theme.padding.v)
        ) {
            var expanded by rememberFalse()

            Row(
                modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SimpleEllipsisText(title)
                Icon(
                    icon = Icons.KeyboardArrowRight,
                    modifier = Modifier.fastAnimateRotate(if (expanded) 90f else 0f)
                )
            }

            ExpandableContent(expanded) {
                Filter(
                    size = items.size,
                    selectedProvider = { items[it].selected },
                    titleProvider = {
                        val item = items[it]
                        "${item.name}(${item.num})"
                    },
                    key = { items[it].name },
                    onClick = { index, selected ->
                        items[index] = items[index].copy(selected = selected)
                    },
                    activeIcon = null,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    @Composable
    fun SwitchLayout(title: String, state: MutableState<Boolean>) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(Theme.padding.h),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SimpleEllipsisText(title)
            Switch(checked = state.value, onCheckedChange = { state.value = it })
        }
    }

    @Composable
    override fun Land() {
        LandDialogTemplate("筛选MOD") {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(Theme.padding.v9)
            ) {
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .dashBorder(Theme.border.v7, Theme.color.primary, Theme.shape.v7)
                        .padding(Theme.padding.value),
                    horizontalArrangement = Arrangement.spacedBy(Theme.padding.h9),
                    verticalArrangement = Arrangement.spacedBy(Theme.padding.v9),
                    maxItemsInEachRow = 2
                ) {
                    SwitchLayout("动画", useAnimation)
                    SwitchLayout("视频", useVideo)
                    SwitchLayout("音游", useRhyme)
                    SwitchLayout("伴奏", useAccompaniment)
                }

                FilterLayout("专辑", albumList)
                FilterLayout("歌手", singerList)
                FilterLayout("作词", lyricistList)
                FilterLayout("作曲", composerList)
            }
        }
    }
}