package love.yinlin.ui.screen.world.game

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.util.fastForEachIndexed
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonElement
import love.yinlin.common.ThemeValue
import love.yinlin.data.rachel.game.GamePublicDetails
import love.yinlin.data.rachel.game.GameResult
import love.yinlin.data.rachel.game.PreflightResult
import love.yinlin.data.rachel.game.info.AQAnswer
import love.yinlin.data.rachel.game.info.AQConfig
import love.yinlin.data.rachel.game.info.AQInfo
import love.yinlin.data.rachel.game.info.AQQuestion
import love.yinlin.data.rachel.game.info.AQResult
import love.yinlin.data.rachel.game.info.AQUserAnswer
import love.yinlin.extension.to
import love.yinlin.extension.toJson
import love.yinlin.ui.component.image.ClickIcon
import love.yinlin.ui.component.input.RachelText
import love.yinlin.ui.component.layout.SimpleEmptyBox
import love.yinlin.ui.component.layout.Space
import love.yinlin.ui.component.screen.FloatingDialogInput
import love.yinlin.ui.component.text.TextInput
import love.yinlin.ui.component.text.TextInputState
import love.yinlin.ui.screen.SubScreenSlot

@Stable
private enum class QuestionType {
    Choice, MultiChoice, Blank
}

@Stable
private sealed interface QuestionItem {
    val name: String

    @Stable
    data class Choice(val question: AQQuestion.Choice, val answer: AQAnswer.Choice) : QuestionItem {
        override val name: String = question.name
    }
    @Stable
    data class MultiChoice(val question: AQQuestion.MultiChoice, val answer: AQAnswer.MultiChoice) : QuestionItem {
        override val name: String = question.name
    }
    @Stable
    data class Blank(val question: AQQuestion.Blank, val answer: AQAnswer.Blank) : QuestionItem {
        override val name: String = question.name
    }
}

@Composable
fun ColumnScope.AnswerQuestionCardInfo(game: GamePublicDetails) {
    val info = remember(game) {
        try { game.info.to<AQInfo>() } catch (_: Throwable) { null }
    }
    if (info != null) {
        RachelText(
            text = remember(info) { "准确率: ${(info.threshold * 100).toInt()}%" },
            icon = Icons.Outlined.Flaky
        )
    }
}

@Stable
class AnswerQuestionCreateGameState(val slot: SubScreenSlot) : CreateGameState {
    override val config = AQConfig

    private var threshold by mutableFloatStateOf(0f)
    private val questions = mutableStateListOf<QuestionItem>()
    private var currentIndex by mutableIntStateOf(-1)

    override val canSubmit: Boolean by derivedStateOf {
        questions.size in AQConfig.minQuestionCount .. AQConfig.maxQuestionCount && questions.all { item ->
            when (item) {
                is QuestionItem.Choice -> item.question.options.size in AQConfig.minOptionCount .. AQConfig.maxOptionCount && item.answer.value != -1
                is QuestionItem.MultiChoice -> item.question.options.size in AQConfig.minOptionCount .. AQConfig.maxOptionCount && item.answer.value.size > 1
                is QuestionItem.Blank -> item.answer.value.size in AQConfig.minAnswerCount .. AQConfig.maxAnswerCount
            }
        }
    }

    override val submitInfo: JsonElement get() = AQInfo(threshold = threshold.cast(AQConfig.minThreshold, AQConfig.maxThreshold)).toJson()

    override val submitQuestion: JsonElement get() = questions.map {
        when (it) {
            is QuestionItem.Choice -> it.question
            is QuestionItem.MultiChoice -> it.question
            is QuestionItem.Blank -> it.question
        }
    }.toJson()

    override val submitAnswer: JsonElement get() = questions.map {
        when (it) {
            is QuestionItem.Choice -> it.answer
            is QuestionItem.MultiChoice -> it.answer
            is QuestionItem.Blank -> it.answer
        }
    }.toJson()

    private fun addQuestion(type: QuestionType) {
        if (questions.size >= AQConfig.maxQuestionCount) slot.tip.warning("题目数量超出上限")
        else {
            val title = "请输入题目..."
            questions.add(++currentIndex, when (type) {
                QuestionType.Choice -> QuestionItem.Choice(AQQuestion.Choice(title), AQAnswer.Choice())
                QuestionType.MultiChoice -> QuestionItem.MultiChoice(AQQuestion.MultiChoice(title), AQAnswer.MultiChoice())
                QuestionType.Blank -> QuestionItem.Blank(AQQuestion.Blank(title), AQAnswer.Blank())
            })
        }
    }

    private fun deleteQuestion() {
        if (currentIndex != -1) {
            questions.removeAt(currentIndex)
            if (currentIndex == questions.size) --currentIndex
        }
    }

