package love.yinlin.screen.music

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import kotlinx.io.files.Path
import love.yinlin.compose.*
import love.yinlin.data.music.MusicInfo
import love.yinlin.data.music.MusicResourceType
import love.yinlin.platform.OS

@Stable
val MusicInfo.path: Path get() = Path(OS.Storage.musicPath, this.id)
@Stable
val MusicInfo.configPath: Path get() = Path(OS.Storage.musicPath, this.id, MusicResourceType.Config.default.toString())
@Stable
val MusicInfo.audioPath: Path get() = Path(OS.Storage.musicPath, this.id, MusicResourceType.Audio.default.toString())
@Stable
val MusicInfo.recordPath: Path get() = Path(OS.Storage.musicPath, this.id, MusicResourceType.Record.default.toString())
@Stable
val MusicInfo.backgroundPath: Path get() = Path(OS.Storage.musicPath, this.id, MusicResourceType.Background.default.toString())
@Stable
val MusicInfo.AnimationPath: Path get() = Path(OS.Storage.musicPath, this.id, MusicResourceType.Animation.default.toString())
@Stable
val MusicInfo.lyricsPath: Path get() = Path(OS.Storage.musicPath, this.id, MusicResourceType.LineLyrics.default.toString())
@Stable
val MusicInfo.videoPath: Path get() = Path(OS.Storage.musicPath, this.id, MusicResourceType.Video.default.toString())
@Stable
val MusicInfo.rhymePath: Path get() = Path(OS.Storage.musicPath, this.id, MusicResourceType.Rhyme.default.toString())

val MusicResourceType?.background: Brush get() = when (this) {
    MusicResourceType.Config -> Brush.linearGradient(listOf(Colors.Yellow4, Colors.Yellow5, Colors.Yellow6))
    MusicResourceType.Audio -> Brush.linearGradient(listOf(Colors.Pink3, Colors.Pink4, Colors.Pink5))
    MusicResourceType.Record -> Brush.linearGradient(listOf(Colors.Purple3, Colors.Purple4, Colors.Purple5))
    MusicResourceType.Background -> Brush.linearGradient(listOf(Colors.Blue3, Colors.Blue4, Colors.Blue5))
    MusicResourceType.Animation -> Brush.linearGradient(listOf(Colors.Orange3, Colors.Orange4, Colors.Orange5))
    MusicResourceType.LineLyrics -> Brush.linearGradient(listOf(Colors.Green5, Colors.Green6, Colors.Green7))
    MusicResourceType.Video -> Brush.linearGradient(listOf(Colors.Green3, Colors.Green4, Colors.Green5))
    MusicResourceType.Rhyme -> Brush.linearGradient(listOf(Colors.Cyan3, Colors.Cyan4, Colors.Cyan5))
    null -> Brush.linearGradient(listOf(Colors.Gray3, Colors.Gray4, Colors.Gray5))
}

val MusicResourceType?.icon: ImageVector get() = when (this) {
    MusicResourceType.Config -> Icons.Outlined.Construction
    MusicResourceType.Audio -> Icons.Outlined.AudioFile
    MusicResourceType.Record -> Icons.Outlined.Album
    MusicResourceType.Background -> Icons.Outlined.Image
    MusicResourceType.Animation -> Icons.Outlined.GifBox
    MusicResourceType.LineLyrics -> Icons.Outlined.Lyrics
    MusicResourceType.Video -> Icons.Outlined.Movie
    MusicResourceType.Rhyme -> Icons.Outlined.MusicNote
    null -> Icons.Outlined.QuestionMark
}