package love.yinlin.ui.component.image

import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import com.github.panpf.sketch.*
import com.github.panpf.sketch.request.ImageOptions
import com.github.panpf.sketch.state.rememberIconPainterStateImage
import com.github.panpf.zoomimage.SketchZoomAsyncImage
import com.github.panpf.zoomimage.SketchZoomState
import com.github.panpf.zoomimage.rememberSketchZoomState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import love.yinlin.common.Colors
import love.yinlin.common.ThemeColor
import love.yinlin.extension.condition
import love.yinlin.extension.rememberState
import love.yinlin.platform.ImageQuality
import love.yinlin.resources.Res
import love.yinlin.resources.placeholder_pic
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

private val DEFAULT_ICON_SIZE = 24.dp

@Composable
fun MiniIcon(
	icon: ImageVector? = null,
	color: Color = MaterialTheme.colorScheme.onSurface,
	size: Dp = DEFAULT_ICON_SIZE,
	modifier: Modifier = Modifier
) {
	if (icon != null) {
		Icon(
			modifier = modifier.size(size),
			imageVector = icon,
			contentDescription = null,
			tint = color,
		)
	}
	else {
		Spacer(modifier = modifier.size(size))
	}
}

@Composable
fun NoImage(
	width: Dp = DEFAULT_ICON_SIZE,
	height: Dp = DEFAULT_ICON_SIZE,
	color: Color = MaterialTheme.colorScheme.onSurface
) {
	if (width == height) {
		Box(
			modifier = Modifier.shadow(2.dp, CircleShape),
			contentAlignment = Alignment.Center
		) {
			Icon(
				modifier = Modifier.size(width),
				imageVector = Icons.AutoMirrored.Filled.Help,
				contentDescription = null,
				tint = color,
			)
		}
	}
	else {
		Box(
			modifier = Modifier.width(width).height(height).shadow(2.dp),
			contentAlignment = Alignment.Center
		) {
			Icon(
				modifier = Modifier.size(min(width, height)).padding(5.dp),
				imageVector = Icons.AutoMirrored.Filled.Help,
				contentDescription = null,
				tint = color,
			)
		}
	}
}

@Stable
data class ColorfulImageVector(
	val icon: ImageVector,
	val color: Color,
	val background: Color
)

@Composable
fun colorfulImageVector(
	icon: ImageVector,
	color: Color = MaterialTheme.colorScheme.onSurface,
	background: Color = Colors.Transparent
) = ColorfulImageVector(icon, color, background)

@Composable
fun ColorfulIcon(
	icon: ColorfulImageVector,
	size: Dp = DEFAULT_ICON_SIZE
) {
	Box(
		modifier = Modifier.size(size)
			.clip(CircleShape)
			.background(icon.background)
			.padding(3.dp),
		contentAlignment = Alignment.Center
	) {
		Icon(
			modifier = Modifier.fillMaxSize(),
			imageVector = icon.icon,
			contentDescription = null,
			tint = icon.color,
		)
	}
}

@Composable
fun ClickIcon(
	icon: ImageVector,
	color: Color = MaterialTheme.colorScheme.onSurface,
	size: Dp = DEFAULT_ICON_SIZE,
	indication: Boolean = true,
	enabled: Boolean = true,
	modifier: Modifier = Modifier,
	onClick: () -> Unit
) {
	val localIndication = if (indication) LocalIndication.current else null
	val interactionSource = if (localIndication is IndicationNodeFactory) null else remember { MutableInteractionSource() }

	Icon(
		modifier = modifier.size(size + 6.dp)
			.clip(MaterialTheme.shapes.small)
			.clickable(
				onClick = onClick,
				indication = localIndication,
				interactionSource = interactionSource,
				enabled = enabled
			).padding(3.dp),
		imageVector = icon,
		contentDescription = null,
		tint = if (enabled) color else ThemeColor.fade,
	)
}

@Composable
fun StaticLoadingIcon(
	isLoading: Boolean,
	icon: ImageVector,
	size: Dp = DEFAULT_ICON_SIZE,
	color: Color = MaterialTheme.colorScheme.onSurface,
	enabled: Boolean = true,
	modifier: Modifier = Modifier,
	iconModifier: Modifier = Modifier.fillMaxSize(),
) {
	Box(
		modifier = modifier.size(size),
		contentAlignment = Alignment.Center
	) {
		if (isLoading) {
			CircularProgressIndicator(
				modifier = Modifier.fillMaxSize(fraction = 0.75f),
				color = color
			)
		}
		else {
			Icon(
				modifier = iconModifier,
				imageVector = icon,
				contentDescription = null,
				tint = if (enabled) color else ThemeColor.fade
			)
		}
	}
}

