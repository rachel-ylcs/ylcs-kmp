package love.yinlin.api.user

import io.ktor.server.routing.Routing
import love.yinlin.DB
import love.yinlin.api.API
import love.yinlin.api.EmptySuccessData
import love.yinlin.api.ImplMap
import love.yinlin.api.ServerRes
import love.yinlin.api.api
import love.yinlin.api.failureData
import love.yinlin.api.failedObject
import love.yinlin.api.successData
import love.yinlin.api.successObject
import love.yinlin.copy
import love.yinlin.currentTS
import love.yinlin.data.Data
import love.yinlin.data.rachel.mail.Mail
import love.yinlin.data.rachel.profile.UserPrivilege
import love.yinlin.deleteRecursively
import love.yinlin.extension.Int
import love.yinlin.extension.IntNull
import love.yinlin.extension.Object
import love.yinlin.extension.String
import love.yinlin.md5
import love.yinlin.mkdir
import love.yinlin.values

fun Routing.accountAPI(implMap: ImplMap) {
	api(API.User.Account.GetInviters) { ->
		val inviters = DB.throwQuerySQL("SELECT name FROM user WHERE (privilege & ${UserPrivilege.VIP_ACCOUNT}) != 0")
		Data.Success(inviters.map { it.Object["name"].String })
	}

	api(API.User.Account.Login) { (name, pwd, platform) ->
		VN.throwName(name)
		VN.throwPassword(pwd)
		val user = DB.querySQLSingle("SELECT uid, pwd FROM user WHERE name = ?", name) ?: return@api "ID未注册".failureData
		if (user["pwd"].String != pwd.md5) return@api "密码错误".failureData
		val uid = user["uid"].Int
		// 根据 uid, platform, timestamp 生成 token
		val token = Token(uid, platform)
		Data.Success(AN.throwGenerateToken(token))
	}

	api(API.User.Account.Logoff) { token ->
		AN.removeToken(token)
		EmptySuccessData
	}

	api(API.User.Account.UpdateToken) { token ->
		Data.Success(AN.throwReGenerateToken(token))
	}

	api(API.User.Account.QueryTokenMutexMap) { ->
		Data.Success(AN.queryTokenMutexMap)
	}

	api(API.User.Account.Register) { (name, pwd, inviterName) ->
		VN.throwName(name, inviterName)
		VN.throwPassword(pwd)

		// 查询ID是否注册过或是否正在审批
		if (DB.querySQLSingle("""
            SELECT name FROM user WHERE name = ?
            UNION
            SELECT param1 FROM mail WHERE param1 = ? AND filter = '${Mail.Filter.REGISTER}'
        """, name, name) != null)
			return@api "\"${name}\"已注册或正在注册审批中".failureData

		// 检查邀请人是否有审核资格
		val inviter = DB.querySQLSingle("""
                SELECT uid, privilege FROM user WHERE (privilege & ${UserPrivilege.VIP_ACCOUNT}) != 0 AND name = ?
            """, inviterName)
		if (inviter == null) return@api "邀请人\"${inviterName}\"不存在".failureData
		if (!UserPrivilege.vipAccount(inviter["privilege"].Int))
			return@api "邀请人\"${inviterName}\"无审核资格".failureData

		// 提交申请
		DB.throwExecuteSQL("""
            INSERT INTO mail(uid, type, title, content, filter, param1, param2) ${values(7)}
        """, inviter["uid"].Int, Mail.Type.DECISION, "用户注册审核",
			"小银子\"${name}\"填写你作为邀请人注册，是否同意？",
			Mail.Filter.REGISTER, name, pwd.md5)
		"提交申请成功, 请等待管理员审核".successData
	}

	implMap[Mail.Filter.REGISTER] = ImplContext@ {
		val inviterID = it.uid
		val registerName = it.param1
		val registerPwd = it.param2
		val createTime = currentTS
		val registerID = DB.throwInsertSQLGeneratedKey("""
            INSERT INTO user(name, pwd, inviter, createTime, lastTime, playlist, signin) ${values(7)}
        """, registerName, registerPwd, inviterID, createTime, createTime, "{}", ByteArray(46)
		).toInt()
		if (registerID == 0) return@ImplContext "\"${registerName}\"已注册".failedObject
		// 处理用户目录
		val userPath = ServerRes.Users.User(registerID)
		// 如果用户目录存在则先删除
		userPath.deleteRecursively()
		// 创建用户目录
		userPath.mkdir()
		// 创建用户图片目录
		userPath.Pics().mkdir()
		// 复制初始资源
		ServerRes.Assets.DefaultAvatar.copy(userPath.avatar)
		ServerRes.Assets.DefaultWall.copy(userPath.wall)
		"注册用户\"${registerName}\"成功".successObject
	}

	api(API.User.Account.ForgotPassword) { (name, pwd) ->
		VN.throwName(name)
		VN.throwPassword(pwd)

		val user = DB.querySQLSingle("SELECT uid, inviter FROM user WHERE name = ?", name) ?: return@api "ID\"${name}\"未注册".failureData
		val inviterUid = user["inviter"].IntNull ?: return@api "原始ID请联系管理员修改密码".failureData

		// 检查邀请人是否有审核资格
		val inviter = DB.querySQLSingle("SELECT name, privilege FROM user WHERE uid = ?", inviterUid) ?: return@api "邀请人不存在".failureData
		val inviterName = inviter["name"].String
		if (!UserPrivilege.vipAccount(inviter["privilege"].Int))
			return@api "邀请人\"${inviterName}\"无审核资格".failureData
		// 查询ID是否注册过或是否正在审批
		if (DB.querySQLSingle("""
            SELECT uid FROM mail
            WHERE uid = ? AND filter = '${Mail.Filter.FORGOT_PASSWORD}' AND param1 = ? AND processed = 0
        """, inviterUid, name) != null)
			return@api "已提交过审批，请等待邀请人\"${inviterName}\"审核".failureData
		// 提交申请
		DB.throwExecuteSQL("""
            INSERT INTO mail(uid, type, title, content, filter, param1, param2) ${values(7)}
        """, inviterUid, Mail.Type.DECISION, "用户修改密码审核",
			"\"${name}\"需要修改密码，是否同意？",
			Mail.Filter.FORGOT_PASSWORD, name, pwd.md5)
		"提交申请成功, 请等待管理员审核".successData
	}

	implMap[Mail.Filter.FORGOT_PASSWORD] = ImplContext@ {
		val name = it.param1
		val pwd = it.param2
		val user = DB.querySQLSingle("SELECT uid FROM user WHERE name = ?", name) ?: return@ImplContext "用户不存在".failedObject
		DB.throwExecuteSQL("UPDATE user SET pwd = ? WHERE name = ?", pwd, name)
		// 清除修改密码用户的 Token
		AN.removeAllTokens(user["uid"].Int)
		"\"${name}\"修改密码成功".successObject
	}

	api(API.User.Account.ChangePassword) { (token, oldPwd, newPwd) ->
		VN.throwPassword(oldPwd, newPwd)
		val uid = AN.throwExpireToken(token)
		val user = DB.throwGetUser(uid, "pwd")
		val oldPwdMd5 = oldPwd.md5
		val newPwdMd5 = newPwd.md5
		if (user["pwd"].String != oldPwdMd5) "原密码错误".failureData
		else if (oldPwdMd5 == newPwdMd5) "原密码与新密码相同".failureData
		else {
			DB.throwExecuteSQL("UPDATE user SET pwd = ? WHERE uid = ?", newPwdMd5, uid)
			AN.removeAllTokens(uid)
			"修改密码成功".successData
		}
	}
}