package love.yinlin.ui.screen.world

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.util.fastForEachIndexed
import androidx.compose.ui.zIndex
import kotlinx.serialization.Serializable
import love.yinlin.AppModel
import love.yinlin.Local
import love.yinlin.api.API
import love.yinlin.api.ClientAPI
import love.yinlin.common.ExtraIcons
import love.yinlin.compose.*
import love.yinlin.data.Data
import love.yinlin.data.rachel.game.Game
import love.yinlin.data.rachel.game.GameRank
import love.yinlin.ui.component.image.MiniImage
import love.yinlin.ui.component.image.WebImage
import love.yinlin.ui.component.layout.EmptyBox
import love.yinlin.ui.component.screen.SubScreen
import love.yinlin.ui.screen.community.ScreenUserCard

@Stable
class ScreenGameRanking(model: AppModel, val args: Args) : SubScreen<ScreenGameRanking.Args>(model) {
    @Stable
    @Serializable
    data class Args(val type: Game)

    private var items by mutableRefStateOf(emptyList<GameRank>())

    private suspend fun requestRank() {
        val result = ClientAPI.request(
            route = API.User.Game.GetGameRank,
            data = args.type
        )
        if (result is Data.Success) items = result.data
    }

    @Composable
    private fun RankItem(
        index: Int,
        rank: GameRank,
        modifier: Modifier = Modifier,
        onClick: () -> Unit
    ) {
        Box(modifier = modifier) {
            Row(
                modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)
                    .clip(MaterialTheme.shapes.extraLarge)
                    .clickable(onClick = onClick)
                    .padding(CustomTheme.padding.value),
                horizontalArrangement = Arrangement.spacedBy(CustomTheme.padding.horizontalSpace),
                verticalAlignment = Alignment.CenterVertically
            ) {
                when (index) {
                    1 -> MiniImage(icon = ExtraIcons.Rank1)
                    2 -> MiniImage(icon = ExtraIcons.Rank2)
                    3 -> MiniImage(icon = ExtraIcons.Rank3)
                    else -> Box(
                        modifier = Modifier.padding(CustomTheme.padding.innerIconSpace).size(CustomTheme.size.icon),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = index.toString(),
                            style = MaterialTheme.typography.titleLarge,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.matchParentSize()
                        )
                    }
                }
                WebImage(
                    uri = remember(rank) { rank.avatarPath },
                    circle = true,
                    modifier = Modifier.fillMaxHeight().aspectRatio(1f)
                )
                Text(
                    text = rank.name,
                    style = if (index in 1 .. 3) MaterialTheme.typography.labelMedium else MaterialTheme.typography.bodyMedium,
                    color = when (index) {
                        1 -> MaterialTheme.colorScheme.primary
                        2 -> MaterialTheme.colorScheme.secondary
                        3 -> MaterialTheme.colorScheme.tertiary
                        else -> MaterialTheme.colorScheme.onSurface
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = rank.cnt.toString(),
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }

    override val title: String = args.type.title

    override suspend fun initialize() {
        requestRank()
    }

    @Composable
    override fun SubContent(device: Device) {
        Box(
            modifier = Modifier.padding(LocalImmersivePadding.current).fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            val isLandscape = LocalDevice.current.type != Device.Type.PORTRAIT

            WebImage(
                uri = remember(isLandscape) { args.type.xyPath(isLandscape) },
                key = Local.VERSION,
                contentScale = ContentScale.Crop,
                alpha = 0.75f,
                modifier = Modifier.fillMaxSize().zIndex(1f)
            )

            Surface(
                modifier = Modifier.padding(CustomTheme.padding.equalExtraValue)
                    .widthIn(max = CustomTheme.size.panelWidth)
                    .fillMaxWidth()
                    .zIndex(2f),
                shape = MaterialTheme.shapes.extraLarge,
                shadowElevation = CustomTheme.shadow.surface
            ) {
                Column(
                    modifier = Modifier
                        .padding(CustomTheme.padding.equalExtraValue)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(CustomTheme.padding.verticalExtraSpace)
                ) {
                    Text(
                        text = "排行榜",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(vertical = CustomTheme.padding.verticalSpace)
                    )
                    if (items.isNotEmpty()) {
                        items.fastForEachIndexed { index, item ->
                            RankItem(
                                index = index + 1,
                                rank = item,
                                modifier = Modifier.fillMaxWidth(),
                                onClick = {
                                    navigate(ScreenUserCard.Args(uid = item.uid))
                                }
                            )
                        }
                    }
                    else {
                        Box(modifier = Modifier.size(CustomTheme.size.cardWidth)) {
                            EmptyBox()
                        }
                    }
                }
            }
        }
    }
}