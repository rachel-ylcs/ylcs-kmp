package love.yinlin.ui.screen.community

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.zIndex
import love.yinlin.common.ThemeStyle
import love.yinlin.common.ThemeValue
import love.yinlin.data.rachel.profile.UserPublicProfile
import love.yinlin.extension.DateEx
import love.yinlin.platform.app
import love.yinlin.ui.component.common.UserLabel
import love.yinlin.ui.component.image.MiniIcon
import love.yinlin.ui.component.image.WebImage
import love.yinlin.ui.component.layout.EqualItem
import love.yinlin.ui.component.layout.EqualRow
import love.yinlin.ui.component.layout.EqualRowScope
import love.yinlin.ui.component.layout.Space
import love.yinlin.ui.component.node.clickableNoRipple
import kotlin.math.max

@Composable
internal fun UserBar(
	avatar: String,
	name: String,
	time: String,
	label: String,
	level: Int,
	onAvatarClick: () -> Unit
) {
	Row(
		modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
		horizontalArrangement = Arrangement.spacedBy(ThemeValue.Padding.HorizontalSpace),
	) {
		Box(modifier = Modifier.fillMaxHeight().aspectRatio(1f)) {
			WebImage(
				uri = avatar,
				key = DateEx.TodayString,
				contentScale = ContentScale.Crop,
				circle = true,
				onClick = onAvatarClick,
				modifier = Modifier.matchParentSize()
			)
		}
		Column(
			modifier = Modifier.weight(1f),
			verticalArrangement = Arrangement.spacedBy(ThemeValue.Padding.VerticalSpace)
		) {
			Text(
				text = name,
				style = MaterialTheme.typography.labelMedium,
				maxLines = 1,
				overflow = TextOverflow.Ellipsis,
				modifier = Modifier.fillMaxWidth()
			)
			Text(
				text = time,
				color = MaterialTheme.colorScheme.onSurfaceVariant,
				style = MaterialTheme.typography.bodySmall,
				maxLines = 1,
				overflow = TextOverflow.Ellipsis,
				modifier = Modifier.fillMaxWidth()
			)
		}
		UserLabel(label = label, level = level)
	}
}

