package love.yinlin.compose.ui.input

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.zIndex
import love.yinlin.compose.*

@Composable
fun ProgressSlider(
    value: Float,
    modifier: Modifier = Modifier,
    height: Dp = CustomTheme.size.sliderHeight,
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
        var isSliding by rememberFalse()
        var percent by rememberFloatState { value.coerceIn(0f, 1f) }
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
            }.zIndex(1f),
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
        }

        // Others
        Box(
            modifier = Modifier.matchParentSize().zIndex(4f),
            contentAlignment = Alignment.CenterStart
        ) {
            content?.invoke(this)
        }
    }
}

@Composable
fun CylinderSlider(
    value: Float,
    onValueChanged: (Float) -> Unit,
    modifier: Modifier = Modifier,
    backgroundColor: Color = MaterialTheme.colorScheme.surface,
    borderColor: Color = MaterialTheme.colorScheme.primary,
    activeColor: Color = MaterialTheme.colorScheme.primaryContainer,
    enabled: Boolean = true,
    content: @Composable ColumnScope.(Float) -> Unit
) {
    BoxWithConstraints(modifier = modifier) {
        var isSliding by rememberFalse()
        var percent by rememberFloatState { value.coerceIn(0f, 1f) }

        LaunchedEffect(value) {
            if (!isSliding) percent = value.coerceIn(0f, 1f)
        }

        Box(modifier = Modifier.height(IntrinsicSize.Min)
            .border(CustomTheme.border.small, borderColor, CircleShape)
            .clip(CircleShape)
            .background(backgroundColor)
            .pointerInput(enabled, maxWidth, onValueChanged) {
                detectHorizontalDragGestures(onDragEnd = {
                    isSliding = false
                    if (enabled) onValueChanged.invoke(percent)
                }) { change, _ ->
                    isSliding = true
                    if (enabled) {
                        val newProgress = (change.position.x / maxWidth.toPx()).coerceIn(0f, 1f)
                        percent = newProgress
                    }
                }
            }.pointerInput(enabled, maxWidth, onValueChanged) {
                detectTapGestures {
                    if (enabled) {
                        val newProgress = (it.x / maxWidth.toPx()).coerceIn(0f, 1f)
                        onValueChanged.invoke(newProgress)
                    }
                }
            },
            contentAlignment = Alignment.CenterStart
        ) {
            Box(modifier = Modifier.matchParentSize().zIndex(1f)) {
                Box(modifier = Modifier.fillMaxWidth(percent).fillMaxHeight().background(color = activeColor.copy(alpha = 0.75f), shape = CircleShape))
            }
            Column(
                modifier = Modifier.fillMaxWidth().padding(CustomTheme.padding.extraValue).zIndex(2f),
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.spacedBy(CustomTheme.padding.littleSpace, Alignment.CenterVertically)
            ) {
                content(percent)
            }
        }
    }
}