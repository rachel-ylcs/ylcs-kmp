@file:OptIn(UnstableApi::class)
package love.yinlin.service

import android.app.PendingIntent
import android.content.ComponentName
import android.content.Intent
import android.os.Build
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import love.yinlin.MainActivity
import love.yinlin.common.FfmpegRenderersFactory
import love.yinlin.platform.ForwardPlayer

object CustomCommands {
    val NotificationPlayerCommands get() = MediaSession.ConnectionResult.DEFAULT_PLAYER_COMMANDS.buildUpon()
        .build()

    val SessionCommands get() = MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS.buildUpon()
        .build()
}

class MusicService : MediaSessionService() {
    private var player: ExoPlayer? = null
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
    }

    override fun onCreate() {
        super.onCreate()

        val context = this
        val keepFocus = true

        val ffmpegPlayer = FfmpegRenderersFactory.build(this, keepFocus)
        val forwardPlayer = ForwardPlayer(ffmpegPlayer)
        player = ffmpegPlayer
        session = MediaSession.Builder(context, forwardPlayer)
            .setCallback(listener)
            .setSessionActivity(
                PendingIntent.getActivity(context, 0,
                    Intent().apply { setComponent(ComponentName(context, MainActivity::class.java)) },
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_IMMUTABLE else PendingIntent.FLAG_ONE_SHOT
                )
            )
            .build()
    }

    override fun onDestroy() {
        player?.release()
        session?.release()
        player = null
        session = null
        super.onDestroy()
    }

    override fun onTaskRemoved(rootIntent: Intent?) = pauseAllPlayersAndStopSelf()

    override fun onGetSession(controller: MediaSession.ControllerInfo): MediaSession? = session
}