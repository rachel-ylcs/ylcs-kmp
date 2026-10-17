package love.yinlin.cs

import love.yinlin.data.rachel.profile.UserPrivilege
import love.yinlin.extension.*

fun ServerScope.backupAPI() {
    ApiBackupUploadPlaylist.response { token, playlist ->
        val uid = AN.throwExpireToken(token)
        val user = VN.throwGetUser(uid, "privilege")
        if (UserPrivilege.backup(user["privilege"].Int)) {
            mysql.throwExecuteSQL("UPDATE user_data SET playlist = ? WHERE uid = ?", playlist.toJsonString(), uid)
        }
        else failure("无权限")
    }

    ApiBackupDownloadPlaylist.response { token ->
        val uid = AN.throwExpireToken(token)
        val user = VN.throwGetUser(uid, "privilege")
        val userData = VN.throwGetUserData(uid, "playlist")
        if (UserPrivilege.backup(user["privilege"].Int)) result(userData.obj("playlist"))
        else failure("无权限")
    }
}