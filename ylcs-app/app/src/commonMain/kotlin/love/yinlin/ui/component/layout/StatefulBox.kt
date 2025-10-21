package love.yinlin.ui.component.layout

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.unit.Dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.serialization.Serializable
import love.yinlin.compose.*
import love.yinlin.platform.app
import love.yinlin.resources.*
import love.yinlin.ui.component.image.MiniIcon
import love.yinlin.ui.component.input.PrimaryLoadingButton
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
fun LoadingAnimation(
	size: Dp = CustomTheme.size.icon,
	color: Color = MaterialTheme.colorScheme.primary,
	duration: Int = app.config.animationSpeed,
	num: Int = 3,
	modifier: Modifier = Modifier
) {
	val transition = rememberInfiniteTransition()
	val values = Array(num) {
		transition.animateFloat(0.3f, 1f, infiniteRepeatable(
			animation = tween(duration, it * duration / num, LinearEasing),
			repeatMode = RepeatMode.Reverse
		))
	}

	Canvas(modifier = modifier.padding(CustomTheme.padding.innerIconSpace).size(size)) {
		values.forEachIndexed { index, state ->
			val width = (size / 5).toPx()
			val spacing = (this.size.width - (num * width)) / 2
			scale(scaleX = 1f, scaleY = state.value) {
				drawLine(
					color = color.copy(alpha = state.value),
					start = Offset(width / 2 + (width + spacing) * index, 0f),
					end = Offset(width / 2 + (width + spacing) * index, this.size.height),
					strokeWidth = width
				)
			}
		}
	}
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
				res = Res.drawable.img_state_loading,
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
				res = Res.drawable.img_state_empty,
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
    retry: (suspend CoroutineScope.() -> Unit)? = null
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
				res = Res.drawable.img_state_network_error,
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
				PrimaryLoadingButton(
					text = stringResource(Res.string.network_error_retry_string),
					onClick = { retry() }
				)
			}
		}
	}
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
        animationSpec = tween(durationMillis = app.config.animationSpeed, easing = FastOutSlowInEasing),
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