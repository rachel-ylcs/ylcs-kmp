package love.yinlin.api

import love.yinlin.api.APIConfig.coercePageNum
import love.yinlin.api.user.AN
import love.yinlin.api.user.VN
import love.yinlin.api.user.throwGetUser
import love.yinlin.data.rachel.prize.Prize
import love.yinlin.data.rachel.prize.PrizeItem
import love.yinlin.data.rachel.prize.PrizeItemPic
import love.yinlin.data.rachel.prize.PrizeResult
import love.yinlin.data.rachel.prize.PrizeStatus
import love.yinlin.data.rachel.profile.UserLevel
import love.yinlin.data.rachel.profile.UserPrivilege
import love.yinlin.extension.Int
import love.yinlin.extension.IntNull
import love.yinlin.extension.Object
import love.yinlin.extension.String
import love.yinlin.extension.StringNull
import love.yinlin.extension.to
import love.yinlin.extension.toJson
import love.yinlin.extension.makeObject
import love.yinlin.server.currentTS
import love.yinlin.server.deleteSQL
import love.yinlin.server.querySQLSingle
import java.time.format.DateTimeFormatter
import love.yinlin.server.throwExecuteSQL
import love.yinlin.server.throwInsertSQLGeneratedKey
import love.yinlin.server.throwQuerySQL
import love.yinlin.server.throwQuerySQLSingle
import love.yinlin.server.values
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import java.math.BigInteger

