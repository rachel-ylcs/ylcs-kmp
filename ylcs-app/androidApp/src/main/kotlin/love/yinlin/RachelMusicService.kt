package love.yinlin

import androidx.activity.ComponentActivity
import love.yinlin.media.MusicService
import kotlin.reflect.KClass

class RachelMusicService : MusicService() {
    override val audioFocus: Boolean get() = app.config.audioFocus
    override val activityClass: KClass<out ComponentActivity> = MainActivity::class
}