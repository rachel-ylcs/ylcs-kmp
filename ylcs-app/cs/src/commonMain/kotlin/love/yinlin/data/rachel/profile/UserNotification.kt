package love.yinlin.data.rachel.profile

import androidx.compose.runtime.Stable
import kotlinx.serialization.Serializable
import love.yinlin.data.rachel.account.SigninData

@Stable
@Serializable
data class UserNotification(
    val mailCount: Int = 0,
    val signinData: SigninData = SigninData.Empty
)