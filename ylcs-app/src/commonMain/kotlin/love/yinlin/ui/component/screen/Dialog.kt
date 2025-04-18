package love.yinlin.ui.component.screen

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowRight
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import love.yinlin.common.Colors
import love.yinlin.platform.app
import love.yinlin.resources.*
import love.yinlin.ui.component.image.MiniIcon
import love.yinlin.ui.component.input.RachelButton
import love.yinlin.ui.component.layout.LoadingBox
import love.yinlin.ui.component.layout.OffsetLayout
import love.yinlin.ui.component.text.InputType
import love.yinlin.ui.component.text.TextInput
import love.yinlin.ui.component.text.TextInputState
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import kotlin.coroutines.cancellation.CancellationException
import kotlin.coroutines.coroutineContext
import kotlin.coroutines.resume
import kotlin.math.roundToInt

@Stable
open class TipState(private val scope: CoroutineScope) {
	enum class Type {
		INFO, SUCCESS, WARNING, ERROR,
	}

	val host = SnackbarHostState()
	var type by mutableStateOf(Type.INFO)
		private set

	fun show(text: String?, type: Type) {
		scope.launch {
			host.currentSnackbarData?.dismiss()
			this@TipState.type = type
			host.showSnackbar(
				message = text ?: "",
				duration = SnackbarDuration.Short
			)
		}
	}

	fun info(text: String?) = show(text, Type.INFO)
	fun success(text: String?) = show(text, Type.SUCCESS)
	fun warning(text: String?) = show(text, Type.WARNING)
	fun error(text: String?) = show(text, Type.ERROR)
}

@Composable
fun Tip(state: TipState) {
	SnackbarHost(
		hostState = state.host,
		modifier = Modifier.fillMaxWidth().zIndex(Float.MAX_VALUE)
	) {
		Box(
			modifier = Modifier.padding(20.dp).fillMaxWidth()
				.clickable(indication = null, interactionSource = null) { }
				.background(
					brush = remember(state.type) { Brush.horizontalGradient(
						colors = when (state.type) {
							TipState.Type.INFO -> listOf(Colors.Gray5, Colors.Gray4, Colors.Gray5)
							TipState.Type.SUCCESS -> listOf(Colors.Green5, Colors.Green4, Colors.Green5)
							TipState.Type.WARNING -> listOf(Colors.Yellow5, Colors.Yellow4, Colors.Yellow5)
							TipState.Type.ERROR -> listOf(Colors.Red5, Colors.Red4, Colors.Red5)
						},
						startX = 0f,
						endX = Float.POSITIVE_INFINITY
					) },
					shape = MaterialTheme.shapes.extraLarge
				)
		) {
			Row(
				modifier = Modifier.fillMaxWidth().padding(10.dp),
				horizontalArrangement = Arrangement.spacedBy(15.dp),
				verticalAlignment = Alignment.CenterVertically
			) {
				MiniIcon(
					icon = when (state.type) {
						TipState.Type.INFO -> Icons.Outlined.Lightbulb
						TipState.Type.SUCCESS -> Icons.Outlined.Check
						TipState.Type.WARNING -> Icons.Outlined.Warning
						TipState.Type.ERROR -> Icons.Outlined.Error
					},
					color = Colors.White
				)
				Text(
					text = it.visuals.message,
					color = Colors.White,
					maxLines = 2,
					overflow = TextOverflow.Ellipsis,
					modifier = Modifier.weight(1f)
				)
			}
		}
	}
}

@Stable
abstract class DialogState<R> {
	var isOpen: Boolean by mutableStateOf(false)
		protected set
	protected var continuation: CancellableContinuation<R?>? = null

	open val dismissOnBackPress: Boolean get() = true
	open val dismissOnClickOutside: Boolean get() = true

	protected fun cancel() {
		continuation = null
		isOpen = false
	}

	protected suspend fun awaitResult(): R? = try {
		val result = suspendCancellableCoroutine { cont ->
			cont.invokeOnCancellation { cancel() }
			continuation?.cancel(CancellationException())
			continuation = cont
			isOpen = true
		}
		if (result != null) cancel()
		result
	}
	catch (_: CancellationException) { null }

	protected suspend fun openAsync() {
		CoroutineScope(coroutineContext).launch { awaitResult() }
	}

	open fun hide() {
		continuation?.resume(null)
		cancel()
	}

	@Composable
	protected abstract fun dialogContent()

	@Composable
	fun withOpen() {
		if (isOpen) dialogContent()
		DisposableEffect(Unit) { onDispose { hide() } }
	}