    private suspend fun modifyTitle(item: QuestionItem) {
        val title = when (item) {
            is QuestionItem.Choice -> item.question.title
            is QuestionItem.MultiChoice -> item.question.title
            is QuestionItem.Blank -> item.question.title
        }
        titleInputDialog.openSuspend(initText = title)?.let { text ->
            questions[currentIndex] = when (item) {
                is QuestionItem.Choice -> item.copy(question = item.question.copy(title = text))
                is QuestionItem.MultiChoice -> item.copy(question = item.question.copy(title = text))
                is QuestionItem.Blank -> item.copy(question = item.question.copy(title = text))
            }
        }
    }

    private fun addOption(item: QuestionItem) {
        when (item) {
            is QuestionItem.Choice -> {
                val options = item.question.options.toMutableList()
                if (options.size >= AQConfig.maxOptionCount) slot.tip.warning("选项数量超出上限")
                else {
                    options += "请输入选项..."
                    questions[currentIndex] = item.copy(
                        question = item.question.copy(options = options),
                        answer = AQAnswer.Choice()
                    )
                }
            }
            is QuestionItem.MultiChoice -> {
                val options = item.question.options.toMutableList()
                if (options.size >= AQConfig.maxOptionCount) slot.tip.warning("选项数量超出上限")
                else {
                    options += "请输入选项..."
                    questions[currentIndex] = item.copy(
                        question = item.question.copy(options = options),
                        answer = AQAnswer.MultiChoice()
                    )
                }
            }
            is QuestionItem.Blank -> {
                val answers = item.answer.value.toMutableList()
                if (answers.size >= AQConfig.maxAnswerCount) slot.tip.warning("备选答案超出上限")
                else {
                    answers += "请输入答案"
                    questions[currentIndex] = item.copy(answer = AQAnswer.Blank(answers))
                }
            }
        }
    }

    private fun deleteOption(item: QuestionItem, index: Int) {
        when (item) {
            is QuestionItem.Choice -> {
                val options = item.question.options.toMutableList()
                options.removeAt(index)
                questions[currentIndex] = item.copy(
                    question = item.question.copy(options = options),
                    answer = AQAnswer.Choice()
                )
            }
            is QuestionItem.MultiChoice -> {
                val options = item.question.options.toMutableList()
                options.removeAt(index)
                questions[currentIndex] = item.copy(
                    question = item.question.copy(options = options),
                    answer = AQAnswer.MultiChoice()
                )
            }
            is QuestionItem.Blank -> {
                val answers = item.answer.value.toMutableList()
                answers.removeAt(index)
                questions[currentIndex] = item.copy(answer = AQAnswer.Blank(answers))
            }
        }
    }

    private suspend fun modifyOption(item: QuestionItem, index: Int) {
        val options = when (item) {
            is QuestionItem.Choice -> item.question.options
            is QuestionItem.MultiChoice -> item.question.options
            is QuestionItem.Blank -> item.answer.value
        }.toMutableList()
        val dialog = if (item is QuestionItem.Blank) answerInputDialog else optionInputDialog
        dialog.openSuspend(initText = options[index])?.let { text ->
            options[index] = text
            questions[currentIndex] = when (item) {
                is QuestionItem.Choice -> item.copy(question = item.question.copy(options = options))
                is QuestionItem.MultiChoice -> item.copy(question = item.question.copy(options = options))
                is QuestionItem.Blank -> item.copy(answer = AQAnswer.Blank(options))
            }
        }
    }

    private fun selectOption(item: QuestionItem, index: Int) {
        questions[currentIndex] = when (item) {
            is QuestionItem.Choice -> item.copy(answer = AQAnswer.Choice(if (item.answer.value == index) -1 else index))
            is QuestionItem.MultiChoice -> {
                val answers = item.answer.value.toMutableList()
                if (index in answers) answers.remove(index)
                else answers.add(index)
                item.copy(answer = AQAnswer.MultiChoice(answers))
            }
            is QuestionItem.Blank -> error("")
        }
    }

