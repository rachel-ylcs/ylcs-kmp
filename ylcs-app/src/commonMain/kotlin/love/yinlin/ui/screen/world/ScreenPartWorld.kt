package love.yinlin.ui.screen.world

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.util.lerp
import androidx.compose.ui.zIndex
import love.yinlin.AppModel
import love.yinlin.ScreenPart
import love.yinlin.common.*
import love.yinlin.data.rachel.game.Game
import love.yinlin.data.rachel.game.GameType
import love.yinlin.data.rachel.game.imgX
import love.yinlin.data.rachel.game.imgY
import love.yinlin.platform.app
import love.yinlin.ui.component.image.ClickImage
import love.yinlin.ui.component.image.ColorfulIcon
import love.yinlin.ui.component.image.MiniImage
import love.yinlin.ui.component.image.colorfulImageVector
import love.yinlin.ui.component.node.condition
import love.yinlin.ui.screen.community.BoxText
import love.yinlin.ui.screen.world.game.*
import kotlin.math.absoluteValue

@Composable
private fun GameCard(
	game: Game,
	isPortrait: Boolean,
	modifier: Modifier = Modifier,
	onClick: () -> Unit
) {
	Column(
		modifier = modifier,
		horizontalAlignment = Alignment.CenterHorizontally,
		verticalArrangement = Arrangement.spacedBy(ThemeValue.Padding.VerticalExtraSpace)
	) {
		ClickImage(
			res = if (isPortrait) game.imgY else game.imgX,
			contentScale = ContentScale.Crop,
			modifier = Modifier
				.fillMaxWidth(fraction = 0.75f)
				.aspectRatio(1f)
				.clip(CircleShape),
			onClick = onClick
		)
		Row(
			horizontalArrangement = Arrangement.spacedBy(ThemeValue.Padding.LittleSpace),
			verticalAlignment = Alignment.CenterVertically
		) {
			Text(
				text = game.title,
				style = MaterialTheme.typography.titleLarge,
				maxLines = 1,
				overflow = TextOverflow.Ellipsis
			)
			BoxText(
				text = game.type.title,
				color = when (game.type) {
                    GameType.RANK -> MaterialTheme.colorScheme.primary
                    GameType.SPEED -> MaterialTheme.colorScheme.secondary
                }
			)
		}
		Text(
			text = game.description,
			color = MaterialTheme.colorScheme.onSurfaceVariant,
			modifier = Modifier
				.fillMaxWidth()
				.padding(
					start = ThemeValue.Padding.EqualExtraSpace,
					end = ThemeValue.Padding.EqualExtraSpace,
					bottom = ThemeValue.Padding.EqualExtraSpace
				)
				.verticalScroll(rememberScrollState())
		)
	}
}

@Stable
class ScreenPartWorld(model: AppModel) : ScreenPart(model) {
	private val pagerState = PagerState { Game.entries.size }

	private fun onGameClick(game: Game) {
		slot.tip.info("小游戏板块即将在下版本开启, 敬请期待!")
//		when (game) {
//            Game.AnswerQuestion -> navigate<ScreenGame1Hall>()
//            Game.BlockText -> navigate<ScreenGame2Hall>()
//            Game.FlowersOrder -> navigate<ScreenGame3Hall>()
//            Game.SearchAll -> navigate<ScreenGame4Hall>()
//        }
	}

	@Composable
	private fun GameBackground(
		modifier: Modifier = Modifier,
		isPortrait: Boolean,
	) {
		Crossfade(
			targetState = pagerState.currentPage,
			animationSpec = tween(durationMillis = app.config.animationSpeed),
			modifier = modifier
		) { currentPage ->
			val background = MaterialTheme.colorScheme.background
			val isDarkMode = LocalDarkMode.current
			val pageOffset = pagerState.currentPageOffsetFraction
			val game = Game.entries[currentPage]

			MiniImage(
				res = if (isPortrait) game.imgY else game.imgX,
				contentScale = ContentScale.Crop,
				alignment = Alignment.TopCenter,
				modifier = Modifier
					.fillMaxSize()
					.clipToBounds()
					.graphicsLayer {
						scaleX = lerp(1f, 1.1f, pageOffset.absoluteValue)
						scaleY = lerp(1f, 1.1f, pageOffset.absoluteValue)
						translationY = lerp(0f, -20f, pageOffset.absoluteValue)
					}
					.drawWithCache {
						val gradient = Brush.verticalGradient(
							colors = listOf(Colors.Transparent, background.copy(alpha = 0.75f)),
							startY = size.height / 3,
							endY = size.height
						)
						onDrawWithContent {
							drawContent()
							drawRect(gradient, blendMode = if (isDarkMode) BlendMode.Darken else BlendMode.Lighten)
						}
					}
			)
		}
	}

