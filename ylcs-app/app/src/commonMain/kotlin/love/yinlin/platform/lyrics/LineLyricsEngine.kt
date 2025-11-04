package love.yinlin.platform.lyrics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import kotlinx.io.files.Path
import love.yinlin.compose.Colors
import love.yinlin.compose.CustomTheme
import love.yinlin.compose.mutableRefStateOf
import love.yinlin.extension.readText
import love.yinlin.platform.Platform
import love.yinlin.platform.platform

@Stable
internal class LineLyricsEngine : LyricsEngine {
    override val type: LyricsEngineType = LyricsEngineType.Line

    private var lines: List<LrcLine>? by mutableRefStateOf(null)
    private var currentIndex by mutableIntStateOf(-1)
    private val currentText by derivedStateOf { lines?.getOrNull(currentIndex)?.text ?: "" }

    override suspend fun load(rootPath: Path): Boolean {
        val source = Path(rootPath, type.resType.filename).readText()
        lines = source?.let { LrcParser(it).paddingLyrics }
        return lines != null
    }

    override fun reset() {
        lines = null
        currentIndex = -1
    }

    override fun update(position: Long) {
        val newIndex = lines?.let { items ->
            val index = items.indexOfFirst { it.position > position } - 1
            if (index >= 0) index else -1
        } ?: -1
        currentIndex = newIndex
    }

    @Composable
    private fun AndroidContent(config: LyricsEngineConfig) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val start = remember(maxWidth, config) { maxWidth * config.android.left.coerceIn(0f, 1f) }
            val end = remember(maxWidth, config) {maxWidth * (1 - config.android.right).coerceIn(0f, 1f)  }

            Box(
                modifier = Modifier.padding(
                    start = start,
                    end = end,
                    top = CustomTheme.padding.verticalExtraSpace * 4f * config.android.top
                ).fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = currentText,
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontSize = MaterialTheme.typography.labelLarge.fontSize * config.textSize
                    ),
                    color = Colors(config.textColor),
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.wrapContentSize(unbounded = true)
                        .background(color = Colors(config.backgroundColor))
                        .padding(CustomTheme.padding.value)
                )
            }
        }
    }

    @Composable
    private fun IOSContent(config: LyricsEngineConfig) {

    }

    @Composable
    private fun DesktopContent(config: LyricsEngineConfig) {
        Box(modifier = Modifier.fillMaxSize()) {
            Text(
                text = currentText,
                style = MaterialTheme.typography.displayMedium.copy(
                    fontSize = MaterialTheme.typography.displayLarge.fontSize * config.textSize
                ),
                color = Colors(config.textColor),
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.background(color = Colors(config.backgroundColor))
                    .padding(CustomTheme.padding.value)
                    .align(Alignment.BottomCenter)
            )
        }
    }

    @Composable
    private fun WebContent(config: LyricsEngineConfig) {

    }

    @Composable
    override fun Content(config: LyricsEngineConfig) {
        CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, 1f)) {
            if (currentText.isNotEmpty()) {
                when (platform) {
                    Platform.Android -> AndroidContent(config)
                    Platform.IOS -> IOSContent(config)
                    Platform.Windows, Platform.Linux, Platform.MacOS -> DesktopContent(config)
                    Platform.WebWasm -> WebContent(config)
                }
            }
        }
    }
}