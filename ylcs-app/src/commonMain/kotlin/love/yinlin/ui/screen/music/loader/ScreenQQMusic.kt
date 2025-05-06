package love.yinlin.ui.screen.music.loader

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import kotlinx.serialization.Serializable
import love.yinlin.AppModel
import love.yinlin.api.QQMusic
import love.yinlin.common.Device
import love.yinlin.common.ThemeValue
import love.yinlin.ui.component.screen.ActionScope
import love.yinlin.ui.component.screen.SubScreen
import love.yinlin.ui.component.text.TextInput
import love.yinlin.ui.component.text.TextInputState

@Composable
private fun QQMusicCard(
    qqMusic: QQMusic,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        shadowElevation = ThemeValue.Shadow.Surface
    ) {

    }
}

@Stable
class ScreenQQMusic(model: AppModel, args: Args) : SubScreen<ScreenQQMusic.Args>(model) {
    @Stable
    @Serializable
    data class Args(val deeplink: String?)

    private var linkState = TextInputState(args.deeplink ?: "")
    private var items by mutableStateOf(emptyList<QQMusic>())

    private suspend fun parseLink(link: String) {

    }

    private suspend fun downloadMusic() {

    }

    override val title: String = "QQ音乐"

    @Composable
    override fun ActionScope.LeftActions() {
        if (linkState.text.isNotEmpty() || items.isNotEmpty()) {
            Action(Icons.Outlined.Refresh) {
                linkState.text = ""
                items = emptyList()
            }
        }
    }

    @Composable
    override fun ActionScope.RightActions() {
        ActionSuspend(
            icon = Icons.Outlined.Preview,
            enabled = linkState.text.isNotEmpty()
        ) {
            parseLink(linkState.text)
        }
        ActionSuspend(
            icon = Icons.Outlined.Download,
            enabled = items.isNotEmpty()
        ) {
            downloadMusic()
        }
    }

    @Composable
    override fun SubContent(device: Device) {
        Column(
            modifier = Modifier.fillMaxSize().padding(ThemeValue.Padding.EqualValue),
            verticalArrangement = Arrangement.spacedBy(ThemeValue.Padding.VerticalSpace)
        ) {
            TextInput(
                state = linkState,
                hint = "ID/链接/歌单",
                modifier = Modifier.fillMaxWidth()
            )
            LazyVerticalGrid(
                columns = GridCells.Adaptive(ThemeValue.Size.CardWidth),
                contentPadding = ThemeValue.Padding.EqualValue,
                horizontalArrangement = Arrangement.spacedBy(ThemeValue.Padding.EqualSpace),
                verticalArrangement = Arrangement.spacedBy(ThemeValue.Padding.EqualSpace),
                modifier = Modifier.fillMaxWidth().weight(1f)
            ) {
                items(
                    items = items,
                    key = { it.id }
                ) {
                    QQMusicCard(
                        qqMusic = it,
                        modifier = Modifier.fillMaxWidth().aspectRatio(1f)
                    )
                }
            }
        }
    }
}