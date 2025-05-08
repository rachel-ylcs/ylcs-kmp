package love.yinlin.ui.screen.music

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Comment
import androidx.compose.material.icons.automirrored.outlined.QueueMusic
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import io.ktor.utils.io.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.io.buffered
import kotlinx.io.files.SystemFileSystem
import love.yinlin.AppModel
import love.yinlin.ScreenPart
import love.yinlin.common.Colors
import love.yinlin.common.Device
import love.yinlin.common.ExtraIcons
import love.yinlin.common.LocalDevice
import love.yinlin.common.ThemeValue
import love.yinlin.data.music.MusicInfo
import love.yinlin.data.music.MusicPlayMode
import love.yinlin.extension.*
import love.yinlin.platform.Coroutines
import love.yinlin.platform.MusicFactory
import love.yinlin.platform.app
import love.yinlin.resources.*
import love.yinlin.ui.component.image.ClickIcon
import love.yinlin.ui.component.image.LocalFileImage
import love.yinlin.ui.component.input.BeautifulSlider
import love.yinlin.ui.component.input.RachelButton
import love.yinlin.ui.component.layout.*
import love.yinlin.ui.component.lyrics.LyricsLrc
import love.yinlin.ui.component.screen.FloatingSheet
import love.yinlin.ui.screen.common.ScreenVideo
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import kotlin.math.abs

@Composable
private fun PlayingMusicStatusCard(
	musicInfo: MusicInfo,
	isCurrent: Boolean,
	onClick: () -> Unit,
	modifier: Modifier = Modifier
) {
	Row(
		modifier = modifier.clickable {
			if (!isCurrent) onClick()
		}.padding(ThemeValue.Padding.ExtraValue),
		horizontalArrangement = Arrangement.spacedBy(ThemeValue.Padding.HorizontalSpace),
		verticalAlignment = Alignment.CenterVertically
	) {
		Text(
			text = musicInfo.name,
			style = MaterialTheme.typography.labelMedium,
			color = if (isCurrent) MaterialTheme.colorScheme.primary else Colors.Unspecified,
			maxLines = 1,
			overflow = TextOverflow.MiddleEllipsis,
			modifier = Modifier.weight(1f)
		)
		Text(
			text = musicInfo.singer,
			style = MaterialTheme.typography.bodySmall,
			maxLines = 1,
			overflow = TextOverflow.MiddleEllipsis,
			color = MaterialTheme.colorScheme.onSurfaceVariant
		)
	}
}

@Stable
class ScreenPartMusic(model: AppModel) : ScreenPart(model) {
	private val factory = app.musicFactory

	private var isAnimationBackground by mutableStateOf(false)
	private val blurState = HazeState()

	private var lyrics = LyricsLrc()

	private var sleepJob: Job? by mutableStateOf(null)
	private var sleepRemainSeconds: Int by mutableIntStateOf(0)

	private val currentPlaylistSheet = FloatingSheet()
	private val sleepModeSheet = FloatingSheet()

	@Composable
	private fun Modifier.hazeBlur(radius: Dp): Modifier {
		val isForeground = rememberOffScreenState()
		return if (isForeground) hazeEffect(
			state = blurState,
			style = HazeStyle(
				blurRadius = radius,
				backgroundColor = Colors.Dark,
				tint = null,
			)
		) else this
	}

	@Composable
	private fun MusicBackground(
		alpha: Float = 1f,
		modifier: Modifier = Modifier
	) {
		Box(modifier = modifier) {
			factory.currentMusic?.let { musicInfo ->
				LocalFileImage(
					path = { if (isAnimationBackground) musicInfo.AnimationPath else musicInfo.backgroundPath },
					musicInfo, isAnimationBackground,
					contentScale = ContentScale.Crop,
					alpha = alpha,
					modifier = Modifier.fillMaxSize()
				)
			}
		}
	}

