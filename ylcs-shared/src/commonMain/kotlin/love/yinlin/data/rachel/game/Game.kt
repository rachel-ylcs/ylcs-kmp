package love.yinlin.data.rachel.game

import androidx.compose.runtime.Stable
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import love.yinlin.Local
import love.yinlin.api.ServerRes

@Stable
@Serializable(Game.Serializer::class)
enum class Game(
    val title: String,
    val description: String,
    val type: GameType
) {
    AnswerQuestion(
        title = "答题",
        description = "简单易懂的答题, 支持选择、多选、填空类型, 内容自定义",
        type = GameType.RANK
    ),
    BlockText(
        title = "网格填词",
        description = "在方形网格中填写缺失的字使得横竖都能构成满足条件的诗词或歌词",
        type = GameType.RANK
    ),
    FlowersOrder(
        title = "寻花令",
        description = "在有限次数内猜测七言诗词中的某一句, 并根据上次内容与位置提示结果来修正答案直至完全猜对",
        type = GameType.EXPLORATION
    ),
    SearchAll(
        title = "词寻",
        description = "根据词库提示尽可能用最短的时间将所有满足条件的内容列出",
        type = GameType.SPEED
    );

    val xPath: String by lazy { "${Local.ClientUrl}/${ServerRes.Game.x(this.ordinal + 1)}" }
    val yPath: String by lazy { "${Local.ClientUrl}/${ServerRes.Game.y(this.ordinal + 1)}" }
    fun xyPath(isX: Boolean): String = "${Local.ClientUrl}/${ServerRes.Game.xy(this.ordinal + 1, isX)}"

    object Serializer : KSerializer<Game> {
        override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("json.convert.Game", PrimitiveKind.INT)

        override fun serialize(encoder: Encoder, value: Game) = encoder.encodeInt(value.ordinal)

        override fun deserialize(decoder: Decoder): Game = when (val game = decoder.decodeInt()) {
            AnswerQuestion.ordinal -> AnswerQuestion
            BlockText.ordinal -> BlockText
            FlowersOrder.ordinal -> FlowersOrder
            SearchAll.ordinal -> SearchAll
            else -> error("Unexpected Game: $game")
        }
    }
}