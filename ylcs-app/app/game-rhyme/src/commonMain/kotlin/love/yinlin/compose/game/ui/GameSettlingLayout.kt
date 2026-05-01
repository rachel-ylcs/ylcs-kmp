package love.yinlin.compose.game.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.zIndex
import love.yinlin.Local
import love.yinlin.app
import love.yinlin.app.game_rhyme.resources.Res
import love.yinlin.app.game_rhyme.resources.rhyme
import love.yinlin.compose.Colors
import love.yinlin.compose.Device
import love.yinlin.compose.LocalImmersivePadding
import love.yinlin.compose.Theme
import love.yinlin.compose.bold
import love.yinlin.compose.data.ImageQuality
import love.yinlin.compose.game.common.BlockResult
import love.yinlin.compose.game.data.RhymeState
import love.yinlin.compose.rememberDeviceType
import love.yinlin.compose.rememberFontFamily
import love.yinlin.compose.ui.container.Surface
import love.yinlin.compose.ui.icon.Icons
import love.yinlin.compose.ui.image.LocalFileImage
import love.yinlin.compose.ui.image.WebImage
import love.yinlin.compose.ui.input.PrimaryButton
import love.yinlin.compose.ui.input.SecondaryButton
import love.yinlin.compose.ui.text.SimpleClipText
import love.yinlin.compose.ui.text.SimpleEllipsisText
import love.yinlin.compose.ui.text.StrokeText
import love.yinlin.cs.ServerRes
import love.yinlin.cs.url
import love.yinlin.data.mod.ModResourceType
import love.yinlin.data.music.MusicInfo
import love.yinlin.data.rachel.game.Game
import love.yinlin.data.rachel.rhyme.CharacterInfo
import love.yinlin.data.rachel.rhyme.RhymeDifficulty
import love.yinlin.data.rachel.rhyme.RhymePlayResult
import love.yinlin.extension.DateEx
import love.yinlin.extension.timeString
import kotlin.math.sqrt

@Composable
private fun SettlingMusicInfo(
    info: MusicInfo,
    difficulty: RhymeDifficulty,
    duration: Long,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shadowElevation = Theme.shadow.v5,
        tonalLevel = 1,
        border = BorderStroke(Theme.border.v4, Theme.color.primary)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(Theme.padding.h9)
        ) {
            LocalFileImage(
                uri = info.path(app.modPath, ModResourceType.Record).path,
                modifier = Modifier.fillMaxHeight().aspectRatio(1f)
            )
            Column(
                modifier = Modifier.fillMaxWidth().padding(top = Theme.padding.v9, bottom = Theme.padding.v9, end = Theme.padding.h9),
                verticalArrangement = Arrangement.spacedBy(Theme.padding.v9)
            ) {
                SimpleEllipsisText(text = info.name, style = Theme.typography.v5.bold, color = Theme.color.primary)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Theme.padding.h),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    DifficultyStar(difficulty, modifier = Modifier.weight(1f).height(Theme.size.icon))
                    SimpleEllipsisText(text = duration.timeString)
                }
            }
        }
    }
}

