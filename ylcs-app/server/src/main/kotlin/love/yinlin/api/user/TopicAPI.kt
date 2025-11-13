package love.yinlin.api.user

import io.ktor.server.routing.Routing
import love.yinlin.api.API2
import love.yinlin.api.APIConfig.coercePageNum
import love.yinlin.api.ImplMap
import love.yinlin.api.NineGridProcessor
import love.yinlin.api.ServerRes2
import love.yinlin.api.api
import love.yinlin.api.failureData
import love.yinlin.api.successData
import love.yinlin.data.Data
import love.yinlin.data.rachel.topic.Comment
import love.yinlin.data.rachel.profile.UserConstraint
import love.yinlin.data.rachel.profile.UserPrivilege
import love.yinlin.extension.Int
import love.yinlin.extension.to
import love.yinlin.server.DB
import love.yinlin.server.throwExecuteSQL
import love.yinlin.server.throwInsertSQLGeneratedKey
import love.yinlin.server.values

fun Routing.topicAPI(implMap: ImplMap) {
	api(API2.User.Topic.GetTopics) { (uid, isTop, tid, num) ->
		VN.throwId(uid)
		val topics = DB.throwQuerySQL("""
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
		Data.Success(topics.to())
	}

	api(API2.User.Topic.GetLatestTopics) { (tid, num) ->
		val topics = DB.throwQuerySQL("""
            SELECT tid, user.uid, title, pics->>'$[0]' AS pic, isTop, coinNum, commentNum, rawSection, name
            FROM topic
            LEFT JOIN user
            ON topic.uid = user.uid
            WHERE isDeleted = 0 AND tid < ?
            ORDER BY tid DESC
            LIMIT ?
        """, tid, num.coercePageNum)
		Data.Success(topics.to())
	}

	api(API2.User.Topic.GetLatestTopicsByComment) { (tid, num) ->
		val topics = DB.throwQuerySQL(
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
		Data.Success(data = topics.to())
	}


	api(API2.User.Topic.GetHotTopics) { (score, tid, num) ->
		val topics = DB.throwQuerySQL("""
			SELECT tid, user.uid, title, pics->>'$[0]' AS pic, isTop, coinNum, commentNum, rawSection, name, score
			FROM topic
			LEFT JOIN user
			ON topic.uid = user.uid
			WHERE isDeleted = 0 AND (score < ? OR (score = ? AND tid < ?))
			ORDER BY score DESC, tid DESC
			LIMIT ?
		""", score, score, tid, num.coercePageNum)
		Data.Success(topics.to())
	}

	api(API2.User.Topic.GetSectionTopics) { (section, tid, num) ->
		val topics = DB.throwQuerySQL("""
			SELECT tid, user.uid, title, pics->>'$[0]' AS pic, isTop, coinNum, commentNum, rawSection, name
			FROM topic
			LEFT JOIN user
			ON topic.uid = user.uid
			WHERE section = ? AND isDeleted = 0 AND tid < ?
			ORDER BY tid DESC
			LIMIT ?
		""", section, tid, num.coercePageNum)
		Data.Success(topics.to())
	}

	api(API2.User.Topic.GetTopicDetails) { tid ->
		VN.throwId(tid)
		val topics = DB.throwQuerySQLSingle("""
			SELECT tid, user.uid, ts, title, content, pics, isTop, coinNum, commentNum, section, rawSection, name, label, exp
			FROM topic
			LEFT JOIN user
			ON topic.uid = user.uid
			WHERE tid = ? AND isDeleted = 0
		""", tid)
		Data.Success(topics.to())
	}

	api(API2.User.Topic.GetTopicComments) { (tid, rawSection, isTop, cid, num) ->
		VN.throwId(tid)
		val tableName = VN.throwSection(rawSection)
		val comments = DB.throwQuerySQL("""
			SELECT cid, user.uid, ts, content, isTop, subCommentNum, name, label, exp
            FROM $tableName
            LEFT JOIN user
            ON $tableName.uid = user.uid
            WHERE tid = ? AND pid IS NULL AND isDeleted = 0 AND ${
				if (isTop) "((isTop = 1 AND cid > ?) OR isTop = 0)"
				else "isTop = 0 AND cid > ?"
			}
            ORDER BY isTop DESC, cid ASC
			LIMIT ?
		""", tid, cid, num.coercePageNum)
		Data.Success(comments.to())
	}

	api(API2.User.Topic.GetTopicSubComments) { (pid, rawSection, cid, num) ->
		VN.throwId(pid)
		val tableName = VN.throwSection(rawSection)
		val subComments = DB.throwQuerySQL("""
			SELECT cid, user.uid, ts, content, name, label, exp
			FROM $tableName
			LEFT JOIN user
			ON $tableName.uid = user.uid
			WHERE pid = ? AND isDeleted = 0 AND cid > ?
			ORDER BY cid ASC
			LIMIT ?
		""", pid, cid, num.coercePageNum)
		Data.Success(subComments.to())
	}

	api(API2.User.Topic.SendTopic) { (token, title, content, section), (pics) ->
		VN.throwEmpty(title, content)
		VN.throwSection(section)
		val ngp = NineGridProcessor(pics)
		val uid = AN.throwExpireToken(token)
		val privilege = DB.throwGetUser(uid, "privilege")["privilege"].Int
		if (!UserPrivilege.topic(privilege) ||
			(section == Comment.Section.NOTIFICATION && !UserPrivilege.vipTopic(privilege)))
			return@api "无权限".failureData
		val tid = DB.throwInsertSQLGeneratedKey("""
            INSERT INTO topic(uid, title, content, pics, section, rawSection) ${values(6)}
        """, uid, title, content, ngp.jsonString, section, section).toInt()
		// 复制主题图片
		val userPics = ServerRes2.Users.User(uid).Pics()
		val pic = ngp.copy { userPics.pic(it) }
		Data.Success(API2.User.Topic.SendTopic.Response(tid, pic), "发表成功")
	}

	api(API2.User.Topic.UpdateTopicTop) { (token, tid, isTop) ->
		VN.throwId(tid)
		val uid = AN.throwExpireToken(token)
		// 权限：主题本人
		if (DB.updateSQL("""
			UPDATE topic SET isTop = ? WHERE uid = ? AND tid = ? AND isDeleted = 0
		""", isTop, uid, tid)) "${if (isTop) "" else "取消"}置顶成功".successData
		else "无权限".failureData
	}

	api(API2.User.Topic.DeleteTopic) { (token, tid) ->
		VN.throwId(tid)
		val uid = AN.throwExpireToken(token)
		// 权限：主题本人，超管
		if (DB.querySQLSingle("""
            SELECT 1 FROM topic WHERE uid = ? AND tid = ? AND isDeleted = 0
			UNION
			SELECT 1 FROM user WHERE uid = ? AND (privilege & ${UserPrivilege.VIP_TOPIC}) != 0
        """, uid, tid, uid) == null) return@api "无权限".failureData
		// 逻辑删除
		DB.throwExecuteSQL("UPDATE topic SET isDeleted = 1 WHERE tid = ? AND isDeleted = 0", tid)
		"删除成功".successData
	}

	api(API2.User.Topic.MoveTopic) { (token, tid, section) ->
		VN.throwId(tid)
		VN.throwSection(section)
		val uid = AN.throwExpireToken(token)
		// 权限：超管
		val user = DB.throwGetUser(uid, "privilege")
		if (!UserPrivilege.vipTopic(user["privilege"].Int)) return@api "无权限".failureData
		// 移动主题
		DB.throwExecuteSQL("UPDATE topic SET section = ? WHERE tid = ? AND isDeleted = 0", section, tid)
		"移动成功".successData
	}

	api(API2.User.Topic.SendCoin) { (token, uid, tid, value) ->
		VN.throwId(uid, tid)
		VN.throwIf(value <= 0, value > UserConstraint.MIN_COIN_REWARD)
		val srcUid = AN.throwExpireToken(token)
		if (srcUid == uid) return@api "不能给自己投币哦".failureData
		val user = DB.throwGetUser(srcUid, "coin, privilege")
		if (!UserPrivilege.topic(user["privilege"].Int)) return@api "无权限".failureData
		if ((user["coin"].Int) < value) return@api "你的银币不够哦".failureData
		DB.throwTransaction {
			// 更新主题投币数
			it.throwExecuteSQL("""
                UPDATE topic SET coinNum = coinNum + ? WHERE tid = ? AND uid = ? AND isDeleted = 0
            """, value, tid, uid)
			// 投币者减少银币
			it.throwExecuteSQL("""
                UPDATE user SET coin = coin - ? , exp = exp + ? WHERE uid = ? AND coin >= ?
            """, value, when (value) {
                UserConstraint.MIN_COIN_REWARD -> 2
                UserConstraint.MIN_COIN_REWARD - 1 -> 1
                else -> 0
            }, srcUid, value)
			// 被投币者增加银币和经验
			if (value == UserConstraint.MIN_COIN_REWARD)
				it.throwExecuteSQL("UPDATE user SET exp = exp + 1 , coin = coin + 1 WHERE uid = ?", uid)
		}
		"投币成功".successData
	}

	api(API2.User.Topic.SendComment) { (token, tid, rawSection, content) ->
		VN.throwId(tid)
		VN.throwEmpty(content)
		val tableName = VN.throwSection(rawSection)
		val uid = AN.throwExpireToken(token)
		val user = DB.throwGetUser(uid, "privilege")
		if (!UserPrivilege.topic(user["privilege"].Int)) return@api "无权限".failureData
		val cid = DB.throwTransaction {
			it.throwExecuteSQL("""
				UPDATE topic SET commentNum = commentNum + 1 WHERE tid = ? AND isDeleted = 0
			""", tid)
			it.throwInsertSQLGeneratedKey("""
                INSERT INTO $tableName(tid, uid, content) ${values(3)}
            """, tid, uid, content).toInt()
		}
		Data.Success(cid, "发送成功")
	}

	api(API2.User.Topic.SendSubComment) { (token, tid, cid, rawSection, content) ->
		VN.throwId(tid, cid)
		VN.throwEmpty(content)
		val tableName = VN.throwSection(rawSection)
		val uid = AN.throwExpireToken(token)
		val user = DB.throwGetUser(uid, "privilege")
		if (!UserPrivilege.topic(user["privilege"].Int)) return@api "无权限".failureData
		val cid = DB.throwTransaction {
			it.throwExecuteSQL("""
				UPDATE $tableName SET subCommentNum = subCommentNum + 1 WHERE cid = ? AND isDeleted = 0
			""", cid)
			it.throwInsertSQLGeneratedKey("""
                INSERT INTO $tableName(pid, tid, uid, content) ${values(4)}
            """, cid, tid, uid, content).toInt()
		}
		Data.Success(cid, "发送成功")
	}

	api(API2.User.Topic.UpdateCommentTop) { (token, tid, cid, rawSection, isTop) ->
		VN.throwId(tid, cid)
		val tableName = VN.throwSection(rawSection)
		val uid = AN.throwExpireToken(token)
		// 权限：主题本人，超管
		if (DB.querySQLSingle("""
			SELECT 1 FROM topic WHERE uid = ? AND tid = ? AND isDeleted = 0
			UNION
            SELECT 1 FROM user WHERE uid = ? AND (privilege & ${UserPrivilege.VIP_TOPIC}) != 0
        """, uid, tid, uid) == null) return@api "无权限".failureData
		DB.throwExecuteSQL("UPDATE $tableName SET isTop = ? WHERE cid = ? AND isDeleted = 0", isTop, cid)
		"${if (isTop) "" else "取消"}置顶成功".successData
	}

	api(API2.User.Topic.DeleteComment) { (token, tid, cid, rawSection) ->
		VN.throwId(tid, cid)
		val tableName = VN.throwSection(rawSection)
		val uid = AN.throwExpireToken(token)
		// 权限：评论本人，主题本人，超管
		if (DB.querySQLSingle("""
			SELECT 1 FROM $tableName WHERE uid = ? AND tid = ? AND cid = ? AND isDeleted = 0
			UNION
			SELECT 1 FROM topic WHERE uid = ? AND tid = ? AND isDeleted = 0
			UNION
            SELECT 1 FROM user WHERE uid = ? AND (privilege & ${UserPrivilege.VIP_TOPIC}) != 0
        """, uid, tid, cid, uid, tid, uid) == null) return@api "无权限".failureData
		DB.throwTransaction {
			// 逻辑删除
			it.throwExecuteSQL("UPDATE $tableName SET isDeleted = 1 WHERE cid = ? AND isDeleted = 0", cid)
			// 更新主题评论数
			it.throwExecuteSQL("UPDATE topic SET commentNum = commentNum - 1 WHERE tid = ?", tid)
		}
		"删除成功".successData
	}

	api(API2.User.Topic.DeleteSubComment) { (token, tid, pid, cid, rawSection) ->
		VN.throwId(tid, pid, cid)
		val tableName = VN.throwSection(rawSection)
		val uid = AN.throwExpireToken(token)
		// 权限：评论本人，主题本人，超管
		if (DB.querySQLSingle("""
			SELECT 1 FROM $tableName WHERE uid = ? AND tid = ? AND pid = ? AND cid = ? AND isDeleted = 0
			UNION
			SELECT 1 FROM topic WHERE uid = ? AND tid = ? AND isDeleted = 0
			UNION
            SELECT 1 FROM user WHERE uid = ? AND (privilege & ${UserPrivilege.VIP_TOPIC}) != 0
        """, uid, tid, pid, cid, uid, tid, uid) == null) return@api "无权限".failureData
		DB.throwTransaction {
			// 逻辑删除
			it.throwExecuteSQL("UPDATE $tableName SET isDeleted = 1 WHERE pid = ? AND cid = ? AND isDeleted = 0", pid, cid)
			// 更新评论楼中楼数
			it.throwExecuteSQL("UPDATE $tableName SET subCommentNum = subCommentNum - 1 WHERE pid IS NULL AND cid = ? AND isDeleted = 0", pid)
		}
		"删除成功".successData
	}
}