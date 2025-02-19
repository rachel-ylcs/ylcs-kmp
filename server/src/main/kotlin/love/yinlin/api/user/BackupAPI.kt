package love.yinlin.api.user

import io.ktor.server.routing.Routing
import love.yinlin.DB
import love.yinlin.api.API
import love.yinlin.api.ImplMap
import love.yinlin.api.api
import love.yinlin.api.failedData
import love.yinlin.api.successData
import love.yinlin.data.Data
import love.yinlin.extension.Int
import love.yinlin.extension.Object
import love.yinlin.extension.toJsonString

fun Routing.backupAPI(implMap: ImplMap) {
	// ------  歌单云备份  ------
	api(API.User.Backup.UploadPlaylist) { (token, playlist) ->
		val uid = AN.throwExpireToken(token)
		if (DB.updateSQL("""
            UPDATE user SET playlist = ? WHERE uid = ? AND (privilege & ${Privilege.BACKUP}) != 0
        """, playlist.toJsonString(), uid)) "上传成功".successData
		else "无权限".failedData
	}

	// ------  歌单云还原  ------
	api(API.User.Backup.DownloadPlaylist) { token ->
		val uid = AN.throwExpireToken(token)
		val user = DB.throwGetUser(uid, "privilege, playlist")
		if (Privilege.backup(user["privilege"].Int))
			Data.Success(user["playlist"].Object, "下载成功")
		else "无权限".failedData
	}
}