@Composable
internal fun BoxText(
	text: String,
	color: Color
) {
	Box(
		modifier = Modifier.padding(ThemeValue.Padding.VerticalSpace / 2)
			.border(ThemeValue.Border.Small, color = color),
		contentAlignment = Alignment.Center
	) {
		Text(
			text = text,
			style = MaterialTheme.typography.labelMedium,
			color = color,
			modifier = Modifier.padding(ThemeValue.Padding.LittleValue)
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
		verticalArrangement = Arrangement.spacedBy(ThemeValue.Padding.VerticalSpace)
	) {
		Text(
			text = value,
			style = MaterialTheme.typography.labelLarge
		)
		Text(text = title)
	}
}

@Composable
internal fun UserProfileInfo(
	profile: UserPublicProfile,
	owner: Boolean,
	modifier: Modifier = Modifier,
	onLevelClick: () -> Unit = {},
	content: @Composable RowScope.(onLevelClick: () -> Unit) -> Unit = {}
) {
	Box(modifier = modifier) {
		Row(
			modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
			horizontalArrangement = Arrangement.spacedBy(ThemeValue.Padding.HorizontalExtraSpace),
			verticalAlignment = Alignment.CenterVertically
		) {
			Box(modifier = Modifier.fillMaxHeight().aspectRatio(1f)) {
				WebImage(
					uri = profile.avatarPath,
					key = if (owner) app.config.cacheUserAvatar else DateEx.TodayString,
					contentScale = ContentScale.Crop,
					circle = true,
					modifier = Modifier.matchParentSize().shadow(ThemeValue.Shadow.Icon, CircleShape)
				)
			}
			Column(
				modifier = Modifier.weight(1f),
				verticalArrangement = Arrangement.spacedBy(ThemeValue.Padding.LittleSpace)
			) {
				Text(
					text = profile.name,
					style = MaterialTheme.typography.labelLarge,
					maxLines = 1,
					overflow = TextOverflow.Ellipsis,
					modifier = Modifier.fillMaxWidth()
				)
				UserLabel(
					label = profile.label,
					level = profile.level,
					onClick = onLevelClick
				)
			}
			content(onLevelClick)
		}
	}
}

@Composable
internal fun UserProfileCard(
	profile: UserPublicProfile,
	owner: Boolean,
	shape: Shape = RectangleShape,
	modifier: Modifier = Modifier,
	onLevelClick: () -> Unit = {},
	onFollowClick: (Int) -> Unit = {},
	content: @Composable (RowScope.(() -> Unit) -> Unit) = {}
) {
	Surface(
		modifier = modifier,
		shape = shape,
		shadowElevation = ThemeValue.Shadow.Surface
	) {
		Column(modifier = Modifier.fillMaxWidth()) {
			WebImage(
				uri = profile.wallPath,
				key = if (owner) app.config.cacheUserWall else DateEx.TodayString,
				modifier = Modifier.fillMaxWidth().aspectRatio(1.77777f)
			)
			Column(
				modifier = Modifier.fillMaxWidth().padding(ThemeValue.Padding.ExtraValue),
				verticalArrangement = Arrangement.spacedBy(ThemeValue.Padding.VerticalExtraSpace * 1.5f)
			) {
				UserProfileInfo(
					profile = profile,
					owner = owner,
					modifier = Modifier.fillMaxWidth(),
					onLevelClick = onLevelClick,
					content = content
				)
				SelectionContainer {
					Text(
						text = profile.signature,
						style = MaterialTheme.typography.bodySmall,
						maxLines = 2,
						overflow = TextOverflow.Ellipsis,
						modifier = Modifier.fillMaxWidth()
					)
				}
				Row(
					modifier = Modifier.fillMaxWidth(),
					horizontalArrangement = Arrangement.SpaceEvenly,
					verticalAlignment = Alignment.CenterVertically
				) {
					PortraitValue(
						value = profile.level.toString(),
						title = "等级",
						modifier = Modifier.clickableNoRipple(onClick = onLevelClick)
					)
					PortraitValue(
						value = profile.coin.toString(),
						title = "银币"
					)
					PortraitValue(
						value = profile.follows.toString(),
						title = "关注",
						modifier = Modifier.clickableNoRipple { onFollowClick(FollowTabItem.FOLLOWS.ordinal) }
					)
					PortraitValue(
						value = profile.followers.toString(),
						title = "粉丝",
						modifier = Modifier.clickableNoRipple { onFollowClick(FollowTabItem.FOLLOWERS.ordinal) }
					)
				}
			}
		}
	}
}

@Stable
data class TipButtonScope(private val equalRowScope: EqualRowScope) {
	@Composable
	fun Item(text: String, icon: ImageVector, label: Int = 0, onClick: () -> Unit) {
		equalRowScope.EqualItem {
			Column(
				modifier = Modifier
					.clip(MaterialTheme.shapes.medium)
					.clickable(onClick = onClick)
					.padding(ThemeValue.Padding.Value),
				verticalArrangement = Arrangement.spacedBy(ThemeValue.Padding.VerticalSpace),
				horizontalAlignment = Alignment.CenterHorizontally
			) {
				Box {
					MiniIcon(
						icon = icon,
						modifier = Modifier.zIndex(1f)
					)
					if (label > 0) {
						val labelString = remember(label) { if (label < 10) label.toString() else "+" }
						Layout(
							modifier = Modifier.background(color = MaterialTheme.colorScheme.error, shape = CircleShape)
								.align(Alignment.TopEnd).zIndex(2f),
							measurePolicy = { measurables, constraints ->
								val textPlaceable = measurables.first().measure(constraints)
								val boxSize = max(textPlaceable.width, textPlaceable.height)
								layout(boxSize, boxSize) {
									textPlaceable.placeRelative(
										x = (boxSize - textPlaceable.width) / 2,
										y = (boxSize - textPlaceable.height) / 2
									)
								}
							},
							content = {
								Text(
									text = labelString,
									color = MaterialTheme.colorScheme.onError,
									textAlign = TextAlign.Center,
									maxLines = 1,
									overflow = TextOverflow.Clip
								)
							}
						)
					}
				}
				Text(
					text = text,
					maxLines = 1,
					overflow = TextOverflow.Ellipsis
				)
			}
		}
	}
}

@Composable
internal fun TipButtonContainer(
	title: String,
	modifier: Modifier = Modifier,
	shape: Shape = RectangleShape,
	content: @Composable TipButtonScope.() -> Unit
) {
	Surface(
		modifier = modifier,
		shape = shape,
		shadowElevation = ThemeValue.Shadow.Surface
	) {
		Column(
			modifier = Modifier.fillMaxWidth().padding(ThemeValue.Padding.EqualValue),
			verticalArrangement = Arrangement.spacedBy(ThemeValue.Padding.VerticalSpace)
		) {
			Text(
				text = title,
				style = MaterialTheme.typography.labelLarge
			)
			EqualRow(modifier = Modifier.fillMaxWidth()) {
				TipButtonScope(this).content()
			}
		}
	}
}