package love.yinlin.ui.component.screen

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowRight
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Error
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.Warning
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
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
import kotlin.coroutines.coroutineContext
import kotlin.math.roundToInt

@Stable
open class TipState {
	enum class Type {
		INFO, SUCCESS, WARNING, ERROR,
	}

	val host = SnackbarHostState()
	var type by mutableStateOf(Type.INFO)
		private set

	suspend fun show(text: String?, type: Type) {
		// showSnackbar会阻塞当前协程, 应该在当前上下文启动新的协程
		CoroutineScope(coroutineContext).launch {
			host.currentSnackbarData?.dismiss()
			this@TipState.type = type
			host.showSnackbar(
				message = text ?: "",
				duration = SnackbarDuration.Short
			)
		}
	}

	suspend fun info(text: String?) = show(text, Type.INFO)
	suspend fun success(text: String?) = show(text, Type.SUCCESS)
	suspend fun warning(text: String?) = show(text, Type.WARNING)
	suspend fun error(text: String?) = show(text, Type.ERROR)
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
					imageVector = when (state.type) {
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

	companion object {
		val instance: DialogInfo get() = if (app.isPortrait) Portrait else Landscape
	}
}

@Stable
abstract class DialogState {
	var isOpen: Boolean by mutableStateOf(false)
		protected set

	open val dismissOnBackPress: Boolean get() = true
	open val dismissOnClickOutside: Boolean get() = true

	open fun open() { isOpen = true }
	open fun hide() { isOpen = false }

