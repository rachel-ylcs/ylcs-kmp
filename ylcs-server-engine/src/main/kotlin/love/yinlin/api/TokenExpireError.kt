package love.yinlin.api

class TokenExpireError(uid: Int) : Throwable() {
    override val message: String = "TokenExpireError $uid"
}