package love.yinlin.ui.screen.world.game

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.zIndex
import kotlinx.coroutines.CoroutineScope
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import love.yinlin.common.Colors
import love.yinlin.common.ThemeValue
import love.yinlin.data.rachel.game.GameConfig
import love.yinlin.data.rachel.game.GameDetailsWithName
import love.yinlin.data.rachel.game.GamePublicDetailsWithName
import love.yinlin.data.rachel.game.GameResult
import love.yinlin.data.rachel.game.PreflightResult
import love.yinlin.data.rachel.game.info.BTConfig
import love.yinlin.data.rachel.game.info.BTResult
import love.yinlin.extension.String
import love.yinlin.extension.catchingNull
import love.yinlin.extension.mutableRefStateOf
import love.yinlin.extension.rememberState
import love.yinlin.extension.rememberValueState
import love.yinlin.extension.to
import love.yinlin.extension.toJson
import love.yinlin.ui.component.image.ClickIcon
import love.yinlin.ui.component.image.LoadingIcon
import love.yinlin.ui.component.screen.FloatingDialogInput
import love.yinlin.ui.screen.SubScreenSlot
import kotlin.jvm.JvmInline
import kotlin.math.min
import kotlin.math.sqrt
import kotlin.to

private fun Modifier.drawGrid(blockSize: Int, color: Color) = drawWithContent {
    drawContent()
    val width = size.width
    val space = width / blockSize
    val strokeWidth = 16f / blockSize
    repeat(blockSize + 1) { index ->
        drawLine(
            color = color,
            start = Offset(index * space, 0f),
            end = Offset(index * space, width),
            strokeWidth = strokeWidth
        )
    }
    repeat(blockSize + 1) { index ->
        drawLine(
            color = color,
            start = Offset(0f, index * space),
            end = Offset(width, index * space),
            strokeWidth = strokeWidth
        )
    }
}

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
private enum class CharacterBlockInputMode {
    DISABLED, HORIZONTAL, VERTICAL;

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
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        var openIndex by rememberValueState(-1)
        var inputMode by rememberState { CharacterBlockInputMode.DISABLED }

        LazyVerticalGrid(
            columns = GridCells.Fixed(blockSize),
            modifier = Modifier.fillMaxWidth().aspectRatio(1f).zIndex(1f).drawGrid(
                blockSize = blockSize,
                color = MaterialTheme.colorScheme.tertiary
            ),
            userScrollEnabled = false
        ) {
            items(blockSize * blockSize) { index ->
                data[index].decode { ch, hide ->
                    Box(
                        modifier = Modifier
                            .widthIn(max = ThemeValue.Size.PanelWidth)
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .background(when {
                                ch == BTConfig.CHAR_EMPTY || ch == BTConfig.CHAR_BLOCK -> Colors.Transparent
                                hide -> MaterialTheme.colorScheme.secondaryContainer
                                else -> MaterialTheme.colorScheme.primaryContainer
                            }).clickable(enabled = enabled) {
                                openIndex = if (writeMode) if (openIndex != -1) -1 else index
                                else if (ch != BTConfig.CHAR_EMPTY && ch != BTConfig.CHAR_BLOCK && hide) {
                                    if (openIndex != -1) -1 else index
                                } else -1
                            }.padding(ThemeValue.Padding.LittleSpace * 16f / blockSize),
                        contentAlignment = Alignment.Center
                    ) {
                        if (ch != BTConfig.CHAR_EMPTY && ch != BTConfig.CHAR_BLOCK) {
                            BasicText(
                                text = ch.toString(),
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = if (hide) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onPrimaryContainer
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Clip,
                                autoSize = TextAutoSize.StepBased()
                            )
                        }
                    }
                }
            }
        }

