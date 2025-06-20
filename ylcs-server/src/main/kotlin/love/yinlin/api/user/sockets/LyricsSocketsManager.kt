package love.yinlin.api.user.sockets

import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import love.yinlin.DB
import love.yinlin.api.user.AN
import love.yinlin.data.rachel.game.Game
import love.yinlin.data.rachel.sockets.LyricsSockets
import love.yinlin.extension.parseJsonValue
import love.yinlin.extension.toJsonString
import love.yinlin.platform.Coroutines
import love.yinlin.values
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.coroutineContext
import kotlin.random.Random

private suspend inline fun WebSocketServerSession.send(data: LyricsSockets.SM) = this.sendSerialized(data)

object LyricsSocketsManager {
    @Serializable
    data class Lyrics(val q: String, val a: String)

    private data class Player(
        val uid: Int,
        val name: String,
        var session: DefaultWebSocketServerSession,
        var room: Room? = null,
        var inviteJob: Job? = null
    ) {
        val info: LyricsSockets.PlayerInfo get() = LyricsSockets.PlayerInfo(uid, name)
    }

    private class Room(val info1: LyricsSockets.PlayerInfo, val info2: LyricsSockets.PlayerInfo) {
        private val lyrics = run {
            val set = mutableSetOf<Int>()
            val random = Random(System.currentTimeMillis())
            for (i in library.size - LyricsSockets.QUESTION_COUNT until library.size) {
                val randomIndex = random.nextInt(i + 1)
                if (!set.add(randomIndex)) set.add(i)
            }
            set.map { library[it] }
        }
        val roomId: String = UUID.randomUUID().toString()
        var createTime: Long = System.currentTimeMillis()
        var submitTime1: Long? = null
        var submitTime2: Long? = null
        val questions: List<Pair<String, Int>> = lyrics.map { it.q to it.a.length }
        val answers: List<String> = lyrics.map { it.a }
        val answers1: MutableList<String?> = MutableList(LyricsSockets.QUESTION_COUNT) { null }
        val answers2: MutableList<String?> = MutableList(LyricsSockets.QUESTION_COUNT) { null }
    }

    private val library = run {
        val classLoader = LyricsSocketsManager::class.java.classLoader
        val stream = classLoader.getResourceAsStream("lyrics_game.json")!!
        val lyrics = stream.readAllBytes().decodeToString().parseJsonValue<List<Lyrics>>()!!
        stream.close()
        lyrics
    }

    private val players = ConcurrentHashMap<Int, Player>()
    private val scope = CoroutineScope(Dispatchers.Default)

    private val availablePlayers: List<LyricsSockets.PlayerInfo> get() = players.values.filter { it.room == null }.map { it.info }

    private suspend fun onInviteTimer(uid: Int, targetInfo: LyricsSockets.PlayerInfo) {
        delay(LyricsSockets.INVITE_TIME)
        if (coroutineContext.isActive) {
            // 超时自动拒绝
            val target = players[targetInfo.uid]
            if (target == null || target.room == null) {
                players[uid]?.let { player ->
                    if (player.inviteJob != null) {
                        player.session.send(LyricsSockets.SM.RefuseInvitation(targetInfo))
                        player.inviteJob = null
                    }
                }
            }
        }
    }

    private suspend fun prepareGame(room: Room) {
        // 启动准备计时
        delay(LyricsSockets.PREPARE_TIME)
        // 更新创建时间
        val newCreateTime = System.currentTimeMillis()
        room.submitTime1?.let { room.submitTime1 = it - room.createTime + newCreateTime + LyricsSockets.PLAYING_TIME }
        room.submitTime2?.let { room.submitTime2 = it - room.createTime + newCreateTime + LyricsSockets.PLAYING_TIME }
        room.createTime = newCreateTime
        val player1 = players[room.info1.uid]
        val player2 = players[room.info2.uid]
        val questions = LyricsSockets.SM.GameStart(room.questions)
        if (player1?.room?.roomId == room.roomId) player1.session.send(questions)
        if (player2?.room?.roomId == room.roomId) player2.session.send(questions)
        onPlayingTimer(room)
    }

