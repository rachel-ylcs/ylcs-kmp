package love.yinlin.cs

import io.github.smyrgeorge.sqlx4k.QueryExecutor
import love.yinlin.cs.service.throwExecuteSQL
import love.yinlin.cs.service.throwInsertSQLDuplicateKey
import love.yinlin.cs.service.throwInsertSQLGeneratedKey
import love.yinlin.cs.service.throwQuerySQL
import love.yinlin.cs.service.throwQuerySQLSingle
import love.yinlin.cs.service.values
import love.yinlin.data.rachel.concert.Concert
import love.yinlin.data.rachel.concert.ConcertSong
import love.yinlin.data.rachel.photo.PhotoAlbumList
import love.yinlin.data.rachel.profile.UserPrivilege
import love.yinlin.extension.DateEx
import love.yinlin.extension.Int
import love.yinlin.extension.Object
import love.yinlin.extension.to
import love.yinlin.extension.toJsonString

private suspend fun QueryExecutor.concerts(): List<Concert> = throwQuerySQL("SELECT cid, date, location, title, poster, songs FROM concert ORDER BY cid").to()

private suspend fun QueryExecutor.concertSongs(): List<ConcertSong> = throwQuerySQL("SELECT sid, name, pid FROM concert_song ORDER BY sid").to()

private suspend fun <R> ServerScope.concertEditorTransaction(token: String, block: suspend QueryExecutor.() -> R): R {
    val uid = AN.throwExpireToken(token)
    return mysql.throwTransaction { transaction ->
        val user = transaction.throwQuerySQL("SELECT privilege FROM user WHERE uid = ? FOR SHARE", uid).firstOrNull()
        if (user == null || !UserPrivilege.vipAccount(user.Object["privilege"].Int)) throw FailureException("无权限")
        transaction.block()
    }
}

private suspend fun QueryExecutor.checkConcertPoster(poster: String): String {
    val parts = poster.split('/', limit = 2)
    val name = parts[0]
    val index = parts.getOrNull(1)?.toIntOrNull()
    if (name.isBlank() || name == "." || name == ".." || '\\' in name || index == null || index < 0 || poster != "$name/$index") {
        throw FailureException("海报格式错误，请重新选择照片")
    }
    val albums = throwQuerySQL("SELECT name, picNum FROM photo WHERE name = ? FOR SHARE", name)
    if (albums.none { it.Object["name"]?.to<String>() == name && index in 0 ..< it.Object["picNum"].Int }) {
        throw FailureException("海报对应的图集或照片不存在")
    }
    return poster
}

private suspend fun QueryExecutor.resolveNamedConcertSong(name: String): ConcertSong {
    val existing = throwQuerySQL("SELECT sid, name, pid FROM concert_song WHERE name = ?", name).firstOrNull()
    if (existing != null) return existing.to()

    val matches = throwQuerySQL("SELECT sid FROM song WHERE name = ? LIMIT 2", name)
    val pid = matches.singleOrNull()?.Object?.get("sid")?.to<String>()
    throwInsertSQLDuplicateKey("INSERT INTO concert_song(name, pid) ${values(2)}", name, pid)

    return throwQuerySQLSingle("SELECT sid, name, pid FROM concert_song WHERE name = ? FOR SHARE", name).to()
}

fun ServerScope.concertAPI() {
    ApiConcertGetConcerts.response { token ->
        val uid = AN.throwExpireToken(token)

        mysql.throwTransaction { transaction ->
            val concerts = transaction.concerts()
            val referencedSongs = concerts.flatMap { it.songs }.toSet()
            val songs = transaction.concertSongs().filter { it.sid in referencedSongs }
            val attendedConcerts = transaction.throwQuerySQLSingle("SELECT concerts FROM user_data WHERE uid = ?", uid)["concerts"]!!
            result(concerts, songs, attendedConcerts.to<List<Int>>())
        }
    }

    ApiConcertGetEditorData.response { token ->
        concertEditorTransaction(token) {
            val albums = throwQuerySQL("SELECT aid, name, title, ts, location, author, keyword, picNum FROM photo")
            result(concerts(), concertSongs(), albums.to<PhotoAlbumList>())
        }
    }

    ApiConcertSaveConcert.response { token, request ->
        val title = request.title
        val cid = request.cid
        val location = request.location?.takeIf { it.isNotEmpty() }

        val date = DateEx.Formatter.standardDate.parse(request.date)
        if (date == null || date.year !in 1000 .. 9999) failure("请输入有效的演出日期")
        val dateText = DateEx.Formatter.standardDate.format(date)!!

        checker {
            NotEmpty check title
        }

        val [concert, songs] = concertEditorTransaction(token) {
            val poster = request.poster?.let { checkConcertPoster(it) }
            val old = if (cid == null) null else {
                val concert = throwQuerySQL("SELECT cid, date, location, title, poster, songs FROM concert WHERE cid = ? FOR UPDATE", cid)
                concert.firstOrNull()?.to<Concert>() ?: failure("该演出不存在")
            }

            require(request.songIds.count { it == null } != request.newSongNames.size)

            val orderedNames = request.newSongNames.map { it.trim() }
            val names = orderedNames.distinct().sorted()
            val existing = request.songIds.filterNotNull().distinct().associateWith {
                throwQuerySQL("SELECT sid, name, pid FROM concert_song WHERE sid = ?", it).firstOrNull()?.to<ConcertSong>() ?: failure("演出歌曲 #$it 不存在")
            }

            val named = names.associateWith { resolveNamedConcertSong(it) }
            val newNames = orderedNames.iterator()
            val setlist = request.songIds.map { sid ->
                if (sid == null) named.getValue(newNames.next()) else existing.getValue(sid)
            }

            val songIds = setlist.map { it.sid }
            val json = songIds.toJsonString()
            val saved = if (old == null) {
                val newId = throwInsertSQLGeneratedKey("""
                    INSERT INTO concert(date, location, title, poster, songs) ${values(5)}
                """, dateText, location, title, poster, json).toInt()
                Concert(newId, dateText, location, title, poster, songIds)
            }
            else {
                val updated = Concert(old.cid, dateText, location, title, poster, songIds)
                if (updated != old) {
                    throwExecuteSQL("""
                        UPDATE concert SET date = ?, location = ?, title = ?, poster = ?, songs = ? WHERE cid = ?
                    """, dateText, location, title, poster, json, old.cid)
                }
                updated
            }
            saved to setlist.distinctBy { it.sid }
        }
        result(concert, songs)
    }

    ApiConcertSetAttended.response { token, cid, attended ->
        checker {
            ID check cid
        }

        val uid = AN.throwExpireToken(token)

        mysql.throwTransaction { transaction ->
            if (transaction.throwQuerySQL("SELECT cid FROM concert WHERE cid = ? FOR SHARE", cid).isEmpty()) failure("该演出不存在")

            val attendedConcerts = transaction.throwQuerySQLSingle("SELECT concerts FROM user_data WHERE uid = ? FOR UPDATE", uid)["concerts"]!!.to<List<Int>>()

            val updatedConcerts = if (attended) {
                if (cid in attendedConcerts) attendedConcerts else attendedConcerts + cid
            }
            else {
                attendedConcerts.filter { it != cid }
            }

            if (updatedConcerts != attendedConcerts) {
                transaction.throwExecuteSQL("UPDATE user_data SET concerts = ? WHERE uid = ?", updatedConcerts.toJsonString(), uid)
            }

            result(updatedConcerts)
        }
    }
}
