package love.yinlin.data.rachel

object UserConstraint {
	const val RENAME_COIN_COST = 5
	const val MIN_COIN_REWARD = 3

	private const val MIN_NAME_LENGTH = 2
	private const val MAX_NAME_LENGTH = 16
	private const val MIN_PWD_LENGTH = 6
	private const val MAX_PWD_LENGTH = 18

	fun checkName(vararg args: String) = args.all { it.length in MIN_NAME_LENGTH .. MAX_NAME_LENGTH }
	fun checkPassword(vararg args: String) = args.all { it.length in MIN_PWD_LENGTH..MAX_PWD_LENGTH }
}