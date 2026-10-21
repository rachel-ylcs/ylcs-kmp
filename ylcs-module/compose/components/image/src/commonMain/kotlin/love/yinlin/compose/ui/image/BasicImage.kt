package love.yinlin.compose.ui.image

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.isActive
import love.yinlin.compose.Theme
import love.yinlin.compose.extension.rememberDerivedState
import love.yinlin.compose.extension.rememberState
import love.yinlin.compose.image.ImageManager
import love.yinlin.compose.image.data.*
import love.yinlin.compose.image.decoder.*
import love.yinlin.compose.image.input.ImageSource
import love.yinlin.compose.window.rememberOffScreenWindowState
import love.yinlin.coroutines.Coroutines
import love.yinlin.coroutines.cpuContext
import love.yinlin.extension.catchingError
import love.yinlin.extension.cleaning
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.roundToInt

private class AnimationCursor(private val frameCount: Int, private val repeatCount: Int) {
    var index = 0
        private set

    private var repeats = 0

    fun next(): Int? = when {
        index < frameCount - 1 -> index + 1
        repeatCount !in 0 .. repeats -> 0
        else -> null
    }

    fun advance(index: Int) {
        require(index == next())
        if (index == 0) repeats++
        this.index = index
    }
}

private class ImageSizeResolver {
    val sizes = MutableStateFlow<ImageLayoutSize?>(null)
    var constraints = Constraints()
}

private fun Modifier.resolveImageSize(resolver: ImageSizeResolver): Modifier = layout { measurable, constraints ->
    resolver.constraints = constraints
    val target = if (constraints.hasBoundedWidth && constraints.hasBoundedHeight) {
        IntSize(constraints.maxWidth, constraints.maxHeight)
    } else null
    resolver.sizes.value = ImageLayoutSize(target)
    val placeable = measurable.measure(constraints)
    layout(placeable.width, placeable.height) {
        placeable.place(0, 0)
    }
}

private fun imageContentRect(source: IntSize, viewport: IntSize, scale: ContentScale, alignment: Alignment, direction: LayoutDirection): Rect {
    if (source.width <= 0 || source.height <= 0 || viewport.width <= 0 || viewport.height <= 0) return Rect.Zero
    val factor = scale.computeScaleFactor(source.toSize(), viewport.toSize())
    val size = Size(source.width * factor.scaleX, source.height * factor.scaleY)
    val offset = alignment.align(size.roundToIntSize(), viewport, direction)
    return Rect(offset.toOffset(), size)
}

private fun Modifier.reserveImageSpace(placeholder: ImagePlaceholder): Modifier = layout { measurable, constraints ->
    val ratio = placeholder.aspectRatio ?: (placeholder.size.width / placeholder.size.height)
    var width = placeholder.size.width.roundToPx().coerceAtLeast(1)
    var height = placeholder.size.height.roundToPx().coerceAtLeast(1)
    if (constraints.hasFixedWidth && constraints.hasFixedHeight) {
        width = constraints.maxWidth
        height = constraints.maxHeight
    }
    else if (constraints.hasFixedWidth) {
        width = constraints.maxWidth
        height = (width.toDouble() / ratio).coerceIn(0.0, 32766.0).roundToInt()
    }
    else if (constraints.hasFixedHeight) {
        height = constraints.maxHeight
        width = (height.toDouble() * ratio).coerceIn(0.0, 32766.0).roundToInt()
    }
    else if (placeholder.aspectRatio != null) height = (width.toDouble() / ratio).coerceIn(1.0, 32766.0).roundToInt()
    width = constraints.constrainWidth(width)
    height = constraints.constrainHeight(height)
    val child = measurable.measure(Constraints.fixed(width, height))
    layout(width, height) { child.place(0, 0) }
}

