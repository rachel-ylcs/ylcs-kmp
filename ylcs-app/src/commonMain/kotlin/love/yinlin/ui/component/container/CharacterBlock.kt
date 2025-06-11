package love.yinlin.ui.component.container

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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.zIndex
import love.yinlin.common.Colors
import love.yinlin.common.ThemeValue
import love.yinlin.data.rachel.game.info.BTConfig
import love.yinlin.extension.rememberValueState
import love.yinlin.ui.component.image.ClickIcon
import love.yinlin.ui.component.image.LoadingIcon
import kotlin.jvm.JvmInline

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
value class BlockCharacter(val value: Int) {
    constructor(ch: Char, hide: Boolean) : this(ch.code or (if (hide) 1 shl 16 else 0))

    val ch: Char get() = (value and 0xFFFF).toChar()

    inline fun <R> decode(block: (Char, Boolean) -> R) = block((value and 0xFFFF).toChar(), (value and 0x10000) != 0)

    companion object {
        val Empty = BlockCharacter(BTConfig.CHAR_EMPTY, false)
        val Blank = BlockCharacter(BTConfig.CHAR_BLANK, true)
    }
}

@Composable
fun CharacterBlock(
    blockSize: Int,
    data: List<BlockCharacter>,
    writeMode: Boolean,
    onCharacterSelected: suspend (Int, Char?) -> Char?,
    onCharacterChanged: (Int, BlockCharacter) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        var openIndex by rememberValueState(-1)

        LazyVerticalGrid(
            columns = GridCells.Fixed(blockSize),
            modifier = Modifier.fillMaxSize().zIndex(1f).drawGrid(
                blockSize = blockSize,
                color = MaterialTheme.colorScheme.tertiary
            ),
            userScrollEnabled = false
        ) {
            items(blockSize * blockSize) { index ->
                data[index].decode { ch, hide ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = ThemeValue.Size.PanelWidth)
                            .aspectRatio(1f)
                            .background(when {
                                ch == BTConfig.CHAR_EMPTY || ch == BTConfig.CHAR_BLOCK -> Colors.Transparent
                                hide -> MaterialTheme.colorScheme.secondaryContainer
                                else -> MaterialTheme.colorScheme.primaryContainer
                            }).clickable {
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

        if (openIndex != -1) {
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
                    val textChanged: suspend (Boolean, Int, Char) -> Unit = remember(onCharacterSelected, onCharacterChanged) {
                        { hide, index, ch ->
                            val oldCharacter: Char? = if (ch == BTConfig.CHAR_EMPTY || ch == BTConfig.CHAR_BLOCK || ch == BTConfig.CHAR_BLANK) null else ch
                            onCharacterSelected(index, oldCharacter)?.let { newCharacter ->
                                if (newCharacter != BTConfig.CHAR_EMPTY && newCharacter != BTConfig.CHAR_BLOCK) {
                                    onCharacterChanged(index, BlockCharacter(newCharacter, hide))
                                }
                            }
                        }
                    }

                    LoadingIcon(
                        icon = Icons.Outlined.TextFormat,
                        size = ThemeValue.Size.MediumIcon,
                        onClick = {
                            textChanged(true, openIndex, data[openIndex].ch)
                            openIndex = -1
                        }
                    )
                    if (writeMode) {
                        LoadingIcon(
                            icon = Icons.Outlined.Spellcheck,
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
                        icon = Icons.Outlined.Close,
                        size = ThemeValue.Size.MediumIcon,
                        onClick = { openIndex = -1 }
                    )
                }
            }
        }
    }
}