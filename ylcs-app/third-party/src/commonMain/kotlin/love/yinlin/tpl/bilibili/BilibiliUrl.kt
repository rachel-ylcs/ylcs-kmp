package love.yinlin.tpl.bilibili

import androidx.compose.runtime.Stable

@Stable
object BilibiliUrl {
    const val LANDPAGE = "https://www.bilibili.com/"
    const val API = "https://api.bilibili.com"
    const val NAV = "$API/x/web-interface/nav"

    const val FEATURE = "itemOpusStyle,listOnlyfans,opusBigCover,onlyfansVote,forwardListHidden,decorationCard,commentsNewVersion,onlyfansAssetsV2,ugcDelete,onlyfansQaCard,avatarAutoTheme,sunflowerStyle,cardsEnhance,eva3CardOpus,eva3CardVideo,eva3CardComment,eva3CardUser"

    fun userInfo(params: String): String = "$API/x/space/wbi/acc/info?$params"
    fun userNotice(params: String): String = "$API/x/space/notice?$params"
    fun userDynamic(params: String): String = "$API/x/polymer/web-dynamic/v1/feed/space?$params"
    fun dynamicDetails(params: String): String = "$API/x/polymer/web-dynamic/v1/detail?$params"
}