package love.yinlin.api

import love.yinlin.api.APIConfig.coercePageNum
import love.yinlin.api.sockets.LyricsSockets
import love.yinlin.api.sockets.LyricsSocketsManager
import love.yinlin.api.user.AN
import love.yinlin.api.user.VN
import love.yinlin.api.game.manager
import love.yinlin.data.rachel.game.GameConfig
import love.yinlin.data.rachel.game.GameDetails
import love.yinlin.data.rachel.game.GameRecord
import love.yinlin.data.rachel.profile.UserPrivilege
import love.yinlin.extension.Array
import love.yinlin.extension.Int
import love.yinlin.extension.catchingError
import love.yinlin.extension.to
import love.yinlin.extension.toJsonString
import love.yinlin.server.querySQLSingle
import love.yinlin.server.throwExecuteSQL
import love.yinlin.server.throwInsertSQLGeneratedKey
import love.yinlin.server.throwQuerySQLSingle
import love.yinlin.server.updateSQL
import love.yinlin.server.values

fun APIScope.gameAPI() {
    ApiGameCreateGame.response { token, args ->
        val (title, type, reward, num, cost, info, question, answer) = args
        val uid = AN.throwExpireToken(token)
        // 检查游戏奖励与名额
        VN.throwEmpty(title)
        VN.throwIf(!GameConfig.checkReward(reward, num, cost))
        // 检查游戏数据配置
        catchingError { type.manager(db).check(info, question, answer) }?.let { failure("数据配置非法") }
        val actualCoin = (reward * GameConfig.rewardCostRatio).toInt()
        // 新增游戏行
        db.throwTransaction {
            if (it.updateSQL("UPDATE user SET coin = coin - ? WHERE uid = ? AND coin >= ?", actualCoin, uid, actualCoin)) {
                val gid = it.throwInsertSQLGeneratedKey("""
                    INSERT INTO game(uid, title, type, reward, num, cost, winner, info, question, answer) ${values(10)}
                """, uid, title, type.ordinal, reward, num, cost,
                    "[]", info.toJsonString(), question.toJsonString(), answer.toJsonString()).toInt()
                result(gid)
            }
            else failure("银币不足")
        }
    }

    ApiGameDeleteGame.response { token, gid ->
        val uid = AN.throwExpireToken(token)
        VN.throwId(gid)
        db.throwTransaction {
            val result = it.throwQuerySQLSingle("SELECT uid, reward, num, winner FROM game WHERE gid = ?", gid)
            val userUid = result["uid"].Int
            if (userUid != uid) {
                if (it.querySQLSingle("""
                    SELECT 1 FROM user WHERE uid = ? AND (privilege & ${UserPrivilege.VIP_TOPIC}) != 0
                """, uid) == null) failure("无权限")
            }
            val reward = result["reward"].Int
            val useReward = reward / result["num"].Int * result["winner"].Array.size
            val remainReward = reward - useReward
            it.throwExecuteSQL("UPDATE game SET isDeleted = 1 WHERE gid = ?", gid)
            if (remainReward in 1 .. reward) {
                it.throwExecuteSQL("UPDATE user SET coin = coin + ? WHERE uid = ?", remainReward, userUid)
            }
        }
    }

    ApiGameGetGames.response { type, gid, num ->
        val games = db.throwQuerySQL("""
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
        result(games.to())
    }

    ApiGameGetUserGames.response { token, gid, isCompleted, num ->
        val uid = AN.throwExpireToken(token)
        val games = db.throwQuerySQL("""
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
        result(games.to())
    }

    ApiGameGetUserGameRecords.response { token, rid, num ->
        val uid = AN.throwExpireToken(token)
        val records = db.throwQuerySQL("""
            SELECT record.rid, record.gid, record.ts, user.name, game.title, game.type, record.answer, record.result
            FROM game_record AS record
            LEFT JOIN game ON record.gid = game.gid
            LEFT JOIN user ON game.uid = user.uid
            WHERE record.uid = ? AND record.rid < ? AND game.isDeleted = 0
            ORDER BY record.rid DESC
            LIMIT ?
        """, uid, rid, num.coercePageNum)
        result(records.to())
    }

    ApiGameGetGameRank.response { game ->
        val ranks = db.throwQuerySQL("""
            SELECT uid, name, cnt
            FROM game_rank
            WHERE type = ?
            ORDER BY r ASC
        """, game.ordinal)
        result(ranks.to())
    }

    ApiGamePreflightGame.response { token, gid ->
        val uid = AN.throwExpireToken(token)
        VN.throwId(gid)
        val details = db.throwQuerySQLSingle("""
            SELECT gid, uid, type, reward, num, cost, winner, info, question, answer, isCompleted
            FROM game
            WHERE gid = ? AND isDeleted = 0
        """, gid).to<GameDetails>()
        if (details.uid == uid) failure("不能参与自己创建的游戏哦")
        else if (details.isCompleted) failure("不能参与已经结算的游戏哦")
        else if (uid in details.winner) failure("不能参与完成过的游戏哦")
        else result(details.type.manager(db).preflight(uid, details).copy(info = details.info, question = details.question))
    }

    ApiGameVerifyGame.response { token, gid, rid, answer ->
        val uid = AN.throwExpireToken(token)
        VN.throwId(gid, rid)
        val record = db.throwQuerySQLSingle("""
            SELECT rid, gid, uid, ts, answer, result
            FROM game_record
            WHERE rid = ? AND gid = ? AND uid = ?
        """, rid, gid, uid).to<GameRecord>()
        val details = db.throwQuerySQLSingle("""
            SELECT gid, uid, type, reward, num, cost, winner, info, question, answer, isCompleted
            FROM game
            WHERE gid = ? AND isDeleted = 0
        """, gid).to<GameDetails>()
        if (details.uid == uid) failure("不能参与自己创建的游戏哦")
        else if (details.isCompleted) failure("不能参与已经结算的游戏哦")
        else if (uid in details.winner) failure("不能参与完成过的游戏哦")
        else result(details.type.manager(db).verify(uid, details, record, answer))
    }

    LyricsSockets.connect { LyricsSocketsManager(db, it) }
}