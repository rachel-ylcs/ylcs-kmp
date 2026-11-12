package love.yinlin.cs

import love.yinlin.data.rachel.profile.UserConstraint
import love.yinlin.data.rachel.topic.Comment

@Suppress("PropertyName")
class ServerChecker : APIChecker() {
    val ID by define<Number> { it.toLong() > 0L }
    val Username by define<String> { UserConstraint.checkName(it) }
    val Password by define<String> { UserConstraint.checkPassword(it) }

    val Section by define<Int> { it in Comment.Section.NOTIFICATION .. Comment.Section.DISCUSSION }

    inline operator fun invoke(block: ServerChecker.() -> Unit) = block()
}