@Composable
private fun SettlingIllustration(
    info: CharacterInfo,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shadowElevation = Theme.shadow.v5,
        tonalLevel = 1,
        border = BorderStroke(Theme.border.v4, Theme.color.primary)
    ) {
        WebImage(
            uri = ServerRes.Game.Rhyme.CV.illustration(info.id).url,
            key = info.id,
            modifier = Modifier.fillMaxSize()
        )
        SimpleEllipsisText(
            text = info.title,
            color = Colors.White,
            style = Theme.typography.v6.bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().background(Colors.Dark.copy(alpha = 0.8f)).padding(Theme.padding.value).align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun ResultDivider(modifier: Modifier = Modifier) {
    Box(modifier = modifier.drawWithContent {
        val mainColor = Color(0xFFF8D033)
        val canvasWidth = size.width
        val canvasHeight = size.height
        val gap = canvasWidth * 0.05f
        val triWidth = canvasHeight * (2f / sqrt(3.0f))
        val centerX = canvasWidth / 2f
        val centerY = canvasHeight / 2f
        val pathUp = Path().apply {
            moveTo(centerX, 0f)
            lineTo(centerX + triWidth / 2f, canvasHeight * 0.75f)
            lineTo(centerX - triWidth / 2f, canvasHeight * 0.75f)
            close()
        }
        val pathDown = Path().apply {
            moveTo(centerX, canvasHeight)
            lineTo(centerX - triWidth / 2f, canvasHeight * 0.25f)
            lineTo(centerX + triWidth / 2f, canvasHeight * 0.25f)
            close()
        }

        drawPath(pathUp, color = mainColor, style = Stroke(1f))
        drawPath(pathDown, color = mainColor, style = Stroke(1f))

        val lineHeight = canvasHeight * 0.05f
        val lineY = centerY - (lineHeight / 2f)

        val starOuterEdge = triWidth / 2f
        val lineStartX = centerX + starOuterEdge + gap
        val lineEndX = centerX - starOuterEdge - gap

        drawRect(
            brush = Brush.linearGradient(
                colors = listOf(mainColor, Color.Transparent),
                start = Offset(lineStartX, centerY),
                end = Offset(canvasWidth, centerY)
            ),
            topLeft = Offset(lineStartX, lineY),
            size = Size(canvasWidth - lineStartX, lineHeight)
        )

        drawRect(
            brush = Brush.linearGradient(
                colors = listOf(mainColor, Color.Transparent),
                start = Offset(lineEndX, centerY),
                end = Offset(0f, centerY)
            ),
            topLeft = Offset(0f, lineY),
            size = Size(lineEndX, lineHeight)
        )
    })
}

@Composable
private fun SettlingResult(
    result: RhymePlayResult,
    modifier: Modifier = Modifier,
    onBack: () -> Unit,
    onSubmit: () -> Unit
) {
    val fontFamily = rememberFontFamily(Res.font.rhyme)

    Surface(
        modifier = modifier,
        contentPadding = Theme.padding.eValue7,
        shadowElevation = Theme.shadow.v5,
        tonalLevel = 1,
        border = BorderStroke(Theme.border.v4, Theme.color.primary)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Theme.padding.v7)
        ) {
            val brush = remember { Brush.linearGradient(
                0.0f to Color(0xFFFFFBDF),
                0.2f to Color(0xFFE0BE37),
                0.5f to Color(0xFFCDAC25),
                0.8f to Color(0xFFD5BD73),
                1.0f to Color(0xFFE29E35)
            ) }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SecondaryButton(
                    text = "返回",
                    icon = Icons.ArrowBack,
                    style = Theme.typography.v6.bold,
                    onClick = onBack,
                    padding = Theme.padding.value9
                )
                PrimaryButton(
                    text = "提交成绩",
                    icon = Icons.Upload,
                    style = Theme.typography.v6.bold,
                    onClick = onSubmit,
                    padding = Theme.padding.value9
                )
            }

            SimpleClipText(text = "· 结算 ·", style = Theme.typography.v5.bold, color = Theme.color.primary)

            StrokeText(
                text = result.score.toString(),
                strokeColor = Colors.White,
                strokeWidth = 6.sp,
                letterSpacing = 6.sp,
                style = Theme.typography.v2.bold.copy(brush = brush, fontFamily = fontFamily)
            )

            ResultDivider(modifier = Modifier.fillMaxWidth().height(Theme.size.icon))

            val countList = remember {
                (result.statistics.mapIndexed { index, count ->
                    val blockResult = BlockResult.entries[index]
                    Triple(blockResult.title, blockResult.color, count)
                } + Triple("COMBO+", Colors.Yellow4, result.maxCombo)).reversed()
            }

            countList.fastForEach { (title, color, count) ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = Theme.padding.h9),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SimpleClipText(text = title, style = Theme.typography.v3.bold.copy(fontFamily = fontFamily), color = lerp(color, Colors.White, 0.1f))
                    SimpleEllipsisText(text = count.toString(), style = Theme.typography.v3.bold.copy(fontFamily = fontFamily), color = color)
                }
            }

            SimpleEllipsisText(text = remember { DateEx.CurrentString }, style = Theme.typography.v6.bold, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
internal fun GameSettlingLayout(
    state: RhymeState.Settling,
    onBack: () -> Unit,
    onSubmit: () -> Unit
) {
    val (info, config, result) = state
    val device by rememberDeviceType()

    Box(modifier = Modifier.fillMaxSize()) {
        WebImage(
            uri = Game.Rhyme.logo.url,
            key = Local.info.version,
            quality = ImageQuality.Full,
            contentScale = ContentScale.Crop,
            alpha = 0.75f,
            modifier = Modifier.fillMaxSize().zIndex(1f)
        )

        Box(modifier = Modifier.fillMaxSize().padding(LocalImmersivePadding.current).zIndex(2f)) {
            if (device == Device.Type.PORTRAIT) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(Theme.padding.eValue9).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(Theme.padding.v7)
                ) {
                    SettlingMusicInfo(
                        info = info,
                        difficulty = config.difficulty,
                        duration = result.duration,
                        modifier = Modifier.fillMaxWidth()
                    )
                    SettlingResult(
                        result = result,
                        modifier = Modifier.fillMaxWidth(),
                        onBack = onBack,
                        onSubmit = onSubmit
                    )
                    SettlingIllustration(
                        info = config.character,
                        modifier = Modifier.fillMaxWidth().aspectRatio(1f)
                    )
                }
            }
            else {
                Row(
                    modifier = Modifier.fillMaxSize().padding(Theme.padding.eValue9),
                    horizontalArrangement = Arrangement.spacedBy(Theme.padding.h7)
                ) {
                    Column(
                        modifier = Modifier.widthIn(min = Theme.size.cell1, max = Theme.size.cell2 * 1.5f).fillMaxHeight().verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(Theme.padding.v7)
                    ) {
                        SettlingMusicInfo(
                            info = info,
                            difficulty = config.difficulty,
                            duration = result.duration,
                            modifier = Modifier.fillMaxWidth()
                        )
                        SettlingIllustration(
                            info = config.character,
                            modifier = Modifier.fillMaxWidth().aspectRatio(1f)
                        )
                    }
                    Box(modifier = Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState())) {
                        SettlingResult(
                            result = result,
                            modifier = Modifier.fillMaxSize(),
                            onBack = onBack,
                            onSubmit = onSubmit
                        )
                    }
                }
            }
        }
    }
}