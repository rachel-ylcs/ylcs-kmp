package love.yinlin.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import love.yinlin.app
import love.yinlin.compose.extension.mutableRefStateOf
import love.yinlin.compose.screen.Screen
import love.yinlin.cs.ApiConcertGetConcerts
import love.yinlin.cs.ApiConcertGetPhotos
import love.yinlin.cs.requestNull
import love.yinlin.data.rachel.concert.Concert
import love.yinlin.data.rachel.concert.ConcertPhoto
import love.yinlin.data.rachel.concert.ConcertSong

@Stable
class ScreenConcertEditor : Screen() {
    override val title: String = "演出记忆编辑器"

    private var concerts: List<Concert> by mutableRefStateOf([])
    private var songs: List<ConcertSong> by mutableStateOf([])
    private var photos: List<ConcertPhoto> by mutableStateOf([])

    override fun initialize() {
        launch {
            val token = app.config.userToken
            val [v1, v2, _] = ApiConcertGetConcerts.requestNull(token) ?: return@launch
            if (v1.isEmpty() || v2.isEmpty()) return@launch
            val [v3] = ApiConcertGetPhotos.requestNull(token) ?: return@launch
            concerts = v1
            songs = v2
            photos = v3
        }
    }

    @Composable
    override fun Content() {

    }
}