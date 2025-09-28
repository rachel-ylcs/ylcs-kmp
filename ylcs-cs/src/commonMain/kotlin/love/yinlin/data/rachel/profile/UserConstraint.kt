package love.yinlin.data.rachel.profile

object UserConstraint {
	const val RENAME_COIN_COST = 5
	const val MIN_COIN_REWARD = 3

	const val MIN_NAME_LENGTH = 2
	const val MAX_NAME_LENGTH = 16
	const val MIN_PWD_LENGTH = 6
	const val MAX_PWD_LENGTH = 18

	const val MAX_SIGNATURE_LENGTH = 64

	fun checkName(vararg args: String) = args.all { it.length in MIN_NAME_LENGTH .. MAX_NAME_LENGTH }
	fun checkPassword(vararg args: String) = args.all { it.length in MIN_PWD_LENGTH..MAX_PWD_LENGTH }
}