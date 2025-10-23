package love.yinlin.compose.ui.layout

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
import love.yinlin.platform.platform

val UnsupportedPlatformText = "不支持的平台 $platform"

open class UnsupportedPlatformException : Exception(UnsupportedPlatformText)

fun unsupportedPlatform(): Nothing = throw UnsupportedPlatformException()

@Composable
fun UnsupportedComponent(modifier: Modifier = Modifier) {
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