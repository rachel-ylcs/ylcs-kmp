package love.yinlin.data.rachel.profile

object UserLevel {
	// 等级对照表
	private val Default = intArrayOf(
		0, 5, 10, 20,
		30, 50, 75, 100,
		135, 170, 210, 250,
		300, 350, 420, 500,
		600, 720, 850, 1000,
		1200, 1500, 2000, 3000,
		Int.MAX_VALUE
	)

	fun level(num: Int): Int {
		if (num > 0) {
			for (i in Default.indices) {
				if (num >= Default[i] && (i == Default.size - 1 || num < Default[i + 1])) return i + 1
			}
		}
		return 1
	}

	val levelTable: List<Pair<Int, Int>> = List(Default.size - 1) { Default[it] to Default[it + 1] }
}