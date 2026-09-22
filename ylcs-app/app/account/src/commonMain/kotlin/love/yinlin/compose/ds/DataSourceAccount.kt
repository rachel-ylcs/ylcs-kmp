package love.yinlin.compose.ds

import androidx.compose.runtime.Stable
import love.yinlin.app
import love.yinlin.compose.config.CacheState
import love.yinlin.concurrent.atomic
import love.yinlin.cs.*
import love.yinlin.data.Data
import love.yinlin.data.map
import love.yinlin.data.rachel.profile.UserConstraint
import love.yinlin.data.rachel.server.ServerStatus
import love.yinlin.extension.DateEx
import love.yinlin.fs.File

@Stable
object DataSourceAccount {
    // 是否正在更新 token, 加锁
    private val isUpdateToken = atomic(false)

    fun cleanUserToken() {
        app.config.apply {
            userShortToken = 0L
            userToken = ""
            userProfile = null
            cacheUserAvatar = CacheState.UPDATE
            cacheUserWall = CacheState.UPDATE
        }
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

    suspend fun updateAvatar(path: File): Throwable? {
        return ApiProfileUpdateAvatar.request(app.config.userToken, apiFile(path)) {
            app.config.cacheUserAvatar = CacheState.UPDATE
        }
    }

    suspend fun updateWall(path: File): Throwable? {
        return ApiProfileUpdateWall.request(app.config.userToken, apiFile(path)) {
            app.config.cacheUserWall = CacheState.UPDATE
        }
    }

    suspend fun updateUserName(name: String): Throwable? {
        val profile = app.config.userProfile
        if (profile != null && profile.coin >= UserConstraint.RENAME_COIN_COST) {
            return ApiProfileUpdateName.request(app.config.userToken, name) {
                app.config.userProfile = profile.copy(name = name, coin = profile.coin - UserConstraint.RENAME_COIN_COST)
            }
        }
        return null
    }

    suspend fun updateUserSignature(signature: String): Throwable? {
        val profile = app.config.userProfile
        if (profile != null) {
            return ApiProfileUpdateSignature.request(app.config.userToken, signature) {
                app.config.userProfile = profile.copy(signature = signature)
            }
        }
        return null
    }

    suspend fun updatePassword(oldPwd: String, newPwd: String): Throwable? {
        val token = app.config.userToken
        if (token.isNotEmpty()) {
            return ApiAccountChangePassword.request(token, oldPwd, newPwd, ::cleanUserToken)
        }
        return null
    }

    suspend fun resetPicture(): Throwable? {
        val token = app.config.userToken
        if (token.isNotEmpty()) {
            return ApiProfileResetPicture.request(token) {
                app.config.cacheUserAvatar = CacheState.UPDATE
                app.config.cacheUserWall = CacheState.UPDATE
            }
        }
        return null
    }

    suspend fun sendFeedback(content: String): Throwable? {
        return (ApiCommonSendFeedback.request(app.config.userToken, content) as? Data.Failure)?.throwable
    }

    suspend fun checkUpdate(): Data<ServerStatus> {
        return ApiCommonGetServerStatus.request().map { it.o1 }
    }
}