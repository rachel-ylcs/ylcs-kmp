package love.yinlin.compose.ui.floating

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import love.yinlin.compose.Theme
import love.yinlin.compose.ValueTheme
import love.yinlin.compose.bold
import love.yinlin.compose.ui.container.ThemeContainer
import love.yinlin.compose.ui.icon.Icons
import love.yinlin.compose.ui.image.Icon
import love.yinlin.compose.ui.text.SimpleEllipsisText

@Stable
abstract class DialogChoice : Dialog<Int>() {
    open val title: String? = ValueTheme.runtime()

    abstract val num: Int
    @Composable
    abstract fun Name(index: Int)
    @Composable
    abstract fun Icon(index: Int)

    suspend fun open(): Int? = awaitResult()

    @Composable
    final override fun Land() {
        LandDialog {
            Column(verticalArrangement = Arrangement.spacedBy(Theme.padding.v6)) {
                Row(
                    modifier = Modifier.padding(
                        top = Theme.padding.v7,
                        start = Theme.padding.h7,
                        end = Theme.padding.h7
                    ),
                    horizontalArrangement = Arrangement.spacedBy(Theme.padding.h),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ThemeContainer(Theme.color.primary) {
                        Icon(icon = Icons.CheckBox)
                        SimpleEllipsisText(text = title ?: Theme.value.dialogChoiceTitle, style = Theme.typography.v6.bold)
                    }
                }
                LazyColumn(
                    modifier = Modifier.sizeIn(
                        minWidth = Theme.size.cell1,
                        maxWidth = Theme.size.cell1 * 1.75f,
                        minHeight = Theme.size.cell9,
                        maxHeight = Theme.size.cell1 * 2f
                    ).weight(1f, false)
                ) {
                    items(
                        count = num,
                        key = { it }
                    ) { index ->
                        Row(
                            modifier = Modifier.widthIn(min = Theme.size.cell1).clickable {
                                future?.send(index)
                            }.padding(Theme.padding.value),
                            horizontalArrangement = Arrangement.spacedBy(Theme.padding.h),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(index)
                            Name(index)
                        }
                    }
                }
            }
        }
    }

    @Stable
    abstract class ByList : DialogChoice() {
        abstract fun nameFactory(index: Int): String
        abstract fun iconFactory(index: Int): ImageVector

        @Composable
        override fun Name(index: Int) {
            SimpleEllipsisText(text = nameFactory(index))
        }

        @Composable
        override fun Icon(index: Int) {
            Icon(icon = iconFactory(index))
        }
    }

    @Stable
    open class ByDynamicList : ByList() {
        private var items: List<String> = emptyList()
        override val num: Int get() = items.size
        override fun nameFactory(index: Int): String = items[index]
        override fun iconFactory(index: Int): ImageVector = Icons.KeyboardArrowRight

        suspend fun openSuspend(items: List<String>): Int? = if (items.isNotEmpty()) {
            this.items = items
            awaitResult()
        } else null
    }

    companion object {
        @Stable
        fun fromItems(items: List<String>, title: String? = ValueTheme.runtime()): DialogChoice = object : ByList() {
            override val title: String? = title
            override val num: Int = items.size
            override fun nameFactory(index: Int): String = items[index]
            override fun iconFactory(index: Int): ImageVector = Icons.KeyboardArrowRight
        }

        @Stable
        fun fromIconItems(items: List<Pair<String, ImageVector>>, title: String? = ValueTheme.runtime()): DialogChoice = object : ByList() {
            override val title: String? = title
            override val num: Int = items.size
            override fun nameFactory(index: Int): String = items[index].first
            override fun iconFactory(index: Int): ImageVector = items[index].second
        }
    }
}