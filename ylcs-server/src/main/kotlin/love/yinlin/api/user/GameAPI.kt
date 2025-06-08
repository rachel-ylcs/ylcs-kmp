package love.yinlin.api.user

import io.ktor.server.routing.Routing
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import love.yinlin.DB
import love.yinlin.api.API
import love.yinlin.api.APIConfig.coercePageNum
import love.yinlin.api.ImplMap
import love.yinlin.api.api
import love.yinlin.api.failedData
import love.yinlin.api.successData
import love.yinlin.data.Data
import love.yinlin.data.rachel.game.Game
import love.yinlin.data.rachel.game.GameDetails
import love.yinlin.data.rachel.game.GameResult
import love.yinlin.data.rachel.game.info.*
import love.yinlin.extension.to
import love.yinlin.extension.toJson
import love.yinlin.extension.toJsonString
import love.yinlin.throwExecuteSQL
import love.yinlin.throwInsertSQLGeneratedKey
import love.yinlin.updateSQL
import love.yinlin.values

private val Game.manager: GameManager get() = when (this) {
    Game.AnswerQuestion -> GameManager.Game1Manager
    Game.BlockText -> GameManager.Game2Manager
    Game.FlowersOrder -> GameManager.Game3Manager
    Game.SearchAll -> GameManager.Game4Manager
}

sealed interface GameManager {
    companion object {
        const val MAX_REWARD = 30
        const val MAX_RANK = 3
        const val MAX_COST_RATIO = MAX_REWARD / MAX_RANK

        fun checkReward(reward: Int, num: Int, cost: Int): Boolean = reward !in 1 .. MAX_REWARD && num !in 1 .. MAX_RANK &&
                (cost == 0 || cost !in 1 .. (reward / MAX_COST_RATIO).coerceAtLeast(1))
    }

    fun check(info: JsonElement, question: JsonElement, answer: JsonElement)
    fun start(uid: Int, details: GameDetails, userAnswer: JsonElement): Data<GameResult>

    data object Game1Manager : GameManager {
        const val MIN_THRESHOLD = 0.75f

        override fun check(info: JsonElement, question: JsonElement, answer: JsonElement) {
            val actualInfo = info.to<AQInfo>()
            require(actualInfo.threshold in MIN_THRESHOLD .. 1f)
            val actualQuestion = question.to<List<AQQuestion>>()
            val actualAnswer = answer.to<List<AQAnswer>>()
            require(actualQuestion.size == actualAnswer.size)
            for (i in actualQuestion.indices) actualAnswer[i].matchQuestion(actualQuestion[i])
        }

        fun verify(standardAnswer: JsonElement, userAnswer: JsonElement): AQResult {
            val standardAnswers = standardAnswer.to<List<AQAnswer>>()
            val userAnswers = userAnswer.to<List<AQUserAnswer>>()
            val size = standardAnswers.size
            require(size == userAnswers.size)
            var correctCount = 0
            repeat(size) { i ->
                if (userAnswers[i].verifyAnswer(standardAnswers[i])) ++correctCount
            }
            return AQResult(correctCount = correctCount, totalCount = size)
        }

        override fun start(uid: Int, details: GameDetails, userAnswer: JsonElement): Data<GameResult> {
            // 验证游戏结果
            val info = details.info.to<AQInfo>()
            val aqResult = verify(details.answer, userAnswer)
            // 计算奖励与名次
            val isCompleted = (aqResult.correctCount.toFloat() / aqResult.totalCount) >= info.threshold
            val rank = if (isCompleted) details.winner.size + 1 else 0
            val reward = if (isCompleted) details.reward / details.num else 0
            val result = GameResult(
                isCompleted = isCompleted,
                reward = reward,
                rank = rank,
                info = aqResult.toJson()
            )
            var data: Data<GameResult> = Data.Success(result)
            DB.throwTransaction {
                // 消费银币
                val cost = details.cost
                if (cost > 0) {
                    if (it.updateSQL("UPDATE user SET coin = coin - ? WHERE uid = ? AND coin >= ?", cost, uid, cost)) {
                        // 增加发起者银币
                        it.throwExecuteSQL("UPDATE user SET coin = coin + ? WHERE uid = ?", cost, details.uid)
                    }
                    else data = "没有足够的银币参与".failedData
                }
                // 插入游戏记录
                it.throwInsertSQLGeneratedKey("""
                    INSERT INTO game_record(gid, uid, answer, result) ${values(4)}
                """, details.gid, uid, userAnswer.toJsonString(), result.toJsonString())
                // 更新游戏排行榜
                if (isCompleted) {
                    val isGameCompleted = details.winner.size + 1 >= details.num
                    it.throwExecuteSQL("""
                        UPDATE game
                        SET winner = ? , isCompleted = ?
                        WHERE gid = ?
                    """, details.winner.plus(uid.toString()).toJsonString(), isGameCompleted, details.gid)
                }
            }
            return data
        }
    }

    data object Game2Manager : GameManager {
        override fun check(info: JsonElement, question: JsonElement, answer: JsonElement) {

        }

