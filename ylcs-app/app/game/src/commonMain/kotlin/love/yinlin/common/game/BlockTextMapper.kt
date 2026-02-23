package love.yinlin.common.game

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.zIndex
import kotlinx.serialization.json.JsonElement
import love.yinlin.common.CreateGameState
import love.yinlin.common.GameAnswerInfo
import love.yinlin.common.GameMapper
import love.yinlin.common.GameRecordInfo
import love.yinlin.common.PlayGameState
import love.yinlin.compose.Colors
import love.yinlin.compose.LocalColor
import love.yinlin.compose.Theme
import love.yinlin.compose.bold
import love.yinlin.compose.extension.mutableRefStateOf
import love.yinlin.compose.extension.rememberState
import love.yinlin.compose.extension.rememberValueState
import love.yinlin.compose.screen.BasicScreen
import love.yinlin.compose.ui.common.GameSlider
import love.yinlin.compose.ui.common.SliderArgs
import love.yinlin.compose.ui.common.value
import love.yinlin.compose.ui.container.ActionScope
import love.yinlin.compose.ui.container.Surface
import love.yinlin.compose.ui.container.ThemeContainer
import love.yinlin.compose.ui.floating.DialogInput
import love.yinlin.compose.ui.icon.Icons
import love.yinlin.compose.ui.image.Icon
import love.yinlin.compose.ui.image.LoadingIcon
import love.yinlin.compose.ui.layout.Space
import love.yinlin.compose.ui.text.SimpleClipText
import love.yinlin.compose.ui.text.SimpleEllipsisText
import love.yinlin.compose.ui.text.TextIconAdapter
import love.yinlin.compose.ui.text.measureTextHeight
import love.yinlin.data.rachel.game.GameDetailsWithName
import love.yinlin.data.rachel.game.GameResult
import love.yinlin.data.rachel.game.PreflightResult
import love.yinlin.data.rachel.game.info.BTConfig
import love.yinlin.data.rachel.game.info.BTResult
import love.yinlin.extension.String
import love.yinlin.extension.catchingNull
import love.yinlin.extension.json
import love.yinlin.extension.to
import love.yinlin.extension.toJson
import kotlin.jvm.JvmInline
import kotlin.math.min
import kotlin.math.sqrt

@Stable
object BlockTextMapper : GameMapper(), GameAnswerInfo, GameRecordInfo {
    @Stable
    @JvmInline
    private value class BlockCharacter(val value: Int) {
        constructor(ch: Char, hide: Boolean) : this(ch.code or (if (hide) 1 shl 16 else 0))

        val ch: Char get() = (value and 0xFFFF).toChar()

        inline fun <R> decode(block: (Char, Boolean) -> R) = block((value and 0xFFFF).toChar(), (value and 0x10000) != 0)

        companion object {
            val Empty = BlockCharacter(BTConfig.CHAR_EMPTY, false)
            val Blank = BlockCharacter(BTConfig.CHAR_BLANK, true)
        }
    }

    @Stable
    private enum class CharacterBlockInputMode(val icon: ImageVector) {
        DISABLED(Icons.MobiledataOff), HORIZONTAL(Icons.SwapHoriz), VERTICAL(Icons.SwapVert);

        val next: CharacterBlockInputMode get() = when (this) {
            DISABLED -> HORIZONTAL
            HORIZONTAL -> VERTICAL
            VERTICAL -> DISABLED
        }
    }

    @Composable
    private fun CharacterBlock(
        blockSize: Int,
        data: List<BlockCharacter>,
        enabled: Boolean = true,
        writeMode: Boolean,
        onCharacterSelected: suspend (Char?) -> Char? = { null },
        onStringSelected: suspend () -> String? = { null },
        onCharacterChanged: (Int, BlockCharacter) -> Unit = { _, _ -> },
        modifier: Modifier = Modifier
    ) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            var openIndex by rememberValueState(-1)

