package love.yinlin.data.rachel.game

import androidx.compose.runtime.Stable
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import love.yinlin.cs.ServerRes

@Stable
@Serializable(Game.Serializer::class)
enum class Game(
    val title: String,
    val description: String,
    val type: GameType
) {
    AnswerQuestion(
        title = "答题",
        description = """
参与其他小银子发布的答题试卷，在指定准确率下完成10道与银临有关的知识问答。
题型覆盖选择题、多选题、填空题等，内容丰富。
        """.trimIndent(),
        type = GameType.RANK
    ),
    BlockText(
        title = "网格填词",
        description = """
在指定方形网格内根据已经提示显现的部分单字来推测，使得对应行列都能构成满足题意条件的诗词或歌词。
点击可选区域可以使用单字填充与快捷批量填充。
        """.trimIndent(),
        type = GameType.RANK
    ),
    FlowersOrder(
        title = "寻花令",
        description = """
根据题意提示，猜测预设的一句文案（不包含数字、字母和标点符号）。
在有限次数内根据历史猜测记录中两两对比结果来修正你的答案直至完全猜对。
消耗可用次数后可以中途离开，在所有次数用尽前进入可以恢复之前对局。
其中绿色表示位置与文字皆正确，黄色表示文字正确但位置错误，红色表示不存在该文字。
        """.trimIndent(),
        type = GameType.EXPLORATION
    ),
    SearchAll(
        title = "词寻",
        description = """
根据题意提示，将所有满足条件的词组内容尽可能全部列出，最终成绩与用时长短相关。
输入词组时可以快速通过回车来键入，输入完成后就可以提交。
        """.trimIndent(),
        type = GameType.SPEED
    ),
    GuessLyrics(
        title = "歌词对战",
        description = """
1v1在线对战默写歌词，进入匹配大厅后可以选择同时在线的其他小银子。
当对方接受邀请后在准备时间结束后就可以开始10道默写题，根据歌词的半句来默写另一上下句。
当全部默写完成或提前提交后等待对方完成，双方都结束后结算，正确率最高者（相同则最先完成者）获胜。
正确率超过60%的战绩才会被记录到游戏记录中。
        """.trimIndent(),
        type = GameType.BATTLE
    ),
    Pictionary(
        title = "你画我猜",
        description = """
根据对方给出的参考绘图来猜测可能的预设内容。
        """.trimIndent(),
        type = GameType.RANK
    ),
    Rhyme(
        title = "琴韵",
        description = """
银临全曲库音游，体验指尖上的国风。
自由单机享受，多难度共享排行榜。
        """.trimIndent(),
        type = GameType.SINGLE
    );

    val logo by lazy { ServerRes.Game.res(this.ordinal) }

    object Serializer : KSerializer<Game> {
        override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("json.convert.Game", PrimitiveKind.INT)

        override fun serialize(encoder: Encoder, value: Game) = encoder.encodeInt(value.ordinal)

        override fun deserialize(decoder: Decoder): Game = when (val game = decoder.decodeInt()) {
            AnswerQuestion.ordinal -> AnswerQuestion
            BlockText.ordinal -> BlockText
            FlowersOrder.ordinal -> FlowersOrder
            SearchAll.ordinal -> SearchAll
            GuessLyrics.ordinal -> GuessLyrics
            Pictionary.ordinal -> Pictionary
            Rhyme.ordinal -> Rhyme
            else -> error("Unexpected Game: $game")
        }
    }
}