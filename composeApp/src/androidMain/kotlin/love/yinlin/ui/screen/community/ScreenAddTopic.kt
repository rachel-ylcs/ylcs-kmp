package love.yinlin.ui.screen.community

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.serialization.Serializable
import love.yinlin.AppModel
import love.yinlin.api.API
import love.yinlin.api.ClientAPI
import love.yinlin.common.compressImages
import love.yinlin.data.Data
import love.yinlin.data.common.Picture
import love.yinlin.data.rachel.topic.Comment
import love.yinlin.data.rachel.topic.Topic
import love.yinlin.data.rachel.profile.UserProfile
import love.yinlin.extension.safeToSources
import love.yinlin.platform.app
import love.yinlin.ui.component.image.ImageAdder
import love.yinlin.ui.component.image.MiniIcon
import love.yinlin.ui.component.image.PictureSelector
import love.yinlin.ui.component.layout.EmptyBox
import love.yinlin.ui.component.screen.SubScreen
import love.yinlin.ui.component.text.TextInput
import love.yinlin.ui.component.text.TextInputState
import love.yinlin.ui.screen.Screen
import love.yinlin.ui.screen.common.ScreenImagePreview

@Stable
class InputState {
    internal val title = TextInputState()
    internal val content = TextInputState()
    internal var section by mutableIntStateOf(Comment.Section.WATER)
    internal val pics = mutableStateListOf<Picture>()

    val canSubmit by derivedStateOf { title.ok && content.ok }
}

@Stable
@Serializable
actual data object ScreenAddTopic : Screen<ScreenAddTopic.Model> {
    actual class Model(model: AppModel) : Screen.Model(model) {
        val input = InputState()

        fun addPic(lifecycle: LifecycleOwner, items: List<Uri>) {
            if (items.isNotEmpty()) launch {
                slot.loading.open()
                val files = compressImages(
                    lifecycle = lifecycle,
                    images = items
                )
                for (file in files) input.pics += Picture(file.absolutePath)
                slot.loading.hide()
            }
        }

        fun deletePic(index: Int) {
            input.pics.removeAt(index)
        }

        suspend fun addTopic(profile: UserProfile) {
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
                    val part = part<ScreenPartDiscovery>()
                    val currentSection = part.currentSection
                    if (currentSection == Comment.Section.LATEST || currentSection == section) {
                        part.page.items.add(0, Topic(
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
        fun SectionSelectLayout(
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
                                MiniIcon(imageVector = Icons.Filled.Done)
                            }
                        } else null
                    )
                }
            }
        }
    }

    actual override fun model(model: AppModel): Model = Model(model)

    @Composable
    actual override fun content(model: Model) {
        val lifecycle = LocalLifecycleOwner.current
        val picsPicker = PictureSelector((9 - model.input.pics.size).coerceAtLeast(1)) {
            model.addPic(lifecycle, it)
        }

        val profile = app.config.userProfile

        SubScreen(
            modifier = Modifier.fillMaxSize(),
            title = "发表主题",
            onBack = { model.pop() },
            actions = {
                ActionSuspend(icon = Icons.Outlined.Check, enabled = model.input.canSubmit) {
                    if (profile != null) model.addTopic(profile = profile)
                    else model.slot.tip.warning("请先登录")
                }
            },
            slot = model.slot
        ) {
            if (profile == null) EmptyBox()
            else {
                Column(
                    modifier = Modifier.fillMaxSize().padding(10.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    TextInput(
                        state = model.input.title,
                        hint = "标题",
                        maxLength = 48,
                        maxLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )
                    TextInput(
                        state = model.input.content,
                        hint = "内容",
                        maxLength = 512,
                        maxLines = 10,
                        clearButton = false,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(text = "主题板块", style = MaterialTheme.typography.titleMedium)
                    model.SectionSelectLayout(
                        profile = profile,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(text = "主题附图", style = MaterialTheme.typography.titleMedium)
                    ImageAdder(
                        maxNum = 9,
                        pics = model.input.pics,
                        size = 80.dp,
                        modifier = Modifier.fillMaxWidth(),
                        onAdd = { picsPicker.select() },
                        onDelete = { model.deletePic(it) },
                        onClick = { model.navigate(ScreenImagePreview(model.input.pics, it))  }
                    )
                }
            }
        }
    }
}