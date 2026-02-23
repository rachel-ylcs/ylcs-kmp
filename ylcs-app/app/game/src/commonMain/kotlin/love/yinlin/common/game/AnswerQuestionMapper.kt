package love.yinlin.common.game

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.util.fastForEachIndexed
import androidx.compose.ui.util.fastMapIndexed
import kotlinx.serialization.json.JsonElement
import love.yinlin.common.CreateGameState
import love.yinlin.common.GameAnswerInfo
import love.yinlin.common.GameItemExtraInfo
import love.yinlin.common.GameMapper
import love.yinlin.common.GameRecordInfo
import love.yinlin.common.PlayGameState
import love.yinlin.compose.LocalColor
import love.yinlin.compose.Theme
import love.yinlin.compose.extension.mutableRefStateOf
import love.yinlin.compose.extension.rememberValueState
import love.yinlin.compose.screen.BasicScreen
import love.yinlin.compose.ui.collection.TagView
import love.yinlin.compose.ui.common.GameSlider
import love.yinlin.compose.ui.common.SliderArgs
import love.yinlin.compose.ui.common.TopPager
import love.yinlin.compose.ui.common.value
import love.yinlin.compose.ui.container.ActionScope
import love.yinlin.compose.ui.container.ThemeContainer
import love.yinlin.compose.ui.floating.DialogInput
import love.yinlin.compose.ui.icon.Icons
import love.yinlin.compose.ui.image.Icon
import love.yinlin.compose.ui.layout.Space
import love.yinlin.compose.ui.text.Input
import love.yinlin.compose.ui.text.InputState
import love.yinlin.compose.ui.text.SimpleEllipsisText
import love.yinlin.compose.ui.text.Text
import love.yinlin.compose.ui.text.TextIconAdapter
import love.yinlin.data.rachel.game.GameDetailsWithName
import love.yinlin.data.rachel.game.GamePublicDetailsWithName
import love.yinlin.data.rachel.game.GameResult
import love.yinlin.data.rachel.game.PreflightResult
import love.yinlin.data.rachel.game.info.AQAnswer
import love.yinlin.data.rachel.game.info.AQConfig
import love.yinlin.data.rachel.game.info.AQInfo
import love.yinlin.data.rachel.game.info.AQQuestion
import love.yinlin.data.rachel.game.info.AQResult
import love.yinlin.data.rachel.game.info.AQUserAnswer
import love.yinlin.extension.catchingNull
import love.yinlin.extension.to
import love.yinlin.extension.toJson

@Stable
object AnswerQuestionMapper : GameMapper(), GameItemExtraInfo, GameAnswerInfo, GameRecordInfo {
    @Composable
    override fun ColumnScope.GameItemExtraInfoContent(gameDetails: GamePublicDetailsWithName) {
        val info = remember(gameDetails) { catchingNull { gameDetails.info.to<AQInfo>() } }
        if (info != null) {
            TextIconAdapter { idIcon, idText ->
                Icon(icon = Icons.Flaky, modifier = Modifier.idIcon())
                SimpleEllipsisText(text = "准确率: ${(info.threshold * 100).toInt()}%", style = Theme.typography.v8, modifier = Modifier.idText())
            }
        }
    }

