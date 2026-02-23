package love.yinlin.screen

import androidx.compose.animation.animateBounds
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.ZeroCornerSize
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.LookaheadScope
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.util.fastForEachIndexed
import love.yinlin.app
import love.yinlin.common.GameMapper
import love.yinlin.compose.Colors
import love.yinlin.compose.LocalColor
import love.yinlin.compose.LocalImmersivePadding
import love.yinlin.compose.Theme
import love.yinlin.compose.bold
import love.yinlin.compose.extension.mutableRefStateOf
import love.yinlin.compose.extension.rememberFalse
import love.yinlin.compose.screen.NavigationScreen
import love.yinlin.compose.screen.SubScreen
import love.yinlin.compose.ui.container.ActionScope
import love.yinlin.compose.ui.container.Surface
import love.yinlin.compose.ui.container.ThemeContainer
import love.yinlin.compose.ui.floating.Flyout
import love.yinlin.compose.ui.floating.FlyoutPosition
import love.yinlin.compose.ui.floating.SheetContent
import love.yinlin.compose.ui.icon.Icons
import love.yinlin.compose.ui.icon.Icons2
import love.yinlin.compose.ui.image.Icon
import love.yinlin.compose.ui.image.WebImage
import love.yinlin.compose.ui.input.PrimaryButton
import love.yinlin.compose.ui.input.PrimaryTextButton
import love.yinlin.compose.ui.input.SecondaryTextButton
import love.yinlin.compose.ui.input.TertiaryTextButton
import love.yinlin.compose.ui.text.SelectionBox
import love.yinlin.compose.ui.text.SimpleClipText
import love.yinlin.compose.ui.text.SimpleEllipsisText
import love.yinlin.compose.ui.text.Text
import love.yinlin.compose.ui.text.TextIconAdapter
import love.yinlin.cs.ApiGameGetGameRank
import love.yinlin.cs.request
import love.yinlin.cs.url
import love.yinlin.data.rachel.game.Game
import love.yinlin.data.rachel.game.GameRank
import love.yinlin.data.rachel.game.GameType

@Stable
class SubScreenWorld(parent: NavigationScreen) : SubScreen(parent) {
    @Composable
    private fun GameActionLayout(game: Game, modifier: Modifier = Modifier) {
        Column(
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy(Theme.padding.v),
        ) {
            GameMapper.cast<GameMapper>(game)?.let { mapper ->
                PrimaryTextButton(text = "开始", icon = Icons.Play, modifier = Modifier.fillMaxWidth(), onClick = {
                    val profile = app.config.userProfile
                    if (profile != null) with(mapper) { startGame(game, profile) }
                    else slot.tip.warning("请先登录")
                })

                if (mapper.gameCreator != null) {
                    SecondaryTextButton(text = "创建", icon = Icons.Edit, modifier = Modifier.fillMaxWidth(), onClick = {
                        if (app.config.userProfile != null) navigate(::ScreenCreateGame, game)
                        else slot.tip.warning("请先登录")
                    })
                }

                if (mapper.useRanking) {
                    TertiaryTextButton(text = "排行榜", icon = Icons.RewardCup, modifier = Modifier.fillMaxWidth(), onClick = {
                        rankingSheet.open(game)
                    })
                }
            }
        }
    }