@Composable
private fun rememberAnimatedImageFrame(result: ImageResult?, state: ImageState): ImageBitmap? {
    val animation = result?.animation ?: return result?.image
    var frame by rememberState(result) { result.image }
    val foreground by rememberOffScreenWindowState()
    val cursor = remember(result) { AnimationCursor(animation.durations.size, animation.repeatCount) }

    LaunchedEffect(result, state.isPlaying, foreground, state.visible) {
        if (!state.isPlaying || !foreground || !state.visible || cursor.next() == null) return@LaunchedEffect
        var decoder: ImageDecoder? = null
        val error = catchingError {
            cleaning({
                Coroutines.cpu {
                    // 在切回 UI 前记录所有权；取消恰好发生在切换上下文时也会执行 finally.close。
                    decoder = openImageDecoder(animation.bytes, animation.request)
                }
                var deadline = withFrameNanos { it } + animation.durations[cursor.index] * 1000000L
                while (true) {
                    val next = cursor.next() ?: break
                    val nextFrame = Coroutines.cpu { requireNotNull(decoder).frame(next) }
                    var now = withFrameNanos { it }
                    while (now < deadline) now = withFrameNanos { it }
                    frame = nextFrame
                    cursor.advance(next)
                    // 慢解码时继续按顺序显示，避免为了追赶时间忙循环或分配所有历史帧。
                    deadline = maxOf(deadline, now) + animation.durations[next] * 1000000L
                }
            }) {
                Coroutines.with(NonCancellable + cpuContext) { decoder?.close() }
            }
        }
        if (error != null) state.failure(ImageDecodeException(error))
    }
    return frame
}

private fun Modifier.imageZoomGestures(state: ImageZoomState, onTap: ((Offset) -> Unit)? = null, onInteract: () -> Unit): Modifier = this
    .focusable()
    .pointerInput(state, onTap) { detectTapGestures(onDoubleTap = state::toggle, onTap = onTap) }
    .pointerInput(state) {
        awaitEachGesture {
            awaitFirstDown(requireUnconsumed = false)
            onInteract()
            var accepted = false
            var accumulatedZoom = 1f
            var accumulatedPan = Offset.Zero
            do {
                val event = awaitPointerEvent()
                if (event.changes.any { it.isConsumed }) break
                val zoom = event.calculateZoom()
                val pan = event.calculatePan()
                val pivot = event.calculateCentroid(useCurrent = false)
                if (!accepted) {
                    accumulatedZoom *= zoom
                    accumulatedPan += pan
                    val zoomMotion = abs(1f - accumulatedZoom) * event.calculateCentroidSize(useCurrent = false)
                    accepted = zoomMotion > viewConfiguration.touchSlop ||
                            (state.scale > 1f && accumulatedPan.getDistance() > viewConfiguration.touchSlop)
                }
                if (accepted && state.transform(pivot, pan, zoom)) {
                    for (change in event.changes) {
                        if (change.positionChanged()) change.consume()
                    }
                }
            } while (event.changes.any { it.pressed })
        }
    }
    .pointerInput(state) {
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent()
                if (event.type == PointerEventType.Scroll) {
                    onInteract()
                    val change = event.changes.firstOrNull() ?: continue
                    val delta = change.scrollDelta.y
                    if (delta != 0f && state.transform(change.position, zoom = exp((-delta * 0.15f).coerceIn(-2f, 2f)))) change.consume()
                }
            }
        }
    }


