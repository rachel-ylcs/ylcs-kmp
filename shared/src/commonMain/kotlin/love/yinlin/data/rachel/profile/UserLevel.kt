package love.yinlin.data.rachel.profile

object UserLevel {
	// 等级对照表
	private val Default = intArrayOf(
		0, 5, 10, 20, 30,
		50, 75, 100, 125, 150,
		200, 250, 300, 350, 400,
		500, 600, 700, 800, 900,
	)

	fun level(num: Int): Int {
		if (num > 0) {
			for (i in Default.indices) {
				if (num >= Default[i] && (i == Default.size - 1 || num < Default[i + 1])) return i + 1
			}
		}
		return 1
	}
}