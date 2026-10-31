package love.yinlin.media

actual fun buildMusicPlayer(fetcher: MediaMetadataFetcher): MusicPlayer = MiniaudioMusicPlayer(fetcher)