	@Composable
	private fun ToolLayout(modifier: Modifier = Modifier) {
		SplitActionLayout(
			modifier = modifier,
			left = {
				Action(
					icon = Icons.Outlined.LibraryMusic,
					color = Colors.White
				) {
					navigate<ScreenMusicLibrary>()
				}
				Action(
					icon = Icons.AutoMirrored.Outlined.QueueMusic,
					color = Colors.White
				) {
					navigate<ScreenPlaylistLibrary>()
				}
				Action(
					icon = Icons.Outlined.Lyrics,
					color = Colors.White
				) {

				}
			},
			right = {
				Action(
					icon = Icons.Outlined.AlarmOn,
					color = Colors.White
				) {
					sleepModeSheet.open()
				}
			}
		)
	}

	@Composable
	private fun MusicRecord(
		musicInfo: MusicInfo,
		modifier: Modifier = Modifier
	) {
		var lastDegree by rememberState { 0f }
		val animation by rememberState { Animatable(0f) }
		val isForeground = rememberOffScreenState()

		LaunchedEffect(factory.isPlaying, isForeground) {
			if (factory.isPlaying && isForeground) {
				animation.animateTo(
					targetValue = 360f + lastDegree,
					animationSpec = infiniteRepeatable(
						animation = tween(durationMillis = 15000, easing = LinearEasing),
						repeatMode = RepeatMode.Restart
					)
				) {
					lastDegree = this.value
				}
			}
			else {
				animation.snapTo(lastDegree)
				animation.stop()
			}
		}

		LocalFileImage(
			path = { musicInfo.recordPath },
			musicInfo,
			contentScale = ContentScale.Crop,
			circle = true,
			modifier = modifier.rotate(degrees = animation.value)
		)
	}

	@Composable
	private fun MusicRecordLayout(
		offset: Dp,
		musicInfo: MusicInfo?,
		modifier: Modifier = Modifier
	) {
		OffsetLayout(y = offset) {
			Box(
				modifier = modifier,
				contentAlignment = Alignment.Center
			) {
				Image(
					painter = painterResource(Res.drawable.img_music_record),
					contentDescription = null,
					modifier = Modifier.fillMaxSize().zIndex(1f)
				)
				musicInfo?.let {
					MusicRecord(
						musicInfo = it,
						modifier = Modifier.fillMaxSize(fraction = 0.75f).zIndex(2f)
					)
				}
			}
		}
	}

	@Composable
	private fun PortraitMusicInfoLayout(modifier: Modifier = Modifier) {
		val musicInfo = factory.currentMusic
		Row(
			modifier = modifier,
			horizontalArrangement = Arrangement.spacedBy(ThemeValue.Padding.HorizontalExtraSpace)
		) {
			MusicRecordLayout(
				offset = -ThemeValue.Size.LargeImage / 2,
				musicInfo = musicInfo,
				modifier = Modifier.size(ThemeValue.Size.LargeImage)
					.shadow(elevation = ThemeValue.Shadow.Icon, clip = false, shape = CircleShape)
			)
			Column(
				modifier = Modifier.weight(1f),
				verticalArrangement = Arrangement.spacedBy(ThemeValue.Padding.VerticalSpace)
			) {
				Text(
					text = musicInfo?.name ?: stringResource(Res.string.no_audio_source),
					color = MaterialTheme.colorScheme.primary,
					style = MaterialTheme.typography.titleLarge,
					textAlign = TextAlign.Center,
					maxLines = 1,
					overflow = TextOverflow.Ellipsis
				)
				Text(
					text = musicInfo?.singer ?: stringResource(Res.string.unknown_singer),
					color = Colors.White,
					style = MaterialTheme.typography.bodyMedium,
					textAlign = TextAlign.Center,
					maxLines = 1,
					overflow = TextOverflow.Ellipsis
				)
			}
		}
	}

