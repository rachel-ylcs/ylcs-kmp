package love.yinlin.api

import io.ktor.server.request.receive
import io.ktor.server.routing.Routing
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import love.yinlin.DB
import love.yinlin.Redis
import love.yinlin.Resources
import love.yinlin.currentTS
import love.yinlin.extension.byteArray
import love.yinlin.extension.int
import love.yinlin.extension.intOrNull
import love.yinlin.extension.makeObject
import love.yinlin.extension.string
import love.yinlin.extension.toJsonString
import love.yinlin.md5
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.nio.ByteBuffer
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

/* ------------------ 邮箱 --------------------- */

object Mail {
	const val TIP = 1 // 通知
	const val CONFIRM = 2 // 确认
	const val DECISION = 4 // 决定
	const val INPUT = 8 // 输入

	object Filter {
		const val REGISTER = "user#register"
		const val FORGOT_PASSWORD = "user#forgotPassword"
		const val OPEN_BETA_2024 = "user#2024OpenBeta"
		const val COIN_REWARD = "user#coinReward"
	}
}

/* ------------------ 权限 --------------------- */

object Privilege {
	const val BACKUP = 1 // 备份
	const val RES = 2 // 美图
	const val TOPIC = 4 // 主题
	const val UNUSED = 8 // 未定
	const val VIP_ACCOUNT = 16 // 账号管理
	const val VIP_TOPIC = 32 // 主题管理
	const val VIP_CALENDAR = 64 // 日历管理

	fun has(privilege: Int, mask: Int): Boolean = (privilege and mask) != 0
	fun backup(privilege: Int): Boolean = has(privilege, BACKUP)
	fun res(privilege: Int): Boolean = has(privilege, RES)
	fun topic(privilege: Int): Boolean = has(privilege, TOPIC)
	fun vipAccount(privilege: Int): Boolean = has(privilege, VIP_ACCOUNT)
	fun vipTopic(privilege: Int): Boolean = has(privilege, VIP_TOPIC)
	fun vipCalendar(privilege: Int): Boolean = has(privilege, VIP_CALENDAR)
}

/* ------------------ 验证 --------------------- */

object VN {
	class ValidationError(source: String, data: Any? = null) : Throwable() {
		override val message: String = "ValidationError $data"
	}

	private const val MIN_NAME_LENGTH = 2
	private const val MAX_NAME_LENGTH = 16
	private const val MIN_PWD_LENGTH = 6
	private const val MAX_PWD_LENGTH = 18

	const val RENAME_COIN_COST = 5

	fun throwIf(vararg args: Boolean) = if (args.any { it })
		throw ValidationError("If") else Unit
	fun throwName(vararg args: String) = if (args.any { it.length !in MIN_NAME_LENGTH .. MAX_NAME_LENGTH })
		throw ValidationError("Name", args) else Unit
	fun throwId(vararg args: Number) = if (args.any { it.toLong() <= 0L })
		throw ValidationError("Id", args) else Unit
	fun throwPassword(vararg args: String) = if (args.any { it.length !in MIN_PWD_LENGTH .. MAX_PWD_LENGTH })
		throw ValidationError("Password", args) else Unit
	fun throwEmpty(vararg args: String) = if (args.any { it.isEmpty() })
		throw ValidationError("Empty", args) else Unit
}

fun DB.throwGetUser(uid: Int, col: String = "uid") = DB.throwQuerySQLSingle("SELECT $col FROM user WHERE uid = ?", uid)

/* ------------------ 鉴权 --------------------- */

object AN {
	private const val TOKEN_SECRET_KEY = "token_secret_key"

	private val AES_KEY: SecretKey = Redis.use {
		val keyString = it.get(TOKEN_SECRET_KEY)
		if (keyString != null) {
			val bis = ByteArrayInputStream(Base64.getDecoder().decode(keyString))
			val ois = ObjectInputStream(bis)
			val secretKey = ois.readObject() as SecretKey
			ois.close()
			secretKey
		}
		else {
			val generator = KeyGenerator.getInstance("AES")
			generator.init(256)
			val secretKey = generator.generateKey()
			val bos = ByteArrayOutputStream()
			val oos = ObjectOutputStream(bos)
			oos.writeObject(secretKey)
			oos.flush()
			oos.close()
			it.set(TOKEN_SECRET_KEY, Base64.getEncoder().encodeToString(bos.toByteArray()))
			secretKey
		}
	}

	private fun tokenKey(uid: Int): String = "token/${uid}"

