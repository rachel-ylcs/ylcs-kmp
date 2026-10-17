package love.yinlin.cs

import love.yinlin.cs.service.throwExecuteSQL
import love.yinlin.cs.service.throwQuerySQL
import love.yinlin.cs.service.throwQuerySQLSingle
import love.yinlin.data.rachel.concert.Concert
import love.yinlin.data.rachel.concert.ConcertSong
import love.yinlin.extension.to
import love.yinlin.extension.toJsonString

fun ServerScope.concertAPI() {
    ApiConcertGetConcerts.response { token ->
        val uid = AN.throwExpireToken(token)
        mysql.throwTransaction { transaction ->
            val concerts = transaction.throwQuerySQL("""
                SELECT cid, date, location, title, poster, songs
                FROM concert
                ORDER BY cid
            """).to<List<Concert>>()

            val songs = transaction.throwQuerySQL("""
                SELECT sid, name, pid
                FROM concert_song
                ORDER BY sid
            """).to<List<ConcertSong>>()

            val attendedConcerts = transaction.throwQuerySQLSingle(
                "SELECT concerts FROM user_data WHERE uid = ?", uid
            )["concerts"]!!.to<List<Int>>()
            result(concerts, songs, attendedConcerts)
        }
    }

    ApiConcertSetAttended.response { token, cid, attended ->
        VN.throwId(cid)
        val uid = AN.throwExpireToken(token)
        mysql.throwTransaction { transaction ->
            if (transaction.throwQuerySQL("SELECT cid FROM concert WHERE cid = ?", cid).isEmpty()) failure("该演出不存在")

            val attendedConcerts = transaction.throwQuerySQLSingle("SELECT concerts FROM user_data WHERE uid = ? FOR UPDATE", uid)["concerts"]!!.to<List<Int>>()

            val updatedConcerts = if (attended) {
                if (cid in attendedConcerts) attendedConcerts else attendedConcerts + cid
            }
            else attendedConcerts.filter { it != cid }

            if (updatedConcerts != attendedConcerts) {
                transaction.throwExecuteSQL("UPDATE user_data SET concerts = ? WHERE uid = ?", updatedConcerts.toJsonString(), uid)
            }
            result(updatedConcerts)
        }
    }
}
