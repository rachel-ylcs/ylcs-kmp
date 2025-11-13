package love.yinlin.api.user

import io.ktor.server.routing.Routing
import love.yinlin.api.API2
import love.yinlin.api.ImplMap
import love.yinlin.api.api
import love.yinlin.api.failureData
import love.yinlin.api.successData
import love.yinlin.data.Data
import love.yinlin.data.rachel.profile.UserPrivilege
import love.yinlin.extension.Int
import love.yinlin.extension.Object
import love.yinlin.extension.toJsonString
import love.yinlin.server.DB

fun Routing.backupAPI(implMap: ImplMap) {
	api(API2.User.Backup.UploadPlaylist) { (token, playlist) ->
		val uid = AN.throwExpireToken(token)
		if (DB.updateSQL("""
            UPDATE user SET playlist = ? WHERE uid = ? AND (privilege & ${UserPrivilege.BACKUP}) != 0
        """, playlist.toJsonString(), uid)) "上传成功".successData
		else "无权限".failureData
	}

	api(API2.User.Backup.DownloadPlaylist) { token ->
		val uid = AN.throwExpireToken(token)
		val user = DB.throwGetUser(uid, "privilege, playlist")
		if (UserPrivilege.backup(user["privilege"].Int))
			Data.Success(user["playlist"].Object, "下载成功")
		else "无权限".failureData
	}
}