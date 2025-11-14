package love.yinlin.api.user

import io.ktor.server.routing.*
import love.yinlin.api.*
import love.yinlin.data.Data
import love.yinlin.data.rachel.profile.UserPrivilege
import love.yinlin.extension.DateEx
import love.yinlin.extension.Int
import love.yinlin.extension.Object
import love.yinlin.extension.json
import love.yinlin.extension.to
import love.yinlin.extension.toJson
import love.yinlin.extension.toJsonString
import love.yinlin.server.DB
import love.yinlin.server.copy
import love.yinlin.server.currentUniqueId
import love.yinlin.server.values

fun Routing.activityAPI(implMap: ImplMap) {
	api(API2.User.Activity.GetActivities) { ->
		Data.Success(
			DB.throwQuerySQL("""
			SELECT aid, ts, tsInfo, location, shortTitle, title, content, price, saleTime, lineup, photo, link, playlist
			FROM activity
			ORDER BY aid DESC
		""").to())
	}

	api(API2.User.Activity.AddActivity) { token ->
		val uid = AN.throwExpireToken(token)
		val user = DB.throwGetUser(uid, "privilege")
		if (!UserPrivilege.vipCalendar(user["privilege"].Int)) return@api "无权限".failureData
		// 插入活动
		val aid = DB.throwInsertSQLGeneratedKey("INSERT INTO activity(title) ${values(1)}", DateEx.CurrentLong.toString()).toInt()
		Data.Success(aid)
	}

	api(API2.User.Activity.UpdateActivityInfo) { (token, activity) ->
		val uid = AN.throwExpireToken(token)
		val user = DB.throwGetUser(uid, "privilege")
		if (!UserPrivilege.vipCalendar(user["privilege"].Int)) return@api "无权限".failureData
		DB.throwExecuteSQL("""
			UPDATE activity
			SET ts = ? , tsInfo = ? , location = ? , shortTitle = ? , title = ? , content = ? , price = ? , saleTime = ? , lineup = ? , link = ? , playlist = ?
			WHERE aid = ?
		""",activity.ts, activity.tsInfo, activity.location, activity.shortTitle, activity.title, activity.content,
			activity.price.toJsonString(), activity.saleTime.toJsonString(), activity.lineup.toJsonString(), activity.link.toJsonString(), activity.playlist.toJsonString(),
			activity.aid)
		"修改成功".successData
	}

	api(API2.User.Activity.UpdateActivityPhoto) { (token, aid, key), (pic) ->
		VN.throwId(aid)
		val uid = AN.throwExpireToken(token)
		val user = DB.throwGetUser(uid, "privilege")
		if (!UserPrivilege.vipCalendar(user["privilege"].Int)) return@api "无权限".failureData
		val photo = DB.throwQuerySQLSingle("SELECT photo FROM activity WHERE aid = ?", aid)["photo"].Object.toMutableMap()
		val picName = currentUniqueId()
		photo[key] = picName.json
		DB.throwExecuteSQL("UPDATE activity SET photo = ? WHERE aid = ?", photo.toJsonString(), aid)
		pic.copy(ServerRes2.Activity.activity(picName))
		Data.Success(picName)
	}

	api(API2.User.Activity.DeleteActivityPhoto) { (token, aid, key) ->
		VN.throwId(aid)
		val uid = AN.throwExpireToken(token)
		val user = DB.throwGetUser(uid, "privilege")
		if (!UserPrivilege.vipCalendar(user["privilege"].Int)) return@api "无权限".failureData
		val photo = DB.throwQuerySQLSingle("SELECT photo FROM activity WHERE aid = ?", aid)["photo"].Object.toMutableMap()
		val oldValue = photo.remove(key)
		if (oldValue != null) DB.throwExecuteSQL("UPDATE activity SET photo = ? WHERE aid = ?", photo.toJsonString(), aid)
		"删除成功".successData
	}

	api(API2.User.Activity.AddActivityPhotos) { (token, aid, key), (pics) ->
		VN.throwId(aid)
		val ngp = NineGridProcessor(pics)
		val uid = AN.throwExpireToken(token)
		val user = DB.throwGetUser(uid, "privilege")
		if (!UserPrivilege.vipCalendar(user["privilege"].Int)) return@api "无权限".failureData
		val photo = DB.throwQuerySQLSingle("SELECT photo FROM activity WHERE aid = ?", aid)["photo"].Object.toMutableMap()
		val oldPics = photo[key]?.to<MutableList<String>>() ?: mutableListOf()
		VN.throwIf(pics.isEmpty(), oldPics.size + pics.size > 9)
		oldPics += ngp.actualPics
		photo[key] = oldPics.toJson()
		DB.throwExecuteSQL("UPDATE activity SET photo = ? WHERE aid = ?", photo.toJsonString(), aid)
		ngp.copy { ServerRes2.Activity.activity(it) }
		Data.Success(ngp.actualPics)
	}

	api(API2.User.Activity.UpdateActivityPhotos) { (token, aid, key, index), (pic) ->
		VN.throwId(aid)
		val uid = AN.throwExpireToken(token)
		val user = DB.throwGetUser(uid, "privilege")
		if (!UserPrivilege.vipCalendar(user["privilege"].Int)) return@api "无权限".failureData
		val photo = DB.throwQuerySQLSingle("SELECT photo FROM activity WHERE aid = ?", aid)["photo"].Object.toMutableMap()
		val oldPics = photo[key]!!.to<MutableList<String>>()
		VN.throwIf(index < 0, index >= oldPics.size)
		val picName = currentUniqueId()
		oldPics[index] = picName
		photo[key] = oldPics.toJson()
		DB.throwExecuteSQL("UPDATE activity SET photo = ? WHERE aid = ?", photo.toJsonString(), aid)
		pic.copy(ServerRes2.Activity.activity(picName))
		Data.Success(picName)
	}

	api(API2.User.Activity.DeleteActivityPhotos) { (token, aid, key, index) ->
		VN.throwId(aid)
		val uid = AN.throwExpireToken(token)
		val user = DB.throwGetUser(uid, "privilege")
		if (!UserPrivilege.vipCalendar(user["privilege"].Int)) return@api "无权限".failureData
		val photo = DB.throwQuerySQLSingle("SELECT photo FROM activity WHERE aid = ?", aid)["photo"].Object.toMutableMap()
		val oldPics = photo[key]!!.to<MutableList<String>>()
		VN.throwIf(index < 0, index >= oldPics.size)
		oldPics.removeAt(index)
		photo[key] = oldPics.toJson()
		DB.throwExecuteSQL("UPDATE activity SET photo = ? WHERE aid = ?", photo.toJsonString(), aid)
		"删除成功".successData
	}

	api(API2.User.Activity.DeleteActivity) { (token, aid) ->
		VN.throwId(aid)
		val uid = AN.throwExpireToken(token)
		val user = DB.throwGetUser(uid, "privilege")
		if (!UserPrivilege.vipCalendar(user["privilege"].Int)) return@api "无权限".failureData
		if (!DB.deleteSQL("DELETE FROM activity WHERE aid = ?", aid)) return@api "该活动不存在".failureData
		"删除成功".successData
	}
}