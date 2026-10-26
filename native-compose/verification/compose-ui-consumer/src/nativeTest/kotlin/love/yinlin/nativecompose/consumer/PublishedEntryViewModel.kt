package love.yinlin.nativecompose.consumer

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel

@Stable
internal class PublishedEntryViewModel : ViewModel() {
    var cleared = false
        private set

    override fun onCleared() { cleared = true }
}