    private suspend fun onPlayingTimer(room: Room) {
        // 启动游戏计时
        delay(LyricsSockets.PLAYING_TIME)
        // 强制结算
        players[room.info1.uid]?.room?.let { room1 ->
            if (room.roomId == room1.roomId) submitAnswers(room, room.info1.uid)
        }
        players[room.info2.uid]?.room?.let { room2 ->
            if (room.roomId == room2.roomId) submitAnswers(room, room.info2.uid)
        }
    }

    private suspend fun submitAnswers(room: Room, uid: Int) {
        if (room.info1.uid == uid && room.submitTime1 == null) {
            room.submitTime1 = System.currentTimeMillis()
            if (room.submitTime2 != null) endGame(room)
        }
        else if (room.info2.uid == uid && room.submitTime2 == null) {
            room.submitTime2 = System.currentTimeMillis()
            if (room.submitTime1 != null) endGame(room)
        }
    }

    private suspend fun endGame(room: Room) {
        val player1 = players[room.info1.uid]
        val player2 = players[room.info2.uid]
        val standardAnswers = room.answers
        val answers1 = room.answers1
        val answers2 = room.answers2
        // 计算时长
        val lastSubmitTime = System.currentTimeMillis()
        val duration1 = ((room.submitTime1 ?: lastSubmitTime) - room.createTime).coerceIn(0L, LyricsSockets.PLAYING_TIME)
        val duration2 = ((room.submitTime2 ?: lastSubmitTime) - room.createTime).coerceIn(0L, LyricsSockets.PLAYING_TIME)
        // 计算正确数
        var count1 = 0
        var count2 = 0
        for (index in standardAnswers.indices) {
            val answer = standardAnswers[index]
            if (answers1[index] == answer) ++count1
            if (answers2[index] == answer) ++count2
        }
        // 生成结果
        val result1 = LyricsSockets.GameResult(room.info1, count1, duration1)
        val result2 = LyricsSockets.GameResult(room.info2, count2, duration2)
        // 仍然在线的玩家发送结果
        if (player1?.room?.roomId == room.roomId) {
            player1.session.send(LyricsSockets.SM.SendResult(result1, result2))
            player1.room = null
        }
        if (player2?.room?.roomId == room.roomId) {
            player2.session.send(LyricsSockets.SM.SendResult(result2, result1))
            player2.room = null
        }
        // 超过 60% 正确率的胜者存入数据库
        Coroutines.io {
            val isWinner1 = if (count1 > count2) true else if (count1 == count2) duration1 <= duration2 else false
            val winnerResult = if (isWinner1) result1 else result2
            val winnerAnswer = if (isWinner1) answers1 else answers2
            if (winnerResult.count >= (LyricsSockets.QUESTION_COUNT * 0.6f).toInt()) {
                runCatching {
                    DB.throwInsertSQLGeneratedKey("INSERT INTO game_alone_record (type, uid, question, answer, result) ${values(5)}",
                        Game.GuessLyrics.ordinal, winnerResult.player.uid,
                        room.questions.toJsonString(), winnerAnswer.toJsonString(),
                        LyricsSockets.StorageResult(winnerResult.count, winnerResult.duration).toJsonString()
                    )
                }
            }
        }
    }

