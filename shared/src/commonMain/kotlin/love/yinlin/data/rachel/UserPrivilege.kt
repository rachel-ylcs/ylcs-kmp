package love.yinlin.data.rachel

object UserPrivilege {
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