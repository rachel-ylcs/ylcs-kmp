package love.yinlin.compose.ui.floating

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import love.yinlin.compose.Theme
import love.yinlin.compose.ui.icon.Icons
import love.yinlin.compose.ui.image.Icon
import love.yinlin.compose.ui.text.SimpleEllipsisText

@Stable
abstract class DialogChoice : DialogTemplate<Int>() {
    override val scrollable: Boolean = false
    override val showTitle: Boolean = false

    override val contentPadding: PaddingValues @Composable get() = PaddingValues.Zero

    abstract val num: Int
    @Composable
    abstract fun Name(index: Int)
    @Composable
    abstract fun Icon(index: Int)

    suspend fun open(): Int? = awaitResult()

    @Composable
    final override fun Land() {
        LandDialogTemplate("") {
            LazyColumn(modifier = Modifier.widthIn(min = minContentWidth)) {
                items(num) { index ->
                    Row(
                        modifier = Modifier.widthIn(min = minContentWidth).clickable { future?.send(index) }.padding(Theme.padding.value),
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

    @Stable
    abstract class ByList : DialogChoice() {
        abstract fun nameFactory(index: Int): String
        abstract fun iconFactory(index: Int): ImageVector

        @Composable
        override fun Name(index: Int) {
            SimpleEllipsisText(text = remember(index) { nameFactory(index) })
        }

        @Composable
        override fun Icon(index: Int) {
            Icon(icon = remember(index) { iconFactory(index) })
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
        fun fromItems(items: List<String>): DialogChoice = object : ByList() {
            override val num: Int = items.size
            override fun nameFactory(index: Int): String = items[index]
            override fun iconFactory(index: Int): ImageVector = Icons.KeyboardArrowRight
        }

        @Stable
        fun fromIconItems(items: List<Pair<String, ImageVector>>): DialogChoice = object : ByList() {
            override val num: Int = items.size
            override fun nameFactory(index: Int): String = items[index].first
            override fun iconFactory(index: Int): ImageVector = items[index].second
        }
    }
}