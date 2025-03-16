package love.yinlin.api.user

import io.ktor.server.routing.*
import love.yinlin.DB
import love.yinlin.api.*
import love.yinlin.copy
import love.yinlin.currentUniqueId
import love.yinlin.data.Data
import love.yinlin.data.rachel.UserPrivilege
import love.yinlin.extension.Int
import love.yinlin.extension.to
import love.yinlin.extension.toJsonString
import love.yinlin.values

fun Routing.activityAPI(implMap: ImplMap) {
	api(API.User.Activity.GetActivities) { ->
		Data.Success(DB.throwQuerySQL("""
			SELECT aid, ts, title, content, pic, pics, showstart, damai, maoyan, link
			FROM activity
			ORDER BY aid DESC
		""").to())
	}

	api(API.User.Activity.AddActivity) { (token, activity), (pic, pics) ->
		VN.throwEmpty(activity.content)
		val ngp = NineGridProcessor(pics)
		val picName = if (pic == null) null else currentUniqueId()
		val uid = AN.throwExpireToken(token)
		val user = DB.throwGetUser(uid, "privilege")
		if (!UserPrivilege.vipCalendar(user["privilege"].Int)) return@api "无权限".failedData
		// 插入活动
		val aid = DB.throwInsertSQLGeneratedKey("""
            INSERT INTO activity(ts, title, content, pic, pics, showstart, damai, maoyan, link) ${values(9)}
        """, activity.ts, activity.title, activity.content, picName, ngp.jsonString,
			activity.showstart, activity.damai, activity.maoyan, activity.link).toInt()

		// 复制活动图片与海报
		if (pic != null && picName != null) pic.copy(ServerRes.Activity.activity(picName))
		ngp.copy { ServerRes.Activity.activity(it) }
		Data.Success(API.User.Activity.AddActivity.Response(
			aid = aid,
			pic = picName,
			pics = ngp.actualPics
		))
	}

	api(API.User.Activity.ModifyActivityInfo) { (token, activity) ->
		VN.throwEmpty(activity.content)
		val uid = AN.throwExpireToken(token)
		val user = DB.throwGetUser(uid, "privilege")
		if (!UserPrivilege.vipCalendar(user["privilege"].Int)) return@api "无权限".failedData
		DB.throwExecuteSQL("""
			UPDATE activity
			SET ts = ? AND title = ? AND content = ? AND showstart = ? AND damai = ? AND maoyan = ? AND link = ?
			WHERE aid = ?
		""", activity.ts, activity.title, activity.content,
			activity.showstart, activity.damai, activity.maoyan, activity.link,
			activity.aid)
		"修改成功".successData
	}

	api(API.User.Activity.ModifyActivityPicture) { (token, aid), (pic) ->
		VN.throwId(aid)
		val uid = AN.throwExpireToken(token)
		val user = DB.throwGetUser(uid, "privilege")
		if (!UserPrivilege.vipCalendar(user["privilege"].Int)) return@api "无权限".failedData
		val picName = currentUniqueId()
		DB.throwExecuteSQL("UPDATE activity SET pic = ? WHERE aid = ?", picName, aid)
		pic.copy(ServerRes.Activity.activity(picName))
		"修改成功".successData
	}

	api(API.User.Activity.DeleteActivityPicture) { (token, aid) ->
		VN.throwId(aid)
		val uid = AN.throwExpireToken(token)
		val user = DB.throwGetUser(uid, "privilege")
		if (!UserPrivilege.vipCalendar(user["privilege"].Int)) return@api "无权限".failedData
		DB.throwExecuteSQL("UPDATE activity SET pic = NULL WHERE aid = ?", aid)
		"删除成功".successData
	}

	api(API.User.Activity.AddActivityPictures) { (token, aid), (pic) ->
		VN.throwId(aid)
		val uid = AN.throwExpireToken(token)
		val user = DB.throwGetUser(uid, "privilege")
		if (!UserPrivilege.vipCalendar(user["privilege"].Int)) return@api "无权限".failedData
		val activities = DB.throwQuerySQLSingle("SELECT pics FROM activity WHERE aid = ?", aid)
		val pics = activities["pics"]!!.to<MutableList<String>>()
		VN.throwIf(pics.size >= 9)
		val picName = currentUniqueId()
		pics += picName
		DB.throwExecuteSQL("UPDATE activity SET pics = ? WHERE aid = ?", pics.toJsonString(), aid)
		pic.copy(ServerRes.Activity.activity(picName))
		"添加成功".successData
	}

	api(API.User.Activity.ModifyActivityPictures) { (token, aid, index), (pic) ->
		VN.throwId(aid)
		val uid = AN.throwExpireToken(token)
		val user = DB.throwGetUser(uid, "privilege")
		if (!UserPrivilege.vipCalendar(user["privilege"].Int)) return@api "无权限".failedData
		val activities = DB.throwQuerySQLSingle("SELECT pics FROM activity WHERE aid = ?", aid)
		val pics = activities["pics"]!!.to<MutableList<String>>()
		VN.throwIf(index < 0, index >= pics.size)
		val picName = currentUniqueId()
		pics[index] = picName
		DB.throwExecuteSQL("UPDATE activity SET pics = ? WHERE aid = ?", pics.toJsonString(), aid)
		pic.copy(ServerRes.Activity.activity(picName))
		"修改成功".successData
	}

	api(API.User.Activity.DeleteActivityPictures) { (token, aid, index) ->
		VN.throwId(aid)
		val uid = AN.throwExpireToken(token)
		val user = DB.throwGetUser(uid, "privilege")
		if (!UserPrivilege.vipCalendar(user["privilege"].Int)) return@api "无权限".failedData
		val activities = DB.throwQuerySQLSingle("SELECT pics FROM activity WHERE aid = ?", aid)
		val pics = activities["pics"]!!.to<MutableList<String>>()
		VN.throwIf(index < 0, index >= pics.size)
		pics.removeAt(index)
		DB.throwExecuteSQL("UPDATE activity SET pics = ? WHERE aid = ?", pics.toJsonString(), aid)
		"删除成功".successData
	}

	api(API.User.Activity.DeleteActivity) { (token, ts) ->
		VN.throwEmpty(ts)
		val uid = AN.throwExpireToken(token)
		val user = DB.throwGetUser(uid, "privilege")
		if (!UserPrivilege.vipCalendar(user["privilege"].Int)) return@api "无权限".failedData
		val activity = DB.querySQLSingle("SELECT pic, pics FROM activity WHERE ts = ?", ts)
		if (activity == null) return@api "该活动不存在".failedData
		DB.throwExecuteSQL("DELETE FROM activity WHERE ts = ?", ts)
		"删除成功".successData
	}
}