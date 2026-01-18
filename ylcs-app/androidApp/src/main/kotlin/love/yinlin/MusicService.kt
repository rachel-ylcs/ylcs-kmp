@file:OptIn(UnstableApi::class)
package love.yinlin

import android.app.PendingIntent
import android.content.ComponentName
import android.content.Intent
import android.os.Build
import android.os.Bundle
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
import love.yinlin.common.FfmpegRenderersFactory
import love.yinlin.data.music.MusicPlayMode
import love.yinlin.platform.CustomCommands
import love.yinlin.platform.ForwardPlayer
import love.yinlin.platform.mergePlayMode

class MusicService : MediaSessionService() {
    private var exoPlayer: ExoPlayer? = null
    private var session: MediaSession? = null

    private val listener = object : MediaSession.Callback {
        override fun onConnect(session: MediaSession, controller: MediaSession.ControllerInfo): MediaSession.ConnectionResult = MediaSession.ConnectionResult.AcceptedResultBuilder(session).apply {
            if (session.isMediaNotificationController(controller) ||
                session.isAutomotiveController(controller) ||
                session.isAutoCompanionController(controller)) {
                setAvailablePlayerCommands(CustomCommands.NotificationPlayerCommands)
            }
            setAvailableSessionCommands(CustomCommands.SessionCommands)
        }.build()

        override fun onCustomCommand(session: MediaSession, controller: MediaSession.ControllerInfo, customCommand: SessionCommand, args: Bundle): ListenableFuture<SessionResult> {
            val player = exoPlayer ?: return Futures.immediateFuture(SessionResult(SessionError.ERROR_SESSION_DISCONNECTED))
            return when (customCommand) {
                CustomCommands.SetMode -> {
                    val playMode = if (args.isEmpty) mergePlayMode(player.repeatMode, player.shuffleModeEnabled).next
                    else MusicPlayMode.entries.getOrNull(args.getInt(CustomCommands.Args.SET_MODE_ARG_MODE)) ?: MusicPlayMode.ORDER
                    when (playMode) {
                        MusicPlayMode.ORDER -> {
                            player.repeatMode = Player.REPEAT_MODE_ALL
                            player.shuffleModeEnabled = false
                        }
                        MusicPlayMode.LOOP -> {
                            player.repeatMode = Player.REPEAT_MODE_ONE
                            player.shuffleModeEnabled = false
                        }
                        MusicPlayMode.RANDOM -> {
                            player.repeatMode = Player.REPEAT_MODE_ALL
                            player.shuffleModeEnabled = true
                        }
                    }
                    session.setCustomLayout(CustomCommands.prepareButtons(playMode))
                    Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                }
                CustomCommands.Stop -> {
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

        val ffmpegPlayer = FfmpegRenderersFactory.build(this, app.config.audioFocus)
        val forwardPlayer = ForwardPlayer(ffmpegPlayer)
        exoPlayer = ffmpegPlayer
        session = MediaSession.Builder(context, forwardPlayer)
            .setCustomLayout(CustomCommands.prepareButtons(MusicPlayMode.ORDER))
            .setCallback(listener)
            .setSessionActivity(
                PendingIntent.getActivity(context, 0,
                    Intent().apply { setComponent(ComponentName(context, MainActivity::class.java)) },
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_IMMUTABLE else PendingIntent.FLAG_ONE_SHOT
                )
            ).build()
    }

    override fun onDestroy() {
        exoPlayer?.release()
        session?.release()
        exoPlayer = null
        session = null
        super.onDestroy()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        pauseAllPlayersAndStopSelf()
    }

    override fun onGetSession(controller: MediaSession.ControllerInfo): MediaSession? = session
}