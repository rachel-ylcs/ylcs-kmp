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
import love.yinlin.api.API
import love.yinlin.api.ClientAPI
import love.yinlin.common.Colors
import love.yinlin.common.Device
import love.yinlin.common.ExtraIcons
import love.yinlin.common.LocalDevice
import love.yinlin.common.LocalImmersivePadding
import love.yinlin.common.ThemeValue
import love.yinlin.data.Data
import love.yinlin.data.music.MusicInfo
import love.yinlin.data.music.MusicPlayMode
import love.yinlin.extension.*
import love.yinlin.platform.Coroutines
import love.yinlin.platform.MusicFactory
import love.yinlin.platform.app
import love.yinlin.resources.*
import love.yinlin.ui.component.image.ClickIcon
import love.yinlin.ui.component.image.LocalFileImage
import love.yinlin.ui.component.input.ProgressSlider
import love.yinlin.ui.component.input.RachelButton
import love.yinlin.ui.component.layout.*
import love.yinlin.ui.component.lyrics.LyricsLrc
import love.yinlin.ui.component.node.clickableNoRipple
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
	SplitLayout(
		modifier = modifier.clickable {
			if (!isCurrent) onClick()
		}.padding(ThemeValue.Padding.ExtraValue),
		horizontalArrangement = ThemeValue.Padding.HorizontalSpace,
		aspectRatio = 2f,
		left = {
			Text(
				text = musicInfo.name,
				style = MaterialTheme.typography.labelMedium,
				color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground,
				maxLines = 1,
				overflow = TextOverflow.MiddleEllipsis
			)
		},
		right = {
			Text(
				text = musicInfo.singer,
				style = MaterialTheme.typography.bodySmall,
				maxLines = 1,
				overflow = TextOverflow.MiddleEllipsis,
				color = MaterialTheme.colorScheme.onSurfaceVariant,
			)
		}
	)
}

@Stable
class ScreenPartMusic(model: AppModel) : ScreenPart(model) {
	private val factory = app.musicFactory

	private var isAnimationBackground by mutableStateOf(false)
	private val blurState = HazeState()

	private var currentDebounceTime by mutableLongStateOf(0L)
	private var hasAnimation by mutableStateOf(false)
	private var hasVideo by mutableStateOf(false)
	private var lyrics = LyricsLrc()

	private var sleepJob: Job? by mutableStateOf(null)
	private var sleepRemainSeconds: Int by mutableIntStateOf(0)

	private fun openMusicComment() {
		factory.currentMusic?.let { musicInfo ->
			launch {
				val result = ClientAPI.request(
					route = API.User.Song.GetSong,
					data = musicInfo.id
				)
				when (result) {
					is Data.Success -> navigate(ScreenSongDetails.Args(song = result.data))
					is Data.Error -> slot.tip.error(result.message)
				}
			}
		}
	}

