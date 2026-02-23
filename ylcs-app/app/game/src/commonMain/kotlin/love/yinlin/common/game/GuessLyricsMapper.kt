package love.yinlin.common.game

import androidx.compose.runtime.Stable
import love.yinlin.common.CreateGameState
import love.yinlin.common.GameMapper
import love.yinlin.compose.screen.BasicScreen
import love.yinlin.compose.screen.SubScreen
import love.yinlin.data.rachel.game.Game
import love.yinlin.data.rachel.profile.UserProfile
import love.yinlin.screen.ScreenGuessLyrics

@Stable
object GuessLyricsMapper : GameMapper() {
    override val gameCreator: ((BasicScreen) -> CreateGameState)? = null

    override fun SubScreen.startGame(game: Game, profile: UserProfile) {
        navigate(::ScreenGuessLyrics, profile.uid, profile.name)
    }
}