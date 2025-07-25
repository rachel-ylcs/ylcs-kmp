package love.yinlin.api.user

import io.ktor.server.routing.Routing
import love.yinlin.DB
import love.yinlin.api.API
import love.yinlin.api.ImplMap
import love.yinlin.api.ServerRes
import love.yinlin.api.api
import love.yinlin.api.failureData
import love.yinlin.api.successData
import love.yinlin.copy
import love.yinlin.currentTS
import love.yinlin.data.Data
import love.yinlin.data.rachel.follows.FollowStatus
import love.yinlin.data.rachel.profile.UserConstraint
import love.yinlin.data.rachel.profile.UserProfile
import love.yinlin.extension.JsonConverter
import love.yinlin.extension.makeObject
import love.yinlin.extension.to
import love.yinlin.extension.toJson
import java.time.LocalDate
import java.time.temporal.ChronoUnit

private inline fun <R> ByteArray.checkSignin(block: (Boolean, Int, Int, Int) -> R): R {
	val currentDate = LocalDate.now()
	val firstDayOfYear = LocalDate.of(currentDate.year, 1, 1)
	val dayIndex = ChronoUnit.DAYS.between(firstDayOfYear, currentDate).toInt()
	val byteIndex = dayIndex / 8
	val bitIndex = dayIndex % 8
	val byteValue = this[byteIndex].toInt()
	val isSignin = ((byteValue shr bitIndex) and 1) == 1
	return block(isSignin, byteValue, byteIndex, bitIndex)
}

fun Routing.profileAPI(implMap: ImplMap) {
	api(API.User.Profile.GetProfile) { token ->
		val uid = AN.throwExpireToken(token)
		val user = DB.throwQuerySQLSingle("""
            SELECT
				u1.uid, u1.name, u1.privilege, u1.signature, u1.label, u1.exp, u1.coin, u1.signin, u1.follows, u1.followers, u2.name AS inviterName,
				JSON_OBJECT(
					'mailCount', (SELECT COUNT(*) FROM mail WHERE processed = 0 AND uid = ?)
				) AS notification
            FROM user AS u1
            LEFT JOIN user AS u2
            ON u1.inviter = u2.uid
            WHERE u1.uid = ?
        """, uid, uid)
		// 处理提醒
		val signin = user["signin"]!!.to(JsonConverter.ByteArray)
		var profile = user.to<UserProfile>()
		signin.checkSignin { isSignin, _, _, _ ->
			profile = profile.copy(
				notification = profile.notification.copy(
					isSignin = isSignin
				)
			)
		}
		// 更新最后上线时间
		DB.throwExecuteSQL("UPDATE user SET lastTime = ? WHERE uid = ?", currentTS, uid)
		Data.Success(profile)
	}

	api(API.User.Profile.GetPublicProfile) { (token, uid2) ->
		VN.throwId(uid2)
		val uid1 = token?.let { AN.throwExpireToken(it) }
		val user = DB.throwQuerySQLSingle("""
			SELECT uid, name, signature, label, exp, follows, followers FROM user WHERE uid = ?
		""", uid2)
		val status: FollowStatus = if (uid1 == null) FollowStatus.UNAUTHORIZE
			else if (uid1 == uid2) FollowStatus.SELF
			else {
				val (relationship1, relationship2) = DB.queryRelationship(uid1, uid2)
				if (relationship2 != true) when (relationship1) {
					null -> FollowStatus.UNFOLLOW
					true -> FollowStatus.BLOCKED
					false -> FollowStatus.FOLLOW
				}
				else if (relationship1 == true) FollowStatus.BIDIRECTIONAL_BLOCK
				else FollowStatus.BLOCK
			}
		Data.Success(makeObject {
			merge(user)
			"status" with status.toJson()
		}.to())
	}

	api(API.User.Profile.UpdateName) { (token, name) ->
		VN.throwName(name)
		val uid = AN.throwExpireToken(token)
		if (DB.querySQLSingle("SELECT 1 FROM user WHERE name = ?", name) != null)
			return@api "ID\"${name}\"已被注册".failureData
		if (DB.updateSQL("""
            UPDATE user SET name = ? , coin = coin - ? WHERE uid = ? AND coin >= ?
        """, name, UserConstraint.RENAME_COIN_COST, uid, UserConstraint.RENAME_COIN_COST)) "修改ID成功".successData
		else "你的银币不够哦".failureData
	}

	api(API.User.Profile.UpdateAvatar) { token, (avatar) ->
		val uid = AN.throwExpireToken(token)
		// 保存头像
		avatar.copy(ServerRes.Users.User(uid).avatar)
		"更新成功".successData
	}

	api(API.User.Profile.UpdateSignature) { (token, signature) ->
		VN.throwEmpty(signature)
		val uid = AN.throwExpireToken(token)
		DB.throwExecuteSQL("UPDATE user SET signature = ? WHERE uid = ?", signature, uid)
		"更新成功".successData
	}

	api(API.User.Profile.UpdateWall) { token, (wall) ->
		val uid = AN.throwExpireToken(token)
		// 保存背景墙
		wall.copy(ServerRes.Users.User(uid).wall)
		"更新成功".successData
	}

	api(API.User.Profile.ResetPicture) { token ->
		val uid = AN.throwExpireToken(token)
		val userPath = ServerRes.Users.User(uid)
		ServerRes.Assets.DefaultAvatar.copy(userPath.avatar)
		ServerRes.Assets.DefaultWall.copy(userPath.wall)
		"重置成功".successData
	}

	api(API.User.Profile.Signin) { token ->
		val uid = AN.throwExpireToken(token)
		val user = DB.throwGetUser(uid, "signin")
		// 查询是否签到 ... 签到记录46字节(368位)
		val signin = user["signin"]!!.to(JsonConverter.ByteArray)
		signin.checkSignin { isSignin, byteValue, byteIndex, bitIndex ->
			if (!isSignin) {
				// 更新签到值，银币增加
				signin[byteIndex] = (byteValue or (1 shl bitIndex)).toByte()
				DB.throwExecuteSQL("UPDATE user SET signin = ? , exp = exp + 1, coin = coin + 1 WHERE uid = ?", signin, uid)
			}
			Data.Success(API.User.Profile.Signin.Response(isSignin, byteValue, bitIndex))
		}
	}
}