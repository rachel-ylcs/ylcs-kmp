package love.yinlin.api.user
import io.ktor.server.routing.Routing
import love.yinlin.DB
import love.yinlin.api.API
import love.yinlin.api.APIConfig.coercePageNum
import love.yinlin.api.ImplMap
import love.yinlin.api.api
import love.yinlin.api.failedData
import love.yinlin.api.successData
import love.yinlin.data.Data
import love.yinlin.data.rachel.profile.UserPrivilege
import love.yinlin.extension.to
import love.yinlin.values


fun Routing.songAPI(implMap: ImplMap){
    api(API.User.Song.GetSong) { (sid, num) ->
        val song = DB.throwQuerySQL("""
			SELECT sid, version, name, singer, lyricist, composer, album, bgd, video
			FROM song
            where sid > ?
			ORDER BY sid ASC
			LIMIT ?
		""", sid, num.coercePageNum)
        Data.Success(song.to())
    }
    api(API.User.Song.GetSongComment) { (sid, cid, num) ->
        val songComment = DB.throwQuerySQL(
            """
        WITH
    base AS (
    SELECT
        COALESCE(
        (
        SELECT sc2.ts
        FROM song_comment AS sc2
        WHERE sc2.sid = ?  AND sc2.cid = ? ),
        NOW() ) AS base_ts)
    SELECT sc.cid, sc.sid, sc.uid, DATE_FORMAT(sc.ts, '%Y-%m-%d %H:%i:%s') AS ts, sc.content, u.name
    FROM song_comment AS sc
    JOIN `user` AS u
        ON sc.uid = u.uid
    CROSS JOIN base
    WHERE sc.sid = ?  AND (sc.ts < base.base_ts  OR (sc.ts = base.base_ts AND sc.cid < ?)   
  )
    ORDER BY
    sc.ts  DESC,sc.cid DESC
    LIMIT ?;                                 
    """,
            sid, cid,
            sid, cid,
            num.coercePageNum
        )


        Data.Success(songComment.to())
    }
    api(API.User.Song.SendSongComment){ (token,sid,content)->
        VN.throwId(sid)
        val uid = AN.throwExpireToken(token)
        val cid = DB.throwInsertSQLGeneratedKey("""
            INSERT INTO song_comment(sid, uid, content) ${values(3)}
        """, sid,uid,content).toInt()
        Data.Success(cid,"评论发送成功")
    }
    api(API.User.Song.DeleteSongComment) { (token, cid) ->
        VN.throwId( cid)
        val uid = AN.throwExpireToken(token)
        // 权限：评论本人，超管
        if (DB.querySQLSingle("""SELECT 1 FROM song_comment WHERE cid =?""",cid)==null) return@api "未找到该评论或评论已删除".failedData
        else if (DB.querySQLSingle(
                """
            SELECT 1 FROM song_comment WHERE uid = ? AND cid = ?
			UNION
			SELECT 1 FROM user WHERE uid = ? AND (privilege & ${UserPrivilege.VIP_TOPIC}) != 0
        """, uid, cid, uid
            ) == null
        ) return@api "无权限".failedData
        // 逻辑删除
        DB.throwExecuteSQL("DELETE FROM song_comment WHERE cid=?", cid)
        "删除成功".successData
    }
    api(API.User.Song.UpdateSongComment){(token,cid,content)->
        VN.throwId( cid)
        val uid = AN.throwExpireToken(token)
        // 权限：评论本人
        if (DB.querySQLSingle("""SELECT 1 FROM song_comment WHERE cid =?""",cid)==null) return@api "评论不存在".failedData
        else if (DB.querySQLSingle(
                """
            SELECT 1 FROM song_comment WHERE uid = ? AND cid = ?
        """, uid, cid
            ) == null
        ) return@api "无权限".failedData
        DB.throwExecuteSQL("UPDATE song_comment SET content=? where cid=?",content,cid)
        "更新评论内容".successData
    }

}