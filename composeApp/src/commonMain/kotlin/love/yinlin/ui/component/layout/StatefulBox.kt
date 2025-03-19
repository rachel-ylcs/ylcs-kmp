package love.yinlin.ui.component.layout

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import love.yinlin.ui.component.button.LoadingButton
import love.yinlin.ui.component.image.MiniIcon
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import ylcs_kmp.composeapp.generated.resources.*

enum class BoxState {
	LOADING,
	CONTENT,
	EMPTY,
	NETWORK_ERROR
}

@Composable
fun LoadingAnimation(
	size: Dp = 32.dp,
	color: Color = MaterialTheme.colorScheme.primary,
	duration: Int = 500,
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

	Canvas(modifier = modifier.size(size)) {
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
		LoadingAnimation(size = 32.dp)
	}
}

@Composable
fun LoadingBox() {
	Box(
		modifier = Modifier.fillMaxSize(),
		contentAlignment = Alignment.Center
	) {
		Column(
			horizontalAlignment = Alignment.CenterHorizontally,
			verticalArrangement = Arrangement.spacedBy(20.dp)
		) {
			Image(
				modifier = Modifier.height(200.dp),
				painter = painterResource(Res.drawable.img_state_loading),
				contentDescription = null
			)
			Row(
				verticalAlignment = Alignment.CenterVertically,
				horizontalArrangement = Arrangement.spacedBy(20.dp)
			) {
				LoadingAnimation(size = 32.dp)
				Text(text = stringResource(Res.string.loading_state_string))
			}
		}
	}
}

@Composable
fun SimpleEmptyBox() {
	Box(
		modifier = Modifier.fillMaxSize(),
		contentAlignment = Alignment.Center
	) {
		Row(
			verticalAlignment = Alignment.CenterVertically,
			horizontalArrangement = Arrangement.spacedBy(20.dp)
		) {
			MiniIcon(Icons.Filled.Error)
			Text(text = stringResource(Res.string.empty_state_string))
		}
	}
}

@Composable
fun EmptyBox() {
	Box(
		modifier = Modifier.fillMaxSize(),
		contentAlignment = Alignment.Center
	) {
		Column(
			horizontalAlignment = Alignment.CenterHorizontally,
			verticalArrangement = Arrangement.spacedBy(20.dp)
		) {
			Image(
				modifier = Modifier.size(200.dp),
				painter = painterResource(Res.drawable.img_state_empty),
				contentDescription = null
			)
			Row(
				verticalAlignment = Alignment.CenterVertically,
				horizontalArrangement = Arrangement.spacedBy(20.dp)
			) {
				MiniIcon(Icons.Filled.Error)
				Text(text = stringResource(Res.string.empty_state_string))
			}
		}
	}
}

@Composable
fun NetWorkErrorBox(retry: (suspend CoroutineScope.() -> Unit)? = null) {
	Box(
		modifier = Modifier.fillMaxSize(),
		contentAlignment = Alignment.Center
	) {
		Column(
			horizontalAlignment = Alignment.CenterHorizontally,
			verticalArrangement = Arrangement.spacedBy(20.dp)
		) {
			Image(
				modifier = Modifier.size(200.dp),
				painter = painterResource(Res.drawable.img_state_network_error),
				contentDescription = null
			)
			Row(
				verticalAlignment = Alignment.CenterVertically,
				horizontalArrangement = Arrangement.spacedBy(20.dp)
			) {
				MiniIcon(
					imageVector = Icons.Filled.WifiOff,
					color = MaterialTheme.colorScheme.error
				)
				Text(
					text = stringResource(Res.string.network_error_state_string),
					color = MaterialTheme.colorScheme.error
				)
			}
			if (retry != null) {
				LoadingButton(
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
			BoxState.NETWORK_ERROR -> NetWorkErrorBox(retry)
		}
	}
}