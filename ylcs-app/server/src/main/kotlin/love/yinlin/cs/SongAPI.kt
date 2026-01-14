package love.yinlin.cs

import love.yinlin.cs.APIConfig.coercePageNum
import love.yinlin.cs.service.*
import love.yinlin.cs.user.*
import love.yinlin.data.rachel.profile.UserPrivilege
import love.yinlin.extension.to

fun APIScope.songAPI() {
    ApiSongGetSongs.response { sid, num ->
        val songs = db.throwQuerySQL("""
			SELECT sid, version, name
			FROM song
            WHERE sid > ?
			ORDER BY sid ASC
			LIMIT ?
		""", sid, num.coercePageNum)
        result(songs.to())
    }

    ApiSongGetSong.response { sid ->
        val song = db.querySQLSingle("""
            SELECT sid, version, name, singer, lyricist, composer, album, animation, video, rhyme
			FROM song
            WHERE sid = ?
        """, sid)
        if (song == null) failure("此歌曲未收录") else result(song.to())
    }

    ApiSongSearchSongs.response { key ->
        val songs = db.throwQuerySQL("""
			SELECT sid, version, name
			FROM song
            WHERE name LIKE ?
			ORDER BY sid ASC
		""", "%${key}%")
        result(songs.to())
    }

    ApiSongGetSongComments.response { sid, cid, num ->
        val songComment = db.throwQuerySQL("""
            SELECT cid, user.uid, ts, content, name, label, exp
            FROM song_comment
            LEFT JOIN user
            ON song_comment.uid = user.uid
            WHERE sid = ? AND isDeleted = 0 AND cid > ?
            ORDER BY cid ASC
            LIMIT ?
        """, sid, cid, num.coercePageNum)
        result(songComment.to())
    }

    ApiSongSendSongComment.response { token, sid, content ->
        VN.throwEmpty(content)
        val uid = AN.throwExpireToken(token)
        val cid = db.throwInsertSQLGeneratedKey("""
            INSERT INTO song_comment(sid, uid, content) ${values(3)}
        """, sid, uid, content)
        result(cid)
    }

    ApiSongDeleteSongComment.response { token, cid ->
        VN.throwId( cid)
        val uid = AN.throwExpireToken(token)
        // 权限：评论本人，超管
        if (db.querySQLSingle("""
            SELECT 1 FROM song_comment WHERE uid = ? AND cid = ? AND isDeleted = 0
            UNION
            SELECT 1 FROM user WHERE uid = ? AND (privilege & ${UserPrivilege.VIP_TOPIC}) != 0
        """, uid, cid, uid) == null) failure("无权限")
        // 逻辑删除
        db.throwExecuteSQL("UPDATE song_comment SET isDeleted = 1 WHERE cid = ?", cid)
    }
}