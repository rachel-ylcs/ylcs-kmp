package love.yinlin.data.rachel.rhyme

import androidx.compose.runtime.Stable
import kotlinx.serialization.Serializable

@Stable
@Serializable
enum class CharacterInfo(
    val id: Int, // ID
    val title: String, // 名称
    val description: String, // 描述
    val rawSkill: String, // 技能
    val cost: Int, // 花费
    val enabled: Boolean = true, // 是否开启
    val metadata: Map<String, CharacterMetadata> = emptyMap(), // 元信息
) {
    ChuXing(
        id = 1,
        title = "初心",
        description = """
守紧我源自星空下的承诺
激动的邂逅 命运的枷锁
相遇莫错过
        """.trimIndent(),
        rawSkill = """
A. 坚守初心定有回报
        """.trimIndent(),
        cost = 0
    ),
    DuanWei(
        id = 2,
        title = "断尾",
        description = """
一滩污水照清你是谁
面目早已全非
你是谁都无甚所谓
路分南北永别不再会
也算天公作美
不再会才人心快慰
        """.trimIndent(),
        rawSkill = """
A. 音轨到达最后一句时所有音符以Perfect结算
        """.trimIndent(),
        cost = 10
    ),
    FuCaoWeiYing(
        id = 3,
        title = "腐草为萤",
        description = """
清浅池塘边 重生破土的冲动
天地正玲珑 殡葬了飞虫
迢迢河汉间 有磷火坠地如彗锋
奢望着能生死相拥
        """.trimIndent(),
        rawSkill = """
A. 你的第{maxGoodCount}个Good转化为Perfect结算并重新计数
        """.trimIndent(),
        cost = 10,
        metadata = mapOf(
            "maxGoodCount" to CharacterMetadata.MInt(5), // 最大Good数
        )
    ),
    PiFuDuHai(
        id = 4,
        title = "蚍蜉渡海",
        description = """
声嘶力竭 向悲泣的虚空 祈祷至最后
神为何 依然残酷冷漠
天国或地狱 也奢求 你无垢眼眸
就让我 再次被你拯救
        """.trimIndent(),
        rawSkill = """
A. 你具有额外{range}的视野
        """.trimIndent(),
        cost = 10,
        metadata = mapOf(
            "range" to CharacterMetadata.MPercent(0.3f), // 视野
        )
    ),
    ChiChi(
        id = 5,
        title = "迟迟",
        description = """
只如初见 人间该太过美满
美人难免 总要悲一悲画扇
可意难平到后来若真是堪堪
那又该多不堪
        """.trimIndent(),
        rawSkill = """
A. 你具有额外{range}的准备时间
        """.trimIndent(),
        cost = 10,
        metadata = mapOf(
            "range" to CharacterMetadata.MPercent(0.3f), // 额外时间
        )
    ),
    BuLi(
        id = 6,
        title = "不离",
        description = """
是天地一埃尘 饰我床头对枕
作浪形一楚人 做你眉头金粉
唤旧事一念恩 还我千刀新痕
静悄悄刻铭文 惊你心头夜奔
        """.trimIndent(),
        rawSkill = """
A. 你的Bad结算有{probability}概率不重置连击
        """.trimIndent(),
        cost = 10,
        metadata = mapOf(
            "probability" to CharacterMetadata.MPercent(0.3f),
        )
    ),
    WuJiYa(
        id = 7,
        title = "无际涯",
        description = """
有多少烟涛填不满
有多少蜃楼望不穿
有多少天涯走不完
我才是沧流中无际彼岸
        """.trimIndent(),
        rawSkill = """
A. 音轨到达一句的最后一个音符以Perfect结算
        """.trimIndent(),
        cost = 10
    ),
    LiDiShiGongFenA(
        id = 8,
        title = "离地十公分A面",
        description = """
诉尽无尽事
回头回声捡
人寻找真切
假想爱里面
        """.trimIndent(),
        rawSkill = """
A. 你看不见局内所有未完成结算的音符
B. 你的所有Bad转化为Good结算
        """.trimIndent(),
        cost = 10
    ),
    LiDiShiGongFenB(
        id = 9,
        title = "离地十公分B面",
        description = """
可何时才能相见
趁沙漏中仍剩余时间
就让我们 赶在日出之前
有多远 就飞多远
        """.trimIndent(),
        rawSkill = """
A. 你看不见局内大部分UI信息
B. 你看不见局内背景
C. 你看不见局内大部分动画效果
D. 你听不见局内音效
        """.trimIndent(),
        cost = 10
    ),
    WuNian(
        id = 10,
        title = "无念",
        description = """
离离芳菲舞流光
漫漫长路冷冷尘霜
亦真亦幻落星相望
旧梦依稀人路茫茫
        """.trimIndent(),
        rawSkill = """
A. 你具有{maxCount}次忽略错按的机会
        """.trimIndent(),
        cost = 10,
        metadata = mapOf(
            "maxCount" to CharacterMetadata.MInt(10)
        )
    ),
    ShengSiJie(
        id = 11,
        title = "生死劫",
        description = """
我梦中流连的云竹青山
井中古月又轮了几番圆满
但求来世将这情字落款
今生命里劫数难转
        """.trimIndent(),
        rawSkill = """
A. 你具有{maxCount}次将Miss转化为Good的机会
        """.trimIndent(),
        cost = 10,
        metadata = mapOf(
            "maxCount" to CharacterMetadata.MInt(3)
        )
    ),
    JingLiChao(
        id = 12,
        title = "锦鲤抄",
        description = """
晨曦惊扰了陌上新桑
风卷起庭前落花穿过回廊
浓墨追逐着情绪流淌
染我素衣白裳
        """.trimIndent(),
        rawSkill = """
A. 你具有{maxCount}次将Bad转化为Good的机会
        """.trimIndent(),
        cost = 10,
        metadata = mapOf(
            "maxCount" to CharacterMetadata.MInt(6)
        )
    ),
    SaTuoGe(
        id = 13,
        title = "洒拓歌",
        description = """
推杯换盏 新酿旧醅自味冷暖辛辣
偏壶斗平 独酌见底余酒不足分他
扶头仍酣 醉里戏称人世不过来耍
酒后莫问生杀
        """.trimIndent(),
        rawSkill = """
A. 你的多音符点击顺序自由
        """.trimIndent(),
        cost = 10
    ),
    WanYouYinLi(
        id = 14,
        title = "万有引力",
        description = """
我在人群中游弋
直到能和你平行
你不是启程原因
而是我最终的目的地
        """.trimIndent(),
        rawSkill = """
A. 你的包含音级4的音符以Perfect结算
        """.trimIndent(),
        cost = 10
    ),
    QiXi(
        id = 15,
        title = "七夕",
        description = """
七夕的夜 银烛秋光
喜鹊唱在柳梢新月明亮
青石小巷 灯火摇晃
牵你的手演织女和牛郎
        """.trimIndent(),
        rawSkill = """
A. 你的包含音级7的音符以Perfect结算
        """.trimIndent(),
        cost = 10
    ),
    LiuGuangJi(
        id = 16,
        title = "流光记",
        description = """
月亮舟楫 驮来天星
屋檐下晚风 正搭讪蝉鸣
可怜牛郎织女 还没到重逢假期
是不是喜鹊偷懒 不肯太殷勤
        """.trimIndent(),
        rawSkill = """
A. 你的连击奖励要求降低{range}
        """.trimIndent(),
        cost = 10,
        metadata = mapOf(
            "range" to CharacterMetadata.MPercent(0.1f)
        )
    );

    val skill: String get() {
        var raw = rawSkill
        for ((key, value) in metadata) raw = raw.replace("{${key}}", value.description)
        return raw
    }

    companion object {
        val Default = ChuXing
        val Pool = CharacterInfo.entries.asSequence().filter { it.enabled }.sortedBy(CharacterInfo::id).associateBy(CharacterInfo::id)
    }
}