package love.yinlin.ui.screen.community

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import love.yinlin.common.ThemeValue
import love.yinlin.data.rachel.profile.UserPublicProfile
import love.yinlin.extension.DateEx
import love.yinlin.platform.app
import love.yinlin.ui.component.common.UserLabel
import love.yinlin.ui.component.image.MiniIcon
import love.yinlin.ui.component.image.WebImage
import love.yinlin.ui.component.layout.EqualRow
import love.yinlin.ui.component.layout.EqualRowScope
import love.yinlin.ui.component.layout.EqualItem

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
internal fun UserProfileCard(
	profile: UserPublicProfile,
	owner: Boolean,
	shape: Shape = RectangleShape,
	modifier: Modifier = Modifier
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
				verticalArrangement = Arrangement.spacedBy(ThemeValue.Padding.VerticalExtraSpace)
			) {
				Row(
					modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
					horizontalArrangement = Arrangement.spacedBy(ThemeValue.Padding.HorizontalExtraSpace)
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
						verticalArrangement = Arrangement.spacedBy(ThemeValue.Padding.VerticalSpace)
					) {
						Text(
							text = profile.name,
							style = MaterialTheme.typography.labelLarge,
							maxLines = 1,
							overflow = TextOverflow.Ellipsis,
							modifier = Modifier.fillMaxWidth()
						)
						UserLabel(label = profile.label, level = profile.level)
					}
					Row(
						horizontalArrangement = Arrangement.spacedBy(ThemeValue.Padding.HorizontalSpace),
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
				SelectionContainer {
					Text(
						text = profile.signature,
						style = MaterialTheme.typography.bodySmall,
						maxLines = 2,
						overflow = TextOverflow.Ellipsis,
						modifier = Modifier.fillMaxWidth()
					)
				}
			}
		}
	}
}

@Stable
data class TipButtonScope(private val equalRowScope: EqualRowScope) {
	@Composable
	fun Item(text: String, icon: ImageVector, onClick: () -> Unit) {
		equalRowScope.EqualItem {
			Column(
				modifier = Modifier
					.clip(MaterialTheme.shapes.medium)
					.clickable(onClick = onClick)
					.padding(ThemeValue.Padding.Value),
				verticalArrangement = Arrangement.spacedBy(ThemeValue.Padding.VerticalSpace),
				horizontalAlignment = Alignment.CenterHorizontally
			) {
				MiniIcon(icon = icon)
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