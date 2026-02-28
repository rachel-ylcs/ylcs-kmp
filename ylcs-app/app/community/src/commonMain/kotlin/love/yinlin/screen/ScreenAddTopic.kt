package love.yinlin.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.ImeAction
import kotlinx.io.files.Path
import kotlinx.io.readByteArray
import love.yinlin.app
import love.yinlin.common.DataSourceDiscovery
import love.yinlin.compose.LocalImmersivePadding
import love.yinlin.compose.Theme
import love.yinlin.compose.bold
import love.yinlin.compose.data.ImageQuality
import love.yinlin.compose.graphics.PlatformImage
import love.yinlin.compose.graphics.decode
import love.yinlin.compose.graphics.encode
import love.yinlin.compose.graphics.thumbnail
import love.yinlin.compose.screen.Screen
import love.yinlin.compose.ui.container.AdderBox
import love.yinlin.compose.ui.icon.Icons
import love.yinlin.compose.ui.image.Icon
import love.yinlin.compose.ui.image.LoadingIcon
import love.yinlin.compose.ui.image.WebImage
import love.yinlin.compose.ui.input.Filter
import love.yinlin.compose.ui.text.Input
import love.yinlin.compose.ui.text.InputState
import love.yinlin.compose.ui.text.RichEditor
import love.yinlin.compose.ui.text.RichEditorState
import love.yinlin.compose.ui.text.Text
import love.yinlin.cs.ApiTopicSendTopic
import love.yinlin.cs.apiFile
import love.yinlin.cs.request
import love.yinlin.data.compose.Picture
import love.yinlin.data.rachel.profile.UserProfile
import love.yinlin.data.rachel.topic.Comment
import love.yinlin.data.rachel.topic.EditedTopic
import love.yinlin.data.rachel.topic.Topic

@Stable
class ScreenAddTopic : Screen() {
    @Stable
    private class TopicInputState {
        val title = InputState(maxLength = 48)
        val content = RichEditorState(maxLength = 512)
        var section by mutableIntStateOf(Comment.Section.WATER)
        val pics = mutableStateListOf<Picture>()

        val canSubmit by derivedStateOf { title.isSafe && content.isSafe }
    }

    private val input = TopicInputState()

    private suspend fun pickPictures() {
        app.picker.pickPicture((9 - input.pics.size).coerceAtLeast(1))?.use { sources ->
            for (source in sources) {
                app.os.storage.createTempFile { sink ->
                    val image = PlatformImage.decode(source.readByteArray())!!
                    image.thumbnail()
                    sink.write(image.encode(quality = ImageQuality.High)!!)
                    true
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
        ApiTopicSendTopic.request(app.config.userToken, title, input.content.richString.toString(), section, apiFile(input.pics.map { Path(it.image) })) { tid, pic ->
            val currentSection = DataSourceDiscovery.currentSection
            if (currentSection == Comment.Section.LATEST_TOPIC || currentSection == section) {
                DataSourceDiscovery.page.items.add(0, Topic(
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
            app.config.editedTopic = null
            pop()
        }.errorTip
    }

    override val title: String = "发表主题"

    override suspend fun initialize() {
        app.config.editedTopic?.let { editedTopic ->
            input.title.text = editedTopic.title
            input.content.text = editedTopic.content
            input.section = editedTopic.section
            input.pics += editedTopic.pics.map { Picture(it) }
        }
    }

    override fun onBack() {
        val title = input.title.text
        val content = input.content.text
        val pics = input.pics.map { it.image }
        if (title.isNotEmpty() || content.isNotEmpty() || pics.isNotEmpty()) {
            app.config.editedTopic = EditedTopic(
                title = title,
                content = content,
                section = input.section,
                pics = pics
            )
        }
        else if (app.config.editedTopic != null) app.config.editedTopic = null
        pop()
    }

    @Composable
    override fun RowScope.LeftActions() {
        Icon(icon = Icons.Clear, tip = "发表", onClick = {
            app.config.editedTopic = null
            pop()
        })
    }

    @Composable
    override fun RowScope.RightActions() {
        LoadingIcon(icon = Icons.Check, tip = "放弃更改", enabled = input.canSubmit, onClick = {
            val profile = app.config.userProfile
            if (profile != null) addTopic(profile = profile)
            else slot.tip.warning("请先登录")
        })
    }

    @Composable
    override fun Content() {
        val profile = app.config.userProfile
        if (profile != null) {
            Column(
                modifier = Modifier
                    .padding(LocalImmersivePadding.current)
                    .fillMaxSize()
                    .padding(Theme.padding.eValue)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(Theme.padding.v9)
            ) {
                Input(
                    state = input.title,
                    hint = "标题",
                    maxLines = 2,
                    minLines = 1,
                    imeAction = ImeAction.Next,
                    modifier = Modifier.fillMaxWidth()
                )

                RichEditor(state = input.content, hint = "内容", modifier = Modifier.fillMaxWidth())

                Text(text = "主题", style = Theme.typography.v7.bold)
                Filter(
                    size = Comment.Section.MovableSection.size,
                    selectedProvider = { input.section == Comment.Section.MovableSection[it] },
                    titleProvider = { Comment.Section.sectionName(Comment.Section.MovableSection[it]) },
                    enabledProvider = { Comment.Section.MovableSection[it] != Comment.Section.NOTIFICATION || profile.hasPrivilegeVIPTopic },
                    onClick = { index, selected ->
                        if (selected) input.section = Comment.Section.MovableSection[index]
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Text(text = "图片", style = Theme.typography.v7.bold)
                AdderBox(
                    maxNum = 9,
                    items = input.pics,
                    modifier = Modifier.fillMaxWidth(),
                    onAdd = { launch { pickPictures() } },
                    onReplace = { index, _ -> navigate(::ScreenImagePreview, input.pics.toList(), index) },
                    onDelete = { index, _ -> deletePic(index) }
                ) { _, pic ->
                    WebImage(
                        uri = pic.image,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}