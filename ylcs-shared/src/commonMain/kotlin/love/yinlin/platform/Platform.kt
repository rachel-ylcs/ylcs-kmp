package love.yinlin.platform

enum class Platform {
	Android,
	IOS,
	Windows,
	Linux,
	MacOS,
	WebWasm;

	companion object {
		val Phone = arrayOf(Android, IOS)
		val Desktop = arrayOf(Windows, Linux, MacOS)

		fun fromInt(value: Int): Platform? = when (value) {
			Android.ordinal -> Android
			IOS.ordinal -> IOS
			Windows.ordinal -> Windows
			Linux.ordinal -> Linux
			MacOS.ordinal -> MacOS
			WebWasm.ordinal -> WebWasm
			else -> null
		}
	}
}