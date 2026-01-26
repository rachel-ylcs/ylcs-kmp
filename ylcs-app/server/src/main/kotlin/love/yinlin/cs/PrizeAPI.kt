package love.yinlin.cs

import love.yinlin.cs.APIConfig.coercePageNum
import love.yinlin.cs.service.*
import love.yinlin.cs.user.*
import love.yinlin.data.rachel.prize.Prize
import love.yinlin.data.rachel.prize.PrizeItem
import love.yinlin.data.rachel.profile.UserLevel
import love.yinlin.data.rachel.profile.UserPrivilege
import love.yinlin.extension.DateEx
import love.yinlin.extension.Int
import love.yinlin.extension.IntNull
import love.yinlin.extension.Object
import love.yinlin.extension.String
import love.yinlin.extension.StringNull
import love.yinlin.extension.to
import love.yinlin.extension.toJson
import love.yinlin.extension.makeObject
import java.security.MessageDigest
import java.security.SecureRandom
import java.math.BigInteger
import kotlin.io.encoding.Base64

fun APIScope.prizeAPI() {
    ApiPrizeGetPrize.response { pid, num ->
        VN.throwId(pid)
        val prizebody = db.throwQuerySQL("""
                SELECT pid, title, content, organizer_uid AS organizerUid,
                    deadline, drawtime,
                    draw_num AS drawNum, mix_app_level AS mixAppLevel , total_slots AS totalSlots,
                    ts,
                    seed_commitment AS seedCommitment, revealed_seed AS revealedSeed,
                    IF(COALESCE(is_drawn, 0) = 1, TRUE, FALSE) AS isDrawn
                FROM prize
                WHERE pid < ?
                ORDER BY pid DESC
                LIMIT ?
            """, pid, num.coercePageNum)

        // 对每个prizeitem进行单独处理
        val result = prizebody.map { prizeRow ->
            val prizePid = prizeRow.Object["pid"].Int
            val prizeItemsRows = db.throwQuerySQL("""
                SELECT item_id AS itemID, pid, name, description, pic, count
                FROM prize_item
                WHERE pid = ?
            """, prizePid)

            val prizeItems = prizeItemsRows.map { itemRow ->
                itemRow.to<PrizeItem>()
            }

            // 为prizeItems构建json对象
            // 转换isDrawn从0/1到布尔值
            val isDrawnValue = prizeRow.Object["isDrawn"].Int == 1
            val prizeJson = makeObject {
                merge(prizeRow.Object)
                "isDrawn" with isDrawnValue
                arr("prizeItems") {
                    for (item in prizeItems) {
                        add(item.toJson())
                    }
                }
            }

            prizeJson.to<Prize>()
        }

        result(result)
    }

    ApiPrizeCreatePrize.response { token, prizedata ->
        val uid = AN.throwExpireToken(token)
        val user = db.throwGetUser(uid, "privilege")
        if (!UserPrivilege.vipPrize(user["privilege"].Int)) failure("无权限")
        
        VN.throwEmpty(prizedata.title, prizedata.content)
        
        // 创建 32-byte的随机数种子和 SHA256 的公开承诺
        val seed = ByteArray(32)
        SecureRandom().nextBytes(seed)
        val seedBase64 = Base64.encode(seed)
        
        val commitment = MessageDigest.getInstance("SHA-256").digest(seed)
        val commitmentBase64 = Base64.encode(commitment)
        
        // 创建并发布抽奖，存储种子（私密）和承诺（公开）
        // 种子将在抽签时揭晓，揭晓前的种子值保持为空（实际存储在seed字段中，但revealed_seed为空）
        // drawNum初始为0，后续添加奖品后会自动更新
        val pid = db.throwInsertSQLGeneratedKey("""
            INSERT INTO prize(title, content, organizer_uid, deadline, drawtime, draw_num, mix_app_level, total_slots, seed, seed_commitment, ts)
            ${values(11)}
        """, prizedata.title, prizedata.content, uid,
            prizedata.deadline, prizedata.drawtime,
            0, prizedata.mixAppLevel, prizedata.totalSlots, seedBase64, commitmentBase64, currentTS).toInt()
        
        result(pid)
    }

    ApiPrizeUpdatePrize.response { token, pid, prizedata ->
        VN.throwId(pid)
        val uid = AN.throwExpireToken(token)
        val user = db.throwGetUser(uid, "privilege")
        if (!UserPrivilege.vipPrize(user["privilege"].Int)) failure("无权限")
        
        VN.throwEmpty(prizedata.title, prizedata.content)
        
        // 检查是否已开奖
        val prize = db.throwQuerySQLSingle("""
            SELECT is_drawn FROM prize WHERE pid = ?
        """, pid)

        if (prize["is_drawn"].Int == 1) {
            failure("已开奖的抽奖无法修改")
        }
        

        db.throwExecuteSQL("""
            UPDATE prize 
            SET title = ?, content = ?, deadline = ?, drawtime = ?, mix_app_level = ?, total_slots = ?
            WHERE pid = ?
        """, prizedata.title, prizedata.content, prizedata.deadline, prizedata.drawtime,
            prizedata.mixAppLevel, prizedata.totalSlots, pid)

    }


    ApiPrizeAddItem.response { token, pid, pic, itemdata ->
        VN.throwId(pid)
        val uid = AN.throwExpireToken(token)
        val user = db.throwGetUser(uid, "privilege")
        if (!UserPrivilege.vipPrize(user["privilege"].Int)) failure("无权限")
        
        VN.throwEmpty(itemdata.name)
        if (itemdata.count <= 0) failure("奖品数量必须大于0")
        
        // 检查抽奖权限和是否已开奖
        val prize = db.throwQuerySQLSingle("""
            SELECT is_drawn FROM prize WHERE pid = ?
        """, pid)
        
        // 只要没开奖就可以添加奖品
        if (prize["is_drawn"].Int == 1) {
            failure("已开奖的抽奖无法添加奖品")
        }
        
        val itemId = db.throwTransaction {
            // 插入奖品
            val itemIdValue = it.throwInsertSQLGeneratedKey("""
                INSERT INTO prize_item(pid, name, description, pic, count)
                ${values(5)}
            """, pid, itemdata.name, itemdata.description, null, itemdata.count).toInt()
            
            // 处理图片
            if (pic != null && !pic.isEmpty) {
                val picName = itemIdValue.toString()
                val prizePic = ServerRes.Prize.prize(itemIdValue)
                pic.copy(prizePic)
                it.throwExecuteSQL("""
                    UPDATE prize_item SET pic = ? WHERE item_id = ?
                """, picName, itemIdValue)
            }
            
            // 更新抽奖的 drawNum（所有奖品数量的总和）
            val totalCount = it.throwQuerySQLSingle("""
                SELECT COALESCE(SUM(count), 0) AS total FROM prize_item WHERE pid = ?
            """, pid)["total"].Int
            
            it.throwExecuteSQL("""
                UPDATE prize SET draw_num = ? WHERE pid = ?
            """, totalCount, pid)
            
            itemIdValue
        }
        
        result(itemId)
    }


    ApiPrizeDeleteItem.response { token, pid, itemID ->
        VN.throwId(pid)
        VN.throwId(itemID)
        val uid = AN.throwExpireToken(token)
        val user = db.throwGetUser(uid, "privilege")
        if (!UserPrivilege.vipPrize(user["privilege"].Int)) failure("无权限")
        
        // 检查抽奖是否已开奖
        val prize = db.throwQuerySQLSingle("""
            SELECT is_drawn FROM prize WHERE pid = ?
        """, pid)
        
        // 只要没开奖就可以删除奖品
        if (prize["is_drawn"].Int == 1) {
            failure("已开奖的抽奖无法删除奖品")
        }
        
        db.throwTransaction {
            // 获取奖品信息
            it.querySQLSingle("""
                SELECT pic FROM prize_item WHERE item_id = ? AND pid = ?
            """, itemID, pid) ?: failure("奖品不存在")

            // 删除奖品
            it.throwExecuteSQL("DELETE FROM prize_item WHERE item_id = ? AND pid = ?", itemID, pid)
            
            // 更新抽奖的 drawNum
            val totalCount = it.throwQuerySQLSingle("""
                SELECT COALESCE(SUM(count), 0) AS total FROM prize_item WHERE pid = ?
            """, pid)["total"].Int
            
            it.throwExecuteSQL("""
                UPDATE prize SET draw_num = ? WHERE pid = ?
            """, totalCount, pid)
        }

    }
    
    // 更新奖品
    ApiPrizeUpdateItem.response { token, itemID, pic, itemdata ->
        VN.throwId(itemID)
        val uid = AN.throwExpireToken(token)
        val user = db.throwGetUser(uid, "privilege")
        if (!UserPrivilege.vipPrize(user["privilege"].Int)) failure("无权限")

        VN.throwEmpty(itemdata.name)
        if (itemdata.count <= 0) failure("奖品数量必须大于0")

        // 获取奖品所属的抽奖
        val item = db.throwQuerySQLSingle("""
            SELECT pid, pic FROM prize_item WHERE item_id = ?
        """, itemID)

        val pid = item["pid"].Int
        val oldPic = item["pic"].StringNull

        // 检查抽奖权限和是否已开奖
        val prize = db.throwQuerySQLSingle("""
            SELECT  is_drawn FROM prize WHERE pid = ?
        """, pid)

        // 只要没开奖就可以修改奖品
        if (prize["is_drawn"].Int == 1) {
            failure("已开奖的抽奖无法修改奖品")
        }

        db.throwTransaction {
            // 处理图片更新
            val picNameValue = when {
                pic != null && !pic.isEmpty -> {
                    // 上传新图片
                    val newPicName = itemID.toString()
                    val prizePic = ServerRes.Prize.prize(itemID)

                    // 删除旧图片
                    if (oldPic != null) {
                        try {
                            prizePic.delete()
                        } catch (e: Exception) {
                            // 忽略删除失败
                        }
                    }

                    pic.copy(prizePic)
                    newPicName
                }
                pic == null && oldPic != null -> {
                    // pic传入null表示无图片
                    try {
                        val prizePic = ServerRes.Prize.prize(itemID)
                        prizePic.delete()
                    } catch (e: Exception) {
                    }
                    null
                }
                else -> {
                    // 保持原图
                    oldPic
                }
            }

            // 更新奖品信息
            it.throwExecuteSQL("""
                UPDATE prize_item 
                SET name = ?, description = ?, pic = ?, count = ?
                WHERE item_id = ?
            """, itemdata.name, itemdata.description, picNameValue, itemdata.count, itemID)

            // 更新抽奖的 drawNum
            val totalCount = it.throwQuerySQLSingle("""
                SELECT COALESCE(SUM(count), 0) AS total FROM prize_item WHERE pid = ?
            """, pid)["total"].Int

            it.throwExecuteSQL("""
                UPDATE prize SET draw_num = ? WHERE pid = ?
            """, totalCount, pid)

        }
        result(itemID)
    }

    ApiPrizeDeletePrize.response { token, pid ->
        VN.throwId(pid)
        val uid = AN.throwExpireToken(token)
        val user = db.throwGetUser(uid, "privilege")
        if (!UserPrivilege.vipPrize(user["privilege"].Int)) failure("无权限")
        
        // 检查抽奖是否存在
        db.querySQLSingle("""
            SELECT organizer_uid FROM prize WHERE pid = ?
        """, pid) ?: failure("抽奖不存在")
        
        // 使用事务删除所有相关数据
        db.throwTransaction {
            // 1. 删除参与记录（prize_draw表）
            it.throwExecuteSQL("DELETE FROM prize_draw WHERE pid = ?", pid)
            
            // 2. 删除奖品（prize_item表）
            it.throwExecuteSQL("DELETE FROM prize_item WHERE pid = ?", pid)
            
            // 3. 删除抽奖主体（prize表）
            it.throwExecuteSQL("DELETE FROM prize WHERE pid = ?", pid)
        }
        
        result(pid)
    }

    ApiPrizeParticipate.response { token, pid ->
        VN.throwId(pid)
        val uid = AN.throwExpireToken(token)

        //检查抽奖是否存在并获取信息
        val prize = db.throwQuerySQLSingle("""
            SELECT seed_commitment, is_drawn, deadline, mix_app_level, total_slots,
                (SELECT COUNT(*) FROM prize_draw WHERE pid = ?) AS current_participants
            FROM prize WHERE pid = ?
        """, pid, pid)

        // 这是一次保险，检查公开承诺是否存在，即抽奖是否正常
        if (prize["seed_commitment"].StringNull == null) {
            failure("该抽奖尚未发布")
        }

        // 检查是否已开奖
        if (prize["is_drawn"].Int == 1) {
            failure("该抽奖已开奖，无法参加")
        }

        // 检查截止日期 - 比较日期时间字符串
        val deadline = prize["deadline"].String
        val currentTSString = DateEx.CurrentString
        if (deadline <= currentTSString) failure("抽奖已截止")

        // 检查是否重复参加
        val existing = db.querySQLSingle("SELECT id FROM prize_draw WHERE pid = ? AND uid = ?", pid, uid)

        if (existing != null) failure("你已经报名参加这个抽奖，请勿重复参加")

        // 检查是否满足最小参与等级
        val mixAppLevel = prize["mix_app_level"].Int
        if (mixAppLevel > 0) {
            val user = db.throwGetUser(uid, "exp")
            val userLevel = UserLevel.level(user["exp"].Int)
            if (userLevel < mixAppLevel) failure("等级不足，无法参加此抽奖，该抽奖需求的最小level为$mixAppLevel")
        }

        // 检查是否参与名额已满
        val totalSlots = prize["total_slots"].IntNull
        val currentParticipants = prize["current_participants"].Int
        if (totalSlots != null && currentParticipants >= totalSlots) failure("抽奖名额已满")

        val userName = db.throwGetUser(uid, "name")["name"].String

        // 执行插入操作 - 只记录参与信息
        db.throwExecuteSQL("""
            INSERT INTO prize_draw(pid, uid, name, ts)
            ${values(4)}
        """, pid, uid, userName, currentTS)
    }

    ApiPrizeGetWinners.response { pid ->
        VN.throwId(pid)

        // 检查是否已开奖
        val prize = db.throwQuerySQLSingle("""
            SELECT is_drawn FROM prize WHERE pid = ?
        """, pid)

        if (prize["is_drawn"].Int == 0) {
            failure("还未开奖")
        }

        // 查询所有中奖者数据（按 itemid 分组）
        val winnersData = db.throwQuerySQL("""
            SELECT itemid, CAST(winner_list AS CHAR) AS winner_list FROM prize_winner WHERE pid = ?
        """, pid)

        // 构造返回格式：{itemid1:[uid1,uid2], itemid2:[uid3], ...}
        val resultJson = winnersData.joinToString(
            prefix = "{",
            postfix = "}",
            separator = ","
        ) { row ->
            val itemid = row.Object["itemid"].Int
            val winnerListJson = row.Object["winner_list"].String
            """"$itemid":$winnerListJson"""
        }

        result(resultJson)
    }


    ApiPrizeDrawPrize.response { token, pid ->
        VN.throwId(pid)
        val uid = AN.throwExpireToken(token)
        val user = db.throwGetUser(uid, "privilege")
        if (!UserPrivilege.vipPrize(user["privilege"].Int)) failure("无权限")

        val prize = db.throwQuerySQLSingle("""
            SELECT seed, seed_commitment, draw_num, is_drawn
            FROM prize WHERE pid = ?
        """, pid)

        // 检查是否已发布（seed_commitment 不为空表示已发布）
        if (prize["seed_commitment"].StringNull == null) {
            failure("该抽奖尚未发布，无法开奖")
        }

        // 检查是否已开奖
        if (prize["is_drawn"].Int == 1) {
            failure("该抽奖已经开过奖了")
        }

        val seed = prize["seed"].StringNull ?: failure("抽奖种子未生成，请联系管理员")
        val drawNum = prize["draw_num"].Int

        // 获取所有奖品及其数量
        val prizeItems = db.throwQuerySQL("""
            SELECT item_id, count FROM prize_item WHERE pid = ? ORDER BY item_id ASC
        """, pid)

        if (prizeItems.isEmpty()) failure("没有设置奖品，无法开奖")

        // 获取所有参与者（按 uid 排序以保证确定性）
        val participants = db.throwQuerySQL("""
            SELECT uid FROM prize_draw WHERE pid = ? ORDER BY uid ASC
        """, pid)

        if (participants.isEmpty()) failure("没有参与者，无法开奖")

        // 使用承诺揭示机制打乱参与者顺序
        val sortedUids = participants.map { it.Object["uid"].Int }
        val seedBytes = Base64.decode(seed)

        // 生成初始随机源：种子+排序uid列表
        val uidString = sortedUids.joinToString(",")
        val initialData = seedBytes + uidString.toByteArray()
        var currentHash = MessageDigest.getInstance("SHA-256").digest(initialData)

        // 使用 Fisher-Yates 洗牌算法打乱参与者顺序
        val shuffledUids = sortedUids.toMutableList()
        for (i in shuffledUids.size - 1 downTo 1) {
            val bigInt = BigInteger(1, currentHash)
            val j = (bigInt.mod(BigInteger.valueOf((i + 1).toLong()))).toInt()
            
            // 交换 shuffledUids[i] 和 shuffledUids[j]
            val temp = shuffledUids[i]
            shuffledUids[i] = shuffledUids[j]
            shuffledUids[j] = temp
            
            // 更新随机源
            currentHash = MessageDigest.getInstance("SHA-256").digest(currentHash)
        }

        // 从左往右按照奖品顺序分配中奖者
        val itemWinners = mutableMapOf<Int, MutableList<Int>>()
        var currentIndex = 0
        var totalWinners = 0

        for (itemRow in prizeItems) {
            val itemId = itemRow.Object["item_id"].Int
            val count = itemRow.Object["count"].Int
            val winners = mutableListOf<Int>()

            // 分配该奖品的中奖者
            repeat(count) {
                if (currentIndex < shuffledUids.size) {
                    winners.add(shuffledUids[currentIndex])
                    currentIndex++
                    totalWinners++
                }
                // 如果参与人数不足，该奖品空缺
            }

            itemWinners[itemId] = winners
        }

        // 用transaction更新数据库，确保原子性
        db.throwTransaction {
            // 设置开奖状态，公开种子
            it.throwExecuteSQL(
                """
                UPDATE prize SET revealed_seed = ?, is_drawn = 1 WHERE pid = ?
            """, seed, pid
            )

            // 插入中奖者记录到 prize_winner 表（按 itemid 存储）
            for ((itemId, winners) in itemWinners) {
                // 将 winners 列表转换为 JSON 数组字符串 [uid1,uid2,uid3]
                val winnerListJson = winners.joinToString(
                    prefix = "[",
                    postfix = "]",
                    separator = ","
                )
                
                it.throwExecuteSQL(
                    """
                    INSERT INTO prize_winner(pid, itemid, winner_list)
                    ${values(3)}
                """, pid, itemId, winnerListJson
                )
            }
        }

        val message = if (participants.size < drawNum) {
            "开奖成功，参与人数(${participants.size})少于奖品总数($drawNum)，已分配${totalWinners}个奖品，剩余${drawNum - totalWinners}个奖品空缺"
        } else {
            "开奖成功，共分配${totalWinners}个奖品给${totalWinners}位中奖者"
        }
        result(message)
    }

}