    @Composable
    private fun GameCard(game: Game, brush: Brush, modifier: Modifier = Modifier) {
        Column(modifier = modifier) {
            Surface(
                modifier = Modifier.fillMaxWidth(fraction = 0.6f),
                shape = Theme.shape.v3.copy(bottomStart = ZeroCornerSize, bottomEnd = ZeroCornerSize),
                tonalLevel = 5,
                shadowElevation = Theme.shadow.v3,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().background(brush).padding(Theme.padding.value),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ThemeContainer {
                        var visible by rememberFalse()

                        SimpleEllipsisText(text = game.title, style = Theme.typography.v6.bold)

                        Flyout(
                            visible = visible,
                            onClickOutside = { visible = false },
                            position = FlyoutPosition.Bottom,
                            flyout = {
                                Surface(
                                    modifier = Modifier.width(Theme.size.cell3),
                                    shape = Theme.shape.v3,
                                    contentAlignment = Alignment.TopStart,
                                    contentPadding = Theme.padding.value9,
                                    tonalLevel = 8,
                                    shadowElevation = Theme.shadow.v3
                                ) {
                                    Column(verticalArrangement = Arrangement.spacedBy(Theme.padding.v)) {
                                        TextIconAdapter { idIcon, idText ->
                                            Icon(icon = Icons.Lightbulb, modifier = Modifier.idIcon())
                                            SimpleEllipsisText(text = "提示", style = Theme.typography.v7.bold, modifier = Modifier.idText())
                                        }

                                        SelectionBox {
                                            Text(text = game.description, style = Theme.typography.v8)
                                        }
                                    }
                                }
                            }
                        ) {
                            Icon(icon = Icons.Info, onClick = { visible = true })
                        }
                    }
                }
            }
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = Theme.shape.v3.copy(topStart = ZeroCornerSize),
                contentPadding = Theme.padding.value,
                shadowElevation = Theme.shadow.v3,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Theme.padding.h)
                ) {
                    WebImage(
                        uri = game.logo.url,
                        modifier = Modifier.size(Theme.size.image5).clip(Theme.shape.v7),
                    )
                    GameActionLayout(game = game, modifier = Modifier.weight(1f))
                }
            }
        }
    }

    @Composable
    private fun GameGroupLayout(type: GameType, group: List<Game>, modifier: Modifier = Modifier) {
        Column(modifier = modifier) {
            TextIconAdapter(modifier = Modifier.padding(Theme.padding.value9)) { idIcon, idText ->
                Icon(icon = GameMapper.TypeIcons[type]!!, modifier = Modifier.idIcon())
                SimpleEllipsisText(
                    text = type.title,
                    style = Theme.typography.v4.bold,
                    modifier = Modifier.idText()
                )
            }

            val brush = GameMapper.rememberTypeBrush(type)

            LookaheadScope {
                FlowRow(modifier = Modifier.fillMaxWidth()) {
                    for (game in group) {
                        GameCard(
                            game = game,
                            brush = brush,
                            modifier = Modifier.width(Theme.size.cell1).padding(Theme.padding.value9).animateBounds(this@LookaheadScope)
                        )
                    }
                }
            }
        }
    }

    @Composable
    override fun Content() {
        Column(modifier = Modifier.padding(LocalImmersivePadding.current).fillMaxSize().verticalScroll(rememberScrollState())) {
            ActionScope.Right.Container(modifier = Modifier.fillMaxWidth().padding(Theme.padding.value)) {
                PrimaryButton(text = "历史", icon = Icons.History, onClick = {
                    navigate(::ScreenGameRecordHistory)
                })
                PrimaryButton(text = "对局", icon = Icons.Castle, onClick = {
                    navigate(::ScreenGameHistory)
                })
            }

            for ((type, group) in GameMapper.Groups) {
                GameGroupLayout(type = type, group = group, modifier = Modifier.fillMaxWidth())
            }
        }
    }

    private val rankingSheet = this land object : SheetContent<Game>() {
        private var items by mutableRefStateOf(emptyList<GameRank>())

        override suspend fun initialize(args: Game) {
            ApiGameGetGameRank.request(args) { items = it }
        }

        @Composable
        override fun Content(args: Game) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(Theme.padding.eValue9),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Theme.padding.v5)
            ) {
                SimpleEllipsisText(text = args.title, style = Theme.typography.v6.bold)
                items.fastForEachIndexed { index, rank ->
                    Row(
                        modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                        horizontalArrangement = Arrangement.spacedBy(Theme.padding.h),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val rankIndex = index + 1

                        when (rankIndex) {
                            1 -> Icon(icon = Icons2.Rank1, color = Colors.Unspecified)
                            2 -> Icon(icon = Icons2.Rank2, color = Colors.Unspecified)
                            3 -> Icon(icon = Icons2.Rank3, color = Colors.Unspecified)
                            else -> {
                                SimpleClipText(
                                    text = rankIndex.toString(),
                                    style = Theme.typography.v6.bold,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.size(Theme.size.icon)
                                )
                            }
                        }

                        WebImage(
                            uri = rank.avatarPath.url,
                            circle = true,
                            modifier = Modifier.fillMaxHeight().aspectRatio(1f)
                        )

                        SimpleEllipsisText(
                            text = rank.name,
                            style = if (rankIndex in 1 .. 3) Theme.typography.v7.bold else Theme.typography.v7,
                            color = when (index) {
                                1 -> Theme.color.primary
                                2 -> Theme.color.secondary
                                3 -> Theme.color.tertiary
                                else -> LocalColor.current
                            },
                            modifier = Modifier.weight(1f)
                        )

                        SimpleEllipsisText(text = rank.cnt.toString(), style = Theme.typography.v6)
                    }
                }
            }
        }
    }
}