fun APIScope.prizeAPI() {
    ApiPrizeGetPrize.response { pid, num, status,token ->
        VN.throwId(pid)
        val uid = AN.throwExpireToken(token)
        val user = db.throwGetUser(uid, "privilege")

        if (!UserPrivilege.vipPrize(user["privilege"].Int) && status.name=="draft") throw FailureException("只有prize发布者才可以查看草稿")

        val prizebody = db.throwQuerySQL("""
                SELECT pid, title, content, organizer_uid AS organizerUid, status,
                    DATE_FORMAT(deadline, '%Y-%m-%d %H:%i:%s') AS deadline,
                    DATE_FORMAT(drawtime, '%Y-%m-%d %H:%i:%s') AS drawtime,
                    draw_num AS drawNum, mix_app_level AS mixAppLevel , total_slots AS totalSlots,
                    DATE_FORMAT(ts, '%Y-%m-%d %H:%i:%s') AS ts,
                    seed_commitment AS seedCommitment, revealed_seed AS revealedSeed
                FROM prize
                WHERE pid < ? AND status = ?
                ORDER BY pid DESC
                LIMIT ?
            """, pid, status.name, num.coercePageNum)

        // 对每个prizeitem进行单独处理
        val result = prizebody.map { prizeRow ->
            val prizePid = prizeRow.Object["pid"].Int
            val prizeItemsRows = db.throwQuerySQL("""
                SELECT item_id AS itemID, pid, prize_level AS prizeLevel, name, description, pic, count
                FROM prize_item
                WHERE pid = ?
            """, prizePid)

            val prizeItems = prizeItemsRows.map { itemRow ->
                itemRow.to<PrizeItem>()
            }

            // 为prizeItems构建json对象
            val prizeJson = makeObject {
                merge(prizeRow.Object)
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
        
        // 创建草稿，drawNum初始为0，后续添加奖品后会自动更新
        val pid = db.throwInsertSQLGeneratedKey("""
            INSERT INTO prize(title, content, organizer_uid, status, deadline, drawtime, draw_num, mix_app_level, total_slots, ts)
            ${values(10)}
        """, prizedata.title, prizedata.content, uid,
            PrizeStatus.Draft.name, prizedata.deadline, prizedata.drawtime,
            0, prizedata.mixAppLevel, prizedata.totalSlots, currentTS).toInt()
        
        result(pid)
    }
    
    // 修改抽奖主体
    ApiPrizeUpdatePrize.response { token, pid, prizedata ->
        VN.throwId(pid)
        val uid = AN.throwExpireToken(token)
        val user = db.throwGetUser(uid, "privilege")
        if (!UserPrivilege.vipPrize(user["privilege"].Int)) failure("无权限")
        
        VN.throwEmpty(prizedata.title, prizedata.content)
        
        // 检查抽奖状态和权限
        val prize = db.throwQuerySQLSingle("""
            SELECT status, organizer_uid FROM prize WHERE pid = ?
        """, pid)
        
        if (prize["status"].String != PrizeStatus.Draft.name) {
            failure("只能修改草稿状态的抽奖")
        }
        
        if (prize["organizer_uid"].Int != uid) {
            failure("只有创建者可以修改")
        }
        
        // 更新抽奖主体信息
        db.throwExecuteSQL("""
            UPDATE prize 
            SET title = ?, content = ?, deadline = ?, drawtime = ?, mix_app_level = ?, total_slots = ?
            WHERE pid = ?
        """, prizedata.title, prizedata.content, prizedata.deadline, prizedata.drawtime,
            prizedata.mixAppLevel, prizedata.totalSlots, pid)

    }
    
    // 删除抽奖草稿（包括所有关联的奖品）
    ApiPrizeDeletePrizeDraft.response { token, pid ->
        VN.throwId(pid)
        val uid = AN.throwExpireToken(token)
        val user = db.throwGetUser(uid, "privilege")
        if (!UserPrivilege.vipPrize(user["privilege"].Int)) failure("无权限")
        
        // 检查抽奖状态和权限
        val prize = db.throwQuerySQLSingle("""
            SELECT status, organizer_uid FROM prize WHERE pid = ?
        """, pid)
        
        if (prize["status"].String != PrizeStatus.Draft.name) {
            failure("只能删除草稿状态的抽奖，当前状态为：${prize["status"].String}")
        }
        
        if (prize["organizer_uid"].Int != uid) {
            failure("只有创建者可以删除")
        }
        
        db.throwTransaction {
            // 获取所有奖品的图片，用于删除文件
            val items = it.throwQuerySQL("""
                SELECT item_id, pic FROM prize_item WHERE pid = ?
            """, pid)
            
            // 删除奖品图片文件
            items.forEach { itemRow ->
                val itemId = itemRow.Object["item_id"].Int
                val pic = itemRow.Object["pic"].StringNull
                if (pic != null) {
                    try {
                        val picFile = ServerRes.Prize.prize(itemId)
                        picFile.delete()
                    } catch (e: Exception) {
                        // 文件删除失败不影响数据库操作
                    }
                }
            }
            
            // 删除所有奖品
            it.deleteSQL("DELETE FROM prize_item WHERE pid = ?", pid)
            
            // 删除抽奖主体
            it.throwExecuteSQL("DELETE FROM prize WHERE pid = ?", pid)
        }

    }

    // 添加奖品到指定抽奖
    ApiPrizeAddItem.response { token, pid, pic, itemdata ->
        VN.throwId(pid)
        val uid = AN.throwExpireToken(token)
        val user = db.throwGetUser(uid, "privilege")
        if (!UserPrivilege.vipPrize(user["privilege"].Int)) failure("无权限")
        
        VN.throwEmpty(itemdata.name)
        if (itemdata.count <= 0) failure("奖品数量必须大于0")
        
        // 检查抽奖状态和权限
        val prize = db.throwQuerySQLSingle("""
            SELECT status, organizer_uid FROM prize WHERE pid = ?
        """, pid)
        
        if (prize["status"].String != PrizeStatus.Draft.name) {
            failure("只能为草稿状态的抽奖添加奖品")
        }
        
        if (prize["organizer_uid"].Int != uid) {
            failure("只有创建者可以添加奖品")
        }
        
        val (itemId, picName) = db.throwTransaction {
            // 插入奖品
            val itemIdValue = it.throwInsertSQLGeneratedKey("""
                INSERT INTO prize_item(pid, prize_level, name, description, pic, count)
                ${values(6)}
            """, pid, itemdata.prizeLevel, itemdata.name, itemdata.description, null, itemdata.count).toInt()
            
            // 处理图片
            val picNameValue = if (pic != null && !pic.isEmpty) {
                val picName = itemIdValue.toString()
                val prizePic = ServerRes.Prize.prize(itemIdValue)
                pic.copy(prizePic)
                it.throwExecuteSQL("""
                    UPDATE prize_item SET pic = ? WHERE item_id = ?
                """, picName, itemIdValue)
                picName
            } else {
                null
            }
            
            // 更新抽奖的 drawNum（所有奖品数量的总和）
            val totalCount = it.throwQuerySQLSingle("""
                SELECT COALESCE(SUM(count), 0) AS total FROM prize_item WHERE pid = ?
            """, pid)["total"].Int
            
            it.throwExecuteSQL("""
                UPDATE prize SET draw_num = ? WHERE pid = ?
            """, totalCount, pid)
            
            itemIdValue to picNameValue
        }
        
        result(PrizeItemPic(itemId, picName))
    }
    
    // 删除奖品
    ApiPrizeDeleteItem.response { token, pid, itemID ->
        VN.throwId(pid)
        VN.throwId(itemID)
        val uid = AN.throwExpireToken(token)
        val user = db.throwGetUser(uid, "privilege")
        if (!UserPrivilege.vipPrize(user["privilege"].Int)) failure("无权限")
        
        // 检查抽奖状态和权限
        val prize = db.throwQuerySQLSingle("""
            SELECT status FROM prize WHERE pid = ?
        """, pid)
        
        if (prize["status"].String != PrizeStatus.Draft.name) {
            failure("只能删除草稿状态的抽奖中的奖品")
        }
        
        db.throwTransaction {
            // 获取奖品信息
            val item = it.querySQLSingle("""
                SELECT pic FROM prize_item WHERE item_id = ? AND pid = ?
            """, itemID, pid) ?: failure("奖品不存在")
            
            // 删除图片文件
            val pic = item["pic"].StringNull
            if (pic != null) {
                try {
                    val picFile = ServerRes.Prize.prize(itemID)
                    picFile.delete()
                } catch (e: Exception) {
                    // 文件删除失败不影响数据库操作
                }
            }
            
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

        // 检查抽奖状态和权限
        val prize = db.throwQuerySQLSingle("""
            SELECT status, organizer_uid FROM prize WHERE pid = ?
        """, pid)

        if (prize["status"].String != PrizeStatus.Draft.name) {
            failure("只能修改草稿状态的抽奖中的奖品")
        }

        if (prize["organizer_uid"].Int != uid) {
            failure("只有创建者可以修改奖品")
        }

        var finalPicName: String? = null
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
                    // pic传入null表示删除图片
                    try {
                        val prizePic = ServerRes.Prize.prize(itemID)
                        prizePic.delete()
                    } catch (e: Exception) {
                        // 忽略删除失败
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
                SET prize_level = ?, name = ?, description = ?, pic = ?, count = ?
                WHERE item_id = ?
            """, itemdata.prizeLevel, itemdata.name, itemdata.description, picNameValue, itemdata.count, itemID)

            // 更新抽奖的 drawNum
            val totalCount = it.throwQuerySQLSingle("""
                SELECT COALESCE(SUM(count), 0) AS total FROM prize_item WHERE pid = ?
            """, pid)["total"].Int

            it.throwExecuteSQL("""
                UPDATE prize SET draw_num = ? WHERE pid = ?
            """, totalCount, pid)

            finalPicName = picNameValue
        }
        result(PrizeItemPic(itemID,finalPicName))
    }

    ApiPrizePublish.response { pid, token ->
        VN.throwId(pid)
        val uid = AN.throwExpireToken(token)
        val user = db.throwGetUser(uid, "privilege")
        if (!UserPrivilege.vipPrize(user["privilege"].Int)) failure("无权限")

        val prize = db.throwQuerySQLSingle("""
            SELECT pid, organizer_uid, status FROM prize WHERE pid = ?
        """, pid)

        if (prize["status"].String != PrizeStatus.Draft.name) failure("只能发布草稿状态的抽奖")

        // 验证抽奖是否有奖品
        val items = db.throwQuerySQL("""
            SELECT prize_level, count FROM prize_item WHERE pid = ? ORDER BY prize_level
        """, pid)

        if (items.isEmpty()) {
            failure("抽奖至少需要一个奖品")
        }

        // 验证奖品等级：必须按 1, 2, 3 排序，不允许 null
        val levels = items.mapNotNull { it.Object["prize_level"].IntNull }

        if (levels.size != items.size) {
            failure("所有奖品必须设置等级（1, 2, 3...）")
        }

        val uniqueLevels = levels.distinct().sorted()

        // 检查是否从1开始连续
        val expectedLevels = (1..uniqueLevels.size).toList()
        if (uniqueLevels != expectedLevels) {
            failure("奖品等级必须从1开始连续排序（1, 2, 3...），不允许跳过或重复。当前等级：${uniqueLevels.joinToString(", ")}")
        }

        // 创建 32-byte的随机数种子和 SHA256 的公开承诺
        val seed = ByteArray(32)
        SecureRandom().nextBytes(seed)
        val seedBase64 = Base64.getEncoder().encodeToString(seed)

        val commitment = MessageDigest.getInstance("SHA-256").digest(seed)
        val commitmentBase64 = Base64.getEncoder().encodeToString(commitment)

        // 存储种子（私密）和承诺（公开）
        // 种子将在抽签时揭晓，揭晓前的种子值保持为空
        db.throwExecuteSQL("""
            UPDATE prize SET status = ?, seed = ?, seed_commitment = ? WHERE pid = ?
        """, PrizeStatus.Published.name, seedBase64, commitmentBase64, pid)

        result("pid:$pid 发布成功")

    }

    ApiPrizeCancelPrize.response { token, pid ->
        VN.throwId(pid)
        val uid = AN.throwExpireToken(token)
        val user = db.throwGetUser(uid, "privilege")
        if (!UserPrivilege.vipPrize(user["privilege"].Int)) failure("无权限")

        val prize=db.throwQuerySQLSingle("SELECT status FROM prize WHERE pid = ?", pid)
        if(prize["status"].String != PrizeStatus.Published.name) failure("抽奖不存在或状态不为published，无法取消")

        db.updateSQL("""
            UPDATE prize SET status = ? WHERE pid = ?
        """, PrizeStatus.Cancelled.name, pid)
        db.updateSQL("""
            UPDATE prize_draw SET result=? WHERE pid = ?
        """, PrizeResult.Cancelled.name,pid)

        result("pid:$pid 成功被取消")
    }

    ApiPrizeParticipate.response { token, pid ->
        VN.throwId(pid)
        val uid = AN.throwExpireToken(token)

        //检查是否存在
        val prize = db.throwQuerySQLSingle("""
            SELECT status, deadline, mix_app_level, total_slots,
                (SELECT COUNT(*) FROM prize_draw WHERE pid = ?) AS current_participants
            FROM prize WHERE pid = ?
        """, pid, pid)

        if (prize["status"].String != PrizeStatus.Published.name) failure("只能参加已发布的抽奖")

        // 检查截止日期 - 比较日期时间字符串
        val deadline = prize["deadline"].String
        val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        val currentTSString = currentTS.toLocalDateTime().format(dateTimeFormatter)
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

        // 执行插入操作
        db.throwExecuteSQL("""
            INSERT INTO prize_draw(pid, uid, result, ts, name, prize_level)
            ${values(6)}
        """, pid, uid, PrizeResult.Notdrawn.name, currentTS, userName,null)
    }

    ApiPrizeGetWinners.response { pid ->
        VN.throwId(pid)

        // 检查是否手动开奖
        val prize = db.throwQuerySQLSingle("""
            SELECT  status FROM prize WHERE pid = ?
        """, pid)

        if (prize["status"].String != PrizeStatus.Closed.name) failure("还未开奖或已被取消")

        val winners = db.throwQuerySQL("""
            SELECT id AS drawid, pid, uid, result, prize_level AS prizeLevel , name,
                DATE_FORMAT(ts, '%Y-%m-%d %H:%i:%s') AS ts
            FROM prize_draw
            WHERE pid = ? AND result = ?
            ORDER BY prize_level ASC, drawid ASC
        """, pid, PrizeResult.Win.name)

        result(winners.to())
    }

    ApiPrizeGetAllParticipators.response { pid, drawid, num ->
        VN.throwId(pid)
        VN.throwId(drawid)
        // 按参与的先后顺序对参与者进行降序排序（最新参与的会放在最前面，确保用户可以看在参与列表里看到自己最新的操作），时间相同则安装uid降序
        val participators = db.throwQuerySQL("""
            SELECT id AS drawid, pid, uid, result , prize_level AS prizeLevel, name,
                DATE_FORMAT(ts, '%Y-%m-%d %H:%i:%s') AS ts
            FROM prize_draw
            WHERE pid = ? AND id < ?
            ORDER BY ts DESC, uid DESC
            LIMIT ?
        """, pid, drawid, num.coercePageNum)
        result(participators.to())
    }

    ApiPrizeDrawPrize.response { token, pid ->
        VN.throwId(pid)
        val uid = AN.throwExpireToken(token)
        val user = db.throwGetUser(uid, "privilege")
        if (!UserPrivilege.vipPrize(user["privilege"].Int)) failure("无权限")

        val prize = db.throwQuerySQLSingle("""
            SELECT pid, status, seed, seed_commitment, draw_num, revealed_seed
            FROM prize WHERE pid = ?
        """, pid)

        if (prize["status"].String != PrizeStatus.Published.name) failure("只能对已发布的抽奖进行开奖")
        if (prize["revealed_seed"].StringNull != null) failure("该抽奖已经开过奖了")

        val seed = prize["seed"].StringNull ?: failure("抽奖种子未生成，请联系管理员")
        val drawNum = prize["draw_num"].Int

        // Get all participants sorted by uid
        val participants = db.throwQuerySQL("""
            SELECT id, uid FROM prize_draw WHERE pid = ? ORDER BY uid ASC
        """, pid)

        if (participants.isEmpty()) failure("没有参与者，无法开奖")

        // 获取奖品等级信息，用于分配奖项
        val prizeItems = db.throwQuerySQL("""
            SELECT prize_level, count FROM prize_item WHERE pid = ? AND prize_level IS NOT NULL ORDER BY prize_level ASC
        """, pid)

        // 构建奖品等级分配列表：每个等级对应其数量
        val prizeLevelDistribution = mutableListOf<Int>()
        for (item in prizeItems) {
            val level = item.Object["prize_level"].Int
            val count = item.Object["count"].Int
            repeat(count) {
                prizeLevelDistribution.add(level)
            }
        }

        // 使用承诺揭示机制进行绘图
        val sortedUids = participants.map { it.Object["uid"].Int }
        val seedBytes = Base64.getDecoder().decode(seed)

        val winners = mutableListOf<Int>()

        // 特殊情况：当参与人数少于开奖人数时，所有参与者都获奖
        if (participants.size < drawNum) {
            winners.addAll(sortedUids)
        } else {
            // 正常情况：使用随机算法抽取中奖者
            // 生成初始随机源：种子+排序uid列表
            val uidString = sortedUids.joinToString(",")
            val initialData = seedBytes + uidString.toByteArray()
            var currentHash = MessageDigest.getInstance("SHA-256").digest(initialData)

            val remainingUids = sortedUids.toMutableList()

            repeat(drawNum) {
                //将哈希转换为BigInteger
                val bigInt = BigInteger(1, currentHash)
                // 计算胜者指数
                val winnerIndex = (bigInt.mod(BigInteger.valueOf(remainingUids.size.toLong()))).toInt()

                winners.add(remainingUids[winnerIndex])
                remainingUids.removeAt(winnerIndex)

                //更新随机源：使用之前的哈希值作为新的输入
                currentHash = MessageDigest.getInstance("SHA-256").digest(currentHash)
            }
        }

        // 为 winners 随机分配奖品等级
        val winnersWithLevels = mutableListOf<Pair<Int, Int?>>() // (uid, prizeLevel)

        if (prizeLevelDistribution.isEmpty()) {
            for (winnerUid in winners) {
                winnersWithLevels.add(winnerUid to null)
            }
        } else {
            // 有设置等级的奖品，需要随机分配
            val remainingLevels = prizeLevelDistribution.toMutableList()
            // 使用排序后的 winners 列表
            val sortedWinners = winners.sorted()
            val remainingWinners = sortedWinners.toMutableList()

            // 使用额外的哈希值来分配等级（基于已选出的中奖者列表）
            // 使用 winners 列表的字符串表示作为额外随机源
            val winnersString = sortedWinners.joinToString(",")
            val levelAllocationData = seedBytes + winnersString.toByteArray()
            var levelHash = MessageDigest.getInstance("SHA-256").digest(levelAllocationData)

            // 为每个中奖者随机分配等级
            while (remainingWinners.isNotEmpty() && remainingLevels.isNotEmpty()) {
                // 随机选择一个中奖者
                val winnerBigInt = BigInteger(1, levelHash)
                val winnerIndex = (winnerBigInt.mod(BigInteger.valueOf(remainingWinners.size.toLong()))).toInt()
                val winnerUid = remainingWinners[winnerIndex]
                remainingWinners.removeAt(winnerIndex)
                levelHash = MessageDigest.getInstance("SHA-256").digest(levelHash)

                // 随机选择一个等级
                val levelBigInt = BigInteger(1, levelHash)
                val levelIndex = (levelBigInt.mod(BigInteger.valueOf(remainingLevels.size.toLong()))).toInt()
                val prizeLevel = remainingLevels[levelIndex]
                remainingLevels.removeAt(levelIndex)
                levelHash = MessageDigest.getInstance("SHA-256").digest(levelHash)

                winnersWithLevels.add(winnerUid to prizeLevel)
            }

            // 如果还有剩余的中奖者但没有等级了（理论上不应该发生，因为 drawNum = 总奖品数）,这里把剩余的winner设置为null,依旧符合随机过程
            for (remainingWinner in remainingWinners) {
                winnersWithLevels.add(remainingWinner to null)
            }
        }

        // 用transaction更新数据库，确保原子性，防止出现开奖了但是没有正常选出中奖者的情况
        db.throwTransaction {
            it.throwExecuteSQL(
                """
                UPDATE prize SET revealed_seed = ?, status = ? WHERE pid = ?
            """, seed, PrizeStatus.Closed.name, pid
            )

            //这里有 N+1 问题，之后需要找时间解决
            for ((winnerUid, prizeLevel) in winnersWithLevels) {
                it.throwExecuteSQL(
                    """
                    UPDATE prize_draw SET result = ?, prize_level = ? WHERE pid = ? AND uid = ?
                """, PrizeResult.Win.name, prizeLevel, pid, winnerUid
                )
            }
            if (participants.size >= drawNum) {
                it.throwExecuteSQL(
                    """
                UPDATE prize_draw SET result = ? WHERE pid = ? AND result = ?
            """, PrizeResult.Loss.name, pid, PrizeResult.Notdrawn.name
                )
            }
        }

        val message = if (participants.size < drawNum) {
            "开奖成功，参与人数(${participants.size})少于开奖人数($drawNum)，所有参与者均获奖，共${winners.size}位中奖者"
        } else {
            "开奖成功，共抽取${winners.size}位中奖者"
        }
        result(message)
    }

}
