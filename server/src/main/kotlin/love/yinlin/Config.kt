package love.yinlin

import java.util.Properties

object Config {
	private val prop = Properties().apply {
		val classLoader = Config::class.java.classLoader
		val stream = classLoader.getResourceAsStream("config.properties")!!
		load(stream)
		stream.close()
	}

	object Mysql {
		val PORT: Int = (prop["mysql.port"] as String).toInt()
		val NAME: String = prop["mysql.name"] as String
		val USERNAME: String = prop["mysql.username"] as String
		val PASSWORD: String = prop["mysql.password"] as String
	}

	object Redis {
		val PORT: Int = (prop["redis.port"] as String).toInt()
		val PASSWORD: String = prop["redis.password"] as String
	}
}