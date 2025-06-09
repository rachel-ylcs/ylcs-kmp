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
import love.yinlin.data.rachel.game.PreflightResult
import love.yinlin.data.rachel.game.info.*
import love.yinlin.extension.Int
import love.yinlin.extension.Long
import love.yinlin.extension.String
import love.yinlin.extension.to
import love.yinlin.extension.toJson
import love.yinlin.extension.toJsonString
import love.yinlin.throwExecuteSQL
import love.yinlin.throwInsertSQLGeneratedKey
import love.yinlin.updateSQL
import love.yinlin.values
import kotlin.math.sqrt

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
    fun preflight(uid: Int, details: GameDetails): PreflightResult
    fun start(uid: Int, details: GameDetails, userAnswer: JsonElement): Data<GameResult>
    fun uploadResult(uid: Int, details: GameDetails, answer: JsonElement, isCompleted: Boolean, info: JsonElement): Data<GameResult>

    // 排位
    abstract class RankGameManager : GameManager {
        override fun preflight(uid: Int, details: GameDetails): PreflightResult = PreflightResult()

        override fun uploadResult(uid: Int, details: GameDetails, answer: JsonElement, isCompleted: Boolean, info: JsonElement): Data<GameResult> {
            return DB.throwTransaction {
                // 消费银币
                val cost = details.cost
                if (cost > 0) {
                    if (it.updateSQL("UPDATE user SET coin = coin - ? WHERE uid = ? AND coin >= ?", cost, uid, cost)) {
                        // 增加发起者银币
                        it.throwExecuteSQL("UPDATE user SET coin = coin + ? WHERE uid = ?", cost, details.uid)
                    }
                    else return@throwTransaction "没有足够的银币参与".failedData
                }
                // 插入游戏记录
                it.throwInsertSQLGeneratedKey("""
                    INSERT INTO game_record(gid, uid, answer, result) ${values(4)}
                """, details.gid, uid, answer.toJsonString(), info.toJsonString())
                // 更新游戏排行榜
                if (isCompleted) {
                    val isGameCompleted = details.winner.size + 1 >= details.num
                    it.throwExecuteSQL("""
                        UPDATE game
                        SET winner = ? , isCompleted = ?
                        WHERE gid = ?
                    """, details.winner.plus(uid.toString()).toJsonString(), isGameCompleted, details.gid)
                }
                Data.Success(GameResult(
                    isCompleted = isCompleted,
                    reward = if (isCompleted) details.reward / details.num else 0,
                    rank = if (isCompleted) details.winner.size + 1 else 0,
                    info = info
                ))
            }
        }
    }

    // 探索
    abstract class ExplorationGameManager : GameManager {
        open val minTryCount: Int = 3 // 最小尝试次数
        open val maxTryCount: Int = 10 // 最大尝试次数

        abstract fun fetchTryCount(info: JsonElement): Int

        override fun preflight(uid: Int, details: GameDetails): PreflightResult {
            // 检查尝试记录
            val oldRecord = DB.querySQLSingle("""
                SELECT rid, answer, result
                FROM game_record
                WHERE uid = ? AND gid = ?
            """, uid, details.gid)
            val oldAnswer = oldRecord?.get("answer")?.to<List<JsonElement>>() ?: emptyList()
            return if (oldAnswer.size >= fetchTryCount(details.info)) PreflightResult("尝试次数达到上限") else PreflightResult()
        }

        override fun uploadResult(uid: Int, details: GameDetails, answer: JsonElement, isCompleted: Boolean, info: JsonElement): Data<GameResult> {
            return DB.throwTransaction {
                // 检查尝试记录
                val oldRecord = DB.querySQLSingle("""
                    SELECT rid, answer, result
                    FROM game_record
                    WHERE uid = ? AND gid = ?
                """, uid, details.gid)
                val rid = oldRecord?.get("rid")?.Long
                val oldAnswer = oldRecord?.get("answer")?.to<MutableList<JsonElement>>() ?: mutableListOf()
                val oldResult = oldRecord?.get("result")?.to<MutableList<JsonElement>>() ?: mutableListOf()
                if (oldAnswer.size >= fetchTryCount(details.info)) return@throwTransaction "尝试次数达到上限".failedData
                // 消费银币
                val cost = details.cost
                // 只有第一次尝试消耗银币
                if (cost > 0 && rid == null) {
                    if (it.updateSQL("UPDATE user SET coin = coin - ? WHERE uid = ? AND coin >= ?", cost, uid, cost)) {
                        // 增加发起者银币
                        it.throwExecuteSQL("UPDATE user SET coin = coin + ? WHERE uid = ?", cost, details.uid)
                    }
                    else return@throwTransaction "没有足够的银币参与".failedData
                }
                // 插入游戏记录
                oldAnswer += answer
                oldResult += info
                if (rid == null) {
                    it.throwInsertSQLGeneratedKey("""
                        INSERT INTO game_record(gid, uid, answer, result) ${values(4)}
                    """, details.gid, uid, oldAnswer.toJsonString(), oldResult.toJsonString())
                }
                else {
                    it.throwExecuteSQL("""
                        UPDATE game_record
                        SET answer = ? , result = ? , ts = CURRENT_TIMESTAMP
                        WHERE rid = ?
                    """, oldAnswer.toJsonString(), oldResult.toJsonString(), rid)
                }
                // 更新游戏排行榜
                if (isCompleted) {
                    val isGameCompleted = details.winner.size + 1 >= details.num
                    it.throwExecuteSQL("""
                        UPDATE game
                        SET winner = ? , isCompleted = ?
                        WHERE gid = ?
                    """, details.winner.plus(uid.toString()).toJsonString(), isGameCompleted, details.gid)
                }
                Data.Success(GameResult(
                    isCompleted = isCompleted,
                    reward = if (isCompleted) details.reward / details.num else 0,
                    rank = if (isCompleted) details.winner.size + 1 else 0,
                    info = info
                ))
            }
        }
    }

    // 竞速
    abstract class SpeedGameManager : RankGameManager() {

    }

    // 答题
    data object Game1Manager : RankGameManager() {
        const val MIN_THRESHOLD = 0.75f // 最小成功阈值

        override fun check(info: JsonElement, question: JsonElement, answer: JsonElement) {
            val actualInfo = info.to<AQInfo>()
            val actualQuestion = question.to<List<AQQuestion>>()
            val actualAnswer = answer.to<List<AQAnswer>>()
            require(actualInfo.threshold in MIN_THRESHOLD .. 1f)
            require(actualQuestion.size == actualAnswer.size)
            for (i in actualQuestion.indices) actualAnswer[i].matchQuestion(actualQuestion[i])
        }

        private fun verify(standardAnswer: JsonElement, userAnswer: JsonElement): AQResult {
            val actualStandardAnswer = standardAnswer.to<List<AQAnswer>>()
            val actualUserAnswer = userAnswer.to<List<AQUserAnswer>>()
            val size = actualStandardAnswer.size
            require(size == actualUserAnswer.size)
            var correctCount = 0
            repeat(size) { i ->
                if (actualUserAnswer[i].verifyAnswer(actualStandardAnswer[i])) ++correctCount
            }
            return AQResult(correctCount = correctCount, totalCount = size)
        }

        override fun start(uid: Int, details: GameDetails, userAnswer: JsonElement): Data<GameResult> {
            val info = details.info.to<AQInfo>()
            val aqResult = verify(details.answer, userAnswer)
            return uploadResult(
                uid = uid,
                details = details,
                answer = userAnswer,
                isCompleted = (aqResult.correctCount.toFloat() / aqResult.totalCount) >= info.threshold,
                info = aqResult.toJson()
            )
        }
    }

    // 网格填词
    data object Game2Manager : RankGameManager() {
        const val CHAR_EMPTY = '$' // 空字符
        const val CHAR_BLOCK = '#' // 方格字符
        const val MIN_BLOCK_SIZE = 7 // 最小网格大小
        const val MAX_BLOCK_SIZE = 12 // 最大网格大小

        override fun check(info: JsonElement, question: JsonElement, answer: JsonElement) {
            val actualQuestion = question.String
            val actualAnswer = answer.String
            val blockSize = sqrt(actualQuestion.length.toFloat()).toInt()
            // 网格大小是完全平方数
            require(blockSize in MIN_BLOCK_SIZE .. MAX_BLOCK_SIZE)
            require(blockSize * blockSize == actualQuestion.length)
            // 题目与答案大小相同
            require(blockSize == actualAnswer.length)
            // 题目中至少包含一个方格
            require(actualQuestion.contains(CHAR_BLOCK))
            // 答案中不能包含方格且至少包含一个非空
            require(!actualAnswer.contains(CHAR_BLOCK) && !actualAnswer.all { it == CHAR_EMPTY })
            for (index in actualQuestion.indices) {
                val q = actualQuestion[index]
                val a = actualAnswer[index]
                when (q) {
                    // 题目空则答案空
                    CHAR_EMPTY -> require(a == CHAR_EMPTY)
                    // 题目方格则答案非空
                    CHAR_BLOCK -> require(a != CHAR_EMPTY)
                    // 否则题目和答案一致
                    else -> require(a == q)
                }
            }
        }

        private fun verify(standardAnswer: JsonElement, userAnswer: JsonElement): BTResult {
            val actualStandardAnswer = standardAnswer.String
            val actualUserAnswer = userAnswer.String
            require(actualStandardAnswer.length == actualUserAnswer.length)
            var correctCount = 0
            for (i in actualStandardAnswer.indices) {
                val ch1 = actualStandardAnswer[i]
                val ch2 = actualUserAnswer[i]
                if (ch1 == ch2) ++correctCount
            }
            return BTResult(correctCount = correctCount, totalCount = actualStandardAnswer.length)
        }

        override fun start(uid: Int, details: GameDetails, userAnswer: JsonElement): Data<GameResult> {
            val btResult = verify(details.answer, userAnswer)
            return uploadResult(
                uid = uid,
                details = details,
                answer = userAnswer,
                isCompleted = btResult.correctCount == btResult.totalCount,
                info = btResult.toJson()
            )
        }
    }

    // 寻花令
    data object Game3Manager : ExplorationGameManager() {
        const val MIN_LENGTH = 10 // 最小长度
        const val MAX_LENGTH = 14 // 最大长度

        override fun fetchTryCount(info: JsonElement): Int = info.to<FOInfo>().tryCount

        override fun check(info: JsonElement, question: JsonElement, answer: JsonElement) {
            val actualInfo = info.to<FOInfo>()
            val actualQuestion = question.Int
            val actualAnswer = answer.String
            // 尝试次数
            require(actualInfo.tryCount in minTryCount .. maxTryCount)
            // 问题长度
            require(actualQuestion in MIN_LENGTH .. MAX_LENGTH)
            // 答案长度与问题一致
            require(actualAnswer.length == actualQuestion)
        }

        private fun verify(standardAnswer: JsonElement, userAnswer: JsonElement): Int {
            val actualStandardAnswer = standardAnswer.String
            val actualUserAnswer = userAnswer.String
            require(actualStandardAnswer.length == actualUserAnswer.length)
            val items = List(actualStandardAnswer.length) { i ->
                val ch1 = actualStandardAnswer[i]
                val ch2 = actualUserAnswer[i]
                if (ch1 == ch2) FOType.CORRECT
                else if (actualUserAnswer.contains(ch1)) FOType.INVALID_POS
                else FOType.INCORRECT
            }
            return FOType.encode(MIN_LENGTH, items)
        }

        override fun start(uid: Int, details: GameDetails, userAnswer: JsonElement): Data<GameResult> {
            val foResult = verify(details.answer, userAnswer)
            return uploadResult(
                uid = uid,
                details = details,
                answer = userAnswer,
                isCompleted = FOType.verify(foResult),
                info = foResult.toJson()
            )
        }
    }

    // 词寻
    data object Game4Manager : SpeedGameManager() {
        override fun check(info: JsonElement, question: JsonElement, answer: JsonElement) {

        }

        private fun verify(standardAnswer: JsonElement, userAnswer: JsonElement): JsonElement {
            return JsonNull
        }

        override fun start(uid: Int, details: GameDetails, userAnswer: JsonElement): Data<GameResult> {
            return Data.Error()
        }
    }
}

private data class PreflightInfo(
    val uid: Int,
    val details: GameDetails,
    val result: PreflightResult
)

private fun preflightGame(token: String, gid: Int): PreflightInfo {
    val uid = AN.throwExpireToken(token)
    VN.throwId(gid)
    val gameDetails = DB.throwQuerySQLSingle("""
        SELECT gid, uid, ts, title, type, reward, num, cost, winner, info, question, answer, isCompleted
        FROM game
        WHERE gid = ? AND isDeleted = 0
    """, gid).to<GameDetails>()
    val result = if (gameDetails.uid == uid) PreflightResult("不能参与自己创建的游戏哦")
        else if (gameDetails.isCompleted) PreflightResult("不能参与已经结算的游戏哦")
        else if (uid.toString() in gameDetails.winner) PreflightResult("不能参与完成过的游戏哦")
        else gameDetails.type.manager.preflight(uid, gameDetails)
    return PreflightInfo(uid, gameDetails, result)
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

    api(API.User.Game.PreflightGame) { (token, gid) ->
        Data.Success(preflightGame(token, gid).result)
    }

    api(API.User.Game.StartGame) { (token, gid, answer) ->
        val info = preflightGame(token, gid)
        info.details.type.manager.start(info.uid, info.details, answer)
    }
}