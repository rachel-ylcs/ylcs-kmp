package love.yinlin.cs

import love.yinlin.cs.APIConfig.coercePageNum
import love.yinlin.cs.service.values
import love.yinlin.data.rachel.profile.UserPrivilege
import love.yinlin.data.rachel.song.SongPreview
import love.yinlin.extension.to

fun ServerScope.songAPI() {
    ApiSongGetSongs.response { sid, num ->
        val songs = mysql.throwQuerySQL("""
			SELECT sid, version, name
			FROM song
            WHERE sid > ?
			ORDER BY sid
			LIMIT ?
		""", sid, num.coercePageNum)
        result(songs.to())
    }

    ApiSongGetSong.response { sid ->
        val song = mysql.querySQLSingle("""
            SELECT sid, version, name, singer, lyricist, composer, album, animation, video, rhyme, accompaniment
			FROM song
            WHERE sid = ?
        """, sid)
        if (song == null) failure("此歌曲未收录") else result(song.to())
    }

    ApiSongSearchSongs.response { key ->
        val songs = mysql.throwQuerySQL("""
			SELECT sid, version, name
			FROM song
            WHERE name LIKE ?
			ORDER BY sid
		""", "%${key}%")
        result(songs.to())
    }

    ApiSongSearchSongsByAlbum.response { album ->
        val songs = mysql.throwQuerySQL("""
            SELECT sid, version, name
            FROM song
            WHERE album = ?
            ORDER BY sid
        """, album)
        result(songs.to())
    }

    ApiSongSearchFilter.response { filter ->
        val args = mutableListOf<Any>()
        val sql = buildString {
            append("SELECT sid, version, name FROM song WHERE 1=1")

            val key = filter.key
            if (key != null) {
                append(" AND name LIKE ?")
                args.add("%$key%")
            }

            val singer = filter.singer
            if (singer != null) {
                append(" AND FIND_IN_SET(?, singer) > 0")
                args.add(singer)
            }

            val lyricist = filter.lyricist
            if (lyricist != null) {
                append(" AND FIND_IN_SET(?, lyricist) > 0")
                args.add(lyricist)
            }

            val composer = filter.composer
            if (composer != null) {
                append(" AND FIND_IN_SET(?, composer) > 0")
                args.add(composer)
            }

            val album = filter.album
            if (album != null) {
                append(" AND album = ?")
                args.add(album)
            }

            if (filter.useAnimation) append(" AND animation = 1")
            if (filter.useVideo) append(" AND video = 1")
            if (filter.useRhyme) append(" AND rhyme = 1")
            if (filter.useAccompaniment) append(" AND accompaniment = 1")

            append(" ORDER BY sid ASC")
        }

        val songs: List<SongPreview> = if (args.isEmpty() && !filter.useAnimation && !filter.useVideo && !filter.useRhyme && !filter.useAccompaniment) {
            emptyList()
        }
        else mysql.throwQuerySQL(sql, *args.toTypedArray()).to()

        result(songs)
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
        VN.throwEmpty(content)
        val uid = AN.throwExpireToken(token)
        val cid = mysql.throwInsertSQLGeneratedKey("""
            INSERT INTO song_comment(sid, uid, content) ${values(3)}
        """, sid, uid, content)
        result(cid)
    }

    ApiSongDeleteSongComment.response { token, cid ->
        VN.throwId( cid)
        val uid = AN.throwExpireToken(token)
        // 权限：评论本人，超管
        if (mysql.querySQLSingle("""
            SELECT 1 FROM song_comment WHERE uid = ? AND cid = ? AND isDeleted = 0
            UNION
            SELECT 1 FROM user WHERE uid = ? AND (privilege & ${UserPrivilege.VIP_TOPIC}) != 0
        """, uid, cid, uid) == null) failure("无权限")
        // 逻辑删除
        mysql.throwExecuteSQL("UPDATE song_comment SET isDeleted = 1 WHERE cid = ?", cid)
    }
}