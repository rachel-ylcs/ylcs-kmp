package love.yinlin.ui.screen.music

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Comment
import androidx.compose.material.icons.automirrored.outlined.QueueMusic
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import dev.chrisbanes.haze.*
import io.ktor.utils.io.*
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import love.yinlin.AppModel
import love.yinlin.ScreenPart
import love.yinlin.common.Colors
import love.yinlin.common.ExtraIcons
import love.yinlin.common.ThemeColor
import love.yinlin.data.music.MusicInfo
import love.yinlin.data.music.MusicPlayMode
import love.yinlin.extension.rememberDerivedState
import love.yinlin.extension.rememberState
import love.yinlin.extension.timeString
import love.yinlin.platform.Coroutines
import love.yinlin.platform.ImageQuality
import love.yinlin.platform.MusicFactory
import love.yinlin.platform.app
import love.yinlin.resources.Res
import love.yinlin.resources.img_music_record
import love.yinlin.resources.no_audio_source
import love.yinlin.resources.unknown_singer
import love.yinlin.ui.component.image.ClickIcon
import love.yinlin.ui.component.image.LocalFileImage
import love.yinlin.ui.component.layout.EqualRow
import love.yinlin.ui.component.layout.OffsetLayout
import love.yinlin.ui.component.layout.SplitActionLayout
import love.yinlin.ui.component.layout.equalItem
import love.yinlin.ui.component.lyrics.LyricsLrc
import love.yinlin.ui.component.screen.BottomSheet
import love.yinlin.ui.component.screen.CommonSheetState
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
		}.padding(horizontal = 15.dp, vertical = 10.dp),
		horizontalArrangement = Arrangement.spacedBy(10.dp),
		verticalAlignment = Alignment.CenterVertically
	) {
		Text(
			text = musicInfo.name,
			style = MaterialTheme.typography.titleMedium,
			color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
			maxLines = 1,
			overflow = TextOverflow.MiddleEllipsis,
			modifier = Modifier.weight(1f)
		)
		Text(
			text = musicInfo.singer,
			style = MaterialTheme.typography.bodyMedium,
			maxLines = 1,
			overflow = TextOverflow.MiddleEllipsis,
			color = ThemeColor.fade
		)
	}
}

@Stable
class ScreenPartMusic(model: AppModel) : ScreenPart(model) {
	private val factory = app.musicFactory

	private var lyrics = LyricsLrc()

	private val blurState = HazeState()

	private val currentPlaylistSheet = CommonSheetState()

	@Composable
	private fun ToolLayout(modifier: Modifier = Modifier) {
		SplitActionLayout(
			modifier = modifier,
			left = {
				Action(Icons.Outlined.LibraryMusic) {
					navigate(ScreenMusicLibrary.Args)
				}
				Action(Icons.AutoMirrored.Outlined.QueueMusic) {
					navigate(ScreenPlaylistLibrary.Args)
				}
				Action(Icons.Outlined.Lyrics) {

				}
			},
			right = {
				Action(Icons.Outlined.AlarmOn) {

				}
			}
		)
	}

