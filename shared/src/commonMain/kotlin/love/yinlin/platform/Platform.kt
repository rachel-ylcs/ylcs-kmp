package love.yinlin.platform

import androidx.compose.runtime.Stable

@Stable
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

	@Stable
	val isPhone: Boolean get() = this == Android || this == IOS
	@Stable
	val isDesktop: Boolean get() = this == Windows || this == Linux || this == MacOS
	@Stable
	val isWeb: Boolean get() = this == WebWasm
}

