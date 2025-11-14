package love.yinlin.api

import love.yinlin.api.user.AN

fun APIScope.apiUser(implMap: ImplMap) {
    ApiUserUpdateAvatar.response { token, avatar ->
        val uid = AN.throwExpireToken(token)
        // 保存头像
        avatar.copy(ServerRes2.Users.User(uid).avatar)
    }
}