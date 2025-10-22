package love.yinlin.compose.ui.floating

import androidx.compose.animation.*
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowRight
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import love.yinlin.compose.*
import love.yinlin.extension.catchingNull
import love.yinlin.platform.Coroutines
import love.yinlin.compose.ui.node.clickableNoRipple
import love.yinlin.compose.ui.image.MiniIcon
import love.yinlin.compose.ui.text.InputType
import love.yinlin.compose.ui.text.TextInput
import love.yinlin.compose.ui.text.TextInputState
import love.yinlin.compose.ui.input.ClickText
import love.yinlin.compose.ui.layout.LoadingBox
import love.yinlin.compose.screen.resources.Res
import love.yinlin.compose.screen.resources.*
import org.jetbrains.compose.resources.stringResource
import kotlin.coroutines.cancellation.CancellationException
import kotlin.coroutines.resume
import kotlin.math.roundToInt

@Stable
abstract class FloatingDialog<R> : Floating<Unit>() {
    override fun alignment(device: Device): Alignment = Alignment.Center
    override fun enter(device: Device, animationSpeed: Int): EnterTransition = scaleIn(
        animationSpec = tween(durationMillis = animationSpeed, easing = LinearOutSlowInEasing)
    ) + fadeIn(
        animationSpec = tween(durationMillis = animationSpeed, easing = LinearOutSlowInEasing)
    )
    override fun exit(device: Device, animationSpeed: Int): ExitTransition = scaleOut(
        animationSpec = tween(durationMillis = animationSpeed, easing = LinearOutSlowInEasing)
    ) + fadeOut(
        animationSpec = tween(durationMillis = animationSpeed, easing = LinearOutSlowInEasing)
    )
    override val zIndex: Float = Z_INDEX_DIALOG

    protected var continuation: CancellableContinuation<R?>? = null

    override fun close() {
        continuation?.resume(null)
        continuation = null
        super.close()
    }

    protected suspend fun awaitResult(): R? = catchingNull {
        val result = suspendCancellableCoroutine { cont ->
            cont.invokeOnCancellation {
                continuation = null
                super.close()
            }
            continuation?.cancel(CancellationException())
            continuation = cont
            super.open(Unit)
        }
        if (result != null) {
            continuation = null
            super.close()
        }
        result
    }

    @Composable
    override fun Wrapper(block: @Composable (() -> Unit)) {
        Box(modifier = Modifier.padding(LocalImmersivePadding.current)) {
            block()
        }
    }

    @Composable
    fun Land() {
        super.Land { _ -> }
    }
}

@Stable
abstract class FloatingBaseDialog<R> : FloatingDialog<R>() {
    open val scrollable: Boolean get() = true
    abstract val title: String?
    open val actions: @Composable (RowScope.() -> Unit)? = null

    @Composable
    override fun Wrapper(block: @Composable () -> Unit) {
        super.Wrapper {
            Box(
                modifier = Modifier.fillMaxSize().clickableNoRipple { if (dismissOnClickOutside) close() },
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = MaterialTheme.shapes.extraLarge,
                    modifier = Modifier.width(CustomTheme.size.dialogWidth).clickableNoRipple { }
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(
                            top = CustomTheme.padding.verticalExtraSpace * 2,
                            bottom = CustomTheme.padding.verticalExtraSpace,
                            start = CustomTheme.padding.horizontalExtraSpace,
                            end = CustomTheme.padding.horizontalExtraSpace
                        ),
                        verticalArrangement = Arrangement.spacedBy(CustomTheme.padding.verticalExtraSpace)
                    ) {
                        title?.let {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.fillMaxWidth().padding(horizontal = CustomTheme.padding.horizontalExtraSpace)
                            )
                        }

                        Box(modifier = Modifier
                            .heightIn(min = CustomTheme.size.minDialogContentHeight, max = CustomTheme.size.maxDialogContentHeight)
                            .fillMaxWidth()
                            .verticalScroll(enabled = scrollable, state = rememberScrollState())
                        ) {
                            block()
                        }
                        actions?.let {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(CustomTheme.padding.horizontalSpace, Alignment.End)
                            ) {
                                it()
                            }
                        }
                    }
                }
            }
        }
    }
}

