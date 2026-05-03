package love.yinlin.compose.game.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.util.fastForEachIndexed
import love.yinlin.compose.Colors
import love.yinlin.compose.LocalColor
import love.yinlin.compose.LocalImmersivePadding
import love.yinlin.compose.Theme
import love.yinlin.compose.bold
import love.yinlin.compose.extension.rememberState
import love.yinlin.compose.game.data.RhymePlayConfig
import love.yinlin.compose.game.data.RhymeRankItem
import love.yinlin.compose.ui.container.Surface
import love.yinlin.compose.ui.icon.Icons2
import love.yinlin.compose.ui.image.Icon
import love.yinlin.compose.ui.image.WebImage
import love.yinlin.compose.ui.input.Filter
import love.yinlin.compose.ui.text.SimpleClipText
import love.yinlin.compose.ui.text.SimpleEllipsisText
import love.yinlin.data.music.MusicInfo
import love.yinlin.data.rachel.rhyme.RhymeDifficulty

@Composable
internal fun GameRankLayout(
    info: MusicInfo,
    map: Map<RhymeDifficulty, List<RhymeRankItem>>
) {
    Box(
        modifier = Modifier.fillMaxSize().padding(LocalImmersivePadding.current),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .widthIn(min = Theme.size.cell1, max = Theme.size.cell1 * 1.5f)
                .fillMaxHeight()
                .padding(Theme.padding.eValue9)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Theme.padding.v5)
        ) {
            var difficulty by rememberState { RhymePlayConfig.Default.difficulty }
            val items = map[difficulty] ?: emptyList()

            RhymeMusicCard(info = info, modifier = Modifier.fillMaxWidth()) {
                DifficultyStar(difficulty, modifier = Modifier.fillMaxWidth().height(Theme.size.icon))
            }

            Filter(
                size = RhymeDifficulty.entries.size,
                selectedProvider = { difficulty == RhymeDifficulty.entries[it] },
                titleProvider = { RhymeDifficulty.entries[it].title },
                onClick = { index, selected -> if (selected) difficulty = RhymeDifficulty.entries[index] }
            )

            Surface(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = Theme.padding.eValue9,
                shadowElevation = Theme.shadow.v5,
                tonalLevel = 1,
                shape = Theme.shape.v5
            ) {
                if (items.isEmpty()) SimpleEllipsisText(text = "榜单空空如也, 快来占位吧...")
                else {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(Theme.padding.v5)
                    ) {
                        items.fastForEachIndexed { index, item ->
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
                                    uri = item.avatarPath,
                                    circle = true,
                                    modifier = Modifier.fillMaxHeight().aspectRatio(1f)
                                )

                                SimpleEllipsisText(
                                    text = "${item.name} / ${item.character.title}",
                                    style = if (rankIndex in 1 .. 3) Theme.typography.v7.bold else Theme.typography.v7,
                                    color = when (rankIndex) {
                                        1 -> Theme.color.primary
                                        2 -> Theme.color.secondary
                                        3 -> Theme.color.tertiary
                                        else -> LocalColor.current
                                    },
                                    modifier = Modifier.weight(1f)
                                )

                                SimpleEllipsisText(text = item.score.toString(), style = Theme.typography.v6)
                            }
                        }
                    }
                }
            }
        }
    }
}