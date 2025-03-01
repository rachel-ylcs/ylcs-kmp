package love.yinlin.api.user

import io.ktor.server.routing.*
import love.yinlin.DB
import love.yinlin.api.API
import love.yinlin.api.ImplMap
import love.yinlin.api.api
import love.yinlin.data.Data
import love.yinlin.extension.to

fun Routing.activityAPI(implMap: ImplMap) {
	api(API.User.Activity.GetActivities) { ->
		Data.Success(DB.throwQuerySQL("""
			SELECT aid, ts, title, content, pic, pics, showstart, damai, maoyan, link FROM activity
		""").to())
	}

//
//	api(API.User.Activity.AddActivity) { (token, info, pics) ->
//		VN.throwEmpty(info.ts, info.title, info.content)
//		val ngp = NineGridProcessor(pics)
//		val uid = AN.throwExpireToken(token)
//		val user = DB.throwGetUser(uid, "privilege")
//		if (!UserPrivilege.vipCalendar(user["privilege"].Int)) return@api "无权限".failedData
//		if (DB.throwInsertSQLDuplicateKey("""
//            INSERT INTO activity(ts, title, content, pics, showstart, damai, maoyan) VALUES(?, ?, ?, ?, ?, ?, ?)
//        """, info.ts, info.title, info.content, ngp.jsonString, info.showstart, info.damai, info.maoyan))
//			return@api "该日期已添加活动".failedData
//		// 复制活动图片
//		ngp.copy { ServerRes.Activity.activity(it) }
//		"添加成功".successData
//	}
//
//	api(API.User.Activity.ModifyActivity) { (token, info, pics) ->
//		VN.throwEmpty(info.ts, info.title, info.content)
//		val ngp = pics?.let { NineGridProcessor(it) }
//		val uid = AN.throwExpireToken(token)
//		val user = DB.throwGetUser(uid, "privilege")
//		if (!UserPrivilege.vipCalendar(user["privilege"].Int)) return@api "无权限".failedData
//		if (ngp == null) DB.throwExecuteSQL("""
//			UPDATE activity
//			SET title = ? AND content = ? AND showstart = ? AND damai = ? AND maoyan = ?
//			WHERE ts = ?
//		""", info.title, info.content, info.showstart, info.damai, info.maoyan, info.ts)
//		else {
//			DB.throwExecuteSQL("""
//				UPDATE activity
//				SET title = ? AND content = ? AND pics = ? AND showstart = ? AND damai = ? AND maoyan = ?
//				WHERE ts = ?
//			""", info.title, info.content, ngp.jsonString, info.showstart, info.damai, info.maoyan, info.ts)
//			ngp.copy { ServerRes.Activity.activity(it) }
//		}
//		"修改成功".successData
//	}
//
//	api(API.User.Activity.DeleteActivity) { (token, ts) ->
//		VN.throwEmpty(ts)
//		val uid = AN.throwExpireToken(token)
//		val user = DB.throwGetUser(uid, "privilege")
//		if (!UserPrivilege.vipCalendar(user["privilege"].Int)) return@api "无权限".failedData
//		val activity = DB.querySQLSingle("SELECT pics FROM activity WHERE ts = ?", ts)
//		if (activity == null) return@api "该活动不存在".failedData
//		DB.throwExecuteSQL("DELETE FROM activity WHERE ts = ?", ts)
//		// 删除活动图片
//		for (pic in activity["pics"].Array) ServerRes.Activity.activity(pic.String).delete()
//		"删除成功".successData
//	}
}