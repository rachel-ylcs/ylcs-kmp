package love.yinlin.ui.component.screen

import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import love.yinlin.extension.rememberState
import love.yinlin.platform.app

@Stable
interface BaseSheetState<T> {
    @Composable
    fun withOpen(block: @Composable (T) -> Unit)
    val isOpen: Boolean
    fun open(value: T)
    fun hide()
}

@Stable
class SheetState<T>(default: T? = null) : BaseSheetState<T> {
    private var state by mutableStateOf(default)

    @Composable
    override fun withOpen(block: @Composable (T) -> Unit) {
        state?.let { block(it) }
        DisposableEffect(Unit) { onDispose { hide() } }
    }
    override val isOpen: Boolean get() = state != null
    override fun open(value: T) { state = value }
    override fun hide() { state = null }
}

@Stable
class CommonSheetState(status: Boolean = false) : BaseSheetState<Unit> {
    private var state by mutableStateOf(status)

    @Composable
    override fun withOpen(block: @Composable (Unit) -> Unit) {
        if (state) block(Unit)
        DisposableEffect(Unit) { onDispose { hide() } }
    }
    override val isOpen: Boolean get() = state
    override fun open(value: Unit) { state = true }
    fun open() = open(Unit)
    override fun hide() { state = false }
}

@Composable
private fun ModalLandscapeSheet(
    onDismissRequest: () -> Unit,
    content: @Composable () -> Unit
) {
    var isVisible by rememberState { false }
    val offset by animateFloatAsState(
        targetValue = if (isVisible) 0f else 360f,
        animationSpec = tween(
            durationMillis = 300,
            easing = LinearOutSlowInEasing
        ),
        finishedListener = {
            if (!isVisible) onDismissRequest()
        }
    )

    LaunchedEffect(Unit) { isVisible = true }

    Dialog(
        onDismissRequest = { isVisible = false },
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
        )
    ) {
       Row(modifier = Modifier.fillMaxSize()
           .clip(shape = MaterialTheme.shapes.extraLarge)
           .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.4f * (1 - offset / 360)))
       ) {
           Box(modifier = Modifier.weight(1f)
               .fillMaxHeight()
               .clickable(interactionSource = null, indication = null, onClick = { isVisible = false })
           )

           Surface(
               shadowElevation = 5.dp,
               modifier = Modifier.width(360.dp).fillMaxHeight().offset(x = offset.dp)
           ) {
               content()
           }
       }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> Sheet(
    state: BaseSheetState<T>,
    hasHandle: Boolean = true,
    content: @Composable () -> Unit
) {
    if (app.isPortrait) {
        ModalBottomSheet(
            onDismissRequest = { state.hide() },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            shape = MaterialTheme.shapes.extraLarge.copy(bottomStart = CornerSize(0), bottomEnd = CornerSize(0)),
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 1.dp,
            dragHandle = if (hasHandle) {
                {
                    Surface(
                        modifier = Modifier.padding(vertical = 15.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        shape = MaterialTheme.shapes.extraLarge
                    ) {
                        Box(Modifier.size(width = 32.dp, height = 4.dp))
                    }
                }
            } else null,
            modifier = Modifier.fillMaxWidth()
        ) {
            content()
        }
    }
    else {
        ModalLandscapeSheet(onDismissRequest = { state.hide() }) {
            content()
        }
    }
}