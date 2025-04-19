package love.yinlin.ui.screen.music

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import kotlinx.io.files.Path
import love.yinlin.common.Colors
import love.yinlin.data.music.MusicInfo
import love.yinlin.data.music.MusicResourceType
import love.yinlin.platform.OS

@Stable
val MusicInfo.path get(): Path = Path(OS.Storage.musicPath, this.id)
@Stable
val MusicInfo.audioPath get(): Path = Path(OS.Storage.musicPath, this.id, MusicResourceType.Audio.defaultFilename)
@Stable
val MusicInfo.recordPath get(): Path = Path(OS.Storage.musicPath, this.id, MusicResourceType.Record.defaultFilename)
@Stable
val MusicInfo.backgroundPath get(): Path = Path(OS.Storage.musicPath, this.id, MusicResourceType.Background.defaultFilename)
@Stable
val MusicInfo.lyricsPath get(): Path = Path(OS.Storage.musicPath, this.id, MusicResourceType.LineLyrics.defaultFilename)

@Stable
val MusicResourceType.background get(): Color = when (this) {
    MusicResourceType.Config -> Colors.Yellow5
    MusicResourceType.Audio -> Colors.Pink4
    MusicResourceType.Record -> Colors.Purple4
    MusicResourceType.Background -> Colors.Blue4
    MusicResourceType.Animation -> Colors.Orange4
    MusicResourceType.LineLyrics -> Colors.Green6
    MusicResourceType.Video -> Colors.Green4
}

@Stable
val MusicResourceType.icon get(): ImageVector = when (this) {
    MusicResourceType.Config -> Icons.Outlined.Construction
    MusicResourceType.Audio -> Icons.Outlined.AudioFile
    MusicResourceType.Record -> Icons.Outlined.Album
    MusicResourceType.Background -> Icons.Outlined.Image
    MusicResourceType.Animation -> Icons.Outlined.GifBox
    MusicResourceType.LineLyrics -> Icons.Outlined.Lyrics
    MusicResourceType.Video -> Icons.Outlined.Movie
}

@Stable
sealed interface DeleteProcessor {

}

@Stable
sealed interface ReplaceProcessor {

}

@Stable
val MusicResourceType.deleteProcessor get(): DeleteProcessor? = when (this) {
    MusicResourceType.Config -> TODO()
    MusicResourceType.Audio -> TODO()
    MusicResourceType.Record -> TODO()
    MusicResourceType.Background -> TODO()
    MusicResourceType.Animation -> TODO()
    MusicResourceType.LineLyrics -> TODO()
    MusicResourceType.Video -> TODO()
}

@Stable
val MusicResourceType.replaceProcessor get(): ReplaceProcessor? = when (this) {
    MusicResourceType.Config -> TODO()
    MusicResourceType.Audio -> TODO()
    MusicResourceType.Record -> TODO()
    MusicResourceType.Background -> TODO()
    MusicResourceType.Animation -> TODO()
    MusicResourceType.LineLyrics -> TODO()
    MusicResourceType.Video -> TODO()
}