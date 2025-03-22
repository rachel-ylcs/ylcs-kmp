package love.yinlin.ui.component.screen

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier

@Stable
interface BaseSheetState<T> {
    @Composable fun withOpen(block: @Composable (T) -> Unit)
    val isOpen: Boolean
    fun open(value: T)
    fun hide()
}

class SheetState<T>(default: T? = null) : BaseSheetState<T> {
    private val state = mutableStateOf(default)

    @Composable override fun withOpen(block: @Composable (T) -> Unit) {
        state.value?.let { block(it) }
        DisposableEffect(Unit) { onDispose { hide() } }
    }
    override val isOpen: Boolean get() = state.value != null
    override fun open(value: T) { state.value = value }
    override fun hide() { state.value = null }
}

class BooleanSheetState(status: Boolean = false) : BaseSheetState<Unit> {
    private val state: MutableState<Boolean> = mutableStateOf(status)

    @Composable override fun withOpen(block: @Composable (Unit) -> Unit) {
        if (state.value) block(Unit)
        DisposableEffect(Unit) { onDispose { hide() } }
    }
    override val isOpen: Boolean get() = state.value
    override fun open(value: Unit) { state.value = true }
    fun open() = open(Unit)
    override fun hide() { state.value = false }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> BottomSheet(
    state: BaseSheetState<T>,
    content: @Composable () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = { state.hide() },
        dragHandle = null,
        modifier = Modifier.fillMaxWidth()
    ) {
        content()
    }
}