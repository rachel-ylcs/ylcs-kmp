package love.yinlin.compose.ui.floating

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import love.yinlin.compose.Theme
import love.yinlin.compose.ui.input.PrimaryTextButton
import love.yinlin.compose.ui.input.Switch
import love.yinlin.compose.ui.input.TextButton
import love.yinlin.compose.ui.node.dashBorder
import love.yinlin.compose.ui.text.Input
import love.yinlin.compose.ui.text.InputState
import love.yinlin.compose.ui.text.SimpleEllipsisText
import love.yinlin.data.rachel.song.SongFilter

@Stable
internal class DialogServerModFilter : DialogTemplate<SongFilter>() {
    val inputKey = InputState(maxLength = 32)
    val inputSinger = InputState(maxLength = 32)
    val inputLyricist = InputState(maxLength = 32)
    val inputComposer = InputState(maxLength = 32)
    val inputAlbum = InputState(maxLength = 32)

    var useAnimation = mutableStateOf(false)
    var useVideo = mutableStateOf(false)
    var useRhyme = mutableStateOf(false)
    var useAccompaniment = mutableStateOf(false)

    suspend fun open(): SongFilter? {
        inputKey.clear()
        inputSinger.clear()
        inputLyricist.clear()
        inputComposer.clear()
        inputAlbum.clear()
        useAnimation.value = false
        useVideo.value = false
        useRhyme.value = false
        useAccompaniment.value = false
        return awaitResult()
    }

    override val actions: @Composable RowScope.() -> Unit = {
        PrimaryTextButton(text = Theme.value.dialogOkText, onClick = {
            future?.send(SongFilter(
                key = inputKey.text.ifEmpty { null },
                singer = inputSinger.text.ifEmpty { null },
                lyricist = inputLyricist.text.ifEmpty { null },
                composer = inputComposer.text.ifEmpty { null },
                album = inputAlbum.text.ifEmpty { null },
                useAnimation = useAnimation.value,
                useVideo = useVideo.value,
                useRhyme = useRhyme.value,
                useAccompaniment = useAccompaniment.value
            ))
        })
        TextButton(text = Theme.value.dialogCancelText, onClick = ::close)
    }

    @Composable
    fun SwitchLayout(title: String, state: MutableState<Boolean>) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(Theme.padding.h),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SimpleEllipsisText(title)
            Switch(checked = state.value, onCheckedChange = { state.value = it })
        }
    }

    @Composable
    override fun Land() {
        LandDialogTemplate("筛选MOD") {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(Theme.padding.v9)
            ) {
                Input(state = inputKey, hint = "关键字(可选)", modifier = Modifier.fillMaxWidth())

                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .dashBorder(Theme.border.v7, Theme.color.primary, Theme.shape.v7)
                        .padding(Theme.padding.value),
                    horizontalArrangement = Arrangement.spacedBy(Theme.padding.h9),
                    verticalArrangement = Arrangement.spacedBy(Theme.padding.v9),
                    maxItemsInEachRow = 2
                ) {
                    SwitchLayout("动画", useAnimation)
                    SwitchLayout("视频", useVideo)
                    SwitchLayout("音游", useRhyme)
                    SwitchLayout("伴奏", useAccompaniment)
                }

                Input(state = inputSinger, hint = "歌手(可选)", modifier = Modifier.fillMaxWidth())
                Input(state = inputLyricist, hint = "作词(可选)", modifier = Modifier.fillMaxWidth())
                Input(state = inputComposer, hint = "作曲(可选)", modifier = Modifier.fillMaxWidth())
                Input(state = inputAlbum, hint = "专辑(可选)", modifier = Modifier.fillMaxWidth())
            }
        }
    }
}