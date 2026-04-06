package love.yinlin.screen

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.util.fastForEachIndexed
import androidx.compose.ui.zIndex
import love.yinlin.app
import love.yinlin.app.portal.resources.*
import love.yinlin.common.*
import love.yinlin.isAppInitialized
import love.yinlin.compose.*
import love.yinlin.compose.extension.movableComposable
import love.yinlin.compose.screen.DataSource
import love.yinlin.compose.screen.MultiDataSource
import love.yinlin.compose.screen.NavigationScreen
import love.yinlin.compose.ui.container.Surface
import love.yinlin.compose.ui.icon.Icons
import love.yinlin.compose.ui.image.Icon
import love.yinlin.compose.ui.node.fastRotate
import love.yinlin.compose.ui.text.SimpleClipText
import love.yinlin.compose.ui.text.SimpleEllipsisText
import love.yinlin.compose.ui.text.TextIconAdapter
import love.yinlin.compose.ui.text.TextIconBinder
import org.jetbrains.compose.resources.DrawableResource

@Stable
class ScreenMain : NavigationScreen(), DataSource by MultiDataSource(
    DataSourceAccount, DataSourceInformation, DataSourceDiscovery, DataSourceWeibo
) {
    @Stable
    private enum class TabItem(val title: String, val iconNormal: DrawableResource, val iconActive: DrawableResource) {
        Information("资讯", Res.drawable.tab_information_normal, Res.drawable.tab_information_active),
        Music("音乐", Res.drawable.tab_world_normal, Res.drawable.tab_world_active),
        Discovery("发现", Res.drawable.tab_music_normal, Res.drawable.tab_music_active),
        World("世界", Res.drawable.tab_discovery_normal, Res.drawable.tab_discovery_active),
        Me("银子", Res.drawable.tab_me_normal, Res.drawable.tab_me_active),
    }

    init {
        createSubScreen(::SubScreenInformation, ::SubScreenMusic, ::SubScreenDiscovery, ::SubScreenWorld, ::SubScreenMe)
    }

    private fun navigateSubScreen(index: Int) {
        if (isAppInitialized) pageIndex = index
    }

    override fun onBack() {
        if (isAppInitialized) app.backHome()
    }

    override suspend fun initialize() {
        if (!DataSourceAccount.updateUserToken()) navigate(::ScreenLogin)
    }

    @Composable
    private fun PortraitNavigator(index: Int, padding: PaddingValues, modifier: Modifier = Modifier) {
        Surface(
            modifier = modifier,
            contentPadding = padding,
            shadowElevation = Theme.shadow.v1,
            tonalLevel = 5
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TabItem.entries.fastForEachIndexed { i, item ->
                    val isCurrent = i == index
                    val primaryColor = Theme.color.primary
                    val indicatorAngle = animateFloatAsState(
                        targetValue = if (isCurrent) 360f else 0f,
                        animationSpec = tween(Theme.animation.duration.v5)
                    )

                    TextIconBinder(
                        modifier = Modifier
                            .drawBehind {
                                val (boxWidth, boxHeight) = this.size
                                val indicatorHeight = boxHeight * 0.05f
                                val indicatorRatio = indicatorAngle.value / 360
                                val startRatio = (1 - indicatorRatio) / 2
                                drawRoundRect(
                                    color = primaryColor,
                                    topLeft = Offset(startRatio * boxWidth, 0f),
                                    size = Size(indicatorRatio * boxWidth, indicatorHeight),
                                    cornerRadius = CornerRadius(indicatorHeight)
                                )
                            }
                            .clickable { navigateSubScreen(i) }
                            .weight(indicatorAngle.value / 720 + 1)
                            .padding(vertical = Theme.padding.v),
                        gapRatio = 0.25f
                    ) { idIcon, idText ->
                        Icon(
                            icon = if (isCurrent) item.iconActive else item.iconNormal,
                            modifier = Modifier.fastRotate(indicatorAngle).idIcon()
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

    @Composable
    private fun LandscapeNavigator(index: Int, padding: PaddingValues, modifier: Modifier = Modifier) {
        Surface(
            modifier = modifier,
            contentPadding = padding,
            shadowElevation = Theme.shadow.v1,
            tonalLevel = 5
        ) {
            Column(modifier = Modifier.width(Theme.size.cell4).fillMaxHeight().verticalScroll(rememberScrollState())) {
                TabItem.entries.fastForEachIndexed { i, item ->
                    val isCurrent = i == index
                    val primaryColor = Theme.color.primary
                    val indicatorAngle = animateFloatAsState(
                        targetValue = if (isCurrent) 360f else 0f,
                        animationSpec = tween(Theme.animation.duration.v5)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth().clickable {
                            navigateSubScreen(i)
                        }.drawBehind {
                            val (boxWidth, boxHeight) = this.size
                            val indicatorRatio = indicatorAngle.value / 720
                            val indicatorWidth = boxWidth * 0.03f
                            drawRoundRect(
                                color = primaryColor,
                                topLeft = Offset(indicatorWidth, (1 - indicatorRatio) / 2 * boxHeight),
                                size = Size(indicatorWidth, indicatorRatio * boxHeight),
                                cornerRadius = CornerRadius(indicatorWidth)
                            )
                        }.padding(Theme.padding.value9),
                        horizontalArrangement = Arrangement.spacedBy(Theme.padding.h9),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            icon = if (isCurrent) item.iconActive else item.iconNormal,
                            modifier = Modifier.size(Theme.size.smallIcon).fastRotate(indicatorAngle)
                        )
                        SimpleClipText(
                            text = item.title,
                            color = if (isCurrent) primaryColor else LocalColor.current,
                            style = Theme.typography.v7.bold,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                Box(modifier = Modifier.weight(1f))
                Row(
                    modifier = Modifier.fillMaxWidth().clickable {
                        navigate(::ScreenSettings)
                    }.padding(Theme.padding.value9),
                    horizontalArrangement = Arrangement.spacedBy(Theme.padding.h9),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(icon = Icons.Settings, modifier = Modifier.size(Theme.size.smallIcon))
                    SimpleClipText(text = "设置", style = Theme.typography.v7.bold, modifier = Modifier.weight(1f))
                }
            }
        }
    }

    private val currentContent = movableComposable { padding: ImmersivePadding, modifier: Modifier, content: @Composable () -> Unit ->
        Box(modifier = modifier) {
            CompositionLocalProvider(LocalImmersivePadding provides padding, content = content)
        }
    }

    @Composable
    private fun Portrait(index: Int, content: @Composable () -> Unit) {
        Column(modifier = Modifier.fillMaxSize()) {
            val immersivePadding = LocalImmersivePadding.current

            currentContent(immersivePadding.withoutBottom, Modifier.fillMaxWidth().weight(1f).zIndex(1f), content)
            PortraitNavigator(index, immersivePadding.withoutTop, Modifier.fillMaxWidth().zIndex(2f))
        }
    }

    @Composable
    private fun Landscape(index: Int, content: @Composable () -> Unit) {
        Row(modifier = Modifier.fillMaxSize()) {
            val immersivePadding = LocalImmersivePadding.current

            LandscapeNavigator(index, immersivePadding.withoutEnd, Modifier.fillMaxHeight().zIndex(2f))
            currentContent(immersivePadding.withoutStart, Modifier.weight(1f).fillMaxHeight().zIndex(1f), content)
        }
    }

    @Composable
    override fun DecorateContent(index: Int, content: @Composable () -> Unit) {
        val deviceType by rememberDeviceType()
        when (deviceType) {
            Device.Type.PORTRAIT -> Portrait(index, content)
            Device.Type.LANDSCAPE, Device.Type.SQUARE -> Landscape(index, content)
        }
    }
}