	@OptIn(ExperimentalComposeUiApi::class)
    @Composable
	protected fun BaseDialog(content: @Composable () -> Unit) {
		BackHandler(!dismissOnBackPress) {}

		Dialog(
			onDismissRequest = { hide() },
			properties = DialogProperties(
				dismissOnBackPress = dismissOnBackPress,
				dismissOnClickOutside = dismissOnClickOutside,
				usePlatformDefaultWidth = true
			),
			content = content
		)
	}
}

@Stable
abstract class RachelDialogState<R> : DialogState<R>() {
	@Stable
	sealed interface DialogInfo {
		val rachelWidth: Dp
		val minContentHeight: Dp
		val maxContentHeight: Dp
		val width: Dp

		data object Portrait : DialogInfo {
			override val rachelWidth: Dp = 140.dp
			override val minContentHeight: Dp = 50.dp
			override val maxContentHeight: Dp = 260.dp
			override val width: Dp = 300.dp
		}

		data object Landscape : DialogInfo {
			override val rachelWidth: Dp = 140.dp
			override val minContentHeight: Dp = 40.dp
			override val maxContentHeight: Dp = 280.dp
			override val width: Dp = 500.dp
		}
	}

	open val scrollable: Boolean get() = true
	abstract val title: String?

	private val info: DialogInfo = if (app.isPortrait) DialogInfo.Portrait else DialogInfo.Landscape

	@Composable
	protected fun RachelDialog(
		actions: (@Composable RowScope.() -> Unit)? = null,
		content: @Composable () -> Unit
	) {
		BaseDialog {
			Column(
				modifier = Modifier.width(info.width),
				horizontalAlignment = Alignment.CenterHorizontally
			) {
				OffsetLayout(y = info.rachelWidth / 15.5f) {
					Image(
						painter = painterResource(Res.drawable.img_dialog_rachel),
						contentDescription = null,
						modifier = Modifier.size(info.rachelWidth)
					)
				}
				Surface(
					shape = MaterialTheme.shapes.extraLarge,
					shadowElevation = 5.dp,
					modifier = Modifier.fillMaxWidth()
				) {
					Column(
						modifier = Modifier.fillMaxWidth().padding(15.dp),
						verticalArrangement = Arrangement.spacedBy(15.dp)
					) {
						title?.let {
							Text(
								text = it,
								style = MaterialTheme.typography.titleLarge,
								color = MaterialTheme.colorScheme.primary,
								maxLines = 1,
								overflow = TextOverflow.Ellipsis,
								modifier = Modifier.fillMaxWidth().padding(horizontal = 15.dp)
							)
						}
						Box(
							modifier = Modifier.fillMaxWidth()
								.heightIn(min = info.minContentHeight, max = info.maxContentHeight)
								.verticalScroll(enabled = scrollable, state = rememberScrollState())
						) {
							content()
						}
						if (actions != null) {
							Row(
								modifier = Modifier.fillMaxWidth(),
								horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End)
							) {
								actions()
							}
						}
					}
				}
			}
		}
	}
}

@Stable
open class DialogInfo(
	title: String = "提示",
	content: String = ""
) : RachelDialogState<Unit>() {
	override var title: String? by mutableStateOf(title)
		protected set
	var content: String by mutableStateOf(content)
		protected set

	suspend fun open(title: String = "提示", content: String) {
		this.title = title
		this.content = content
		awaitResult()
	}

	@Composable
	override fun dialogContent() {
		RachelDialog {
			Text(
				text = content,
				style = MaterialTheme.typography.bodyLarge,
				modifier = Modifier.fillMaxWidth()
			)
		}
	}
}

@Stable
open class DialogConfirm(
	title: String = "确认",
	content: String = ""
) : RachelDialogState<Boolean>() {
	override var title: String? by mutableStateOf(title)
		protected set
	var content: String by mutableStateOf(content)
		protected set

	suspend fun open(title: String = "确认", content: String): Boolean {
		this.title = title
		this.content = content
		return awaitResult() ?: false
	}

	@Composable
	override fun dialogContent() {
		RachelDialog(
			actions = {
				RachelButton(
					text = stringResource(Res.string.dialog_yes),
					onClick = { continuation?.resume(true) }
				)
				RachelButton(
					text = stringResource(Res.string.dialog_no),
					onClick = { hide() }
				)
			}
		) {
			Text(
				text = content,
				style = MaterialTheme.typography.bodyLarge,
				modifier = Modifier.fillMaxWidth()
			)
		}
	}
}

