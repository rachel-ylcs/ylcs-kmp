package love.yinlin.data.rachel.game.info

import androidx.compose.runtime.Stable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import love.yinlin.data.rachel.game.RankConfig

@Stable
@Suppress("MayBeConstant")
data object AQConfig : RankConfig() {
    val minThreshold: Float = 0.75f // 最小成功准确率
    val maxThreshold: Float = 1f // 最大成功准确率
    val minQuestionCount: Int = 3 // 最小题数
    val maxQuestionCount: Int = 30 // 最大题数
    val minOptionCount: Int = 2 // 最小选项数
    val maxOptionCount: Int = 8 // 最大选项数
    val minAnswerCount: Int = 1 // 最小答案数
    val maxAnswerCount: Int = 10 // 最大答案数
}

@Stable
@Serializable
data class AQInfo(
    val threshold: Float, // [准确率]
)

@Stable
@Serializable
sealed class AQQuestion {
    abstract val title: String

    abstract val name: String

    // 单选
    @Stable
    @Serializable
    @SerialName("Choice")
    data class Choice(override val title: String, val options: List<String> = emptyList()) : AQQuestion() {
        override val name: String = "单选"
    }

    // 多选
    @Stable
    @Serializable
    @SerialName("MultiChoice")
    data class MultiChoice(override val title: String, val options: List<String> = emptyList()) : AQQuestion() {
        override val name: String = "多选"
    }

    // 填空
    @Stable
    @Serializable
    @SerialName("Blank")
    data class Blank(override val title: String) : AQQuestion() {
        override val name: String = "填空"
    }
}

@Stable
@Serializable
sealed class AQAnswer {
    abstract fun matchQuestion(config: AQConfig, question: AQQuestion)

    // 单选
    @Stable
    @Serializable
    @SerialName("Choice")
    data class Choice(val value: Int = -1) : AQAnswer() {
        override fun matchQuestion(config: AQConfig, question: AQQuestion) {
            require(question is AQQuestion.Choice)
            require(question.title.isNotBlank())
            require(question.options.size in config.minOptionCount .. config.maxOptionCount)
            require(value in 0 ..< question.options.size)
        }
    }

    // 多选
    @Stable
    @Serializable
    @SerialName("MultiChoice")
    data class MultiChoice(val value: List<Int> = emptyList()) : AQAnswer() {
        override fun matchQuestion(config: AQConfig, question: AQQuestion) {
            require(question is AQQuestion.MultiChoice)
            require(question.title.isNotBlank())
            require(question.options.size in config.minOptionCount .. config.maxOptionCount)
            require(value.size > 1)
            require(value.all { it in 0 ..< question.options.size })
            require(value.size == value.toSet().size)
        }
    }

    // 填空
    @Stable
    @Serializable
    @SerialName("Blank")
    data class Blank(val value: List<String> = emptyList()) : AQAnswer() {
        override fun matchQuestion(config: AQConfig, question: AQQuestion) {
            require(question is AQQuestion.Blank)
            require(value.size in config.minAnswerCount .. config.maxAnswerCount)
            require(value.size == value.toSet().size)
            require(value.all { it.isNotBlank() })
        }
    }
}

@Stable
@Serializable
sealed class AQUserAnswer {
    abstract fun verifyAnswer(answer: AQAnswer): Boolean

    // 单选
    @Stable
    @Serializable
    @SerialName("Choice")
    data class Choice(val value: Int = -1) : AQUserAnswer() {
        override fun verifyAnswer(answer: AQAnswer): Boolean = answer is AQAnswer.Choice && value == answer.value
    }

    // 多选
    @Stable
    @Serializable
    @SerialName("MultiChoice")
    data class MultiChoice(val value: List<Int> = emptyList()) : AQUserAnswer() {
        override fun verifyAnswer(answer: AQAnswer): Boolean = answer is AQAnswer.MultiChoice &&
                value.size == answer.value.size &&
                answer.value.groupingBy { it }.eachCount() == value.groupingBy { it }.eachCount()
    }

    // 填空
    @Stable
    @Serializable
    @SerialName("Blank")
    data class Blank(val value: String = "") : AQUserAnswer() {
        override fun verifyAnswer(answer: AQAnswer): Boolean = answer is AQAnswer.Blank && value in answer.value
    }
}

@Stable
@Serializable
data class AQResult(
    val correctCount: Int,
    val totalCount: Int
)