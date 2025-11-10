package love.yinlin.compose.screen

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.zIndex
import love.yinlin.compose.*
import love.yinlin.compose.ui.floating.Floating
import love.yinlin.compose.ui.image.ClickIcon
import love.yinlin.compose.ui.layout.ActionScope
import love.yinlin.compose.ui.layout.NavigationBack
import love.yinlin.compose.ui.layout.SplitActionLayout

@Stable
abstract class Screen(manager: ScreenManager) : BasicScreen(manager) {
    @Composable
    protected abstract fun Content(device: Device)

    protected open val title: String? = null

    protected open fun onBack() = pop()

    @Composable
    protected open fun ActionScope.LeftActions() { }

    @Composable
    protected open fun ActionScope.RightActions() { }

    @Composable
    protected open fun ColumnScope.SecondTitleBar() { }

    @Composable
    protected open fun BottomBar() { }

    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    final override fun BasicContent() {
        NavigationBack(onBack = ::onBack)

        val immersivePadding = rememberImmersivePadding()
        Column(modifier = Modifier.fillMaxSize()) {
            title?.let { titleString ->
                Surface(
                    modifier = Modifier.fillMaxWidth().zIndex(Floating.Z_INDEX_COMMON),
                    tonalElevation = CustomTheme.shadow.tonal,
                    shadowElevation = CustomTheme.shadow.surface
                ) {
                    Column(modifier = Modifier.padding(immersivePadding.withoutBottom).fillMaxWidth()) {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(vertical = CustomTheme.padding.verticalSpace),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier.fillMaxWidth().zIndex(2f),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = titleString,
                                    style = MaterialTheme.typography.titleMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            SplitActionLayout(
                                modifier = Modifier.fillMaxWidth().zIndex(1f),
                                left = {
                                    ClickIcon(
                                        modifier = Modifier.padding(start = CustomTheme.padding.horizontalSpace),
                                        icon = Icons.AutoMirrored.Outlined.ArrowBack,
                                        tip = "返回",
                                        onClick = ::onBack
                                    )
                                    LeftActions()
                                },
                                right = {
                                    RightActions()
                                }
                            )
                        }
                        SecondTitleBar()
                    }
                }
            }
            Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                CompositionLocalProvider(
                    LocalImmersivePadding provides (if (title == null) immersivePadding else immersivePadding.withoutTop)
                ) {
                    Content(LocalDevice.current)
                }
            }
            Surface(
                modifier = Modifier.fillMaxWidth().zIndex(Floating.Z_INDEX_COMMON),
                tonalElevation = CustomTheme.shadow.tonal,
                shadowElevation = CustomTheme.shadow.surface
            ) {
                CompositionLocalProvider(
                    LocalImmersivePadding provides (if (title == null) immersivePadding else immersivePadding.withoutTop)
                ) {
                    BottomBar()
                }
            }
        }
    }
}