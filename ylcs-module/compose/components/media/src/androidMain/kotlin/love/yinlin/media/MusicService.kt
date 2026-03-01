package love.yinlin.media

import android.app.PendingIntent
import android.content.ComponentName
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.annotation.OptIn
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionError
import androidx.media3.session.SessionResult
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import love.yinlin.compose.data.media.MediaPlayMode
import kotlin.reflect.KClass

@OptIn(UnstableApi::class)
abstract class MusicService : MediaSessionService() {
    abstract val audioFocus: Boolean
    abstract val activityClass: KClass<out ComponentActivity>

    private var exoPlayer: ExoPlayer? = null
    private var musicSession: MediaSession? = null

    private val listener = object : MediaSession.Callback {
        override fun onConnect(session: MediaSession, controller: MediaSession.ControllerInfo): MediaSession.ConnectionResult = MediaSession.ConnectionResult.AcceptedResultBuilder(session).apply {
            if (session.isMediaNotificationController(controller) ||
                session.isAutomotiveController(controller) ||
                session.isAutoCompanionController(controller)) {
                setAvailablePlayerCommands(MediaCommands.NotificationPlayerCommands)
            }
            setAvailableSessionCommands(MediaCommands.SessionCommands)
        }.build()

        override fun onCustomCommand(session: MediaSession, controller: MediaSession.ControllerInfo, customCommand: SessionCommand, args: Bundle): ListenableFuture<SessionResult> {
            val player = exoPlayer ?: return Futures.immediateFuture(SessionResult(SessionError.ERROR_SESSION_DISCONNECTED))
            return when (customCommand) {
                MediaCommands.SetMode -> {
                    val playMode = if (args.isEmpty) mergePlayMode(player.repeatMode, player.shuffleModeEnabled).next
                    else MediaPlayMode.entries.getOrNull(args.getInt(MediaCommands.Args.SET_MODE_ARG_MODE)) ?: MediaPlayMode.Order
                    when (playMode) {
                        MediaPlayMode.Order -> {
                            player.repeatMode = Player.REPEAT_MODE_ALL
                            player.shuffleModeEnabled = false
                        }
                        MediaPlayMode.Loop -> {
                            player.repeatMode = Player.REPEAT_MODE_ONE
                            player.shuffleModeEnabled = false
                        }
                        MediaPlayMode.Random -> {
                            player.repeatMode = Player.REPEAT_MODE_ALL
                            player.shuffleModeEnabled = true
                        }
                    }
                    session.setCustomLayout(MediaCommands.prepareButtons(playMode))
                    Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                }
                MediaCommands.Stop -> {
                    player.clearMediaItems()
                    Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                }
                else -> super.onCustomCommand(session, controller, customCommand, args)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()

        val context = this

        val ffmpegPlayer = FfmpegRenderersFactory.build(this, audioFocus)
        val forwardPlayer = ForwardPlayer(ffmpegPlayer)
        exoPlayer = ffmpegPlayer
        musicSession = MediaSession.Builder(context, forwardPlayer)
            .setCustomLayout(MediaCommands.prepareButtons(MediaPlayMode.Order))
            .setCallback(listener)
            .setSessionActivity(
                PendingIntent.getActivity(
                    context,
                    0,
                    Intent().apply { setComponent(ComponentName(context, activityClass.java)) },
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_IMMUTABLE else PendingIntent.FLAG_ONE_SHOT
                )
            ).build()
    }

    override fun onDestroy() {
        exoPlayer?.release()
        exoPlayer = null
        musicSession?.release()
        musicSession = null
        super.onDestroy()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        pauseAllPlayersAndStopSelf()
    }

    override fun onGetSession(controller: MediaSession.ControllerInfo): MediaSession? = musicSession
}