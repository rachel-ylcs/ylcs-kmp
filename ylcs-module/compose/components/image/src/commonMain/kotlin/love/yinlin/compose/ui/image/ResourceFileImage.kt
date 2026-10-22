package love.yinlin.compose.ui.image

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import love.yinlin.compose.image.data.ImageOptions
import love.yinlin.compose.image.input.ImageSource

@Composable
fun ResourceFileImage(
    source: String,
    modifier: Modifier = Modifier,
    state: ImageState = rememberImageState(),
    version: String? = null,
    contentScale: ContentScale = ContentScale.Fit,
    alignment: Alignment = Alignment.Center,
    alpha: Float = 1f,
    onClick: (() -> Unit)? = null,
    contentDescription: String = "",
    placeholder: ImagePlaceholder = ImagePlaceholder.Default,
    options: ImageOptions = ImageOptions.Default
) {
    val input = remember(source) {
        ImageSource.resource(source)
    }

    BasicImage(
        input,
        modifier = modifier,
        state = state,
        version = version,
        contentScale = contentScale,
        alignment = alignment,
        alpha = alpha,
        onClick = onClick,
        contentDescription = contentDescription,
        placeholder = placeholder,
        options = options
    )
}