    @Composable
    override fun ColumnScope.GameAnswerInfoContent(gameDetails: GameDetailsWithName) {
        val data = remember(gameDetails) {
            catchingNull {
                val questions = gameDetails.question.to<List<AQQuestion>>()
                val answers = gameDetails.answer.to<List<AQAnswer>>()
                require(questions.size == answers.size && questions.size in AQConfig.minQuestionCount .. AQConfig.maxQuestionCount)
                questions to answers
            }
        }

        data?.let { (questions, answers) ->
            var currentIndex by rememberValueState(0)
            val question = questions[currentIndex]
            val answer = answers[currentIndex]

            TopPager(
                currentIndex = currentIndex,
                name = question.name,
                onIncrease = { if (currentIndex < questions.size - 1) ++currentIndex },
                onDecrease = { if (currentIndex > 0) --currentIndex },
                modifier = Modifier.fillMaxWidth()
            )

            Column(
                modifier = Modifier.fillMaxWidth().weight(1f).padding(Theme.padding.eValue),
                verticalArrangement = Arrangement.spacedBy(Theme.padding.v9)
            ) {
                when (question) {
                    is AQQuestion.Choice -> {
                        answer as AQAnswer.Choice
                        Text(text = question.title)
                        question.options.fastForEachIndexed { index, option ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(Theme.padding.g5),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val isSelected = index == answer.value

                                ThemeContainer(if (isSelected) Theme.color.primary else LocalColor.current) {
                                    Icon(icon = if (isSelected) Icons.RadioButtonChecked else Icons.RadioButtonUnchecked)
                                    Text(text = option, modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                    is AQQuestion.MultiChoice -> {
                        answer as AQAnswer.MultiChoice
                        Text(text = question.title)
                        question.options.fastForEachIndexed { index, option ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(Theme.padding.g5),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val isSelected = index in answer.value

                                ThemeContainer(if (isSelected) Theme.color.primary else LocalColor.current) {
                                    Icon(icon = if (isSelected) Icons.CheckBox else Icons.CheckBoxOutlineBlank)
                                    Text(text = option, modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                    is AQQuestion.Blank -> {
                        answer as AQAnswer.Blank
                        Text(text = question.title)
                        for (option in answer.value) Text(text = option, modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        }
    }

    @Composable
    override fun ColumnScope.GameRecordInfoContent(data: GameRecordInfo.Data) {
        val pairData = remember(data) {
            catchingNull {
                val (answer, info) = data
                answer.to<List<AQUserAnswer>>().fastMapIndexed { index, item ->
                    val text = when (item) {
                        is AQUserAnswer.Choice -> if (item.value == -1) "未填" else ('A' + item.value).toString()
                        is AQUserAnswer.MultiChoice -> if (item.value.isEmpty()) "未填" else item.value.joinToString("") { ('A' + it).toString() }
                        is AQUserAnswer.Blank -> item.value
                    }
                    "${index + 1}: $text"
                } to info.to<AQResult>()
            }
        }

        pairData?.let { (totalAnswer, actualResult) ->
            TextIconAdapter { idIcon, idText ->
                Icon(icon = Icons.Flaky, modifier = Modifier.idIcon())
                SimpleEllipsisText(text = "正确率: ${actualResult.correctCount} / ${actualResult.totalCount}", modifier = Modifier.idText())
            }
            TagView(
                size = totalAnswer.size,
                titleProvider = { totalAnswer[it] },
                readonly = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    @Stable
    class AQCreateGameState(val parent: BasicScreen) : CreateGameState {
        @Stable
        private enum class QuestionType { Choice, MultiChoice, Blank; }

        @Stable
        private sealed interface QuestionItem {
            val name: String
            val question: AQQuestion
            val answer: AQAnswer

            @Stable
            data class Choice(override val question: AQQuestion.Choice, override val answer: AQAnswer.Choice) : QuestionItem {
                override val name: String = question.name
            }
            @Stable
            data class MultiChoice(override val question: AQQuestion.MultiChoice, override val answer: AQAnswer.MultiChoice) : QuestionItem {
                override val name: String = question.name
            }
            @Stable
            data class Blank(override val question: AQQuestion.Blank, override val answer: AQAnswer.Blank) : QuestionItem {
                override val name: String = question.name
            }
        }

        private var threshold by mutableRefStateOf(SliderArgs(AQConfig.minThreshold, AQConfig.minThreshold, AQConfig.maxThreshold))
        private val questions = mutableStateListOf<QuestionItem>()
        private var currentIndex by mutableIntStateOf(-1)

        override val config = AQConfig

        override val canSubmit: Boolean by derivedStateOf {
            questions.size in AQConfig.minQuestionCount .. AQConfig.maxQuestionCount && questions.all { item ->
                when (item) {
                    is QuestionItem.Choice -> item.question.options.size in AQConfig.minOptionCount .. AQConfig.maxOptionCount && item.answer.value != -1
                    is QuestionItem.MultiChoice -> item.question.options.size in AQConfig.minOptionCount .. AQConfig.maxOptionCount && item.answer.value.size > 1
                    is QuestionItem.Blank -> item.answer.value.size in AQConfig.minAnswerCount .. AQConfig.maxAnswerCount
                }
            }
        }

        override val submitInfo: JsonElement get() = AQInfo(threshold = threshold.value).toJson()

        override val submitQuestion: JsonElement get() = questions.map { it.question }.toJson()

        override val submitAnswer: JsonElement get() = questions.map { it.answer }.toJson()

        private fun addQuestion(type: QuestionType) {
            if (questions.size >= AQConfig.maxQuestionCount) parent.slot.tip.warning("题目数量超出上限")
            else {
                val title = "点此输入题目..."
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
            val title = item.question.title
            titleInputDialog.open(initText = title)?.let { text ->
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
                    if (options.size >= AQConfig.maxOptionCount) parent.slot.tip.warning("选项数量超出上限")
                    else {
                        options += "点此输入选项..."
                        questions[currentIndex] = item.copy(
                            question = item.question.copy(options = options),
                            answer = AQAnswer.Choice()
                        )
                    }
                }
                is QuestionItem.MultiChoice -> {
                    val options = item.question.options.toMutableList()
                    if (options.size >= AQConfig.maxOptionCount) parent.slot.tip.warning("选项数量超出上限")
                    else {
                        options += "点此输入选项..."
                        questions[currentIndex] = item.copy(
                            question = item.question.copy(options = options),
                            answer = AQAnswer.MultiChoice()
                        )
                    }
                }
                is QuestionItem.Blank -> {
                    val answers = item.answer.value.toMutableList()
                    if (answers.size >= AQConfig.maxAnswerCount) parent.slot.tip.warning("备选答案超出上限")
                    else {
                        answers += "点此输入答案"
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
            dialog.open(initText = options[index])?.let { text ->
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
        override fun ColumnScope.ConfigContent() {
            GameSlider(
                title = "准确率",
                args = threshold,
                onValueChange = { threshold = threshold.copy(tmpValue = it) },
                modifier = Modifier.fillMaxWidth()
            )
        }

        @Composable
        override fun ColumnScope.Content() {
            Column(
                modifier = Modifier.fillMaxWidth().aspectRatio(1f)
                    .border(width = Theme.border.v7, color = Theme.color.primary)
                    .padding(Theme.padding.eValue),
                verticalArrangement = Arrangement.spacedBy(Theme.padding.v)
            ) {
                val item = questions.getOrNull(currentIndex)

                if (item == null) {
                    Box(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        SimpleEllipsisText(text = "当前未添加任何题目")
                    }
                }
                else {
                    TopPager(
                        currentIndex = currentIndex,
                        name = item.name,
                        onIncrease = {
                            if (questions.isNotEmpty() && currentIndex < questions.size - 1) ++currentIndex
                        },
                        onDecrease = {
                            if (questions.isNotEmpty() && currentIndex > 0) --currentIndex
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Column(
                        modifier = Modifier.fillMaxWidth().weight(1f)
                            .border(width = Theme.border.v7, color = Theme.color.secondary)
                            .padding(Theme.padding.eValue)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(Theme.padding.v)
                    ) {
                        val question = item.question
                        val answer = item.answer

                        Text(text = question.title, modifier = Modifier.clickable {
                            parent.launch { modifyTitle(item) }
                        })
                        Space()

                        when (question) {
                            is AQQuestion.Choice -> {
                                answer as AQAnswer.Choice
                                question.options.fastForEachIndexed { index, option ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(Theme.padding.g9),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        val isSelected = index == answer.value

                                        ThemeContainer(if (isSelected) Theme.color.primary else LocalColor.current) {
                                            Icon(icon = if (isSelected) Icons.RadioButtonChecked else Icons.RadioButtonUnchecked, onClick = { selectOption(item, index) })
                                            Text(text = option, modifier = Modifier.weight(1f).clickable {
                                                parent.launch { modifyOption(item, index) }
                                            }.padding(vertical = Theme.padding.g9))
                                        }
                                        Icon(icon = Icons.Delete, onClick = { deleteOption(item, index) })
                                    }
                                }
                            }
                            is AQQuestion.MultiChoice -> {
                                answer as AQAnswer.MultiChoice
                                question.options.fastForEachIndexed { index, option ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(Theme.padding.g9),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        val isSelected = index in answer.value

                                        ThemeContainer(if (isSelected) Theme.color.primary else LocalColor.current) {
                                            Icon(icon = if (isSelected) Icons.CheckBox else Icons.CheckBoxOutlineBlank, onClick = { selectOption(item, index) })
                                            Text(text = option, modifier = Modifier.weight(1f).clickable {
                                                parent.launch { modifyOption(item, index) }
                                            }.padding(vertical = Theme.padding.g9))
                                        }
                                        Icon(icon = Icons.Delete, onClick = { deleteOption(item, index) })
                                    }
                                }
                            }
                            is AQQuestion.Blank -> {
                                answer as AQAnswer.Blank
                                answer.value.fastForEachIndexed { index, option ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(Theme.padding.g9),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(text = option, modifier = Modifier.weight(1f).clickable {
                                            parent.launch { modifyOption(item, index) }
                                        }.padding(vertical = Theme.padding.g9))
                                        Icon(icon = Icons.Delete, onClick = { deleteOption(item, index) })
                                    }
                                }
                            }
                        }
                     }
                }

                ActionScope.SplitContainer(
                    modifier = Modifier.fillMaxWidth(),
                    left = {
                        if (item != null) {
                            Icon(icon = Icons.Add, tip = "添加选项", onClick = { addOption(item) })
                        }
                    },
                    right = {
                        Icon(icon = Icons.RadioButtonChecked, tip = "单选题", onClick = { addQuestion(QuestionType.Choice) })
                        Icon(icon = Icons.CheckBox, tip = "多选题", onClick = { addQuestion(QuestionType.MultiChoice) })
                        Icon(icon = Icons.Translate, tip = "填空题", onClick = { addQuestion(QuestionType.Blank) })
                        Icon(icon = Icons.Delete, tip = "删除", onClick = { deleteQuestion() })
                    }
                )
            }
        }

        @Composable
        override fun Floating() {
            titleInputDialog.Land()
            optionInputDialog.Land()
            answerInputDialog.Land()
        }

        private val titleInputDialog = DialogInput(hint = "输入题目", maxLength = 256, maxLines = 5, minLines = 1)
        private val optionInputDialog = DialogInput(hint = "输入选项", maxLength = 64, maxLines = 3, minLines = 1)
        private val answerInputDialog = DialogInput(hint = "输入备选答案", maxLength = 16)
    }

    override val gameCreator: (BasicScreen) -> CreateGameState = ::AQCreateGameState

    @Stable
    class AQPlayGameState(parent: BasicScreen) : PlayGameState {
        @Stable
        private data class Preflight(val info: AQInfo, val questions: List<AQQuestion>)

        override val config = AQConfig

        private var preflight: Preflight? by mutableRefStateOf(null)
        private var result: AQResult? by mutableRefStateOf(null)

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
            preflight = catchingNull {
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
            }
        }

        override fun settle(gameResult: GameResult) {
            result = catchingNull { gameResult.info.to() }
        }

        @Composable
        override fun ColumnScope.Content() {
            preflight?.let { (_, questions) ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(width = Theme.border.v7, color = Theme.color.primary)
                        .padding(Theme.padding.eValue),
                    verticalArrangement = Arrangement.spacedBy(Theme.padding.v)
                ) {
                    val question = questions.getOrNull(currentIndex)
                    val answer = answers.getOrNull(currentIndex)

                    if (question != null && answer != null) {
                        TopPager(
                            currentIndex = currentIndex,
                            name = question.name,
                            onIncrease = {
                                if (currentIndex < questions.size - 1) ++currentIndex
                            },
                            onDecrease = {
                                if (currentIndex > 0) --currentIndex
                            },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Text(text = question.title)

                        when (question) {
                            is AQQuestion.Choice -> {
                                answer as AQUserAnswer.Choice
                                question.options.fastForEachIndexed { index, option ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(Theme.padding.g9),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        val isSelected = index == answer.value

                                        ThemeContainer(if (isSelected) Theme.color.primary else LocalColor.current) {
                                            Icon(icon = if (isSelected) Icons.RadioButtonChecked else Icons.RadioButtonUnchecked, onClick = {
                                                answers[currentIndex] = answer.copy(value = if (isSelected) -1 else index)
                                                if (!isSelected && currentIndex < questions.size - 1) ++currentIndex
                                            })
                                            Text(text = option, modifier = Modifier.weight(1f).padding(vertical = Theme.padding.g9))
                                        }
                                    }
                                }
                            }
                            is AQQuestion.MultiChoice -> {
                                answer as AQUserAnswer.MultiChoice
                                question.options.fastForEachIndexed { index, option ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(Theme.padding.g9),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        val isSelected = index in answer.value

                                        ThemeContainer(if (isSelected) Theme.color.primary else LocalColor.current) {
                                            Icon(icon = if (isSelected) Icons.CheckBox else Icons.CheckBoxOutlineBlank, onClick = {
                                                val newAnswers = answer.value.toMutableList()
                                                if (index in newAnswers) newAnswers.remove(index)
                                                else newAnswers.add(index)
                                                answers[currentIndex] = answer.copy(value = newAnswers)
                                            })
                                            Text(text = option, modifier = Modifier.weight(1f).padding(vertical = Theme.padding.g9))
                                        }
                                    }
                                }
                            }
                            is AQQuestion.Blank -> {
                                answer as AQUserAnswer.Blank
                                Text(text = "当前答案: ${answer.value.ifEmpty { "未填写" }}", color = Theme.color.primary)

                                val inputState = remember(currentIndex) { InputState(maxLength = 64) }
                                val focusRequester = remember { FocusRequester() }

                                LaunchedEffect(currentIndex) {
                                    focusRequester.requestFocus()
                                }

                                Input(
                                    state = inputState,
                                    hint = "输入答案(回车保存)",
                                    onImeClick = {
                                        if (inputState.isSafe) {
                                            answers[currentIndex] = answer.copy(value = inputState.text)
                                            inputState.text = ""
                                            if (currentIndex < questions.size - 1) ++currentIndex
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth().focusRequester(focusRequester)
                                )
                            }
                        }
                    }
                    else SimpleEllipsisText(text = "题目数据为空")
                }
            }
        }

        @Composable
        override fun ColumnScope.Settlement() {
            result?.let {
                TextIconAdapter { idIcon, idText ->
                    Icon(icon = Icons.Flaky, modifier = Modifier.idIcon())
                    SimpleEllipsisText(text = "正确率: ${it.correctCount} / ${it.totalCount}", modifier = Modifier.idText())
                }
            }
        }

        @Composable
        override fun Floating() { }
    }

    override val gamePlayer: (BasicScreen) -> PlayGameState = ::AQPlayGameState
}