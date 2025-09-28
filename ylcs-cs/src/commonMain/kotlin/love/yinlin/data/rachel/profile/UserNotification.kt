package love.yinlin.data.rachel.profile

import androidx.compose.runtime.Stable
import kotlinx.serialization.Serializable

@Stable
@Serializable
data class UserNotification(
    val mailCount: Int = 0,
    val isSignin: Boolean = true
)