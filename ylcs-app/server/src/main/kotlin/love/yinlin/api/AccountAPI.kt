package love.yinlin.api

import love.yinlin.api.user.AN
import love.yinlin.api.user.Token
import love.yinlin.api.user.VN
import love.yinlin.api.user.throwGetUser
import love.yinlin.callMap
import love.yinlin.data.rachel.mail.Mail
import love.yinlin.data.rachel.profile.UserPrivilege
import love.yinlin.extension.Int
import love.yinlin.extension.IntNull
import love.yinlin.extension.Object
import love.yinlin.extension.String
import love.yinlin.server.DB
import love.yinlin.server.currentTS
import love.yinlin.server.md5
import love.yinlin.server.values

fun APIScope.accountAPI() {
    ApiAccountGetInviters.response {
        val inviters = DB.throwQuerySQL("SELECT name FROM user WHERE (privilege & ${UserPrivilege.VIP_ACCOUNT}) != 0")
        result(inviters.map { it.Object["name"].String })
    }

    ApiAccountLogin.response { name, pwd, platform ->
        VN.throwName(name)
        VN.throwPassword(pwd)
        val user = DB.querySQLSingle("SELECT uid, pwd FROM user WHERE name = ?", name) ?: failure("ID未注册")
        if (user["pwd"].String != pwd.md5) failure("密码错误")
        val uid = user["uid"].Int
        // 根据 uid, platform, timestamp 生成 token
        result(AN.throwGenerateToken(Token(uid, platform)))
    }

    ApiAccountLogOff.response { token ->
        AN.removeToken(token)
    }

    ApiAccountUpdateToken.response { token ->
        result(AN.throwReGenerateToken(token))
    }

    ApiAccountRegister.response { name, pwd, inviterName ->
        VN.throwName(name, inviterName)
        VN.throwPassword(pwd)

        // 查询ID是否注册过或是否正在审批
        if (DB.querySQLSingle("""
            SELECT name FROM user WHERE name = ?
            UNION
            SELECT param1 FROM mail WHERE param1 = ? AND filter = '${Mail.Filter.Register}'
        """, name, name) != null) failure("\"${name}\"已注册或正在注册审批中")

        // 检查邀请人是否有审核资格
        val inviter = DB.querySQLSingle("""
                SELECT uid, privilege FROM user WHERE (privilege & ${UserPrivilege.VIP_ACCOUNT}) != 0 AND name = ?
            """, inviterName)
        if (inviter == null) failure("邀请人\"${inviterName}\"不存在")
        if (!UserPrivilege.vipAccount(inviter["privilege"].Int)) failure("邀请人\"${inviterName}\"无审核资格")

        // 提交申请
        DB.throwExecuteSQL("""
            INSERT INTO mail(uid, type, title, content, filter, param1, param2) ${values(7)}
        """, inviter["uid"].Int, Mail.Type.DECISION, "用户注册审核",
            "小银子\"${name}\"填写你作为邀请人注册，是否同意？",
            Mail.Filter.Register.toString(), name, pwd.md5)
    }

    callMap[Mail.Filter.Register] = {
        val inviterID = it.uid
        val registerName = it.param1
        val registerPwd = it.param2
        val createTime = currentTS
        val registerID = DB.throwInsertSQLGeneratedKey("""
            INSERT INTO user(name, pwd, inviter, createTime, lastTime, playlist, signin) ${values(7)}
        """, registerName, registerPwd, inviterID, createTime, createTime, "{}", ByteArray(46)
        ).toInt()
        if (registerID == 0) failure("\"${registerName}\"已注册")
        // 处理用户目录
        val userPath = ServerRes.Users.User(registerID)
        // 如果用户目录存在则先删除
        userPath.delete()
        // 创建用户目录
        userPath.mkdir()
        // 创建用户图片目录
        userPath.Pics().mkdir()
        // 复制初始资源
        ServerRes.Assets.DefaultAvatar.copy(userPath.avatar)
        ServerRes.Assets.DefaultWall.copy(userPath.wall)
        "注册用户\"${registerName}\"成功"
    }

    ApiAccountForgotPassword.response { name, pwd ->
        VN.throwName(name)
        VN.throwPassword(pwd)

        val user = DB.querySQLSingle("SELECT uid, inviter FROM user WHERE name = ?", name) ?: failure("ID\"${name}\"未注册")
        val inviterUid = user["inviter"].IntNull ?: failure("原始ID请联系管理员修改密码")

        // 检查邀请人是否有审核资格
        val inviter = DB.querySQLSingle("SELECT name, privilege FROM user WHERE uid = ?", inviterUid) ?: failure("邀请人不存在")
        val inviterName = inviter["name"].String
        if (!UserPrivilege.vipAccount(inviter["privilege"].Int)) failure("邀请人\"${inviterName}\"无审核资格")

        // 查询ID是否注册过或是否正在审批
        if (DB.querySQLSingle("""
            SELECT uid FROM mail
            WHERE uid = ? AND filter = '${Mail.Filter.ForgotPassword}' AND param1 = ? AND processed = 0
        """, inviterUid, name) != null) failure("已提交过审批，请等待邀请人\"${inviterName}\"审核")

        // 提交申请
        DB.throwExecuteSQL("""
            INSERT INTO mail(uid, type, title, content, filter, param1, param2) ${values(7)}
        """, inviterUid, Mail.Type.DECISION, "用户修改密码审核",
            "\"${name}\"需要修改密码，是否同意？", Mail.Filter.ForgotPassword.toString(), name, pwd.md5)
    }

    callMap[Mail.Filter.ForgotPassword] = {
        val name = it.param1
        val pwd = it.param2
        val user = DB.querySQLSingle("SELECT uid FROM user WHERE name = ?", name) ?: failure("用户不存在")
        DB.throwExecuteSQL("UPDATE user SET pwd = ? WHERE name = ?", pwd, name)
        // 清除修改密码用户的 Token
        AN.removeAllTokens(user["uid"].Int)
        "\"${name}\"修改密码成功"
    }

    ApiAccountChangePassword.response { token, oldPwd, newPwd ->
        VN.throwPassword(oldPwd, newPwd)
        val uid = AN.throwExpireToken(token)
        val user = DB.throwGetUser(uid, "pwd")
        val oldPwdMd5 = oldPwd.md5
        val newPwdMd5 = newPwd.md5
        if (user["pwd"].String != oldPwdMd5) failure("原密码错误")
        else if (oldPwdMd5 == newPwdMd5) failure("原密码与新密码相同")
        else {
            DB.throwExecuteSQL("UPDATE user SET pwd = ? WHERE uid = ?", newPwdMd5, uid)
            AN.removeAllTokens(uid)
        }
    }
}