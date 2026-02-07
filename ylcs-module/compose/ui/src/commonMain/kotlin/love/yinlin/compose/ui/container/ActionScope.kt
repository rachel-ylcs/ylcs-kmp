package love.yinlin.compose.ui.container

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import love.yinlin.compose.Theme

@Stable
sealed class ActionScope(private val ltr: Boolean) {
    @Stable
    data object Left : ActionScope(ltr = true)
    @Stable
    data object Right : ActionScope(ltr = false)

    @Composable
    fun Container(modifier: Modifier = Modifier, content: @Composable RowScope.() -> Unit) {
        Row(
            modifier = modifier,
            horizontalArrangement = Arrangement.spacedBy(
                space = Theme.padding.h,
                alignment = if (ltr) Alignment.Start else Alignment.End
            ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            content()
        }
    }
}