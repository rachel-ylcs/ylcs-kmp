package love.yinlin.compose.ui.common

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import love.yinlin.compose.Theme
import love.yinlin.compose.bold
import love.yinlin.compose.ui.floating.Dialog
import love.yinlin.compose.ui.icon.Icons
import love.yinlin.compose.ui.image.Icon
import love.yinlin.compose.ui.text.Text

class DialogModFactory : Dialog<Int>() {
    @Composable
    private fun ModFactoryItem(
        text: String,
        textColor: Color,
        icon: ImageVector,
        iconColor: Color,
        modifier: Modifier = Modifier
    ) {
        Column(modifier = modifier) {
            Icon(
                icon = icon,
                color = iconColor,
                modifier = Modifier.size(Theme.size.image7).align(Alignment.Start)
            )
            Text(
                text = text,
                color = textColor,
                style = Theme.typography.v5.bold,
                modifier = Modifier.align(Alignment.End)
            )
        }
    }

    suspend fun open(): Int? = awaitResult()

    @Composable
    override fun Land() {
        LandDialog {
            Column(modifier = Modifier.width(Theme.size.cell1)) {
                ModFactoryItem(
                    text = "银临无损音乐集",
                    textColor = Theme.color.primary,
                    icon = Icons.LibraryMusic,
                    iconColor = Theme.color.primary,
                    modifier = Modifier.fillMaxWidth()
                )
                ModFactoryItem(
                    text = "导入MOD",
                    textColor = Theme.color.primary,
                    icon = Icons.Upload,
                    iconColor = Theme.color.primary,
                    modifier = Modifier.fillMaxWidth()
                )
                ModFactoryItem(
                    text = "本地创建",
                    textColor = Theme.color.primary,
                    icon = Icons.DesignServices,
                    iconColor = Theme.color.primary,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}