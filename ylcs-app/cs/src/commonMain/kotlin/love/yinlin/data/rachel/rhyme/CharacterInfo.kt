package love.yinlin.data.rachel.rhyme

import androidx.compose.runtime.Stable

@Stable
enum class CharacterInfo(
    val id: Int, // ID
    val title: String, // 名称
    val description: String, // 描述
    val skill: String, // 技能
    val cost: Int, // 花费
    val enabled: Boolean = true, // 是否开启
) {
    ChuXing(
        id = 1,
        title = "初心",
        description = """
守紧我源自星空下的承诺
激动的邂逅 命运的枷锁
相遇莫错过
        """.trimIndent(),
        skill = """
A. 你没有任何琴韵，坚守初心定有回报
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
        skill = """
A. 【断尾时刻】音轨到达最后一句时触发
B. 断尾时刻所有音符自动以Perfect结算
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
        skill = """
A. 你的第4个Good转化为Perfect结算
B. 触发A后重新计数
        """.trimIndent(),
        cost = 20
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
        skill = """
A. 你具有额外30%的视野
        """.trimIndent(),
        cost = 30
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
        skill = """
A. 你具有额外20%的准备时间
        """.trimIndent(),
        cost = 40
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
        skill = """
A. 你的Bad结算有20%概率不重置连击
        """.trimIndent(),
        cost = 50
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
        skill = """
A. 【转角时刻】音轨到达一句的最后一个音符时触发
B. 转角时刻所有音符自动以Perfect结算
        """.trimIndent(),
        cost = 60
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
        skill = """
A. 你看不见局内所有未完成结算的音符
B. 你的所有Bad转化为Good结算
        """.trimIndent(),
        cost = 70
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
        skill = """
A. 你看不见局内所有已完成的音符
B. 你看不见局内UI信息
C. 你看不见局内背景
D. 你的局内动画大幅削弱表现效果
E. 你听不见局内音效
        """.trimIndent(),
        cost = 80
    );

    companion object {
        val Default = ChuXing
        val Pool = CharacterInfo.entries.asSequence().filter { it.enabled }.sortedBy(CharacterInfo::id).associateBy(CharacterInfo::id)
    }
}