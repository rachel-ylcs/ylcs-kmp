package love.yinlin.api

import io.ktor.client.call.*
import io.ktor.client.request.*
import kotlinx.coroutines.ensureActive
import kotlinx.serialization.json.JsonObject
import love.yinlin.app
import love.yinlin.data.Data
import love.yinlin.data.common.Picture
import love.yinlin.data.weibo.Weibo
import love.yinlin.data.weibo.WeiboContainer
import love.yinlin.data.weibo.WeiboUser
import love.yinlin.data.weibo.WeiboUserInfo
import love.yinlin.extension.arr
import love.yinlin.extension.int
import love.yinlin.extension.obj
import love.yinlin.extension.string
import love.yinlin.platform.Constants
import kotlin.coroutines.coroutineContext

object WeiboAPI {
	private fun getWeibo(card: JsonObject): Weibo {
		var blogs = card.obj("mblog")
		// 提取ID
		val blogId = blogs["id"].string
		// 提取名称和头像
		val user = blogs.obj("user")
		val userId = user["id"].string
		val userName = user["screen_name"].string
		val avatar = user["avatar_hd"].string
		// 提取时间
		val time = Weibo.formatTime(blogs["created_at"].string)
		// 提取IP
		val location = blogs["region_name"]?.string?.let {
			val index = it.indexOf(' ')
			if (index != -1) it.substring(index + 1) else it
		} ?: "IP未知"
		// 提取内容
		val text = blogs["text"].string
		// 提取数据
		val commentNum = blogs["comments_count"].int
		val likeNum = blogs["attitudes_count"].int
		val repostNum = blogs["reposts_count"].int
		blogs = blogs["retweeted_status"]?.obj ?: blogs // 转发微博
		// 图片微博
		val pictures = mutableListOf<Picture>()
		if (blogs.contains("pics")) {
			for (picItem in blogs.arr("pics")) {
				val pic = picItem.obj
				pictures += Picture(pic["url"].string, pic.obj("large")["url"].string)
			}
		} else if (blogs.contains("page_info")) {
			val pageInfo = blogs.obj("page_info")
			if (pageInfo["type"].string == "video") {
				val urls = pageInfo.obj("urls")
				val videoUrl = if (urls.contains("mp4_720p_mp4")) urls["mp4_720p_mp4"].string
				else if (urls.contains("mp4_hd_mp4")) urls["mp4_hd_mp4"].string
				else urls["mp4_ld_mp4"].string
				val videoPicUrl = pageInfo.obj("page_pic")["url"].string
				pictures += Picture(videoPicUrl, videoPicUrl, videoUrl)
			}
		}
		return Weibo(
			id = blogId,
			info = WeiboUserInfo(userId, userName, avatar),
			time = time,
			location = location,
			text = text,
			commentNum = commentNum,
			likeNum = likeNum,
			repostNum = repostNum,
			pictures = pictures
		)
	}

	suspend fun getUserWeibo(uid: String): Data<List<Weibo>> = try {
		val items = mutableListOf<Weibo>()
		val url = "https://${Constants.WEIBO_HOST}/api/container/getIndex?type=uid&value=$uid&containerid=${WeiboContainer.weibo(uid)}"
		val json = app.client.get(url).body<JsonObject>()
		val cards = json.obj("data").arr("cards")
		for (item in cards) {
			try {
				val card = item.obj
				if (card["card_type"].int != 9) continue  // 非微博类型
				items += getWeibo(card)
			}
			catch (_: Exception) {
				coroutineContext.ensureActive()
			}
		}
		Data.Success(items)
	}
	catch (_: Exception) {
		coroutineContext.ensureActive()
		Data.Error()
	}

	suspend fun getWeiboUser(uid: String): Data<WeiboUser> = try {
		val url = "https://${Constants.WEIBO_HOST}/api/container/getIndex?type=uid&value=$uid"
		val json = app.client.get(url).body<JsonObject>()
		val userInfo = json.obj("data").obj("userInfo")
		val id = userInfo["id"].string
		val name = userInfo["screen_name"].string
		val avatar = userInfo["avatar_hd"].string
		val background = userInfo["cover_image_phone"].string
		val signature = userInfo["description"].string
		val followNum = userInfo["follow_count"].string
		val fansNum = userInfo["followers_count_str"]?.string ?: userInfo["followers_count"].string
		Data.Success(WeiboUser(
			info = WeiboUserInfo(id, name, avatar),
			background = background,
			signature = signature,
			followNum = followNum,
			fansNum = fansNum
		))
	}
	catch (_: Exception) {
		coroutineContext.ensureActive()
		Data.Error()
	}
}