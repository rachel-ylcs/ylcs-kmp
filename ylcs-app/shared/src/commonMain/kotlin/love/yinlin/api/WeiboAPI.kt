package love.yinlin.api

import androidx.compose.runtime.Stable
import com.fleeksoft.ksoup.Ksoup
import com.fleeksoft.ksoup.nodes.Element
import com.fleeksoft.ksoup.nodes.Node
import com.fleeksoft.ksoup.nodes.TextNode
import io.ktor.http.HeadersBuilder
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.util.appendAll
import io.ktor.util.encodeBase64
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import love.yinlin.uri.Uri
import love.yinlin.data.compose.Picture
import love.yinlin.data.weibo.*
import love.yinlin.extension.*
import love.yinlin.platform.NetClient
import love.yinlin.platform.Platform
import love.yinlin.compose.ui.text.RichContainer
import love.yinlin.compose.ui.text.RichString
import love.yinlin.compose.ui.text.buildRichString

@Stable
data object WeiboAPI {
	private fun proxy(url: String) = Platform.use(Platform.WebWasm,
		ifTrue = { ClientEngine.proxy(APIConfig.PROXY_NAME, url) },
		ifFalse = { url }
	)

	private fun proxyRes(url: String) = Platform.use(Platform.WebWasm,
		ifTrue = {
			val cookie = "SUB=${weiboCookie?.sub};SUBP=${weiboCookie?.subp};XSRF-TOKEN=${weiboCookie?.xsrfToken}"
			val referer = "https://m.weibo.cn"
			"${ClientEngine.proxy(APIConfig.PROXY_NAME, url)}&Cookie=${Uri.encodeUri(cookie)}&Referer=${Uri.encodeUri(referer)}"
		},
		ifFalse = { url }
	)

	@Stable
	data class WeiboCookie(
		val sub: String,
		val subp: String,
		val xsrfToken: String
	)

	var weiboCookie: WeiboCookie? = null

	private object Container {
		fun searchUser(key: String): String = proxy("https://m.weibo.cn/api/container/getIndex?containerid=100103type%3D3%26q=${Uri.encodeUri(key)}&page_type=searchall")
		fun searchTopic(name: String): String = proxy("https://m.weibo.cn/search?containerid=231522type=1&q=$name")
		fun userDetails(uid: String): String = proxy("https://m.weibo.cn/api/container/getIndex?type=uid&value=$uid&containerid=107603$uid")
		fun userInfo(uid: String): String = proxy("https://m.weibo.cn/api/container/getIndex?type=uid&value=$uid")
		fun weiboDetails(uid: String): String = proxy("https://m.weibo.cn/comments/hotflow?id=$uid&mid=$uid")
		fun userAlbum(uid: String): String = proxy("https://m.weibo.cn/api/container/getIndex?type=uid&value=$uid&containerid=107803$uid")
		fun albumPics(containerId: String, page: Int, limit: Int): String = proxy("https://m.weibo.cn/api/container/getSecond?containerid=$containerId&count=$limit&page=$page")
		fun chaohua(sinceId: Long): String = proxy("https://m.weibo.cn/api/container/getIndex?containerid=10080848e33cc4065cd57c5503c2419cdea983_-_sort_time&type=uid&value=2266537042&since_id=$sinceId")
		fun href(route: String) = proxy("https://m.weibo.cn$route")
		val xsrfConfig: String get() = proxy("https://m.weibo.cn/api/config")
		val genvisitor2: String get() = proxy("https://visitor.passport.weibo.cn/visitor/genvisitor2")
	}

