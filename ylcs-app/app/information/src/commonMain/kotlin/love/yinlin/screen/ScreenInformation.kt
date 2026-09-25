package love.yinlin.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import love.yinlin.common.MessageManager
import love.yinlin.compose.Colors
import love.yinlin.compose.LocalImmersivePadding
import love.yinlin.compose.Theme
import love.yinlin.compose.bold
import love.yinlin.compose.ds.DataSourceInformation
import love.yinlin.compose.screen.BasicScreen
import love.yinlin.compose.ui.container.HorizontalScrollContainer
import love.yinlin.compose.ui.container.Surface
import love.yinlin.compose.ui.floating.SheetContent
import love.yinlin.compose.ui.image.Icon
import love.yinlin.compose.ui.text.SimpleEllipsisText

@Stable
class ScreenInformation : BasicScreen() {
    @Composable
    override fun BasicContent() {
        Column(modifier = Modifier.padding(LocalImmersivePadding.current).fillMaxSize()) {
            val state = rememberLazyListState()

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shadowElevation = Theme.shadow.v7
            ) {
                HorizontalScrollContainer(state = state, modifier = Modifier.fillMaxWidth()) {
                    LazyRow(
                        modifier = Modifier.fillMaxWidth().background(Theme.color.surface),
                        state = state,
                        contentPadding = Theme.padding.value9,
                        horizontalArrangement = Arrangement.spacedBy(Theme.padding.h),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        items(items = DataSourceInformation.managers, key = { it::class }) { manager ->
                            Column(
                                modifier = Modifier.clip(Theme.shape.v7)
                                    .clickable { settingsSheet.open(manager) }
                                    .padding(Theme.padding.eValue),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(Theme.padding.v)
                            ) {
                                Icon(icon = manager.icon, color = Colors.Unspecified, modifier = Modifier.size(Theme.size.image9))
                                SimpleEllipsisText(text = manager.name, style = Theme.typography.v7.bold)
                            }
                        }
                    }
                }
            }
        }
    }

    private val settingsSheet = this land object : SheetContent<MessageManager>() {
        override suspend fun initialize(args: MessageManager) {
            args.onSettingsOpen()
        }

        @Composable
        override fun Content(args: MessageManager) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(Theme.padding.v9)
            ) {
                SimpleEllipsisText(
                    text = "${args.name}设置",
                    style = Theme.typography.v6.bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                with(args) { SettingsLayout() }
            }
        }
    }
}