	@Composable
	private fun MusicRecord(
		path: Path,
		modifier: Modifier = Modifier
	) {
		var lastDegree by rememberState { 0f }
		val animation by rememberState { Animatable(0f) }

		LaunchedEffect(factory.isPlaying) {
			if (factory.isPlaying) {
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
			path = path,
			contentScale = ContentScale.Crop,
			circle = true,
			modifier = modifier.rotate(degrees = animation.value)
		)
	}

	@Composable
	private fun MusicRecordLayout(
		recordPath: Path?,
		modifier: Modifier = Modifier
	) {
		OffsetLayout(y = (-50).dp) {
			Box(
				modifier = modifier,
				contentAlignment = Alignment.Center
			) {
				Image(
					painter = painterResource(Res.drawable.img_music_record),
					contentDescription = null,
					modifier = Modifier.fillMaxSize().zIndex(1f)
				)
				recordPath?.let {
					MusicRecord(
						path = it,
						modifier = Modifier.fillMaxSize(fraction = 0.75f).zIndex(2f)
					)
				}
			}
		}
	}

	@Composable
	private fun MusicTitleLayout(
		name: String?,
		singer: String?,
		modifier: Modifier = Modifier
	) {
		Column(
			modifier = modifier,
			verticalArrangement = Arrangement.spacedBy(5.dp)
		) {
			Text(
				text = name ?: stringResource(Res.string.no_audio_source),
				style = MaterialTheme.typography.titleLarge,
				textAlign = TextAlign.Center,
				maxLines = 1,
				overflow = TextOverflow.Ellipsis
			)
			Text(
				text = singer ?: stringResource(Res.string.unknown_singer),
				style = MaterialTheme.typography.bodyLarge,
				textAlign = TextAlign.Center,
				maxLines = 1,
				overflow = TextOverflow.Ellipsis
			)
		}
	}

	@Composable
	private fun MusicInfoLayout(modifier: Modifier = Modifier) {
		Row(
			modifier = modifier,
			horizontalArrangement = Arrangement.spacedBy(20.dp)
		) {
			MusicRecordLayout(
				recordPath = factory.currentMusic?.recordPath,
				modifier = Modifier.size(100.dp).shadow(elevation = 5.dp, clip = false, shape = CircleShape)
			)
			MusicTitleLayout(
				name = factory.currentMusic?.name,
				singer = factory.currentMusic?.singer,
				modifier = Modifier.weight(1f)
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
			horizontalArrangement = Arrangement.spacedBy(20.dp)
		) {
			Text(
				text = remember(currentTime) { currentTime.timeString },
				textAlign = TextAlign.Start,
				modifier = Modifier.weight(1f)
			)
			Text(
				text = remember(duration) { duration.timeString },
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
					.width(14.dp)
					.height(8.dp)
					.clickable(
						interactionSource = null,
						indication = null,
						onClick = {
							launch { factory.seekTo(hotpot) }
						}
					)
					.padding(horizontal = 3.dp)
					.shadow(elevation = 2.dp, shape = CircleShape)
					.background(
						color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
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
		chorus: List<Long>,
		modifier: Modifier = Modifier
	) {
		BoxWithConstraints(modifier = modifier) {
			Box(modifier = Modifier
				.fillMaxSize()
				.padding(vertical = 3.dp)
				.background(
					color = Colors.Gray5,
					shape = MaterialTheme.shapes.medium
				)
				.pointerInput(duration, maxWidth) {
					detectTapGestures(onTap = { offset ->
						if (duration != 0L) {
							launch { factory.seekTo((offset.x / maxWidth.toPx() * duration).toLong()) }
						}
					})
				}
				.zIndex(1f)
			)
			if (duration != 0L) {
				Box(modifier = Modifier
					.fillMaxWidth(fraction = currentTime / duration.toFloat())
					.fillMaxHeight()
					.padding(vertical = 3.dp)
					.background(
						color = MaterialTheme.colorScheme.primary,
						shape = MaterialTheme.shapes.medium
					)
					.pointerInput(duration, maxWidth) {
						detectTapGestures(onTap = { offset ->
							if (duration != 0L) {
								launch { factory.seekTo((offset.x / maxWidth.toPx() * duration).toLong()) }
							}
						})
					}
					.zIndex(2f)
				)
				MusicChorus(
					chorus = chorus,
					duration = duration,
					modifier = Modifier.fillMaxSize().zIndex(3f)
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
			verticalArrangement = Arrangement.spacedBy(2.dp)
		) {
			MusicProgressText(
				currentTime = currentTime,
				duration = factory.currentDuration,
				modifier = Modifier.fillMaxWidth()
			)

			MusicProgressBar(
				currentTime = currentTime,
				duration = factory.currentDuration,
				chorus = factory.currentMusic?.chorus ?: emptyList(),
				modifier = Modifier.fillMaxWidth().height(10.dp)
			)
		}
	}

	@Composable
	private fun MusicControlLayout(modifier: Modifier = Modifier) {
		EqualRow(modifier = modifier) {
			equalItem {
				ClickIcon(
					icon = when (factory.playMode) {
                        MusicPlayMode.ORDER -> ExtraIcons.OrderMode
                        MusicPlayMode.LOOP -> ExtraIcons.LoopMode
                        MusicPlayMode.RANDOM -> ExtraIcons.ShuffleMode
                    },
					onClick = {
						launch { factory.switchPlayMode() }
					}
				)
			}
			equalItem {
				ClickIcon(
					icon = ExtraIcons.GotoPrevious,
					onClick = {
						launch { factory.gotoPrevious() }
					}
				)
			}
			equalItem {
				ClickIcon(
					icon = if (factory.isPlaying) ExtraIcons.Pause else ExtraIcons.Play,
					onClick = {
						launch {
							if (factory.isPlaying) factory.pause()
							else factory.play()
						}
					}
				)
			}
			equalItem {
				ClickIcon(
					icon = ExtraIcons.GotoNext,
					onClick = {
						launch { factory.gotoNext() }
					}
				)
			}
			equalItem {
				ClickIcon(
					icon = ExtraIcons.Playlist,
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
			equalItem {
				ClickIcon(
					icon = Icons.Outlined.GifBox,
					onClick = {
						launch {
							factory.startPlaylist(app.config.playlistLibrary["歌单1"]!!)
						}
					}
				)
			}
			equalItem {
				ClickIcon(
					icon = Icons.Outlined.MusicVideo,
					onClick = {
						launch {
							factory.startPlaylist(app.config.playlistLibrary["歌单2"]!!)
						}
					}
				)
			}
			equalItem {
				ClickIcon(
					icon = ExtraIcons.ShowLyrics,
					onClick = {
						launch {
							factory.startPlaylist(app.config.playlistLibrary["歌单3"]!!)
						}
					}
				)
			}
			equalItem {
				ClickIcon(
					icon = Icons.AutoMirrored.Outlined.Comment,
					onClick = {
						launch {
							factory.stop()
						}
					}
				)
			}
		}
	}

	@Composable
	private fun LyricsLayout(modifier: Modifier = Modifier) {
		LaunchedEffect(factory.currentMusic) {
			lyrics.reset()
			factory.currentMusic?.lyricsPath?.let { path ->
				try {
					Coroutines.io {
						SystemFileSystem.source(path).buffered().use { source ->
							lyrics.parseLrcString(source.readText())
						}
					}
				}
				catch (_: Throwable) { }
			}
		}

		LaunchedEffect(factory.currentPosition) {
			lyrics.updateIndex(factory.currentPosition)
		}

		Box(modifier = modifier) {
			lyrics.content(
				modifier = Modifier.fillMaxSize(),
				onLyricsClick = {
					launch {
						factory.seekTo(it)
					}
				}
			)
		}
	}

	@Composable
	private fun ControlLayout(modifier: Modifier = Modifier) {
		Column(
			modifier = modifier,
			verticalArrangement = Arrangement.spacedBy(5.dp)
		) {
			MusicInfoLayout(modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp))
			MusicProgressLayout(modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp))
			MusicControlLayout(modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 5.dp))
			MusicToolLayout(modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp))
		}
	}

	@Composable
	private fun CurrentPlaylistLayout() {
		val isEmptyList by rememberDerivedState { factory.musicList.isEmpty() }

		LaunchedEffect(isEmptyList) {
			if (isEmptyList) currentPlaylistSheet.hide()
		}

		BottomSheet(state = currentPlaylistSheet) {
			Column(modifier = Modifier.fillMaxWidth().fillMaxHeight(fraction = 0.6f)) {
				Row(
					modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 10.dp, bottom = 10.dp),
					horizontalArrangement = Arrangement.spacedBy(10.dp),
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
							currentPlaylistSheet.hide()
							launch { factory.stop() }
						}
					)
				}
				HorizontalDivider(modifier = Modifier.height(1.dp))

				val currentId by derivedStateOf { factory.currentMusic?.id }
				LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f)) {
					items(
						items = factory.musicList,
						key = { it.id }
					) { musicInfo ->
						PlayingMusicStatusCard(
							musicInfo = musicInfo,
							isCurrent = musicInfo.id == currentId,
							onClick = {
								currentPlaylistSheet.hide()
								launch { factory.gotoIndex(factory.musicList.indexOfFirst { it.id == musicInfo.id }) }
							},
							modifier = Modifier.fillMaxWidth()
						)
					}
				}
			}
		}
	}

