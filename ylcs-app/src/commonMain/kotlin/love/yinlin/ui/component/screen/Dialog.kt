package love.yinlin.ui.component.screen

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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import love.yinlin.common.Device
import love.yinlin.common.LocalDevice
import love.yinlin.common.LocalImmersivePadding
import love.yinlin.common.ThemeValue
import love.yinlin.extension.clickableNoRipple
import love.yinlin.platform.app
import love.yinlin.resources.*
import love.yinlin.ui.component.image.MiniIcon
import love.yinlin.ui.component.input.RachelButton
import love.yinlin.ui.component.layout.LoadingBox
import love.yinlin.ui.component.layout.OffsetLayout
import love.yinlin.ui.component.text.InputType
import love.yinlin.ui.component.text.TextInput
import love.yinlin.ui.component.text.TextInputState
import org.jetbrains.compose.resources.stringResource
import kotlin.coroutines.cancellation.CancellationException
import kotlin.coroutines.coroutineContext
import kotlin.coroutines.resume
import kotlin.math.roundToInt

@Stable
abstract class FloatingDialog<R>() : Floating<Unit>() {
	override fun alignment(device: Device): Alignment = Alignment.Center
	override fun enter(device: Device): EnterTransition = scaleIn(
		animationSpec = tween(durationMillis = app.config.animationSpeed, easing = LinearOutSlowInEasing)) +
			fadeIn(tween(durationMillis = app.config.animationSpeed, easing = LinearOutSlowInEasing)
	)
	override fun exit(device: Device): ExitTransition = scaleOut(
		animationSpec = tween(durationMillis = app.config.animationSpeed, easing = LinearOutSlowInEasing)) +
			fadeOut(tween(durationMillis = app.config.animationSpeed, easing = LinearOutSlowInEasing)
	)
	override val zIndex: Float = Z_INDEX_DIALOG

	protected var continuation: CancellableContinuation<R?>? = null

	override fun close() {
		continuation?.resume(null)
		continuation = null
		super.close()
	}

