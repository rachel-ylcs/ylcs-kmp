package love.yinlin.compose.ui.image

import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.*
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
import androidx.compose.ui.graphics.Paint
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
import androidx.compose.ui.node.ObserverModifierNode
import androidx.compose.ui.node.observeReads
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.node.invalidateMeasurement
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.ViewConfiguration
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.*
import kotlinx.coroutines.*
import love.yinlin.compose.Theme
import love.yinlin.compose.data.ImageQuality
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
import love.yinlin.coroutines.waitContext
import love.yinlin.extension.catchingError
import love.yinlin.extension.cleaning
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds

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

private class ImageNode(
    var state: ImageState,
    var loader: ImageLoader?,
    var placeholder: ImagePlaceholder,
    var geometry: ImageGeometry,
    var decodeScale: Float,
    var quality: ImageQuality,
    var animated: Boolean,
    var hardwareAllowed: Boolean,
    var reloadKey: Int,
    var alpha: Float,
    var colorFilter: ColorFilter?,
    var filterQuality: FilterQuality,
    var bitmapContent: Boolean,
    var fadeIn: Boolean,
    var fadeMillis: Int,
) : Modifier.Node(), LayoutModifierNode, DrawModifierNode, ObserverModifierNode {
    // 绘制参数变化不应使布局失效；合并节点后分别调度测量与重绘
    override val shouldAutoInvalidate: Boolean get() = false

    private var pending: Job? = null
    private var requested = true
    private var configured = true
    private var layoutData: ImageLayoutData? = null
    private var result: ImageResult? = null
    private var viewport = IntSize.Zero
    private var viewportState: ImageState? = null
    private var bounds = Rect.Zero
    private var fading: Job? = null
    private var fadeAlpha = 1f
    private var fadePaint: Paint? = null

    private fun resetDrawing() {
        fading?.cancel()
        fading = null
        result = null
        viewportState = null
        bounds = Rect.Zero
        fadeAlpha = 1f
        fadePaint = null
    }

    private fun prepare() {
        pending?.cancel()
        pending = null
        requested = true
        state.updateGeometry(geometry)
        state.begin(if (loader == null) null else geometry.source, geometry.version)
    }

    override fun onAttach() {
        if (configured) {
            prepare()
            observeContent()
        }
        invalidateMeasurement()
    }

    override fun onDetach() {
        pending?.cancel()
        pending = null
        requested = true
        resetDrawing()
    }

    override fun onReset() {
        pending?.cancel()
        pending = null
        requested = true
        configured = false
        resetDrawing()
    }

    fun update(element: ImageLayoutData): Boolean {
        // alpha / 滤色变化时复用布局参数，不重复读取状态或运行加载配置判断
        if (configured && layoutData === element) return false
        layoutData = element

        val layoutChanged = !configured || state !== element.state || loader !== element.loader ||
                placeholder != element.placeholder || geometry != element.geometry || decodeScale != element.decodeScale ||
                quality != element.quality || animated != element.animated || hardwareAllowed != element.hardwareAllowed || reloadKey != element.reloadKey

        val reload = !configured || state !== element.state || loader !== element.loader ||
                geometry.source != element.geometry.source || geometry.version != element.geometry.version ||
                geometry.contentScale != element.geometry.contentScale || decodeScale != element.decodeScale ||
                quality != element.quality || animated != element.animated || hardwareAllowed != element.hardwareAllowed || reloadKey != element.reloadKey

        state = element.state
        loader = element.loader
        placeholder = element.placeholder
        geometry = element.geometry
        decodeScale = element.decodeScale
        quality = element.quality
        animated = element.animated
        hardwareAllowed = element.hardwareAllowed
        reloadKey = element.reloadKey
        configured = true

        if (isAttached) {
            if (reload) prepare()
            else state.updateGeometry(geometry)
            if (layoutChanged) invalidateMeasurement()
        }
        return reload
    }

    private fun load(size: IntSize) {
        val source = geometry.source ?: return
        val loader = loader ?: return

        val state = state
        val version = geometry.version
        val contentScale = geometry.contentScale

        val scaled = IntSize((size.width * decodeScale).roundToInt(), (size.height * decodeScale).roundToInt())
        val target = when (contentScale) {
            ContentScale.None -> null
            ContentScale.FillWidth -> IntSize(scaled.width, Int.MAX_VALUE)
            ContentScale.FillHeight -> IntSize(Int.MAX_VALUE, scaled.height)
            else -> scaled
        }
        val request = ImageRequest(source, version, target, contentScale == ContentScale.Crop || contentScale == ContentScale.FillBounds, animated, quality = quality, hardwareAllowed = hardwareAllowed)

        val retained = state.result
        val retainedRequest = state.retainedRequest
        if (retained != null && retainedRequest != null && request.sameContent(retainedRequest) &&
            state.retainedGeneration == loader.cacheGeneration && request.canReuse(retained)) {
            pending?.cancel()
            pending = null
            if (state.status != ImageLoadStatus.Success) state.success(retained)
            return
        }
        pending?.cancel()
        pending = null
        state.begin(source, version)

        pending = coroutineScope.launch(start = CoroutineStart.UNDISPATCHED) {
            val error = try {
                catchingError {
                    val generation = loader.cacheGeneration
                    val result = coroutineScope {
                        val feedback = launch {
                            delay(loader.configuration.loadingDelayMillis.milliseconds)
                            if (state.result == null) state.feedbackStatus = ImageLoadStatus.Loading
                        }
                        cleaning({ loader.load(request) { state.progress = it } }, feedback::cancel)
                    }
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

    override fun MeasureScope.measure(measurable: Measurable, constraints: Constraints): MeasureResult {
        val target = imageLayoutSize(placeholder, constraints)
        if (configured) {
            val resized = viewport != target || viewportState !== state
            viewport = target
            viewportState = state
            if (resized) state.updateSize(target)
            // 每次请求只解析首个有效布局尺寸；后续布局变化仅缩放已有结果，不重启加载。
            if (requested && target.width > 0 && target.height > 0) {
                requested = false
                load(target)
            }
            if (resized) observeContent()
        }

        val child = measurable.measure(Constraints.fixed(target.width, target.height))
        return layout(target.width, target.height) { child.place(0, 0) }
    }

    override fun IntrinsicMeasureScope.minIntrinsicWidth(measurable: IntrinsicMeasurable, height: Int): Int = 0
    override fun IntrinsicMeasureScope.maxIntrinsicWidth(measurable: IntrinsicMeasurable, height: Int): Int = 0
    override fun IntrinsicMeasureScope.minIntrinsicHeight(measurable: IntrinsicMeasurable, width: Int): Int = 0
    override fun IntrinsicMeasureScope.maxIntrinsicHeight(measurable: IntrinsicMeasurable, width: Int): Int = 0

    override fun onObservedReadsChanged() = observeContent()

    fun observeContent() {
        observeReads {
            val current = state.activeResult(geometry.source, geometry.version, geometry.ready)
            // 静态几何仅随结果、布局和配置变化；每帧绘制直接使用缓存，不再重复读取快照状态
            bounds = if (bitmapContent && current != null) geometry.bounds(current.size, viewport) else Rect.Zero
            if (result !== current || !fadeIn) {
                fading?.cancel()
                fading = null
                fadeAlpha = 1f
                result = current

                if (current != null && state.consumeFade(current) && fadeIn) {
                    fadeAlpha = 0f
                    fading = coroutineScope.launch {
                        animate(0f, 1f, animationSpec = tween(fadeMillis)) { value, _ ->
                            fadeAlpha = value
                            invalidateDraw()
                        }
                        fadeAlpha = 1f
                        fading = null
                        fadePaint = null
                        invalidateDraw()
                    }
                }
            }
        }
        invalidateDraw()
    }

    override fun ContentDrawScope.draw() {
        clipRect {
            if (bitmapContent) {
                val result = this@ImageNode.result
                if (result != null && alpha > 0f) {
                    val bounds = this@ImageNode.bounds
                    if (!bounds.isEmpty) {
                        drawImage(
                            image = result.image,
                            dstOffset = bounds.topLeft.round(),
                            dstSize = bounds.size.roundToIntSize(),
                            alpha = alpha * fadeAlpha,
                            colorFilter = colorFilter,
                            filterQuality = filterQuality
                        )
                    }
                }
            }
            if (!bitmapContent && fadeAlpha < 1f) {
                val paint = fadePaint ?: Paint().also { fadePaint = it }
                paint.alpha = fadeAlpha
                val canvas = drawContext.canvas
                canvas.saveLayer(Rect(Offset.Zero, size), paint)
                cleaning({ this@draw.drawContent() }, canvas::restore)
            }
            else this@draw.drawContent()
        }
    }
}

@Stable
private data class ImageLayoutData(
    val state: ImageState,
    val loader: ImageLoader?,
    val placeholder: ImagePlaceholder,
    val geometry: ImageGeometry,
    val decodeScale: Float,
    val quality: ImageQuality,
    val animated: Boolean,
    val hardwareAllowed: Boolean,
    val reloadKey: Int,
)

private data class ImageElement(
    val layout: ImageLayoutData,
    val alpha: Float,
    val colorFilter: ColorFilter?,
    val filterQuality: FilterQuality,
    val bitmapContent: Boolean,
    val fadeIn: Boolean,
    val fadeMillis: Int,
) : ModifierNodeElement<ImageNode>() {
    override fun create(): ImageNode = ImageNode(layout.state, layout.loader, layout.placeholder, layout.geometry,
        layout.decodeScale, layout.quality, layout.animated, layout.hardwareAllowed, layout.reloadKey,
        alpha, colorFilter, filterQuality, bitmapContent, fadeIn, fadeMillis)

    override fun update(node: ImageNode) {
        val contentChanged = node.state !== layout.state || node.geometry != layout.geometry || node.fadeIn != fadeIn || node.bitmapContent != bitmapContent
        val reload = node.update(layout)
        node.alpha = alpha
        node.colorFilter = colorFilter
        node.filterQuality = filterQuality
        node.bitmapContent = bitmapContent
        node.fadeIn = fadeIn
        node.fadeMillis = fadeMillis
        if (node.isAttached && (contentChanged || reload)) node.observeContent()
        else if (node.isAttached) node.invalidateDraw()
    }

    override fun InspectorInfo.inspectableProperties() = inspector("image")
}

@Composable
private fun rememberAnimatedImageFrame(result: ImageResult, state: ImageState): State<ImageBitmap> {
    val animation = requireNotNull(result.animation)
    val frame = rememberState(result) { result.image }
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
                    while (now < deadline) {
                        val remaining = (deadline - now) / 1000000L
                        if (remaining > 16L) delay((remaining - 16L).milliseconds)
                        now = withFrameNanos { it }
                    }
                    frame.value = nextFrame
                    cursor.advance(next)
                    deadline = maxOf(deadline, now) + animation.durations[next] * 1000000L
                }
            }) {
                Coroutines.with(waitContext, cpuContext) { decoder?.close() }
            }
        }

        if (error != null) state.failure(ImageDecodeException(error))
    }
    return frame
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
                        Coroutines.wait {
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

    return Modifier.focusRequester(focus).imageZoomGestures(state, interactionSource, onTap) { focus.requestFocus() }
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
    val platform = if (result?.isAnimated == true) rememberPlatformAnimatedPainter(result, state) else null
    val frame = if (result?.isAnimated == true && platform == null) rememberAnimatedImageFrame(result, state) else null
    val painter = platform ?: remember(result, frame, options.quality) {
        val filterQuality = ImageQualityPolicy.from(options.quality).filterQuality
        if (frame != null) AnimatedImagePainter(frame, requireNotNull(result).size, filterQuality)
        else result?.image?.let { BitmapPainter(it, filterQuality = filterQuality) } ?: ColorPainter(Color.Transparent)
    }
    val zoom = if (options.zoom) state.zoom else null
    val drawing = if (zoom != null) Modifier.graphicsLayer {
        scaleX = zoom.scale
        scaleY = scaleX
        translationX = zoom.translation.x
        translationY = zoom.translation.y
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
    val animated = options.animated
    val hardwareAllowed = imageHardwareAllowed()
    val direction = LocalLayoutDirection.current
    val geometry = remember(source, version, loader != null, contentScale, alignment, direction, options.zoom) {
        ImageGeometry(source, version, loader != null, contentScale, alignment, direction, options.zoom)
    }
    val reloadKey = state.reloadKey
    val layout = remember(state, loader, placeholder, geometry, decodeScale, options.quality, animated, hardwareAllowed, reloadKey) {
        ImageLayoutData(state, loader, placeholder, geometry, decodeScale, options.quality, animated, hardwareAllowed, reloadKey)
    }
    val animatedResult = state.isAnimated && state.activeResult(source, version, loader != null) != null
    val interaction = if (options.zoom) rememberImageZoomModifier(state, onClick) else Modifier
    val visibility = if (animatedResult) rememberImageVisibilityModifier(state) else Modifier
    val shape = if (options.circular) Modifier.clip(Theme.shape.circle) else Modifier
    val separateContent = options.zoom || animatedResult
    val clipping = if (separateContent) Modifier.clipToBounds() else Modifier
    val semantics = remember(contentDescription, separateContent) {
        if (!separateContent && contentDescription.isNotEmpty()) Modifier.semantics {
            this.contentDescription = contentDescription
            role = Role.Image
        } else Modifier
    }
    val drawing = ImageElement(layout, alpha, options.colorFilter, ImageQualityPolicy.from(options.quality).filterQuality,
        !separateContent, options.fadeIn, if (options.fadeIn) Theme.animation.duration.default else 0)

    val overlayClick = if (options.zoom) null else onClick
    val overlays: @Composable BoxScope.() -> Unit = remember(state, geometry, placeholder, overlayClick) {
        {
            if (overlayClick != null) ImageClickOverlay(state, geometry, overlayClick)
            ImageFeedback(state, geometry, placeholder)
        }
    }

    // 静态图的 alpha / 滤色变化只更新绘制节点，不使占位与点击内容的组合失效
    val content: @Composable BoxScope.() -> Unit = if (separateContent) {
        {
            ImageContent(state, geometry, contentDescription, alpha, options)
            overlays()
        }
    } else overlays

    Box(
        modifier = modifier.then(visibility).then(shape).then(clipping).then(interaction).then(semantics).then(drawing),
        content = content
    )
}
