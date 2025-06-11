package love.yinlin.api.user

import io.ktor.server.routing.Routing
import kotlinx.serialization.json.JsonElement
import love.yinlin.DB
import love.yinlin.api.API
import love.yinlin.api.APIConfig.coercePageNum
import love.yinlin.api.ImplMap
import love.yinlin.api.api
import love.yinlin.api.failedData
import love.yinlin.api.successData
import love.yinlin.data.Data
import love.yinlin.data.rachel.game.ExplorationConfig
import love.yinlin.data.rachel.game.Game
import love.yinlin.data.rachel.game.GameConfig
import love.yinlin.data.rachel.game.GameDetails
import love.yinlin.data.rachel.game.GameResult
import love.yinlin.data.rachel.game.PreflightResult
import love.yinlin.data.rachel.game.RankConfig
import love.yinlin.data.rachel.game.SpeedConfig
import love.yinlin.data.rachel.game.SpeedGameAnswer
import love.yinlin.data.rachel.game.info.*
import love.yinlin.extension.Array
import love.yinlin.extension.Int
import love.yinlin.extension.Long
import love.yinlin.extension.String
import love.yinlin.extension.makeArray
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
    val config: GameConfig get() = GameConfig
    fun check(info: JsonElement, question: JsonElement, answer: JsonElement)
    fun preflight(uid: Int, details: GameDetails): PreflightResult
    fun start(uid: Int, details: GameDetails, userAnswer: JsonElement): Data<GameResult>
    fun uploadResult(uid: Int, details: GameDetails, answer: JsonElement, isCompleted: Boolean, info: JsonElement): Data<GameResult>

    // 排位
    abstract class RankGameManager : GameManager {
        override val config: RankConfig = RankConfig

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
                Data.Success(GameResult.Settlement(
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
        override val config: ExplorationConfig = ExplorationConfig

        abstract fun fetchTryCount(info: JsonElement): Int

        override fun check(info: JsonElement, question: JsonElement, answer: JsonElement) {
            val tryCount = fetchTryCount(info)
            require(tryCount in config.minTryCount .. config.maxTryCount)
        }

        override fun preflight(uid: Int, details: GameDetails): PreflightResult {
            // 检查尝试记录
            val oldRecord = DB.querySQLSingle("""
                SELECT rid, answer, result
                FROM game_record
                WHERE uid = ? AND gid = ?
            """, uid, details.gid)
            val oldAnswer = oldRecord?.get("answer")?.Array ?: makeArray { }
            return if (oldAnswer.size >= fetchTryCount(details.info)) PreflightResult("尝试次数达到上限") else PreflightResult(oldAnswer)
        }

        override fun uploadResult(uid: Int, details: GameDetails, answer: JsonElement, isCompleted: Boolean, info: JsonElement): Data<GameResult> {
            // 检查尝试记录
            val oldRecord = DB.querySQLSingle("""
                SELECT rid, answer, result
                FROM game_record
                WHERE uid = ? AND gid = ?
            """, uid, details.gid)
            val rid = oldRecord?.get("rid")?.Long
            val oldAnswer = oldRecord?.get("answer")?.to<MutableList<JsonElement>>() ?: mutableListOf()
            val oldResult = oldRecord?.get("result")?.to<MutableList<JsonElement>>() ?: mutableListOf()
            return if (oldAnswer.size >= fetchTryCount(details.info)) "尝试次数达到上限".failedData
            else DB.throwTransaction {
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
                Data.Success(GameResult.Settlement(
                    isCompleted = isCompleted,
                    reward = if (isCompleted) details.reward / details.num else 0,
                    rank = if (isCompleted) details.winner.size + 1 else 0,
                    info = info
                ))
            }
        }
    }

    // 竞速
    abstract class SpeedGameManager : GameManager {
        override val config: SpeedConfig = SpeedConfig

        abstract fun fetchTimeLimit(info: JsonElement): Int

        override fun check(info: JsonElement, question: JsonElement, answer: JsonElement) {
            val timeLimit = fetchTimeLimit(info)
            require(timeLimit in config.minTimeLimit .. config.maxTimeLimit)
        }

        override fun preflight(uid: Int, details: GameDetails): PreflightResult = PreflightResult()

        protected fun time(uid: Int, details: GameDetails, answer: JsonElement): Data<SpeedGameAnswer> {
            val speedGameAnswer = answer.to<SpeedGameAnswer>()
            if (speedGameAnswer.start) { // 计时开始
                val currentTime = System.currentTimeMillis() // 修正时间
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
                    // 生成初始记录
                    val rid = it.throwInsertSQLGeneratedKey("""
                        INSERT INTO game_record(gid, uid, answer, result) ${values(4)}
                    """, details.gid, uid, speedGameAnswer.copy(ts = currentTime).toJsonString(), Unit.toJsonString())
                    Data.Success(speedGameAnswer.copy(rid = rid, ts = currentTime))
                }
            }
            else return Data.Success(speedGameAnswer) // 计时结束
        }

        override fun uploadResult(uid: Int, details: GameDetails, answer: JsonElement, isCompleted: Boolean, info: JsonElement): Data<GameResult> {
            val speedGameAnswer = answer.to<SpeedGameAnswer>()
            require(!speedGameAnswer.start)
            VN.throwId(speedGameAnswer.rid)
            return DB.throwTransaction {
                // 更新游戏记录
                it.throwExecuteSQL("""
                    UPDATE game_record
                    SET answer = ? , result = ? , ts = CURRENT_TIMESTAMP
                    WHERE uid = ? AND gid = ? AND rid = ?
                """, speedGameAnswer.toJsonString(), info.toJsonString(), uid, details.gid, speedGameAnswer.rid)
                // 更新游戏排行榜
                if (isCompleted) {
                    val isGameCompleted = details.winner.size + 1 >= details.num
                    it.throwExecuteSQL("""
                        UPDATE game
                        SET winner = ? , isCompleted = ?
                        WHERE gid = ?
                    """, details.winner.plus(uid.toString()).toJsonString(), isGameCompleted, details.gid)
                }
                Data.Success(GameResult.Settlement(
                    isCompleted = isCompleted,
                    reward = if (isCompleted) details.reward / details.num else 0,
                    rank = if (isCompleted) details.winner.size + 1 else 0,
                    info = info
                ))
            }
        }
    }

    // 答题
    data object Game1Manager : RankGameManager() {
        override val config: AQConfig = AQConfig

        override fun check(info: JsonElement, question: JsonElement, answer: JsonElement) {
            val actualInfo = info.to<AQInfo>()
            val actualQuestion = question.to<List<AQQuestion>>()
            val actualAnswer = answer.to<List<AQAnswer>>()
            require(actualInfo.threshold in config.minThreshold .. config.maxThreshold)
            require(actualQuestion.size in config.minQuestionCount .. config.maxQuestionCount)
            require(actualQuestion.size == actualAnswer.size)
            for (i in actualQuestion.indices) actualAnswer[i].matchQuestion(config, actualQuestion[i])
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
        override val config: BTConfig = BTConfig

        override fun check(info: JsonElement, question: JsonElement, answer: JsonElement) {
            val actualQuestion = question.String
            val actualAnswer = answer.String
            val blockSize = sqrt(actualQuestion.length.toFloat()).toInt()
            // 网格大小是完全平方数
            require(blockSize in config.minBlockSize .. config.maxBlockSize)
            require(blockSize * blockSize == actualQuestion.length)
            // 题目与答案大小相同
            require(actualQuestion.length == actualAnswer.length)
            // 题目中至少包含一个方格
            require(actualQuestion.contains(BTConfig.CHAR_BLOCK))
            // 答案中不能包含方格且至少包含一个非空
            require(!actualAnswer.contains(BTConfig.CHAR_BLOCK) && !actualAnswer.all { it == BTConfig.CHAR_EMPTY })
            for (index in actualQuestion.indices) {
                val q = actualQuestion[index]
                val a = actualAnswer[index]
                when (q) {
                    // 题目空则答案空
                    BTConfig.CHAR_EMPTY -> require(a == BTConfig.CHAR_EMPTY)
                    // 题目方格则答案非空
                    BTConfig.CHAR_BLOCK -> require(a != BTConfig.CHAR_EMPTY)
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
        override val config: FOConfig = FOConfig

        override fun fetchTryCount(info: JsonElement): Int = info.to<FOInfo>().tryCount

        override fun check(info: JsonElement, question: JsonElement, answer: JsonElement) {
            super.check(info, question, answer)
            val actualQuestion = question.Int
            val actualAnswer = answer.String
            // 问题长度
            require(actualQuestion in config.minLength .. config.maxLength)
            // 答案长度与问题一致
            require(actualAnswer.length == actualQuestion)
            // 无ASCII字符
            require(actualAnswer.all { it.code !in 0 .. 127 })
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
            return FOType.encode(config.minLength, items)
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
        override val config: SAConfig = SAConfig

        override fun fetchTimeLimit(info: JsonElement): Int = info.to<SAInfo>().timeLimit

        override fun check(info: JsonElement, question: JsonElement, answer: JsonElement) {
            super.check(info, question, answer)
            val actualInfo = info.to<SAInfo>()
            val actualQuestion = question.Int
            val actualAnswer = answer.to<List<String>>()
            // 阈值限制
            require(actualInfo.threshold in config.minThreshold .. config.maxThreshold)
            // 答案数量限制
            require(actualAnswer.size in config.minCount .. config.maxCount)
            // 答案长度与问题一致
            require(actualAnswer.size == actualQuestion)
            // 备选答案长度限制
            require(actualAnswer.all { it.length in config.minLength .. config.maxLength })
            // 答案无重复
            require(actualAnswer.size == actualAnswer.toSet().size)
        }

        private fun verify(standardAnswer: JsonElement, speedGameAnswer: SpeedGameAnswer): SAResult {
            val actualStandardAnswer = standardAnswer.to<List<String>>().toHashSet()
            val actualUserAnswer = speedGameAnswer.answer.to<List<String>>().toHashSet()
            var correctCount = 0
            for (answer in actualUserAnswer) {
                if (answer in actualStandardAnswer) ++correctCount
            }
            val duration = System.currentTimeMillis() - speedGameAnswer.ts
            return SAResult(
                correctCount = correctCount,
                totalCount = actualStandardAnswer.size,
                duration = (duration / 1000L).toInt()
            )
        }

        override fun start(uid: Int, details: GameDetails, userAnswer: JsonElement): Data<GameResult> {
            return when (val result = time(uid, details, userAnswer)) {
                is Data.Success -> {
                    val speedGameAnswer = result.data
                    if (speedGameAnswer.start) { // 计时开始
                        return Data.Success(GameResult.Time(
                            rid = speedGameAnswer.rid,
                            ts = speedGameAnswer.ts
                        ))
                    }
                    else { // 计时结束
                        val info = details.info.to<SAInfo>()
                        val saResult = verify(details.answer, speedGameAnswer)
                        return uploadResult(
                            uid = uid,
                            details = details,
                            answer = userAnswer,
                            isCompleted = (saResult.correctCount.toFloat() / saResult.totalCount) >= info.threshold,
                            info = saResult.toJson()
                        )
                    }
                }
                is Data.Error -> result
            }
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
        VN.throwIf(!GameConfig.checkReward(reward, num, cost))
        // 检查游戏数据配置
        try {
            type.manager.check(info, question, answer)
        } catch (_: Throwable) {
            return@api "数据配置非法".failedData
        }
        val actualCoin = (reward * GameConfig.rewardCostRatio).toInt()
        // 新增游戏行
        DB.throwTransaction {
            if (it.updateSQL("UPDATE user SET coin = coin - ? WHERE uid = ? AND coin >= ?", actualCoin, uid, actualCoin)) {
                val gid = it.throwInsertSQLGeneratedKey("""
                    INSERT INTO game(uid, title, type, reward, num, cost, winner, info, question, answer) ${values(10)}
                """, uid, title, type.ordinal, reward, num, cost,
                    "[]", info.toJsonString(), question.toJsonString(), answer.toJsonString()).toInt()
                Data.Success(gid, "创建成功")
            }
            else "银币不足".failedData
        }
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
            WHERE game.gid < ? AND game.type = ? AND game.isDeleted = 0
            ORDER BY game.gid DESC
            LIMIT ?
        """, gid, type.ordinal, num.coercePageNum)
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