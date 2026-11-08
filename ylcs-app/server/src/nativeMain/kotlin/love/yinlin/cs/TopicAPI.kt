package love.yinlin.cs

import io.github.smyrgeorge.sqlx4k.QueryExecutor
import love.yinlin.cs.APIConfig.coercePageNum
import love.yinlin.cs.resource.NineGridProcessor
import love.yinlin.cs.resource.copyNewPictures
import love.yinlin.cs.service.*
import love.yinlin.data.rachel.profile.UserConstraint
import love.yinlin.data.rachel.profile.UserPrivilege
import love.yinlin.data.rachel.topic.Comment
import love.yinlin.extension.*

private suspend fun QueryExecutor.lockCommentModeration(uid: Int, tid: Int, rawSection: Int) {
    if (throwQuerySQL("SELECT uid FROM user WHERE uid = ? FOR SHARE", uid).isEmpty() ||
        throwQuerySQL("SELECT tid FROM topic WHERE tid = ? AND rawSection = ? FOR UPDATE", tid, rawSection).isEmpty()) throw FailureException("无权限")
}

fun ServerScope.topicAPI() {
    ApiTopicGetTopics.response { uid, isTop, tid, num ->
        VN.throwId(uid)
        val topics = mysql.throwQuerySQL("""
			SELECT tid, user.uid, title, pics->>'$[0]' AS pic, isTop, coinNum, commentNum, rawSection, name
            FROM topic
            LEFT JOIN user
            ON topic.uid = user.uid
            WHERE user.uid = ? AND isDeleted = 0 AND ${
            if (isTop) "((isTop = 1 AND tid < ?) OR isTop = 0)"
            else "isTop = 0 AND tid < ?"
        }
            ORDER BY isTop DESC, tid DESC
			LIMIT ?
		""", uid, tid, num.coercePageNum)
        result(topics.to())
    }

    ApiTopicGetLatestTopics.response { tid, num ->
        val topics = mysql.throwQuerySQL("""
            SELECT tid, user.uid, title, pics->>'$[0]' AS pic, isTop, coinNum, commentNum, rawSection, name
            FROM topic
            LEFT JOIN user
            ON topic.uid = user.uid
            WHERE isDeleted = 0 AND tid < ?
            ORDER BY tid DESC
            LIMIT ?
        """, tid, num.coercePageNum)
        result(topics.to())
    }

    ApiTopicGetLatestTopicsByComment.response { tid, num ->
        val topics = mysql.throwQuerySQL(
            """
        WITH
        -- 1. 先把 base_ts 算出来
        base AS (
            SELECT COALESCE(MAX(ts), NOW()) AS base_ts
            FROM (
                SELECT ts FROM comment_activity    WHERE tid = ? AND isDeleted=0
                UNION ALL
                SELECT ts FROM comment_discussion  WHERE tid = ? AND isDeleted=0
                UNION ALL
                SELECT ts FROM comment_notification WHERE tid = ? AND isDeleted=0
                UNION ALL
                SELECT ts FROM comment_water       WHERE tid = ? AND isDeleted=0
            ) AS tid_ts
        ),
        -- 2. 再做 latest_tids 去重、分页
        latest_tids AS (
            SELECT tid, MAX(ts) AS max_ts
            FROM (
                SELECT tid, ts FROM comment_activity WHERE isDeleted=0
                UNION ALL
                SELECT tid, ts FROM comment_discussion WHERE isDeleted=0
                UNION ALL
                SELECT tid, ts FROM comment_notification WHERE isDeleted=0
                UNION ALL
                SELECT tid, ts FROM comment_water WHERE isDeleted=0
            ) AS all_comments
            GROUP BY tid
            HAVING (MAX(ts) < (SELECT base_ts FROM base))
                OR (MAX(ts) = (SELECT base_ts FROM base) AND tid < ?)
            ORDER BY max_ts DESC, tid DESC
        )
        SELECT
            t.tid,
            u.uid,
            t.title,
            t.pics->>'$[0]' AS pic,
            t.isTop,
            t.coinNum,
            t.commentNum,
            t.rawSection,
            u.name,
            CAST(lt.max_ts AS CHAR) AS lastMaxTs
        FROM latest_tids lt
        JOIN topic t ON t.tid = lt.tid
        LEFT JOIN user u ON t.uid = u.uid
        WHERE t.isDeleted = 0
        ORDER BY lt.max_ts DESC, lt.tid DESC
		LIMIT ?
        """,
            // 四次 tid 用来算 base_ts，再一次用在 HAVING 中
            tid, tid, tid, tid,
            tid, num.coercePageNum
        )
        result(topics.to())
    }

    ApiTopicGetHotTopics.response { score, tid, num ->
        val topics = mysql.throwQuerySQL("""
			SELECT tid, user.uid, title, pics->>'$[0]' AS pic, isTop, coinNum, commentNum, rawSection, name, score
			FROM topic
			LEFT JOIN user
			ON topic.uid = user.uid
			WHERE isDeleted = 0 AND (score < ? OR (score = ? AND tid < ?))
			ORDER BY score DESC, tid DESC
			LIMIT ?
		""", score, score, tid, num.coercePageNum)
        result(topics.to())
    }

    ApiTopicGetSectionTopics.response { section, tid, num ->
        val topics = mysql.throwQuerySQL("""
			SELECT tid, user.uid, title, pics->>'$[0]' AS pic, isTop, coinNum, commentNum, rawSection, name
			FROM topic
			LEFT JOIN user
			ON topic.uid = user.uid
			WHERE section = ? AND isDeleted = 0 AND tid < ?
			ORDER BY tid DESC
			LIMIT ?
		""", section, tid, num.coercePageNum)
        result(topics.to())
    }

    ApiTopicGetTopicDetails.response { tid ->
        VN.throwId(tid)
        val topics = mysql.throwQuerySQLSingle("""
			SELECT tid, user.uid, ts, title, content, pics, isTop, coinNum, commentNum, section, rawSection, name, label, exp
			FROM topic
			LEFT JOIN user
			ON topic.uid = user.uid
			WHERE tid = ? AND isDeleted = 0
		""", tid)
        result(topics.to())
    }

    ApiTopicGetTopicComments.response { tid, rawSection, isTop, cid, num ->
        VN.throwId(tid)
        val tableName = VN.throwSection(rawSection)
        val comments = mysql.throwQuerySQL("""
			SELECT cid, user.uid, ts, content, isTop, subCommentNum, name, label, exp
            FROM $tableName
            LEFT JOIN user
            ON $tableName.uid = user.uid
            WHERE tid = ? AND pid IS NULL AND isDeleted = 0 AND ${
            if (isTop) "((isTop = 1 AND cid > ?) OR isTop = 0)"
            else "isTop = 0 AND cid > ?"
        }
            ORDER BY isTop DESC, cid
			LIMIT ?
		""", tid, cid, num.coercePageNum)
        result(comments.to())
    }

    ApiTopicGetTopicSubComments.response { pid, rawSection, cid, num ->
        VN.throwId(pid)
        val tableName = VN.throwSection(rawSection)
        val subComments = mysql.throwQuerySQL("""
			SELECT cid, user.uid, ts, content, name, label, exp
			FROM $tableName
			LEFT JOIN user
			ON $tableName.uid = user.uid
			WHERE pid = ? AND isDeleted = 0 AND cid > ?
			ORDER BY cid
			LIMIT ?
		""", pid, cid, num.coercePageNum)
        result(subComments.to())
    }

    ApiTopicSendTopic.response { token, title, content, section, pics ->
        VN.throwEmpty(title, content)
        VN.throwSection(section)
        val ngp = NineGridProcessor(pics)
        val uid = AN.throwExpireToken(token)
        val userPics = ServerRes.Users.User(uid).Pics()
        val tid = mysql.throwTransaction { transaction ->
            // 有图片时按用户串行化写入，避免同名文件被覆盖。
            val lock = if (ngp.actualPics.isEmpty()) "FOR SHARE" else "FOR UPDATE"
            val privilege = transaction.throwQuerySQLSingle("SELECT privilege FROM user WHERE uid = ? $lock", uid)["privilege"].Int
            if (!UserPrivilege.topic(privilege) || (section == Comment.Section.NOTIFICATION && !UserPrivilege.vipTopic(privilege))) failure("无权限")
            ngp.sourcePics.copyNewPictures(ngp.actualPics.map { userPics.pic(it) }) {
                transaction.throwInsertSQLGeneratedKey("""
                    INSERT INTO topic(uid, title, content, pics, section, rawSection) ${values(6)}
                """, uid, title, content, ngp.jsonString, section, section).toInt()
            }
        }
        result(tid, ngp.actualPics.firstOrNull())
    }

    ApiTopicUpdateTopicTop.response { token, tid, isTop ->
        VN.throwId(tid)
        val uid = AN.throwExpireToken(token)
        // 权限：主题本人
        if (!mysql.updateSQL("""
			UPDATE topic SET isTop = ? WHERE uid = ? AND tid = ? AND isDeleted = 0
		""", isTop, uid, tid)) failure("无权限")
    }

    ApiTopicDeleteTopic.response { token, tid ->
        VN.throwId(tid)
        val uid = AN.throwExpireToken(token)
        mysql.throwTransaction { transaction ->
            val isModerator = transaction.throwQuerySQL("""
                SELECT 1 FROM user WHERE uid = ? AND (privilege & ${UserPrivilege.VIP_TOPIC}) != 0 FOR SHARE
            """, uid).isNotEmpty()
            if (!isModerator && transaction.throwQuerySQL("""
                SELECT 1 FROM topic WHERE tid = ? AND uid = ? AND isDeleted = 0 FOR UPDATE
            """, tid, uid).isEmpty()) failure("无权限")
            transaction.throwExecuteSQL("UPDATE topic SET isDeleted = 1 WHERE tid = ? AND isDeleted = 0", tid)
        }
    }

    ApiTopicMoveTopic.response { token, tid, section ->
        VN.throwId(tid)
        VN.throwSection(section)
        val uid = AN.throwExpireToken(token)
        mysql.throwTransaction { transaction ->
            val privilege = transaction.throwQuerySQLSingle("SELECT privilege FROM user WHERE uid = ? FOR SHARE", uid)["privilege"].Int
            if (!UserPrivilege.vipTopic(privilege)) failure("无权限")
            transaction.throwExecuteSQL("UPDATE topic SET section = ? WHERE tid = ? AND isDeleted = 0", section, tid)
        }
    }

    ApiTopicSendCoin.response { token, uid, tid, value ->
        VN.throwId(uid, tid)
        VN.throwIf(value <= 0, value > UserConstraint.MIN_COIN_REWARD)
        val srcUid = AN.throwExpireToken(token)
        if (srcUid == uid) failure("不能给自己投币哦")
        val rewardRecipient = value == UserConstraint.MIN_COIN_REWARD
        mysql.throwTransaction { transaction ->
            // 两个账户都需要更新时，按 UID 顺序取锁。
            if (rewardRecipient && uid < srcUid) transaction.throwQuerySQL("SELECT uid FROM user WHERE uid = ? FOR UPDATE", uid)
            val user = transaction.throwQuerySQLSingle("SELECT coin, privilege FROM user WHERE uid = ? FOR UPDATE", srcUid)
            if (!UserPrivilege.topic(user["privilege"].Int)) failure("无权限")
            if (user["coin"].Int < value) failure("你的银币不够哦")
            if (rewardRecipient && uid > srcUid) transaction.throwQuerySQL("SELECT uid FROM user WHERE uid = ? FOR UPDATE", uid)
            // 更新主题投币数
            transaction.throwExecuteSQL("""
                UPDATE topic SET coinNum = coinNum + ? WHERE tid = ? AND uid = ? AND isDeleted = 0
            """, value, tid, uid)
            // 投币者减少银币
            transaction.throwExecuteSQL("""
                UPDATE user SET coin = coin - ? , exp = exp + ? WHERE uid = ? AND coin >= ?
            """, value, when (value) {
                UserConstraint.MIN_COIN_REWARD -> 2
                UserConstraint.MIN_COIN_REWARD - 1 -> 1
                else -> 0
            }, srcUid, value)
            // 被投币者增加银币和经验
            if (rewardRecipient) transaction.throwExecuteSQL("UPDATE user SET exp = exp + 1 , coin = coin + 1 WHERE uid = ?", uid)
        }
    }

    ApiTopicSendComment.response { token, tid, rawSection, content ->
        VN.throwId(tid)
        VN.throwEmpty(content)
        val tableName = VN.throwSection(rawSection)
        val uid = AN.throwExpireToken(token)
        val cid = mysql.throwTransaction { transaction ->
            val privilege = transaction.throwQuerySQLSingle("SELECT privilege FROM user WHERE uid = ? FOR SHARE", uid)["privilege"].Int
            if (!UserPrivilege.topic(privilege)) failure("无权限")
            transaction.throwExecuteSQL("""
                UPDATE topic SET commentNum = commentNum + 1 WHERE tid = ? AND rawSection = ? AND isDeleted = 0
            """, tid, rawSection)
            transaction.throwInsertSQLGeneratedKey("""
                INSERT INTO $tableName(tid, uid, content) ${values(3)}
            """, tid, uid, content).toInt()
        }
        result(cid)
    }

    ApiTopicSendSubComment.response { token, tid, cid, rawSection, content ->
        VN.throwId(tid, cid)
        VN.throwEmpty(content)
        val tableName = VN.throwSection(rawSection)
        val uid = AN.throwExpireToken(token)
        val cid = mysql.throwTransaction { transaction ->
            val privilege = transaction.throwQuerySQLSingle("SELECT privilege FROM user WHERE uid = ? FOR SHARE", uid)["privilege"].Int
            if (!UserPrivilege.topic(privilege)) failure("无权限")
            transaction.throwQuerySQLSingle("""
                SELECT tid FROM topic WHERE tid = ? AND rawSection = ? AND isDeleted = 0 FOR UPDATE
            """, tid, rawSection)
            transaction.throwExecuteSQL("""
                UPDATE $tableName SET subCommentNum = subCommentNum + 1
                WHERE tid = ? AND cid = ? AND pid IS NULL AND isDeleted = 0
            """, tid, cid)
            transaction.throwInsertSQLGeneratedKey("""
                INSERT INTO $tableName(pid, tid, uid, content) ${values(4)}
            """, cid, tid, uid, content).toInt()
        }
        result(cid)
    }

    ApiTopicUpdateCommentTop.response { token, tid, cid, rawSection, isTop ->
        VN.throwId(tid, cid)
        val tableName = VN.throwSection(rawSection)
        val uid = AN.throwExpireToken(token)
        // 权限：主题本人，超管
        mysql.throwTransaction { transaction ->
            transaction.lockCommentModeration(uid, tid, rawSection)
            if (transaction.throwQuerySQL("""
                SELECT c.cid
                FROM $tableName AS c
                JOIN topic AS t ON t.tid = c.tid
                JOIN user AS u ON u.uid = ?
                WHERE c.tid = ? AND c.cid = ?
                    AND c.pid IS NULL AND c.isDeleted = 0
                    AND t.rawSection = ?
                    AND (
                        (t.uid = u.uid AND t.isDeleted = 0)
                        OR (u.privilege & ${UserPrivilege.VIP_TOPIC}) != 0
                    )
                FOR UPDATE OF c
            """, uid, tid, cid, rawSection).isEmpty()) failure("无权限")
            transaction.throwExecuteSQL("""
                UPDATE $tableName SET isTop = ?
                WHERE tid = ? AND cid = ? AND pid IS NULL AND isDeleted = 0
            """, isTop, tid, cid)
        }
    }

    ApiTopicDeleteComment.response { token, tid, cid, rawSection ->
        VN.throwId(tid, cid)
        val tableName = VN.throwSection(rawSection)
        val uid = AN.throwExpireToken(token)
        // 权限：评论本人，主题本人，超管
        mysql.throwTransaction { transaction ->
            transaction.lockCommentModeration(uid, tid, rawSection)
            if (transaction.throwQuerySQL("""
                SELECT c.cid
                FROM $tableName AS c
                JOIN topic AS t ON t.tid = c.tid
                JOIN user AS u ON u.uid = ?
                WHERE c.tid = ? AND c.cid = ?
                  AND c.pid IS NULL AND c.isDeleted = 0
                  AND t.rawSection = ?
                  AND (
                      c.uid = u.uid
                      OR (t.uid = u.uid AND t.isDeleted = 0)
                      OR (u.privilege & ${UserPrivilege.VIP_TOPIC}) != 0
                  )
                FOR UPDATE OF c
            """, uid, tid, cid, rawSection).isEmpty()) failure("无权限")
            // 逻辑删除
            transaction.throwExecuteSQL("""
                UPDATE $tableName SET isDeleted = 1
                WHERE tid = ? AND cid = ? AND pid IS NULL AND isDeleted = 0
            """, tid, cid)
            // 更新主题评论数
            transaction.throwExecuteSQL("""
                UPDATE topic SET commentNum = commentNum - 1
                WHERE tid = ? AND rawSection = ?
            """, tid, rawSection)
        }
    }

    ApiTopicDeleteSubComment.response { token, tid, pid, cid, rawSection ->
        VN.throwId(tid, pid, cid)
        val tableName = VN.throwSection(rawSection)
        val uid = AN.throwExpireToken(token)
        // 权限：评论本人，主题本人，超管
        mysql.throwTransaction { transaction ->
            transaction.lockCommentModeration(uid, tid, rawSection)
            if (transaction.throwQuerySQL("""
                SELECT c.cid
                FROM $tableName AS c
                JOIN $tableName AS p ON p.cid = c.pid AND p.tid = c.tid
                JOIN topic AS t ON t.tid = c.tid
                JOIN user AS u ON u.uid = ?
                WHERE c.tid = ? AND c.pid = ? AND c.cid = ?
                  AND c.isDeleted = 0
                  AND p.pid IS NULL AND p.isDeleted = 0
                  AND t.rawSection = ?
                  AND (
                      c.uid = u.uid
                      OR (t.uid = u.uid AND t.isDeleted = 0)
                      OR (u.privilege & ${UserPrivilege.VIP_TOPIC}) != 0
                  )
                FOR UPDATE OF c, p
            """, uid, tid, pid, cid, rawSection).isEmpty()) failure("无权限")
            // 逻辑删除
            transaction.throwExecuteSQL("""
                UPDATE $tableName SET isDeleted = 1
                WHERE tid = ? AND pid = ? AND cid = ? AND isDeleted = 0
            """, tid, pid, cid)
            // 更新评论楼中楼数
            transaction.throwExecuteSQL("""
                UPDATE $tableName SET subCommentNum = subCommentNum - 1
                WHERE tid = ? AND cid = ? AND pid IS NULL AND isDeleted = 0
            """, tid, pid)
        }
    }
}
