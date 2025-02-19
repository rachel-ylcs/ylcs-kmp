package love.yinlin.api.user

import io.ktor.server.routing.Routing
import love.yinlin.DB
import love.yinlin.Resources
import love.yinlin.api.API
import love.yinlin.api.ImplMap
import love.yinlin.api.api
import love.yinlin.api.failedData
import love.yinlin.api.successData
import love.yinlin.currentTS
import love.yinlin.data.Data
import love.yinlin.extension.ByteArray
import java.io.File
import java.time.LocalDate
import java.time.temporal.ChronoUnit

fun Routing.profileAPI(implMap: ImplMap) {
	// ------  取用户信息  ------
	api(API.User.Profile.GetProfile) { token ->
		val uid = AN.throwExpireToken(token)
		val user = DB.throwQuerySQLSingle("""
            SELECT u1.uid, u1.name, u1.privilege, u1.signature, u1.label, u1.coin, u2.name AS inviterName
            FROM user AS u1
            LEFT JOIN user AS u2
            ON u1.inviter = u2.uid
            WHERE u1.uid = ?
        """, uid)
		// 更新最后上线时间
		DB.throwExecuteSQL("UPDATE user SET lastTime = ? WHERE uid = ?", currentTS, uid)
		Data.Success(user)
	}

	// ------  取用户公开信息  ------
	api(API.User.Profile.GetPublicProfile) { uid ->
		VN.throwId(uid)
		val user = DB.throwQuerySQLSingle("SELECT uid, name, signature, label, coin FROM user WHERE uid = ?", uid)
		Data.Success(user)
	}

	// ------  更新昵称  ------
	api(API.User.Profile.UpdateName) { (token, name) ->
		VN.throwName(name)
		val uid = AN.throwExpireToken(token)
		if (DB.querySQLSingle("SELECT 1 FROM user WHERE name = ?", name) != null)
			return@api "ID\"${name}\"已被注册".failedData
		if (DB.updateSQL("""
            UPDATE user SET name = ? , coin = coin - ? WHERE uid = ? AND coin >= ?
        """, name, VN.RENAME_COIN_COST, uid, VN.RENAME_COIN_COST)) "修改ID成功".successData
		else "你的银币不够哦".failedData
	}

	// ------  更新头像  ------
	api(API.User.Profile.UpdateAvatar) { (token, avatar) ->
		val uid = AN.throwExpireToken(token)
		// 保存头像
		File(avatar).copyTo(Resources.Public.Users.User(uid).avatar)
		"更新成功".successData
	}

	// ------  更新个性签名  ------
	api(API.User.Profile.UpdateSignature) { (token, signature) ->
		VN.throwEmpty(signature)
		val uid = AN.throwExpireToken(token)
		DB.throwExecuteSQL("UPDATE user SET signature = ? WHERE uid = ?", signature, uid)
		"更新成功".successData
	}

	// ------  更新背景墙  ------
	api(API.User.Profile.UpdateWall) { (token, wall) ->
		val uid = AN.throwExpireToken(token)
		// 保存背景墙
		File(wall).copyTo(Resources.Public.Users.User(uid).wall)
		"更新成功".successData
	}

	// ------  签到  ------
	api(API.User.Profile.Signin) { token ->
		val uid = AN.throwExpireToken(token)
		val user = DB.throwGetUser(uid, "signin")
		// 查询是否签到 ... 签到记录46字节(368位)
		val signin = user["signin"].ByteArray
		val currentDate = LocalDate.now()
		val firstDayOfYear = LocalDate.of(currentDate.year, 1, 1)
		val dayIndex = ChronoUnit.DAYS.between(firstDayOfYear, currentDate).toInt()
		val byteIndex = dayIndex / 8
		val bitIndex = dayIndex % 8
		val byteValue = signin[byteIndex].toInt()
		val bitValue = (byteValue shr bitIndex) and 1
		if (bitValue == 1) return@api "今天已经签到!".failedData
		signin[byteIndex] = (byteValue or (1 shl bitIndex)).toByte()
		// 更新签到值，银币增加
		DB.throwExecuteSQL("UPDATE user SET signin = ? , coin = coin + 1 WHERE uid = ?", signin, uid)
		"签到成功, 银币+1".successData
	}
}