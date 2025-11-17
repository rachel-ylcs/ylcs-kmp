package love.yinlin.api.user

import love.yinlin.data.rachel.profile.UserConstraint
import love.yinlin.data.rachel.topic.Comment
import love.yinlin.server.Database

object VN {
    class ValidationError(source: String, data: Any? = null) : Throwable() {
        override val message: String = "ValidationError $source $data"
    }

    fun throwIf(vararg args: Boolean) = if (args.any { it })
        throw ValidationError("If", args.joinToString(",")) else Unit
    fun throwName(vararg args: String) = if (!UserConstraint.checkName(*args))
        throw ValidationError("Name", args.joinToString(",")) else Unit
    fun throwId(vararg args: Number) = if (args.any { it.toLong() <= 0L })
        throw ValidationError("Id", args.joinToString(",")) else Unit
    fun throwPassword(vararg args: String) = if (!UserConstraint.checkPassword(*args))
        throw ValidationError("Password", args.joinToString(",")) else Unit
    fun throwEmpty(vararg args: String) = if (args.any { it.isEmpty() })
        throw ValidationError("Empty", args.joinToString(",")) else Unit
    fun throwSection(section: Int) = if (section !in Comment.Section.NOTIFICATION .. Comment.Section.DISCUSSION)
        throw ValidationError("Section", section) else Comment.Section.commentTable(section)
}

fun Database.throwGetUser(uid: Int, col: String = "uid") = this.throwQuerySQLSingle("SELECT $col FROM user WHERE uid = ?", uid)