	protected suspend fun awaitResult(): R? = try {
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
	catch (_: CancellationException) { null }

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
abstract class FloatingRachelDialog<R>() : FloatingDialog<R>() {
	open val scrollable: Boolean get() = true
	abstract val title: String?
	open val actions: @Composable (RowScope.() -> Unit)? = null

	@Composable
	override fun Wrapper(block: @Composable () -> Unit) {
		super.Wrapper {
			val sizeType = LocalDevice.current.size

			val rachelWidth = when (sizeType) {
				Device.Size.SMALL -> 140.dp
				Device.Size.MEDIUM -> 150.dp
				Device.Size.LARGE -> 160.dp
			}
			val minContentHeight = when (sizeType) {
				Device.Size.SMALL -> 50.dp
				Device.Size.MEDIUM -> 45.dp
				Device.Size.LARGE -> 40.dp
			}
			val maxContentHeight = when (sizeType) {
				Device.Size.SMALL -> 260.dp
				Device.Size.MEDIUM -> 280.dp
				Device.Size.LARGE -> 300.dp
			}

			Column(
				modifier = Modifier.width(ThemeValue.Size.DialogWidth)
					.clickableNoRipple { close() }
					.padding(bottom = rachelWidth),
				horizontalAlignment = Alignment.CenterHorizontally
			) {
				OffsetLayout(y = rachelWidth / 15.5f + ThemeValue.Padding.VerticalExtraSpace / 5) {
					MiniIcon(
						res = Res.drawable.img_dialog_rachel,
						size = rachelWidth,
						modifier = Modifier.clickableNoRipple { }
					)
				}
				Surface(
					shape = MaterialTheme.shapes.extraLarge,
					modifier = Modifier.fillMaxWidth().clickableNoRipple { }
				) {
					Column(
						modifier = Modifier.fillMaxWidth().padding(
							top = ThemeValue.Padding.VerticalExtraSpace * 2,
							bottom = ThemeValue.Padding.VerticalExtraSpace,
							start = ThemeValue.Padding.HorizontalExtraSpace,
							end = ThemeValue.Padding.HorizontalExtraSpace
						),
						verticalArrangement = Arrangement.spacedBy(ThemeValue.Padding.VerticalExtraSpace)
					) {
						title?.let {
							Text(
								text = it,
								style = MaterialTheme.typography.titleLarge,
								color = MaterialTheme.colorScheme.primary,
								maxLines = 1,
								overflow = TextOverflow.Ellipsis,
								modifier = Modifier.fillMaxWidth().padding(horizontal = ThemeValue.Padding.HorizontalExtraSpace)
							)
						}

						Box(modifier = Modifier
							.heightIn(min = minContentHeight, max = maxContentHeight)
							.fillMaxWidth()
							.verticalScroll(enabled = scrollable, state = rememberScrollState())
						) {
							block()
						}
						actions?.let {
							Row(
								modifier = Modifier.fillMaxWidth(),
								horizontalArrangement = Arrangement.spacedBy(ThemeValue.Padding.HorizontalSpace, Alignment.End)
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
) : FloatingRachelDialog<Unit>() {
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
) : FloatingRachelDialog<Unit>() {
	override var title: String? by mutableStateOf(title)
		protected set
	var content: String by mutableStateOf(content)
		protected set

	override val actions: @Composable (RowScope.() -> Unit)? = {
		RachelButton(
			text = stringResource(Res.string.dialog_yes),
			onClick = { continuation?.resume(Unit) }
		)
		RachelButton(
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
	val clearButton: Boolean = maxLines == 1
) : FloatingRachelDialog<String>() {
	final override val dismissOnBackPress: Boolean = false
	final override val dismissOnClickOutside: Boolean = false
	final override val scrollable: Boolean = false

	override val title: String? = null

	private val textInputState = TextInputState()

	override val actions: @Composable (RowScope.() -> Unit)? = {
		RachelButton(
			text = stringResource(Res.string.dialog_ok),
			enabled = textInputState.ok,
			onClick = { continuation?.resume(textInputState.text) }
		)
		RachelButton(
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
abstract class FloatingDialogChoice(
	override val title: String? = null
) : FloatingRachelDialog<Int>() {
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
				verticalArrangement = Arrangement.spacedBy(ThemeValue.Padding.VerticalSpace)
			) {
				repeat(num) { index ->
					Row(
						modifier = Modifier.fillMaxWidth().clickable {
							continuation?.resume(index)
						}.padding(ThemeValue.Padding.Value),
						horizontalArrangement = Arrangement.spacedBy(ThemeValue.Padding.HorizontalSpace),
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
			size = ThemeValue.Size.ExtraIcon
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
open class FloatingDialogProgress : FloatingRachelDialog<Unit>() {
	var current by mutableStateOf("0")
	var total by mutableStateOf("0")
	var progress by mutableFloatStateOf(0f)

	final override val dismissOnBackPress: Boolean get() = false
	final override val dismissOnClickOutside: Boolean get() = false
	final override val scrollable: Boolean get() = false

	override val title: String? = "下载中..."

	override val actions: @Composable (RowScope.() -> Unit)? = {
		RachelButton(
			text = stringResource(Res.string.dialog_cancel),
			enabled = isOpen,
			onClick = { close() }
		)
	}

	suspend fun openSuspend() {
		current = "0"
		total = "0"
		progress = 0f
		CoroutineScope(coroutineContext).launch { awaitResult() }
	}

	@Composable
	override fun Wrapper(block: @Composable () -> Unit) {
		super.Wrapper {
			Column(
				modifier = Modifier.fillMaxWidth(),
				verticalArrangement = Arrangement.spacedBy(ThemeValue.Padding.VerticalSpace)
			) {
				LinearProgressIndicator(
					progress = { progress },
					modifier = Modifier.fillMaxWidth().height(ThemeValue.Size.ProgressHeight),
					gapSize = ThemeValue.Padding.ZeroSpace,
					drawStopIndicator = {}
				)
				Row(
					modifier = Modifier.fillMaxWidth(),
					horizontalArrangement = Arrangement.spacedBy(ThemeValue.Padding.HorizontalExtraSpace),
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
		CoroutineScope(coroutineContext).launch { awaitResult() }
	}

	@Composable
	override fun Wrapper(block: @Composable (() -> Unit)) {
		super.Wrapper {
			Surface(
				shape = MaterialTheme.shapes.extraLarge,
				shadowElevation = ThemeValue.Shadow.Surface,
				modifier = Modifier.size(ThemeValue.Size.DialogWidth)
			) {
				LoadingBox()
			}
		}
	}
}