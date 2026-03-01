package love.yinlin.media

import android.os.Bundle
import androidx.annotation.OptIn
import androidx.compose.runtime.Stable
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.CommandButton
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionCommands
import love.yinlin.compose.components.media.R
import love.yinlin.compose.data.media.MediaPlayMode

@Stable
@OptIn(UnstableApi::class)
object MediaCommands {
    object Args {
        const val SET_MODE_ARG_MODE = "SET_MODE_ARG_MODE"
    }

    val SetMode = SessionCommand("SetMode", Bundle.EMPTY)
    val Stop = SessionCommand("Stop", Bundle.EMPTY)

    val NotificationPlayerCommands: Player.Commands get() = MediaSession.ConnectionResult.DEFAULT_PLAYER_COMMANDS.buildUpon()
        .build()

    val SessionCommands: SessionCommands get() = MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS.buildUpon()
        .add(SetMode)
        .add(Stop)
        .build()

    val OrderModeButton: CommandButton = CommandButton.Builder(CommandButton.ICON_UNDEFINED)
        .setDisplayName("顺序播放")
        .setCustomIconResId(R.drawable.icon_player_mode_order)
        .setSessionCommand(SetMode)
        .setSlots(CommandButton.SLOT_FORWARD_SECONDARY)
        .build()

    val LoopModeButton: CommandButton = CommandButton.Builder(CommandButton.ICON_UNDEFINED)
        .setDisplayName("单曲循环")
        .setCustomIconResId(R.drawable.icon_player_mode_loop)
        .setSessionCommand(SetMode)
        .setSlots(CommandButton.SLOT_FORWARD_SECONDARY)
        .build()

    val RandomModeButton: CommandButton = CommandButton.Builder(CommandButton.ICON_UNDEFINED)
        .setDisplayName("随机播放")
        .setCustomIconResId(R.drawable.icon_player_mode_random)
        .setSessionCommand(SetMode)
        .setSlots(CommandButton.SLOT_FORWARD_SECONDARY)
        .build()

    val StopButton: CommandButton = CommandButton.Builder(CommandButton.ICON_UNDEFINED)
        .setDisplayName("停止播放")
        .setCustomIconResId(R.drawable.icon_player_stop)
        .setSessionCommand(Stop)
        .setSlots(CommandButton.SLOT_BACK_SECONDARY)
        .build()

    fun prepareButtons(mode: MediaPlayMode): List<CommandButton> = buildList {
        add(when (mode) {
            MediaPlayMode.Order -> OrderModeButton
            MediaPlayMode.Loop -> LoopModeButton
            MediaPlayMode.Random -> RandomModeButton
        })
        add(StopButton)
    }
}