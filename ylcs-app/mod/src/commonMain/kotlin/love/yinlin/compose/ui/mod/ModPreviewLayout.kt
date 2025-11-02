package love.yinlin.compose.ui.mod

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import love.yinlin.compose.Colors
import love.yinlin.compose.CustomTheme
import love.yinlin.compose.ui.layout.Space
import love.yinlin.data.compose.ItemKey
import love.yinlin.extension.fileSizeString
import love.yinlin.mod.ModFactory.Preview.PreviewResult

@Composable
fun ModPreviewLayout(modifier: Modifier = Modifier, result: PreviewResult) {
    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(CustomTheme.padding.verticalSpace)
    ) {
        item(ItemKey("Metadata")) {
            val metadata = result.metadata
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shadowElevation = CustomTheme.shadow.surface,
                border = BorderStroke(width = CustomTheme.border.small, color = Colors.Gray3)
            ) {
                Column(
                    modifier = Modifier.padding(CustomTheme.padding.value),
                    verticalArrangement = Arrangement.spacedBy(CustomTheme.padding.verticalSpace)
                ) {
                    Text(
                        text = "MOD v${metadata.version}",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                    Text(text = "媒体数: ${metadata.mediaNum}")
                    Text(text = "作者: ${metadata.info.author}")
                }
            }
        }
        items(
            items = result.medias,
            key = { it.id }
        ) { mediaItem ->
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shadowElevation = CustomTheme.shadow.surface,
                border = BorderStroke(width = CustomTheme.border.small, color = Colors.Gray3)
            ) {
                Column(
                    modifier = Modifier.padding(CustomTheme.padding.extraValue),
                    verticalArrangement = Arrangement.spacedBy(CustomTheme.padding.verticalSpace)
                ) {
                    val config = mediaItem.config
                    if (config != null) {
                        Text(
                            text = config.name,
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                        Text(text = "版本: ${config.version}")
                        Text(text = "作者: ${config.author}")
                        Text(text = "ID: ${config.id}")
                        Text(text = "演唱: ${config.singer}")
                        Text(text = "作词: ${config.lyricist}")
                        Text(text = "作曲: ${config.composer}")
                        Text(text = "专辑: ${config.album}")
                        Text(text = "副歌点: ${config.chorus}")
                        Space()
                    }
                    Text(
                        text = "资源表",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                    for ((resource, length) in mediaItem.resources) {
                        Row(
                            modifier = Modifier.fillMaxWidth().border(CustomTheme.border.small, Colors.Gray3).padding(CustomTheme.padding.value),
                            horizontalArrangement = Arrangement.spacedBy(CustomTheme.padding.horizontalExtraSpace)
                        ) {
                            Text(
                                text = "${resource.description} (${resource.type})",
                                modifier = Modifier.weight(1f)
                            )
                            Text(text = remember(length) { length.toLong().fileSizeString })
                        }
                    }
                }
            }
        }
    }
}