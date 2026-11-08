package love.yinlin.cs

import love.yinlin.cs.service.throwExecuteSQL
import love.yinlin.cs.service.throwQuerySQLSingle
import love.yinlin.data.rachel.profile.UserPrivilege
import love.yinlin.extension.*

fun ServerScope.backupAPI() {
    ApiBackupUploadPlaylist.response { token, playlist ->
        val uid = AN.throwExpireToken(token)
        mysql.throwTransaction { transaction ->
            val user = transaction.throwQuerySQLSingle("SELECT privilege FROM user WHERE uid = ? FOR SHARE", uid)
            if (!UserPrivilege.backup(user["privilege"].Int)) failure("无权限")
            transaction.throwExecuteSQL("UPDATE user_data SET playlist = ? WHERE uid = ?", playlist.toJsonString(), uid)
        }
    }

    ApiBackupDownloadPlaylist.response { token ->
        val uid = AN.throwExpireToken(token)
        val backup = mysql.throwQuerySQLSingle("""
            SELECT u.privilege, d.playlist
            FROM user AS u
            JOIN user_data AS d ON d.uid = u.uid
            WHERE u.uid = ?
        """, uid)
        if (UserPrivilege.backup(backup["privilege"].Int)) result(backup.obj("playlist"))
        else failure("无权限")
    }
}
