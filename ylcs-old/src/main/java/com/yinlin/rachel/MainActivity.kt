@file:SuppressLint("UnsafeOptInUsageError")
package com.yinlin.rachel

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.tencent.mmkv.MMKV
import com.tencent.mmkv.MMKVLogLevel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.io.asSink
import kotlinx.io.asSource
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File

val musicMap = mapOf(
    "腐草为萤" to "1",
    "棠梨煎雪" to "2",
    "泸沽寻梦" to "3",
    "锦鲤抄" to "4",
    "洒拓歌" to "5",
    "落梅笺" to "6",
    "浮生辞" to "7",
    "故城" to "8",
    "情囚" to "9",
    "Backseat Stargazer" to "10",
    "Zodiac" to "11",
    "是风动" to "12",
    "如一" to "13",
    "不老梦" to "14",
    "青山揽梦" to "15",
    "裁梦为魂" to "16",
    "灼" to "17",
    "不离" to "18",
    "春笺" to "19",
    "秋水" to "20",
    "说余梦" to "21",
    "卑微情书" to "22",
    "金陵谣" to "23",
    "偏偏" to "24",
    "诗与岁月同歌" to "25",
    "七夕" to "26",
    "风烟倦" to "27",
    "谁能不思量" to "28",
    "商女恨" to "29",
    "牵丝戏" to "30",
    "流光记" to "31",
    "人间纵月" to "32",
    "乐游记" to "33",
    "云梦心曲" to "34",
    "且笑红尘" to "35",
    "何须问" to "36",
    "惹" to "37",
    "珍珠" to "38",
    "焦骨" to "39",
    "无题雪" to "40",
    "月球" to "41",
    "琉璃" to "42",
    "终身成就" to "43",
    "迟迟" to "44",
    "西施江南" to "45",
    "无际涯" to "46",
    "白噪音" to "47",
    "玫瑰与泪" to "48",
    "海棠春睡" to "49",
    "美人灯" to "50",
    "记忘歌" to "51",
    "亲爱的瑞秋" to "52",
    "花与酒" to "53",
    "拒霜思" to "54",
    "游园惊梦" to "55",
    "红杏枝头春意闹" to "56",
    "意难平" to "57",
    "无人生还" to "58",
    "雪花" to "59",
    "有一封信" to "60",
    "你是" to "61",
    "眉南边" to "62",
    "一命矣" to "63",
    "窗前明月光" to "64",
    "见夏如晤" to "65",
    "日出前起飞" to "66",
    "Drive Until Sunset" to "67",
    "天意安排" to "68",
    "万有引力" to "69",
    "快雪" to "70",
    "枕万梦" to "71",
    "笔底知交" to "72",
    "春风化雨" to "73",
    "澹月秋千" to "74",
    "山居秋暝" to "75",
    "眠花去" to "76",
    "沧海飞尘" to "77",
    "夜国" to "78",
    "愚人歌" to "79",
    "少女庄周" to "80",
    "我生于野" to "81",
    "魄心" to "82",
    "幻海同游" to "83",
    "折柳记" to "84",
    "你奔向春野" to "85",
    "小倩" to "86",
    "山色有无中" to "87",
    "眉目如画" to "88",
    "天若灵犀" to "89",
    "十世镜" to "90",
    "邻里天涯" to "91",
    "雪舞" to "92",
    "花鸟卷" to "93",
    "青原樱" to "94",
    "晴川雪" to "95",
    "千秋令" to "96",
    "可不可以" to "97",
    "Emmm" to "98",
    "孔雀辞" to "99",
    "爱的停格" to "100",
    "云川雪青" to "101",
    "盗橘令" to "102",
    "长安幻世绘" to "103",
    "旧友" to "104",
    "弦外知音" to "105",
    "徒然梦" to "106",
    "青白" to "107",
    "山外云" to "108",
    "一场雨的瞬间" to "109",
    "东风志" to "110",
    "往岁乘霄" to "111",
    "山海侧" to "112",
    "梦独吟" to "113",
    "暂借问" to "114",
    "半生时光" to "115",
    "诛仙敕勒歌" to "116",
    "情戒" to "117",
    "远行歌" to "118",
    "人间最值得" to "119",
    "一梦逍遥" to "120",
    "潜别离" to "121",
    "芳华旧" to "122",
    "故友" to "123",
    "锦瑟长思" to "124",
    "贺新谣" to "125",
    "夏目的美丽日记" to "126",
    "生死劫" to "127",
    "特雷西亚的远行" to "128",
    "苦竹林" to "129",
    "谢却荼蘼" to "130",
    "飞行安全颂" to "131",
    "且邀江山共白首" to "132",
    "云梦谣" to "133",
    "大美江湖" to "134",
    "知了的歌" to "135",
    "怀梦之泽" to "136",
    "梅深不见冬" to "137",
    "一杯月" to "138",
    "妙笔浮生" to "139",
    "闻妖" to "140",
    "问琴" to "141",
    "惊鸿" to "142",
    "鼎立山河" to "143",
    "神木有灵" to "144",
    "手绘的美好" to "145",
    "杏花微雨时" to "146",
    "迷画" to "147",
    "九愿" to "148",
    "一梦情深" to "149",
    "柳仙儿" to "150",
    "无念" to "151",
    "数红" to "152",
    "今宵" to "153",
    "画狐" to "154",
    "春江无明月" to "155",
    "摸鱼儿半阙·桃花妖" to "156",
    "鱼雁说" to "157",
    "落英" to "158",
    "我是你的雨" to "159",
    "春" to "160",
    "洛神赋" to "161",
    "秋水无恙" to "162",
    "月下逢君" to "163",
    "凡尘与你" to "164",
    "过空门" to "165",
    "忆千年" to "166",
    "初心" to "167",
    "种相思" to "168",
    "扶摇一梦" to "169",
    "洇雪" to "170",
    "船儿摇过海" to "171",
    "七夕·珍馐记" to "172",
    "传刀" to "173",
    "花开自在" to "174",
    "青花" to "175",
    "春读" to "176",
    "春归" to "177",
    "青城山下白素贞" to "178",
    "江山行歌" to "179",
    "请君入梦" to "180",
    "离家最近的路" to "181",
    "不夜侯" to "182",
    "生而为匠" to "183",
    "七夕·珍馐记" to "184",
    "以梦为裳" to "185",
    "风月本是故人心" to "186",
    "双契辞" to "187",
    "菩萨蛮" to "188",
    "谢谢陌生的你" to "189",
    "可期" to "190",
    "宝贝，回家" to "191",
    "大唐西游历险记" to "192",
    "玉剑掀澜" to "193",
    "玉剑风流" to "194",
    "牵绊" to "195",
    "盛世" to "196",
    "红豆词" to "197",
    "离殇" to "198",
    "寻仙歌" to "199",
    "一人一花" to "200",
    "傀" to "201",
    "往事一杯酒" to "202",
    "次元传说" to "203",
    "少年中国" to "204",
    "不负时光" to "205",
    "茉莉花" to "206",
    "冠世一战" to "207",
    "醉" to "208",
    "不挂科DJ版" to "209",
    "最美的相逢" to "210"
)