        fun verify(standardAnswer: JsonElement, userAnswer: JsonElement): JsonElement {
            return JsonNull
        }

        override fun start(uid: Int, details: GameDetails, userAnswer: JsonElement): Data<GameResult> {
            return Data.Error()
        }
    }

    data object Game3Manager : GameManager {
        override fun check(info: JsonElement, question: JsonElement, answer: JsonElement) {

        }

        fun verify(standardAnswer: JsonElement, userAnswer: JsonElement): JsonElement {
            return JsonNull
        }

        override fun start(uid: Int, details: GameDetails, userAnswer: JsonElement): Data<GameResult> {
            return Data.Error()
        }
    }

    data object Game4Manager : GameManager {
        override fun check(info: JsonElement, question: JsonElement, answer: JsonElement) {

        }

        fun verify(standardAnswer: JsonElement, userAnswer: JsonElement): JsonElement {
            return JsonNull
        }

        override fun start(uid: Int, details: GameDetails, userAnswer: JsonElement): Data<GameResult> {
            return Data.Error()
        }
    }
}

fun Routing.gameAPI(implMap: ImplMap) {
    api(API.User.Game.CreateGame) { (token, title, type, reward, num, cost, info, question, answer) ->
        val uid = AN.throwExpireToken(token)
        // 检查游戏奖励与名额
        VN.throwEmpty(title)
        VN.throwIf(!GameManager.checkReward(reward, num, cost))
        // 检查游戏数据配置
        try {
            type.manager.check(info, question, answer)
        } catch (_: Throwable) {
            return@api "数据配置非法".failedData
        }
        // 新增游戏行
        val gid = DB.throwInsertSQLGeneratedKey("""
            INSERT INTO game(uid, title, type, reward, num, cost, info, question, answer) ${values(9)}
        """, uid, title, type.ordinal, reward, num, cost,
            info.toJsonString(), question.toJsonString(), answer.toJsonString()
        ).toInt()
        Data.Success(gid)
    }

    api(API.User.Game.DeleteGame) { (token, gid) ->
        val uid = AN.throwExpireToken(token)
        VN.throwId(gid)
        DB.throwExecuteSQL("UPDATE game SET isDeleted = 1 WHERE gid = ? AND uid = ?", gid, uid)
        "删除成功".successData
    }

    api(API.User.Game.GetGames) { (type, gid, num) ->
        val games = DB.throwQuerySQL("""
            SELECT game.gid, user.name, game.ts, game.title, game.type, game.reward, game.num, game.cost, game.winner, game.info
            FROM game
            LEFT JOIN user ON game.uid = user.uid
            WHERE game.gid < ? AND game.isDeleted = 0 ${if(type != null) "AND game.type = ${type.ordinal}" else ""}
            ORDER BY game.gid DESC
            LIMIT ?
        """, gid, num.coercePageNum)
        Data.Success(games.to())
    }

    api(API.User.Game.GetUserGames) { (token, gid, isCompleted, num) ->
        val uid = AN.throwExpireToken(token)
        val games = DB.throwQuerySQL("""
            SELECT gid, uid, ts, title, type, reward, num, cost, winner, info, question, answer, isCompleted
            FROM game
            WHERE uid = ? AND isDeleted = 0 AND ${
                if (isCompleted) "isCompleted = 1 AND gid < ?"
                else "((isCompleted = 0 AND gid < ?) OR isCompleted = 1)"
            }
            ORDER BY isCompleted ASC, gid DESC
            LIMIT ?
        """, uid, gid, num.coercePageNum)
        Data.Success(games.to())
    }

    api(API.User.Game.GetUserGameRecords) { (token, rid, num) ->
        val uid = AN.throwExpireToken(token)
        val records = DB.throwQuerySQL("""
            SELECT record.rid, record.gid, record.ts, user.name, game.title, game.type, record.answer, record.result
            FROM game_record AS record
            LEFT JOIN game ON record.gid = game.gid
            LEFT JOIN user ON game.uid = user.uid
            WHERE record.uid = ? AND record.rid < ? AND game.isDeleted = 0
            ORDER BY record.rid DESC
            LIMIT ?
        """, uid, rid, num.coercePageNum)
        Data.Success(records.to())
    }

    api(API.User.Game.StartGame) { (token, gid, answer) ->
        val uid = AN.throwExpireToken(token)
        VN.throwId(gid)
        val gameDetails = DB.throwQuerySQLSingle("""
            SELECT gid, uid, ts, title, type, reward, num, cost, winner, info, question, answer, isCompleted
            FROM game
            WHERE gid = ? AND isDeleted = 0
        """, gid).to<GameDetails>()
        if (gameDetails.uid == uid) return@api "不能参与自己创建的游戏哦".failedData
        if (gameDetails.isCompleted) return@api "不能参与已经结算的游戏哦".failedData
        if (uid.toString() in gameDetails.winner) return@api "不能参与完成过的游戏哦".failedData
        gameDetails.type.manager.start(uid, gameDetails, answer)
    }
}