	@Composable
	private fun LandscapeMusicInfoLayout(modifier: Modifier = Modifier) {
		val musicInfo = factory.currentMusic
		Column(
			modifier = modifier,
			horizontalAlignment = Alignment.CenterHorizontally,
			verticalArrangement = Arrangement.spacedBy(ThemeValue.Padding.VerticalExtraSpace)
		) {
			Text(
				text = musicInfo?.name ?: stringResource(Res.string.no_audio_source),
				color = MaterialTheme.colorScheme.primary,
				style = MaterialTheme.typography.displayMedium,
				textAlign = TextAlign.Center,
				maxLines = 2,
				overflow = TextOverflow.Ellipsis
			)
			Text(
				text = musicInfo?.singer ?: stringResource(Res.string.unknown_singer),
				color = Colors.White,
				style = MaterialTheme.typography.headlineSmall,
				textAlign = TextAlign.Center,
				maxLines = 1,
				overflow = TextOverflow.Ellipsis
			)
		}
	}

	@Composable
	private fun MusicProgressText(
		currentTime: Long,
		duration: Long,
		modifier: Modifier = Modifier
	) {
		Row(
			modifier = modifier,
			horizontalArrangement = Arrangement.spacedBy(ThemeValue.Padding.HorizontalExtraSpace)
		) {
			Text(
				text = remember(currentTime) { currentTime.timeString },
				color = Colors.White,
				textAlign = TextAlign.Start,
				modifier = Modifier.weight(1f)
			)
			Text(
				text = remember(duration) { duration.timeString },
				color = Colors.White,
				textAlign = TextAlign.End,
				modifier = Modifier.weight(1f)
			)
		}
	}

	@Composable
	private fun MusicChorus(
		duration: Long,
		chorus: List<Long>,
		modifier: Modifier = Modifier
	) {
		Box(modifier = modifier) {
			for (hotpot in chorus) {
				Box(modifier = Modifier
					.align(BiasAlignment(
						horizontalBias = hotpot / duration.toFloat() * 2 - 1,
						verticalBias = 0f
					))
					.width((ThemeValue.Size.ProgressHeight + ThemeValue.Padding.LittleSpace) * 2)
					.height(ThemeValue.Size.ProgressHeight * 2)
					.clickableNoRipple {
						launch {
							factory.seekTo(hotpot)
							if (!factory.isPlaying) factory.play()
						}
					}
					.padding(horizontal = ThemeValue.Padding.LittleSpace)
					.shadow(elevation = ThemeValue.Shadow.Item, shape = CircleShape)
					.background(
						color = Colors.White.copy(alpha = 0.8f),
						shape = CircleShape
					)
				)
			}
		}
	}

	@Composable
	private fun MusicProgressBar(
		currentTime: Long,
		duration: Long,
		chorus: List<Long>?,
		modifier: Modifier = Modifier
	) {
		BeautifulSlider(
			value = if (duration == 0L) 0f else currentTime / duration.toFloat(),
			height = ThemeValue.Size.ProgressHeight,
			showThumb = false,
			onValueChangeFinished = {
				launch {
					factory.seekTo((it * duration).toLong())
					if (!factory.isPlaying) factory.play()
				}
			},
			modifier = modifier
		) {
			if (chorus != null && duration != 0L) {
				MusicChorus(
					chorus = chorus,
					duration = duration,
					modifier = Modifier.fillMaxSize()
				)
			}
		}
	}

	@Composable
	private fun MusicProgressLayout(modifier: Modifier = Modifier) {
		var currentTime by rememberState(factory.currentDuration) { 0L }

		LaunchedEffect(factory.currentPosition) {
			val position = factory.currentPosition
			if (abs(position - currentTime) > 1000L - MusicFactory.UPDATE_INTERVAL) currentTime = position
		}

		Column(
			modifier = modifier,
			verticalArrangement = Arrangement.spacedBy(ThemeValue.Padding.LittleSpace)
		) {
			MusicProgressText(
				currentTime = currentTime,
				duration = factory.currentDuration,
				modifier = Modifier.fillMaxWidth()
			)

			MusicProgressBar(
				currentTime = currentTime,
				duration = factory.currentDuration,
				chorus = factory.currentMusic?.chorus,
				modifier = Modifier.fillMaxWidth().height(ThemeValue.Size.ProgressHeight * 2)
			)
		}
	}

