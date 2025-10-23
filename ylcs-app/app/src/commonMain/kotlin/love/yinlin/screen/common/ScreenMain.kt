package love.yinlin.screen.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.zIndex
import kotlinx.serialization.Serializable
import love.yinlin.compose.CustomTheme
import love.yinlin.compose.Device
import love.yinlin.compose.LocalImmersivePadding
import love.yinlin.compose.rememberImmersivePadding
import love.yinlin.compose.screen.CommonNavigationScreen
import love.yinlin.compose.screen.ScreenManager
import love.yinlin.compose.ui.image.MiniIcon
import love.yinlin.resources.*
import love.yinlin.screen.account.SubScreenMe
import love.yinlin.screen.community.SubScreenDiscovery
import love.yinlin.screen.msg.SubScreenMsg
import love.yinlin.screen.music.SubScreenMusic
import love.yinlin.screen.world.SubScreenWorld
import love.yinlin.ui.component.layout.EqualItem
import love.yinlin.ui.component.layout.EqualRow
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

@Stable
@Serializable
enum class TabItem(
    val title: StringResource,
    val iconNormal: DrawableResource,
    val iconActive: DrawableResource
) {
    MSG(Res.string.home_nav_msg, Res.drawable.tab_msg_normal, Res.drawable.tab_msg_active),
    WORLD(Res.string.home_nav_world, Res.drawable.tab_world_normal, Res.drawable.tab_world_active),
    MUSIC(Res.string.home_nav_music, Res.drawable.tab_music_normal, Res.drawable.tab_music_active),
    DISCOVERY(Res.string.home_nav_discovery, Res.drawable.tab_discovery_normal, Res.drawable.tab_discovery_active),
    ME(Res.string.home_nav_me, Res.drawable.tab_me_normal, Res.drawable.tab_me_active),
}

@Composable
private fun NavigationIcon(
    current: Int,
    index: Int,
    onClick: () -> Unit
) {
    val isSelected = index == current
    val tabItem = TabItem.entries[index]
    Column(
        modifier = Modifier
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick)
            .padding(CustomTheme.padding.value),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(CustomTheme.padding.verticalSpace)
    ) {
        MiniIcon(res = if (isSelected) tabItem.iconActive else tabItem.iconNormal)
        Text(
            text = stringResource(tabItem.title),
            style = MaterialTheme.typography.labelMedium,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun PortraitNavigation(
    currentPage: Int,
    onNavigate: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        tonalElevation = CustomTheme.shadow.tonal,
        shadowElevation = CustomTheme.shadow.surface
    ) {
        EqualRow(modifier = Modifier
            .padding(LocalImmersivePadding.current)
            .fillMaxWidth()
            .padding(CustomTheme.padding.littleSpace)) {
            for (index in TabItem.entries.indices) {
                EqualItem {
                    NavigationIcon(
                        index = index,
                        current = currentPage,
                        onClick = { onNavigate(index) }
                    )
                }
            }
        }
    }
}

@Composable
private fun LandscapeNavigation(
    currentPage: Int,
    onNavigate: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        tonalElevation = CustomTheme.shadow.tonal,
        shadowElevation = CustomTheme.shadow.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(CustomTheme.padding.littleSpace)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            for (index in TabItem.entries.indices) {
                NavigationIcon(
                    index = index,
                    current = currentPage,
                    onClick = { onNavigate(index) }
                )
            }
        }
    }
}

@Stable
class ScreenMain(manager: ScreenManager) : CommonNavigationScreen(manager) {
    override val subs: List<SubScreenInfo> = listOf(
        sub(::SubScreenMsg),
        sub(::SubScreenWorld),
        sub(::SubScreenMusic),
        sub(::SubScreenDiscovery),
        sub(::SubScreenMe),
    )

    @Composable
    private fun Portrait(device: Device, index: Int, content: @Composable (Device) -> Unit) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Bottom
        ) {
            CompositionLocalProvider(LocalImmersivePadding provides LocalImmersivePadding.current.withoutBottom) {
                Box(modifier = Modifier.fillMaxWidth().weight(1f).zIndex(1f)) {
                    content(device)
                }
            }
            CompositionLocalProvider(LocalImmersivePadding provides LocalImmersivePadding.current.withoutTop) {
                PortraitNavigation(
                    modifier = Modifier.fillMaxWidth().zIndex(2f),
                    currentPage = index,
                    onNavigate = { pageIndex = it }
                )
            }
        }
    }

    @Composable
    private fun Landscape(device: Device, index: Int, content: @Composable (Device) -> Unit) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.Start
        ) {
            CompositionLocalProvider(LocalImmersivePadding provides LocalImmersivePadding.current.withoutEnd) {
                LandscapeNavigation(
                    modifier = Modifier.fillMaxHeight().zIndex(2f),
                    currentPage = index,
                    onNavigate = { pageIndex = it }
                )
            }
            CompositionLocalProvider(LocalImmersivePadding provides LocalImmersivePadding.current.withoutStart) {
                Box(modifier = Modifier.weight(1f).fillMaxHeight().zIndex(1f)) {
                    content(device)
                }
            }
        }
    }

    @Composable
    override fun Wrapper(device: Device, index: Int, content: @Composable (Device) -> Unit) {
        val immersivePadding = rememberImmersivePadding()
        CompositionLocalProvider(LocalImmersivePadding provides immersivePadding) {
            when (device.type) {
                Device.Type.PORTRAIT -> Portrait(device, index, content)
                Device.Type.LANDSCAPE, Device.Type.SQUARE -> Landscape(device, index, content)
            }
        }
    }

    override suspend fun initialize() {
        get<SubScreenMe>().updateUserToken()
    }
}