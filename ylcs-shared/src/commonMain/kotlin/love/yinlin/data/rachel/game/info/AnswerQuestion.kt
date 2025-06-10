package love.yinlin.data.rachel.game.info

import androidx.compose.runtime.Stable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Stable
@Serializable
data class AQInfo(
    val threshold: Float, // [成功阈值]
)

@Stable
@Serializable
sealed class AQQuestion {
    companion object {
        const val MAX_OPTION_COUNT = 10
    }

    abstract val title: String

    // 单选
    @Stable
    @Serializable
    @SerialName("Choice")
    data class Choice(override val title: String, val options: List<String>) : AQQuestion()

    // 多选
    @Stable
    @Serializable
    @SerialName("MultiChoice")
    data class MultiChoice(override val title: String, val options: List<String>) : AQQuestion()

    // 填空
    @Stable
    @Serializable
    @SerialName("Blank")
    data class Blank(override val title: String) : AQQuestion()
}

@Stable
@Serializable
sealed class AQAnswer {
    abstract fun matchQuestion(question: AQQuestion)

    // 单选
    @Stable
    @Serializable
    @SerialName("Choice")
    data class Choice(val value: Int) : AQAnswer() {
        override fun matchQuestion(question: AQQuestion) {
            require(question is AQQuestion.Choice)
            require(question.title.isNotBlank())
            require(question.options.size in 1 ..AQQuestion.MAX_OPTION_COUNT)
            require(value in 0 ..< question.options.size)
        }
    }

    // 多选
    @Stable
    @Serializable
    @SerialName("MultiChoice")
    data class MultiChoice(val value: List<Int>) : AQAnswer() {
        override fun matchQuestion(question: AQQuestion) {
            require(question is AQQuestion.MultiChoice)
            require(question.title.isNotBlank())
            require(question.options.size in 1 ..AQQuestion.MAX_OPTION_COUNT)
            require(value.all { it in 0 ..< question.options.size })
            require(value.size == value.toSet().size)
        }
    }

    // 填空
    @Stable
    @Serializable
    @SerialName("Blank")
    data class Blank(val value: List<String>) : AQAnswer() {
        override fun matchQuestion(question: AQQuestion) {
            require(question is AQQuestion.Blank)
            require(value.size in 1 ..AQQuestion.MAX_OPTION_COUNT)
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
    data class Choice(val value: Int) : AQUserAnswer() {
        override fun verifyAnswer(answer: AQAnswer): Boolean = answer is AQAnswer.Choice && value == answer.value
    }

    // 多选
    @Stable
    @Serializable
    @SerialName("MultiChoice")
    data class MultiChoice(val value: List<Int>) : AQUserAnswer() {
        override fun verifyAnswer(answer: AQAnswer): Boolean = answer is AQAnswer.MultiChoice &&
                value.size == answer.value.size &&
                answer.value.groupingBy { it }.eachCount() == value.groupingBy { it }.eachCount()
    }

    // 填空
    @Stable
    @Serializable
    @SerialName("Blank")
    data class Blank(val value: String) : AQUserAnswer() {
        override fun verifyAnswer(answer: AQAnswer): Boolean = answer is AQAnswer.Blank && value in answer.value
    }
}

@Stable
@Serializable
data class AQResult(
    val correctCount: Int,
    val totalCount: Int
)