    @Composable
    override fun ColumnScope.Content() {
        val scope = rememberCoroutineScope()

        GameSlider(
            title = "准确率",
            progress = threshold,
            minValue = AQConfig.minThreshold,
            maxValue = AQConfig.maxThreshold,
            onProgressChange = { threshold = it },
            modifier = Modifier.fillMaxWidth()
        )
        Column(
            modifier = Modifier.fillMaxWidth()
                .height(ThemeValue.Size.CardWidth)
                .border(width = ThemeValue.Border.Small, color = MaterialTheme.colorScheme.primary)
                .padding(ThemeValue.Padding.EqualValue),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(ThemeValue.Padding.VerticalSpace)
        ) {
            val item = questions.getOrNull(currentIndex)

            if (item != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(ThemeValue.Padding.HorizontalSpace),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ClickIcon(
                        icon = Icons.AutoMirrored.Outlined.KeyboardArrowLeft,
                        onClick = {
                            if (questions.isNotEmpty() && currentIndex > 0) --currentIndex
                        }
                    )
                    Text(
                        text = remember(currentIndex) { "${currentIndex + 1} ${item.name}" },
                        style = MaterialTheme.typography.titleLarge,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    ClickIcon(
                        icon = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                        onClick = {
                            if (questions.isNotEmpty() && currentIndex < questions.size - 1) ++currentIndex
                        }
                    )
                }
                Column(
                    modifier = Modifier.fillMaxWidth().weight(1f)
                        .border(width = ThemeValue.Border.Small, color = MaterialTheme.colorScheme.secondary)
                        .padding(ThemeValue.Padding.EqualValue)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(ThemeValue.Padding.VerticalSpace)
                ) {
                    when (item) {
                        is QuestionItem.Choice -> {
                            val (question, answer) = item
                            Text(
                                text = question.title,
                                modifier = Modifier.clickable {
                                    scope.launch { modifyTitle(item) }
                                }
                            )
                            Space()
                            question.options.fastForEachIndexed { index, option ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val isSelected = index == answer.value

                                    ClickIcon(
                                        icon = Icons.Outlined.Delete,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                        onClick = { deleteOption(item, index) }
                                    )
                                    ClickIcon(
                                        icon = if (isSelected) Icons.Outlined.RadioButtonChecked else Icons.Outlined.RadioButtonUnchecked,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                        onClick = { selectOption(item, index) }
                                    )
                                    Text(
                                        text = option,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.weight(1f).clickable {
                                            scope.launch { modifyOption(item, index) }
                                        }
                                    )
                                }
                            }
                        }
                        is QuestionItem.MultiChoice -> {
                            val (question, answer) = item
                            Text(
                                text = question.title,
                                modifier = Modifier.clickable {
                                    scope.launch { modifyTitle(item) }
                                }
                            )
                            Space()
                            question.options.fastForEachIndexed { index, option ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val isSelected = index in answer.value

                                    ClickIcon(
                                        icon = Icons.Outlined.Delete,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                        onClick = { deleteOption(item, index) }
                                    )
                                    ClickIcon(
                                        icon = if (isSelected) Icons.Outlined.CheckBox else Icons.Outlined.CheckBoxOutlineBlank,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                        onClick = { selectOption(item, index) }
                                    )
                                    Text(
                                        text = option,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.weight(1f).clickable {
                                            scope.launch { modifyOption(item, index) }
                                        }
                                    )
                                }
                            }
                        }
                        is QuestionItem.Blank -> {
                            val (question, answer) = item
                            Text(
                                text = question.title,
                                modifier = Modifier.clickable {
                                    scope.launch { modifyTitle(item) }
                                }
                            )
                            Space()
                            answer.value.fastForEachIndexed { index, option ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    ClickIcon(
                                        icon = Icons.Outlined.Delete,
                                        onClick = { deleteOption(item, index) }
                                    )
                                    Text(
                                        text = option,
                                        modifier = Modifier.weight(1f).clickable {
                                            scope.launch { modifyOption(item, index) }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
            else {
                Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                    SimpleEmptyBox()
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(ThemeValue.Padding.HorizontalSpace, Alignment.End),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (item != null) {
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        ClickIcon(Icons.Outlined.Add) {
                            addOption(item)
                        }
                    }
                }
                ClickIcon(Icons.Outlined.RadioButtonChecked) { addQuestion(QuestionType.Choice) }
                ClickIcon(Icons.Outlined.CheckBox) { addQuestion(QuestionType.MultiChoice) }
                ClickIcon(Icons.Outlined.Translate) { addQuestion(QuestionType.Blank) }
                ClickIcon(Icons.Outlined.Delete) { deleteQuestion() }
            }
        }
    }

    private val titleInputDialog = FloatingDialogInput(
        hint = "输入题目",
        maxLength = 256,
        maxLines = 5,
        minLines = 1,
        clearButton = false
    )

    private val optionInputDialog = FloatingDialogInput(
        hint = "输入选项",
        maxLength = 64,
        maxLines = 3,
        minLines = 1,
        clearButton = false
    )

    private val answerInputDialog = FloatingDialogInput(
        hint = "输入备选答案",
        maxLength = 16
    )

    @Composable
    override fun Floating() {
        titleInputDialog.Land()
        optionInputDialog.Land()
        answerInputDialog.Land()
    }
}

@Stable
class AnswerQuestionPlayGameState(val slot: SubScreenSlot) : PlayGameState {
    @Stable
    private data class Preflight(val info: AQInfo, val questions: List<AQQuestion>)

    override val config = AQConfig

    private var preflight: Preflight? by mutableStateOf(null)
    private var result: AQResult? by mutableStateOf(null)

    private val answers = mutableStateListOf<AQUserAnswer>()
    private var currentIndex by mutableIntStateOf(0)

    override val canSubmit: Boolean by derivedStateOf { answers.all { answer ->
        when (answer) {
            is AQUserAnswer.Choice -> answer.value != -1
            is AQUserAnswer.MultiChoice -> answer.value.size > 1
            is AQUserAnswer.Blank -> answer.value.isNotEmpty()
        }
    } }

    override val submitAnswer: JsonElement get() = answers.toList().toJson()

    override fun init(preflightResult: PreflightResult) {
        preflight = try {
            val questions = preflightResult.question.to<List<AQQuestion>>()
            require(questions.size in AQConfig.minQuestionCount .. AQConfig.maxQuestionCount)
            answers.clear()
            for (question in questions) {
                answers += when (question) {
                    is AQQuestion.Choice -> AQUserAnswer.Choice()
                    is AQQuestion.MultiChoice -> AQUserAnswer.MultiChoice()
                    is AQQuestion.Blank -> AQUserAnswer.Blank()
                }
            }
            currentIndex = 0
            Preflight(
                info = preflightResult.info.to(),
                questions = questions
            )
        } catch (_: Throwable) { null }
    }

    override fun settle(gameResult: GameResult) {
        result = try {
            gameResult.info.to()
        } catch (_: Throwable) { null }
    }

    @Composable
    override fun ColumnScope.Content() {
        preflight?.let { (_, questions) ->
            Column(
                modifier = Modifier.fillMaxWidth()
                    .height(ThemeValue.Size.CardWidth)
                    .border(width = ThemeValue.Border.Small, color = MaterialTheme.colorScheme.primary)
                    .padding(ThemeValue.Padding.EqualValue),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(ThemeValue.Padding.VerticalSpace)
            ) {
                val question = questions.getOrNull(currentIndex)
                val answer = answers.getOrNull(currentIndex)

                if (question != null && answer != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(ThemeValue.Padding.HorizontalSpace),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ClickIcon(
                            icon = Icons.AutoMirrored.Outlined.KeyboardArrowLeft,
                            onClick = {
                                if (currentIndex > 0) --currentIndex
                            }
                        )
                        Text(
                            text = remember(currentIndex) { "${currentIndex + 1} ${question.name}" },
                            style = MaterialTheme.typography.titleLarge,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        ClickIcon(
                            icon = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                            onClick = {
                                if (currentIndex < questions.size - 1) ++currentIndex
                            }
                        )
                    }
                    Column(
                        modifier = Modifier.fillMaxWidth().weight(1f)
                            .padding(ThemeValue.Padding.EqualValue)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(ThemeValue.Padding.VerticalSpace)
                    ) {
                        when (question) {
                            is AQQuestion.Choice -> {
                                answer as AQUserAnswer.Choice
                                Text(text = question.title)
                                Space()
                                question.options.fastForEachIndexed { index, option ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        val isSelected = index == answer.value

                                        ClickIcon(
                                            icon = if (isSelected) Icons.Outlined.RadioButtonChecked else Icons.Outlined.RadioButtonUnchecked,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                            onClick = {
                                                answers[currentIndex] = answer.copy(value = if (answer.value == index) -1 else index)
                                            }
                                        )
                                        Text(
                                            text = option,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                            is AQQuestion.MultiChoice -> {
                                answer as AQUserAnswer.MultiChoice
                                Text(text = question.title)
                                Space()
                                question.options.fastForEachIndexed { index, option ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        val isSelected = index in answer.value

                                        ClickIcon(
                                            icon = if (isSelected) Icons.Outlined.CheckBox else Icons.Outlined.CheckBoxOutlineBlank,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                            onClick = {
                                                val newAnswers = answer.value.toMutableList()
                                                if (index in newAnswers) newAnswers.remove(index)
                                                else newAnswers.add(index)
                                                answers[currentIndex] = answer.copy(value = newAnswers)
                                            }
                                        )
                                        Text(
                                            text = option,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                            is AQQuestion.Blank -> {
                                answer as AQUserAnswer.Blank
                                Text(text = question.title)
                                Space()
                                Text(
                                    text = answer.value,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Space()
                                val inputState = remember(currentIndex) { TextInputState() }
                                TextInput(
                                    state = inputState,
                                    hint = "输入答案(回车保存)",
                                    clearButton = false,
                                    onImeClick = {
                                        answers[currentIndex] = answer.copy(value = inputState.text)
                                        inputState.text = ""
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
                else {
                    Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                        SimpleEmptyBox()
                    }
                }
            }
        }
    }

    @Composable
    override fun ColumnScope.Settlement() {
        result?.let { (correctCount, totalCount) ->
            Text(
                text = "结算: $correctCount / $totalCount",
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}