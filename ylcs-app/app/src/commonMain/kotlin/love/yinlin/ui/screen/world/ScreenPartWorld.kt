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
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Castle
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.util.lerp
import androidx.compose.ui.zIndex
import love.yinlin.AppModel
import love.yinlin.Local
import love.yinlin.ScreenPart
import love.yinlin.common.*
import love.yinlin.data.rachel.game.Game
import love.yinlin.data.rachel.game.GamePublicDetailsWithName
import love.yinlin.data.rachel.game.GameType
import love.yinlin.platform.app
import love.yinlin.ui.component.image.ClickIcon
import love.yinlin.ui.component.image.ColorfulIcon
import love.yinlin.ui.component.image.WebImage
import love.yinlin.ui.component.image.colorfulImageVector
import love.yinlin.ui.component.node.condition
import love.yinlin.ui.component.screen.FABAction
import love.yinlin.ui.screen.community.BoxText
import love.yinlin.ui.screen.world.battle.ScreenGuessLyrics
import love.yinlin.ui.screen.world.single.rhyme.ScreenRhyme
import kotlin.math.absoluteValue

@Composable
private fun GameCard(
	game: Game,
	isLandscape: Boolean,
	modifier: Modifier = Modifier,
	onClick: () -> Unit,
	content: @Composable () -> Unit
) {
	Column(
		modifier = modifier,
		horizontalAlignment = Alignment.CenterHorizontally,
		verticalArrangement = Arrangement.spacedBy(ThemeValue.Padding.VerticalExtraSpace)
	) {
		WebImage(
			uri = remember(game, isLandscape) { game.xyPath(isLandscape) },
			key = Local.VERSION,
			contentScale = ContentScale.Crop,
			circle = true,
			modifier = Modifier.fillMaxWidth(fraction = 0.75f).aspectRatio(1f),
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
                    GameType.EXPLORATION, GameType.SINGLE -> MaterialTheme.colorScheme.secondary
                    GameType.SPEED, GameType.BATTLE -> MaterialTheme.colorScheme.tertiary
                }
			)
		}
		content()
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

	var currentGame: GamePublicDetailsWithName? = null

	private fun onGameClick(game: Game) {
		when (game) {
            Game.AnswerQuestion, Game.BlockText,
            Game.FlowersOrder, Game.SearchAll,
            Game.Pictionary -> navigate(ScreenGameHall.Args(game))
            Game.GuessLyrics -> {
				val profile = app.config.userProfile
				if (profile != null) navigate(ScreenGuessLyrics.Args(profile.uid, profile.name))
				else slot.tip.warning("请先登录")
			}
            Game.Rhyme -> navigate<ScreenRhyme>()
		}
	}

	@Composable
	private fun GameBackground(
		modifier: Modifier = Modifier,
		isLandscape: Boolean,
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

			WebImage(
				uri = remember(game, isLandscape) { game.xyPath(isLandscape) },
				key = Local.VERSION,
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
	private fun ButtonLayout(game: Game, modifier: Modifier = Modifier) {
		Row(
			modifier = modifier,
			horizontalArrangement = Arrangement.spacedBy(ThemeValue.Padding.HorizontalSpace, Alignment.CenterHorizontally),
			verticalAlignment = Alignment.CenterVertically
		) {
			when (game) {
                Game.AnswerQuestion, Game.BlockText,
                Game.FlowersOrder, Game.SearchAll,
                Game.Pictionary -> {
                    ClickIcon(
                        icon = Icons.Outlined.Edit,
                        tip = "创建",
                        onClick = {
                            if (app.config.userProfile != null) navigate(ScreenCreateGame.Args(game))
                            else slot.tip.warning("请先登录")
                        }
                    )
                }
                Game.GuessLyrics, Game.Rhyme -> {}
			}
            when (game) {
                Game.AnswerQuestion, Game.BlockText,
                Game.FlowersOrder, Game.SearchAll,
                Game.Pictionary, Game.GuessLyrics -> {
                    ClickIcon(
                        icon = ExtraIcons.RewardCup,
                        tip = "排行榜",
                        onClick = {
                            navigate(ScreenGameRanking.Args(game))
                        }
                    )
                }
                Game.Rhyme -> {}
            }
		}
	}

	@Composable
	private fun Portrait() {
		BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
			GameBackground(
				modifier = Modifier.fillMaxSize(),
				isLandscape = false
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
					isLandscape = false,
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
				) {
					ButtonLayout(game = game, modifier = Modifier.fillMaxWidth())
				}
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
				isLandscape = true
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
					isLandscape = true,
					modifier = Modifier
						.width(ThemeValue.Size.CardWidth)
						.aspectRatio(0.66667f)
						.clip(CircleShape)
						.border(width = ThemeValue.Border.Large, color = MaterialTheme.colorScheme.primary, shape = CircleShape)
						.shadow(
							elevation = ThemeValue.Shadow.Card,
							shape = CircleShape,
							clip = false
						)
						.background(MaterialTheme.colorScheme.surface)
						.padding(ThemeValue.Padding.CardValue),
					onClick = { onGameClick(game) }
				) {
					ButtonLayout(game = game, modifier = Modifier.fillMaxWidth())
				}

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

	override val fabCanExpand: Boolean = true

	override val fabIcon: ImageVector = Icons.Outlined.Add

	override val fabMenus: Array<FABAction> = arrayOf(
		FABAction(Icons.Outlined.History, "我的战绩") {
			navigate<ScreenGameRecordHistory>()
		},
		FABAction(Icons.Outlined.Castle, "我的游戏") {
			navigate<ScreenGameHistory>()
		}
	)
}