package love.yinlin.api

import androidx.compose.runtime.Stable
import love.yinlin.Local

// TODO: 迁移升级
@Stable
data object ClientAPI2 : ClientEngine2(Local.API_BASE_URL)