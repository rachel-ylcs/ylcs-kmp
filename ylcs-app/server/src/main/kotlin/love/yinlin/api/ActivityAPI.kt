package love.yinlin.api

import love.yinlin.api.user.AN
import love.yinlin.api.user.VN
import love.yinlin.api.user.throwGetUser
import love.yinlin.data.rachel.profile.UserPrivilege
import love.yinlin.extension.DateEx
import love.yinlin.extension.Int
import love.yinlin.extension.Object
import love.yinlin.extension.json
import love.yinlin.extension.to
import love.yinlin.extension.toJson
import love.yinlin.extension.toJsonString
import love.yinlin.server.currentUniqueId
import love.yinlin.server.values

fun APIScope.activityAPI() {
    ApiActivityGetActivities.response { token ->
        val showHide = if (token != null) {
            val uid = AN.throwExpireToken(token)
            val user = db.throwGetUser(uid, "privilege")
            UserPrivilege.vipCalendar(user["privilege"].Int)
        } else false
        result(db.throwQuerySQL("""
			SELECT aid, ts, tsInfo, location, shortTitle, title, content, price, saleTime, lineup, photo, link, playlist, hide
			FROM activity
            ${if (showHide) "" else "WHERE hide = 0"}
			ORDER BY aid DESC
		""").to())
    }

    ApiActivityAddActivity.response { token, hide ->
        val uid = AN.throwExpireToken(token)
        val user = db.throwGetUser(uid, "privilege")
        if (!UserPrivilege.vipCalendar(user["privilege"].Int)) failure("无权限")
        // 插入活动
        val aid = db.throwInsertSQLGeneratedKey("""
            INSERT INTO activity(title, hide) ${values(2)}
        """, DateEx.CurrentLong.toString(), hide).toInt()
        result(aid)
    }

    ApiActivityUpdateActivityInfo.response { token, activity ->
        val uid = AN.throwExpireToken(token)
        val user = db.throwGetUser(uid, "privilege")
        if (!UserPrivilege.vipCalendar(user["privilege"].Int)) failure("无权限")
        db.throwExecuteSQL("""
			UPDATE activity
			SET ts = ? , tsInfo = ? , location = ? , shortTitle = ? , title = ? , content = ? , price = ? , saleTime = ? , lineup = ? , link = ? , playlist = ? , hide = ?
			WHERE aid = ?
		""",activity.ts, activity.tsInfo, activity.location, activity.shortTitle, activity.title, activity.content,
            activity.price.toJsonString(), activity.saleTime.toJsonString(), activity.lineup.toJsonString(), activity.link.toJsonString(), activity.playlist.toJsonString(),
            activity.hide, activity.aid)
    }

    ApiActivityUpdateActivityPhoto.response { token, aid, key, pic ->
        VN.throwId(aid)
        val uid = AN.throwExpireToken(token)
        val user = db.throwGetUser(uid, "privilege")
        if (!UserPrivilege.vipCalendar(user["privilege"].Int)) failure("无权限")
        val photo = db.throwQuerySQLSingle("SELECT photo FROM activity WHERE aid = ?", aid)["photo"].Object.toMutableMap()
        val picName = currentUniqueId()
        photo[key] = picName.json
        db.throwExecuteSQL("UPDATE activity SET photo = ? WHERE aid = ?", photo.toJsonString(), aid)
        pic.copy(ServerRes.Activity.activity(picName))
        result(picName)
    }

    ApiActivityDeleteActivityPhoto.response { token, aid, key ->
        VN.throwId(aid)
        val uid = AN.throwExpireToken(token)
        val user = db.throwGetUser(uid, "privilege")
        if (!UserPrivilege.vipCalendar(user["privilege"].Int)) failure("无权限")
        val photo = db.throwQuerySQLSingle("SELECT photo FROM activity WHERE aid = ?", aid)["photo"].Object.toMutableMap()
        val oldValue = photo.remove(key)
        if (oldValue != null) db.throwExecuteSQL("UPDATE activity SET photo = ? WHERE aid = ?", photo.toJsonString(), aid)
    }

    ApiActivityAddActivityPhotos.response { token, aid, key, pics ->
        VN.throwId(aid)
        val ngp = NineGridProcessor(pics)
        val uid = AN.throwExpireToken(token)
        val user = db.throwGetUser(uid, "privilege")
        if (!UserPrivilege.vipCalendar(user["privilege"].Int)) failure("无权限")
        val photo = db.throwQuerySQLSingle("SELECT photo FROM activity WHERE aid = ?", aid)["photo"].Object.toMutableMap()
        val oldPics = photo[key]?.to<MutableList<String>>() ?: mutableListOf()
        VN.throwIf(pics.isEmpty, oldPics.size + pics.num > 9)
        oldPics += ngp.actualPics
        photo[key] = oldPics.toJson()
        db.throwExecuteSQL("UPDATE activity SET photo = ? WHERE aid = ?", photo.toJsonString(), aid)
        ngp.copy { ServerRes.Activity.activity(it) }
        result(ngp.actualPics)
    }

    ApiActivityUpdateActivityPhotos.response { token, aid, key, index, pic ->
        VN.throwId(aid)
        val uid = AN.throwExpireToken(token)
        val user = db.throwGetUser(uid, "privilege")
        if (!UserPrivilege.vipCalendar(user["privilege"].Int)) failure("无权限")
        val photo = db.throwQuerySQLSingle("SELECT photo FROM activity WHERE aid = ?", aid)["photo"].Object.toMutableMap()
        val oldPics = photo[key]!!.to<MutableList<String>>()
        VN.throwIf(index < 0, index >= oldPics.size)
        val picName = currentUniqueId()
        oldPics[index] = picName
        photo[key] = oldPics.toJson()
        db.throwExecuteSQL("UPDATE activity SET photo = ? WHERE aid = ?", photo.toJsonString(), aid)
        pic.copy(ServerRes.Activity.activity(picName))
        result(picName)
    }

    ApiActivityDeleteActivityPhotos.response { token, aid, key, index ->
        VN.throwId(aid)
        val uid = AN.throwExpireToken(token)
        val user = db.throwGetUser(uid, "privilege")
        if (!UserPrivilege.vipCalendar(user["privilege"].Int)) failure("无权限")
        val photo = db.throwQuerySQLSingle("SELECT photo FROM activity WHERE aid = ?", aid)["photo"].Object.toMutableMap()
        val oldPics = photo[key]!!.to<MutableList<String>>()
        VN.throwIf(index < 0, index >= oldPics.size)
        oldPics.removeAt(index)
        photo[key] = oldPics.toJson()
        db.throwExecuteSQL("UPDATE activity SET photo = ? WHERE aid = ?", photo.toJsonString(), aid)
    }

    ApiActivityDeleteActivity.response { token, aid ->
        VN.throwId(aid)
        val uid = AN.throwExpireToken(token)
        val user = db.throwGetUser(uid, "privilege")
        if (!UserPrivilege.vipCalendar(user["privilege"].Int)) failure("无权限")
        if (!db.deleteSQL("DELETE FROM activity WHERE aid = ?", aid)) failure("该活动不存在")
    }
}