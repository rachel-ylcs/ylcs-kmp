package love.yinlin.api

import love.yinlin.extension.to

fun APIScope.photoAPI() {
    ApiPhotoSearchPhotoAlbums.response { keyword, ts, aid, num ->
        val photos = if (keyword == null) db.throwQuerySQL("""
            SELECT aid, name, title, ts, location, author, keyword, picNum
            FROM photo
            WHERE picNum > 0 AND ts < ? OR (ts = ? AND aid < ?)
            ORDER BY ts DESC, aid DESC
            LIMIT ?
        """, ts, ts, aid, num) else db.throwQuerySQL("""
            SELECT aid, name, title, ts, location, author, keyword, picNum
            FROM photo
            WHERE picNum > 0 AND (ts < ? OR (ts = ? AND aid < ?)) AND (JSON_CONTAINS(keyword, JSON_QUOTE(?)) OR location = ? OR author = ? OR title LIKE CONCAT('%', ?, '%'))
            ORDER BY ts DESC, aid DESC
            LIMIT ?
        """, ts, ts, aid, keyword, keyword, keyword, keyword, num)
        result(photos.to())
    }
}