	@Composable
	private fun Portrait() {
		BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
			GameBackground(
				modifier = Modifier.fillMaxSize(),
				isPortrait = true
			)

			val padding = (maxWidth - ThemeValue.Size.CardWidth) / 2

			HorizontalPager(
				state = pagerState,
				verticalAlignment = Alignment.Bottom,
				pageSize = PageSize.Fixed(ThemeValue.Size.CardWidth),
				beyondViewportPageCount = Game.entries.size,
				pageSpacing = padding / 2,
				contentPadding = PaddingValues(horizontal = padding),
				modifier = Modifier.fillMaxSize().zIndex(2f)
			) { index ->
				val offset = pagerState.currentPage - index
				val pageOffset = offset + pagerState.currentPageOffsetFraction

				val game = Game.entries[index]

				GameCard(
					game = game,
					isPortrait = true,
					modifier = Modifier
						.padding(bottom = lerp(40.dp, 0.dp, pageOffset.absoluteValue.coerceIn(0f, 1f)))
						.fillMaxWidth()
						.aspectRatio(0.66667f)
						.scale(scaleX = 1f, scaleY = lerp(1f, 0.9f, pageOffset.absoluteValue.coerceIn(0f, 1f)))
						.clip(CircleShape)
						.condition(offset == 0) { border(width = ThemeValue.Border.Large, color = MaterialTheme.colorScheme.primary, shape = CircleShape) }
						.background(MaterialTheme.colorScheme.background)
						.padding(ThemeValue.Padding.CardValue),
					onClick = { onGameClick(game) }
				)
			}
		}
	}

	@Composable
	private fun Landscape() {
		Box(
			modifier = Modifier.fillMaxSize(),
			contentAlignment = Alignment.Center
		) {
			GameBackground(
				modifier = Modifier.fillMaxSize(),
				isPortrait = false
			)

			Row(
				modifier = Modifier.fillMaxWidth().zIndex(2f),
				horizontalArrangement = Arrangement.SpaceAround,
				verticalAlignment = Alignment.CenterVertically
			) {
				val currentPage = pagerState.currentPage
				val game = Game.entries[currentPage]

				Box(
					modifier = Modifier.size(ThemeValue.Size.ExtraIcon * 1.5f),
					contentAlignment = Alignment.Center
				) {
					if (currentPage > 0) {
						ColorfulIcon(
							icon = colorfulImageVector(
								icon = Icons.AutoMirrored.Outlined.KeyboardArrowLeft,
								color = MaterialTheme.colorScheme.onTertiaryContainer,
								background = MaterialTheme.colorScheme.tertiaryContainer
							),
							size = ThemeValue.Size.ExtraIcon,
							onClick = {
								pagerState.requestScrollToPage(currentPage - 1)
							}
						)
					}
				}

				GameCard(
					game = game,
					isPortrait = false,
					modifier = Modifier
						.width(ThemeValue.Size.CardWidth)
						.aspectRatio(0.66667f)
						.clip(CircleShape)
						.shadow(
							elevation = ThemeValue.Shadow.Card,
							shape = CircleShape,
							clip = false
						)
						.background(MaterialTheme.colorScheme.surface)
						.padding(ThemeValue.Padding.CardValue),
					onClick = { onGameClick(game) }
				)

				Box(
					modifier = Modifier.size(ThemeValue.Size.ExtraIcon * 1.5f),
					contentAlignment = Alignment.Center
				) {
					if (currentPage < pagerState.pageCount - 1) {
						ColorfulIcon(
							icon = colorfulImageVector(
								icon = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
								color = MaterialTheme.colorScheme.onTertiaryContainer,
								background = MaterialTheme.colorScheme.tertiaryContainer
							),
							size = ThemeValue.Size.ExtraIcon,
							onClick = {
								pagerState.requestScrollToPage(currentPage + 1)
							}
						)
					}
				}
			}
		}
	}

	@Composable
	override fun Content() {
		when (LocalDevice.current.type) {
			Device.Type.PORTRAIT -> Portrait()
			Device.Type.SQUARE, Device.Type.LANDSCAPE -> Landscape()
		}
	}
}