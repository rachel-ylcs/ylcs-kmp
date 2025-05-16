package love.yinlin.api.user

import io.ktor.server.routing.Routing
import love.yinlin.DB
import love.yinlin.api.API
import love.yinlin.api.ImplMap
import love.yinlin.api.api
import love.yinlin.api.failedData
import love.yinlin.api.successData
import love.yinlin.data.Data
import love.yinlin.extension.Boolean
import love.yinlin.extension.Long
import love.yinlin.extension.to
import love.yinlin.throwExecuteSQL
import love.yinlin.throwInsertSQLGeneratedKey
import love.yinlin.values

fun Routing.followsAPI(implMap: ImplMap) {
    api(API.User.Follows.FollowUser) { (token, uid2) ->
        val uid1 = AN.throwExpireToken(token)
        if (uid1 == uid2) return@api "不能关注自己哦".failedData
        val follow = DB.querySQLSingle("SELECT isBlocked FROM follows WHERE uid1 = ? AND uid2 = ?", uid1, uid2)
        // 拉黑对方并不影响自己关注 Ta
        if (follow == null) DB.throwTransaction {
            it.throwInsertSQLGeneratedKey("INSERT INTO follows(uid1, uid2) ${values(2)}", uid1, uid2)
            it.throwExecuteSQL("""
                    UPDATE user
                    SET
                        follows = CASE WHEN uid = ? THEN follows + 1 ELSE follows END,
                        followers = CASE WHEN uid = ? THEN followers + 1 ELSE followers END
                    WHERE uid IN (?, ?)
                """, uid1, uid2, uid1, uid2)
            "关注成功".successData
        }
        else if (follow["isBlocked"].Boolean) "已被对方拉黑".failedData
        else "已关注对方".failedData
    }

    api(API.User.Follows.UnfollowUser) { (token, uid2) ->
        val uid1 = AN.throwExpireToken(token)
        if (uid1 == uid2) return@api "不能关注自己哦".failedData
        val follow = DB.querySQLSingle("SELECT fid FROM follows WHERE uid1 = ? AND uid2 = ? AND isBlocked = 0", uid1, uid2)
        if (follow == null) "未关注对方".failedData
        else DB.throwTransaction {
            it.throwExecuteSQL("DELETE FROM follows WHERE fid = ?", follow["fid"].Long)
            it.throwExecuteSQL("""
                    UPDATE user
                    SET
                        follows = CASE WHEN uid = ? THEN GREATEST(0, follows - 1) ELSE follows END,
                        followers = CASE WHEN uid = ? THEN GREATEST(0, followers - 1) ELSE followers END
                    WHERE uid IN (?, ?)
                """, uid1, uid2, uid1, uid2)
            "取消关注成功".successData
        }
    }

    api(API.User.Follows.GetFollows) { (token, score, fid, num) ->
        val uid1 = AN.throwExpireToken(token)
        val follows = DB.throwQuerySQL("""
            SELECT fid, uid2 AS uid, name, ts, score
            FROM follows
            LEFT JOIN user
            ON follows.uid2 = user.uid
            WHERE follows.uid1 = ? AND isBlocked = 0 AND (score < ? OR (score = ? AND fid > ?))
            ORDER BY score DESC, fid ASC
            LIMIT ?
        """, uid1, score, score, fid, num)
        Data.Success(follows.to())
    }

    api(API.User.Follows.GetFollowers) { (token, score, fid, num) ->
        val uid1 = AN.throwExpireToken(token)
        val followers = DB.throwQuerySQL("""
            SELECT fid, uid1 AS uid, name, score
            FROM follows
            LEFT JOIN user
            ON follows.uid1 = user.uid
            WHERE follows.uid2 = ? AND isBlocked = 0 AND (score < ? OR (score = ? AND fid > ?))
            ORDER BY score DESC, fid ASC
            LIMIT ?
        """, uid1, score, score, fid, num)
        Data.Success(followers.to())
    }

    api(API.User.Follows.BlockUser) { (token, uid2) ->
        val uid1 = AN.throwExpireToken(token)
        if (uid1 == uid2) return@api "不能拉黑自己哦".failedData
        val follow = DB.querySQLSingle("SELECT fid, isBlocked FROM follows WHERE uid1 = ? AND uid2 = ?", uid2, uid1)
        DB.throwTransaction {
            if (follow == null) it.throwInsertSQLGeneratedKey("INSERT INTO follows(uid1, uid2, isBlocked) ${values(3)}", uid2, uid1, true)
            else if (!follow["isBlocked"].Boolean) {
                it.throwExecuteSQL("UPDATE follows SET isBlocked = 1 WHERE fid = ?", follow["fid"].Long)
                it.throwExecuteSQL("""
                    UPDATE user
                    SET
                        follows = CASE WHEN uid = ? THEN GREATEST(0, follows - 1) ELSE follows END,
                        followers = CASE WHEN uid = ? THEN GREATEST(0, followers - 1) ELSE followers END
                    WHERE uid IN (?, ?)
                """, uid2, uid1, uid2, uid1)
            }
            "拉黑成功".successData
        }
    }

    api(API.User.Follows.UnblockUser) { (token, uid2) ->
        val uid1 = AN.throwExpireToken(token)
        VN.throwIf(uid1 == uid2)
        val follow = DB.querySQLSingle("SELECT fid, isBlocked FROM follows WHERE uid1 = ? AND uid2 = ?", uid2, uid1)
        if (follow != null && follow["isBlocked"].Boolean) DB.throwExecuteSQL("DELETE FROM follows WHERE fid = ?", follow["fid"].Long)
        "已取消拉黑".successData
    }

    api(API.User.Follows.GetBlockedUsers) { (token, fid, num) ->
        val uid1 = AN.throwExpireToken(token)
        val follows = DB.throwQuerySQL("""
            SELECT fid, uid1 AS uid, name
            FROM follows
            LEFT JOIN user
            ON follows.uid1 = user.uid
            WHERE follows.uid2 = ? AND isBlocked = 1 AND fid > ?
            ORDER BY fid ASC
            LIMIT ?
        """, uid1, fid, num)
        Data.Success(follows.to())
    }
}