package love.yinlin.cs

import love.yinlin.data.rachel.profile.UserPrivilege
import love.yinlin.extension.*

fun ServerScope.backupAPI() {
    ApiBackupUploadPlaylist.response { token, playlist ->
        val uid = AN.throwExpireToken(token)
        if (!mysql.updateSQL("""
            UPDATE user SET playlist = ? WHERE uid = ? AND (privilege & ${UserPrivilege.BACKUP}) != 0
        """, playlist.toJsonString(), uid)) failure("无权限")
    }

    ApiBackupDownloadPlaylist.response { token ->
        val uid = AN.throwExpireToken(token)
        val user = VN.throwGetUser(uid, "privilege, playlist")
        if (UserPrivilege.backup(user["privilege"].Int)) result(user["playlist"].Object)
        else failure("无权限")
    }
}