    suspend fun dispatchMessage(session: DefaultWebSocketServerSession) {
        var currentPlayer: Player? = null

        try {
            for (frame in session.incoming) {
                if (frame !is Frame.Text) continue
                when (val msg = frame.readText().parseJsonValue<LyricsSockets.CM>()!!) {
                    is LyricsSockets.CM.Login -> {
                        val tokenUid = Coroutines.io { AN.throwExpireToken(msg.token) }
                        val (uid, name) = msg.info
                        require(tokenUid == uid && !players.containsKey(uid))
                        val player = Player(uid, name, session)
                        currentPlayer = player
                        players[uid] = player
                        session.send(LyricsSockets.SM.PlayerList(availablePlayers))
                    }
                    LyricsSockets.CM.GetPlayers if currentPlayer != null -> session.send(LyricsSockets.SM.PlayerList(availablePlayers))
                    is LyricsSockets.CM.InvitePlayer if currentPlayer != null -> {
                        val target = players[msg.targetUid]
                        when {
                            target == null -> session.send(LyricsSockets.SM.Error("对方未上线"))
                            currentPlayer.uid == target.uid -> session.send(LyricsSockets.SM.Error("不能邀请自己"))
                            currentPlayer.room != null -> session.send(LyricsSockets.SM.Error("你已在游戏中"))
                            currentPlayer.inviteJob != null -> session.send(LyricsSockets.SM.Error("你已邀请其他人"))
                            target.room != null -> session.send(LyricsSockets.SM.Error("对方已在游戏中"))
                            else -> {
                                target.session.send(LyricsSockets.SM.InviteReceived(currentPlayer.info))
                                // 启动邀请验证
                                currentPlayer.inviteJob = scope.launch { onInviteTimer(currentPlayer.uid, target.info) }
                            }
                        }
                    }
                    is LyricsSockets.CM.InviteResponse if currentPlayer != null -> {
                        val inviter = players[msg.inviterUid]
                        when {
                            inviter == null -> session.send(LyricsSockets.SM.Error("对方未上线"))
                            currentPlayer.uid == inviter.uid -> session.send(LyricsSockets.SM.Error("不能邀请自己"))
                            currentPlayer.room != null -> session.send(LyricsSockets.SM.Error("你已在游戏中"))
                            inviter.room != null -> session.send(LyricsSockets.SM.Error("对方已在游戏中"))
                            else -> {
                                val info = currentPlayer.info
                                val inviterInfo = inviter.info
                                inviter.inviteJob?.let { job ->
                                    job.cancel()
                                    inviter.inviteJob = null
                                    if (msg.accept) {
                                        val room = Room(inviterInfo, info)
                                        inviter.room = room
                                        currentPlayer.room = room
                                        scope.launch { prepareGame(room) }
                                        inviter.session.send(LyricsSockets.SM.GamePrepare(inviterInfo, info))
                                        session.send(LyricsSockets.SM.GamePrepare(info, inviterInfo))
                                    }
                                    else inviter.session.send(LyricsSockets.SM.RefuseInvitation(info))
                                }
                            }
                        }
                    }
                    is LyricsSockets.CM.SaveAnswer if currentPlayer != null -> {
                        currentPlayer.room?.let { room ->
                            val triple = when (currentPlayer.uid) {
                                room.info1.uid -> Triple(room.answers1, room.info2.uid, room.answers2)
                                room.info2.uid -> Triple(room.answers2, room.info1.uid, room.answers1)
                                else -> null
                            }
                            triple?.let { (answers, otherUid, otherAnswers) ->
                                if (msg.index in 0 ..< LyricsSockets.QUESTION_COUNT) {
                                    answers[msg.index] = msg.answer
                                    // 通知进度
                                    val count = answers.count { it != null }
                                    val otherCount = otherAnswers.count { it != null }
                                    session.send(LyricsSockets.SM.AnswerUpdated(count, otherCount))
                                    val otherPlayer = players[otherUid]
                                    if (otherPlayer?.room?.roomId == room.roomId) {
                                        otherPlayer.session.send(LyricsSockets.SM.AnswerUpdated(otherCount, count))
                                    }
                                }
                            }
                        }
                    }
                    LyricsSockets.CM.Submit if currentPlayer != null -> currentPlayer.room?.let { submitAnswers(it, currentPlayer.uid) }
                    else -> {}
                }
            }
        }
        catch (_: Throwable) {
            session.send(LyricsSockets.SM.Error("连接服务器异常"))
        }
        finally {
            currentPlayer?.let { player ->
                player.room?.let { room -> submitAnswers(room, player.uid) }
                players.remove(player.uid)
            }
            session.close(CloseReason(CloseReason.Codes.NORMAL, "连接关闭"))
        }
    }
}