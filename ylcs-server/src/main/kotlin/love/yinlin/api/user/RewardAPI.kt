package love.yinlin.api.user

import io.ktor.server.routing.Routing
import love.yinlin.DB
import love.yinlin.api.ImplMap
import love.yinlin.api.successObject
import love.yinlin.data.rachel.mail.Mail

fun Routing.rewardAPI(implMap: ImplMap) {
    implMap[Mail.Filter.COIN_REWARD] = {
        val uid = it.uid
        val coin = it.param1?.toIntOrNull() ?: 0
        VN.throwIf(coin <= 0)
        DB.throwExecuteSQL("UPDATE user SET coin = coin + ? WHERE uid = ?", coin, uid)
        "领取成功".successObject
    }
}