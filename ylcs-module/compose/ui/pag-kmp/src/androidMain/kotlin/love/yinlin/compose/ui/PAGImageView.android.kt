package love.yinlin.compose.ui

import android.content.Context
import androidx.activity.result.ActivityResultRegistry
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.LifecycleOwner

private class PAGImageViewWrapper : PlatformView<PlatformPAGImageView>() {
    override fun build(context: Context, lifecycleOwner: LifecycleOwner, activityResultRegistry: ActivityResultRegistry?): PlatformPAGImageView = PlatformPAGImageView(context)
}

@Composable
actual fun PAGImageView(
    composition: PAGComposition?,
    modifier: Modifier,
    isPlaying: Boolean,
    config: PAGConfig
) {
    val wrapper = rememberPlatformView { PAGImageViewWrapper() }

    wrapper.HostView(modifier = modifier)

    wrapper.Monitor(config) { view ->
        config.repeatCount.let { if (view.repeatCount() != it) view.setRepeatCount(it) }
        config.scaleMode.ordinal.let { if (view.scaleMode() != it) view.setScaleMode(it) }
    }

    wrapper.Monitor(composition) { view ->
        view.composition = composition?.delegate
    }

    wrapper.Monitor(isPlaying) { view ->
        if (isPlaying) {
            if (!view.isPlaying) view.play()
        }
        else {
            if (view.isPlaying) view.pause()
        }
    }
}