	@Composable protected abstract fun dialogContent()
	@Composable fun withOpen() {
		if (isOpen) dialogContent()
		DisposableEffect(Unit) { onDispose { hide() } }
	}
}

abstract class RachelDialogState : DialogState() {
	open val scrollable: Boolean get() = true
	abstract val title: String?
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun RachelDialog(
	state: RachelDialogState,
	actions: (@Composable RowScope.() -> Unit)? = null,
	content: @Composable () -> Unit
) {
	val info = DialogInfo.instance

	BackHandler(!state.dismissOnBackPress) {}

	Dialog(
		onDismissRequest = { state.hide() },
		properties = DialogProperties(
			dismissOnBackPress = state.dismissOnBackPress,
			dismissOnClickOutside = state.dismissOnClickOutside,
			usePlatformDefaultWidth = true
		)
	) {
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
					state.title?.let {
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
							.verticalScroll(enabled = state.scrollable, state = rememberScrollState())
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

open class DialogInfoState(
	title: String = "提示",
	content: String = ""
) : RachelDialogState() {
	override var title: String? by mutableStateOf(title)
		protected set
	var content: String by mutableStateOf(content)
		protected set

	fun open(title: String = "提示", content: String) {
		this.title = title
		this.content = content
		super.open()
	}

	@Composable
	override fun dialogContent() {
		RachelDialog(state = this) {
			Text(
				text = content,
				style = MaterialTheme.typography.bodyLarge,
				modifier = Modifier.fillMaxWidth()
			)
		}
	}
}

open class DialogConfirmState(
	title: String = "确认",
	content: String = "",
	onYes: (() -> Unit)? = null
) : RachelDialogState() {
	override var title: String? by mutableStateOf(title)
		protected set
	var content: String by mutableStateOf(content)
		protected set
	var onYes = onYes
		protected set

	fun open(title: String = "确认", content: String, onYes: () -> Unit) {
		this.title = title
		this.content = content
		this.onYes = onYes
		super.open()
	}

	@Composable
	override fun dialogContent() {
		RachelDialog(
			state = this,
			actions = {
				RachelButton(
					text = stringResource(Res.string.dialog_yes),
					onClick = {
						hide()
						onYes?.invoke()
					}
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

abstract class DialogInputState(
	val hint: String = "",
	val inputType: InputType = InputType.COMMON,
	val maxLength: Int = 0,
	val maxLines: Int = 1,
	val clearButton: Boolean = maxLines == 1,
) : RachelDialogState() {
	final override val dismissOnBackPress: Boolean = false
	final override val dismissOnClickOutside: Boolean = false
	final override val scrollable: Boolean = false

	override val title: String? = null

	val textInputState = TextInputState()

	abstract fun onInput(text: String)

	fun open(initText: String) {
		textInputState.text = initText
		super.open()
	}

	@Composable
	override fun dialogContent() {
		RachelDialog(
			state = this,
			actions = {
				RachelButton(
					text = stringResource(Res.string.dialog_yes),
					enabled = textInputState.ok,
					onClick = {
						hide()
						onInput(textInputState.text)
					}
				)
				RachelButton(
					text = stringResource(Res.string.dialog_no),
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

abstract class DialogChoiceState(
	val num: Int,
    override val title: String? = null
) : RachelDialogState() {
	final override val dismissOnClickOutside: Boolean = false

	abstract fun name(index: Int): String
	abstract fun icon(index: Int): ImageVector
    abstract fun onSelected(index: Int, text: String)

	@Composable
	override fun dialogContent() {
		RachelDialog(state = this) {
			Column(
				modifier = Modifier.fillMaxWidth(),
				verticalArrangement = Arrangement.spacedBy(5.dp)
			) {
				repeat(num) { index ->
					val nameString = name(index)
					Row(
						modifier = Modifier.fillMaxWidth().clickable {
							hide()
							onSelected(index, nameString)
						}.padding(5.dp),
						horizontalArrangement = Arrangement.spacedBy(10.dp),
						verticalAlignment = Alignment.CenterVertically
					) {
						MiniIcon(
							imageVector = icon(index),
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
		fun fromItems(
			items: List<String>,
			title: String? = null,
			onSelected: (Int, String) -> Unit
		) = object : DialogChoiceState(items.size, title) {
			override fun name(index: Int): String = items[index]
			override fun icon(index: Int): ImageVector = Icons.AutoMirrored.Outlined.ArrowRight
			override fun onSelected(index: Int, text: String) = onSelected(index, text)
		}

		fun fromItems(
			items: List<Pair<String, (Int, String) -> Unit>>,
			title: String? = null
		) = object : DialogChoiceState(items.size, title) {
			override fun name(index: Int): String = items[index].first
			override fun icon(index: Int): ImageVector = Icons.AutoMirrored.Outlined.ArrowRight
			override fun onSelected(index: Int, text: String) = items[index].second(index, text)
		}

		fun fromIconItems(
			items: List<Pair<String, ImageVector>>,
			title: String? = null,
			onSelected: (Int, String) -> Unit
		) = object : DialogChoiceState(items.size, title) {
			override fun name(index: Int): String = items[index].first
			override fun icon(index: Int): ImageVector = items[index].second
			override fun onSelected(index: Int, text: String) = onSelected(index, text)
		}
	}
}

open class DialogProgressState() : RachelDialogState() {
	var current by mutableStateOf("0")
	var total by mutableStateOf("0")
	var progress by mutableFloatStateOf(0f)

	final override val dismissOnBackPress: Boolean get() = false
	final override val dismissOnClickOutside: Boolean get() = false
	final override val scrollable: Boolean get() = false

	override val title: String? = "下载中..."

	override fun open() {
		current = "0"
		total = "0"
		progress = 0f
		super.open()
	}

	@Composable
	override fun dialogContent() {
		RachelDialog(
			state = this,
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

class DialogLoadingState : DialogState() {
	override val dismissOnBackPress: Boolean = false
	override val dismissOnClickOutside: Boolean = false

	@OptIn(ExperimentalComposeUiApi::class)
	@Composable
	override fun dialogContent() {
		BackHandler {}

		Dialog(
			onDismissRequest = { hide() },
			properties = DialogProperties(
				dismissOnBackPress = dismissOnBackPress,
				dismissOnClickOutside = dismissOnClickOutside,
				usePlatformDefaultWidth = true
			)
		) {
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