	private fun tokenParseUid(token: String): Int = try {
		val encryptedBytes = Base64.getDecoder().decode(token)
		val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
		cipher.init(Cipher.DECRYPT_MODE, AES_KEY)
		val bytes = cipher.doFinal(encryptedBytes)
		val uidBytes = bytes.copyOfRange(0, 4)
		val timestampBytes = bytes.copyOfRange(8, 16)
		val uid = ByteBuffer.wrap(uidBytes).int
		val timestamp = ByteBuffer.wrap(timestampBytes).long
		if (uid > 0 && timestamp in 1727755860000..1917058260000) uid
		else 0
	}
	catch (_: Throwable) { 0 }

	fun throwGenerateToken(uid: Int): String {
		val timestamp = System.currentTimeMillis()
		val buffer = ByteBuffer.allocate(Int.SIZE_BYTES + Int.SIZE_BYTES + Long.SIZE_BYTES)
		buffer.putInt(uid)
		buffer.putInt(19911211)
		buffer.putLong(timestamp)
		val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
		cipher.init(Cipher.ENCRYPT_MODE, AES_KEY)
		val encryptedBytes = cipher.doFinal(buffer.array())
		val token = Base64.getEncoder().encodeToString(encryptedBytes)
		Redis.use {
			it.setex(tokenKey(uid), 7 * 24 * 60 * 60, token)
		}
		return token
	}

	fun throwExpireToken(token: String): Int {
		val uid = tokenParseUid(token)
		if (uid > 0) {
			// keyToken 可能是 null 或 已经失效的 token
			val keyToken = Redis.use {
				it.get(tokenKey(uid))
			}
			if (keyToken == token) return uid
		}
		throw TokenExpireError(uid)
	}

	fun removeToken(uid: Int): Boolean {
		val key = tokenKey(uid)
		return Redis.use {
			if (it.exists(key)) {
				it.del(key)
				true
			}
			else false
		}
	}

	fun throwRemoveToken(token: String) {
		val uid = tokenParseUid(token)
		if (uid <= 0 || !removeToken(uid)) throw TokenExpireError(uid)
	}
}

/* ------------------ 接口 --------------------- */