	@Composable
	private fun MusicControlLayout(modifier: Modifier = Modifier) {
		EqualRow(modifier = modifier) {
			EqualItem {
				ClickIcon(
					icon = when (factory.playMode) {
                        MusicPlayMode.ORDER -> ExtraIcons.OrderMode
                        MusicPlayMode.LOOP -> ExtraIcons.LoopMode
                        MusicPlayMode.RANDOM -> ExtraIcons.ShuffleMode
                    },
					color = Colors.White,
					onClick = {
						launch { factory.switchPlayMode() }
					}
				)
			}
			EqualItem {
				ClickIcon(
					icon = ExtraIcons.GotoPrevious,
					color = Colors.White,
					onClick = {
						launch { factory.gotoPrevious() }
					}
				)
			}
			EqualItem {
				ClickIcon(
					icon = if (factory.isPlaying) ExtraIcons.Pause else ExtraIcons.Play,
					color = Colors.White,
					onClick = {
						launch {
							if (factory.isPlaying) factory.pause()
							else factory.play()
						}
					}
				)
			}
			EqualItem {
				ClickIcon(
					icon = ExtraIcons.GotoNext,
					color = Colors.White,
					onClick = {
						launch { factory.gotoNext() }
					}
				)
			}
			EqualItem {
				ClickIcon(
					icon = ExtraIcons.Playlist,
					color = Colors.White,
					onClick = {
						if (factory.isReady) currentPlaylistSheet.open()
					}
				)
			}
		}
	}

	@Composable
	private fun MusicToolLayout(modifier: Modifier = Modifier) {
		EqualRow(modifier = modifier) {
			EqualItem {
				var hasAnimation by rememberState { false }

				LaunchedEffect(factory.currentMusic) {
					hasAnimation = factory.currentMusic?.AnimationPath?.let { SystemFileSystem.metadataOrNull(it) }?.isRegularFile == true
					if (isAnimationBackground && !hasAnimation) isAnimationBackground = false
				}

				ClickIcon(
					icon = Icons.Outlined.GifBox,
					color = if (isAnimationBackground) MaterialTheme.colorScheme.primary else Colors.White,
					enabled = hasAnimation,
					onClick = { isAnimationBackground = !isAnimationBackground }
				)
			}
			EqualItem {
				var hasVideo by rememberState { false }

				LaunchedEffect(factory.currentMusic) {
					hasVideo = factory.currentMusic?.videoPath?.let { SystemFileSystem.metadataOrNull(it) }?.isRegularFile == true
				}

				ClickIcon(
					icon = Icons.Outlined.MusicVideo,
					color = Colors.White,
					enabled = hasVideo,
					onClick = {
						if (hasVideo) launch {
							factory.pause()
							navigate(ScreenVideo.Args(factory.currentMusic?.videoPath.toString()))
						}
					}
				)
			}
			EqualItem {
				ClickIcon(
					icon = ExtraIcons.ShowLyrics,
					color = Colors.White,
					onClick = {

					}
				)
			}
			EqualItem {
				ClickIcon(
					icon = Icons.AutoMirrored.Outlined.Comment,
					color = Colors.White,
					onClick = {

					}
				)
			}
		}
	}

	@Composable
	private fun LyricsLayout(modifier: Modifier = Modifier) {
		LaunchedEffect(factory.currentMusic) {
			val musicInfo = factory.currentMusic

			lyrics.reset()
			musicInfo?.lyricsPath?.let { path ->
				try {
					Coroutines.io {
						SystemFileSystem.source(path).buffered().use { source ->
							lyrics.parseLrcString(source.readText())
						}
					}
				}
				catch (_: Throwable) { }
			}

			if (musicInfo == null) exitSleepMode()
		}

		LaunchedEffect(factory.currentPosition) {
			val newLyricsText = lyrics.updateIndex(factory.currentPosition)
            factory.floatingLyrics?.updateLyrics(newLyricsText)
		}

		Box(modifier = modifier) {
			lyrics.Content(
				modifier = Modifier.fillMaxSize(),
				onLyricsClick = {
					launch {
						factory.seekTo(it)
					}
				}
			)
		}
	}

