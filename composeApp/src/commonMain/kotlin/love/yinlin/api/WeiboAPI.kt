package love.yinlin.api

import androidx.core.uri.UriUtils
import com.fleeksoft.ksoup.Ksoup
import com.fleeksoft.ksoup.nodes.Element
import com.fleeksoft.ksoup.nodes.Node
import com.fleeksoft.ksoup.nodes.TextNode
import io.ktor.client.call.*
import io.ktor.client.request.*
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import love.yinlin.app
import love.yinlin.ui.component.RichContainer
import love.yinlin.ui.component.RichString
import love.yinlin.data.Data
import love.yinlin.data.common.Picture
import love.yinlin.data.weibo.Weibo
import love.yinlin.data.weibo.WeiboAlbum
import love.yinlin.data.weibo.WeiboComment
import love.yinlin.data.weibo.WeiboSubComment
import love.yinlin.data.weibo.WeiboUser
import love.yinlin.data.weibo.WeiboUserInfo
import love.yinlin.extension.arr
import love.yinlin.extension.int
import love.yinlin.extension.obj
import love.yinlin.extension.string
import love.yinlin.platform.safeCall

expect val WEIBO_HOST: String

object WeiboAPI {
	object Container {
//		fun searchUser(name: String): String {
//			val encodeName = try { URLEncoder.encode(name, "UTF-8") } catch (_: Exception) { name }
//			return "100103type%3D3%26q%3D$encodeName"
//		}
		fun searchTopic(name: String): String = "search?containerid=231522type%3D1%26q%3D$name"
		fun userDetails(uid: String): String = "api/container/getIndex?type=uid&value=$uid&containerid=107603$uid"
		fun userInfo(uid: String): String = "api/container/getIndex?type=uid&value=$uid"
		fun weiboDetails(uid: String): String = "comments/hotflow?id=$uid&mid=$uid"
		fun userAlbum(uid: String): String = "api/container/getIndex?type=uid&value=$uid&containerid=107803$uid"
		const val CHAOHUA: String = "10080848e33cc4065cd57c5503c2419cdea983_-_sort_time"
	}

	private fun weiboHtmlNodeTransform(node: Node, container: RichContainer) {
		if (node is TextNode) {
			val text = node.text()
			if (text.startsWith('#') && text.endsWith('#')) { // # 话题
				val topic = text.removePrefix("#").removeSuffix("#")
				container.topic("https://$WEIBO_HOST/${Container.searchTopic(topic)}", text)
			}
			else container.text(text) // 普通文本
		}
		else if (node is Element) {
			val childNodes = node.childNodes()
			when (node.tagName()) {
				"br" -> container.br()
				"a" -> {
					val href = node.attribute("href")?.value
					val text = (childNodes.firstOrNull() as? TextNode?)?.text()
					if (childNodes.size == 1 && href != null && text != null) {
						if (href.startsWith("/n/")) container.at("https://$WEIBO_HOST$href", text) // @ 标签
						else if (href.startsWith("/status/")) container.link("https://$WEIBO_HOST$href", text)
						else container.link(href, text)
					}
					else weiboHtmlNodesTransform(childNodes, container)
				}
				"span" -> weiboHtmlNodesTransform(childNodes, container)
				"img" -> node.attribute("src")?.value?.let { container.image(it) }
			}
		}
	}

	private fun weiboHtmlNodesTransform(nodes: List<Node>, container: RichContainer) {
		for (node in nodes) weiboHtmlNodeTransform(node, container)
	}

	private fun weiboHtmlToRichString(text: String): RichString {
		val html = Ksoup.parse(text).body()
		return RichString {
			weiboHtmlNodesTransform(html.childNodes(), this)
		}
	}

	private fun getWeiboUserInfo(user: JsonObject): WeiboUserInfo {
		// 提取名称和头像
		val userId = user["id"].string
		val userName = user["screen_name"].string
		val avatar = user["avatar_hd"].string
		return WeiboUserInfo(
			id = userId,
			name = userName,
			avatar = avatar
		)
	}