            FlowRow(
                modifier = Modifier.zIndex(1f),
                maxItemsInEachRow = blockSize,
                maxLines = blockSize
            ) {
                val boxSize = measureTextHeight(BTConfig.CHAR_BLOCK.toString()) * 1.5f

                repeat(blockSize * blockSize) { index ->
                    data[index].decode { ch, hide ->
                        Box(
                            modifier = Modifier.size(boxSize).background(when {
                                ch == BTConfig.CHAR_EMPTY || ch == BTConfig.CHAR_BLOCK -> Colors.Transparent
                                hide -> Theme.color.secondaryContainer
                                else -> Theme.color.primaryContainer
                            }).clickable(enabled = enabled) {
                                openIndex = if (writeMode) if (openIndex != -1) -1 else index
                                else if (ch != BTConfig.CHAR_EMPTY && ch != BTConfig.CHAR_BLOCK && hide) {
                                    if (openIndex != -1) -1 else index
                                } else -1
                            }.border(Theme.border.v10, Theme.color.tertiary).padding(Theme.padding.g1 / blockSize),
                            contentAlignment = Alignment.Center
                        ) {
                            if (ch != BTConfig.CHAR_EMPTY && ch != BTConfig.CHAR_BLOCK) ThemeContainer { SimpleClipText(text = ch.toString()) }
                        }
                    }
                }
            }

