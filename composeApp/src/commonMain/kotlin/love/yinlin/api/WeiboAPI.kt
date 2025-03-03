package love.yinlin.api

import androidx.core.uri.UriUtils
import com.fleeksoft.ksoup.Ksoup
import com.fleeksoft.ksoup.nodes.Element
import com.fleeksoft.ksoup.nodes.Node
import com.fleeksoft.ksoup.nodes.TextNode
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import love.yinlin.platform.app
import love.yinlin.ui.component.text.RichContainer
import love.yinlin.ui.component.text.RichString
import love.yinlin.data.Data
import love.yinlin.data.common.Picture
import love.yinlin.data.weibo.Weibo
import love.yinlin.data.weibo.WeiboAlbum
import love.yinlin.data.weibo.WeiboComment
import love.yinlin.data.weibo.WeiboSubComment
import love.yinlin.data.weibo.WeiboUser
import love.yinlin.data.weibo.WeiboUserInfo
import love.yinlin.extension.ArrayEmpty
import love.yinlin.extension.DateEx
import love.yinlin.extension.Int
import love.yinlin.extension.Long
import love.yinlin.extension.Object
import love.yinlin.extension.String
import love.yinlin.extension.StringNull
import love.yinlin.extension.arr
import love.yinlin.extension.obj
import love.yinlin.platform.OS
import love.yinlin.platform.safeGet
import love.yinlin.ui.component.text.buildRichString

object WeiboAPI {
	private const val WEIBO_SOURCE_HOST: String = "m.weibo.cn"
	private const val WEIBO_PROXY_HOST: String = "weibo.yinlin.love"
	private val WEIBO_HOST: String = if (OS.platform.isWeb) WEIBO_PROXY_HOST else WEIBO_SOURCE_HOST

	private fun transferWeiboIconUrl(src: String): String = if (OS.platform.isWeb) {
		src.replace("n.sinaimg.cn", "$WEIBO_PROXY_HOST/icon")
	} else src

	private fun transferWeiboImageUrl(src: String): String = if (OS.platform.isWeb) {
		if (src.contains("wx1.")) src.replace("wx1.sinaimg.cn", "$WEIBO_PROXY_HOST/image")
		else if (src.contains("wx2.")) src.replace("wx2.sinaimg.cn", "$WEIBO_PROXY_HOST/image")
		else if (src.contains("wx3.")) src.replace("wx3.sinaimg.cn", "$WEIBO_PROXY_HOST/image")
		else if (src.contains("wx4.")) src.replace("wx4.sinaimg.cn", "$WEIBO_PROXY_HOST/image")
		else if (src.contains("tvax1.")) src.replace("tvax1.sinaimg.cn", "$WEIBO_PROXY_HOST/image")
		else if (src.contains("tvax2.")) src.replace("tvax2.sinaimg.cn", "$WEIBO_PROXY_HOST/image")
		else if (src.contains("tvax3.")) src.replace("tvax3.sinaimg.cn", "$WEIBO_PROXY_HOST/image")
		else if (src.contains("tvax4.")) src.replace("tvax4.sinaimg.cn", "$WEIBO_PROXY_HOST/image")
		else src
	} else src

	private fun transferWeiboVideoUrl(src: String): String = if (OS.platform.isWeb) {
		src.replace("f.video.weibocdn.com", "$WEIBO_PROXY_HOST/video")
	} else src

	private object Container {
		fun searchUser(key: String): String {
			val encodeName = try { UriUtils.encode(key) } catch (_: Throwable) { key }
			return "api/container/getIndex?containerid=100103type%3D3%26q%3D$encodeName&page_type=searchall"
		}
		fun searchTopic(name: String): String = "search?containerid=231522type%3D1%26q%3D$name"
		fun userDetails(uid: String): String = "api/container/getIndex?type=uid&value=$uid&containerid=107603$uid"
		fun userInfo(uid: String): String = "api/container/getIndex?type=uid&value=$uid"
		fun weiboDetails(uid: String): String = "comments/hotflow?id=$uid&mid=$uid"
		fun userAlbum(uid: String): String = "api/container/getIndex?type=uid&value=$uid&containerid=107803$uid"
		fun albumPics(containerId: String, page: Int, limit: Int): String = "api/container/getSecond?containerid=$containerId&count=$limit&page=$page"
		fun chaohua(sinceId: Long): String = "api/container/getIndex?containerid=10080848e33cc4065cd57c5503c2419cdea983_-_sort_time&type=uid&value=2266537042&since_id=$sinceId"
	}