	private fun getWeibo(card: JsonObject): Weibo {
		var blogs = card.obj("mblog")
		// 提取ID
		val blogId = blogs["id"].string
		val userInfo = getWeiboUserInfo(blogs.obj("user"))
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
			info = userInfo,
			time = time,
			location = location,
			text = weiboHtmlToRichString(text),
			commentNum = commentNum,
			likeNum = likeNum,
			repostNum = repostNum,
			pictures = pictures
		)
	}

	suspend fun getUserWeibo(uid: String): Data<List<Weibo>> = app.client.safeCall { client ->
		val url = "https://$WEIBO_HOST/${Container.userDetails(uid)}"
		val json = client.get(url).body<JsonObject>()
		val cards = json.obj("data").arr("cards")
		val items = mutableListOf<Weibo>()
		for (item in cards) {
			val card = item.obj
			if (card["card_type"].int != 9) continue  // 非微博类型
			items += getWeibo(card)
		}
		items
	}

	private fun getWeiboComment(card: JsonObject): WeiboComment {
		val commentId = card["id"].string
		// 提取名称和头像
		val userInfo = getWeiboUserInfo(card.obj("user"))
		// 提取时间
		val time = Weibo.formatTime(card["created_at"].string)
		// 提取IP
		val location = card["source"]?.string?.removePrefix("来自") ?: "IP未知"
		// 提取内容
		val text = card["text"].string
		// 带图片
		val pic = if (card.containsKey("pic")) {
			card.obj("pic").let { Picture(it["url"].string, it.obj("large")["url"].string) }
		} else null
		// 楼中楼
		val subComments = mutableListOf<WeiboSubComment>()
		val comments = card["comments"]
		if (comments as? JsonArray != null) {
			for (subCard in comments) {
				val subCardObj = subCard.obj
				subComments += WeiboSubComment(
					id = subCardObj["id"].string,
					info = getWeiboUserInfo(subCardObj.obj("user")),
					time = Weibo.formatTime(subCardObj["created_at"].string),
					location = subCardObj["source"]?.string?.removePrefix("来自") ?: "IP未知",
					text = weiboHtmlToRichString(subCardObj["text"].string)
				)
			}
		}
		return WeiboComment(
			id = commentId,
			info = userInfo,
			time = time,
			location = location,
			text = weiboHtmlToRichString(text),
			pic = pic,
			subComments = subComments
		)
	}

	suspend fun getWeiboDetails(id: String): Data<List<WeiboComment>> = app.client.safeCall { client ->
		val url = "https://$WEIBO_HOST/${Container.weiboDetails(id)}"
		val json = client.get(url).body<JsonObject>()
		val cards = json.obj("data").arr("data")
		val items = mutableListOf<WeiboComment>()
		for (item in cards) items += getWeiboComment(item.obj)
		items
	}

	suspend fun getWeiboUser(uid: String): Data<WeiboUser> = app.client.safeCall { client ->
		val url = "https://${WEIBO_HOST}/${Container.userInfo(uid)}"
		val json = client.get(url).body<JsonObject>()
		val userInfo = json.obj("data").obj("userInfo")
		val id = userInfo["id"].string
		val name = userInfo["screen_name"].string
		val avatar = userInfo["avatar_hd"].string
		val background = userInfo["cover_image_phone"].string
		val signature = userInfo["description"].string
		val followNum = userInfo["follow_count"].string
		val fansNum = userInfo["followers_count_str"]?.string ?: userInfo["followers_count"].string
		WeiboUser(
			info = WeiboUserInfo(id, name, avatar),
			background = background,
			signature = signature,
			followNum = followNum,
			fansNum = fansNum
		)
	}

	suspend fun getWeiboUserAlbum(uid: String): Data<List<WeiboAlbum>> = app.client.safeCall { client ->
		val url = "https://${WEIBO_HOST}/${Container.userAlbum(uid)}"
		val json = client.get(url).body<JsonObject>()
		val cards = json.obj("data").arr("cards")
		val items = mutableListOf<WeiboAlbum>()
		for (item1 in cards) {
			val card = item1.obj
			if (card["itemid"].string.endsWith("albumeach")) {
				for (item2 in card.arr("card_group")) {
					val album = item2.obj
					if (album["card_type"].int == 8) {
						val containerId = UriUtils.parse(album["scheme"].string).getQueryParameters("containerid").first()
						items += WeiboAlbum(
							containerId = containerId,
							title = album["title_sub"].string,
							num = album["desc1"].string,
							time = album["desc2"].string,
							pic = album["pic"].string
						)
					}
				}
			}
		}
		items
	}
}