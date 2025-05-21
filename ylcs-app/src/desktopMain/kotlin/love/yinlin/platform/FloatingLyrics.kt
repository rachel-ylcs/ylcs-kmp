package love.yinlin.platform

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import love.yinlin.common.ThemeValue

@Stable
class ActualFloatingLyrics : FloatingLyrics() {
    override val canAttached: Boolean = true
    override var isAttached: Boolean by mutableStateOf(false)

    override fun applyPermission(onResult: (Boolean) -> Unit) {}

    override fun attach() {
        if (!isAttached) isAttached = true
    }

    override fun detach() {
        if (isAttached) isAttached = false
    }

    override fun updateLyrics(lyrics: String?) {
        currentLyrics = lyrics
    }

    @Composable
    override fun FloatingContent() {
        val config = app.config.floatingLyricsConfig
        currentLyrics?.let { lyrics ->
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier.padding(
                        start = this.maxWidth * config.left.coerceIn(0f, 1f),
                        end = this.maxWidth * (1 - config.right).coerceIn(0f, 1f),
                        top = ThemeValue.Padding.VerticalExtraSpace * 4f * config.top
                    ).fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = lyrics,
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontSize = MaterialTheme.typography.labelLarge.fontSize * config.textSize
                        ),
                        color = Color(config.textColor),
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.background(color = Color(config.backgroundColor)).padding(ThemeValue.Padding.Value)
                    )
                }
            }
        }
    }
}