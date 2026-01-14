package love.yinlin.uri

import love.yinlin.platform.Platform

object UriGenerator {
    fun qq(id: String): Uri = Uri(
        scheme = Scheme.QQ,
        host = "card",
        path = "/show_pslcard",
        query = "src_type=internal&version=1&uin=$id&card_type=person&source=qrcode"
    )

    fun qqGroup(id: String) = Uri(
        scheme = Scheme.QQ,
        host = "card",
        path = "/show_pslcard",
        query = "src_type=internal&version=1&uin=$id&card_type=group&source=qrcode"
    )

    fun qqGroupLink(q: String) = Uri(
        scheme = Scheme.Https,
        host = "qm.qq.com",
        path = "/q/${q}"
    )

    fun taobao(shopId: String): Uri = Platform.use(
        *Platform.Phone,
        ifTrue = { Uri(
            scheme = Scheme.Taobao,
            host = "shop.m.taobao.com",
            path = "/shop/shop_index.htm",
            query = "shop_id=$shopId"
        ) },
        ifFalse = {
            Uri(
                scheme = Scheme.Https,
                host = "shop$shopId.taobao.com"
            )
        }
    )
}