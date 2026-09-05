package love.yinlin.cs

import love.yinlin.cs.resource.NineGridProcessor
import love.yinlin.cs.service.values
import love.yinlin.data.rachel.profile.UserPrivilege
import love.yinlin.extension.*

fun ServerScope.activityAPI() {
    ApiActivityGetActivities.response { token ->
        val showHide = if (token != null) {
            val uid = AN.throwExpireToken(token)
            val user = VN.throwGetUser(uid, "privilege")
            UserPrivilege.vipCalendar(user["privilege"].Int)
        } else false
        result(mysql.throwQuerySQL("""
			SELECT aid, ts, tsInfo, location, shortTitle, title, content, price, saleTime, lineup, photo, link, playlist, hide
			FROM activity
            ${if (showHide) "" else "WHERE hide = 0"}
			ORDER BY aid DESC
		""").to())
    }

    ApiActivityAddActivity.response { token, hide ->
        val uid = AN.throwExpireToken(token)
        val user = VN.throwGetUser(uid, "privilege")
        if (!UserPrivilege.vipCalendar(user["privilege"].Int)) failure("无权限")
        // 插入活动
        val aid = mysql.throwInsertSQLGeneratedKey("""
            INSERT INTO activity(title, hide) ${values(2)}
        """, DateEx.CurrentLong.toString(), hide).toInt()
        result(aid)
    }

    ApiActivityUpdateActivityInfo.response { token, activity ->
        val uid = AN.throwExpireToken(token)
        val user = VN.throwGetUser(uid, "privilege")
        if (!UserPrivilege.vipCalendar(user["privilege"].Int)) failure("无权限")
        mysql.throwExecuteSQL("""
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
        val user = VN.throwGetUser(uid, "privilege")
        if (!UserPrivilege.vipCalendar(user["privilege"].Int)) failure("无权限")
        val photo = mysql.throwQuerySQLSingle("SELECT photo FROM activity WHERE aid = ?", aid)["photo"].Object.toMutableMap()
        val picName = DateEx.uniqueTimeId()
        photo[key] = picName.json
        mysql.throwExecuteSQL("UPDATE activity SET photo = ? WHERE aid = ?", photo.toJsonString(), aid)
        pic.copy(ServerRes.Activity.activity(picName))
        result(picName)
    }

    ApiActivityDeleteActivityPhoto.response { token, aid, key ->
        VN.throwId(aid)
        val uid = AN.throwExpireToken(token)
        val user = VN.throwGetUser(uid, "privilege")
        if (!UserPrivilege.vipCalendar(user["privilege"].Int)) failure("无权限")
        val photo = mysql.throwQuerySQLSingle("SELECT photo FROM activity WHERE aid = ?", aid)["photo"].Object.toMutableMap()
        val oldValue = photo.remove(key)
        if (oldValue != null) mysql.throwExecuteSQL("UPDATE activity SET photo = ? WHERE aid = ?", photo.toJsonString(), aid)
    }

    ApiActivityAddActivityPhotos.response { token, aid, key, pics ->
        VN.throwId(aid)
        val ngp = NineGridProcessor(pics)
        val uid = AN.throwExpireToken(token)
        val user = VN.throwGetUser(uid, "privilege")
        if (!UserPrivilege.vipCalendar(user["privilege"].Int)) failure("无权限")
        val photo = mysql.throwQuerySQLSingle("SELECT photo FROM activity WHERE aid = ?", aid)["photo"].Object.toMutableMap()
        val oldPics = photo[key]?.to<MutableList<String>>() ?: mutableListOf()
        VN.throwIf(pics.isEmpty, oldPics.size + pics.num > 9)
        oldPics += ngp.actualPics
        photo[key] = oldPics.toJson()
        mysql.throwExecuteSQL("UPDATE activity SET photo = ? WHERE aid = ?", photo.toJsonString(), aid)
        ngp.copy { ServerRes.Activity.activity(it) }
        result(ngp.actualPics)
    }

    ApiActivityUpdateActivityPhotos.response { token, aid, key, index, pic ->
        VN.throwId(aid)
        val uid = AN.throwExpireToken(token)
        val user = VN.throwGetUser(uid, "privilege")
        if (!UserPrivilege.vipCalendar(user["privilege"].Int)) failure("无权限")
        val photo = mysql.throwQuerySQLSingle("SELECT photo FROM activity WHERE aid = ?", aid)["photo"].Object.toMutableMap()
        val oldPics = photo[key]!!.to<MutableList<String>>()
        VN.throwIf(index < 0, index >= oldPics.size)
        val picName = DateEx.uniqueTimeId()
        oldPics[index] = picName
        photo[key] = oldPics.toJson()
        mysql.throwExecuteSQL("UPDATE activity SET photo = ? WHERE aid = ?", photo.toJsonString(), aid)
        pic.copy(ServerRes.Activity.activity(picName))
        result(picName)
    }

    ApiActivityDeleteActivityPhotos.response { token, aid, key, index ->
        VN.throwId(aid)
        val uid = AN.throwExpireToken(token)
        val user = VN.throwGetUser(uid, "privilege")
        if (!UserPrivilege.vipCalendar(user["privilege"].Int)) failure("无权限")
        val photo = mysql.throwQuerySQLSingle("SELECT photo FROM activity WHERE aid = ?", aid)["photo"].Object.toMutableMap()
        val oldPics = photo[key]!!.to<MutableList<String>>()
        VN.throwIf(index < 0, index >= oldPics.size)
        oldPics.removeAt(index)
        photo[key] = oldPics.toJson()
        mysql.throwExecuteSQL("UPDATE activity SET photo = ? WHERE aid = ?", photo.toJsonString(), aid)
    }

    ApiActivityDeleteActivity.response { token, aid ->
        VN.throwId(aid)
        val uid = AN.throwExpireToken(token)
        val user = VN.throwGetUser(uid, "privilege")
        if (!UserPrivilege.vipCalendar(user["privilege"].Int)) failure("无权限")
        if (!mysql.deleteSQL("DELETE FROM activity WHERE aid = ?", aid)) failure("该活动不存在")
    }
}