@Stable
open class FloatingDialogInfo(
    title: String = "提示",
    content: String = ""
) : FloatingBaseDialog<Unit>() {
    override var title: String? by mutableStateOf(title)
        protected set
    var content: String by mutableStateOf(content)
        protected set

    suspend fun openSuspend(title: String = "提示", content: String) {
        this.title = title
        this.content = content
        awaitResult()
    }

    @Composable
    override fun Wrapper(block: @Composable () -> Unit) {
        super.Wrapper {
            Text(
                text = content,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Stable
open class FloatingDialogConfirm(
    title: String = "确认",
    content: String = ""
) : FloatingBaseDialog<Unit>() {
    override var title: String? by mutableStateOf(title)
        protected set
    var content: String by mutableStateOf(content)
        protected set

    override val actions: @Composable (RowScope.() -> Unit)? = {
        ClickText(
            text = stringResource(Res.string.dialog_yes),
            onClick = { continuation?.resume(Unit) }
        )
        ClickText(
            text = stringResource(Res.string.dialog_no),
            onClick = { close() }
        )
    }

    suspend fun openSuspend(title: String = "确认", content: String): Boolean {
        this.title = title
        this.content = content
        return awaitResult() != null
    }

    @Composable
    override fun Wrapper(block: @Composable () -> Unit) {
        super.Wrapper {
            Text(
                text = content,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Stable
open class FloatingDialogInput(
    val hint: String = "",
    val inputType: InputType = InputType.COMMON,
    val maxLength: Int = 0,
    val maxLines: Int = 1,
    val minLines: Int = maxLines,
    val clearButton: Boolean = maxLines == 1
) : FloatingBaseDialog<String>() {
    final override val scrollable: Boolean = false

    override val title: String? = null

    private val textInputState = TextInputState()

    override val actions: @Composable (RowScope.() -> Unit)? = {
        ClickText(
            text = stringResource(Res.string.dialog_ok),
            enabled = textInputState.ok,
            onClick = { continuation?.resume(textInputState.text) }
        )
        ClickText(
            text = stringResource(Res.string.dialog_cancel),
            onClick = { close() }
        )
    }

    suspend fun openSuspend(initText: String = ""): String? {
        textInputState.text = initText
        return awaitResult()
    }

    @Composable
    override fun Wrapper(block: @Composable () -> Unit) {
        super.Wrapper {
            val focusRequester = remember { FocusRequester() }

            LaunchedEffect(Unit) {
                focusRequester.requestFocus()
            }

            TextInput(
                state = textInputState,
                hint = hint,
                inputType = inputType,
                maxLength = maxLength,
                maxLines = maxLines,
                minLines = minLines,
                clearButton = clearButton,
                onImeClick = {
                    if (textInputState.ok) continuation?.resume(textInputState.text)
                },
                modifier = Modifier.fillMaxWidth().focusRequester(focusRequester)
            )
        }
    }
}

@Stable
abstract class FloatingDialogChoice(
    override val title: String? = null
) : FloatingBaseDialog<Int>() {
    abstract val num: Int
    @Composable
    abstract fun Name(index: Int)
    @Composable
    abstract fun Icon(index: Int)

    suspend fun openSuspend(): Int? = awaitResult()

    @Composable
    override fun Wrapper(block: @Composable (() -> Unit)) {
        super.Wrapper {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(CustomTheme.padding.verticalSpace)
            ) {
                repeat(num) { index ->
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable {
                            continuation?.resume(index)
                        }.padding(CustomTheme.padding.value),
                        horizontalArrangement = Arrangement.spacedBy(CustomTheme.padding.horizontalSpace),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(index)
                        Box(modifier = Modifier.weight(1f)) {
                            Name(index)
                        }
                    }
                }
            }
        }
    }

    companion object {
        fun fromItems(items: List<String>, title: String? = null) = object : ListDialogChoice(title) {
            override val num: Int = items.size
            override fun nameFactory(index: Int): String = items[index]
            override fun iconFactory(index: Int): ImageVector = Icons.AutoMirrored.Outlined.ArrowRight
        }

        fun fromIconItems(items: List<Pair<String, ImageVector>>, title: String? = null) = object : ListDialogChoice(title) {
            override val num: Int = items.size
            override fun nameFactory(index: Int): String = items[index].first
            override fun iconFactory(index: Int): ImageVector = items[index].second
        }
    }
}

@Stable
abstract class ListDialogChoice(title: String? = null) : FloatingDialogChoice(title) {
    abstract fun nameFactory(index: Int): String
    abstract fun iconFactory(index: Int): ImageVector

    @Composable
    override fun Name(index: Int) {
        Text(
            text = remember(index) { nameFactory(index) },
            modifier = Modifier.fillMaxWidth()
        )
    }

    @Composable
    override fun Icon(index: Int) {
        MiniIcon(
            icon = remember(index) { iconFactory(index) },
            size = CustomTheme.size.mediumIcon
        )
    }
}

@Stable
open class FloatingDialogDynamicChoice(title: String? = null) : ListDialogChoice(title) {
    private var items: List<String> = emptyList()
    override val num: Int get() = items.size
    override fun nameFactory(index: Int): String = items[index]
    override fun iconFactory(index: Int): ImageVector = Icons.AutoMirrored.Outlined.ArrowRight

    suspend fun openSuspend(items: List<String>): Int? = if (items.isNotEmpty()) {
        this.items = items
        awaitResult()
    } else null
}

@Stable
open class FloatingDialogProgress : FloatingBaseDialog<Unit>() {
    var current by mutableStateOf("0")
    var total by mutableStateOf("0")
    var progress by mutableFloatStateOf(0f)

    final override val dismissOnBackPress: Boolean get() = false
    final override val dismissOnClickOutside: Boolean get() = false
    final override val scrollable: Boolean get() = false

    override val title: String? = "下载中..."

    override val actions: @Composable (RowScope.() -> Unit)? = {
        ClickText(
            text = stringResource(Res.string.dialog_cancel),
            enabled = isOpen,
            onClick = { close() }
        )
    }

    suspend fun openSuspend() {
        current = "0"
        total = "0"
        progress = 0f
        Coroutines.startCurrent { awaitResult() }
    }

    @Composable
    override fun Wrapper(block: @Composable () -> Unit) {
        super.Wrapper {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(CustomTheme.padding.verticalSpace)
            ) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().height(CustomTheme.size.progressHeight),
                    gapSize = CustomTheme.padding.zeroSpace,
                    drawStopIndicator = {}
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(CustomTheme.padding.horizontalExtraSpace),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${(progress * 100).roundToInt()}%",
                        textAlign = TextAlign.Start,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "$current / $total",
                        textAlign = TextAlign.End,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Stable
class FloatingDialogLoading : FloatingDialog<Unit>() {
    override val dismissOnBackPress: Boolean = false
    override val dismissOnClickOutside: Boolean = false

    suspend fun openSuspend() {
        Coroutines.startCurrent { awaitResult() }
    }

    @Composable
    override fun Wrapper(block: @Composable (() -> Unit)) {
        super.Wrapper {
            Surface(
                shape = MaterialTheme.shapes.extraLarge,
                shadowElevation = CustomTheme.shadow.surface,
                modifier = Modifier.size(CustomTheme.size.dialogWidth)
            ) {
                LoadingBox()
            }
        }
    }
}