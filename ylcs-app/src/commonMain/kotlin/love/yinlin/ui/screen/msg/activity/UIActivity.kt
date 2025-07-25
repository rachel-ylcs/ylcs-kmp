package love.yinlin.ui.screen.msg.activity

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.util.fastMap
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import love.yinlin.common.LocalImmersivePadding
import love.yinlin.common.ThemeValue
import love.yinlin.data.common.Picture
import love.yinlin.data.rachel.activity.Activity
import love.yinlin.extension.DateEx
import love.yinlin.extension.mutableRefStateOf
import love.yinlin.platform.*
import love.yinlin.ui.component.screen.dialog.FloatingDialogCrop
import love.yinlin.ui.component.image.ImageAdder
import love.yinlin.ui.component.image.ReplaceableImage
import love.yinlin.ui.component.input.DockedDatePicker
import love.yinlin.ui.component.text.TextInput
import love.yinlin.ui.component.text.TextInputState

@Stable
internal class ActivityInputState(initActivity: Activity? = null) {
    val initDate = initActivity?.ts?.let { DateEx.Formatter.standardDate.parse(it) }

    internal val title = TextInputState(initActivity?.title ?: "")
    internal var date: LocalDate? by mutableRefStateOf(initDate)
    internal val content = TextInputState(initActivity?.content ?: "")
    internal val showstart = TextInputState(initActivity?.showstart ?: "")
    internal val damai = TextInputState(initActivity?.damai ?: "")
    internal val maoyan = TextInputState(initActivity?.maoyan ?: "")
    internal val link = TextInputState(initActivity?.link ?: "")
    internal var pic: String? by mutableStateOf(initActivity?.picPath)
    internal val pics = initActivity?.let {
        it.pics.fastMap { name -> Picture(it.picPath(name)) }.toMutableStateList()
    } ?: mutableStateListOf()

    // [活动要求]
    // 必须包含内容
    // 必须属于以下之一: <包含轮播图> <包含活动名称及活动日期>
    val canSubmit by derivedStateOf {
        !title.overflow && content.ok
                && !showstart.overflow && !damai.overflow && !maoyan.overflow && !link.overflow
                && (pic != null || (title.text.isNotEmpty() && date != null))
    }

    val ts: String? get() = date?.let { DateEx.Formatter.standardDate.format(it) }
    val titleString: String? get() = title.text.ifEmpty { null }
    val contentString: String get() = content.text
    val showstartString: String? get() = showstart.text.ifEmpty { null }
    val damaiString: String? get() = damai.text.ifEmpty { null }
    val maoyanString: String? get() = maoyan.text.ifEmpty { null }
    val linkString: String? get() = link.text.ifEmpty { null }

    suspend fun pickPicture(cropDialog: FloatingDialogCrop, onPicAdd: (Path) -> Unit) {
        val path = Picker.pickPicture()?.use { source ->
            OS.Storage.createTempFile { sink -> source.transferTo(sink) > 0L }
        }
        if (path != null) {
            cropDialog.openSuspend(url = path.toString(), aspectRatio = 2f)?.let { rect ->
                OS.Storage.createTempFile { sink ->
                    SystemFileSystem.source(path).buffered().use { source ->
                        ImageProcessor(ImageCrop(rect), ImageCompress, quality = ImageQuality.High).process(source, sink)
                    }
                }?.let { onPicAdd(it) }
            }
        }
    }

    suspend fun pickPictures(onPicsAdd: (List<Path>) -> Unit) {
        Picker.pickPicture((9 - pics.size).coerceAtLeast(1))?.use { sources ->
            val path = mutableListOf<Path>()
            for (source in sources) {
                OS.Storage.createTempFile { sink ->
                    ImageProcessor(ImageCompress, quality = ImageQuality.High).process(source, sink)
                }?.let { path += it }
            }
            if (path.isNotEmpty()) onPicsAdd(path)
        }
    }
}

@Composable
internal fun ActivityInfoLayout(
    cropDialog: FloatingDialogCrop,
    input: ActivityInputState,
    onPicAdd: (Path) -> Unit,
    onPicDelete: () -> Unit,
    onPicsAdd: (List<Path>) -> Unit,
    onPicsDelete: (Int) -> Unit,
    onPicsClick: (List<Picture>, Int) -> Unit
) {
    val scope = rememberCoroutineScope()

    Column(modifier = Modifier
        .padding(LocalImmersivePadding.current)
        .fillMaxSize()
        .padding(ThemeValue.Padding.EqualValue)
        .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(ThemeValue.Padding.VerticalSpace),
    ) {
        TextInput(
            state = input.title,
            hint = "活动名称(可空, 2-4字)",
            maxLength = 4,
            imeAction = ImeAction.Next,
            modifier = Modifier.fillMaxWidth()
        )
        DockedDatePicker(
            hint = "活动时间(可空)",
            initDate = input.initDate,
            onDateSelected = { input.date = it },
            modifier = Modifier.fillMaxWidth()
        )
        TextInput(
            state = input.content,
            hint = "活动内容",
            maxLength = 512,
            maxLines = 10,
            imeAction = ImeAction.Next,
            modifier = Modifier.fillMaxWidth()
        )
        TextInput(
            state = input.showstart,
            hint = "秀动ID(可空)",
            maxLength = 1024,
            maxLines = 5,
            imeAction = ImeAction.Next,
            modifier = Modifier.fillMaxWidth()
        )
        TextInput(
            state = input.damai,
            hint = "大麦ID(可空)",
            maxLength = 16,
            imeAction = ImeAction.Next,
            modifier = Modifier.fillMaxWidth()
        )
        TextInput(
            state = input.maoyan,
            hint = "猫眼ID(可空)",
            maxLength = 16,
            imeAction = ImeAction.Next,
            modifier = Modifier.fillMaxWidth()
        )
        TextInput(
            state = input.link,
            hint = "活动链接(可空)",
            maxLength = 256,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = "轮播图(可空)",
            style = MaterialTheme.typography.titleMedium
        )
        ReplaceableImage(
            uri = input.pic,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxWidth().aspectRatio(2f),
            onReplace = {
                scope.launch { input.pickPicture(cropDialog, onPicAdd) }
            },
            onDelete = onPicDelete
        )
        Text(
            text = "活动海报",
            style = MaterialTheme.typography.titleMedium
        )
        ImageAdder(
            maxNum = 9,
            pics = input.pics,
            size = ThemeValue.Size.MicroCellWidth,
            modifier = Modifier.fillMaxWidth(),
            onAdd = { scope.launch { input.pickPictures(onPicsAdd) } },
            onDelete = { onPicsDelete(it) },
            onClick = { onPicsClick(input.pics, it) }
        )
    }
}