	private fun startSleepMode(seconds: Int) {
		exitSleepMode()
		sleepJob = launch {
			sleepRemainSeconds = seconds
			repeat(seconds) {
				delay(1000L)
				sleepRemainSeconds -= 1
			}
			app.musicFactory.stop()
			sleepJob = null
		}
	}

	private fun exitSleepMode() {
		sleepJob?.cancel()
		sleepJob = null
	}

	@Composable
	private fun Portrait() {
		Box(modifier = Modifier.fillMaxSize()) {
			MusicBackground(
				alpha = 0.7f,
				modifier = Modifier.fillMaxSize()
					.background(Colors.Black)
					.hazeSource(state = blurState)
					.zIndex(1f)
			)
			Column(modifier = Modifier.fillMaxSize().zIndex(2f)) {
				ToolLayout(modifier = Modifier
					.fillMaxWidth()
					.padding(ThemeValue.Padding.EqualValue)
					.clip(MaterialTheme.shapes.large)
					.hazeBlur(15.dp)
					.padding(ThemeValue.Padding.Value)
				)
				LyricsLayout(modifier = Modifier
					.padding(
						start = ThemeValue.Padding.HorizontalSpace,
						end = ThemeValue.Padding.HorizontalSpace,
						bottom = ThemeValue.Size.LargeImage / 2
					)
					.fillMaxWidth()
					.weight(1f)
				)
				Column(modifier = Modifier.fillMaxWidth()
					.hazeBlur(10.dp)
					.padding(ThemeValue.Padding.EqualValue)
				) {
					PortraitMusicInfoLayout(modifier = Modifier.fillMaxWidth().padding(ThemeValue.Padding.Value))
					MusicProgressLayout(modifier = Modifier.fillMaxWidth().padding(ThemeValue.Padding.Value))
					MusicControlLayout(modifier = Modifier.fillMaxWidth().padding(ThemeValue.Padding.Value))
					MusicToolLayout(modifier = Modifier.fillMaxWidth().padding(ThemeValue.Padding.Value))
				}
			}
		}
	}

