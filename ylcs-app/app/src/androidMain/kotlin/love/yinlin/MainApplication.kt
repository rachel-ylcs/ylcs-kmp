package love.yinlin

import android.content.Intent
import android.os.Build
import androidx.compose.runtime.Composable
import love.yinlin.compose.LocalDarkMode
import love.yinlin.compose.screen.DeepLink
import love.yinlin.fixup.FixupAndroidStatusBarColor
import love.yinlin.uri.Scheme
import love.yinlin.uri.Uri
import love.yinlin.uri.toUri

class MainApplication : ComposeApplication() {
	override val instance = object : RachelApplication(this) {
		@Composable
		override fun BeginContent(activity: ComposeActivity) {
			FixupAndroidStatusBarColor.AutoTheme(activity.window, LocalDarkMode.current)
		}

		override fun onActivityCreate(activity: ComposeActivity) {
			picker.bindActivity(activity)

			// TODO: 浮动歌词
//			ActualFloatingLyrics(activity).also {
//				musicFactory.instance.floatingLyrics = it
//				if (config.enabledFloatingLyrics) it.attach()
//			}
		}

		override fun onActivityDestroy(activity: ComposeActivity) {
//			(musicFactory.instance.floatingLyrics as? ActualFloatingLyrics)?.detach()
//			musicFactory.instance.floatingLyrics = null
		}

		override fun onIntent(intent: Intent) {
			when (intent.action) {
				Intent.ACTION_VIEW -> intent.data?.let { data ->
					val uri = data.toUri()
					when (uri.scheme) {
						Scheme.Content -> DeepLink.openUri(uri)
					}
				}
				Intent.ACTION_SEND -> {
					@Suppress("DEPRECATION")
					val data = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
						intent.getParcelableExtra(Intent.EXTRA_STREAM, android.net.Uri::class.java)
					} else intent.getParcelableExtra(Intent.EXTRA_STREAM)
					if (data != null) {
						val uri = data.toUri()
						if (uri.scheme == Scheme.Content) {
							DeepLink.openUri(uri)
						}
					}
					else {
						val text = intent.getStringExtra(Intent.EXTRA_TEXT) ?: ""
						when {
							text.contains("QQ音乐") -> {
								val result = Uri.parse("https?://\\S+".toRegex().find(text)?.value!!)!!
								DeepLink.openUri(result.copy(scheme = Scheme.QQMusic))
							}
							text.contains("网易云音乐") -> {
								val result = Uri.parse("https?://\\S+".toRegex().find(text)?.value!!)!!
								DeepLink.openUri(result.copy(scheme = Scheme.NetEaseCloud))
							}
						}
					}
				}
			}
		}
	}
}