package love.yinlin.ui.component.screen

import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material3.DismissibleDrawerSheet
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import love.yinlin.common.Colors
import love.yinlin.extension.rememberState
import love.yinlin.extension.rememberValueState
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
    val offset by animateDpAsState(
        targetValue = if (isVisible) 0.dp else 360.dp,
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
       Row(
           modifier = Modifier.fillMaxSize().clip(shape = MaterialTheme.shapes.extraLarge)
       ) {
           Box(modifier = Modifier.weight(1f).fillMaxHeight().clickable(
               interactionSource = null,
               indication = null,
               onClick = { isVisible = false }
           ))

           Surface(
               shadowElevation = 5.dp,
               modifier = Modifier.width(360.dp).fillMaxHeight().offset(x = offset)
           ) {
               content()
           }
       }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> BottomSheet(
    state: BaseSheetState<T>,
    content: @Composable () -> Unit
) {
    if (app.isPortrait) {
        ModalBottomSheet(
            onDismissRequest = { state.hide() },
            shape = MaterialTheme.shapes.extraLarge.copy(bottomStart = CornerSize(0), bottomEnd = CornerSize(0)),
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 1.dp,
            dragHandle = {
                Surface(
                    modifier = Modifier.padding(vertical = 15.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    shape = MaterialTheme.shapes.extraLarge
                ) {
                    Box(Modifier.size(width = 32.dp, height = 4.dp))
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            content()
        }
    }
    else {
        ModalLandscapeSheet(
            onDismissRequest = { state.hide() },
        ) {
            content()
        }
    }
}