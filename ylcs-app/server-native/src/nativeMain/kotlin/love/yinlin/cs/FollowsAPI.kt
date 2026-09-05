package love.yinlin.cs

import love.yinlin.cs.service.*
import love.yinlin.extension.*

suspend fun MysqlService.queryRelationship(uid1: Int, uid2: Int): Pair<Boolean?, Boolean?> {
    val follow = querySQL("""
            SELECT uid1, isBlocked FROM follows WHERE (uid1 = ? AND uid2 = ?) OR (uid1 = ? AND uid2 = ?)
        """, uid1, uid2, uid2, uid1)?.map { it.Object } ?: emptyList()
    val relationship1 = follow.find { it["uid1"].Int == uid1 }?.get("isBlocked")?.Boolean
    val relationship2 = follow.find { it["uid1"].Int == uid2 }?.get("isBlocked")?.Boolean
    return relationship1 to relationship2
}

fun ServerScope.followsAPI() {
    ApiFollowsFollowUser.response { token, uid2 ->
        val uid1 = AN.throwExpireToken(token)
        if (uid1 == uid2) failure("不能关注自己哦")
        val (relationship1, relationship2) = mysql.queryRelationship(uid1, uid2)
        when (relationship1) {
            null if relationship2 != true -> mysql.throwTransaction {
                it.throwInsertSQLGeneratedKey("INSERT INTO follows(uid1, uid2) ${values(2)}", uid1, uid2)
                it.throwExecuteSQL("""
                    UPDATE user
                    SET
                        follows = IF(uid = ?, follows + 1, follows),
                        followers = IF(uid = ?, followers + 1, followers)
                    WHERE uid IN (?, ?)
                """, uid1, uid2, uid1, uid2
                )
            }
            true -> failure("已被对方拉黑")
            else -> failure("已关注对方")
        }
    }

    ApiFollowsUnfollowUser.response { token, uid2 ->
        val uid1 = AN.throwExpireToken(token)
        if (uid1 == uid2) failure("不能关注自己哦")
        val (relationship1, relationship2) = mysql.queryRelationship(uid1, uid2)
        if (relationship1 == false && relationship2 != true) mysql.throwTransaction {
            it.throwExecuteSQL("DELETE FROM follows WHERE uid1 = ? AND uid2 = ?", uid1, uid2)
            it.throwExecuteSQL("""
                UPDATE user
                SET
                    follows = IF(uid = ?, GREATEST(0, follows - 1), follows),
                    followers = IF(uid = ?, GREATEST(0, followers - 1), followers)
                WHERE uid IN (?, ?)
            """, uid1, uid2, uid1, uid2)
        }
        else failure("未关注对方")
    }

    ApiFollowsGetFollows.response { token, score, fid, num ->
        val uid1 = AN.throwExpireToken(token)
        val follows = mysql.throwQuerySQL("""
            SELECT fid, uid2 AS uid, name, ts, score
            FROM follows
            LEFT JOIN user
            ON follows.uid2 = user.uid
            WHERE follows.uid1 = ? AND isBlocked = 0 AND (score < ? OR (score = ? AND fid > ?))
            ORDER BY score DESC, fid
            LIMIT ?
        """, uid1, score, score, fid, num)
        result(follows.to())
    }

    ApiFollowsGetFollowers.response { token, score, fid, num ->
        val uid1 = AN.throwExpireToken(token)
        val followers = mysql.throwQuerySQL("""
            SELECT fid, uid1 AS uid, name, score
            FROM follows
            LEFT JOIN user
            ON follows.uid1 = user.uid
            WHERE follows.uid2 = ? AND isBlocked = 0 AND (score < ? OR (score = ? AND fid > ?))
            ORDER BY score DESC, fid
            LIMIT ?
        """, uid1, score, score, fid, num)
        result(followers.to())
    }

    ApiFollowsBlockUser.response { token, uid2 ->
        val uid1 = AN.throwExpireToken(token)
        if (uid1 == uid2) failure("不能拉黑自己哦")
        val follow = mysql.querySQLSingle("SELECT fid, isBlocked FROM follows WHERE uid1 = ? AND uid2 = ?", uid2, uid1)
        mysql.throwTransaction {
            if (follow == null) it.throwInsertSQLGeneratedKey("INSERT INTO follows(uid1, uid2, isBlocked) ${values(3)}", uid2, uid1, true)
            else if (!follow["isBlocked"].Boolean) {
                it.throwExecuteSQL("UPDATE follows SET isBlocked = 1 WHERE fid = ?", follow["fid"].Long)
                it.throwExecuteSQL("""
                    UPDATE user
                    SET
                        follows = IF(uid = ?, GREATEST(0, follows - 1), follows),
                        followers = IF(uid = ?, GREATEST(0, followers - 1), followers)
                    WHERE uid IN (?, ?)
                """, uid2, uid1, uid2, uid1)
            }
        }
    }

    ApiFollowsUnblockUser.response { token, uid2 ->
        val uid1 = AN.throwExpireToken(token)
        VN.throwIf(uid1 == uid2)
        val follow = mysql.querySQLSingle("SELECT fid, isBlocked FROM follows WHERE uid1 = ? AND uid2 = ?", uid2, uid1)
        if (follow != null && follow["isBlocked"].Boolean) mysql.throwExecuteSQL("DELETE FROM follows WHERE fid = ?", follow["fid"].Long)
    }

    ApiFollowsGetBlockedUsers.response { token, fid, num ->
        val uid1 = AN.throwExpireToken(token)
        val follows = mysql.throwQuerySQL("""
            SELECT fid, uid1 AS uid, name
            FROM follows
            LEFT JOIN user
            ON follows.uid1 = user.uid
            WHERE follows.uid2 = ? AND isBlocked = 1 AND fid > ?
            ORDER BY fid
            LIMIT ?
        """, uid1, fid, num)
        result(follows.to())
    }
}