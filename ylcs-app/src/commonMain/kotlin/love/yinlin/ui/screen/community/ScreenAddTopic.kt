package love.yinlin.ui.screen.community

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.serialization.Serializable
import love.yinlin.AppModel
import love.yinlin.api.API
import love.yinlin.api.ClientAPI
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
import love.yinlin.ui.component.image.MiniIcon
import love.yinlin.ui.component.layout.EmptyBox
import love.yinlin.ui.component.screen.SubScreen
import love.yinlin.ui.component.text.TextInput
import love.yinlin.ui.component.text.TextInputState
import love.yinlin.ui.screen.Screen
import love.yinlin.ui.screen.common.ScreenImagePreview

@Stable
class ScreenAddTopic(model: AppModel) : Screen<ScreenAddTopic.Args>(model) {
    @Stable
    @Serializable
    data object Args : Screen.Args

    @Stable
    private class InputState {
        val title = TextInputState()
        val content = TextInputState()
        var section by mutableIntStateOf(Comment.Section.WATER)
        val pics = mutableStateListOf<Picture>()

        val canSubmit by derivedStateOf { title.ok && content.ok }
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
                content = input.content.text,
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
                if (currentSection == Comment.Section.LATEST || currentSection == section) {
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

    @Composable
    private fun SectionSelectLayout(
        profile: UserProfile,
        modifier: Modifier = Modifier
    ) {
        FlowRow(
            modifier = modifier,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            itemVerticalAlignment = Alignment.CenterVertically
        ) {
            for (section in Comment.Section.MovableSection) {
                // 管理员才有发布公告权限
                FilterChip(
                    selected = section == input.section,
                    onClick = { input.section = section },
                    enabled = section != Comment.Section.NOTIFICATION || profile.hasPrivilegeVIPTopic,
                    label = { Text(text = Comment.Section.sectionName(section)) },
                    leadingIcon = if (section == input.section) {
                        {
                            MiniIcon(icon = Icons.Filled.Done)
                        }
                    } else null,
                    elevation = FilterChipDefaults.filterChipElevation(hoveredElevation = 0.dp)
                )
            }
        }
    }

    @Composable
    override fun Content() {
        val profile = app.config.userProfile

        SubScreen(
            modifier = Modifier.fillMaxSize(),
            title = "发表主题",
            onBack = { pop() },
            actions = {
                ActionSuspend(
                    icon = Icons.Outlined.Check,
                    enabled = input.canSubmit
                ) {
                    if (profile != null) addTopic(profile = profile)
                    else slot.tip.warning("请先登录")
                }
            }
        ) {
            if (profile == null) EmptyBox()
            else {
                Column(
                    modifier = Modifier.fillMaxSize().padding(10.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    TextInput(
                        state = input.title,
                        hint = "标题",
                        maxLength = 48,
                        maxLines = 2,
                        clearButton = false,
                        modifier = Modifier.fillMaxWidth()
                    )
                    TextInput(
                        state = input.content,
                        hint = "内容",
                        maxLength = 512,
                        maxLines = 10,
                        clearButton = false,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(text = "主题", style = MaterialTheme.typography.titleMedium)
                    SectionSelectLayout(
                        profile = profile,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(text = "图片", style = MaterialTheme.typography.titleMedium)
                    ImageAdder(
                        maxNum = 9,
                        pics = input.pics,
                        size = 80.dp,
                        modifier = Modifier.fillMaxWidth(),
                        onAdd = { launch { pickPictures() } },
                        onDelete = { deletePic(it) },
                        onClick = { navigate(ScreenImagePreview.Args(input.pics, it)) }
                    )
                }
            }
        }
    }
}