@Stable
open class DialogInput(
	val hint: String = "",
	val inputType: InputType = InputType.COMMON,
	val maxLength: Int = 0,
	val maxLines: Int = 1,
	val clearButton: Boolean = maxLines == 1
) : RachelDialogState<String>() {
	final override val dismissOnBackPress: Boolean = false
	final override val dismissOnClickOutside: Boolean = false
	final override val scrollable: Boolean = false

	override val title: String? = null

	val textInputState = TextInputState()

	suspend fun open(initText: String = ""): String? {
		textInputState.text = initText
		return awaitResult()
	}

	@Composable
	override fun dialogContent() {
		RachelDialog(
			actions = {
				RachelButton(
					text = stringResource(Res.string.dialog_ok),
					enabled = textInputState.ok,
					onClick = { continuation?.resume(textInputState.text) }
				)
				RachelButton(
					text = stringResource(Res.string.dialog_cancel),
					onClick = { hide() }
				)
			}
		) {
			TextInput(
				state = textInputState,
				hint = hint,
				inputType = inputType,
				maxLength = maxLength,
				maxLines = maxLines,
				clearButton = clearButton,
				modifier = Modifier.fillMaxWidth()
			)
		}
	}
}

@Stable
abstract class DialogChoice(
    override val title: String? = null
) : RachelDialogState<Int>() {
	final override val dismissOnClickOutside: Boolean = false

	abstract val num: Int
	abstract fun name(index: Int): String
	abstract fun icon(index: Int): ImageVector

	suspend fun open(): Int? = awaitResult()

	@Composable
	override fun dialogContent() {
		RachelDialog {
			Column(
				modifier = Modifier.fillMaxWidth(),
				verticalArrangement = Arrangement.spacedBy(5.dp)
			) {
				repeat(num) { index ->
					val nameString = name(index)
					Row(
						modifier = Modifier.fillMaxWidth().clickable {
							continuation?.resume(index)
						}.padding(5.dp),
						horizontalArrangement = Arrangement.spacedBy(10.dp),
						verticalAlignment = Alignment.CenterVertically
					) {
						MiniIcon(
							icon = icon(index),
							size = 24.dp
						)
						Text(
							text = nameString,
							modifier = Modifier.fillMaxWidth()
						)
					}
				}
			}
		}
	}

	companion object {
		fun fromItems(items: List<String>, title: String? = null) = object : DialogChoice(title) {
			override val num: Int = items.size
			override fun name(index: Int): String = items[index]
			override fun icon(index: Int): ImageVector = Icons.AutoMirrored.Outlined.ArrowRight
		}

		fun fromIconItems(items: List<Pair<String, ImageVector>>, title: String? = null) = object : DialogChoice(title) {
			override val num: Int = items.size
			override fun name(index: Int): String = items[index].first
			override fun icon(index: Int): ImageVector = items[index].second
		}
	}
}

@Stable
open class DialogDynamicChoice(title: String? = null) : DialogChoice(title) {
	private var items: List<String> = emptyList()
	override val num: Int get() = items.size
	override fun name(index: Int): String = items[index]
	override fun icon(index: Int): ImageVector = Icons.AutoMirrored.Outlined.ArrowRight

	suspend fun open(items: List<String>): Int? = if (items.isNotEmpty()) {
		this.items = items
		awaitResult()
	} else null
}

@Stable
open class DialogProgress : RachelDialogState<Unit>() {
	var current by mutableStateOf("0")
	var total by mutableStateOf("0")
	var progress by mutableFloatStateOf(0f)

	final override val dismissOnBackPress: Boolean get() = false
	final override val dismissOnClickOutside: Boolean get() = false
	final override val scrollable: Boolean get() = false

	override val title: String? = "下载中..."

	suspend fun open() {
		current = "0"
		total = "0"
		progress = 0f
		openAsync()
	}

	@Composable
	override fun dialogContent() {
		RachelDialog(
			actions = {
				RachelButton(
					text = stringResource(Res.string.dialog_cancel),
					enabled = isOpen,
					onClick = { hide() }
				)
			}
		) {
			Column(
				modifier = Modifier.fillMaxWidth(),
				verticalArrangement = Arrangement.spacedBy(5.dp)
			) {
				LinearProgressIndicator(
					progress = { progress },
					modifier = Modifier.fillMaxWidth().height(6.dp),
					gapSize = 0.dp,
					drawStopIndicator = {}
				)
				Row(
					modifier = Modifier.fillMaxWidth(),
					horizontalArrangement = Arrangement.spacedBy(20.dp),
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
class DialogLoading : DialogState<Unit>() {
	override val dismissOnBackPress: Boolean = false
	override val dismissOnClickOutside: Boolean = false

	suspend fun open() = openAsync()

	@Composable
	override fun dialogContent() {
		BaseDialog {
			Surface(
				shape = MaterialTheme.shapes.extraLarge,
				shadowElevation = 5.dp,
				modifier = Modifier.size(300.dp)
			) {
				LoadingBox()
			}
		}
	}
}