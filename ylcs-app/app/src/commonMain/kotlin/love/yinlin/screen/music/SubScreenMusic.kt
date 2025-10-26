package love.yinlin.screen.music

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
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.io.buffered
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.readString
import love.yinlin.api.API
import love.yinlin.api.ClientAPI
import love.yinlin.common.*
import love.yinlin.compose.*
import love.yinlin.compose.screen.BasicScreen
import love.yinlin.compose.screen.SubScreen
import love.yinlin.data.Data.Failure
import love.yinlin.data.Data.Success
import love.yinlin.data.music.MusicInfo
import love.yinlin.data.music.MusicPlayMode
import love.yinlin.extension.*
import love.yinlin.platform.Coroutines
import love.yinlin.platform.MusicFactory
import love.yinlin.resources.Res
import love.yinlin.resources.img_music_record
import love.yinlin.resources.no_audio_source
import love.yinlin.resources.unknown_singer
import love.yinlin.compose.ui.image.ClickIcon
import love.yinlin.compose.ui.image.LocalFileImage
import love.yinlin.ui.component.input.ProgressSlider
import love.yinlin.compose.ui.input.ClickText
import love.yinlin.ui.component.layout.*
import love.yinlin.ui.component.lyrics.LyricsLrc
import love.yinlin.compose.ui.node.clickableNoRipple
import love.yinlin.compose.ui.floating.FloatingSheet
import love.yinlin.compose.ui.layout.Space
import love.yinlin.compose.ui.layout.SplitActionLayout
import love.yinlin.compose.ui.layout.SplitLayout
import love.yinlin.screen.common.ScreenVideo
import love.yinlin.service
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
        }.padding(CustomTheme.padding.extraValue),
        horizontalArrangement = CustomTheme.padding.horizontalSpace,
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
class SubScreenMusic(parent: BasicScreen<*>) : SubScreen(parent) {
	private val factory = service.musicFactory.instance

	private var isAnimationBackground by mutableStateOf(false)
	private val blurState = HazeState()

	private var currentDebounceTime by mutableLongStateOf(0L)
	private var hasAnimation by mutableStateOf(false)
	private var hasVideo by mutableStateOf(false)
	private var lyrics = LyricsLrc()

	private var sleepJob: Job? by mutableRefStateOf(null)
	private var sleepRemainSeconds: Int by mutableIntStateOf(0)

