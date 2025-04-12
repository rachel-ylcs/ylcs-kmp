package love.yinlin.platform

enum class Platform {
	Android,
	IOS,
	Windows,
	Linux,
	MacOS,
	WebWasm;

	companion object {
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

	val isPhone: Boolean get() = this == Android || this == IOS
	val isDesktop: Boolean get() = this == Windows || this == Linux || this == MacOS
	val isWeb: Boolean get() = this == WebWasm
}