        if (openIndex != -1 && enabled) {
            Surface(
                modifier = Modifier.zIndex(2f),
                shape = MaterialTheme.shapes.large,
                shadowElevation = ThemeValue.Shadow.Surface
            ) {
                Row(
                    modifier = Modifier.padding(ThemeValue.Padding.ExtraValue),
                    horizontalArrangement = Arrangement.spacedBy(ThemeValue.Padding.HorizontalSpace),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val textChanged: suspend (Boolean, Int, Char) -> Unit =
                        remember(inputMode, onCharacterSelected, onStringSelected, onCharacterChanged) {
                            { hide, index, ch ->
                                if (inputMode == CharacterBlockInputMode.DISABLED) {
                                    val oldCharacter: Char? = if (ch == BTConfig.CHAR_EMPTY || ch == BTConfig.CHAR_BLOCK || ch == BTConfig.CHAR_BLANK) null else ch
                                    onCharacterSelected(oldCharacter)?.let { newCharacter ->
                                        if (newCharacter != BTConfig.CHAR_EMPTY && newCharacter != BTConfig.CHAR_BLOCK) {
                                            onCharacterChanged(index, BlockCharacter(newCharacter, hide))
                                        }
                                    }
                                }
                                else {
                                    onStringSelected()?.let { newString ->
                                        // 确定当前索引的位置
                                        val startIndex = if (inputMode == CharacterBlockInputMode.HORIZONTAL) index % blockSize else index / blockSize
                                        repeat(min(blockSize - startIndex, newString.length)) {
                                            val actualIndex = if (inputMode == CharacterBlockInputMode.HORIZONTAL) index + it else index + it * blockSize
                                            data[actualIndex].decode { currentCharacter, currentHide ->
                                                // 防止将不可重写的格子重写
                                                if (writeMode || (currentCharacter != BTConfig.CHAR_EMPTY && currentCharacter != BTConfig.CHAR_BLOCK && currentHide)) {
                                                    onCharacterChanged(actualIndex, BlockCharacter(newString[it], hide))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                    LoadingIcon(
                        icon = Icons.Outlined.VisibilityOff,
                        size = ThemeValue.Size.MediumIcon,
                        onClick = {
                            textChanged(true, openIndex, data[openIndex].ch)
                            openIndex = -1
                        }
                    )
                    if (writeMode) {
                        LoadingIcon(
                            icon = Icons.Outlined.Visibility,
                            size = ThemeValue.Size.MediumIcon,
                            onClick = {
                                textChanged(false, openIndex, data[openIndex].ch)
                                openIndex = -1
                            }
                        )
                    }
                    ClickIcon(
                        icon = Icons.Outlined.Square,
                        size = ThemeValue.Size.MediumIcon,
                        onClick = {
                            onCharacterChanged(openIndex, if (writeMode) BlockCharacter.Empty else BlockCharacter.Blank)
                            openIndex = -1
                        }
                    )
                    ClickIcon(
                        icon = when (inputMode) {
                            CharacterBlockInputMode.DISABLED -> Icons.Outlined.MobiledataOff
                            CharacterBlockInputMode.HORIZONTAL -> Icons.Outlined.SwapHoriz
                            CharacterBlockInputMode.VERTICAL -> Icons.Outlined.SwapVert
                        },
                        color = if (inputMode == CharacterBlockInputMode.DISABLED) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.primary,
                        onClick = { inputMode = inputMode.next }
                    )
                }
            }
        }
    }
}

@Composable
fun ColumnScope.BlockTextCardInfo(game: GamePublicDetailsWithName) {}

@Composable
fun ColumnScope.BlockTextCardQuestionAnswer(game: GameDetailsWithName) {
    val answer = remember(game) {
        catchingNull {
            val question = game.question.String
            val answer = game.answer.String
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
        Text(
            text = "答案",
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
        CharacterBlock(
            blockSize = blockSize,
            data = data,
            enabled = false,
            writeMode = false,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun ColumnScope.BlockTextRecordResult(result: BTResult) {
    val (correctCount, totalCount) = result
    Text(
        text = "正确率: $correctCount / $totalCount",
        style = MaterialTheme.typography.titleLarge,
        textAlign = TextAlign.Center,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
fun ColumnScope.BlockTextRecordCard(answer: JsonElement, info: JsonElement) {
    val data = remember(answer, info) {
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

    data?.let { (blockSize, data, actualResult) ->
        BlockTextRecordResult(actualResult)
        CharacterBlock(
            blockSize = blockSize,
            data = data,
            enabled = false,
            writeMode = false,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Stable
class BlockTextCreateGameState(val slot: SubScreenSlot) : CreateGameState {
    override val config: GameConfig = BTConfig

    private var blockSize by mutableFloatStateOf(0f)
    private val gridSize by derivedStateOf { blockSize.cast(BTConfig.minBlockSize, BTConfig.maxBlockSize) }
    private val data = List(BTConfig.maxBlockSize * BTConfig.maxBlockSize) { BlockCharacter.Empty }.toMutableStateList()

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

    override val submitQuestion: JsonElement get() = JsonPrimitive(buildString(gridSize * gridSize) {
        data.take(gridSize * gridSize).fastForEach {
            append(it.decode { ch, hide -> if (hide) BTConfig.CHAR_BLOCK else ch })
        }
    })

    override val submitAnswer: JsonElement get() = JsonPrimitive(buildString(gridSize * gridSize) {
        data.take(gridSize * gridSize).fastForEach {
            append(it.ch)
        }
    })

    @Composable
    override fun ColumnScope.Content() {
        GameSlider(
            title = "网格大小",
            progress = blockSize,
            minValue = BTConfig.minBlockSize,
            maxValue = BTConfig.maxBlockSize,
            onProgressChange = { blockSize = it },
            modifier = Modifier.fillMaxWidth()
        )
        CharacterBlock(
            blockSize = gridSize,
            data = data,
            writeMode = true,
            onCharacterSelected = { characterInputDialog.openSuspend(it?.toString() ?: "")?.firstOrNull() },
            onStringSelected = { stringInputDialog.openSuspend() },
            onCharacterChanged = { index, ch -> data[index] = ch },
            modifier = Modifier.fillMaxWidth()
        )
    }

    private val characterInputDialog = FloatingDialogInput(
        hint = "输入文字",
        maxLength = 1,
        clearButton = false
    )

    private val stringInputDialog = FloatingDialogInput(
        hint = "输入多个文字",
        maxLength = BTConfig.maxBlockSize,
        clearButton = false
    )

    @Composable
    override fun Floating() {
        characterInputDialog.Land()
        stringInputDialog.Land()
    }
}

@Stable
class BlockTextPlayGameState(val slot: SubScreenSlot) : PlayGameState {
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
        return JsonPrimitive(buildString(gridSize * gridSize) {
            data.take(gridSize * gridSize).fastForEach {
                append(it.ch)
            }
        })
    }

    override fun init(scope: CoroutineScope, preflightResult: PreflightResult) {
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
                onCharacterSelected = { characterInputDialog.openSuspend(it?.toString() ?: "")?.firstOrNull() },
                onStringSelected = { stringInputDialog.openSuspend() },
                onCharacterChanged = { index, ch -> data[index] = ch },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    @Composable
    override fun ColumnScope.Settlement() {
        result?.let { BlockTextRecordResult(it) }
    }

    private val characterInputDialog = FloatingDialogInput(
        hint = "输入文字",
        maxLength = 1,
        clearButton = false
    )

    private val stringInputDialog = FloatingDialogInput(
        hint = "输入多个文字",
        maxLength = BTConfig.maxBlockSize,
        clearButton = false
    )

    @Composable
    override fun Floating() {
        characterInputDialog.Land()
        stringInputDialog.Land()
    }
}