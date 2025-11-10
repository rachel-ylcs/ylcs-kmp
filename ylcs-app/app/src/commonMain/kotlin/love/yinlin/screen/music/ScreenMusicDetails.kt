package love.yinlin.screen.music

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import love.yinlin.api.APIConfig
import love.yinlin.app
import love.yinlin.common.ExtraIcons
import love.yinlin.common.Paths
import love.yinlin.compose.CustomTheme
import love.yinlin.compose.Device
import love.yinlin.compose.LocalImmersivePadding
import love.yinlin.compose.rememberDerivedState
import love.yinlin.compose.screen.Screen
import love.yinlin.compose.screen.ScreenManager
import love.yinlin.compose.ui.image.PauseLoading
import love.yinlin.compose.ui.image.WebImage
import love.yinlin.compose.ui.input.NormalText
import love.yinlin.compose.ui.layout.ActionScope
import love.yinlin.compose.ui.layout.Pagination
import love.yinlin.compose.ui.layout.PaginationColumn
import love.yinlin.data.mod.ModResourceType
import love.yinlin.data.rachel.song.Song
import love.yinlin.data.rachel.song.SongComment
import love.yinlin.extension.exists
import love.yinlin.screen.community.UserBar

@Stable
class ScreenMusicDetails(manager: ScreenManager, private val id: String) : Screen(manager) {
    private var clientSong: Song? by mutableStateOf(updateClientSong())
    private var remoteSong: Song? by mutableStateOf(null)

    private val pageComments = object : Pagination<SongComment, Long, Long>(
        default = 0L,
        pageNum = APIConfig.MIN_PAGE_NUM
    ) {
        override fun distinctValue(item: SongComment): Long = item.cid
        override fun offset(item: SongComment): Long = item.cid
    }

    private val listState = LazyListState()

    override val title: String by derivedStateOf { clientSong?.name ?: remoteSong?.name ?: "未知歌曲" }

    private fun updateClientSong(): Song? = app.mp.library[id]?.let { musicInfo ->
        Song(
            id = musicInfo.id,
            version = musicInfo.version,
            name = musicInfo.name,
            singer = musicInfo.singer,
            lyricist = musicInfo.lyricist,
            composer = musicInfo.composer,
            album = musicInfo.album,
            animation = musicInfo.path(Paths.modPath, ModResourceType.Animation).exists,
            video = musicInfo.path(Paths.modPath, ModResourceType.Video).exists,
            rhyme = musicInfo.path(Paths.modPath, ModResourceType.Rhyme).exists
        )
    }

    private suspend fun requestNewSongComments() {

    }

    private suspend fun requestMoreSongComments() {

    }

    override suspend fun initialize() {

    }

    @Composable
    private fun DetailsLayout(modifier: Modifier = Modifier) {
        Surface(
            modifier = modifier,
            shadowElevation = CustomTheme.shadow.surface
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(CustomTheme.padding.equalValue),
                verticalArrangement = Arrangement.spacedBy(CustomTheme.padding.verticalSpace)
            ) {
                val song by rememberDerivedState { clientSong ?: remoteSong }

                
            }
        }
    }

    @Composable
    private fun ResourceLayout(modifier: Modifier = Modifier) {

    }

    @Composable
    override fun ActionScope.RightActions() {

    }

    @Composable
    override fun BottomBar() {
        if (app.config.userProfile != null) {

        }
    }

    @Composable
    private fun SongCommentLayout(
        comment: SongComment,
        modifier: Modifier = Modifier
    ) {
        Column(
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy(CustomTheme.padding.verticalSpace)
        ) {
            UserBar(
                avatar = comment.avatarPath,
                name = comment.name,
                time = comment.ts,
                label = comment.label,
                level = comment.level,
                onAvatarClick = {

                }
            )
            SelectionContainer {
                Text(
                    text = comment.content,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = CustomTheme.padding.verticalSpace),
                horizontalArrangement = Arrangement.spacedBy(CustomTheme.padding.horizontalSpace, Alignment.End),
                verticalAlignment = Alignment.CenterVertically
            ) {
                app.config.userProfile?.let { user ->
                    if (user.uid == comment.uid || user.hasPrivilegeVIPTopic) {
                        Text(
                            text = "删除",
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.clickable {

                            }.padding(CustomTheme.padding.littleValue)
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun Portrait() {
        PauseLoading(listState)

        PaginationColumn(
            items = pageComments.items,
            key = { it.cid },
            state = listState,
            canRefresh = false,
            canLoading = pageComments.canLoading,
            onLoading = { requestMoreSongComments() },
            itemDivider = PaddingValues(vertical = CustomTheme.padding.verticalSpace),
            modifier = Modifier.padding(LocalImmersivePadding.current).fillMaxSize(),
            header = {
                DetailsLayout(modifier = Modifier.fillMaxWidth())
                ResourceLayout(modifier = Modifier.fillMaxWidth())
            }
        ) {
            SongCommentLayout(
                comment = it,
                modifier = Modifier.fillMaxWidth().padding(horizontal = CustomTheme.padding.horizontalSpace)
            )
        }
    }

    @Composable
    private fun Landscape() {
        Row(modifier = Modifier.fillMaxSize()) {
            val immersivePadding = LocalImmersivePadding.current

            DetailsLayout(
                modifier = Modifier
                    .padding(immersivePadding.withoutEnd)
                    .weight(1f)
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState())
            )
            VerticalDivider()
            ResourceLayout(
                modifier = Modifier
                    .padding(immersivePadding.withoutHorizontal)
                    .weight(1f)
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState())
            )
            VerticalDivider()
            PaginationColumn(
                items = pageComments.items,
                key = { it.cid },
                state = listState,
                canRefresh = false,
                canLoading = pageComments.canLoading,
                onLoading = { requestMoreSongComments() },
                itemDivider = PaddingValues(vertical = CustomTheme.padding.verticalSpace),
                modifier = Modifier
                    .padding(immersivePadding.withoutStart)
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(CustomTheme.padding.value)
            ) {
                SongCommentLayout(
                    comment = it,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    @Composable
    override fun Content(device: Device) {
        when (device.type) {
            Device.Type.PORTRAIT, Device.Type.SQUARE -> Portrait()
            Device.Type.LANDSCAPE -> Landscape()
        }
    }
}