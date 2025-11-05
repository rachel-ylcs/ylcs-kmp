package love.yinlin.api

import androidx.compose.runtime.Stable
import love.yinlin.Local

@Stable
data object ClientAPI : ClientEngine(Local.API_BASE_URL)