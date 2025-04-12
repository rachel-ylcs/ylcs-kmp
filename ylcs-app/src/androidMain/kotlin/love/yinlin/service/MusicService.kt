@file:OptIn(UnstableApi::class)
package love.yinlin.service

import android.app.PendingIntent
import android.content.ComponentName
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.*
import love.yinlin.MainActivity
import love.yinlin.R
import love.yinlin.common.FfmpegRenderersFactory
import love.yinlin.data.music.MusicPlayMode
import love.yinlin.platform.ForwardPlayer

object CustomCommands {
    val Play = SessionCommand("Play", Bundle.EMPTY)
    val Pause = SessionCommand("Pause", Bundle.EMPTY)
    val Stop = SessionCommand("Stop", Bundle.EMPTY)
    val GetMode = SessionCommand("GetMode", Bundle.EMPTY)
    val NextMode = SessionCommand("NextMode", Bundle.EMPTY)

    val NotificationPlayerCommands get() = MediaSession.ConnectionResult.DEFAULT_PLAYER_COMMANDS.buildUpon()
        .build()

    val SessionCommands get() = MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS.buildUpon()
        .add(Play)
        .add(Pause)
        .add(Stop)
        .add(GetMode)
        .add(NextMode)
        .build()
}

private object QTEButton {
    private val OrderMode = CommandButton.Builder(CommandButton.ICON_UNDEFINED)
        .setDisplayName("顺序播放")
        .setCustomIconResId(R.drawable.icon_player_mode_order)
        .setSessionCommand(CustomCommands.NextMode)
        .setSlots(CommandButton.SLOT_FORWARD_SECONDARY)
        .build()
    private val LoopMode = CommandButton.Builder(CommandButton.ICON_UNDEFINED)
        .setDisplayName("单曲循环")
        .setCustomIconResId(R.drawable.icon_player_mode_loop)
        .setSessionCommand(CustomCommands.NextMode)
        .setSlots(CommandButton.SLOT_FORWARD_SECONDARY)
        .build()
    private val RandomMode = CommandButton.Builder(CommandButton.ICON_UNDEFINED)
        .setDisplayName("随机播放")
        .setCustomIconResId(R.drawable.icon_player_mode_random)
        .setSessionCommand(CustomCommands.NextMode)
        .setSlots(CommandButton.SLOT_FORWARD_SECONDARY)
        .build()
    private val Stop = CommandButton.Builder(CommandButton.ICON_UNDEFINED)
        .setDisplayName("停止播放")
        .setCustomIconResId(R.drawable.icon_player_stop)
        .setSessionCommand(CustomCommands.Stop)
        .setSlots(CommandButton.SLOT_BACK_SECONDARY)
        .build()

    fun build(mode: MusicPlayMode): List<CommandButton> {
        val buttons = mutableListOf<CommandButton>()
        buttons += when (mode) {
            MusicPlayMode.ORDER -> OrderMode
            MusicPlayMode.LOOP -> LoopMode
            MusicPlayMode.RANDOM -> RandomMode
        }
        buttons += Stop
        return buttons
    }
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
                setAvailableSessionCommands(CustomCommands.SessionCommands)
            }
        }.build()
    }

    override fun onCreate() {
        super.onCreate()

        val context = this
        val currentMode = MusicPlayMode.ORDER
        val keepFocus = true

        val ffmpegPlayer = FfmpegRenderersFactory.build(this, keepFocus)
        val forwardPlayer = ForwardPlayer(ffmpegPlayer, currentMode)
        player = ffmpegPlayer
        session = MediaSession.Builder(context, forwardPlayer)
            .setCallback(listener)
            .setMediaButtonPreferences(QTEButton.build(currentMode))
            .setSessionActivity(
                PendingIntent.getActivity(context, 0,
                    Intent().apply { setComponent(ComponentName(context, MainActivity::class.java)) },
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_IMMUTABLE else PendingIntent.FLAG_ONE_SHOT
                )
            )
            .build()
    }

    override fun onDestroy() {
        session?.release()
        player?.release()
        session = null
        player = null
        super.onDestroy()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        pauseAllPlayersAndStopSelf()
    }

    override fun onGetSession(controller: MediaSession.ControllerInfo): MediaSession? = session
}