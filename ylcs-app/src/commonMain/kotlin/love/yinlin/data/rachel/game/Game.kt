package love.yinlin.data.rachel.game

import androidx.compose.runtime.Stable
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import love.yinlin.resources.*
import org.jetbrains.compose.resources.DrawableResource

@Stable
@Serializable(Game.Serializer::class)
enum class Game(
    val title: String,
    val imgX: DrawableResource,
    val imgY: DrawableResource,
    val description: String,
    val type: GameType
) {
    AnswerQuestion(
        title = "答题",
        imgX = Res.drawable.game1x,
        imgY = Res.drawable.game1y,
        description = "简单易懂的答题, 支持选择、多选、填空类型, 内容自定义",
        type = GameType.RANK
    ),
    BlockText(
        title = "网格填词",
        imgX = Res.drawable.game2x,
        imgY = Res.drawable.game2y,
        description = "在方形网格中填写缺失的字使得横竖都能构成满足条件的诗词或歌词",
        type = GameType.RANK
    ),
    FlowersOrder(
        title = "寻花令",
        imgX = Res.drawable.game3x,
        imgY = Res.drawable.game3y,
        description = "在有限次数内猜测七言诗词中的某一句, 并根据上次内容与位置提示结果来修正答案直至完全猜对",
        type = GameType.RANK
    ),
    SearchAll(
        title = "词寻",
        imgX = Res.drawable.game4x,
        imgY = Res.drawable.game4y,
        description = "根据词库提示尽可能用最短的时间将所有满足条件的内容列出",
        type = GameType.SPEED
    );

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