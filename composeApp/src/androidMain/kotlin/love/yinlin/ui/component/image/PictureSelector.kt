package love.yinlin.ui.component.image

import android.net.Uri
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

class PicturePicker(private val launcher: ManagedActivityResultLauncher<PickVisualMediaRequest, *>) {
	fun select() = launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
}

@Composable
fun PictureSelector(onSelect: (Uri) -> Unit): PicturePicker {
	val launcher = rememberLauncherForActivityResult(
		contract = ActivityResultContracts.PickVisualMedia()
	) { result -> result?.let { onSelect(it) } }
	return remember(launcher) { PicturePicker(launcher) }
}

@Composable
fun PictureSelector(num: Int, onSelect: (List<Uri>) -> Unit): PicturePicker {
	val contract = remember(num) {
		if (num == 1) ActivityResultContracts.PickVisualMedia() else ActivityResultContracts.PickMultipleVisualMedia(num)
	}

	val launcher = rememberLauncherForActivityResult(
		contract = contract
	) { result ->
		when (result) {
			is Uri -> onSelect(listOf(result))
			is List<*> -> {
				val items = mutableListOf<Uri>()
				for (item in result) {
					if (item is Uri) items += item
				}
				onSelect(items)
			}
		}
	}
	return remember(launcher) { PicturePicker(launcher) }
}