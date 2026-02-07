package love.yinlin.compose.screen

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Stable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
import love.yinlin.compose.LocalImmersivePadding
import love.yinlin.compose.Theme
import love.yinlin.compose.bold
import love.yinlin.compose.rememberImmersivePadding
import love.yinlin.compose.ui.container.ActionScope
import love.yinlin.compose.ui.container.Surface
import love.yinlin.compose.ui.floating.Floating
import love.yinlin.compose.ui.icon.Icons
import love.yinlin.compose.ui.image.Icon
import love.yinlin.compose.ui.text.SimpleEllipsisText

@Stable
abstract class Screen : BasicScreen() {
    protected open val title: String? = null

    @Composable
    protected open fun RowScope.LeftActions() { }

    @Composable
    protected open fun RowScope.RightActions() { }

    @Composable
    protected open fun ColumnScope.SecondTitleBar() { }

    @Composable
    protected open fun BottomBar() { }

    @Composable
    protected abstract fun Content()

    @Composable
    final override fun BasicContent() {
        val immersivePadding = rememberImmersivePadding()

        Column(modifier = Modifier.fillMaxSize()) {
            title?.let { titleString ->
                Surface(
                    modifier = Modifier.fillMaxWidth().zIndex(Floating.Z_INDEX_COMMON),
                    contentPadding = immersivePadding.withoutBottom,
                    tonalLevel = 5,
                    shadowElevation = Theme.shadow.v2
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(Theme.padding.value9),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier.fillMaxWidth().zIndex(2f),
                                contentAlignment = Alignment.Center
                            ) {
                                SimpleEllipsisText(text = titleString, style = Theme.typography.v6.bold)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth().zIndex(1f),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                ActionScope.Left.Container {
                                    Icon(icon = Icons.ArrowBack, tip = Theme.value.backText, onClick = ::onBack)
                                    LeftActions()
                                }
                                ActionScope.Right.Container {
                                    RightActions()
                                }
                            }
                        }
                        SecondTitleBar()
                    }
                }
            }
            Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                CompositionLocalProvider(
                    LocalImmersivePadding provides (if (title == null) immersivePadding else immersivePadding.withoutTop),
                    content = ::Content
                )
            }
            Surface(
                modifier = Modifier.fillMaxWidth().zIndex(Floating.Z_INDEX_COMMON),
                tonalLevel = 5,
                shadowElevation = Theme.shadow.v2
            ) {
                CompositionLocalProvider(
                    LocalImmersivePadding provides (if (title == null) immersivePadding else immersivePadding.withoutTop),
                    content = ::BottomBar
                )
            }
        }
    }
}