val Json = kotlinx.serialization.json.Json {
    ignoreUnknownKeys = true
}

var weibo by mutableStateOf("")

@Serializable
data class OldUser(val name: String, val userId: String, val avatar: String = "")
@Serializable
data class NewUser(val id: String, val name: String)

var playlist by mutableStateOf("")

@Serializable
data class Playlist(val name: String, val items: List<String>)

fun getKVData(context: Context) {
    MMKV.initialize(context, MMKVLogLevel.LevelNone)
    val kv = MMKV.defaultMMKV()
    runCatching {
        val userString = kv.decodeString("weibo_users", "") ?: ""
        val users  = Json.decodeFromString<List<OldUser>>(userString)
        weibo = Json.encodeToString(users.map { NewUser(it.userId, it.name) })
    }
    runCatching {
        val userString = kv.decodeString("playlist", "") ?: ""
        val playlistMap  = Json.decodeFromString<Map<String, Playlist>>(userString).mapValues { (key, value) ->
            Playlist(key, value.items.map { musicMap[it] ?: "" }.filter { it.isNotEmpty() })
        }
        playlist = Json.encodeToString(playlistMap)
    }
}

var isTransfer by mutableStateOf(false)

data class TransferData(
    val source: String,
    val id: String,
    val target: String,
    val config: String? = null
)

