package love.yinlin.ui.screen.msg.pictures

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import love.yinlin.common.Colors
import love.yinlin.extension.rememberState
import love.yinlin.resources.Res
import love.yinlin.resources.img_logo
import love.yinlin.ui.component.image.CropImage
import love.yinlin.ui.component.image.CropState
import org.jetbrains.compose.resources.imageResource

@Composable
fun ScreenPictures() {
	val bitmap = imageResource(Res.drawable.img_logo)
	var result: ImageBitmap? by rememberState { null }
	val state = remember { CropState(aspectRatio = 2f, bitmap = bitmap) }

	Column(modifier = Modifier.fillMaxSize()) {
		CropImage(
			state = state,
			onCropped = {
				result = it
			},
			modifier = Modifier.fillMaxWidth().weight(1f)
		)
		Box(
			modifier = Modifier.fillMaxWidth().weight(1f).background(Colors.Red4)
		) {
			result?.let {
				Image(
					bitmap = it,
					contentDescription = null,
					contentScale = ContentScale.Inside,
					modifier = Modifier.fillMaxSize()
				)
			}
		}
	}

	//Text(text = "ScreenPictures")
}