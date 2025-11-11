package love.yinlin.api.user

import io.ktor.server.routing.Routing
import love.yinlin.api.API
import love.yinlin.api.APIConfig.coercePageNum
import love.yinlin.api.ImplMap
import love.yinlin.api.api
import love.yinlin.api.failureData
import love.yinlin.api.successData
import love.yinlin.data.Data
import love.yinlin.data.rachel.profile.UserPrivilege
import love.yinlin.extension.to
import love.yinlin.server.DB
import love.yinlin.server.values

fun Routing.songAPI(implMap: ImplMap){
    api(API.User.Song.GetSongs) { (sid, num) ->
        val songs = DB.throwQuerySQL("""
			SELECT sid, version, name
			FROM song
            WHERE sid > ?
			ORDER BY sid ASC
			LIMIT ?
		""", sid, num.coercePageNum)
        Data.Success(songs.to())
    }

    api(API.User.Song.GetSong) { sid ->
        val song = DB.querySQLSingle("""
            SELECT sid, version, name, singer, lyricist, composer, album, animation, video, rhyme
			FROM song
            WHERE sid = ?
        """, sid)
        if (song == null) "此歌曲未收录".failureData else Data.Success(song.to())
    }

    api(API.User.Song.SearchSongs) { key ->
        val songs = DB.throwQuerySQL("""
			SELECT sid, version, name
			FROM song
            WHERE name LIKE ?
			ORDER BY sid ASC
		""", "%${key}%")
        Data.Success(songs.to())
    }

    api(API.User.Song.GetSongComments) { (sid, cid, num) ->
        val songComment = DB.throwQuerySQL("""
            SELECT cid, user.uid, ts, content, name, label, exp
            FROM song_comment
            LEFT JOIN user
            ON song_comment.uid = user.uid
            WHERE sid = ? AND isDeleted = 0 AND cid > ?
            ORDER BY cid ASC
            LIMIT ?
        """, sid, cid, num.coercePageNum)
        Data.Success(songComment.to())
    }

    api(API.User.Song.SendSongComment) { (token, sid, content) ->
        VN.throwEmpty(content)
        val uid = AN.throwExpireToken(token)
        val cid = DB.throwInsertSQLGeneratedKey("""
            INSERT INTO song_comment(sid, uid, content) ${values(3)}
        """, sid, uid, content)
        Data.Success(cid,"评论发送成功")
    }

    api(API.User.Song.DeleteSongComment) { (token, cid) ->
        VN.throwId( cid)
        val uid = AN.throwExpireToken(token)
        // 权限：评论本人，超管
        if (DB.querySQLSingle("""
            SELECT 1 FROM song_comment WHERE uid = ? AND cid = ? AND isDeleted = 0
            UNION
            SELECT 1 FROM user WHERE uid = ? AND (privilege & ${UserPrivilege.VIP_TOPIC}) != 0
        """, uid, cid, uid) == null) return@api "无权限".failureData
        // 逻辑删除
        DB.throwExecuteSQL("UPDATE song_comment SET isDeleted = 1 WHERE cid = ?", cid)
        "删除成功".successData
    }
}