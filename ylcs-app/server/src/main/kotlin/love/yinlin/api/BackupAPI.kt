package love.yinlin.api

import love.yinlin.api.user.AN
import love.yinlin.api.user.throwGetUser
import love.yinlin.data.rachel.mail.Mail
import love.yinlin.data.rachel.mail.MailEntry
import love.yinlin.data.rachel.profile.UserPrivilege
import love.yinlin.extension.Int
import love.yinlin.extension.Object
import love.yinlin.extension.toJsonString
import love.yinlin.server.DB

fun APIScope<Mail.Filter, MailEntry, String>.backupAPI() {
    ApiBackupUploadPlaylist.response { token, playlist ->
        val uid = AN.throwExpireToken(token)
        if (!DB.updateSQL("""
            UPDATE user SET playlist = ? WHERE uid = ? AND (privilege & ${UserPrivilege.BACKUP}) != 0
        """, playlist.toJsonString(), uid)) failure("无权限")
    }

    ApiBackupDownloadPlaylist.response { token ->
        val uid = AN.throwExpireToken(token)
        val user = DB.throwGetUser(uid, "privilege, playlist")
        if (UserPrivilege.backup(user["privilege"].Int)) result(user["playlist"].Object)
        else failure("无权限")
    }
}