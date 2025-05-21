package love.yinlin.ui.screen.community

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import love.yinlin.AppModel
import love.yinlin.api.API
import love.yinlin.api.ClientAPI
import love.yinlin.common.Device
import love.yinlin.common.LocalImmersivePadding
import love.yinlin.common.ThemeValue
import love.yinlin.data.Data
import love.yinlin.data.common.Picture
import love.yinlin.data.rachel.profile.UserProfile
import love.yinlin.data.rachel.topic.Comment
import love.yinlin.data.rachel.topic.Topic
import love.yinlin.extension.safeToSources
import love.yinlin.platform.ImageCompress
import love.yinlin.platform.ImageProcessor
import love.yinlin.platform.ImageQuality
import love.yinlin.platform.OS
import love.yinlin.platform.Picker
import love.yinlin.platform.app
import love.yinlin.ui.component.image.ImageAdder
import love.yinlin.ui.component.input.SingleSelector
import love.yinlin.ui.component.layout.EmptyBox
import love.yinlin.ui.component.layout.ActionScope
import love.yinlin.ui.component.screen.CommonSubScreen
import love.yinlin.ui.component.text.RichEditor
import love.yinlin.ui.component.text.RichEditorState
import love.yinlin.ui.component.text.TextInput
import love.yinlin.ui.component.text.TextInputState
import love.yinlin.ui.screen.common.ScreenImagePreview

@Stable
class ScreenAddTopic(model: AppModel) : CommonSubScreen(model) {
    @Stable
    private class InputState {
        val title = TextInputState()
        val content = RichEditorState()
        var section by mutableIntStateOf(Comment.Section.WATER)
        val pics = mutableStateListOf<Picture>()

        val canSubmit by derivedStateOf { title.ok && content.inputState.ok }
    }

    private val input = InputState()

    private suspend fun pickPictures() {
        Picker.pickPicture((9 - input.pics.size).coerceAtLeast(1))?.use { sources ->
            for (source in sources) {
                OS.Storage.createTempFile { sink ->
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
                token = app.config.userToken,
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
                val currentSection = discoveryPart.currentSection
                if (currentSection == Comment.Section.LATEST_TOPIC || currentSection == section) {
                    discoveryPart.page.items.add(0, Topic(
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
                pop()
            }
            is Data.Error -> slot.tip.error(result.message)
        }
    }

    override val title: String = "发表主题"

    @Composable
    override fun ActionScope.RightActions() {
        ActionSuspend(
            icon = Icons.Outlined.Check,
            enabled = input.canSubmit
        ) {
            val profile = app.config.userProfile
            if (profile != null) addTopic(profile = profile)
            else slot.tip.warning("请先登录")
        }
    }

    @Composable
    override fun SubContent(device: Device) {
        app.config.userProfile?.let { profile ->
            Column(
                modifier = Modifier
                    .padding(LocalImmersivePadding.current)
                    .fillMaxSize()
                    .padding(ThemeValue.Padding.EqualValue)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(ThemeValue.Padding.VerticalSpace)
            ) {
                TextInput(
                    state = input.title,
                    hint = "标题",
                    maxLength = 48,
                    maxLines = 2,
                    minLines = 1,
                    clearButton = false,
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
                    size = ThemeValue.Size.MicroCellWidth,
                    modifier = Modifier.fillMaxWidth(),
                    onAdd = { launch { pickPictures() } },
                    onDelete = { deletePic(it) },
                    onClick = { navigate(ScreenImagePreview.Args(input.pics, it)) }
                )
            }
        } ?: EmptyBox()
    }
}