	@Composable
	private fun Square() {
		Row(modifier = Modifier.fillMaxSize().background(Colors.Black)) {
			Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
				MusicBackground(
					alpha = 0.3f,
					modifier = Modifier.fillMaxSize()
						.hazeSource(state = blurState)
						.zIndex(1f)
				)
				Column(
					modifier = Modifier.fillMaxSize().zIndex(2f),
					verticalArrangement = Arrangement.spacedBy(ThemeValue.Padding.VerticalSpace)
				) {
					ToolLayout(modifier = Modifier
						.fillMaxWidth()
						.padding(ThemeValue.Padding.EqualValue)
						.clip(MaterialTheme.shapes.large)
						.hazeBlur(30.dp)
						.padding(ThemeValue.Padding.Value)
					)
					Box(
						modifier = Modifier.padding(ThemeValue.Padding.EqualExtraValue)
							.fillMaxWidth().weight(1f),
						contentAlignment = Alignment.Center
					) {
						val musicInfo = factory.currentMusic
						if (musicInfo != null) {
							MusicRecordLayout(
								offset = ThemeValue.Padding.ZeroSpace,
								musicInfo = musicInfo,
								modifier = Modifier.fillMaxHeight().aspectRatio(1f)
									.shadow(elevation = ThemeValue.Shadow.Icon, clip = false, shape = CircleShape)
							)
						}
					}
					Column(modifier = Modifier
						.fillMaxWidth()
						.hazeBlur(20.dp)
						.padding(ThemeValue.Padding.ExtraValue)
					) {
						MusicProgressLayout(modifier = Modifier.fillMaxWidth().padding(ThemeValue.Padding.Value))
						MusicControlLayout(modifier = Modifier.fillMaxWidth().padding(ThemeValue.Padding.Value))
						MusicToolLayout(modifier = Modifier.fillMaxWidth().padding(ThemeValue.Padding.Value))
					}
				}
			}
			Box(
				modifier = Modifier.fillMaxHeight().aspectRatio(0.6f),
				contentAlignment = Alignment.Center
			) {
				MusicBackground(
					alpha = 0.7f,
					modifier = Modifier.fillMaxSize()
				)
				LyricsLayout(modifier = Modifier.fillMaxWidth().fillMaxHeight(fraction = 0.7f))
			}
		}
	}

	@Composable
	private fun Landscape() {
		Row(modifier = Modifier.fillMaxSize().background(Colors.Black)) {
			Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
				MusicBackground(
					alpha = 0.3f,
					modifier = Modifier.fillMaxSize()
						.hazeSource(state = blurState)
						.zIndex(1f)
				)
				Column(
					modifier = Modifier.fillMaxSize().zIndex(2f),
					verticalArrangement = Arrangement.spacedBy(ThemeValue.Padding.VerticalSpace)
				) {
					ToolLayout(modifier = Modifier
						.fillMaxWidth()
						.padding(ThemeValue.Padding.EqualValue)
						.clip(MaterialTheme.shapes.large)
						.hazeBlur(30.dp)
						.padding(ThemeValue.Padding.Value)
					)
					Box(
						modifier = Modifier.padding(ThemeValue.Padding.EqualExtraValue)
							.fillMaxWidth().weight(1f),
						contentAlignment = Alignment.Center
					) {
						val musicInfo = factory.currentMusic
						if (musicInfo != null) {
							MusicRecordLayout(
								offset = ThemeValue.Padding.ZeroSpace,
								musicInfo = musicInfo,
								modifier = Modifier.fillMaxHeight().aspectRatio(1f)
									.shadow(elevation = ThemeValue.Shadow.Icon, clip = false, shape = CircleShape)
							)
						}
					}
					Row(
						modifier = Modifier
							.fillMaxWidth()
							.height(IntrinsicSize.Min)
							.hazeBlur(20.dp)
							.padding(ThemeValue.Padding.ExtraValue),
						horizontalArrangement = Arrangement.spacedBy(ThemeValue.Padding.HorizontalExtraSpace),
						verticalAlignment = Alignment.CenterVertically
					) {
						Box(
							modifier = Modifier.width(ThemeValue.Size.CellWidth * 0.8f).fillMaxHeight(),
							contentAlignment = Alignment.Center
						) {
							LandscapeMusicInfoLayout(modifier = Modifier.fillMaxWidth())
						}
						Column(modifier = Modifier.weight(1f)) {
							MusicProgressLayout(modifier = Modifier.fillMaxWidth().padding(ThemeValue.Padding.Value))
							MusicControlLayout(modifier = Modifier.fillMaxWidth().padding(ThemeValue.Padding.Value))
							MusicToolLayout(modifier = Modifier.fillMaxWidth().padding(ThemeValue.Padding.Value))
						}
					}
				}
			}
			Box(
				modifier = Modifier.fillMaxHeight().aspectRatio(0.65f),
				contentAlignment = Alignment.Center
			) {
				MusicBackground(
					alpha = 0.7f,
					modifier = Modifier.fillMaxSize()
				)
				LyricsLayout(modifier = Modifier.fillMaxWidth().fillMaxHeight(fraction = 0.7f))
			}
		}
	}

	@Composable
	override fun Content() {
		when (LocalDevice.current.type) {
			Device.Type.PORTRAIT -> Portrait()
			Device.Type.SQUARE -> Square()
			Device.Type.LANDSCAPE -> Landscape()
		}

		LaunchedEffect(factory.error) {
			factory.error?.let { slot.tip.error(it.message) }
		}
	}

	@OptIn(ExperimentalMaterial3Api::class)
    @Composable
	override fun Floating() {
		sleepModeSheet.Land {
			val state = rememberTimePickerState(is24Hour = true)

			Column(
				modifier = Modifier.fillMaxWidth().padding(ThemeValue.Padding.EqualValue),
				horizontalAlignment = Alignment.CenterHorizontally,
				verticalArrangement = Arrangement.spacedBy(ThemeValue.Padding.VerticalSpace)
			) {
				SplitLayout(
					modifier = Modifier.fillMaxWidth().padding(horizontal = ThemeValue.Padding.HorizontalSpace),
					left = {
						Text(
							text = "睡眠模式",
							style = MaterialTheme.typography.titleLarge,
							color = MaterialTheme.colorScheme.primary
						)
					},
					right = {
						RachelButton(
							text = if (sleepJob == null) "启动" else "停止",
							icon = if (sleepJob == null) Icons.Outlined.AlarmOn else Icons.Outlined.AlarmOff,
							onClick = {
								if (factory.isReady) {
									if (sleepJob == null) {
										val time = state.hour * 3600 + state.minute * 60
										if (time > 0) startSleepMode(time)
										else slot.tip.warning("未设定时间")
									}
									else exitSleepMode()
								}
								else slot.tip.warning("播放器未开启")
							}
						)
					}
				)
				if (sleepJob == null) {
					TimeInput(
						state = state,
						colors = TimePickerDefaults.colors().copy(
							containerColor = MaterialTheme.colorScheme.surface,
							timeSelectorSelectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
							timeSelectorUnselectedContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
							timeSelectorSelectedContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
							timeSelectorUnselectedContentColor = MaterialTheme.colorScheme.onTertiaryContainer
						)
					)
				}
				else {
					Text(
						text = remember(sleepRemainSeconds) { (sleepRemainSeconds * 1000L).timeString },
						style = MaterialTheme.typography.displayMedium,
						color = MaterialTheme.colorScheme.secondary
					)
					Space()
				}
			}
		}
		currentPlaylistSheet.Land {
			val isEmptyList by rememberDerivedState { factory.musicList.isEmpty() }

			LaunchedEffect(isEmptyList) {
				if (isEmptyList) currentPlaylistSheet.close()
			}

			Column(modifier = Modifier.fillMaxSize()) {
				Row(
					modifier = Modifier.fillMaxWidth().padding(ThemeValue.Padding.Value),
					horizontalArrangement = Arrangement.spacedBy(ThemeValue.Padding.HorizontalExtraSpace),
					verticalAlignment = Alignment.CenterVertically
				) {
					Text(
						text = factory.currentPlaylist?.name ?: "",
						style = MaterialTheme.typography.titleLarge,
						color = MaterialTheme.colorScheme.primary,
						modifier = Modifier.weight(1f)
					)
					ClickIcon(
						icon = Icons.Outlined.StopCircle,
						onClick = {
							currentPlaylistSheet.close()
							launch { factory.stop() }
						}
					)
				}

				HorizontalDivider(modifier = Modifier.padding(ThemeValue.Padding.EqualExtraValue))

				val currentIndex by rememberDerivedState { factory.musicList.indexOf(factory.currentMusic) }
				LazyColumn(
					modifier = Modifier.fillMaxWidth().weight(1f),
					state = rememberLazyListState(if (currentIndex != -1) currentIndex else 0)
				) {
					itemsIndexed(
						items = factory.musicList,
						key = { _, musicInfo -> musicInfo.id }
					) { index, musicInfo ->
						PlayingMusicStatusCard(
							musicInfo = musicInfo,
							isCurrent = index == currentIndex,
							onClick = {
								currentPlaylistSheet.close()
								launch { factory.gotoIndex(index) }
							},
							modifier = Modifier.fillMaxWidth()
						)
					}
				}
			}
		}
	}
}