package love.yinlin.api

import love.yinlin.data.rachel.mail.Mail
import love.yinlin.data.rachel.mail.MailEntry
import love.yinlin.extension.to
import love.yinlin.server.DB

fun APIScope<Mail.Filter, MailEntry, String>.photoAPI() {
    ApiPhotoSearchPhotoAlbums.response { keyword, ts, aid, num ->
        val photos = if (keyword == null) DB.throwQuerySQL("""
            SELECT aid, name, title, ts, location, author, keyword, picNum
            FROM photo
            WHERE picNum > 0 AND ts < ? OR (ts = ? AND aid < ?)
            ORDER BY ts DESC, aid DESC
            LIMIT ?
        """, ts, ts, aid, num) else DB.throwQuerySQL("""
            SELECT aid, name, title, ts, location, author, keyword, picNum
            FROM photo
            WHERE picNum > 0 AND (ts < ? OR (ts = ? AND aid < ?)) AND (JSON_CONTAINS(keyword, JSON_QUOTE(?)) OR location = ? OR author = ?)
            ORDER BY ts DESC, aid DESC
            LIMIT ?
        """, ts, ts, aid, keyword, keyword, keyword, num)
        result(photos.to())
    }
}