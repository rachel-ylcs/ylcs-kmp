package love.yinlin.screen.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import love.yinlin.component.NineGrid
import love.yinlin.component.RichText
import love.yinlin.component.WebImage
import love.yinlin.data.weibo.Weibo
import love.yinlin.extension.DateEx

@Composable
fun WeiboCard(
	weibo: Weibo,
	modifier: Modifier = Modifier,
	onClick: () -> Unit
) {
	ElevatedCard(onClick = onClick) {
		Column(
			modifier = modifier,
			verticalArrangement = Arrangement.spacedBy(10.dp)
		) {
			Row(
				modifier = Modifier.fillMaxWidth(),
				verticalAlignment = Alignment.CenterVertically,
				horizontalArrangement = Arrangement.spacedBy(10.dp)
			) {
				WebImage(
					uri = weibo.info.avatar,
					key = DateEx.currentDateString,
					modifier = Modifier.size(50.dp),
					circle = true
				)
				Column(
					modifier = Modifier.weight(1f),
					verticalArrangement = Arrangement.spacedBy(5.dp)
				) {
					Text(
						modifier = Modifier.fillMaxWidth(),
						text = weibo.info.name,
						color = MaterialTheme.colorScheme.primary,
						style = MaterialTheme.typography.titleMedium
					)
					Row(
						modifier = Modifier.fillMaxWidth(),
						horizontalArrangement = Arrangement.spacedBy(10.dp),
					) {
						Text(
							modifier = Modifier.weight(1f),
							text = weibo.time,
							color = MaterialTheme.colorScheme.onSurfaceVariant,
							overflow = TextOverflow.Ellipsis
						)
						Text(
							text = weibo.location,
							color = MaterialTheme.colorScheme.onSurfaceVariant,
						)
					}
				}
			}
			RichText(
				text = weibo.text,
				modifier = Modifier.fillMaxWidth(),
				overflow = TextOverflow.Ellipsis,
				onLinkClick = {
					println(it)
				},
				onTopicClick = {
					println(it)
				},
				onAtClick = {
					println(it)
				}
			)
			NineGrid(
				pics = weibo.pictures,
				modifier = Modifier.fillMaxWidth()
			)
		}
	}
}

@Composable
fun WeiboGrid(
	modifier: Modifier = Modifier,
	items: List<Weibo>,
	onClick: (Weibo) -> Unit
) {
	LazyVerticalStaggeredGrid(
		columns = StaggeredGridCells.Adaptive(300.dp),
		modifier = modifier,
		contentPadding = PaddingValues(10.dp),
		horizontalArrangement = Arrangement.spacedBy(10.dp),
		verticalItemSpacing = 10.dp
	) {
		items(
			items = items,
			key = { it.id }
		) {
			WeiboCard(
				weibo = it,
				modifier = Modifier.fillMaxWidth()
					.background(MaterialTheme.colorScheme.surface)
					.padding(10.dp)
			) {
				onClick(it)
			}
		}
	}
}