	private fun openMusicComment() {
		factory.currentMusic?.let { musicInfo ->
			launch {
				val result = ClientAPI.request(
					route = API.User.Song.GetSong,
					data = musicInfo.id
				)
				when (result) {
					is Success -> navigate(ScreenSongDetails.Args(song = result.data))
					is Failure -> slot.tip.error(result.message)
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
                    tip = "曲库",
                    color = Colors.White
                ) {
                    if (factory.isInit) navigate<ScreenMusicLibrary>()
                    else slot.tip.warning("播放器尚未初始化")
                }
                Action(
                    icon = Icons.AutoMirrored.Outlined.QueueMusic,
                    tip = "歌单",
                    color = Colors.White
                ) {
                    if (factory.isInit) navigate<ScreenPlaylistLibrary>()
                    else slot.tip.warning("播放器尚未初始化")
                }
                Action(
                    icon = Icons.Outlined.Lyrics,
                    tip = "歌词",
                    color = Colors.White
                ) {
                    if (factory.isInit) navigate<ScreenFloatingLyrics>()
                    else slot.tip.warning("播放器尚未初始化")
                }
            },
            right = {
                Action(
                    icon = Icons.Outlined.AlarmOn,
                    tip = "睡眠模式",
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
		var animationRecord by rememberRefState { Animatable(0f) }
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
						modifier = Modifier.fillMaxSize(fraction = 0.75f)
                            .clickable { navigate(ScreenMusicDetails.Args(it.id)) }
                            .zIndex(2f)
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
			horizontalArrangement = Arrangement.spacedBy(CustomTheme.padding.horizontalExtraSpace)
		) {
			MusicRecordLayout(
				offset = -CustomTheme.size.largeImage / 2,
				musicInfo = musicInfo,
				modifier = Modifier.size(CustomTheme.size.largeImage)
					.shadow(elevation = CustomTheme.shadow.icon, clip = false, shape = CircleShape)
			)
			Column(
				modifier = Modifier.weight(1f),
				verticalArrangement = Arrangement.spacedBy(CustomTheme.padding.verticalSpace)
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
			verticalArrangement = Arrangement.spacedBy(CustomTheme.padding.verticalExtraSpace)
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
			horizontalArrangement = Arrangement.spacedBy(CustomTheme.padding.horizontalExtraSpace)
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
					.width((CustomTheme.size.progressHeight + CustomTheme.size.little) * 2)
					.height(CustomTheme.size.progressHeight * 2)
					.clickableNoRipple {
						launch {
							factory.seekTo(hotpot)
							if (!factory.isPlaying) factory.play()
						}
					}
					.padding(horizontal = CustomTheme.padding.littleSpace)
					.shadow(elevation = CustomTheme.shadow.item, shape = CircleShape)
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
			height = CustomTheme.size.progressHeight,
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
			verticalArrangement = Arrangement.spacedBy(CustomTheme.padding.littleSpace)
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
				modifier = Modifier.fillMaxWidth().height(CustomTheme.size.progressHeight * 2)
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
                    tip = "播放模式",
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
                    tip = "播放列表",
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
                    tip = "动画",
					color = if (isAnimationBackground) MaterialTheme.colorScheme.primary else Colors.White,
					enabled = hasAnimation,
					onClick = { isAnimationBackground = !isAnimationBackground }
				)
			}
			EqualItem {
				ClickIcon(
					icon = Icons.Outlined.MusicVideo,
                    tip = "视频",
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
                    tip = "歌词",
					color = Colors.White,
					onClick = {}
				)
			}
			EqualItem {
				ClickIcon(
					icon = Icons.AutoMirrored.Outlined.Comment,
                    tip = "评论",
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
					.padding(CustomTheme.padding.equalValue)
					.clip(MaterialTheme.shapes.large)
					.hazeBlur(15.dp)
					.padding(CustomTheme.padding.value)
				)
				LyricsLayout(modifier = Modifier
					.padding(
						start = CustomTheme.padding.horizontalSpace,
						end = CustomTheme.padding.horizontalSpace,
						bottom = CustomTheme.size.largeImage / 2
					)
					.fillMaxWidth()
					.weight(1f)
				)
				Column(modifier = Modifier.fillMaxWidth()
					.hazeBlur(10.dp)
					.padding(CustomTheme.padding.equalValue)
				) {
					PortraitMusicInfoLayout(modifier = Modifier.fillMaxWidth().padding(CustomTheme.padding.value))
					MusicProgressLayout(modifier = Modifier.fillMaxWidth().padding(CustomTheme.padding.value))
					MusicControlLayout(modifier = Modifier.fillMaxWidth().padding(CustomTheme.padding.value))
					MusicToolLayout(modifier = Modifier.fillMaxWidth().padding(CustomTheme.padding.value))
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
					verticalArrangement = Arrangement.spacedBy(CustomTheme.padding.verticalSpace)
				) {
					ToolLayout(modifier = Modifier
						.fillMaxWidth()
						.padding(CustomTheme.padding.equalValue)
						.clip(MaterialTheme.shapes.large)
						.hazeBlur(30.dp)
						.padding(CustomTheme.padding.value)
					)
					Box(
						modifier = Modifier.padding(CustomTheme.padding.equalExtraValue)
							.fillMaxWidth().weight(1f),
						contentAlignment = Alignment.Center
					) {
						val musicInfo = factory.currentMusic
						if (musicInfo != null) {
							MusicRecordLayout(
								offset = CustomTheme.padding.zeroSpace,
								musicInfo = musicInfo,
								modifier = Modifier.fillMaxHeight().aspectRatio(1f)
									.shadow(elevation = CustomTheme.shadow.icon, clip = false, shape = CircleShape)
							)
						}
					}
					Column(modifier = Modifier
						.fillMaxWidth()
						.hazeBlur(20.dp)
						.padding(CustomTheme.padding.extraValue)
					) {
						MusicProgressLayout(modifier = Modifier.fillMaxWidth().padding(CustomTheme.padding.value))
						MusicControlLayout(modifier = Modifier.fillMaxWidth().padding(CustomTheme.padding.value))
						MusicToolLayout(modifier = Modifier.fillMaxWidth().padding(CustomTheme.padding.value))
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
					verticalArrangement = Arrangement.spacedBy(CustomTheme.padding.verticalSpace)
				) {
					ToolLayout(modifier = Modifier
						.fillMaxWidth()
						.padding(CustomTheme.padding.equalValue)
						.clip(MaterialTheme.shapes.large)
						.hazeBlur(30.dp)
						.padding(CustomTheme.padding.value)
					)
					Box(
						modifier = Modifier.padding(CustomTheme.padding.equalExtraValue)
							.fillMaxWidth().weight(1f),
						contentAlignment = Alignment.Center
					) {
						val musicInfo = factory.currentMusic
						if (musicInfo != null) {
							MusicRecordLayout(
								offset = CustomTheme.padding.zeroSpace,
								musicInfo = musicInfo,
								modifier = Modifier.fillMaxHeight().aspectRatio(1f)
									.shadow(elevation = CustomTheme.shadow.icon, clip = false, shape = CircleShape)
							)
						}
					}
					Row(
						modifier = Modifier
							.fillMaxWidth()
							.height(IntrinsicSize.Min)
							.hazeBlur(20.dp)
							.padding(CustomTheme.padding.extraValue),
						horizontalArrangement = Arrangement.spacedBy(CustomTheme.padding.horizontalExtraSpace),
						verticalAlignment = Alignment.CenterVertically
					) {
						Box(
							modifier = Modifier.width(CustomTheme.size.cellWidth * 0.8f).fillMaxHeight(),
							contentAlignment = Alignment.Center
						) {
							LandscapeMusicInfoLayout(modifier = Modifier.fillMaxWidth())
						}
						Column(modifier = Modifier.weight(1f)) {
							MusicProgressLayout(modifier = Modifier.fillMaxWidth().padding(CustomTheme.padding.value))
							MusicControlLayout(modifier = Modifier.fillMaxWidth().padding(CustomTheme.padding.value))
							MusicToolLayout(modifier = Modifier.fillMaxWidth().padding(CustomTheme.padding.value))
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
						lyrics.parseLrcString(source.readString())
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
	override fun Content(device: Device) {
		when (device.type) {
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
					modifier = Modifier.fillMaxWidth().padding(CustomTheme.padding.extraValue),
					horizontalArrangement = Arrangement.spacedBy(CustomTheme.padding.horizontalExtraSpace),
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
                        tip = "停止",
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
				modifier = Modifier.fillMaxWidth().padding(CustomTheme.padding.sheetValue),
				horizontalAlignment = Alignment.CenterHorizontally,
				verticalArrangement = Arrangement.spacedBy(CustomTheme.padding.verticalSpace)
			) {
				SplitLayout(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = CustomTheme.padding.horizontalSpace),
                    left = {
                        Text(
                            text = "睡眠模式",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    },
                    right = {
                        ClickText(
                            text = if (sleepJob == null) "启动" else "停止",
                            icon = if (sleepJob == null) Icons.Outlined.AlarmOn else Icons.Outlined.AlarmOff,
                            onClick = {
                                if (factory.isReady) {
                                    if (sleepJob == null) {
                                        val time = state.hour * 3600 + state.minute * 60
                                        if (time > 0) startSleepMode(time)
                                        else slot.tip.warning("未设定时间")
                                    } else exitSleepMode()
                                } else slot.tip.warning("播放器未开启")
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