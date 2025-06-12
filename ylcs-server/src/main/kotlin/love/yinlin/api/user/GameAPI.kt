package love.yinlin.api.user

import io.ktor.server.routing.*
import love.yinlin.DB
import love.yinlin.api.*
import love.yinlin.api.APIConfig.coercePageNum
import love.yinlin.api.user.game.manager
import love.yinlin.data.Data
import love.yinlin.data.map
import love.yinlin.data.rachel.game.GameConfig
import love.yinlin.data.rachel.game.GameDetails
import love.yinlin.data.rachel.game.GameRecord
import love.yinlin.data.rachel.profile.UserPrivilege
import love.yinlin.extension.Array
import love.yinlin.extension.Int
import love.yinlin.extension.to
import love.yinlin.extension.toJsonString
import love.yinlin.querySQLSingle
import love.yinlin.throwExecuteSQL
import love.yinlin.throwInsertSQLGeneratedKey
import love.yinlin.throwQuerySQLSingle
import love.yinlin.updateSQL
import love.yinlin.values

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
        DB.throwTransaction {
            val result = it.throwQuerySQLSingle("SELECT uid, reward, num, winner FROM game WHERE gid = ?", gid)
            val userUid = result["uid"].Int
            if (userUid != uid) {
                if (it.querySQLSingle("""
                    SELECT 1 FROM user WHERE uid = ? AND (privilege & ${UserPrivilege.VIP_TOPIC}) != 0
                """, uid) == null) return@throwTransaction "无权限".failedData
            }
            val reward = result["reward"].Int
            val useReward = (reward / result["num"].Int) * result["winner"].Array.size
            val remainReward = reward - useReward
            it.throwExecuteSQL("UPDATE game SET isDeleted = 1 WHERE gid = ?", gid)
            if (remainReward in 1 .. reward) {
                it.throwExecuteSQL("UPDATE user SET coin = coin + ? WHERE uid = ?", remainReward, userUid)
            }
            "删除成功".successData
        }
    }

    api(API.User.Game.GetGames) { (type, gid, num) ->
        val games = DB.throwQuerySQL("""
            SELECT game.gid, user.name, game.ts, game.title, game.type, game.reward, game.num, game.cost, game.info,
                IFNULL((
                    SELECT JSON_ARRAYAGG(COALESCE(u.name, 'Unknown'))
                    FROM JSON_TABLE(game.winner, '$[*]' COLUMNS (uid INT PATH '$')) AS jt
                    LEFT JOIN user AS u ON jt.uid = u.uid
                ), JSON_ARRAY()) AS winner
            FROM game
            LEFT JOIN user ON game.uid = user.uid
            WHERE game.gid < ? AND game.type = ? AND game.isDeleted = 0 AND game.isCompleted = 0
            ORDER BY game.gid DESC
            LIMIT ?
        """, gid, type.ordinal, num.coercePageNum)
        Data.Success(games.to())
    }

    api(API.User.Game.GetUserGames) { (token, gid, isCompleted, num) ->
        val uid = AN.throwExpireToken(token)
        val games = DB.throwQuerySQL("""
            SELECT gid, ts, title, type, reward, num, cost, info, question, answer, isCompleted,
                IFNULL((
                    SELECT JSON_ARRAYAGG(COALESCE(u.name, 'Unknown'))
                    FROM JSON_TABLE(game.winner, '$[*]' COLUMNS (uid INT PATH '$')) AS jt
                    LEFT JOIN user u ON jt.uid = u.uid
                ), JSON_ARRAY()) AS winner
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
        val uid = AN.throwExpireToken(token)
        VN.throwId(gid)
        val details = DB.throwQuerySQLSingle("""
            SELECT gid, uid, type, reward, num, cost, winner, info, question, answer, isCompleted
            FROM game
            WHERE gid = ? AND isDeleted = 0
        """, gid).to<GameDetails>()
        if (details.uid == uid) "不能参与自己创建的游戏哦".failedData
            else if (details.isCompleted) "不能参与已经结算的游戏哦".failedData
            else if (uid in details.winner) "不能参与完成过的游戏哦".failedData
            else details.type.manager.preflight(uid, details).map { it.copy(info = details.info, question = details.question) }
    }

    api(API.User.Game.VerifyGame) { (token, gid, rid, answer) ->
        val uid = AN.throwExpireToken(token)
        VN.throwId(gid, rid)
        val record = DB.throwQuerySQLSingle("""
            SELECT rid, gid, uid, ts, answer, result
            FROM game_record
            WHERE rid = ? AND gid = ? AND uid = ?
        """, rid, gid, uid).to<GameRecord>()
        val details = DB.throwQuerySQLSingle("""
            SELECT gid, uid, type, reward, num, cost, winner, info, question, answer, isCompleted
            FROM game
            WHERE gid = ? AND isDeleted = 0
        """, gid).to<GameDetails>()
        if (details.uid == uid) "不能参与自己创建的游戏哦".failedData
        else if (details.isCompleted) "不能参与已经结算的游戏哦".failedData
        else if (uid in details.winner) "不能参与完成过的游戏哦".failedData
        else details.type.manager.verify(uid, details, record, answer)
    }
}