package love.yinlin.data.weibo

object WeiboContainer {
//	fun searchUser(name: String): String {
//		val encodeName = try { URLEncoder.encode(name, "UTF-8") } catch (_: Exception) { name }
//		return "100103type%3D3%26q%3D$encodeName"
//	}
	fun weibo(uid: String): String = "107603$uid"
	fun album(uid: String): String = "107803$uid"
	const val CHAOHUA: String = "10080848e33cc4065cd57c5503c2419cdea983_-_sort_time"
}