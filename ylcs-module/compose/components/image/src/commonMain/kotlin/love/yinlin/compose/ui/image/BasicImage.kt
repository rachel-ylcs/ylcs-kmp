package love.yinlin.compose.ui.image

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.BoxScope
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.*
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.invalidateMeasurement
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.ViewConfiguration
import androidx.compose.ui.unit.*
import kotlinx.coroutines.*
import love.yinlin.compose.Theme
import love.yinlin.compose.extension.rememberDerivedState
import love.yinlin.compose.extension.rememberState
import love.yinlin.compose.image.ImageLoader
import love.yinlin.compose.image.ImageManager
import love.yinlin.compose.image.data.*
import love.yinlin.compose.image.decoder.*
import love.yinlin.compose.image.input.ImageSource
import love.yinlin.compose.platform.inspector
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

private fun Density.imageLayoutSize(placeholder: ImagePlaceholder, constraints: Constraints): IntSize {
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

    return IntSize(constraints.constrainWidth(width), constraints.constrainHeight(height))
}

private class ImageLayoutNode(
    var state: ImageState,
    var loader: ImageLoader?,
    var placeholder: ImagePlaceholder,
    var geometry: ImageGeometry,
    var multiplier: Float,
    var animated: Boolean,
    var reloadKey: Int,
) : Modifier.Node(), LayoutModifierNode {
    private var pending: Job? = null
    private var layout: ImageLayoutSize? = null
    private var requested = true
    private var configured = true

    private fun prepare() {
        pending?.cancel()
        pending = null
        requested = true
        state.updateGeometry(geometry)
        state.begin(if (loader == null) null else geometry.source, geometry.version)
    }

    override fun onAttach() {
        if (configured) prepare()
        invalidateMeasurement()
    }

    override fun onDetach() {
        pending?.cancel()
        pending = null
        requested = true
    }

    override fun onReset() {
        pending?.cancel()
        pending = null
        layout = null
        requested = true
        configured = false
    }

    fun update(element: ImageLayoutElement) {
        val reload = !configured || state !== element.state || loader !== element.loader ||
            geometry.source != element.geometry.source || geometry.version != element.geometry.version ||
            geometry.contentScale != element.geometry.contentScale || multiplier != element.multiplier ||
            animated != element.animated || reloadKey != element.reloadKey

        state = element.state
        loader = element.loader
        placeholder = element.placeholder
        geometry = element.geometry
        multiplier = element.multiplier
        animated = element.animated
        reloadKey = element.reloadKey
        configured = true

        if (isAttached) {
            if (reload) prepare()
            else state.updateGeometry(geometry)
        }
    }

    private fun load(size: IntSize) {
        pending?.cancel()
        pending = null

        if (size.width == 0 || size.height == 0) return
        val source = geometry.source ?: return
        val loader = loader ?: return

        val state = state
        val version = geometry.version
        val contentScale = geometry.contentScale

        val target = when (contentScale) {
            ContentScale.None -> null
            ContentScale.FillWidth -> IntSize(size.width, Int.MAX_VALUE)
            ContentScale.FillHeight -> IntSize(Int.MAX_VALUE, size.height)
            else -> size
        }
        val request = ImageRequest(source, version, target, multiplier, contentScale == ContentScale.Crop || contentScale == ContentScale.FillBounds, animated)

        val retained = state.result
        if (retained != null && state.retainedRequest == request && state.retainedGeneration == loader.cacheGeneration) {
            state.success(retained)
            return
        }
        state.begin(source, version)

        pending = coroutineScope.launch(start = CoroutineStart.UNDISPATCHED) {
            currentCoroutineContext().ensureActive()
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

        if (pending?.isCompleted == false && state.result == null) state.feedbackStatus = ImageLoadStatus.Loading
    }

    override fun MeasureScope.measure(measurable: Measurable, constraints: Constraints): MeasureResult {
        val target = imageLayoutSize(placeholder, constraints)
        if (configured) {
            state.updateSize(target)
            if (requested || layout?.target != target) {
                requested = false
                layout = ImageLayoutSize(target)
                load(target)
            }
        }

        val child = measurable.measure(Constraints.fixed(target.width, target.height))
        return layout(target.width, target.height) { child.place(0, 0) }
    }

    override fun IntrinsicMeasureScope.minIntrinsicWidth(measurable: IntrinsicMeasurable, height: Int): Int = 0
    override fun IntrinsicMeasureScope.maxIntrinsicWidth(measurable: IntrinsicMeasurable, height: Int): Int = 0
    override fun IntrinsicMeasureScope.minIntrinsicHeight(measurable: IntrinsicMeasurable, width: Int): Int = 0
    override fun IntrinsicMeasureScope.maxIntrinsicHeight(measurable: IntrinsicMeasurable, width: Int): Int = 0
}

private data class ImageLayoutElement(
    val state: ImageState,
    val loader: ImageLoader?,
    val placeholder: ImagePlaceholder,
    val geometry: ImageGeometry,
    val multiplier: Float,
    val animated: Boolean,
    val reloadKey: Int,
) : ModifierNodeElement<ImageLayoutNode>() {
    override fun create(): ImageLayoutNode = ImageLayoutNode(state, loader, placeholder, geometry, multiplier, animated, reloadKey)

    override fun update(node: ImageLayoutNode) = node.update(this)

    override fun InspectorInfo.inspectableProperties() = inspector("imageLayout")
}

private class ImageDrawNode(
    var state: ImageState,
    var geometry: ImageGeometry,
    var alpha: Float,
    var colorFilter: ColorFilter?,
    var quality: FilterQuality,
    var description: String,
    var bitmapContent: Boolean,
) : Modifier.Node(), DrawModifierNode {
    override fun ContentDrawScope.draw() {
        clipRect {
            if (bitmapContent) {
                val result = state.activeResult(geometry.source, geometry.version, geometry.ready)
                if (result != null && alpha > 0f) {
                    val bounds = state.contentRect
                    if (!bounds.isEmpty) {
                        drawImage(
                            image = result.image,
                            dstOffset = bounds.topLeft.round(),
                            dstSize = bounds.size.roundToIntSize(),
                            alpha = alpha,
                            colorFilter = colorFilter,
                            filterQuality = quality
                        )
                    }
                }
            }
            this@draw.drawContent()
        }
    }
}

private data class ImageDrawElement(
    val state: ImageState,
    val geometry: ImageGeometry,
    val alpha: Float,
    val colorFilter: ColorFilter?,
    val quality: FilterQuality,
    val description: String,
    val bitmapContent: Boolean,
) : ModifierNodeElement<ImageDrawNode>() {
    override fun create(): ImageDrawNode = ImageDrawNode(state, geometry, alpha, colorFilter, quality, description, bitmapContent)

    override fun update(node: ImageDrawNode) {
        node.state = state
        node.geometry = geometry
        node.alpha = alpha
        node.colorFilter = colorFilter
        node.quality = quality
        node.description = description
        node.bitmapContent = bitmapContent
    }

    override fun InspectorInfo.inspectableProperties() = inspector("imageContent")
}

@Composable
private fun rememberAnimatedImageFrame(result: ImageResult, state: ImageState): ImageBitmap {
    val animation = result.animation ?: return result.image
    var frame by rememberState(result) { result.image }
    val foreground by rememberOffScreenWindowState()
    val cursor = remember(result) { AnimationCursor(animation.durations.size, animation.repeatCount) }

    LaunchedEffect(result, state.isPlaying, foreground, state.visible) {
        if (!state.isPlaying || !foreground || !state.visible || cursor.next() == null) return@LaunchedEffect
        var decoder: ImageDecoder? = null
        val error = catchingError {
            cleaning({
                Coroutines.cpu {
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

@Composable
private fun rememberImageFade(source: ImageSource?, version: String?, state: ImageState, result: ImageResult?): State<Float>? {
    val displayed = remember(source, version, state) { result != null }
    if (displayed || result == null) return null

    val fade = remember(source, version, state) {
        if (result.origin == ImageOrigin.Memory) null else Animatable(0f)
    } ?: return null

    val fadeMillis = Theme.animation.duration.default

    LaunchedEffect(result) {
        if (fade.targetValue == 1f) fade.snapTo(1f)
        else fade.animateTo(1f, tween(fadeMillis))
    }

    return fade.asState()
}

@Composable
private fun rememberImageDecodeScale(state: ImageState): Float {
    if (state.isAnimated) return 1f
    val zoom = state.zoom
    val scale by rememberDerivedState(zoom) {
        when {
            zoom.scale > 4f -> 8f
            zoom.scale > 2f -> 4f
            zoom.scale > 1f -> 2f
            else -> 1f
        }
    }
    return scale
}

@Suppress("ModifierFactoryExtensionFunction")
@Composable
private fun rememberImageVisibilityModifier(state: ImageState): Modifier {
    DisposableEffect(state) {
        onDispose { state.visible = false }
    }

    return Modifier.onGloballyPositioned {
        val root = it.findRootCoordinates().size
        state.visible = it.isAttached && !it.boundsInRoot().intersect(Rect(Offset.Zero, root.toSize())).isEmpty
    }
}

@Composable
private fun rememberImageTap(state: ImageState, onClick: () -> Unit): (Offset) -> Unit {
    val currentClick by rememberUpdatedState(onClick)

    return remember(state) {
        { point ->
            if (state.contentRect.contains(point)) currentClick()
        }
    }
}



private fun Modifier.imageZoomGestures(state: ImageState, interactionSource: MutableInteractionSource, onTap: ((Offset) -> Unit)? = null, onInteract: () -> Unit): Modifier = this
    .focusable(interactionSource = interactionSource)
    .pointerInput(state, onTap, interactionSource) {
        detectTapGestures(
            onDoubleTap = state.zoom::toggle,
            onPress = { position ->
                if (onTap != null && state.contentRect.contains(position)) {
                    val press = PressInteraction.Press(position)
                    var released = false

                    cleaning({
                        interactionSource.emit(press)
                        released = tryAwaitRelease()
                    }) {
                        Coroutines.with(NonCancellable) {
                            interactionSource.emit(if (released) PressInteraction.Release(press) else PressInteraction.Cancel(press))
                        }
                    }
                }
            },
            onTap = onTap
        )
    }.pointerInput(state) {
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

                    accepted = zoomMotion > viewConfiguration.touchSlop || (state.zoom.scale > 1f && accumulatedPan.getDistance() > viewConfiguration.touchSlop)
                }

                if (accepted && state.zoom.transform(pivot, pan, zoom)) {
                    for (change in event.changes) {
                        if (change.positionChanged()) change.consume()
                    }
                }
            } while (event.changes.any { it.pressed })
        }
    }.pointerInput(state) {
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent()

                if (event.type == PointerEventType.Scroll) {
                    onInteract()

                    val change = event.changes.firstOrNull() ?: continue
                    val delta = change.scrollDelta.y

                    if (delta != 0f && state.zoom.transform(pivot = change.position, zoom = exp((-delta * 0.15f).coerceIn(-2f, 2f)))) {
                        change.consume()
                    }
                }
            }
        }
    }

@Suppress("ModifierFactoryExtensionFunction")
@Composable
private fun rememberImageZoomModifier(state: ImageState, onClick: (() -> Unit)?): Modifier {
    val focus = remember { FocusRequester() }
    val interactionSource = remember { MutableInteractionSource() }
    val onTap = if (onClick != null) rememberImageTap(state, onClick) else null

    return Modifier.focusRequester(focus)
        .indication(interactionSource, LocalIndication.current)
        .imageZoomGestures(state, interactionSource, onTap) { focus.requestFocus() }
}

@Composable
private fun ImageClickOverlay(state: ImageState, geometry: ImageGeometry, onClick: () -> Unit) {
    if (state.activeResult(geometry.source, geometry.version, geometry.ready) == null) return

    val bounds = state.contentRect
    val viewport = state.contentViewport
    val visible = bounds.intersect(Rect(Offset.Zero, viewport.toSize()))
    if (visible.isEmpty) return

    val density = LocalDensity.current
    val width = with(density) { visible.width.toDp() }
    val height = with(density) { visible.height.toDp() }

    val viewConfiguration = LocalViewConfiguration.current
    val touchTargetSize = DpSize(
        minOf(viewConfiguration.minimumTouchTargetSize.width, width),
        minOf(viewConfiguration.minimumTouchTargetSize.height, height)
    )
    val boundedConfiguration = remember(viewConfiguration, touchTargetSize) {
        object : ViewConfiguration by viewConfiguration {
            override val minimumTouchTargetSize = touchTargetSize
        }
    }

    CompositionLocalProvider(LocalViewConfiguration provides boundedConfiguration) {
        Box(modifier = Modifier
            .offset { visible.topLeft.round() }
            .size(width, height)
            .clipToBounds()
            .clickable(onClick = onClick)
        )
    }
}

@Composable
private fun BoxScope.ImageContent(state: ImageState, geometry: ImageGeometry, contentDescription: String, alpha: Float, options: ImageOptions) {
    val result = state.activeResult(geometry.source, geometry.version, geometry.ready)
    val frame = if (result?.isAnimated == true) rememberAnimatedImageFrame(result, state) else result?.image
    val painter = remember(frame, options.quality) {
        frame?.let { BitmapPainter(it, filterQuality = options.quality.filterQuality) } ?: ColorPainter(Color.Transparent)
    }
    val fade = if (options.fadeIn) rememberImageFade(geometry.source, geometry.version, state, result) else null
    val zoom = if (options.zoom) state.zoom else null
    val drawing = if (zoom != null || fade != null) Modifier.graphicsLayer {
        this.alpha = fade?.value ?: 1f
        scaleX = zoom?.scale ?: 1f
        scaleY = scaleX
        translationX = zoom?.translation?.x ?: 0f
        translationY = zoom?.translation?.y ?: 0f
    } else Modifier

    Image(
        painter = painter,
        contentDescription = contentDescription,
        alignment = geometry.alignment,
        contentScale = geometry.contentScale,
        alpha = alpha,
        colorFilter = options.colorFilter,
        modifier = Modifier.matchParentSize().then(drawing)
    )
}

@Composable
private fun BoxScope.ImageFeedback(state: ImageState, geometry: ImageGeometry, placeholder: ImagePlaceholder) {
    val status = if (geometry.source == null || !geometry.ready) ImageLoadStatus.Empty else state.feedbackStatus ?: return

    Box(Modifier.matchParentSize().clipToBounds()) {
        when (status) {
            ImageLoadStatus.Loading -> placeholder.loading(this, state)
            ImageLoadStatus.Error -> placeholder.error(this, state)
            ImageLoadStatus.Empty -> placeholder.empty(this, state)
            else -> { }
        }
    }
}

@Composable
fun BasicImage(
    source: ImageSource?,
    modifier: Modifier = Modifier,
    state: ImageState = rememberImageState(),
    version: String? = null,
    contentScale: ContentScale = ContentScale.Fit,
    alignment: Alignment = Alignment.Center,
    alpha: Float = 1f,
    onClick: (() -> Unit)? = null,
    contentDescription: String = "",
    placeholder: ImagePlaceholder = ImagePlaceholder.Default,
    options: ImageOptions = ImageOptions.Default,
) {
    val loader = if (source == null) null else ImageManager.loaderOrNull
    val decodeScale = if (options.zoom) rememberImageDecodeScale(state) else 1f
    val multiplier = options.quality.sizeMultiplier * decodeScale
    val animated = options.animated
    val geometry = ImageGeometry(source, version, loader != null, contentScale, alignment, LocalLayoutDirection.current, options.zoom)
    val animatedResult = state.isAnimated && state.activeResult(source, version, loader != null) != null
    val interaction = if (options.zoom) rememberImageZoomModifier(state, onClick) else Modifier
    val visibility = if (animatedResult) rememberImageVisibilityModifier(state) else Modifier
    val shape = if (options.circular) Modifier.clip(Theme.shape.circle) else Modifier
    val separateContent = options.zoom || options.fadeIn || animatedResult
    val clipping = if (separateContent) Modifier.clipToBounds() else Modifier
    val drawing = ImageDrawElement(state, geometry, alpha, options.colorFilter, options.quality.filterQuality, contentDescription, !separateContent)

    Box(
        modifier = modifier.then(ImageLayoutElement(state, loader, placeholder, geometry, multiplier, animated, state.reloadKey))
            .then(visibility).then(shape).then(clipping).then(interaction).then(drawing)
    ) {
        if (separateContent) ImageContent(state, geometry, contentDescription, alpha, options)

        if (!options.zoom && onClick != null) ImageClickOverlay(state, geometry, onClick)

        ImageFeedback(state, geometry, placeholder)
    }
}
