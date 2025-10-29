package love.yinlin.compose.ui.layout

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.serialization.Serializable
import love.yinlin.compose.*
import love.yinlin.compose.ui.animation.LoadingAnimation
import love.yinlin.compose.ui.image.MiniIcon
import love.yinlin.compose.ui.input.LoadingPrimaryButton
import love.yinlin.compose.ui.resources.*
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.stringResource

@Stable
@Serializable
enum class BoxState {
    LOADING,
    CONTENT,
    EMPTY,
    NETWORK_ERROR
}

@Composable
fun SimpleLoadingBox() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        LoadingAnimation()
    }
}

@Composable
fun LoadingBox(
    text: String = stringResource(Res.string.loading_state_string),
    icon: DrawableResource = Res.drawable.img_state_loading,
    color: Color = LocalContentColor.current
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(CustomTheme.padding.verticalExtraSpace)
        ) {
            MiniIcon(
                res = icon,
                size = CustomTheme.size.extraImage
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(CustomTheme.padding.horizontalExtraSpace)
            ) {
                LoadingAnimation()
                Text(
                    text = text,
                    color = color
                )
            }
        }
    }
}

@Composable
fun SimpleEmptyBox(
    text: String = stringResource(Res.string.empty_state_string),
    color: Color = LocalContentColor.current
) {
    Box(
        modifier = Modifier.fillMaxSize().padding(CustomTheme.padding.extraValue),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(CustomTheme.padding.horizontalExtraSpace)
        ) {
            MiniIcon(
                icon = Icons.Filled.Error,
                color = color
            )
            Text(
                text = text,
                color = color
            )
        }
    }
}

@Composable
fun EmptyBox(
    text: String = stringResource(Res.string.empty_state_string),
    icon: DrawableResource = Res.drawable.img_state_empty,
    color: Color = LocalContentColor.current
) {
    Box(
        modifier = Modifier.fillMaxSize().padding(CustomTheme.padding.extraValue),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(CustomTheme.padding.verticalExtraSpace)
        ) {
            MiniIcon(
                res = icon,
                size = CustomTheme.size.extraImage
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(CustomTheme.padding.horizontalExtraSpace)
            ) {
                MiniIcon(
                    icon = Icons.Filled.Error,
                    color = color
                )
                Text(
                    text = text,
                    color = color
                )
            }
        }
    }
}

@Composable
fun NetWorkErrorBox(
    text: String = stringResource(Res.string.network_error_state_string),
    icon: DrawableResource = Res.drawable.img_state_network_error,
    retry: (suspend CoroutineScope.() -> Unit)? = null,
    retryText: String = stringResource(Res.string.network_error_retry_string)
) {
    Box(
        modifier = Modifier.fillMaxSize().padding(CustomTheme.padding.extraValue),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(CustomTheme.padding.verticalExtraSpace)
        ) {
            MiniIcon(
                res = icon,
                size = CustomTheme.size.extraImage
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(CustomTheme.padding.horizontalExtraSpace)
            ) {
                MiniIcon(
                    icon = Icons.Filled.WifiOff,
                    color = MaterialTheme.colorScheme.error
                )
                Text(
                    text = text,
                    color = MaterialTheme.colorScheme.error
                )
            }
            if (retry != null) {
                LoadingPrimaryButton(
                    text = retryText,
                    onClick = { retry() }
                )
            }
        }
    }
}

@Composable
fun StatusBox(
    ok: Boolean,
    size: Dp,
    iconTrue: DrawableResource = Res.drawable.img_state_loading,
    iconFalse: DrawableResource = Res.drawable.img_state_network_error
) {
    MiniIcon(res = if (ok) iconTrue else iconFalse, size = size)
}

@Composable
fun StatefulBox(
    state: BoxState,
    retry: (suspend CoroutineScope.() -> Unit)? = null,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Crossfade(
        targetState = state,
        animationSpec = tween(durationMillis = LocalAnimationSpeed.current, easing = FastOutSlowInEasing),
        modifier = modifier,
    ) {
        when (it) {
            BoxState.CONTENT -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    content()
                }
            }
            BoxState.LOADING -> LoadingBox()
            BoxState.EMPTY -> EmptyBox()
            BoxState.NETWORK_ERROR -> NetWorkErrorBox(retry = retry)
        }
    }
}