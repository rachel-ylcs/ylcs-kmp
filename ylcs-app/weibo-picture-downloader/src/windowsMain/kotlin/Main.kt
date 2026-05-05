import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.headers
import io.ktor.util.appendAll
import kotlinx.cinterop.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonObject
import love.yinlin.extension.*
import love.yinlin.foundation.NetClient
import love.yinlin.fs.File
import love.yinlin.fs.StandardPath
import platform.windows.*

data class Weibo(
    val time: String,
    val text: String,
    val pictures: List<String>,
)

fun weiboTime(time: String) = DateEx.Formatter.weiboDateTime.parse(time)!!.toLocalDateTime()

suspend fun getUserWeibo(uid: String, extraHeaders: Map<String, String>): List<Weibo> {
    return NetClient.Common.request({
        url = "https://m.weibo.cn/api/container/getIndex?type=uid&value=$uid&containerid=107603$uid"
        headers = headers { appendAll(extraHeaders) }
    }) { json: JsonObject ->
        val cards = json.obj("data").arr("cards")
        val items = mutableListOf<Weibo>()
        for (item in cards) {
            val card = item.Object
            if (card["card_type"].Int != 9) continue  // 非微博类型
            var blogs = card.obj("mblog")
            val time = weiboTime(blogs["created_at"].String)
            val text = blogs["text"].String
            blogs = blogs["retweeted_status"]?.Object ?: blogs // 转发微博
            val pictures = mutableListOf<String>()
            if ("pics" in blogs) {
                for (picItem in blogs.arr("pics")) pictures += picItem.Object.obj("large")["url"].String.replace("mw2000", "large")
            }
            if (pictures.isNotEmpty()) items += Weibo(DateEx.Formatter.standardDateTime.format(time)!!, text, pictures)
        }
        items
    } ?: emptyList()
}

@OptIn(ExperimentalForeignApi::class)
fun runWebp(index: Int) = memScoped {
    val command = "cwebp.exe -lossless -mt ${index}.jpg -o ${index}.webp"
    val si = alloc<STARTUPINFOW>()
    si.cb = sizeOf<STARTUPINFOW>().toUInt()
    val pi = alloc<PROCESS_INFORMATION>()
    val commandPtr = command.wcstr.ptr
    // 启动进程
    val result = CreateProcessW(
        null,
        commandPtr,
        null,
        null,
        0,
        0.toUInt(),
        null,
        null,
        si.ptr,
        pi.ptr
    )

    if (result != 0) {
        WaitForSingleObject(pi.hProcess, INFINITE)
        CloseHandle(pi.hProcess)
        CloseHandle(pi.hThread)
    }
}

fun main() = runBlocking {

    // 1. 生成微博cookie

    val xsrfToken = NetClient.Common.request<ByteArray, String>({
        url = "https://m.weibo.cn/api/config"
    }) {
        cookies.filter { it.name.equals("XSRF-TOKEN", ignoreCase = true) }.first { !it.value.equals("deleted", ignoreCase = true) }.value
    } ?: "fku"

    val (sub, subp) = NetClient.Common.request({
        url = "https://visitor.passport.weibo.cn/visitor/genvisitor2"
        method = HttpMethod.Post
        form = mapOf("cb" to "visitor_gray_callback")
    }) { text: String ->
        val json = text.substringAfter("(").substringBeforeLast(")").parseJson.Object
        val data = json.obj("data")
        data["sub"].String to data["subp"].String
    } ?: ("_2AkMeSKrwf8NxqwJRmvwUymjlZIh3zw_EieKoFFsrJRM3HRl-yT9yqhAgtRB6NciEEb-f-w8Zld8pGpTn4blqg02DqNuH" to "0033WrSXqPxfM72-Ws9jqgMF55529P9D9WhjLXMq867aPUPiUkd8wq4Y")

    val extraHeaders = mapOf(
        HttpHeaders.Cookie to "SUB=${sub};SUBP=${subp};XSRF-TOKEN=${xsrfToken}",
        HttpHeaders.Referrer to "https://m.weibo.cn",
    )

    println("[Info] 获取cookie成功: sub=$sub, subp=$subp, XSRF-TOKEN=$xsrfToken\n")

    // 2. 获取微博

    val weiboList = getUserWeibo("2266537042", extraHeaders) + getUserWeibo("7802114712", extraHeaders)

    println("[Info] 正在获取微博列表...")

    weiboList.forEachIndexed { index, weibo ->
        val newText = weibo.text.filter { it.code > 255 }.take(20) + "..."
        println("[${index}] (图片: ${weibo.pictures.size}) ${weibo.time}|${newText}")
    }

    // 3. 下载图片
    println("[Input] 请选择要下载的序号 0 ~ ${weiboList.lastIndex}\n")
    val index = readln().toInt()

    val currentPath = File(StandardPath.Running.path).parent!!

    weiboList[index].pictures.forEachIndexed { index, pic ->
        println("[$index] 正在下载 $pic")
        val downloadFile = File(currentPath, "${index}.jpg")
        downloadFile.write { sink ->
            NetClient.File.download(pic, sink)
        }

        runWebp(index)
        downloadFile.delete()
    }
}