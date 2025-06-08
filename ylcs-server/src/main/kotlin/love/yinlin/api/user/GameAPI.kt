package love.yinlin.api.user

import io.ktor.server.routing.Routing
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import love.yinlin.DB
import love.yinlin.api.API
import love.yinlin.api.ImplMap
import love.yinlin.api.api
import love.yinlin.api.failedData
import love.yinlin.api.successData
import love.yinlin.data.Data
import love.yinlin.data.rachel.game.Game
import love.yinlin.data.rachel.game.GameDetails
import love.yinlin.data.rachel.game.info.*
import love.yinlin.extension.to
import love.yinlin.extension.toJson
import love.yinlin.extension.toJsonString
import love.yinlin.values

private val Game.manager: GameManager get() = when (this) {
    Game.AnswerQuestion -> GameManager.Game1Manager
    Game.BlockText -> GameManager.Game2Manager
    Game.FlowersOrder -> GameManager.Game3Manager
    Game.SearchAll -> GameManager.Game4Manager
}

sealed interface GameManager {
    fun check(info: JsonElement?, question: JsonElement, answer: JsonElement)
    fun verify(standardAnswer: JsonElement, userAnswer: JsonElement): JsonElement
    fun start(uid: Int, details: GameDetails): Data<JsonElement>

    data object Game1Manager : GameManager {
        override fun check(info: JsonElement?, question: JsonElement, answer: JsonElement) {
            val questions = question.to<List<AQQuestion>>()
            val answers = answer.to<List<AQAnswer>>()
            require(questions.size == answers.size)
            for (i in questions.indices) answers[i].matchQuestion(questions[i])
        }

        override fun verify(standardAnswer: JsonElement, userAnswer: JsonElement): JsonElement {
            val standardAnswers = standardAnswer.to<List<AQAnswer>>()
            val userAnswers = userAnswer.to<List<AQUserAnswer>>()
            val size = standardAnswers.size
            require(size == userAnswers.size)
            var correctCount = 0
            repeat(size) { i ->
                if (userAnswers[i].verifyAnswer(standardAnswers[i])) ++correctCount
            }
            return AQResult(
                isCompleted = correctCount == size,
                correctCount = correctCount,
                totalCount = size
            ).toJson()
        }

        override fun start(uid: Int, details: GameDetails): Data<JsonElement> {
            return Data.Error()
        }
    }

    data object Game2Manager : GameManager {
        override fun check(info: JsonElement?, question: JsonElement, answer: JsonElement) {

        }

        override fun verify(standardAnswer: JsonElement, userAnswer: JsonElement): JsonElement {
            return JsonNull
        }

        override fun start(uid: Int, details: GameDetails): Data<JsonElement> {
            return Data.Error()
        }
    }

    data object Game3Manager : GameManager {
        override fun check(info: JsonElement?, question: JsonElement, answer: JsonElement) {

        }

        override fun verify(standardAnswer: JsonElement, userAnswer: JsonElement): JsonElement {
            return JsonNull
        }

        override fun start(uid: Int, details: GameDetails): Data<JsonElement> {
            return Data.Error()
        }
    }

    data object Game4Manager : GameManager {
        override fun check(info: JsonElement?, question: JsonElement, answer: JsonElement) {

        }

        override fun verify(standardAnswer: JsonElement, userAnswer: JsonElement): JsonElement {
            return JsonNull
        }

        override fun start(uid: Int, details: GameDetails): Data<JsonElement> {
            return Data.Error()
        }
    }
}

fun Routing.gameAPI(implMap: ImplMap) {
    api(API.User.Game.CreateGame) { (token, title, type, reward, num, info, question, answer) ->
        val uid = AN.throwExpireToken(token)
        // 检查游戏奖励与名额
        VN.throwEmpty(title)
        VN.throwIf(reward !in 1 .. 20, num !in 1 .. 3)
        // 检查游戏数据配置
        try { type.manager.check(info, question, answer) } catch (_: Throwable) { return@api "数据配置非法".failedData }
        // 新增游戏行
        val gid = DB.throwInsertSQLGeneratedKey("""
            INSERT INTO game(uid, title, reward, type, num, info, question, answer) ${values(8)}
        """, uid, title, reward, type, num, info?.toJsonString(), question.toJsonString(), answer.toJsonString()).toInt()
        Data.Success(gid)
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

    api(API.User.Game.GetUserGameRecords) { (token, rid, num) ->
        Data.Success(emptyList())
    }

    api(API.User.Game.GetGameRecordDetails) { (token, rid) ->
        Data.Error()
    }

    api(API.User.Game.StartGame) { (token, gid, answer) ->
        val uid = AN.throwExpireToken(token)
        VN.throwId(gid)
        val gameDetails = DB.throwQuerySQLSingle("""
            SELECT gid, uid, '' AS name, ts, title, reward, type, num, winner, info, question, answer, isCompleted
            FROM game
            WHERE gid = ? AND isDeleted = 0
        """).to<GameDetails>()
        if (gameDetails.uid == uid) return@api "不能参与自己创建的游戏哦".failedData
        if (gameDetails.isCompleted) return@api "不能参与已经结算的游戏哦".failedData
        if (uid.toString() in gameDetails.winners) return@api "不能参与完成过的游戏哦".failedData
        gameDetails.type.manager.start(uid, gameDetails)
    }
}