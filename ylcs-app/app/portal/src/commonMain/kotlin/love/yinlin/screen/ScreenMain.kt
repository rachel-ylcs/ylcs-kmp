package love.yinlin.screen

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.util.fastForEachIndexed
import androidx.compose.ui.zIndex
import love.yinlin.app.portal.resources.*
import love.yinlin.common.DataSourceAccount
import love.yinlin.common.DataSourceDiscovery
import love.yinlin.common.DataSourceInformation
import love.yinlin.common.DataSourceWeibo
import love.yinlin.isAppInitialized
import love.yinlin.compose.Device
import love.yinlin.compose.LocalColor
import love.yinlin.compose.LocalDevice
import love.yinlin.compose.LocalImmersivePadding
import love.yinlin.compose.Theme
import love.yinlin.compose.bold
import love.yinlin.compose.screen.DataSource
import love.yinlin.compose.screen.MultiDataSource
import love.yinlin.compose.screen.NavigationScreen
import love.yinlin.compose.ui.container.Surface
import love.yinlin.compose.ui.image.Icon
import love.yinlin.compose.ui.text.SimpleClipText
import love.yinlin.compose.ui.text.TextIconAdapter
import love.yinlin.compose.ui.text.TextIconBinder
import org.jetbrains.compose.resources.DrawableResource

@Stable
class ScreenMain : NavigationScreen(), DataSource by MultiDataSource(
    DataSourceAccount, DataSourceInformation, DataSourceDiscovery, DataSourceWeibo
) {
    @Stable
    private enum class TabItem(
        val title: String,
        val iconNormal: DrawableResource,
        val iconActive: DrawableResource
    ) {
        Information("资讯", Res.drawable.tab_information_normal, Res.drawable.tab_information_active),
        Music("音乐", Res.drawable.tab_world_normal, Res.drawable.tab_world_active),
        Discovery("发现", Res.drawable.tab_music_normal, Res.drawable.tab_music_active),
        World("世界", Res.drawable.tab_discovery_normal, Res.drawable.tab_discovery_active),
        Me("银子", Res.drawable.tab_me_normal, Res.drawable.tab_me_active),
    }

    val information = create(::SubScreenInformation)
    val music = create(::SubScreenMusic)
    val discovery = create(::SubScreenDiscovery)
    val world = create(::SubScreenWorld)
    val me = create(::SubScreenMe)

    private fun navigateSubScreen(index: Int) {
        if (isAppInitialized) pageIndex = index
    }

    override suspend fun initialize() {
        if (!DataSourceAccount.updateUserToken()) navigate(::ScreenLogin)
    }

    @Composable
    private fun Portrait(index: Int, content: @Composable () -> Unit) {
        Column(modifier = Modifier.fillMaxSize()) {
            val immersivePadding = LocalImmersivePadding.current

            Box(modifier = Modifier.fillMaxWidth().weight(1f).zIndex(1f)) {
                CompositionLocalProvider(LocalImmersivePadding provides immersivePadding.withoutBottom) {
                    content()
                }
            }
            Surface(
                modifier = Modifier.fillMaxWidth().zIndex(2f),
                shadowElevation = Theme.shadow.v1,
                tonalLevel = 5
            ) {
                CompositionLocalProvider(LocalImmersivePadding provides immersivePadding.withoutTop) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TabItem.entries.fastForEachIndexed { i, item ->
                            val isCurrent = i == index
                            val primaryColor = Theme.color.primary
                            val indicatorRatio by animateFloatAsState(
                                targetValue = if (isCurrent) 1f else 0f,
                                animationSpec = tween(Theme.animation.duration.v5)
                            )
                            val weightRatio = indicatorRatio / 2 + 1
                            val iconAngle = indicatorRatio * 360f

                            TextIconBinder(
                                modifier = Modifier
                                    .drawBehind {
                                        val (boxWidth, boxHeight) = this.size
                                        val indicatorHeight = boxHeight * 0.05f
                                        val startRatio = (1 - indicatorRatio) / 2
                                        drawRoundRect(
                                            color = primaryColor,
                                            topLeft = Offset(startRatio * boxWidth, 0f),
                                            size = Size(indicatorRatio * boxWidth, indicatorHeight),
                                            cornerRadius = CornerRadius(indicatorHeight)
                                        )
                                    }
                                    .clickable { navigateSubScreen(i) }
                                    .weight(weightRatio)
                                    .padding(vertical = Theme.padding.v),
                                gapRatio = 0.25f
                            ) { idIcon, idText ->
                                Icon(
                                    icon = if (isCurrent) item.iconActive else item.iconNormal,
                                    modifier = Modifier.rotate(iconAngle).idIcon()
                                )
                                SimpleClipText(
                                    text = item.title,
                                    color = if (isCurrent) primaryColor else LocalColor.current,
                                    style = Theme.typography.v7.bold,
                                    modifier = Modifier.idText()
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun Landscape(index: Int, content: @Composable () -> Unit) {
        Row(modifier = Modifier.fillMaxSize()) {
            val immersivePadding = LocalImmersivePadding.current

            Surface(
                modifier = Modifier.fillMaxHeight().zIndex(2f),
                shadowElevation = Theme.shadow.v1,
                tonalLevel = 5
            ) {
                CompositionLocalProvider(LocalImmersivePadding provides immersivePadding.withoutEnd) {
                    Column(modifier = Modifier.padding(LocalImmersivePadding.current).fillMaxHeight().verticalScroll(rememberScrollState())) {
                        TabItem.entries.fastForEachIndexed { i, item ->
                            val isCurrent = i == index
                            val primaryColor = Theme.color.primary
                            val indicatorRatio by animateFloatAsState(
                                targetValue = if (isCurrent) 0.5f else 0f,
                                animationSpec = tween(Theme.animation.duration.v5)
                            )
                            val iconAngle = indicatorRatio * 720f

                            TextIconAdapter(
                                modifier = Modifier.drawBehind {
                                    val (boxWidth, boxHeight) = this.size
                                    val indicatorWidth = boxWidth * 0.03f
                                    drawRoundRect(
                                        color = primaryColor,
                                        topLeft = Offset(indicatorWidth, (1 - indicatorRatio) / 2 * boxHeight),
                                        size = Size(indicatorWidth, indicatorRatio * boxHeight),
                                        cornerRadius = CornerRadius(indicatorWidth)
                                    )
                                }.clickable {
                                    navigateSubScreen(i)
                                }.padding(Theme.padding.value8),
                                gapRatio = 1f
                            ) { idIcon, idText ->
                                Icon(
                                    icon = if (isCurrent) item.iconActive else item.iconNormal,
                                    modifier = Modifier.rotate(iconAngle).idIcon()
                                )
                                SimpleClipText(
                                    text = item.title,
                                    color = if (isCurrent) primaryColor else LocalColor.current,
                                    style = Theme.typography.v6.bold,
                                    modifier = Modifier.idText()
                                )
                            }
                        }
                        Box(modifier = Modifier.weight(1f))
                    }
                }
            }
            Box(modifier = Modifier.weight(1f).fillMaxHeight().zIndex(1f)) {
                CompositionLocalProvider(LocalImmersivePadding provides immersivePadding.withoutStart) {
                    content()
                }
            }
        }
    }

    @Composable
    override fun DecorateContent(index: Int, content: @Composable () -> Unit) {
        when (LocalDevice.current.type) {
            Device.Type.PORTRAIT -> Portrait(index, content)
            Device.Type.LANDSCAPE, Device.Type.SQUARE -> Landscape(index, content)
        }
    }
}