@Composable
fun TransferMod() {
    val context = LocalContext.current
    var step by remember { mutableIntStateOf(1) }
    var progress by remember { mutableFloatStateOf(0f) }
    var info by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize().padding(20.dp).verticalScroll(rememberScrollState())) {
        Text(text = "1. 此页面为迁移MOD页, 如需返回导出歌单或微博请退出重进")
        Text(text = "2. 迁移分为3个步骤, 当步骤3进度到达100%后即代表完成, 即可彻底卸载旧版APP")
        Text(text = "3. 迁移成功后的MOD(每30首打一个包)保存在系统下载(Downloads)目录, 自行导入到新版APP中")
        Text(text = "4. 迁移过程请不要任何关闭动作, 等待进度完成，否则迁移失败数据丢失")
        Text(text = "迁移进度 步骤${step} $progress%", color = Color.Red)
        Spacer(modifier = Modifier.height(20.dp))
        Text(text = "迁移日志")
        Text(text = info)
    }

    LaunchedEffect(Unit) {
        val names = mutableSetOf<String>()

        withContext(Dispatchers.IO) {
            try {
                step = 1
                progress = 0f

                val allData = mutableListOf<TransferData>()
                val path = File(context.filesDir, "music")
                val fileList = path.listFiles()!!
                var total = fileList.size
                fileList.forEachIndexed { fileIndex, file ->
                    withContext(Dispatchers.Main) {
                        progress = ((fileIndex + 1) * 100f / total)
                    }
                    try {
                        val filename = file.name
                        val extension = file.extension
                        val name = file.nameWithoutExtension
                        val arr = filename.split('_')
                        val (actualName, actualType) = when (arr.size) {
                            2 -> arr[0] to arr[1]
                            1 -> name to extension
                            else -> error("")
                        }

                        withContext(Dispatchers.Main) {
                            names += actualName
                            info = names.joinToString("\n")
                        }
                        val id = musicMap[actualName] ?: return@forEachIndexed

                        val data = when (actualType) {
                            "flac" -> TransferData(
                                source = filename,
                                id = id,
                                target = "10-flac"
                            )
                            "record.webp" -> TransferData(
                                source = filename,
                                id = id,
                                target = "20-record"
                            )
                            "bgs.webp" -> TransferData(
                                source = filename,
                                id = id,
                                target = "21-background"
                            )
                            "bgd.webp" -> TransferData(
                                source = filename,
                                id = id,
                                target = "22-animation"
                            )
                            "lrc" -> TransferData(
                                source = filename,
                                id = id,
                                target = "30-lrc"
                            )
                            "mp4" -> TransferData(
                                source = filename,
                                id = id,
                                target = "40-pv"
                            )
                            "json" -> TransferData(
                                source = filename,
                                id = id,
                                target = "0-config",
                                config = run {
                                    val json = Json.decodeFromString<JsonObject>(file.readText())
                                    val newJson = buildJsonObject {
                                        put("version", json["version"]!!)
                                        put("author", json["author"]!!)
                                        put("id", JsonPrimitive(id))
                                        put("name", json["name"]!!)
                                        put("singer", json["singer"]!!)
                                        put("lyricist", json["lyricist"]!!)
                                        put("composer", json["composer"]!!)
                                        put("album", json["album"]!!)
                                        put("chorus", json["chorus"]!!)
                                    }
                                    Json.encodeToString(newJson)
                                }
                            )
                            else -> null
                        }
                        data?.let { allData += it }
                    }
                    catch (_: Throwable) {}
                }

                step = 2
                progress = 0f
                total = allData.size

                val tmpPath = File(context.filesDir, "tmp")
                if (tmpPath.exists()) tmpPath.deleteRecursively()
                tmpPath.mkdirs()
                for ((fileIndex, data) in allData.withIndex()) {
                    withContext(Dispatchers.Main) {
                        progress = ((fileIndex + 1) * 100f / total)
                    }
                    val tmpOutputPath = File(tmpPath, data.id)
                    tmpOutputPath.mkdirs()
                    if (data.config == null) {
                        File(path, data.source).copyTo(
                            File(tmpOutputPath, data.target),
                            true
                        )
                    }
                    else File(tmpOutputPath, data.target).writeText(data.config)
                }

                val tmpList = tmpPath.listFiles()!!.toList().chunked(30)

                step = 3
                progress = 0f
                total = tmpList.size

                for ((index, files) in tmpList.withIndex()) {
                    val contentValues = ContentValues().apply {
                        put(MediaStore.Downloads.DISPLAY_NAME, "银临茶舍MOD迁移包${index + 1}_${System.currentTimeMillis()}.rachel")
                        put(MediaStore.Downloads.MIME_TYPE, "application/octet-stream")
                        put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                    }
                    val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)!!
                    context.contentResolver.openOutputStream(uri)!!.asSink().buffered().use { sink ->
                        ModFactory.Merge(
                            mediaPaths = files.map { Path(it.absolutePath) },
                            sink = sink
                        ).process { a, b, name ->
                            progress = ((index + 1) * 100f / total) * ((a + 1f) / b)
                            info = "资源: $name"
                        }
                    }
                    context.contentResolver.update(uri, contentValues, null, null)
                }
            }
            catch (e: Throwable) {
                println(e.stackTraceToString())
                info = "迁移失败\n\n${e.stackTraceToString()}"
                isTransfer = false
            }
        }
    }
}

@Composable
fun App(padding: PaddingValues) {
    if (isTransfer) TransferMod()
    else {
        Column(modifier = Modifier.fillMaxSize()
            .padding(padding)
            .verticalScroll(rememberScrollState())
        ) {
            Text(text = "★★★★点击下方迁移MOD★★★★", color = Color.Red)
            Button(onClick = { isTransfer = true }) { Text("迁移MOD") }
            Text(text = "★★★★全选下方微博关注复制导入★★★★", color = Color.Red)
            SelectionContainer { Text(text = weibo) }
            Text(text = "★★★★全选下方歌单复制导入★★★★", color = Color.Red)
            SelectionContainer { Text(text = playlist) }
        }
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        getKVData(this)

        setContent {
            MaterialTheme {
                Scaffold(modifier = Modifier.fillMaxSize().padding(20.dp)) { padding ->
                    App(padding)
                }
            }
        }
    }
}