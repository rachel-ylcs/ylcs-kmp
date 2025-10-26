package love.yinlin.compose.ui.layout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import love.yinlin.compose.ui.image.MiniIcon
import org.jetbrains.compose.resources.DrawableResource

@Composable
fun AppTopBar(
    title: String,
    icon: DrawableResource? = null,
    modifier: Modifier = Modifier,
    actions: @Composable ActionScope.() -> Unit = {}
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            MiniIcon(res = icon)
        }
        Space()
        Text(
            text = title,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Space()
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.End
        ) {
            ActionScope.Right.actions()
        }
    }
}