	@Composable
	private fun Modifier.hazeBlur(radius: Dp): Modifier = hazeEffect(
		state = blurState,
		style = HazeStyle(
			blurRadius = radius,
			backgroundColor = Colors.Dark,
			tint = null
		)
	)

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
					if (app.musicFactory.isInit) navigate<ScreenMusicLibrary>()
					else slot.tip.warning("播放器尚未初始化")
				}
				Action(
					icon = Icons.AutoMirrored.Outlined.QueueMusic,
					color = Colors.White
				) {
					if (app.musicFactory.isInit) navigate<ScreenPlaylistLibrary>()
					else slot.tip.warning("播放器尚未初始化")
				}
				Action(
					icon = Icons.Outlined.Lyrics,
					color = Colors.White
				) {
					if (app.musicFactory.isInit) navigate<ScreenFloatingLyrics>()
					else slot.tip.warning("播放器尚未初始化")
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
		var animationRecord by rememberState { Animatable(0f) }
		var lastDegree by rememberValueState(0f)
		val isForeground = rememberOffScreenState()

		LaunchedEffect(factory.isPlaying, isForeground) {
			if (factory.isPlaying && isForeground) {
				animationRecord.animateTo(
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
				animationRecord.snapTo(lastDegree)
				animationRecord.stop()
			}
		}

		LocalFileImage(
			path = { musicInfo.recordPath },
			musicInfo,
			contentScale = ContentScale.Crop,
			circle = true,
			modifier = modifier.rotate(degrees = animationRecord.value)
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
					.width((ThemeValue.Size.ProgressHeight + ThemeValue.Size.Little) * 2)
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
		ProgressSlider(
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
		Column(
			modifier = modifier,
			verticalArrangement = Arrangement.spacedBy(ThemeValue.Padding.LittleSpace)
		) {
			MusicProgressText(
				currentTime = currentDebounceTime,
				duration = factory.currentDuration,
				modifier = Modifier.fillMaxWidth()
			)

			MusicProgressBar(
				currentTime = currentDebounceTime,
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
				ClickIcon(
					icon = Icons.Outlined.GifBox,
					color = if (isAnimationBackground) MaterialTheme.colorScheme.primary else Colors.White,
					enabled = hasAnimation,
					onClick = { isAnimationBackground = !isAnimationBackground }
				)
			}
			EqualItem {
				ClickIcon(
					icon = Icons.Outlined.MusicVideo,
					color = Colors.White,
					enabled = hasVideo,
					onClick = {
						launch {
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
					onClick = {}
				)
			}
			EqualItem {
				ClickIcon(
					icon = Icons.AutoMirrored.Outlined.Comment,
					color = Colors.White,
					onClick = { openMusicComment() }
				)
			}
		}
	}

	@Composable
	private fun LyricsLayout(modifier: Modifier = Modifier) {
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
			factory.stop()
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
			Column(modifier = Modifier.fillMaxSize()
				.padding(LocalImmersivePadding.current.withoutBottom)
				.zIndex(2f)
			) {
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
			val immersivePadding = LocalImmersivePadding.current

			Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
				MusicBackground(
					alpha = 0.3f,
					modifier = Modifier.fillMaxSize()
						.hazeSource(state = blurState)
						.zIndex(1f)
				)
				Column(
					modifier = Modifier.fillMaxSize()
						.padding(immersivePadding.withoutEnd)
						.zIndex(2f),
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
				LyricsLayout(modifier = Modifier.fillMaxWidth().fillMaxHeight(fraction = 0.7f).padding(immersivePadding.withoutStart))
			}
		}
	}

	@Composable
	private fun Landscape() {
		Row(modifier = Modifier.fillMaxSize().background(Colors.Black)) {
			val immersivePadding = LocalImmersivePadding.current

			Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
				MusicBackground(
					alpha = 0.3f,
					modifier = Modifier.fillMaxSize()
						.hazeSource(state = blurState)
						.zIndex(1f)
				)
				Column(
					modifier = Modifier.fillMaxSize()
						.padding(immersivePadding.withoutEnd)
						.zIndex(2f),
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
				LyricsLayout(modifier = Modifier.fillMaxWidth().fillMaxHeight(fraction = 0.7f).padding(immersivePadding.withoutStart))
			}
		}
	}

	override suspend fun initialize() {
		monitor(state = { factory.currentPosition }) { position ->
			// 处理进度条
			if (abs(position - currentDebounceTime) > 1000L - MusicFactory.UPDATE_INTERVAL) currentDebounceTime = position
			// 处理歌词
			val newLine = lyrics.updateIndex(position)
			// 处理悬浮歌词
			factory.floatingLyrics?.let {
				if (it.isAttached) it.updateLyrics(newLine)
			}
		}
		monitor(state = { factory.currentMusic }) { musicInfo ->
			lyrics.reset()

			if (musicInfo != null) catching {
				Coroutines.io {
					SystemFileSystem.source(musicInfo.lyricsPath).buffered().use { source ->
						lyrics.parseLrcString(source.readText())
					}
					hasAnimation = SystemFileSystem.metadataOrNull(musicInfo.AnimationPath)?.isRegularFile == true
					hasVideo = SystemFileSystem.metadataOrNull(musicInfo.videoPath)?.isRegularFile == true
				}
			}
			else {
				hasAnimation = false
				hasVideo = false
				exitSleepMode()
			}

			if (isAnimationBackground && !hasAnimation) isAnimationBackground = false
		}
		monitor(state = { factory.error }) { error ->
			error?.let { slot.tip.error(it.message) }
		}
	}

	@Composable
	override fun Content() {
		when (LocalDevice.current.type) {
			Device.Type.PORTRAIT -> Portrait()
			Device.Type.SQUARE -> Square()
			Device.Type.LANDSCAPE -> Landscape()
		}
	}

	private val currentPlaylistSheet = object : FloatingSheet() {
		@Composable
		override fun Content() {
			val isEmptyList by rememberDerivedState { factory.musicList.isEmpty() }

			LaunchedEffect(isEmptyList) {
				if (isEmptyList) close()
			}

			val currentIndex by rememberDerivedState { factory.musicList.indexOf(factory.currentMusic) }

			Column(modifier = Modifier.fillMaxWidth()) {
				Row(
					modifier = Modifier.fillMaxWidth().padding(ThemeValue.Padding.ExtraValue),
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
							close()
							launch { factory.stop() }
						}
					)
				}
				HorizontalDivider()
				LazyColumn(
					modifier = Modifier.fillMaxWidth(),
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
								close()
								launch { factory.gotoIndex(index) }
							},
							modifier = Modifier.fillMaxWidth()
						)
					}
				}
			}
		}
	}

	@OptIn(ExperimentalMaterial3Api::class)
    private val sleepModeSheet = object : FloatingSheet() {
		@Composable
		override fun Content() {
			val state = rememberTimePickerState(is24Hour = true)

			Column(
				modifier = Modifier.fillMaxWidth().padding(ThemeValue.Padding.SheetValue),
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
	}

    @Composable
	override fun Floating() {
		sleepModeSheet.Land()
		currentPlaylistSheet.Land()
	}
}