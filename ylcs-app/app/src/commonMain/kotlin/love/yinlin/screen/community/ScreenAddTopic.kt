package love.yinlin.screen.community

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import love.yinlin.api.API
import love.yinlin.api.ClientAPI
import love.yinlin.compose.*
import love.yinlin.compose.data.ImageQuality
import love.yinlin.compose.screen.CommonScreen
import love.yinlin.compose.screen.ScreenManager
import love.yinlin.compose.ui.layout.EmptyBox
import love.yinlin.compose.ui.text.TextInput
import love.yinlin.compose.ui.text.TextInputState
import love.yinlin.data.Data
import love.yinlin.data.common.Picture
import love.yinlin.data.rachel.profile.UserProfile
import love.yinlin.data.rachel.topic.Comment
import love.yinlin.data.rachel.topic.EditedTopic
import love.yinlin.data.rachel.topic.Topic
import love.yinlin.extension.safeToSources
import love.yinlin.platform.ImageCompress
import love.yinlin.platform.ImageProcessor
import love.yinlin.platform.Picker
import love.yinlin.ui.component.image.ImageAdder
import love.yinlin.ui.component.input.SingleSelector
import love.yinlin.compose.ui.layout.ActionScope
import love.yinlin.screen.common.ScreenImagePreview
import love.yinlin.screen.common.ScreenMain
import love.yinlin.service
import love.yinlin.ui.component.text.RichEditor
import love.yinlin.ui.component.text.RichEditorState

@Stable
class ScreenAddTopic(manager: ScreenManager) : CommonScreen(manager) {
    @Stable
    private class InputState {
        val title = TextInputState()
        val content = RichEditorState()
        var section by mutableIntStateOf(Comment.Section.WATER)
        val pics = mutableStateListOf<Picture>()

        val canSubmit by derivedStateOf { title.ok && content.ok }
    }

    private val subScreenDiscovery = manager.get<ScreenMain>().get<SubScreenDiscovery>()

    private val input = InputState()

    private suspend fun pickPictures() {
        Picker.pickPicture((9 - input.pics.size).coerceAtLeast(1))?.use { sources ->
            for (source in sources) {
                service.os.storage.createTempFile { sink ->
                    ImageProcessor(ImageCompress, quality = ImageQuality.High).process(source, sink)
                }?.let {
                    input.pics += Picture(it.toString())
                }
            }
        }
    }

    private fun deletePic(index: Int) {
        input.pics.removeAt(index)
    }

    private suspend fun addTopic(profile: UserProfile) {
        val title = input.title.text
        val section = input.section
        val result = ClientAPI.request(
            route = API.User.Topic.SendTopic,
            data = API.User.Topic.SendTopic.Request(
                token = service.config.userToken,
                title = title,
                content = input.content.richString.toString(),
                section = section
            ),
            files = {
                API.User.Topic.SendTopic.Files(
                    pics = file(input.pics.safeToSources { SystemFileSystem.source(Path(it.image)) })
                )
            }
        )
        when (result) {
            is Data.Success -> {
                val (tid, pic) = result.data
                val currentSection = subScreenDiscovery.currentSection
                if (currentSection == Comment.Section.LATEST_TOPIC || currentSection == section) {
                    subScreenDiscovery.page.items.add(0, Topic(
                        tid = tid,
                        uid = profile.uid,
                        title = title,
                        pic = pic,
                        isTop = false,
                        coinNum = 0,
                        commentNum = 0,
                        rawSection = section,
                        name = profile.name
                    ))
                }
                service.config.editedTopic = null
                pop()
            }
            is Data.Failure -> slot.tip.error(result.message)
        }
    }

    override val title: String = "发表主题"

    override fun onBack() {
        val title = input.title.text
        val content = input.content.text
        val pics = input.pics.map { it.image }
        if (title.isNotEmpty() || content.isNotEmpty() || pics.isNotEmpty()) {
            service.config.editedTopic = EditedTopic(
                title = title,
                content = content,
                section = input.section,
                pics = pics
            )
        }
        else if (service.config.editedTopic != null) service.config.editedTopic = null
        pop()
    }

    @Composable
    override fun ActionScope.LeftActions() {
        Action(Icons.Outlined.Close, "发表") {
            service.config.editedTopic = null
            pop()
        }
    }

    @Composable
    override fun ActionScope.RightActions() {
        ActionSuspend(
            icon = Icons.Outlined.Check,
            tip = "放弃更改",
            enabled = input.canSubmit
        ) {
            val profile = service.config.userProfile
            if (profile != null) addTopic(profile = profile)
            else slot.tip.warning("请先登录")
        }
    }

    override suspend fun initialize() {
        service.config.editedTopic?.let { editedTopic ->
            input.title.text = editedTopic.title
            input.content.text = editedTopic.content
            input.section = editedTopic.section
            input.pics += editedTopic.pics.map { Picture(it) }
        }
    }

    @Composable
    override fun Content(device: Device) {
        service.config.userProfile?.let { profile ->
            Column(
                modifier = Modifier
                    .padding(LocalImmersivePadding.current)
                    .fillMaxSize()
                    .padding(CustomTheme.padding.equalValue)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(CustomTheme.padding.verticalSpace)
            ) {
                TextInput(
                    state = input.title,
                    hint = "标题",
                    maxLength = 48,
                    maxLines = 2,
                    minLines = 1,
                    clearButton = false,
                    imeAction = ImeAction.Next,
                    modifier = Modifier.fillMaxWidth()
                )
                RichEditor(
                    state = input.content,
                    maxLength = 512,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(text = "主题", style = MaterialTheme.typography.titleMedium)
                SingleSelector(
                    current = input.section,
                    onSelected = { input.section = it },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    for (section in Comment.Section.MovableSection) {
                        Item(
                            item = section,
                            title = remember(section) { Comment.Section.sectionName(section) },
                            enabled = section != Comment.Section.NOTIFICATION || profile.hasPrivilegeVIPTopic
                        )
                    }
                }
                Text(text = "图片", style = MaterialTheme.typography.titleMedium)
                ImageAdder(
                    maxNum = 9,
                    pics = input.pics,
                    size = CustomTheme.size.microCellWidth,
                    modifier = Modifier.fillMaxWidth(),
                    onAdd = { launch { pickPictures() } },
                    onDelete = { deletePic(it) },
                    onClick = { navigate(ScreenImagePreview.Args(input.pics, it)) }
                )
            }
        } ?: EmptyBox()
    }
}