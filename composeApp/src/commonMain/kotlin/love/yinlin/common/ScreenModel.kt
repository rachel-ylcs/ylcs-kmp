package love.yinlin.common

import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

open class ScreenModel : ViewModel() {
	open fun initialize() { }

	fun launch(block: suspend CoroutineScope.() -> Unit): Job = viewModelScope.launch(block = block)
}

@Composable
inline fun <reified VM : ScreenModel> screen(crossinline factory: () -> VM): VM =
	viewModel { factory().also { it.initialize() } }