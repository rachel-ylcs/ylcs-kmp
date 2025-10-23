package love.yinlin.platform

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationImportant
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import love.yinlin.compose.CustomTheme
import love.yinlin.compose.ui.image.MiniIcon

@Composable
fun UnsupportedPlatformComponent(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(CustomTheme.padding.verticalSpace, Alignment.CenterVertically)
    ) {
        MiniIcon(
            icon = Icons.Filled.NotificationImportant,
            size = CustomTheme.size.image
        )
        Text(text = UnsupportedPlatformText)
    }
}