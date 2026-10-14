package love.yinlin.compose.ui.widget

import androidx.compose.runtime.*
import kotlinx.datetime.LocalDate
import kotlinx.datetime.number
import love.yinlin.compose.components.calendar.resources.Res
import love.yinlin.compose.extension.mutableRefStateOf
import love.yinlin.concurrent.Mutex

@Stable
internal object LunarLoader {
    @Stable
    private val lunarFestivalTable = mapOf(
        101 to "春节", 115 to "元宵", 202 to "龙抬头", 505 to "端午",
        707 to "七夕", 715 to "中元", 815 to "中秋", 909 to "重阳",
        1001 to "寒衣", 1208 to "腊八"
    )

    @Stable
    private val solarFestivalTable = mapOf(
        101 to "元旦", 214 to "情人节", 308 to "妇女节", 401 to "愚人节",
        501 to "劳动节", 601 to "儿童节", 701 to "建党节", 801 to "建军节",
        910 to "教师节", 1001 to "国庆节", 1101 to "万圣节", 1225 to "圣诞节"
    )

    @Stable
    private val lunarDayTable: Array<String> = ["", "一", "二", "三", "四", "五", "六", "七", "八", "九", "十"]

    @Stable
    private val lunarDaysTable: Array<String> = ["初", "十", "廿", "三"]

    @Stable
    private val lunarMonthTable: Array<String> = ["", "正", "二", "三", "四", "五", "六", "七", "八", "九", "十", "冬", "腊"]

    @Stable
    private val solarTermTable: Array<String> = [
        "小寒", "大寒", "立春", "雨水", "惊蛰", "春分",
        "清明", "谷雨", "立夏", "小满", "芒种", "夏至",
        "小暑", "大暑", "立秋", "处暑", "白露", "秋分",
        "寒露", "霜降", "立冬", "小雪", "大雪", "冬至"
    ]

    var table: ByteArray? by mutableRefStateOf(null)
        private set

    private val mutex = Mutex()

    @Composable
    fun Load() {
        LaunchedEffect(Unit) {
            if (table == null) {
                mutex.with {
                    if (table == null) table = Res.readBytes("files/lunar.bin")
                }
            }
        }
    }

    fun lunar(date: LocalDate): String {
        val lunarTable = table ?: return ".."
        val solarYear = date.year
        val solarMonth = date.month.number
        val solarDay = date.day
        val index = ((solarYear - 2000) * 12 * 31 + (solarMonth - 1) * 31 + (solarDay - 1)) * 2
        val byte0 = lunarTable[index].toInt() and 0xFF
        val byte1 = lunarTable[index + 1].toInt() and 0xFF
        val combined = (byte1 shl 8) or byte0
        val isTerm = (combined and 0x01) != 0
        val isLeap = (combined and 0x02) != 0
        // val isCurrentYear = (combined and 0x04) != 0
        val lunarMonth = ((combined ushr 3) and 0x0F) + 1
        val lunarDay = ((combined ushr 7) and 0x1F) + 1

        return solarFestivalTable[solarMonth * 100 + solarDay] ?: lunarFestivalTable[lunarMonth * 100 + lunarDay] ?:
        if (isTerm) solarTermTable[(solarMonth - 1) * 2 + solarDay / 16]
        else when (lunarDay) {
            1 -> "${if (isLeap) "闰" else ""}${lunarMonthTable[lunarMonth]}月"
            10 -> "初十"
            20 -> "二十"
            30 -> "三十"
            else -> "${lunarDaysTable[lunarDay / 10]}${lunarDayTable[lunarDay % 10]}"
        }
    }
}