package love.yinlin.data.rachel.sockets

import androidx.compose.runtime.Stable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import love.yinlin.Local
import love.yinlin.api.ServerRes
import love.yinlin.api.Sockets

object LyricsSockets : Sockets("/lyricsGame", "歌词默写") {
    const val QUESTION_COUNT = 10
    const val INVITE_TIME = 15 * 1000L
    const val PLAYING_TIME = 5 * 60 * 1000L

    @Stable
    @Serializable
    data class PlayerInfo(val uid: Int, val name: String) {
        val avatarPath: String by lazy { "${Local.API_BASE_URL}/${ServerRes.Users.User(uid).avatar}" }
    }

    @Stable
    @Serializable
    data class GameResult(val player: PlayerInfo, val count: Int, val duration: Long)

    @Serializable
    sealed interface CM {
        @Serializable
        @SerialName("Login")
        data class Login(val info: PlayerInfo) : CM
        @Serializable
        @SerialName("GetPlayers")
        data object GetPlayers : CM
        @Serializable
        @SerialName("InvitePlayer")
        data class InvitePlayer(val targetUid: Int) : CM
        @Serializable
        @SerialName("InviteResponse")
        data class InviteResponse(val inviterUid: Int, val accept: Boolean) : CM
        @Serializable
        @SerialName("SaveAnswer")
        data class SaveAnswer(val index: Int, val answer: String) : CM
        @Serializable
        @SerialName("Submit")
        data object Submit : CM
    }

    @Serializable
    sealed interface SM {
        @Serializable
        @SerialName("Error")
        data class Error(val message: String) : SM
        @Serializable
        @SerialName("PlayerList")
        data class PlayerList(val players: List<PlayerInfo>) : SM
        @Serializable
        @SerialName("InviteReceived")
        data class InviteReceived(val player: PlayerInfo) : SM
        @Serializable
        @SerialName("InviteResult")
        data class InviteResult(val accepted: Boolean) : SM
        @Serializable
        @SerialName("GameStart")
        data class GameStart(val player1: PlayerInfo, val player2: PlayerInfo, val startTime: Long, val questions: List<String>) : SM
        @Serializable
        @SerialName("OtherAnswerUpdated")
        data class OtherAnswerUpdated(val count: Int) : SM
        @Serializable
        @SerialName("SendResult")
        data class SendResult(val result1: GameResult, val result2: GameResult) : SM
    }
}