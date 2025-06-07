package love.yinlin.api.user

import io.ktor.server.routing.Routing
import kotlinx.serialization.json.JsonNull
import love.yinlin.api.API
import love.yinlin.api.ImplMap
import love.yinlin.api.api
import love.yinlin.api.successData
import love.yinlin.data.Data
import love.yinlin.data.rachel.game.Game

sealed interface GameManager {
    companion object {
        val Game.manager: GameManager get() = when (this) {
            Game.AnswerQuestion -> Game1Manager
            Game.BlockText -> Game2Manager
            Game.FlowersOrder -> Game3Manager
            Game.SearchAll -> Game4Manager
        }
    }

    data object Game1Manager : GameManager {

    }

    data object Game2Manager : GameManager {

    }

    data object Game3Manager : GameManager {

    }

    data object Game4Manager : GameManager {

    }
}

fun Routing.gameAPI(implMap: ImplMap) {
    api(API.User.Game.CreateGame) { obj ->
        Data.Success(0)
    }

    api(API.User.Game.DeleteGame) { (token, gid) ->
        "".successData
    }

    api(API.User.Game.GetGames) { (type, gid, num) ->
        Data.Success(emptyList())
    }

    api(API.User.Game.GetUserGames) { (token, gid, isCompleted, num) ->
        Data.Success(emptyList())
    }

    api(API.User.Game.GetUserGameRecords) { (token, gid, isCompleted, num) ->
        Data.Success(emptyList())
    }

    api(API.User.Game.StartGame) { (token, gid, answer) ->
        Data.Success(JsonNull)
    }
}