fun Routing.userAPI(implMap: ImplMap) {
	// ------  登录  ------
	catchAsyncPost("/user/login") {
		@Serializable
		data class Body(val name: String, val pwd: String)

		val (name, pwd) = call.to<Body>()
		VN.throwName(name)
		VN.throwPassword(pwd)
		val user = DB.querySQLSingle("SELECT uid, pwd FROM user WHERE name = ?", name)
		if (user == null) return@catchAsyncPost call.failed("ID未注册")
		if (user["pwd"].string != pwd.md5) return@catchAsyncPost call.failed("密码错误")
		val uid = user["uid"].int
		// 根据 uid 和 timestamp 生成 token
		val token = AN.throwGenerateToken(uid)
		// 返回用户信息
		call.success(makeObject { "token" to token })
	}

	// ------  注销  ------
	catchAsyncPost("/user/logoff") {
		@Serializable
		data class Body(val token: String)

		val (token) = call.receive<Body>()
		AN.throwRemoveToken(token)
		call.success()
	}

	// ------  更新 Token  ------
	catchAsyncPost("/user/updateToken") {
		@Serializable
		data class Body(val token: String)

		val (token) = call.receive<Body>()
		val uid = AN.throwExpireToken(token)
		// 生成新的Token
		val newToken = AN.throwGenerateToken(uid)
		call.success(makeObject { "token" to newToken })
	}

	// ------  注册审批  ------
	catchAsyncPost("/user/register") {
		@Serializable
		data class Body(val name: String, val pwd: String, val inviterName: String)

		val (name, pwd, inviterName) = call.receive<Body>()
		VN.throwName(name, inviterName)
		VN.throwPassword(pwd)

		// 查询ID是否注册过或是否正在审批
		if (DB.querySQLSingle("""
            SELECT name FROM user WHERE name = ?
            UNION
            SELECT param1 FROM mail WHERE param1 = ? AND filter = '${Mail.Filter.REGISTER}'
        """, name, name) != null)
			return@catchAsyncPost call.failed("\"${name}\"已注册或正在注册审批中")

		// 检查邀请人是否有审核资格
		val inviter = DB.querySQLSingle("""
                SELECT uid, privilege FROM user WHERE (privilege & ${Privilege.VIP_ACCOUNT}) != 0 AND name = ?
            """, inviterName)
		if (inviter == null) return@catchAsyncPost call.failed("邀请人\"${inviterName}\"不存在")
		if (!Privilege.vipAccount(inviter["privilege"].int))
			return@catchAsyncPost call.failed("邀请人\"${inviterName}\"无审核资格")

		// 提交申请
		DB.throwExecuteSQL("""
            INSERT INTO mail(uid, type, title, content, filter, param1, param2)
            VALUES(?, ?, ?, ?, ?, ?, ?)
        """, inviter["uid"].int, Mail.DECISION, "用户注册审核",
			"小银子\"${name}\"填写你作为邀请人注册，是否同意？",
			Mail.Filter.REGISTER, name, pwd.md5)
		call.success("提交申请成功, 请等待管理员审核")
	}

	// ------  # 注册实现 #  ------
	implMap[Mail.Filter.REGISTER] = ImplContext@ {
		val inviterID = it["uid"].int
		val registerName = it["param1"].string
		val registerPwd = it["param2"].string
		val createTime = currentTS
		val registerID = DB.throwInsertSQLGeneratedKey("""
            INSERT INTO user(name, pwd, inviter, createTime, lastTime, playlist, signin)
            VALUES(?, ?, ?, ?, ?, ?, ?)
        """, registerName, registerPwd, inviterID, createTime, createTime, "{}", ByteArray(46)
		).toInt()
		if (registerID == 0) return@ImplContext "\"${registerName}\"已注册".failedObject
		// 处理用户目录
		val userPath = Resources.Public.Users.User(registerID)
		// 如果用户目录存在则先删除
		if (userPath.exists()) userPath.deleteRecursively()
		// 创建用户目录
		userPath.mkdir()
		// 创建用户图片目录
		userPath.Pics().mkdir()
		// 复制初始资源
		Resources.Public.Assets.DefaultAvatar.copyTo(userPath.avatar)
		Resources.Public.Assets.DefaultWall.copyTo(userPath.wall)
		"注册用户\"${registerName}\"成功".successObject
	}

	// ------  忘记密码审批  ------
	catchAsyncPost("/user/forgotPassword") {
		@Serializable
		data class Body(val name: String, val pwd: String)

		val (name, pwd) = call.receive<Body>()
		VN.throwName(name)
		VN.throwPassword(pwd)
		val user = DB.querySQLSingle("SELECT uid, inviter FROM user WHERE name = ?", name)
		if (user == null) return@catchAsyncPost call.failed("ID\"${name}\"未注册")
		val inviterUid = user["inviter"].intOrNull ?: return@catchAsyncPost call.failed("原始ID请联系管理员修改密码")

		// 检查邀请人是否有审核资格
		val inviter = DB.querySQLSingle("SELECT name, privilege FROM user WHERE uid = ?", inviterUid)
		if (inviter == null) return@catchAsyncPost call.failed("邀请人不存在")
		val inviterName = inviter["name"].string
		if (!Privilege.vipAccount(inviter["privilege"].int))
			return@catchAsyncPost call.failed("邀请人\"${inviterName}\"无审核资格")
		// 查询ID是否注册过或是否正在审批
		if (DB.querySQLSingle("""
            SELECT uid FROM mail
            WHERE uid = ? AND filter = '${Mail.Filter.FORGOT_PASSWORD}' AND param1 = ? AND processed = 0
        """, inviterUid, name) != null)
			return@catchAsyncPost call.failed("已提交过审批，请等待邀请人\"${inviterName}\"审核")
		// 提交申请
		DB.throwExecuteSQL("""
            INSERT INTO mail(uid, type, title, content, filter, param1, param2) VALUES(?, ?, ?, ?, ?, ?, ?)
        """, inviterUid, Mail.DECISION, "用户修改密码审核",
			"\"${name}\"需要修改密码，是否同意？",
			Mail.Filter.FORGOT_PASSWORD, name, pwd.md5)
		call.success("提交申请成功, 请等待管理员审核")
	}

	// ------  # 忘记密码实现 #  ------
	implMap[Mail.Filter.FORGOT_PASSWORD] = ImplContext@ {
		val name = it["param1"].string
		val pwd = it["param2"].string
		val user = DB.querySQLSingle("SELECT uid FROM user WHERE name = ?", name)
		if (user == null) return@ImplContext "用户不存在".failedObject
		DB.throwExecuteSQL("UPDATE user SET pwd = ? WHERE name = ?", pwd, name)
		// 清除修改密码用户的 Token
		AN.removeToken(user["uid"].int)
		"\"${name}\"修改密码成功".successObject
	}

	// ------  取用户信息  ------
	catchAsyncPost("/user/getInfo") {
		@Serializable
		data class Body(val token: String)

		val (token) = call.receive<Body>()
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
		call.success(user)
	}

	// ------  更新昵称  ------
	catchAsyncPost("/user/updateName") {
		@Serializable
		data class Body(val token: String, val name: String)

		val (token, name) = call.receive<Body>()
		VN.throwName(name)
		val uid = AN.throwExpireToken(token)
		if (DB.querySQLSingle("SELECT 1 FROM user WHERE name = ?", name) != null)
			return@catchAsyncPost call.failed("ID\"${name}\"已被注册")
		if (DB.updateSQL("""
            UPDATE user SET name = ? , coin = coin - ? WHERE uid = ? AND coin >= ?
        """, name, VN.RENAME_COIN_COST, uid, VN.RENAME_COIN_COST)) call.success("修改ID成功")
		else call.failed("你的银币不够哦")
	}

	// ------  更新头像  ------
	catchAsyncPost("/user/updateAvatar") {
		@Serializable
		data class Body(val token: String, val avatar: String)

		val (token, avatar) = call.toFormObject<Body>()
		val uid = AN.throwExpireToken(token)
		// 保存头像
		File(avatar).copyTo(Resources.Public.Users.User(uid).avatar)
		call.success("更新成功")
	}

	// ------  更新个性签名  ------
	catchAsyncPost("/user/updateSignature") {
		@Serializable
		data class Body(val token: String, val signature: String)

		val (token, signature) = call.receive<Body>()
		VN.throwEmpty(signature)
		val uid = AN.throwExpireToken(token)
		DB.throwExecuteSQL("UPDATE user SET signature = ? WHERE uid = ?", signature, uid)
		call.success("更新成功")
	}

	// ------  更新背景墙  ------
	catchAsyncPost("/user/updateWall") {
		@Serializable
		data class Body(val token: String, val wall: String)

		val (token, wall) = call.toFormObject<Body>()
		val uid = AN.throwExpireToken(token)
		// 保存背景墙
		File(wall).copyTo(Resources.Public.Users.User(uid).wall)
		call.success("更新成功")
	}

	// ------  歌单云备份  ------
	catchAsyncPost("/user/uploadPlaylist") {
		@Serializable
		data class Body(val token: String, val playlist: JsonObject)

		val (token, playlist) = call.receive<Body>()
		val uid = AN.throwExpireToken(token)
		if (DB.updateSQL("""
            UPDATE user SET playlist = ? WHERE uid = ? AND (privilege & ${Privilege.BACKUP}) != 0
        """, playlist.toJsonString(), uid)) call.success("上传成功")
		else call.failed("无权限")
	}

	// ------  歌单云还原  ------
	catchAsyncPost("/user/downloadPlaylist") {
		@Serializable
		data class Body(val token: String)

		val (token) = call.receive<Body>()
		val uid = AN.throwExpireToken(token)
		val user = DB.throwGetUser(uid, "privilege, playlist")
		if (Privilege.backup(user["privilege"].int)) call.success("下载成功", user["playlist"]!!)
		else call.failed("无权限")
	}

	// ------  签到  ------
	catchAsyncPost("/user/signin") {
		@Serializable
		data class Body(val token: String)

		val (token) = call.receive<Body>()
		val uid = AN.throwExpireToken(token)
		val user = DB.throwGetUser(uid, "signin")
		// 查询是否签到 ... 签到记录46字节(368位)
		val signin = user["signin"].byteArray
		val currentDate = LocalDate.now()
		val firstDayOfYear = LocalDate.of(currentDate.year, 1, 1)
		val dayIndex = ChronoUnit.DAYS.between(firstDayOfYear, currentDate).toInt()
		val byteIndex = dayIndex / 8
		val bitIndex = dayIndex % 8
		val byteValue = signin[byteIndex].toInt()
		val bitValue = (byteValue shr bitIndex) and 1
		if (bitValue == 1) return@catchAsyncPost call.failed("今天已经签到!")
		signin[byteIndex] = (byteValue or (1 shl bitIndex)).toByte()
		// 更新签到值，银币增加
		DB.throwExecuteSQL("UPDATE user SET signin = ? , coin = coin + 1 WHERE uid = ?", signin, uid)
		call.success("签到成功, 银币+1")
	}
}