package love.yinlin.cs

import io.github.smyrgeorge.sqlx4k.QueryExecutor
import kotlinx.serialization.json.JsonElement
import love.yinlin.concurrent.Mutex
import love.yinlin.cs.resource.NineGridProcessor
import love.yinlin.cs.resource.copyNewPictures
import love.yinlin.cs.service.deleteSQL
import love.yinlin.cs.service.throwExecuteSQL
import love.yinlin.cs.service.throwInsertSQLGeneratedKey
import love.yinlin.cs.service.throwQuerySQL
import love.yinlin.cs.service.throwQuerySQLSingle
import love.yinlin.cs.service.values
import love.yinlin.data.rachel.profile.UserPrivilege
import love.yinlin.extension.*

private val activityPhotoUploadMutex = Mutex()

private suspend fun <R> ServerScope.activityTransaction(token: String, block: suspend QueryExecutor.() -> R): R {
    val uid = AN.throwExpireToken(token)
    return mysql.throwTransaction { transaction ->
        val user = transaction.throwQuerySQLSingle("SELECT privilege FROM user WHERE uid = ? FOR SHARE", uid)
        if (!UserPrivilege.vipCalendar(user["privilege"].Int)) throw FailureException("无权限")
        transaction.block()
    }
}

private suspend fun QueryExecutor.lockActivityPhoto(aid: Int): MutableMap<String, JsonElement> =
    throwQuerySQLSingle("SELECT photo FROM activity WHERE aid = ? FOR UPDATE", aid)["photo"].Object.toMutableMap()

private suspend fun QueryExecutor.writeActivityPhoto(aid: Int, photo: Map<String, JsonElement>) {
    throwExecuteSQL("UPDATE activity SET photo = ? WHERE aid = ?", photo.toJsonString(), aid)
}

private suspend fun QueryExecutor.uploadActivityPhotos(aid: Int, photo: Map<String, JsonElement>, pics: APIFile, names: List<String>) {
    activityPhotoUploadMutex.with {
        pics.copyNewPictures(names.map { ServerRes.Activity.activity(it) }) { writeActivityPhoto(aid, photo) }
    }
}

fun ServerScope.activityAPI() {
    ApiActivityGetActivities.response { token ->
        val query = """
            SELECT aid, ts, tsInfo, location, shortTitle, title, content, price, saleTime, lineup, photo, link, playlist, hide
            FROM activity
        """.trimIndent()
        val activities = if (token == null) mysql.throwQuerySQL("$query WHERE hide = 0 ORDER BY aid DESC")
        else {
            val uid = AN.throwExpireToken(token)
            mysql.throwTransaction { transaction ->
                val user = transaction.throwQuerySQLSingle("SELECT privilege FROM user WHERE uid = ?", uid)
                val where = if (UserPrivilege.vipCalendar(user["privilege"].Int)) "" else "WHERE hide = 0"
                transaction.throwQuerySQL("$query $where ORDER BY aid DESC")
            }
        }
        result(activities.to())
    }

    ApiActivityAddActivity.response { token, hide ->
        val aid = activityTransaction(token) {
            throwInsertSQLGeneratedKey("""
                INSERT INTO activity(title, hide) ${values(2)}
            """, DateEx.CurrentLong.toString(), hide).toInt()
        }
        result(aid)
    }

    ApiActivityUpdateActivityInfo.response { token, activity ->
        activityTransaction(token) {
            throwExecuteSQL("""
                UPDATE activity
                SET ts = ? , tsInfo = ? , location = ? , shortTitle = ? , title = ? , content = ? , price = ? , saleTime = ? , lineup = ? , link = ? , playlist = ? , hide = ?
                WHERE aid = ?
            """, activity.ts, activity.tsInfo, activity.location, activity.shortTitle, activity.title, activity.content,
                activity.price.toJsonString(), activity.saleTime.toJsonString(), activity.lineup.toJsonString(), activity.link.toJsonString(), activity.playlist.toJsonString(),
                activity.hide, activity.aid)
        }
    }

    ApiActivityUpdateActivityPhoto.response { token, aid, key, pic ->
        VN.throwId(aid)
        val picName = activityTransaction(token) {
            val photo = lockActivityPhoto(aid)
            val name = DateEx.uniqueTimeId()
            photo[key] = name.json
            uploadActivityPhotos(aid, photo, pic, [name])
            name
        }
        result(picName)
    }

    ApiActivityDeleteActivityPhoto.response { token, aid, key ->
        VN.throwId(aid)
        activityTransaction(token) {
            val photo = lockActivityPhoto(aid)
            if (photo.remove(key) != null) writeActivityPhoto(aid, photo)
        }
    }

    ApiActivityAddActivityPhotos.response { token, aid, key, pics ->
        VN.throwId(aid)
        val ngp = NineGridProcessor(pics)
        val picNames = activityTransaction(token) {
            val photo = lockActivityPhoto(aid)
            val oldPics = photo[key]?.to<MutableList<String>>() ?: []
            VN.throwIf(pics.isEmpty, oldPics.size + pics.num > 9)
            val names = ngp.actualPics
            oldPics += names
            photo[key] = oldPics.toJson()
            uploadActivityPhotos(aid, photo, ngp.sourcePics, names)
            names
        }
        result(picNames)
    }

    ApiActivityUpdateActivityPhotos.response { token, aid, key, index, pic ->
        VN.throwId(aid)
        val picName = activityTransaction(token) {
            val photo = lockActivityPhoto(aid)
            val oldPics = photo[key]!!.to<MutableList<String>>()
            VN.throwIf(index < 0, index >= oldPics.size)
            val name = DateEx.uniqueTimeId()
            oldPics[index] = name
            photo[key] = oldPics.toJson()
            uploadActivityPhotos(aid, photo, pic, [name])
            name
        }
        result(picName)
    }

    ApiActivityDeleteActivityPhotos.response { token, aid, key, index ->
        VN.throwId(aid)
        activityTransaction(token) {
            val photo = lockActivityPhoto(aid)
            val oldPics = photo[key]!!.to<MutableList<String>>()
            VN.throwIf(index < 0, index >= oldPics.size)
            oldPics.removeAt(index)
            photo[key] = oldPics.toJson()
            writeActivityPhoto(aid, photo)
        }
    }

    ApiActivityDeleteActivity.response { token, aid ->
        VN.throwId(aid)
        activityTransaction(token) {
            if (!deleteSQL("DELETE FROM activity WHERE aid = ?", aid)) failure("该活动不存在")
        }
    }
}
