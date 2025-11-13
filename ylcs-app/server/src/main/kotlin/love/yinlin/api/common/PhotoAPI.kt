package love.yinlin.api.common

import io.ktor.server.routing.Routing
import love.yinlin.api.API2
import love.yinlin.api.ImplMap
import love.yinlin.api.api
import love.yinlin.data.Data
import love.yinlin.extension.to
import love.yinlin.server.DB

fun Routing.photoAPI(implMap: ImplMap) {
    api(API2.Common.Photo.SearchPhotoAlbums) { (keyword, ts, aid, num) ->
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
        Data.Success(photos.to())
    }
}