	private fun weiboHtmlNodeTransform(node: Node, container: RichContainer) {
		if (node is TextNode) {
			val text = node.text()
			if (text.startsWith('#') && text.endsWith('#')) { // # 话题
				val topic = text.removePrefix("#").removeSuffix("#")
				container.topic(Container.searchTopic(topic), text)
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
						if (href.startsWith("/n/")) container.at(Container.href(href), text) // @ 标签
						else if (href.startsWith("/status/")) container.link(Container.href(href), text)
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
		return buildRichString {
			weiboHtmlNodesTransform(html.childNodes(), this)
		}
	}

	private fun weiboTime(time: String) = DateEx.Formatter.weiboDateTime.parse(time)!!.toLocalDateTime()

	private fun getWeiboUserInfo(user: JsonObject): WeiboUserInfo {
		// 提取名称和头像
		val userId = user["id"].String
		val userName = user["screen_name"].String
		val avatar = proxyRes(user["avatar_hd"].String)
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
		if ("pics" in blogs) {
			for (picItem in blogs.arr("pics")) {
				val pic = picItem.Object
				pictures += Picture(
					image = proxyRes(pic["url"].String),
					source = proxyRes(pic.obj("large")["url"].String)
				)
			}
		} else if ("page_info" in blogs) {
			val pageInfo = blogs.obj("page_info")
			if (pageInfo["type"].String == "video") {
				val urls = pageInfo.obj("urls")
				val videoUrl = if ("mp4_720p_mp4" in urls) urls["mp4_720p_mp4"].String
				else if ("mp4_hd_mp4" in urls) urls["mp4_hd_mp4"].String
				else urls["mp4_ld_mp4"].String
				val videoPicUrl = pageInfo.obj("page_pic")["url"].String
				pictures += Picture(
					image = proxyRes(videoPicUrl),
					source = proxyRes(videoPicUrl),
					video = proxyRes(videoUrl)
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
		val pic = if ("pic" in card) {
			card.obj("pic").let {
				Picture(
					image = proxyRes(it["url"].String),
					source = proxyRes(it.obj("large")["url"].String)
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

	suspend fun generateWeiboCookie(): WeiboCookie {
		val xsrfToken = NetClient.request<ByteArray, String>({
			url = Container.xsrfConfig
		}) {
			cookies.filter { it.name.equals("XSRF-TOKEN", ignoreCase = true) }.first { !it.value.equals("deleted", ignoreCase = true) }.value
		} ?: "fukyou"
		val sub = NetClient.request(Container.genvisitor2, {
			method = HttpMethod.Post
			form = mapOf("cb" to "visitor_gray_callback")
		}) { text: String ->
			val json = text.substringAfter("(").substringBeforeLast(")").parseJson.Object
			val data = json.obj("data")
			data["sub"].String to data["subp"].String
		} ?: ("_2AkMeSKrwf8NxqwJRmvwUymjlZIh3zw_EieKoFFsrJRM3HRl-yT9yqhAgtRB6NciEEb-f-w8Zld8pGpTn4blqg02DqNuH" to "0033WrSXqPxfM72-Ws9jqgMF55529P9D9WhjLXMq867aPUPiUkd8wq4Y")
		return WeiboCookie(sub.first, sub.second, xsrfToken)
	}

	private val buildWeiboCookieHeader: HeadersBuilder.() -> Unit = {
		weiboCookie?.let { cookie ->
			val extraHeaders = mapOf(
				HttpHeaders.Cookie to "SUB=${cookie.sub};SUBP=${cookie.subp};XSRF-TOKEN=${cookie.xsrfToken}",
				HttpHeaders.Referrer to "https://m.weibo.cn"
			)
			Platform.use(Platform.WebWasm,
				ifTrue = { append("VHeaders", extraHeaders.toJsonString().encodeBase64()) },
				ifFalse = { appendAll(extraHeaders) }
			)
		}
	}

	suspend fun getUserWeibo(uid: String): List<Weibo>? = NetClient.request(Container.userDetails(uid), {
		headers(buildWeiboCookieHeader)
	}) { json: JsonObject ->
		val cards = json.obj("data").arr("cards")
		val items = mutableListOf<Weibo>()
		for (item in cards) {
			val card = item.Object
			if (card["card_type"].Int != 9) continue  // 非微博类型
			items += getWeibo(card)
		}
		items
	}

	suspend fun getWeiboDetails(id: String): List<WeiboComment>? = NetClient.request(Container.weiboDetails(id), {
		headers(buildWeiboCookieHeader)
	}) { json: JsonObject ->
		val cards = json.obj("data").arr("data")
		val items = mutableListOf<WeiboComment>()
		for (item in cards) items += getWeiboComment(item.Object)
		items
	}

	suspend fun getWeiboUser(uid: String): WeiboUser? = NetClient.request(Container.userInfo(uid), {
		headers(buildWeiboCookieHeader)
	}) { json: JsonObject ->
		val userInfo = json.obj("data").obj("userInfo")
		val id = userInfo["id"].String
		val name = userInfo["screen_name"].String
		val avatar = proxyRes(userInfo["avatar_hd"].String)
		val background = proxyRes(userInfo["cover_image_phone"].String)
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

	suspend fun getWeiboUserAlbum(uid: String): List<WeiboAlbum>? = NetClient.request(Container.userAlbum(uid), {
		headers(buildWeiboCookieHeader)
	}) { json: JsonObject ->
		val cards = json.obj("data").arr("cards")
		val items = mutableListOf<WeiboAlbum>()
		for (item1 in cards) {
			val card = item1.Object
			if (card["itemid"].String.endsWith("albumeach")) {
				for (item2 in card.arr("card_group")) {
					val album = item2.Object
					if (album["card_type"].Int == 8) {
						val containerId = Uri.parse(album["scheme"].String)!!.params["containerid"]!!
						items += WeiboAlbum(
							containerId = containerId,
							title = album["title_sub"].String,
							num = album["desc1"].String,
							time = album["desc2"].String,
							pic = proxyRes(album["pic"].String)
						)
					}
				}
			}
		}
		items
	}

	suspend fun getWeiboAlbumPics(
		containerId: String, page: Int, limit: Int
	): Pair<List<Picture>, Int>? = NetClient.request(Container.albumPics(containerId, page, limit), {
		headers(buildWeiboCookieHeader)
	}) { json: JsonObject ->
		val data = json.obj("data")
		val cards = data.arr("cards")
		val pics = mutableListOf<Picture>()
		for (item1 in cards) {
			val card = item1.Object
			for (item2 in card.arr("pics")) {
				val pic = item2.Object
				pics += Picture(
					image = proxyRes(pic["pic_middle"].String),
					source = proxyRes(pic["pic_ori"].String)
				)
			}
		}
		pics.take(limit) to data["count"].Int
	}

	suspend fun searchWeiboUser(key: String): List<WeiboUserInfo>? = NetClient.request(Container.searchUser(key), {
		headers(buildWeiboCookieHeader)
	}) { json: JsonObject ->
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
							avatar = proxyRes(user["avatar_hd"].String)
						)
					}
				}
			}
		}
		items
	}

	suspend fun extractChaohua(sinceId: Long): Pair<List<Weibo>, Long>? = NetClient.request(Container.chaohua(sinceId), {
		headers(buildWeiboCookieHeader)
	}) { json: JsonObject ->
		val data = json.obj("data")
		val newSinceId = data.obj("pageInfo")["since_id"].Long
		val cards = data.arr("cards")
		val items = mutableListOf<Weibo>()
		for (item in cards) {
			catching {
				val card = item.Object
				val cardType = card["card_type"].Int
				if (cardType == 11) {
					for (subCard in card["card_group"].ArrayEmpty) items += getWeibo(subCard.Object)
				}
				else if (cardType == 9) items += getWeibo(card)
			}
		}
		items to newSinceId
	}
}