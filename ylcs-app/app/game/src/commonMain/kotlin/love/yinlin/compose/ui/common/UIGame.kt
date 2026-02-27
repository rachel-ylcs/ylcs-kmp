package love.yinlin.compose.ui.common

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import love.yinlin.Local
import love.yinlin.common.GameItemExtraInfo
import love.yinlin.common.GameMapper
import love.yinlin.compose.LocalColorVariant
import love.yinlin.compose.Theme
import love.yinlin.compose.bold
import love.yinlin.compose.ui.collection.TagView
import love.yinlin.compose.ui.container.ActionScope
import love.yinlin.compose.ui.container.Surface
import love.yinlin.compose.ui.container.ThemeContainer
import love.yinlin.compose.ui.icon.Icons
import love.yinlin.compose.ui.image.Icon
import love.yinlin.compose.ui.image.WebImage
import love.yinlin.compose.ui.text.SimpleEllipsisText
import love.yinlin.compose.ui.text.Text
import love.yinlin.compose.ui.text.TextIconAdapter
import love.yinlin.cs.url
import love.yinlin.data.rachel.game.GamePublicDetailsWithName

@Composable
internal fun TopPager(
    currentIndex: Int,
    name: String,
    onIncrease: () -> Unit,
    onDecrease: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(Theme.padding.h),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon = Icons.KeyboardArrowLeft, tip = "上行", onClick = onDecrease)
        SimpleEllipsisText(
            text = "${currentIndex + 1} $name",
            style = Theme.typography.v7.bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f)
        )
        Icon(icon = Icons.KeyboardArrowRight, tip = "下行", onClick = onIncrease)
    }
}

@Composable
internal fun GameItem(
    gameDetails: GamePublicDetailsWithName,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Surface(
        modifier = modifier,
        shape = Theme.shape.v3,
        contentPadding = Theme.padding.value,
        shadowElevation = Theme.shadow.v3,
        onClick = onClick
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(Theme.padding.v10)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(Theme.padding.h)
            ) {
                WebImage(
                    uri = gameDetails.type.logo.url,
                    key = Local.info.version,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxHeight().aspectRatio(1f).clip(Theme.shape.v7)
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(Theme.padding.v10)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SimpleEllipsisText(text = gameDetails.type.title, style = Theme.typography.v7.bold)
                        ActionScope.Right.Container(content = actions)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextIconAdapter { idIcon, idText ->
                            Icon(icon = Icons.AccountCircle, modifier = Modifier.idIcon())
                            SimpleEllipsisText(text = gameDetails.name, style = Theme.typography.v8, modifier = Modifier.idText())
                        }
                        SimpleEllipsisText(text = gameDetails.ts, color = LocalColorVariant.current, style = Theme.typography.v8)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ThemeContainer(Theme.color.primary) {
                            TextIconAdapter { idIcon, idText ->
                                Icon(icon = Icons.Diamond, modifier = Modifier.idIcon())
                                SimpleEllipsisText(text = "奖池 ${gameDetails.reward}", style = Theme.typography.v8.bold, modifier = Modifier.idText())
                            }
                        }

                        ThemeContainer(Theme.color.secondary) {
                            TextIconAdapter { idIcon, idText ->
                                Icon(icon = Icons.FormatListNumbered, modifier = Modifier.idIcon())
                                SimpleEllipsisText(text = "名额 ${gameDetails.num}", style = Theme.typography.v8.bold, modifier = Modifier.idText())
                            }
                        }

                        ThemeContainer(Theme.color.tertiary) {
                            TextIconAdapter { idIcon, idText ->
                                Icon(icon = Icons.Paid, modifier = Modifier.idIcon())
                                SimpleEllipsisText(text = "入场 ${gameDetails.cost}", style = Theme.typography.v8.bold, modifier = Modifier.idText())
                            }
                        }
                    }
                }
            }
            Text(text = gameDetails.title, modifier = Modifier.fillMaxWidth())

            GameMapper.cast<GameItemExtraInfo>(gameDetails.type)?.apply { GameItemExtraInfoContent(gameDetails) }

            val winnerSize = gameDetails.winner.size
            if (winnerSize > 0) {
                TagView(
                    size = winnerSize,
                    titleProvider = { gameDetails.winner[it] },
                    iconProvider = { Icons.Check },
                    readonly = true,
                    style = Theme.typography.v8,
                    contentColor = Theme.color.primary,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}