	private fun weiboHtmlNodeTransform(node: Node, container: RichContainer) {
		if (node is TextNode) {
			val text = node.text()
			if (text.startsWith('#') && text.endsWith('#')) { // # 话题
				val topic = text.removePrefix("#").removeSuffix("#")
				container.topic("https://$WEIBO_SOURCE_HOST/${Container.searchTopic(topic)}", text)
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
						if (href.startsWith("/n/")) container.at("https://$WEIBO_SOURCE_HOST$href", text) // @ 标签
						else if (href.startsWith("/status/")) container.link("https://$WEIBO_SOURCE_HOST$href", text)
						else container.link(href, text)
					}
					else weiboHtmlNodesTransform(childNodes, container)
				}
				"span" -> weiboHtmlNodesTransform(childNodes, container)
				"img" -> node.attribute("src")?.value?.let { container.image(transferWeiboIconUrl(it)) }
			}
		}
	}

	private fun weiboHtmlNodesTransform(nodes: List<Node>, container: RichContainer) {
		for (node in nodes) weiboHtmlNodeTransform(node, container)
	}

	private fun weiboHtmlToRichString(text: String): RichString {
		val html = Ksoup.parse(text).body()
		return buildRichString {
			weiboHtmlNodesTransform(html.childNodes(), this)
		}
	}

	private fun weiboTime(time: String) = DateEx.Formatter.weiboDateTime.parse(time).toLocalDateTime()

	private fun getWeiboUserInfo(user: JsonObject): WeiboUserInfo {
		// 提取名称和头像
		val userId = user["id"].String
		val userName = user["screen_name"].String
		val avatar = transferWeiboImageUrl(user["avatar_hd"].String)
		return WeiboUserInfo(
			id = userId,
			name = userName,
			avatar = avatar
		)
	}

	private fun getWeibo(card: JsonObject): Weibo {
		var blogs = card.obj("mblog")
		// 提取ID
		val blogId = blogs["id"].String
		val userInfo = getWeiboUserInfo(blogs.obj("user"))
		// 提取时间
		val time = weiboTime(blogs["created_at"].String)
		// 提取IP
		val location = blogs["region_name"]?.StringNull?.let {
			val index = it.indexOf(' ')
			if (index != -1) it.substring(index + 1) else it
		} ?: "IP未知"
		// 提取内容
		val text = blogs["text"].String
		// 提取数据
		val commentNum = blogs["comments_count"].Int
		val likeNum = blogs["attitudes_count"].Int
		val repostNum = blogs["reposts_count"].Int
		blogs = blogs["retweeted_status"]?.Object ?: blogs // 转发微博
		// 图片微博
		val pictures = mutableListOf<Picture>()
		if (blogs.contains("pics")) {
			for (picItem in blogs.arr("pics")) {
				val pic = picItem.Object
				pictures += Picture(
					image = transferWeiboImageUrl(pic["url"].String),
					source = transferWeiboImageUrl(pic.obj("large")["url"].String)
				)
			}
		} else if (blogs.contains("page_info")) {
			val pageInfo = blogs.obj("page_info")
			if (pageInfo["type"].String == "video") {
				val urls = pageInfo.obj("urls")
				val videoUrl = if (urls.contains("mp4_720p_mp4")) urls["mp4_720p_mp4"].String
				else if (urls.contains("mp4_hd_mp4")) urls["mp4_hd_mp4"].String
				else urls["mp4_ld_mp4"].String
				val videoPicUrl = pageInfo.obj("page_pic")["url"].String
				pictures += Picture(
					image = transferWeiboImageUrl(videoPicUrl),
					source = transferWeiboImageUrl(videoPicUrl),
					video = transferWeiboVideoUrl(videoUrl)
				)
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

	private fun getWeiboComment(card: JsonObject): WeiboComment {
		val commentId = card["id"].String
		// 提取名称和头像
		val userInfo = getWeiboUserInfo(card.obj("user"))
		// 提取时间
		val time = weiboTime(card["created_at"].String)
		// 提取IP
		val location = card["source"]?.StringNull?.removePrefix("来自") ?: "IP未知"
		// 提取内容
		val text = card["text"].String
		// 带图片
		val pic = if (card.containsKey("pic")) {
			card.obj("pic").let {
				Picture(
					image = transferWeiboImageUrl(it["url"].String),
					source = transferWeiboImageUrl(it.obj("large")["url"].String)
				)
			}
		} else null
		// 楼中楼
		val subComments = mutableListOf<WeiboSubComment>()
		val comments = card["comments"]
		if (comments as? JsonArray != null) {
			for (subCard in comments) {
				val subCardObj = subCard.Object
				subComments += WeiboSubComment(
					id = subCardObj["id"].String,
					info = getWeiboUserInfo(subCardObj.obj("user")),
					time = weiboTime(subCardObj["created_at"].String),
					location = subCardObj["source"]?.StringNull?.removePrefix("来自") ?: "IP未知",
					text = weiboHtmlToRichString(subCardObj["text"].String)
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

	suspend fun getUserWeibo(
		uid: String
	): Data<List<Weibo>> = app.client.safeGet(
		"https://$WEIBO_HOST/${Container.userDetails(uid)}"
	) { json: JsonObject ->
		val cards = json.obj("data").arr("cards")
		val items = mutableListOf<Weibo>()
		for (item in cards) {
			val card = item.Object
			if (card["card_type"].Int != 9) continue  // 非微博类型
			items += getWeibo(card)
		}
		items
	}

	suspend fun getWeiboDetails(
		id: String
	): Data<List<WeiboComment>> = app.client.safeGet(
		"https://$WEIBO_HOST/${Container.weiboDetails(id)}"
	) { json: JsonObject ->
		val cards = json.obj("data").arr("data")
		val items = mutableListOf<WeiboComment>()
		for (item in cards) items += getWeiboComment(item.Object)
		items
	}

	suspend fun getWeiboUser(
		uid: String
	): Data<WeiboUser> = app.client.safeGet(
		"https://$WEIBO_HOST/${Container.userInfo(uid)}"
	) { json: JsonObject ->
		val userInfo = json.obj("data").obj("userInfo")
		val id = userInfo["id"].String
		val name = userInfo["screen_name"].String
		val avatar = transferWeiboImageUrl(userInfo["avatar_hd"].String)
		val background = transferWeiboImageUrl(userInfo["cover_image_phone"].String)
		val signature = userInfo["description"].String
		val followNum = userInfo["follow_count"].String
		val fansNum = userInfo["followers_count_str"]?.StringNull ?: userInfo["followers_count"].String
		WeiboUser(
			info = WeiboUserInfo(id, name, avatar),
			background = background,
			signature = signature,
			followNum = followNum,
			fansNum = fansNum
		)
	}

	suspend fun getWeiboUserAlbum(
		uid: String
	): Data<List<WeiboAlbum>> = app.client.safeGet(
		"https://$WEIBO_HOST/${Container.userAlbum(uid)}"
	) { json: JsonObject ->
		val cards = json.obj("data").arr("cards")
		val items = mutableListOf<WeiboAlbum>()
		for (item1 in cards) {
			val card = item1.Object
			if (card["itemid"].String.endsWith("albumeach")) {
				for (item2 in card.arr("card_group")) {
					val album = item2.Object
					if (album["card_type"].Int == 8) {
						val containerId = UriUtils.parse(album["scheme"].String).getQueryParameters("containerid").first()
						items += WeiboAlbum(
							containerId = containerId,
							title = album["title_sub"].String,
							num = album["desc1"].String,
							time = album["desc2"].String,
							pic = transferWeiboImageUrl(album["pic"].String)
						)
					}
				}
			}
		}
		items
	}

	suspend fun getWeiboAlbumPics(
		containerId: String,
		page: Int,
		limit: Int
	): Data<Pair<List<Picture>, Int>> = app.client.safeGet(
		"https://$WEIBO_HOST/${Container.albumPics(containerId, page, limit)}"
	) { json: JsonObject ->
		val data = json.obj("data")
		val cards = data.arr("cards")
		val pics = mutableListOf<Picture>()
		for (item1 in cards) {
			val card = item1.Object
			for (item2 in card.arr("pics")) {
				val pic = item2.Object
				pics += Picture(
					image = transferWeiboImageUrl(pic["pic_middle"].String),
					source = transferWeiboImageUrl(pic["pic_ori"].String)
				)
			}
		}
		pics.take(limit) to data["count"].Int
	}

	suspend fun searchWeiboUser(
		key: String
	): Data<List<WeiboUserInfo>> = app.client.safeGet(
		"https://$WEIBO_HOST/${Container.searchUser(key)}"
	) { json: JsonObject ->
		val cards = json.obj("data").arr("cards")
		val items = mutableListOf<WeiboUserInfo>()
		for (item1 in cards) {
			val group = item1.Object
			if (group["card_type"].Int == 11) {
				for (item2 in group.arr("card_group")) {
					val card = item2.Object
					if (card["card_type"].Int == 10) {
						val user = card["user"].Object
						items += WeiboUserInfo(
							id = user["id"].String,
							name = user["screen_name"].String,
							avatar = transferWeiboImageUrl(user["avatar_hd"].String)
						)
					}
				}
			}
		}
		items
	}

	suspend fun extractChaohua(
		sinceId: Long
	): Data<Pair<List<Weibo>, Long>> = app.client.safeGet(
		"https://$WEIBO_HOST/${Container.chaohua(sinceId)}"
	) { json: JsonObject ->
		val data = json.obj("data")
		val newSinceId = data.obj("pageInfo")["since_id"].Long
		val cards = data.arr("cards")
		val items = mutableListOf<Weibo>()
		for (item in cards) {
			try {
				val card = item.Object
				val cardType = card["card_type"].Int
				if (cardType == 11) {
					for (subCard in card["card_group"].ArrayEmpty) items += getWeibo(subCard.Object)
				}
				else if (cardType == 9) items += getWeibo(card)
			}
			catch (_: Throwable) { }
		}
		items to newSinceId
	}
}