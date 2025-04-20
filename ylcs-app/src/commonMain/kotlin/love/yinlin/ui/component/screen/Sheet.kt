package love.yinlin.ui.component.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> BottomSheet(
    state: BaseSheetState<T>,
    content: @Composable () -> Unit
) {
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