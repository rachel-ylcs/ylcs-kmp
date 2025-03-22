package love.yinlin.ui.component.image

import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import io.moyuru.cropify.Cropify
import io.moyuru.cropify.CropifyOption
import io.moyuru.cropify.CropifySize
import io.moyuru.cropify.CropifyState
import love.yinlin.ui.component.input.RachelButton

class DialogCropState {
	var uri: Uri? by mutableStateOf(null)
	val isOpen: Boolean by derivedStateOf { uri != null }
	val cropState = CropifyState()
}

@Composable
fun DialogCrop(
	state: DialogCropState,
	aspectRatio: Float? = null,
	onCropped: (ImageBitmap) -> Unit
) {
	val frameSize = remember(aspectRatio) {
		if (aspectRatio == null || aspectRatio == 0f) null
		else CropifySize.FixedAspectRatio(1 / aspectRatio)
	}
	Dialog(
		onDismissRequest = { state.uri = null },
		properties = DialogProperties(
			dismissOnBackPress = true,
			dismissOnClickOutside = false,
			usePlatformDefaultWidth = true
		)
	) {
		state.uri?.let { uri ->
			Box(
				modifier = Modifier.fillMaxWidth().fillMaxHeight(fraction = 0.7f),
				contentAlignment = Alignment.Center
			) {
				Cropify(
					uri = uri,
					state = state.cropState,
					modifier = Modifier.fillMaxSize().zIndex(1f),
					option = CropifyOption(frameSize = frameSize),
					onImageCropped = {
						state.uri = null
						onCropped(it)
					},
					onFailedToLoadImage = {
						state.uri = null
					}
				)
				RachelButton(
					text = "裁剪",
					modifier = Modifier.align(Alignment.BottomEnd).zIndex(2f),
					onClick = { state.cropState.crop() }
				)
			}
		}
	}
}