	@Composable
	private fun Portrait() {
		Box(modifier = Modifier.fillMaxSize()) {
			Box(modifier = Modifier
				.fillMaxSize()
				.background(MaterialTheme.colorScheme.onBackground)
				.hazeSource(state = blurState)
				.zIndex(1f)
			) {
				factory.currentMusic?.backgroundPath?.let { path ->
					LocalFileImage(
						path = path,
						quality = ImageQuality.Full,
						contentScale = ContentScale.Crop,
						alpha = if (app.isDarkMode) 0.9f else 0.7f,
						modifier = Modifier.fillMaxSize()
					)
				}
			}
			Column(modifier = Modifier.fillMaxSize().zIndex(2f)) {
				ToolLayout(modifier = Modifier
					.fillMaxWidth()
					.padding(10.dp)
					.clip(MaterialTheme.shapes.large)
					.hazeEffect(
						state = blurState,
						style = HazeStyle(
							blurRadius = 15.dp,
							backgroundColor = MaterialTheme.colorScheme.background,
							tint = null,
						)
					)
					.padding(10.dp)
				)
				LyricsLayout(modifier = Modifier
					.padding(start = 10.dp, end = 10.dp, top = 30.dp, bottom = 50.dp)
					.fillMaxWidth()
					.weight(1f)
				)
				ControlLayout(modifier = Modifier
					.fillMaxWidth()
					.hazeEffect(
						state = blurState,
						style = HazeStyle(
							blurRadius = 10.dp,
							backgroundColor = MaterialTheme.colorScheme.background,
							tint = null,
						)
					)
					.padding(10.dp)
				)
			}
		}
	}

	@Composable
	private fun Landscape() {
		Column(
			modifier = Modifier.fillMaxSize()
		) {
			Surface(
				modifier = Modifier.fillMaxWidth(),
				shadowElevation = 5.dp
			) {
				ToolLayout(modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 5.dp))
			}
			Row(
				modifier = Modifier.weight(1f)
			) {

			}
		}
	}

    @Composable
	override fun content() {
		if (app.isPortrait) Portrait()
		else Landscape()

		currentPlaylistSheet.withOpen {
			CurrentPlaylistLayout()
		}
	}
}