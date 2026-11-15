package love.yinlin.cs

import love.yinlin.cs.APIConfig.coercePageNum
import love.yinlin.cs.service.throwExecuteSQL
import love.yinlin.cs.service.throwQuerySQL
import love.yinlin.cs.service.values
import love.yinlin.data.rachel.profile.UserPrivilege
import love.yinlin.extension.to

fun ServerScope.songAPI() {
    ApiSongGetSongLibrary.response {
        val songs = mysql.throwQuerySQL("SELECT sid, version, name, time, singer, lyricist, composer, album, animation, video, rhyme, accompaniment FROM song")
        result(songs.to())
    }

    ApiSongGetSong.response { sid ->
        val song = mysql.querySQLSingle("""
            SELECT sid, version, name, time, singer, lyricist, composer, album, animation, video, rhyme, accompaniment
			FROM song
            WHERE sid = ?
        """, sid)
        if (song == null) failure("此歌曲未收录") else result(song.to())
    }

    ApiSongGetSongComments.response { sid, cid, num ->
        val songComment = mysql.throwQuerySQL("""
            SELECT cid, user.uid, ts, content, name, label, exp
            FROM song_comment
            LEFT JOIN user
            ON song_comment.uid = user.uid
            WHERE sid = ? AND isDeleted = 0 AND cid > ?
            ORDER BY cid
            LIMIT ?
        """, sid, cid, num.coercePageNum)
        result(songComment.to())
    }

    ApiSongSendSongComment.response { token, sid, content ->
        checker {
            NotEmpty check content
        }

        val uid = AN.throwExpireToken(token)
        val cid = mysql.throwInsertSQLGeneratedKey("""
            INSERT INTO song_comment(sid, uid, content) ${values(3)}
        """, sid, uid, content)
        result(cid)
    }

    ApiSongDeleteSongComment.response { token, cid ->
        checker {
            ID check cid
        }

        val uid = AN.throwExpireToken(token)
        mysql.throwTransaction { transaction ->
            val isModerator = transaction.throwQuerySQL("""
                SELECT 1 FROM user WHERE uid = ? AND (privilege & ${UserPrivilege.VIP_TOPIC}) != 0 FOR SHARE
            """, uid).isNotEmpty()
            if (!isModerator && transaction.throwQuerySQL("""
                SELECT 1 FROM song_comment WHERE cid = ? AND uid = ? AND isDeleted = 0 FOR UPDATE
            """, cid, uid).isEmpty()) failure("无权限")
            transaction.throwExecuteSQL("UPDATE song_comment SET isDeleted = 1 WHERE cid = ?", cid)
        }
    }
}
