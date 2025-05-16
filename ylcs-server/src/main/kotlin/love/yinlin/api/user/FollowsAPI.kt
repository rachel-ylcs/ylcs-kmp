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
import love.yinlin.extension.Int
import love.yinlin.extension.Long
import love.yinlin.extension.Object
import love.yinlin.extension.to
import love.yinlin.throwExecuteSQL
import love.yinlin.throwInsertSQLGeneratedKey
import love.yinlin.values

fun DB.queryRelationship(uid1: Int, uid2: Int): Pair<Boolean?, Boolean?> {
    val follow = querySQL("""
            SELECT uid1, isBlocked FROM follows WHERE (uid1 = ? AND uid2 = ?) OR (uid1 = ? AND uid2 = ?)
        """, uid1, uid2, uid2, uid1)?.map { it.Object } ?: emptyList()
    val relationship1 = follow.find { it["uid1"].Int == uid1 }?.get("isBlocked")?.Boolean
    val relationship2 = follow.find { it["uid1"].Int == uid2 }?.get("isBlocked")?.Boolean
    return relationship1 to relationship2
}

fun Routing.followsAPI(implMap: ImplMap) {
    api(API.User.Follows.FollowUser) { (token, uid2) ->
        val uid1 = AN.throwExpireToken(token)
        if (uid1 == uid2) return@api "不能关注自己哦".failedData
        val (relationship1, relationship2) = DB.queryRelationship(uid1, uid2)
        if (relationship1 == null && relationship2 != true) DB.throwTransaction {
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
        else "已关注对方".failedData
    }

    api(API.User.Follows.UnfollowUser) { (token, uid2) ->
        val uid1 = AN.throwExpireToken(token)
        if (uid1 == uid2) return@api "不能关注自己哦".failedData
        val (relationship1, relationship2) = DB.queryRelationship(uid1, uid2)
        if (relationship1 == false && relationship2 != true) DB.throwTransaction {
            it.throwExecuteSQL("DELETE FROM follows WHERE uid1 = ? AND uid2 = ?", uid1, uid2)
            it.throwExecuteSQL("""
                UPDATE user
                SET
                    follows = CASE WHEN uid = ? THEN GREATEST(0, follows - 1) ELSE follows END,
                    followers = CASE WHEN uid = ? THEN GREATEST(0, followers - 1) ELSE followers END
                WHERE uid IN (?, ?)
            """, uid1, uid2, uid1, uid2)
            "取消关注成功".successData
        }
        else "未关注对方".failedData
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