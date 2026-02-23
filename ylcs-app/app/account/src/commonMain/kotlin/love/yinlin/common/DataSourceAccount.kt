package love.yinlin.common

import androidx.compose.runtime.Stable
import love.yinlin.app
import love.yinlin.compose.config.CacheState
import love.yinlin.compose.screen.DataSource
import love.yinlin.concurrent.atomic
import love.yinlin.cs.*
import love.yinlin.data.Data
import love.yinlin.extension.DateEx

@Stable
object DataSourceAccount : DataSource {
    private val isUpdateToken = atomic(false)

    fun cleanUserToken() {
        app.config.userShortToken = 0L
        app.config.userToken = ""
        app.config.userProfile = null
        app.config.cacheUserAvatar = CacheState.UPDATE
        app.config.cacheUserWall = CacheState.UPDATE
    }

    suspend fun updateUserToken(): Boolean {
        val token = app.config.userToken
        if (token.isNotEmpty() && isUpdateToken.compareAndSet(expect = false, update = true)) {
            val currentTime = DateEx.CurrentLong
            val duration = currentTime - app.config.userShortToken
            val isExpired = if (duration > 7 * 24 * 3600 * 1000L) { // 更新 Token
                ApiAccountUpdateToken.request(token) {
                    app.config.userShortToken = currentTime
                    app.config.userToken = it
                } is UnauthorizedException
            }
            else { // 校验 Token
                (ApiAccountValidateToken.request(token) as? Data.Success)?.data?.o1 == false
            }
            if (isExpired) {
                cleanUserToken()
                return false
            }
            isUpdateToken.value = false
        }
        return true
    }

    suspend fun updateUserProfile() {
        val token = app.config.userToken
        if (token.isNotEmpty() && !isUpdateToken.value) {
            ApiProfileGetProfile.request(token) {
                app.config.userProfile = it
            }
        }
    }

    suspend fun logoff() {
        val token = app.config.userToken
        if (token.isNotEmpty()) {
            ApiAccountLogOff.request(token) { }
            // 不论是否成功均从本地设备退出登录
            cleanUserToken()
        }
    }

    override fun onDataSourceClean() { }
}