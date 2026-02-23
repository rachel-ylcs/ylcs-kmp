package love.yinlin.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Brush
import love.yinlin.common.game.AnswerQuestionMapper
import love.yinlin.common.game.BlockTextMapper
import love.yinlin.common.game.FlowersOrderMapper
import love.yinlin.common.game.GuessLyricsMapper
import love.yinlin.common.game.PictionaryMapper
import love.yinlin.common.game.RhymeMapper
import love.yinlin.common.game.SearchAllMapper
import love.yinlin.compose.Theme
import love.yinlin.compose.screen.BasicScreen
import love.yinlin.compose.screen.SubScreen
import love.yinlin.compose.ui.icon.Icons
import love.yinlin.data.rachel.game.Game
import love.yinlin.data.rachel.game.GameType
import love.yinlin.data.rachel.profile.UserProfile
import love.yinlin.screen.ScreenGameHall

@Stable
abstract class GameMapper {
    open val gameCreator: ((BasicScreen) -> CreateGameState)? = null
    open val gamePlayer: ((BasicScreen) -> PlayGameState)? = null

    open val useRanking: Boolean = true

    open fun SubScreen.startGame(game: Game, profile: UserProfile) {
        navigate(::ScreenGameHall, game)
    }

    companion object {
        val Groups = Game.entries.groupBy { it.type }

        // 使用when是为了利用enum域穷尽检查，实际查找仍然是基于预先associateWith的Map
        val TypeIcons = GameType.entries.associateWith {
            when (it) {
                GameType.RANK -> Icons.Leaderboard
                GameType.EXPLORATION -> Icons.RocketLaunch
                GameType.SPEED -> Icons.Timer
                GameType.SINGLE -> Icons.Person
                GameType.BATTLE -> Icons.Sword
            }
        }

        @Composable
        fun rememberTypeBrush(type: GameType): Brush {
            val startColor = when (type) {
                GameType.RANK -> Theme.color.primaryContainer
                GameType.EXPLORATION, GameType.SPEED -> Theme.color.secondaryContainer
                GameType.SINGLE, GameType.BATTLE -> Theme.color.tertiaryContainer
            }
            val endColor = when (type) {
                GameType.RANK -> Theme.color.primary
                GameType.EXPLORATION, GameType.SPEED -> Theme.color.secondary
                GameType.SINGLE, GameType.BATTLE -> Theme.color.tertiary
            }
            return remember(type) { Brush.horizontalGradient(listOf(startColor, endColor)) }
        }

        inline fun <reified T> cast(game: Game): T? = when (game) {
            Game.AnswerQuestion -> AnswerQuestionMapper as? T
            Game.BlockText -> BlockTextMapper as? T
            Game.FlowersOrder -> FlowersOrderMapper as? T
            Game.SearchAll -> SearchAllMapper as? T
            Game.GuessLyrics -> GuessLyricsMapper as? T
            Game.Pictionary -> PictionaryMapper as? T
            Game.Rhyme -> RhymeMapper as? T
        }
    }
}