@Composable
fun BasicImage(
    source: ImageSource?,
    modifier: Modifier = Modifier,
    state: ImageState = rememberImageState(),
    version: String = "",
    contentScale: ContentScale = ContentScale.Fit,
    alignment: Alignment = Alignment.Center,
    alpha: Float = 1f,
    onClick: (() -> Unit)? = null,
    contentDescription: String = "",
    placeholder: ImagePlaceholder = ImagePlaceholder.Default,
    options: ImageOptions = ImageOptions.Default,
) {
    val loader = if (source == null) null else ImageManager.loaderOrNull
    val resolver = remember { ImageSizeResolver() }
    val decodeScale by rememberDerivedState(state, options.zoom) {
        if (!options.zoom || state.isAnimated) 1f
        else when {
            state.zoom.scale > 4f -> 8f
            state.zoom.scale > 2f -> 4f
            state.zoom.scale > 1f -> 2f
            else -> 1f
        }
    }
    LaunchedEffect(source, version, loader, state, options, contentScale, decodeScale, state.reloadKey) {
        state.begin(if (loader == null) null else source, version)
        if (source == null || loader == null) return@LaunchedEffect
        resolver.sizes.filterNotNull().collectLatest { layout ->
            if (layout.target?.let { it.width == 0 || it.height == 0 } == true) return@collectLatest
            val target = when (contentScale) {
                ContentScale.None -> null
                ContentScale.FillWidth -> layout.target?.let { IntSize(it.width, Int.MAX_VALUE) }
                ContentScale.FillHeight -> layout.target?.let { IntSize(Int.MAX_VALUE, it.height) }
                else -> layout.target
            }
            val request = ImageRequest(source, version, target,
                options.quality.sizeMultiplier * decodeScale,
                contentScale == ContentScale.Crop || contentScale == ContentScale.FillBounds,
                options.animated)
            val retained = state.result
            if (retained != null && state.retainedRequest == request && state.retainedGeneration == loader.cacheGeneration) {
                state.success(retained)
                return@collectLatest
            }
            state.begin(source, version)
            val error = try {
                catchingError {
                    val generation = loader.cacheGeneration
                    val result = loader.load(request) { state.progress = it }
                    state.retainedRequest = request
                    state.retainedGeneration = generation
                    state.success(result)
                }
            }
            catch (error: CancellationException) {
                if (currentCoroutineContext().isActive) state.failure(error)
                throw error
            }
            if (error != null) state.failure(error)
        }
    }

    val result = state.result.takeIf { loader != null && state.source == source && state.version == version }
    val frame = rememberAnimatedImageFrame(result, state)
    val painter = remember(frame, options.quality) {
        frame?.let { BitmapPainter(it, filterQuality = options.quality.filterQuality) } ?: ColorPainter(Color.Transparent)
    }
    val fade = remember(source, version, state) { Animatable(1f) }
    var displayed by rememberState(source, version, state) { false }
    val fadeMillis = Theme.animation.duration.default
    LaunchedEffect(result) {
        if (result != null) {
            if (!displayed && result.origin != ImageOrigin.Memory) {
                fade.snapTo(0f)
                fade.animateTo(1f, tween(fadeMillis))
            }
            displayed = true
        }
    }

    val density = LocalDensity.current
    val direction = LocalLayoutDirection.current
    val viewport = state.size ?: IntSize.Zero
    val bounds = imageContentRect(result?.size ?: IntSize.Zero, viewport, contentScale, alignment, direction)
    SideEffect {
        state.zoom.updateBounds(viewport, bounds)
        state.updateContentBounds(bounds, viewport, options.zoom)
    }
    val focus = remember { FocusRequester() }
    val currentClick by rememberUpdatedState(onClick)
    val currentBounds by rememberUpdatedState(bounds)
    val clickAt: (Offset) -> Unit = remember(state) {
        { point ->
            val center = Offset(state.zoom.viewport.width / 2f, state.zoom.viewport.height / 2f)
            val original = (point - center - state.zoom.translation) / state.zoom.scale + center
            if (currentBounds.contains(original)) currentClick?.invoke()
        }
    }
    val interaction = if (options.zoom) Modifier.focusRequester(focus).imageZoomGestures(state.zoom, onTap = clickAt) { focus.requestFocus() } else Modifier
    val shape = if (options.circular) Modifier.clip(Theme.shape.circle) else Modifier
    val drawing = if (options.zoom) Modifier.graphicsLayer {
        scaleX = state.zoom.scale
        scaleY = state.zoom.scale
        translationX = state.zoom.translation.x
        translationY = state.zoom.translation.y
    } else Modifier
    Box(modifier.reserveImageSpace(placeholder).resolveImageSize(resolver)
        .onSizeChanged { state.size = it }.onGloballyPositioned {
            val root = it.findRootCoordinates().size
            state.visible = it.isAttached && !it.boundsInRoot().intersect(Rect(Offset.Zero, root.toSize())).isEmpty
        }.then(shape).clipToBounds()
        .background(placeholder.background ?: Color.Transparent).then(interaction)) {
        Image(painter, contentDescription, alignment = alignment, contentScale = contentScale,
            alpha = alpha * fade.value, colorFilter = options.colorFilter,
            modifier = Modifier.matchParentSize().then(drawing))
        if (!options.zoom && onClick != null && result != null) {
            val visible = bounds.intersect(Rect(Offset.Zero, viewport.toSize()))
            if (!visible.isEmpty) {
                Box(modifier = Modifier.offset { visible.topLeft.round() }
                    .size(with(density) { visible.width.toDp() }, with(density) { visible.height.toDp() })
                    .focusRequester(focus).focusable().pointerInput(Unit) {
                        detectTapGestures { point ->
                            if (point.x >= 0f && point.y >= 0f && point.x < size.width && point.y < size.height) {
                                focus.requestFocus()
                                currentClick?.invoke()
                            }
                        }
                    })
            }
        }
        when (state.status) {
            ImageLoadStatus.Loading -> if (result == null) placeholder.loading(this, state)
            ImageLoadStatus.Error -> placeholder.error(this, state)
            ImageLoadStatus.Empty -> placeholder.empty(this, state)
            ImageLoadStatus.Success -> { }
        }
    }
}
