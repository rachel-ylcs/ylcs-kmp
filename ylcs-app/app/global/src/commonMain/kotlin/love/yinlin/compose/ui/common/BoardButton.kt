package love.yinlin.compose.ui.common

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.zIndex
import love.yinlin.compose.Theme
import love.yinlin.compose.bold
import love.yinlin.compose.ui.container.Surface
import love.yinlin.compose.ui.image.Icon
import love.yinlin.compose.ui.text.SimpleClipText
import love.yinlin.compose.ui.text.SimpleEllipsisText
import love.yinlin.compose.ui.text.TextIconBinder

@Composable
fun BoardButton(text: String, icon: ImageVector, label: Int = 0, onClick: () -> Unit) {
    Box {
        TextIconBinder(modifier = Modifier
            .clip(Theme.shape.v7)
            .clickable(onClick = onClick)
            .padding(Theme.padding.value)
            .zIndex(1f)
        ) { idIcon, idText ->
            Icon(icon = icon, modifier = Modifier.idIcon())
            SimpleEllipsisText(text = text, modifier = Modifier.idText())
        }
        if (label > 0) {
            Layout(
                modifier = Modifier
                    .background(color = Theme.color.error, shape = Theme.shape.circle)
                    .align(Alignment.TopEnd)
                    .zIndex(2f),
                content = {
                    SimpleClipText(
                        text = if (label < 10) label.toString() else "+",
                        color = Theme.color.onError,
                        textAlign = TextAlign.Center
                    )
                }
            ) { measurables, constraints ->
                val textPlaceable = measurables.first().measure(constraints)
                val boxSize = maxOf(textPlaceable.width, textPlaceable.height)
                layout(boxSize, boxSize) {
                    textPlaceable.placeRelative(x = (boxSize - textPlaceable.width) / 2, y = (boxSize - textPlaceable.height) / 2)
                }
            }
        }
    }
}

@Composable
fun BoardButtonContainer(title: String, content: @Composable RowScope.() -> Unit) {
    Surface(
        modifier = Modifier.padding(Theme.padding.value).fillMaxWidth(),
        contentPadding = Theme.padding.value9,
        shape = Theme.shape.v3,
        shadowElevation = Theme.shadow.v3
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(Theme.padding.v)
        ) {
            SimpleEllipsisText(text = title, style = Theme.typography.v6.bold)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically,
                content = content
            )
        }
    }
}