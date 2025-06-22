package love.yinlin.ui.component.image

import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import com.github.panpf.sketch.*
import com.github.panpf.sketch.ability.progressIndicator
import com.github.panpf.sketch.painter.internal.AbsProgressPainter
import com.github.panpf.sketch.request.ImageOptions
import com.github.panpf.sketch.request.disallowAnimatedImage
import com.github.panpf.sketch.request.pauseLoadWhenScrolling
import com.github.panpf.sketch.state.ColorPainterStateImage
import com.github.panpf.zoomimage.SketchZoomAsyncImage
import com.github.panpf.zoomimage.SketchZoomState
import com.github.panpf.zoomimage.rememberSketchZoomState
import io.github.alexzhirkevich.qrose.toImageBitmap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import love.yinlin.common.Colors
import love.yinlin.common.ThemeValue
import love.yinlin.extension.rememberFalse
import love.yinlin.ui.component.node.condition
import love.yinlin.platform.ImageQuality
import love.yinlin.platform.app
import love.yinlin.resources.Res
import love.yinlin.resources.placeholder_pic
import love.yinlin.ui.component.screen.BallonTip
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

@Composable
fun MiniIcon(
	icon: ImageVector,
	color: Color = MaterialTheme.colorScheme.onSurface,
	size: Dp = ThemeValue.Size.Icon,
	modifier: Modifier = Modifier
) {
	Box(
		modifier = modifier,
		contentAlignment = Alignment.Center
	) {
		Icon(
			modifier = Modifier.padding(ThemeValue.Padding.InnerIcon).size(size),
			imageVector = icon,
			contentDescription = null,
			tint = color,
		)
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
	color: Color = Colors.Ghost,
	background: Color = Colors.Transparent
) = ColorfulImageVector(icon, color, background)

@Composable
fun ColorfulIcon(
	icon: ColorfulImageVector,
	size: Dp = ThemeValue.Size.Icon,
    gap: Float = 1.5f,
	onClick: (() -> Unit)? = null
) {
	Box(
		modifier = Modifier
			.clip(CircleShape)
			.condition(onClick != null) { clickable { onClick?.invoke() } }
			.background(icon.background.copy(alpha = 0.6f)),
		contentAlignment = Alignment.Center
	) {
		Icon(
			modifier = Modifier.padding(ThemeValue.Padding.InnerIcon * gap).size(size),
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
	size: Dp = ThemeValue.Size.Icon,
	indication: Boolean = true,
	enabled: Boolean = true,
	modifier: Modifier = Modifier,
	onClick: () -> Unit
) = MiniIcon(
	icon = icon,
	color = if (enabled) color else MaterialTheme.colorScheme.onSurfaceVariant,
	size = size,
	modifier = modifier
		.clip(MaterialTheme.shapes.extraSmall)
		.clickable(
			onClick = onClick,
			indication = if (indication) LocalIndication.current else null,
			interactionSource = remember { MutableInteractionSource() },
			enabled = enabled
		)
)

@Composable
fun ClickIcon(
    icon: ImageVector,
    tip: String,
    color: Color = MaterialTheme.colorScheme.onSurface,
    size: Dp = ThemeValue.Size.Icon,
    indication: Boolean = true,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    BallonTip(text = tip) { ClickIcon(icon, color, size, indication, enabled, modifier, onClick) }
}

@Composable
fun LoadingCircle(
	modifier: Modifier = Modifier,
	size: Dp = ThemeValue.Size.Icon,
	color: Color = MaterialTheme.colorScheme.onSurface,
) {
	Box(modifier = modifier) {
		CircularProgressIndicator(
			modifier = Modifier.padding(ThemeValue.Padding.InnerIcon).size(size),
			color = color
		)
	}
}

@Composable
fun StaticLoadingIcon(
	isLoading: Boolean,
	icon: ImageVector,
	size: Dp = ThemeValue.Size.Icon,
	color: Color = MaterialTheme.colorScheme.onSurface,
	enabled: Boolean = true,
	modifier: Modifier = Modifier
) {
	if (isLoading) {
		LoadingCircle(
			size = size,
			color = color,
			modifier = modifier
		)
	}
	else {
		MiniIcon(
			icon = icon,
			color = if (enabled) color else MaterialTheme.colorScheme.onSurfaceVariant,
			size = size,
			modifier = modifier
		)
	}
}

@Composable
fun LoadingIcon(
	icon: ImageVector,
	size: Dp = ThemeValue.Size.Icon,
	color: Color = MaterialTheme.colorScheme.onSurface,
	enabled: Boolean = true,
	modifier: Modifier = Modifier,
	onClick: suspend CoroutineScope.() -> Unit
) {
	val scope = rememberCoroutineScope()
	var isLoading by rememberFalse()

	if (isLoading) {
		LoadingCircle(
			size = size,
			color = color,
			modifier = modifier
		)
	}
	else {
		ClickIcon(
			icon = icon,
			color = if (enabled) color else MaterialTheme.colorScheme.onSurfaceVariant,
			size = size,
			enabled = enabled && !isLoading,
			onClick = {
				scope.launch {
					isLoading = true
					onClick()
					isLoading = false
				}
			},
			modifier = modifier
		)
	}
}

@Composable
fun LoadingIcon(
    icon: ImageVector,
    tip: String,
    size: Dp = ThemeValue.Size.Icon,
    color: Color = MaterialTheme.colorScheme.onSurface,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    onClick: suspend CoroutineScope.() -> Unit
) {
    BallonTip(text = tip) { LoadingIcon(icon, size, color, enabled, modifier, onClick) }
}

@Composable
fun MiniIcon(
	res: DrawableResource,
	size: Dp = ThemeValue.Size.Icon,
	modifier: Modifier = Modifier
) {
	Box(
		modifier = modifier,
		contentAlignment = Alignment.Center
	) {
		Image(
			modifier = Modifier.padding(ThemeValue.Padding.InnerIcon).size(size),
			painter = painterResource(res),
			contentDescription = null
		)
	}
}

@Composable
fun ClickIcon(
	res: DrawableResource,
	size: Dp = ThemeValue.Size.Icon,
	modifier: Modifier = Modifier,
	onClick: () -> Unit
) = MiniIcon(
	res = res,
	size = size,
	modifier = modifier
		.clip(MaterialTheme.shapes.extraSmall)
		.clickable(onClick = onClick)
)

@Composable
fun ClickIcon(
    res: DrawableResource,
    tip: String,
    size: Dp = ThemeValue.Size.Icon,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    BallonTip(text = tip) { ClickIcon(res, size, modifier, onClick) }
}

@Composable
fun IconText(
	icon: ImageVector,
	text: String,
	size: Dp = ThemeValue.Size.ExtraIcon,
	shape: Shape = MaterialTheme.shapes.large,
	onClick: () -> Unit
) {
	Column(
		modifier = Modifier
			.clip(shape)
			.clickable(onClick = onClick)
			.padding(ThemeValue.Padding.EqualValue),
		horizontalAlignment = Alignment.CenterHorizontally,
		verticalArrangement = Arrangement.spacedBy(ThemeValue.Padding.VerticalSpace)
	) {
		Image(
			painter = rememberVectorPainter(icon),
			contentDescription = null,
			modifier = Modifier.size(size)
		)
		Text(
			text = text,
			style = MaterialTheme.typography.labelMedium
		)
	}
}

@Composable
fun MiniImage(
	icon: ImageVector,
	size: Dp = ThemeValue.Size.Icon,
	modifier: Modifier = Modifier
) {
	Box(
		modifier = modifier,
		contentAlignment = Alignment.Center
	) {
		Image(
			painter = rememberVectorPainter(icon),
			contentDescription = null,
			modifier = Modifier.padding(ThemeValue.Padding.InnerIcon).size(size)
		)
	}
}

@Composable
fun MiniImage(
	res: DrawableResource,
	contentScale: ContentScale = ContentScale.Fit,
	alignment: Alignment = Alignment.Center,
	alpha: Float = 1f,
	modifier: Modifier = Modifier
) {
	Box(
		modifier = modifier,
		contentAlignment = Alignment.Center
	) {
		Image(
			painter = painterResource(res),
			modifier = Modifier.matchParentSize(),
			contentScale = contentScale,
			alignment = alignment,
			alpha = alpha,
			contentDescription = null
		)
	}
}

@Composable
fun MiniImage(
	painter: Painter,
	contentScale: ContentScale = ContentScale.Fit,
	alignment: Alignment = Alignment.Center,
	alpha: Float = 1f,
	modifier: Modifier = Modifier
) {
	Box(
		modifier = modifier,
		contentAlignment = Alignment.Center
	) {
		Image(
			painter = painter,
			modifier = Modifier.matchParentSize(),
			contentScale = contentScale,
			alignment = alignment,
			alpha = alpha,
			contentDescription = null
		)
	}
}

@Composable
fun ClickImage(
	res: DrawableResource,
	contentScale: ContentScale = ContentScale.Fit,
	alignment: Alignment = Alignment.Center,
	alpha: Float = 1f,
	modifier: Modifier = Modifier,
	onClick: () -> Unit
) = MiniImage(
	res = res,
	contentScale = contentScale,
	alignment = alignment,
	alpha = alpha,
	modifier = modifier.clickable(onClick = onClick)
)

@Composable
private fun rememberWebImageKeyUrl(uri: String, key: Any? = null): String = remember(uri, key) {
	if (key == null) uri
	else if (uri.contains("?")) "$uri&_cacheKey=$key"
	else "$uri?_cacheKey=$key"
}

@Composable
fun rememberWebImageState(
	quality: ImageQuality,
	background: Color? = MaterialTheme.colorScheme.scrim,
	isCrossfade: Boolean = true,
	animated: Boolean = true,
): AsyncImageState {
	val context = LocalPlatformContext.current
	val options = remember(quality, background, isCrossfade, animated) {
		ImageOptions.Builder().apply {
			sizeMultiplier(quality.sizeMultiplier)
			disallowAnimatedImage(!animated)
			pauseLoadWhenScrolling(true)
			background?.let { placeholder(ColorPainterStateImage(it)) }
			if (isCrossfade) crossfade()
		}.merge(SingletonSketch.get(context).globalImageOptions).build()
	}
	return rememberAsyncImageState(options)
}

@Stable
private class WebImageIndicator(
	density: Density,
	private val indicatorSize: Dp,
	private val indicatorColor: Color,
	private val indicatorImage: ImageBitmap
) : AbsProgressPainter(
	hiddenWhenIndeterminate = false,
	hiddenWhenCompleted = true,
	stepAnimationDuration = app.config.animationSpeed
) {
	override val intrinsicSize: Size = with(density) { Size(indicatorSize.toPx(), indicatorSize.toPx()) }

	override fun DrawScope.drawProgress(drawProgress: Float) {
		val widthRadius = size.width / 2f
		val heightRadius = size.height / 2f
		val radius = widthRadius.coerceAtMost(heightRadius)
		val cx = 0 + widthRadius
		val cy = 0 + heightRadius
		val center = Offset(widthRadius, heightRadius)
		val ringWidth = (indicatorSize * 0.1f).toPx()
		drawImage(
			image = indicatorImage,
			topLeft = Offset(x = intrinsicSize.width / 4, y = intrinsicSize.height / 4)
		)
		drawCircle(
			color = indicatorColor.copy(alpha = 0.25f),
			radius = radius,
			center = center,
			style = Stroke(ringWidth)
		)
		val sweepAngle = drawProgress * 360f
		drawArc(
			color = indicatorColor,
			startAngle = 270f,
			sweepAngle = sweepAngle,
			useCenter = false,
			topLeft = Offset(cx - radius, cy - radius),
			size = Size(radius * 2f, radius * 2f),
			style = Stroke(ringWidth, cap = StrokeCap.Round),
		)
	}
}

@Composable
private fun rememberWebImageIndicator(): WebImageIndicator {
	val density = LocalDensity.current
	val size = ThemeValue.Size.MediumInput
	val color = MaterialTheme.colorScheme.primaryContainer
	val imagePainter = painterResource(Res.drawable.placeholder_pic)
	val image = remember(imagePainter) {
		with(density) {
			val imageSize = (size / 2).roundToPx()
			imagePainter.toImageBitmap(imageSize, imageSize)
		}
	}
	return remember(density, size, color, image) {
		WebImageIndicator(density, size, color, image)
	}
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
	animated: Boolean = true,
	state: AsyncImageState = rememberWebImageState(quality, animated = animated),
	onClick: (() -> Unit)? = null
) {
	Box(modifier = modifier) {
		val progressIndicator = rememberWebImageIndicator()

		AsyncImage(
			uri = rememberWebImageKeyUrl(uri, key),
			contentDescription = null,
			state = state,
			alignment = alignment,
			contentScale = contentScale,
			filterQuality = quality.filterQuality,
			alpha = alpha,
			modifier = Modifier.matchParentSize().condition(circle) { clip(CircleShape) }
				.condition(onClick != null) { clickable(onClick = onClick ?: {}) }
				.progressIndicator(state, progressIndicator)
		)
	}
}

@Composable
fun LocalFileImage(
	path: () -> Path,
	vararg key: Any,
	modifier: Modifier = Modifier,
	circle: Boolean = false,
	contentScale: ContentScale = ContentScale.Fit,
	alpha: Float = 1f,
	animated: Boolean = true,
	state: AsyncImageState = rememberWebImageState(ImageQuality.Full, background = null, animated = animated),
	onClick: (() -> Unit)? = null
) {
	val baseUri = remember(*key) { path().toString() }
	val baseKey = remember(*key) { SystemFileSystem.metadataOrNull(path())?.size ?: 0L }
	Box(modifier = modifier) {
		AsyncImage(
			uri = rememberWebImageKeyUrl(baseUri, baseKey),
			contentDescription = null,
			state = state,
			alignment = Alignment.Center,
			contentScale = contentScale,
			filterQuality = ImageQuality.Full.filterQuality,
			alpha = alpha,
			modifier = Modifier.matchParentSize().condition(circle) { clip(CircleShape) }
				.condition(onClick != null) { clickable(onClick = onClick ?: {}) }
		)
	}
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
	state: AsyncImageState = rememberWebImageState(quality, isCrossfade = false)
) {
	Box(modifier = modifier) {
		val progressIndicator = rememberWebImageIndicator()

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
			modifier = Modifier.matchParentSize().progressIndicator(state, progressIndicator)
		)
	}
}