            // 浮窗操作按钮
            if (openIndex != -1 && enabled) {
                Surface(
                    modifier = Modifier.zIndex(2f),
                    shape = Theme.shape.v5,
                    contentPadding = Theme.padding.value9,
                    shadowElevation = Theme.shadow.v3
                ) {
                    ActionScope.Left.Container {
                        var inputMode by rememberState { CharacterBlockInputMode.DISABLED }
                        val onCharacterSelectedUpdate by rememberUpdatedState(onCharacterSelected)
                        val onStringSelectedUpdate by rememberUpdatedState(onStringSelected)
                        val onCharacterChangedUpdate by rememberUpdatedState(onCharacterChanged)
                        val textChanged = remember(blockSize, data, writeMode) {
                            suspend { hide: Boolean, index: Int, ch: Char ->
                                if (inputMode == CharacterBlockInputMode.DISABLED) {
                                    val oldCharacter: Char? = if (ch == BTConfig.CHAR_EMPTY || ch == BTConfig.CHAR_BLOCK || ch == BTConfig.CHAR_BLANK) null else ch
                                    onCharacterSelectedUpdate(oldCharacter)?.let { newCharacter ->
                                        if (newCharacter != BTConfig.CHAR_EMPTY && newCharacter != BTConfig.CHAR_BLOCK) {
                                            onCharacterChangedUpdate(index, BlockCharacter(newCharacter, hide))
                                        }
                                    }
                                }
                                else {
                                    onStringSelectedUpdate()?.let { newString ->
                                        // 确定当前索引的位置
                                        val startIndex = if (inputMode == CharacterBlockInputMode.HORIZONTAL) index % blockSize else index / blockSize
                                        repeat(min(blockSize - startIndex, newString.length)) {
                                            val actualIndex = if (inputMode == CharacterBlockInputMode.HORIZONTAL) index + it else index + it * blockSize
                                            data[actualIndex].decode { currentCharacter, currentHide ->
                                                // 防止将不可重写的格子重写
                                                if (writeMode || (currentCharacter != BTConfig.CHAR_EMPTY && currentCharacter != BTConfig.CHAR_BLOCK && currentHide)) {
                                                    onCharacterChangedUpdate(actualIndex, BlockCharacter(newString[it], hide))
                                                }
                                            }
                                        }
                                    }
                                }
                                Unit
                            }
                        }

                        LoadingIcon(icon = Icons.VisibilityOff, onClick = {
                            textChanged(true, openIndex, data[openIndex].ch)
                            openIndex = -1
                        })
                        if (writeMode) {
                            LoadingIcon(icon = Icons.Visibility, onClick = {
                                textChanged(false, openIndex, data[openIndex].ch)
                                openIndex = -1
                            })
                        }
                        Icon(icon = Icons.Square, onClick = {
                            onCharacterChanged(openIndex, if (writeMode) BlockCharacter.Empty else BlockCharacter.Blank)
                            openIndex = -1
                        })
                        ThemeContainer(if (inputMode == CharacterBlockInputMode.DISABLED) LocalColor.current else Theme.color.primary) {
                            Icon(icon = inputMode.icon, onClick = { inputMode = inputMode.next })
                        }
                    }
                }
            }
        }
    }

    @Composable
    override fun ColumnScope.GameAnswerInfoContent(gameDetails: GameDetailsWithName) {
        val answer = remember(gameDetails) {
            catchingNull {
                val question = gameDetails.question.String
                val answer = gameDetails.answer.String
                val gridSize = sqrt(answer.length.toFloat()).toInt()
                require(question.length == answer.length && gridSize * gridSize == answer.length && gridSize in BTConfig.minBlockSize .. BTConfig.maxBlockSize)
                gridSize to List(gridSize * gridSize) { index ->
                    val ch1 = question[index]
                    val ch2 = answer[index]
                    when (ch1) {
                        BTConfig.CHAR_EMPTY -> BlockCharacter.Empty
                        BTConfig.CHAR_BLOCK -> BlockCharacter(ch2, true)
                        else -> BlockCharacter(ch2, false)
                    }
                }
            }
        }

        answer?.let { (blockSize, data) ->
            TextIconAdapter { idIcon, idText ->
                Icon(icon = Icons.Lightbulb, modifier = Modifier.idIcon())
                SimpleEllipsisText(text = "答案", style = Theme.typography.v6.bold, modifier = Modifier.idText())
            }
            Space()
            CharacterBlock(
                blockSize = blockSize,
                data = data,
                enabled = false,
                writeMode = false,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }

    @Composable
    override fun ColumnScope.GameRecordInfoContent(data: GameRecordInfo.Data) {
        val pairData = remember(data) {
            val (answer, info) = data
            catchingNull {
                val answerString = answer.String
                val gridSize = sqrt(answerString.length.toFloat()).toInt()
                require(gridSize * gridSize == answerString.length && gridSize in BTConfig.minBlockSize .. BTConfig.maxBlockSize)
                val data = List(gridSize * gridSize) { index ->
                    when (val ch = answerString[index]) {
                        BTConfig.CHAR_EMPTY -> BlockCharacter.Empty
                        else -> BlockCharacter(ch, false)
                    }
                }
                val actualResult = info.to<BTResult>()
                Triple(gridSize, data, actualResult)
            }
        }

        pairData?.let { (blockSize, list, actualResult) ->
            TextIconAdapter { idIcon, idText ->
                Icon(icon = Icons.Flaky, modifier = Modifier.idIcon())
                SimpleEllipsisText(text = "正确率: ${actualResult.correctCount} / ${actualResult.totalCount}", modifier = Modifier.idText())
            }
            CharacterBlock(
                blockSize = blockSize,
                data = list,
                enabled = false,
                writeMode = false,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    @Stable
    class BTCreateGameState(parent: BasicScreen) : CreateGameState {
        private var blockSize by mutableRefStateOf(SliderArgs(BTConfig.minBlockSize, BTConfig.minBlockSize, BTConfig.maxBlockSize))
        private val blockSquareSize: Int get() = blockSize.value * blockSize.value
        private val data = List(BTConfig.maxBlockSize * BTConfig.maxBlockSize) { BlockCharacter.Empty }.toMutableStateList()

        override val config = BTConfig

        override val canSubmit: Boolean by derivedStateOf {
            var anyHide = false
            var anyBlock = false
            data.fastForEach { it.decode { ch, hide ->
                if (hide) anyHide = true
                if (ch == BTConfig.CHAR_BLOCK) anyBlock = true
            } }
            anyHide && !anyBlock
        }

        override val submitInfo: JsonElement = Unit.toJson()

        override val submitQuestion: JsonElement get() = buildString(blockSquareSize) {
            data.take(blockSquareSize).fastForEach { append(it.decode { ch, hide -> if (hide) BTConfig.CHAR_BLOCK else ch }) }
        }.json

        override val submitAnswer: JsonElement get() = buildString(blockSquareSize) {
            data.take(blockSquareSize).fastForEach { append(it.ch) }
        }.json

        @Composable
        override fun ColumnScope.ConfigContent() {
            GameSlider(
                title = "网格大小",
                args = blockSize,
                onValueChange = { blockSize = blockSize.copy(tmpValue = it) },
                modifier = Modifier.fillMaxWidth()
            )
        }

        @Composable
        override fun ColumnScope.Content() {
            CharacterBlock(
                blockSize = blockSize.value,
                data = data,
                writeMode = true,
                onCharacterSelected = { characterInputDialog.open(it?.toString() ?: "")?.firstOrNull() },
                onStringSelected = { stringInputDialog.open() },
                onCharacterChanged = { index, ch -> data[index] = ch },
                modifier = Modifier.fillMaxWidth()
            )
        }

        @Composable
        override fun Floating() {
            characterInputDialog.Land()
            stringInputDialog.Land()
        }

        private val characterInputDialog = DialogInput(hint = "输入文字", maxLength = 1)
        private val stringInputDialog = DialogInput(hint = "输入多个文字", maxLength = BTConfig.maxBlockSize)
    }

    override val gameCreator: (BasicScreen) -> CreateGameState = ::BTCreateGameState

    @Stable
    class BTPlayGameState(parent: BasicScreen) : PlayGameState {
        @Stable
        private data class Preflight(val gridSize: Int)

        override val config = BTConfig

        private var preflight: Preflight? by mutableRefStateOf(null)
        private var result: BTResult? by mutableRefStateOf(null)

        private val data = List(BTConfig.maxBlockSize * BTConfig.maxBlockSize) { BlockCharacter.Empty }.toMutableStateList()

        override val canSubmit: Boolean by derivedStateOf {
            preflight?.let { (gridSize) ->
                data.take(gridSize * gridSize).any {
                    it.decode { ch, hide -> ch != BTConfig.CHAR_BLANK && hide }
                }
            } ?: false
        }

        override val submitAnswer: JsonElement get() {
            val gridSize = preflight?.gridSize ?: BTConfig.minBlockSize
            return buildString(gridSize * gridSize) {
                data.take(gridSize * gridSize).fastForEach {
                    append(it.ch)
                }
            }.json
        }

        override fun init(preflightResult: PreflightResult) {
            preflight = catchingNull {
                val text = preflightResult.question.String
                val gridSize = sqrt(text.length.toFloat()).toInt()
                require(gridSize * gridSize == text.length && gridSize in BTConfig.minBlockSize .. BTConfig.maxBlockSize)
                data.fill(BlockCharacter.Empty)
                text.forEachIndexed { index, ch ->
                    data[index] = when (ch) {
                        BTConfig.CHAR_EMPTY -> BlockCharacter.Empty
                        BTConfig.CHAR_BLOCK -> BlockCharacter.Blank
                        else -> BlockCharacter(ch, false)
                    }
                }
                Preflight(gridSize = gridSize)
            }
        }

        override fun settle(gameResult: GameResult) {
            result = catchingNull { gameResult.info.to() }
        }

        @Composable
        override fun ColumnScope.Content() {
            preflight?.let { (gridSize) ->
                CharacterBlock(
                    blockSize = gridSize,
                    data = data,
                    writeMode = false,
                    onCharacterSelected = { characterInputDialog.open(it?.toString() ?: "")?.firstOrNull() },
                    onStringSelected = { stringInputDialog.open() },
                    onCharacterChanged = { index, ch -> data[index] = ch },
                    modifier = Modifier.fillMaxWidth()
                )
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
        override fun Floating() {
            characterInputDialog.Land()
            stringInputDialog.Land()
        }

        private val characterInputDialog = DialogInput(hint = "输入文字", maxLength = 1)
        private val stringInputDialog = DialogInput(hint = "输入多个文字", maxLength = BTConfig.maxBlockSize)
    }

    override val gamePlayer: (BasicScreen) -> PlayGameState = ::BTPlayGameState
}