@file:OptIn(UnstableApi::class)
package love.yinlin.service

import android.app.PendingIntent
import android.content.ComponentName
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.annotation.OptIn
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.CommandButton
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionError
import androidx.media3.session.SessionResult
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import love.yinlin.MainActivity
import love.yinlin.R
import love.yinlin.common.FfmpegRenderersFactory
import love.yinlin.data.music.MusicPlayMode
import love.yinlin.platform.ForwardPlayer
import love.yinlin.platform.mergePlayMode
import love.yinlin.service

object CustomCommands {
    object Args {
        const val SET_MODE_ARG_MODE = "SET_MODE_ARG_MODE"
    }

    val SetMode = SessionCommand("SetMode", Bundle.EMPTY)
    val Stop = SessionCommand("Stop", Bundle.EMPTY)

    val NotificationPlayerCommands get() = MediaSession.ConnectionResult.DEFAULT_PLAYER_COMMANDS.buildUpon()
        .build()

    val SessionCommands get() = MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS.buildUpon()
        .add(SetMode)
        .add(Stop)
        .build()

    val OrderModeButton = CommandButton.Builder(CommandButton.ICON_UNDEFINED)
        .setDisplayName("顺序播放")
        .setCustomIconResId(R.drawable.icon_player_mode_order)
        .setSessionCommand(SetMode)
        .setSlots(CommandButton.SLOT_FORWARD_SECONDARY)
        .build()

    val LoopModeButton = CommandButton.Builder(CommandButton.ICON_UNDEFINED)
        .setDisplayName("单曲循环")
        .setCustomIconResId(R.drawable.icon_player_mode_loop)
        .setSessionCommand(SetMode)
        .setSlots(CommandButton.SLOT_FORWARD_SECONDARY)
        .build()

    val RandomModeButton = CommandButton.Builder(CommandButton.ICON_UNDEFINED)
        .setDisplayName("随机播放")
        .setCustomIconResId(R.drawable.icon_player_mode_random)
        .setSessionCommand(SetMode)
        .setSlots(CommandButton.SLOT_FORWARD_SECONDARY)
        .build()

    val StopButton = CommandButton.Builder(CommandButton.ICON_UNDEFINED)
        .setDisplayName("停止播放")
        .setCustomIconResId(R.drawable.icon_player_stop)
        .setSessionCommand(Stop)
        .setSlots(CommandButton.SLOT_BACK_SECONDARY)
        .build()

    fun prepareButtons(mode: MusicPlayMode): List<CommandButton> = buildList {
        add(when (mode) {
            MusicPlayMode.ORDER -> OrderModeButton
            MusicPlayMode.LOOP -> LoopModeButton
            MusicPlayMode.RANDOM -> RandomModeButton
        })
        add(StopButton)
    }
}

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
                    else MusicPlayMode.fromInt(args.getInt(CustomCommands.Args.SET_MODE_ARG_MODE)) ?: MusicPlayMode.ORDER
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

        val ffmpegPlayer = FfmpegRenderersFactory.build(this, service.config.audioFocus)
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