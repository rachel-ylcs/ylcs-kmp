package love.yinlin.ui.screen.community

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import love.yinlin.data.rachel.UserPublicProfile
import love.yinlin.extension.DateEx
import love.yinlin.platform.app
import love.yinlin.ui.component.common.UserLabel
import love.yinlin.ui.component.image.MiniIcon
import love.yinlin.ui.component.image.WebImage
import love.yinlin.ui.component.layout.OffsetLayout

@Composable
internal fun BoxText(
	text: String,
	color: Color
) {
	Box(
		modifier = Modifier.padding(vertical = 3.dp).border(1.dp, color = color),
		contentAlignment = Alignment.Center
	) {
		Text(
			text = text,
			style = MaterialTheme.typography.labelMedium,
			color = color,
			modifier = Modifier.padding(horizontal = 3.dp, vertical = 2.dp)
		)
	}
}

@Composable
internal fun PortraitValue(
	value: String,
	title: String,
	modifier: Modifier = Modifier
) {
	Column(
		modifier = modifier,
		horizontalAlignment = Alignment.CenterHorizontally,
		verticalArrangement = Arrangement.spacedBy(5.dp)
	) {
		Text(
			text = value,
			style = MaterialTheme.typography.titleLarge
		)
		Text(
			text = title
		)
	}
}

@Composable
internal fun ColumnScope.PortraitUserProfileCard(
	profile: UserPublicProfile,
	owner: Boolean,
	toolbar: @Composable RowScope.() -> Unit = {}
) {
	WebImage(
		uri = profile.wallPath,
		key = if (owner) app.config.cacheUserWall else DateEx.TodayString,
		modifier = Modifier.fillMaxWidth().aspectRatio(1.77777f)
	)
	Column(
		modifier = Modifier.fillMaxWidth()
			.shadow(elevation = 5.dp, clip = false)
			.background(MaterialTheme.colorScheme.surface)
			.padding(10.dp),
		verticalArrangement = Arrangement.spacedBy(10.dp)
	) {
		Row(
			modifier = Modifier.fillMaxWidth(),
			horizontalArrangement = Arrangement.spacedBy(10.dp)
		) {
			OffsetLayout(y = (-46).dp) {
				WebImage(
					uri = profile.avatarPath,
					key = if (owner) app.config.cacheUserAvatar else DateEx.TodayString,
					contentScale = ContentScale.Crop,
					circle = true,
					modifier = Modifier.size(72.dp).shadow(5.dp, CircleShape)
				)
			}
			Text(
				text = profile.name,
				style = MaterialTheme.typography.titleLarge,
				maxLines = 1,
				overflow = TextOverflow.Ellipsis,
				modifier = Modifier.weight(1f).padding(horizontal = 10.dp)
			)
			toolbar()
		}
		Row(
			modifier = Modifier.fillMaxWidth(),
			horizontalArrangement = Arrangement.spacedBy(10.dp),
			verticalAlignment = Alignment.CenterVertically
		) {
			UserLabel(
				label = profile.label,
				level = profile.level
			)
			Row(
				modifier = Modifier.weight(1f),
				horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End),
				verticalAlignment = Alignment.CenterVertically
			) {
				PortraitValue(
					value = profile.level.toString(),
					title = "等级"
				)
				PortraitValue(
					value = profile.coin.toString(),
					title = "银币"
				)
			}
		}
		Text(
			text = profile.signature,
			maxLines = 2,
			overflow = TextOverflow.Ellipsis,
			modifier = Modifier.fillMaxWidth()
		)
	}
}

@Composable
internal fun LandscapeUserProfileCard(
	profile: UserPublicProfile,
	owner: Boolean,
	modifier: Modifier = Modifier
) {
	Surface(
		modifier = modifier,
		shape = MaterialTheme.shapes.large,
		shadowElevation = 5.dp
	) {
		Column(
			modifier = Modifier.fillMaxWidth(),
			verticalArrangement = Arrangement.spacedBy(10.dp)
		) {
			WebImage(
				uri = profile.wallPath,
				key = if (owner) app.config.cacheUserWall else DateEx.TodayString,
				modifier = Modifier.fillMaxWidth().aspectRatio(1.77777f)
			)
			Row(
				modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min).padding(horizontal = 10.dp),
				horizontalArrangement = Arrangement.spacedBy(15.dp)
			) {
				Box(modifier = Modifier.fillMaxHeight().aspectRatio(1f)) {
					WebImage(
						uri = profile.avatarPath,
						key = if (owner) app.config.cacheUserAvatar else DateEx.TodayString,
						contentScale = ContentScale.Crop,
						circle = true,
						modifier = Modifier.matchParentSize().shadow(5.dp, CircleShape)
					)
				}
				Column(
					modifier = Modifier.weight(1f),
					verticalArrangement = Arrangement.spacedBy(5.dp)
				) {
					Text(
						text = profile.name,
						style = MaterialTheme.typography.titleLarge,
						maxLines = 1,
						overflow = TextOverflow.Ellipsis,
						modifier = Modifier.fillMaxWidth()
					)
					UserLabel(
						label = profile.label,
						level = profile.level
					)
				}
				Row(
					horizontalArrangement = Arrangement.spacedBy(10.dp),
					verticalAlignment = Alignment.CenterVertically
				) {
					PortraitValue(
						value = profile.level.toString(),
						title = "等级"
					)
					PortraitValue(
						value = profile.coin.toString(),
						title = "银币"
					)
				}
			}
			Text(
				text = profile.signature,
				maxLines = 2,
				overflow = TextOverflow.Ellipsis,
				modifier = Modifier.fillMaxWidth().padding(start = 10.dp, end = 10.dp, bottom = 10.dp)
			)
		}
	}
}

@Stable
internal data class TipButtonInfo(
	val text: String,
	val icon: ImageVector,
	val onClick: () -> Unit
)

@Composable
internal fun TipButton(
	info: TipButtonInfo,
	modifier: Modifier = Modifier
) {
	Column(
		modifier = modifier.clickable(onClick = info.onClick).padding(3.dp),
		verticalArrangement = Arrangement.spacedBy(3.dp),
		horizontalAlignment = Alignment.CenterHorizontally
	) {
		MiniIcon(
			imageVector = info.icon,
			color = MaterialTheme.colorScheme.onSurface
		)
		Text(
			text = info.text,
			color = MaterialTheme.colorScheme.onSurface
		)
	}
}