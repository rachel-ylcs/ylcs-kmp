package love.yinlin.compose.ui.mod

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import love.yinlin.compose.Theme
import love.yinlin.compose.bold
import love.yinlin.compose.data.ItemKey
import love.yinlin.compose.ui.container.Surface
import love.yinlin.compose.ui.layout.Space
import love.yinlin.compose.ui.text.Text
import love.yinlin.extension.fileSizeString
import love.yinlin.mod.ModFactory.Preview.PreviewResult

@Composable
fun ModPreviewLayout(modifier: Modifier = Modifier, result: PreviewResult) {
    LazyColumn(modifier = modifier) {
        item(ItemKey("Metadata")) {
            val metadata = result.metadata
            Surface(
                modifier = Modifier.padding(vertical = Theme.padding.v9).fillMaxWidth(),
                contentPadding = Theme.padding.value9,
                shadowElevation = Theme.shadow.v3,
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(Theme.padding.v)
                ) {
                    Text(
                        text = "MOD v${metadata.version}",
                        style = Theme.typography.v6.bold,
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
                modifier = Modifier.padding(vertical = Theme.padding.v9).fillMaxWidth(),
                contentPadding = Theme.padding.value9,
                shadowElevation = Theme.shadow.v3
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(Theme.padding.v)
                ) {
                    val config = mediaItem.config
                    if (config != null) {
                        Text(
                            text = config.name,
                            style = Theme.typography.v6.bold,
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
                        style = Theme.typography.v6.bold,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                    for ((resource, length) in mediaItem.resources) {
                        Row(
                            modifier = Modifier.fillMaxWidth().border(Theme.border.v7, Theme.color.outline).padding(Theme.padding.value),
                            horizontalArrangement = Arrangement.spacedBy(Theme.padding.h9)
                        ) {
                            Text(
                                text = "${resource.description} (${resource.type})",
                                modifier = Modifier.weight(1f)
                            )
                            Text(text = length.toLong().fileSizeString)
                        }
                    }
                }
            }
        }
    }
}