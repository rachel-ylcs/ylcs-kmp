package love.yinlin.ui.component.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import love.yinlin.common.Colors
import love.yinlin.platform.app
import love.yinlin.ui.component.button.RachelButton
import love.yinlin.ui.component.text.InputType
import love.yinlin.ui.component.layout.LoadingBox
import love.yinlin.ui.component.image.MiniIcon
import love.yinlin.ui.component.layout.OffsetLayout
import love.yinlin.ui.component.text.TextInput
import love.yinlin.ui.component.text.TextInputState
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import love.yinlin.resources.*
import kotlin.math.roundToInt

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

open class DialogState {
	open var isOpen: Boolean by mutableStateOf(false)
}

open class TipState {
	enum class Type {
		INFO, SUCCESS, WARNING, ERROR,
	}

	val host = SnackbarHostState()
	var type by mutableStateOf(Type.INFO)
		private set

	suspend fun show(text: String?, type: Type = Type.INFO) {
		host.currentSnackbarData?.dismiss()
		this.type = type
		host.showSnackbar(
			message = text ?: "",
			duration = SnackbarDuration.Short
		)
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
		modifier = Modifier.fillMaxSize().zIndex(Float.MAX_VALUE)
	) {
		Box(
			modifier = Modifier.fillMaxSize(),
			contentAlignment = Alignment.TopCenter
		) {
			Row(modifier = Modifier.padding(top = 50.dp).size(width = 300.dp, height = 150.dp)) {
				OffsetLayout(x = 10.dp) {
					Image(
						painter = painterResource(Res.drawable.img_toast_rachel),
						contentDescription = null,
						modifier = Modifier.width(75.dp).fillMaxHeight()
					)
				}
				Surface(
					shape = MaterialTheme.shapes.extraLarge,
					shadowElevation = 5.dp,
					color = when (state.type) {
						TipState.Type.INFO -> MaterialTheme.colorScheme.surface
						TipState.Type.SUCCESS -> Colors.Green4
						TipState.Type.WARNING -> Colors.Yellow4
						TipState.Type.ERROR -> Colors.Red4
					},
					modifier = Modifier.weight(1f).fillMaxHeight()
						.padding(top = 40.dp, bottom = 20.dp)
				) {
					Text(
						text = it.visuals.message,
						overflow = TextOverflow.Ellipsis,
						modifier = Modifier.fillMaxSize()
							.padding(top = 5.dp, bottom = 5.dp, start = 25.dp, end = 5.dp)
					)
				}
			}
		}
	}
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun RachelDialog(
	state: DialogState,
	dismissOnBackPress: Boolean = true,
	dismissOnClickOutside: Boolean = true,
	scrollable: Boolean = true,
	title: String? = null,
	actions: (@Composable RowScope.() -> Unit)? = null,
	content: @Composable () -> Unit
) {
	val info = DialogInfo.instance

	BackHandler(!dismissOnBackPress) {}

	Dialog(
		onDismissRequest = { state.isOpen = false },
		properties = DialogProperties(
			dismissOnBackPress = dismissOnBackPress,
			dismissOnClickOutside = dismissOnClickOutside,
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
					if (title != null) {
						Text(
							text = title,
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

@Composable
fun DialogInfo(
	state: DialogState,
	title: String = stringResource(Res.string.dialog_info),
	content: String
) {
	RachelDialog(
		state = state,
		title = title
	) {
		Text(
			text = content,
			style = MaterialTheme.typography.bodyLarge,
			modifier = Modifier.fillMaxWidth()
		)
	}
}

@Composable
fun DialogConfirm(
	state: DialogState,
	title: String = stringResource(Res.string.dialog_confirm),
	content: String,
	onYes: () -> Unit
) {
	RachelDialog(
		state = state,
		title = title,
		actions = {
			RachelButton(
				text = stringResource(Res.string.dialog_yes),
				onClick = {
					state.isOpen = false
					onYes()
				}
			)
			RachelButton(
				text = stringResource(Res.string.dialog_no),
				onClick = {
					state.isOpen = false
				}
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

@Composable
fun DialogInput(
	state: DialogState,
	hint: String,
	inputType: InputType = InputType.COMMON,
	maxLength: Int = 0,
	maxLines: Int = 1,
	clearButton: Boolean = true,
	onInput: (String) -> Unit
) {
	val textInputState = remember { TextInputState() }

	RachelDialog(
		state = state,
		dismissOnBackPress = false,
		dismissOnClickOutside = false,
		scrollable = false,
		actions = {
			RachelButton(
				text = stringResource(Res.string.dialog_yes),
				enabled = textInputState.ok,
				onClick = {
					state.isOpen = false
					onInput(textInputState.text)
				}
			)
			RachelButton(
				text = stringResource(Res.string.dialog_no),
				onClick = {
					state.isOpen = false
				}
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

@Composable
fun DialogChoiceWithIcon(
	state: DialogState,
	title: String? = null,
	items: List<Pair<String, ImageVector>>,
	onSelected: (String) -> Unit
) {
	RachelDialog(
		state = state,
		dismissOnClickOutside = false,
		title = title
	) {
		Column(
			modifier = Modifier.fillMaxWidth(),
			verticalArrangement = Arrangement.spacedBy(5.dp)
		) {
			for ((name, icon) in items) {
				Row(
					modifier = Modifier.fillMaxWidth().clickable {
						state.isOpen = false
						onSelected(name)
					}.padding(5.dp),
					horizontalArrangement = Arrangement.spacedBy(10.dp),
					verticalAlignment = Alignment.CenterVertically
				) {
					MiniIcon(
						imageVector = icon,
						size = 24.dp
					)
					Text(
						text = name,
						modifier = Modifier.fillMaxWidth()
					)
				}
			}
		}
	}
}

@Composable
fun DialogChoice(
	state: DialogState,
	title: String? = null,
	items: List<String>,
	onSelected: (String) -> Unit
) {
	RachelDialog(
		state = state,
		dismissOnClickOutside = false,
		title = title
	) {
		Column(
			modifier = Modifier.fillMaxWidth(),
			verticalArrangement = Arrangement.spacedBy(5.dp)
		) {
			for (name in items) {
				Row(
					modifier = Modifier.fillMaxWidth().clickable {
						state.isOpen = false
						onSelected(name)
					}.padding(5.dp),
					horizontalArrangement = Arrangement.spacedBy(10.dp),
					verticalAlignment = Alignment.CenterVertically
				) {
					MiniIcon(
						imageVector = Icons.AutoMirrored.Outlined.ArrowRight,
						size = 24.dp
					)
					Text(
						text = name,
						modifier = Modifier.fillMaxWidth()
					)
				}
			}
		}
	}
}

@Composable
fun DialogChoice(
	state: DialogState,
	title: String? = null,
	items: List<Pair<String, () -> Unit>>
) {
	RachelDialog(
		state = state,
		dismissOnClickOutside = false,
		title = title
	) {
		Column(
			modifier = Modifier.fillMaxWidth(),
			verticalArrangement = Arrangement.spacedBy(5.dp)
		) {
			for ((name, onSelected) in items) {
				Row(
					modifier = Modifier.fillMaxWidth().clickable {
						state.isOpen = false
						onSelected()
					}.padding(5.dp),
					horizontalArrangement = Arrangement.spacedBy(10.dp),
					verticalAlignment = Alignment.CenterVertically
				) {
					MiniIcon(
						imageVector = Icons.AutoMirrored.Outlined.ArrowRight,
						size = 24.dp
					)
					Text(
						text = name,
						modifier = Modifier.fillMaxWidth()
					)
				}
			}
		}
	}
}

open class DialogProgressState : DialogState() {
	override var isOpen: Boolean
		get() = super.isOpen
		set(value) {
			super.isOpen = value
			current = "0"
			total = "0"
			progress = 0f
		}

	var current by mutableStateOf("0")
	var total by mutableStateOf("0")
	var progress by mutableFloatStateOf(0f)
}

@Composable
fun DialogProgress(
	state: DialogProgressState,
	title: String = stringResource(Res.string.dialog_progress)
) {
	RachelDialog(
		state = state,
		dismissOnBackPress = false,
		dismissOnClickOutside = false,
		scrollable = false,
		title = title,
		actions = {
			RachelButton(
				text = stringResource(Res.string.dialog_cancel),
				enabled = state.isOpen,
				onClick = {
					state.isOpen = false
				}
			)
		}
	) {
		Column(
			modifier = Modifier.fillMaxWidth(),
			verticalArrangement = Arrangement.spacedBy(5.dp)
		) {
			LinearProgressIndicator(
				progress = { state.progress },
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
					text = "${(state.progress * 100).roundToInt()}%",
					textAlign = TextAlign.Start,
					maxLines = 1,
					overflow = TextOverflow.Ellipsis,
					modifier = Modifier.weight(1f)
				)
				Text(
					text = "${state.current} / ${state.total}",
					textAlign = TextAlign.End,
					maxLines = 1,
					overflow = TextOverflow.Ellipsis,
					modifier = Modifier.weight(1f)
				)
			}
		}
	}
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun DialogLoading(
	state: DialogState
) {
	BackHandler {}

	Dialog(
		onDismissRequest = { state.isOpen = false },
		properties = DialogProperties(
			dismissOnBackPress = false,
			dismissOnClickOutside = false,
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