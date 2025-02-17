package love.yinlin

object Config {
	const val DEBUG = true
	val HOST: String = if (DEBUG) "49.235.151.78" else "localhost"

	object Redis {
		const val PORT: Int = 11050
		const val PASSWORD: String = "ylcs_rachel1211"
	}

	object Mysql {
		const val PORT: Int = 20240
		const val NAME: String = "rachel"
		const val USERNAME: String = "rachel"
		const val PASSWORD: String = "ylcs_rachel1211"
	}
}