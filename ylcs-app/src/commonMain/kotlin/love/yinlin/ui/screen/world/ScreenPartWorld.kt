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
import love.yinlin.platform.app
import love.yinlin.resources.*
import love.yinlin.ui.component.image.ClickImage
import love.yinlin.ui.component.image.ColorfulIcon
import love.yinlin.ui.component.image.MiniImage
import love.yinlin.ui.component.image.colorfulImageVector
import love.yinlin.ui.component.node.condition
import love.yinlin.ui.screen.community.BoxText
import org.jetbrains.compose.resources.DrawableResource
import kotlin.math.absoluteValue

@Stable
private enum class GameType(val title: String) {
	RANK("排位"),
	SPEED("竞速")
}

@Stable
private enum class Game(
	val title: String,
	val imgX: DrawableResource,
	val imgY: DrawableResource,
	val description: String,
	val type: GameType
) {
	AnswerQuestion(
		title = "答题",
		imgX = Res.drawable.game1x,
		imgY = Res.drawable.game1y,
		description = "简单易懂的答题, 支持选择、多选、填空类型, 内容自定义",
		type = GameType.RANK
	),
	BlockText(
		title = "网格填词",
		imgX = Res.drawable.game2x,
		imgY = Res.drawable.game2y,
		description = "在方形网格中填写缺失的字使得横竖都能构成满足条件的诗词或歌词",
		type = GameType.RANK
	),
	FlowersOrder(
		title = "寻花令",
		imgX = Res.drawable.game3x,
		imgY = Res.drawable.game3y,
		description = "在有限次数内猜测七言诗词中的某一句, 并根据上次内容与位置提示结果来修正最终答案直至完全猜对",
		type = GameType.RANK
	),
	SearchAll(
		title = "词寻",
		imgX = Res.drawable.game4x,
		imgY = Res.drawable.game4y,
		description = "根据词库提示尽可能用最短的时间将所有满足条件的内容列出",
		type = GameType.SPEED
	)
}

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
				ColorfulIcon(
					icon = colorfulImageVector(
						icon = Icons.AutoMirrored.Outlined.KeyboardArrowLeft,
						color = MaterialTheme.colorScheme.onTertiaryContainer,
						background = MaterialTheme.colorScheme.tertiaryContainer
					),
					size = ThemeValue.Size.ExtraIcon,
					onClick = {
						pagerState.requestScrollToPage((pagerState.currentPage - 1 + pagerState.pageCount) % pagerState.pageCount)
					}
				)

				val game = Game.entries[pagerState.currentPage]

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

				ColorfulIcon(
					icon = colorfulImageVector(
						icon = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
						color = MaterialTheme.colorScheme.onTertiaryContainer,
						background = MaterialTheme.colorScheme.tertiaryContainer
					),
					size = ThemeValue.Size.ExtraIcon,
					onClick = {
						pagerState.requestScrollToPage((pagerState.currentPage + 1) % pagerState.pageCount)
					}
				)
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