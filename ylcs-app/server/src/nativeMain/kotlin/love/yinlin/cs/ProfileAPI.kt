package love.yinlin.cs

import io.github.smyrgeorge.sqlx4k.SQLError
import love.yinlin.cs.service.throwExecuteSQL
import love.yinlin.cs.service.throwQuerySQL
import love.yinlin.cs.service.throwQuerySQLSingle
import love.yinlin.data.rachel.follows.FollowStatus
import love.yinlin.data.rachel.profile.UserConstraint
import love.yinlin.data.rachel.profile.UserProfile
import love.yinlin.extension.*

private inline fun <R> ByteArray.checkSignin(block: (Boolean, Int, Int, Int) -> R): R {
    val currentDate = DateEx.Current.date
    val dayIndex = currentDate.dayOfYear - 1
    val byteIndex = dayIndex / 8
    val bitIndex = dayIndex % 8
    val byteValue = this[byteIndex].toInt()
    val isSignin = (byteValue shr bitIndex) and 1 == 1
    return block(isSignin, byteValue, byteIndex, bitIndex)
}

fun ServerScope.profileAPI() {
    ApiProfileGetProfile.response { token ->
        val uid = AN.throwExpireToken(token)
        val profile = mysql.throwTransaction { transaction ->
            val user = transaction.throwQuerySQLSingle("""
                SELECT
                    u1.uid, u1.name, u1.privilege, u1.signature, u1.label, u1.exp, u1.coin, u1.follows, u1.followers,
                    u2.name AS inviterName, d.signin,
                    JSON_OBJECT(
                        'mailCount', (SELECT COUNT(*) FROM mail AS m WHERE m.processed = 0 AND m.uid = u1.uid)
                    ) AS notification
                FROM user AS u1
                JOIN user_data AS d ON d.uid = u1.uid
                LEFT JOIN user AS u2 ON u2.uid = u1.inviter
                WHERE u1.uid = ?
            """, uid)
            val signin = user["signin"]!!.to(ByteArraySerializer)
            val profile = user.to<UserProfile>()
            val updatedProfile = signin.checkSignin { isSignin, _, _, _ ->
                profile.copy(notification = profile.notification.copy(isSignin = isSignin))
            }
            transaction.throwExecuteSQL("UPDATE user SET lastTime = ? WHERE uid = ?", DateEx.CurrentString, uid)
            updatedProfile
        }
        result(profile)
    }

    ApiProfileGetPublicProfile.response { token, uid2 ->
        checker {
            ID check uid2
        }

        val uid1 = token?.let { AN.throwExpireToken(it) }
        val user = mysql.throwQuerySQLSingle("""
            SELECT u.uid, u.name, u.signature, u.label, u.exp, u.follows, u.followers,
                CAST(f1.isBlocked AS SIGNED) AS relationship1, CAST(f2.isBlocked AS SIGNED) AS relationship2
            FROM user AS u
            LEFT JOIN follows AS f1 ON f1.uid1 = ? AND f1.uid2 = u.uid
            LEFT JOIN follows AS f2 ON f2.uid1 = u.uid AND f2.uid2 = ?
            WHERE u.uid = ?
        """, uid1, uid1, uid2)
        val status: FollowStatus = if (uid1 == null) FollowStatus.UNAUTHORIZE
        else if (uid1 == uid2) FollowStatus.SELF
        else {
            val relationship1 = user["relationship1"].IntNull?.let { it != 0 }
            val relationship2 = user["relationship2"].IntNull?.let { it != 0 }
            if (relationship2 != true) when (relationship1) {
                null -> FollowStatus.UNFOLLOW
                true -> FollowStatus.BLOCKED
                false -> FollowStatus.FOLLOW
            }
            else if (relationship1 == true) FollowStatus.BIDIRECTIONAL_BLOCK
            else FollowStatus.BLOCK
        }
        result(makeObject {
            merge(user)
            "status" with status.toJson()
        }.to())
    }

    ApiProfileUpdateName.response { token, name ->
        checker {
            Username check name
        }

        val uid = AN.throwExpireToken(token)
        val cost = UserConstraint.RENAME_COIN_COST
        val nameTaken = "ID\"${name}\"已被注册"
        mysql.throwTransaction { transaction ->
            val user = transaction.throwQuerySQL("SELECT coin FROM user WHERE uid = ? FOR UPDATE", uid).firstOrNull()?.Object
            if (transaction.throwQuerySQL("SELECT 1 FROM user WHERE name = ?", name).isNotEmpty()) failure(nameTaken)
            if (user == null || user["coin"].Int < cost) failure("你的银币不够哦")
            catchingError {
                transaction.throwExecuteSQL("""
                    UPDATE user SET name = ?, coin = coin - ? WHERE uid = ? AND coin >= ?
                """, name, cost, uid, cost)
            }?.then { error ->
                if (error is SQLError && error.nativeCode == 1062) failure(nameTaken)
                throw error
            }
        }
    }

    ApiProfileUpdateSignature.response { token, signature ->
        checker {
            NotEmpty check signature
        }

        val uid = AN.throwExpireToken(token)
        mysql.throwExecuteSQL("UPDATE user SET signature = ? WHERE uid = ?", signature, uid)
    }

    ApiProfileUpdateAvatar.response { token, avatar ->
        val uid = AN.throwExpireToken(token)
        avatar.copy(ServerRes.Users.User(uid).avatar)
    }

    ApiProfileUpdateWall.response { token, wall ->
        val uid = AN.throwExpireToken(token)
        wall.copy(ServerRes.Users.User(uid).wall)
    }

    ApiProfileResetPicture.response { token ->
        val uid = AN.throwExpireToken(token)
        val userPath = ServerRes.Users.User(uid)
        ServerRes.Assets.DefaultAvatar.copy(userPath.avatar)
        ServerRes.Assets.DefaultWall.copy(userPath.wall)
    }

    ApiProfileSignin.response { token ->
        val uid = AN.throwExpireToken(token)
        mysql.throwTransaction { transaction ->
            transaction.throwQuerySQLSingle("SELECT uid FROM user WHERE uid = ? FOR UPDATE", uid)
            val userData = transaction.throwQuerySQLSingle("SELECT signin FROM user_data WHERE uid = ? FOR UPDATE", uid)
            // 查询是否签到 ... 签到记录46字节(368位)
            val signin = userData["signin"]!!.to(ByteArraySerializer)
            signin.checkSignin { isSignin, byteValue, byteIndex, bitIndex ->
                if (!isSignin) {
                    // 更新签到值，银币增加
                    signin[byteIndex] = (byteValue or (1 shl bitIndex)).toByte()
                    transaction.throwExecuteSQL("UPDATE user SET exp = exp + 1, coin = coin + 1 WHERE uid = ?", uid)
                    transaction.throwExecuteSQL("UPDATE user_data SET signin = ? WHERE uid = ?", signin, uid)
                }
                result(isSignin, byteValue, bitIndex)
            }
        }
    }
}
