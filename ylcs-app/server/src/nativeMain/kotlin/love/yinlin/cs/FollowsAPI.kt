package love.yinlin.cs

import io.github.smyrgeorge.sqlx4k.QueryExecutor
import love.yinlin.cs.service.*
import love.yinlin.extension.*

private suspend fun QueryExecutor.queryRelationship(uid1: Int, uid2: Int): Pair<Boolean?, Boolean?> {
    val follow = throwQuerySQL("""
        SELECT uid1, isBlocked FROM follows WHERE (uid1 = ? AND uid2 = ?) OR (uid1 = ? AND uid2 = ?)
    """, uid1, uid2, uid2, uid1).map { it.Object }
    val relationship1 = follow.find { it["uid1"].Int == uid1 }?.get("isBlocked")?.Boolean
    val relationship2 = follow.find { it["uid1"].Int == uid2 }?.get("isBlocked")?.Boolean
    return relationship1 to relationship2
}

private suspend fun MysqlService.relationshipTransaction(uid1: Int, uid2: Int, block: suspend QueryExecutor.(Boolean?, Boolean?) -> Unit) {
    throwTransaction { transaction ->
        // 关系行可能尚未创建，按 UID 顺序锁定双方账户后再读取关系。
        transaction.throwQuerySQL("SELECT uid FROM user WHERE uid = ? FOR UPDATE", minOf(uid1, uid2))
        transaction.throwQuerySQL("SELECT uid FROM user WHERE uid = ? FOR UPDATE", maxOf(uid1, uid2))
        val [relationship1, relationship2] = transaction.queryRelationship(uid1, uid2)
        transaction.block(relationship1, relationship2)
    }
}

fun ServerScope.followsAPI() {
    ApiFollowsFollowUser.response { token, uid2 ->
        val uid1 = AN.throwExpireToken(token)
        if (uid1 == uid2) failure("不能关注自己哦")
        mysql.relationshipTransaction(uid1, uid2) { relationship1, relationship2 ->
            when (relationship1) {
                null if relationship2 != true -> {
                    throwInsertSQLGeneratedKey("INSERT INTO follows(uid1, uid2) ${values(2)}", uid1, uid2)
                    throwExecuteSQL("""
                        UPDATE user
                        SET
                            follows = IF(uid = ?, follows + 1, follows),
                            followers = IF(uid = ?, followers + 1, followers)
                        WHERE uid IN (?, ?)
                    """, uid1, uid2, uid1, uid2)
                }
                true -> failure("已被对方拉黑")
                else -> failure("已关注对方")
            }
        }
    }

    ApiFollowsUnfollowUser.response { token, uid2 ->
        val uid1 = AN.throwExpireToken(token)
        if (uid1 == uid2) failure("不能关注自己哦")
        mysql.relationshipTransaction(uid1, uid2) { relationship1, relationship2 ->
            if (relationship1 != false || relationship2 == true) failure("未关注对方")
            throwExecuteSQL("DELETE FROM follows WHERE uid1 = ? AND uid2 = ? AND isBlocked = 0", uid1, uid2)
            throwExecuteSQL("""
                UPDATE user
                SET
                    follows = IF(uid = ?, GREATEST(0, follows - 1), follows),
                    followers = IF(uid = ?, GREATEST(0, followers - 1), followers)
                WHERE uid IN (?, ?)
            """, uid1, uid2, uid1, uid2)
        }
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
        mysql.relationshipTransaction(uid1, uid2) { _, relationship2 ->
            if (relationship2 == null) throwInsertSQLGeneratedKey("INSERT INTO follows(uid1, uid2, isBlocked) ${values(3)}", uid2, uid1, true)
            else if (!relationship2) {
                throwExecuteSQL("UPDATE follows SET isBlocked = 1 WHERE uid1 = ? AND uid2 = ? AND isBlocked = 0", uid2, uid1)
                throwExecuteSQL("""
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

        checker {
            NotEquals check (uid1 to uid2)
        }

        mysql.relationshipTransaction(uid1, uid2) { _, relationship2 ->
            if (relationship2 == true) throwExecuteSQL("DELETE FROM follows WHERE uid1 = ? AND uid2 = ? AND isBlocked = 1", uid2, uid1)
        }
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
