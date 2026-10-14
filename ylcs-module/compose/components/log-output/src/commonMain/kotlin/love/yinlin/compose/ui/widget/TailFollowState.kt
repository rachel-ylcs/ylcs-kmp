package love.yinlin.compose.ui.widget

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

@Stable
internal class TailFollowState {
    var following by mutableStateOf(true)
        private set

    var unread by mutableIntStateOf(0)
        private set

    fun pause() {
        following = false
    }

    fun onUserViewport(atBottom: Boolean) {
        if (atBottom) resume() else pause()
    }

    fun onAppend(count: Int = 1) {
        if (!following) unread += count
    }

    fun resume() {
        following = true
        unread = 0
    }
}