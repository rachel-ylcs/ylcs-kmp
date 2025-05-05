package love.yinlin.ui.component.input

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.zIndex
import love.yinlin.common.ThemeValue
import love.yinlin.extension.rememberState

@Composable
fun BeautifulSlider(
    value: Float,
    modifier: Modifier = Modifier,
    height: Dp = ThemeValue.Size.ProgressHeight,
    trackColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    activeColor: Color = MaterialTheme.colorScheme.primaryContainer,
    trackShape: Shape = MaterialTheme.shapes.medium,
    thumbColor: Color = MaterialTheme.colorScheme.onPrimaryContainer,
    showThumb: Boolean = true,
    enabled: Boolean = true,
    onValueChange: ((Float) -> Unit)? = null,
    onValueChangeFinished: ((Float) -> Unit)? = null,
    content: (@Composable BoxScope.() -> Unit)? = null
) {
    BoxWithConstraints(modifier = modifier) {
        var isSliding by rememberState { false }
        var percent by rememberState { value.coerceIn(0f, 1f) }
        val offsetX = maxWidth * percent

        LaunchedEffect(value) {
            if (!isSliding) percent = value.coerceIn(0f, 1f)
        }

        Box(modifier = Modifier.fillMaxWidth().height(height * 2)
            .pointerInput(enabled, maxWidth, onValueChange, onValueChangeFinished) {
                detectHorizontalDragGestures(onDragEnd = {
                    isSliding = false
                    if (enabled) onValueChangeFinished?.invoke(percent)
                }) { change, _ ->
                    isSliding = true
                    if (enabled) {
                        val newProgress = (change.position.x / maxWidth.toPx()).coerceIn(0f, 1f)
                        percent = newProgress
                        onValueChange?.invoke(newProgress)
                    }
                }
            }.pointerInput(enabled, maxWidth, onValueChange, onValueChangeFinished) {
                detectTapGestures {
                    if (enabled) {
                        val newProgress = (it.x / maxWidth.toPx()).coerceIn(0f, 1f)
                        onValueChange?.invoke(newProgress)
                        onValueChangeFinished?.invoke(newProgress)
                    }
                }
            },
            contentAlignment = Alignment.CenterStart
        ) {
            // Track
            Box(
                modifier = Modifier.fillMaxWidth().height(height)
                    .background(color = trackColor, shape = trackShape)
                    .zIndex(1f),
                contentAlignment = Alignment.CenterStart
            ) {
                // ActiveTrack
                Box(modifier = Modifier.fillMaxWidth(percent).fillMaxHeight()
                    .background(color = activeColor, shape = trackShape)
                )
            }

            // Thumb
            if (showThumb) {
                Box(modifier = Modifier.size(height * 2)
                    .offset(offsetX - height)
                    .shadow(
                        elevation = height,
                        shape = CircleShape,
                        spotColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ).background(thumbColor, CircleShape)
                    .zIndex(2f)
                )
            }

            // Others
            Box(
                modifier = Modifier.fillMaxSize().zIndex(3f),
                contentAlignment = Alignment.CenterStart
            ) {
                content?.invoke(this)
            }
        }
    }
}