@Composable
fun LoadingIcon(
	icon: ImageVector,
	size: Dp = DEFAULT_ICON_SIZE,
	color: Color = MaterialTheme.colorScheme.onSurface,
	enabled: Boolean = true,
	modifier: Modifier = Modifier,
	onClick: suspend CoroutineScope.() -> Unit
) {
	val scope = rememberCoroutineScope()
	var isLoading by rememberState { false }

	StaticLoadingIcon(
		isLoading = isLoading,
		icon = icon,
		size = size + 6.dp,
		color = color,
		enabled = enabled,
		modifier = modifier,
		iconModifier = Modifier.fillMaxSize()
			.clip(MaterialTheme.shapes.small)
			.clickable(
				enabled = enabled && !isLoading,
				onClick = {
					scope.launch {
						isLoading = true
						onClick()
						isLoading = false
					}
				}
			).padding(3.dp),
	)
}

@Composable
fun MiniImage(
	res: DrawableResource,
	size: Dp = DEFAULT_ICON_SIZE,
	modifier: Modifier = Modifier
) {
	Image(
		modifier = modifier.size(size),
		painter = painterResource(res),
		contentDescription = null
	)
}

@Composable
fun ClickImage(
	res: DrawableResource,
	modifier: Modifier = Modifier,
	onClick: () -> Unit
) {
	Image(
		modifier = modifier
			.clip(MaterialTheme.shapes.small)
			.clickable(onClick = onClick)
			.padding(3.dp),
		painter = painterResource(res),
		contentDescription = null
	)
}

@Composable
private fun rememberWebImageKeyUrl(uri: String, key: Any? = null): String = remember(uri, key) {
	if (key == null) uri
	else if (uri.contains("?")) "$uri&_cacheKey=$key"
	else "$uri?_cacheKey=$key"
}

@Composable
private fun rememberLocalFileImageKeyUrl(path: Path): String = remember(path) {
	"$path?_cacheKey=${SystemFileSystem.metadataOrNull(path)?.size}"
}

@Composable
fun rememberWebImageState(
	quality: ImageQuality,
	placeholder: DrawableResource? = null,
	isCrossfade: Boolean = true
): AsyncImageState {
	val context = LocalPlatformContext.current
	val holder = placeholder?.let { rememberIconPainterStateImage(it) }
	val options = remember(quality, holder, isCrossfade) {
		ImageOptions.Builder().apply {
			sizeMultiplier(quality.sizeMultiplier)
			placeholder(holder)
			if (isCrossfade) crossfade()
		}.merge(SingletonSketch.get(context).globalImageOptions).build()
	}
	return rememberAsyncImageState(options)
}

@Composable
fun WebImage(
	uri: String,
	key: Any? = null,
	modifier: Modifier = Modifier,
	circle: Boolean = false,
	quality: ImageQuality = ImageQuality.Medium,
	contentScale: ContentScale = ContentScale.Fit,
	alignment: Alignment = Alignment.Center,
	alpha: Float = 1f,
	placeholder: DrawableResource = Res.drawable.placeholder_pic,
	state: AsyncImageState = rememberWebImageState(quality, placeholder, true),
	onClick: (() -> Unit)? = null
) {
	AsyncImage(
		uri = rememberWebImageKeyUrl(uri, key),
		contentDescription = null,
		state = state,
		alignment = alignment,
		contentScale = contentScale,
		filterQuality = quality.filterQuality,
		alpha = alpha,
		modifier = modifier.condition(circle) { clip(CircleShape) }
			.condition(onClick != null) { clickable(onClick = onClick ?: {}) }
	)
}

@Composable
fun LocalFileImage(
	path: Path,
	modifier: Modifier = Modifier,
	circle: Boolean = false,
	quality: ImageQuality = ImageQuality.Medium,
	contentScale: ContentScale = ContentScale.Fit,
	alignment: Alignment = Alignment.Center,
	alpha: Float = 1f,
	state: AsyncImageState = rememberWebImageState(quality, null, true),
	onClick: (() -> Unit)? = null
) {
	AsyncImage(
		uri = rememberLocalFileImageKeyUrl(path),
		contentDescription = null,
		state = state,
		alignment = alignment,
		contentScale = contentScale,
		filterQuality = quality.filterQuality,
		alpha = alpha,
		modifier = modifier.condition(circle) { clip(CircleShape) }
			.condition(onClick != null) { clickable(onClick = onClick ?: {}) }
	)
}

@Composable
fun ZoomWebImage(
	uri: String,
	key: Any? = null,
	zoomState: SketchZoomState = rememberSketchZoomState(),
	modifier: Modifier = Modifier,
	quality: ImageQuality = ImageQuality.High,
	contentScale: ContentScale = ContentScale.Fit,
	alignment: Alignment = Alignment.Center,
	alpha: Float = 1f,
	placeholder: DrawableResource = Res.drawable.placeholder_pic,
	state: AsyncImageState = rememberWebImageState(quality, placeholder, false)
) {
	SketchZoomAsyncImage(
		uri = rememberWebImageKeyUrl(uri, key),
		contentDescription = null,
		state = state,
		zoomState = zoomState,
		alignment = alignment,
		contentScale = contentScale,
		filterQuality = quality.filterQuality,
		alpha = alpha,
		scrollBar = null,
		modifier = modifier
	)
}