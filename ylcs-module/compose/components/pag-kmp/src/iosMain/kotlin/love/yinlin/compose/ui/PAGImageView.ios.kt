@file:OptIn(ExperimentalForeignApi::class)
package love.yinlin.compose.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import kotlinx.cinterop.ExperimentalForeignApi

private class PAGImageViewWrapper : PlatformView<PlatformPAGImageView>() {
    override fun build(): PlatformPAGImageView = PlatformPAGImageView()
}

@Composable
actual fun PAGImageView(
    composition: PAGComposition?,
    modifier: Modifier,
    isPlaying: Boolean,
    config: PAGConfig
) {
    val wrapper: PAGImageViewWrapper = rememberPlatformView { PAGImageViewWrapper() }

    wrapper.HostView(modifier = modifier)

    wrapper.Monitor(config) { view ->
        config.repeatCount.let { if (view.repeatCount() != it) view.setRepeatCount(it) }
        config.renderScale.let { if (view.renderScale() != it) view.setRenderScale(it) }
    }

    wrapper.Monitor(composition) { view ->
        view.setComposition(composition?.delegate)
    }

    wrapper.Monitor(isPlaying) { view ->
        if (isPlaying) {
            if (!view.isPlaying()) view.play()
        }
        else {
            if (view.isPlaying()) view.pause()
        }
    }
}