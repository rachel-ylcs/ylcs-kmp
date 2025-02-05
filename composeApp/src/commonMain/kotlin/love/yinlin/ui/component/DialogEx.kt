package love.yinlin.ui.component

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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import love.yinlin.app
import love.yinlin.extension.condition
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import ylcs_kmp.composeapp.generated.resources.*
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
		@Stable
		val instance: DialogInfo get() = if (app.isPortrait) Portrait else Landscape
	}
}

open class DialogState {
	var isOpen: Boolean by mutableStateOf(false)
}

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
	Dialog(
		onDismissRequest = { state.isOpen = false },
		properties = DialogProperties(
			dismissOnBackPress = dismissOnBackPress,
			dismissOnClickOutside = dismissOnClickOutside,
			usePlatformDefaultWidth = true
		)
	) {
		Column(
			modifier = Modifier.width(info.width)
				.offset(y = (-18).dp),
			horizontalAlignment = Alignment.CenterHorizontally
		) {
			Image(
				painter = painterResource(Res.drawable.img_dialog_rachel),
				contentDescription = null,
				modifier = Modifier.size(info.rachelWidth)
					.offset(y = 18.dp).zIndex(2f)
			)
			Surface(
				shape = MaterialTheme.shapes.extraLarge,
				shadowElevation = 5.dp,
				modifier = Modifier.fillMaxWidth().zIndex(1f)
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
							.condition(scrollable, { verticalScroll(rememberScrollState()) })
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
private fun DialogButton(
	text: String,
	enabled: Boolean = true,
	onClick: () -> Unit
) {
	TextButton(
		enabled = enabled,
		onClick = onClick
	) {
		Text(
			text = text
		)
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
			DialogButton(text = stringResource(Res.string.dialog_yes)) {
				state.isOpen = false
				onYes()
			}
			DialogButton(text = stringResource(Res.string.dialog_no)) {
				state.isOpen = false
			}
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
	onInput: (String) -> Unit
) {
	val textInputState = rememberTextInputState()

	RachelDialog(
		state = state,
		dismissOnBackPress = false,
		dismissOnClickOutside = false,
		scrollable = false,
		actions = {
			DialogButton(
				text = stringResource(Res.string.dialog_yes),
				enabled = !textInputState.overflow && textInputState.text.isNotEmpty()
			) {
				state.isOpen = false
				onInput(textInputState.text)
			}
			DialogButton(text = stringResource(Res.string.dialog_no)) {
				state.isOpen = false
			}
		}
	) {
		TextInput(
			state = textInputState,
			hint = hint,
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
	var progress by mutableIntStateOf(0)
	var maxProgress by mutableIntStateOf(100)
	var isCancel by mutableStateOf(false)
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
			DialogButton(
				text = stringResource(Res.string.dialog_cancel),
				enabled = !state.isCancel
			) {
				state.isCancel = true
				state.isOpen = false
			}
		}
	) {
		Column(
			modifier = Modifier.fillMaxWidth(),
			verticalArrangement = Arrangement.spacedBy(5.dp)
		) {
			val value = if (state.maxProgress == 0) 0f else state.progress.toFloat() / state.maxProgress
			LinearProgressIndicator(
				progress = { value },
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
					text = "${(value * 100).roundToInt()}%",
					textAlign = TextAlign.Start,
					maxLines = 1,
					overflow = TextOverflow.Ellipsis,
					modifier = Modifier.weight(1f)
				)
				Text(
					text = "${state.progress} / ${state.maxProgress}",
					textAlign = TextAlign.End,
					maxLines = 1,
					overflow = TextOverflow.Ellipsis,
					modifier = Modifier.weight(1f)
				)
			}
		}
	}
}

@Composable
fun DialogLoading(
	state: DialogState
) {
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