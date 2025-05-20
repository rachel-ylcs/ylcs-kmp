package love.yinlin.ui.component.text

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.AddReaction
import androidx.compose.material.icons.outlined.AlternateEmail
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Preview
import androidx.compose.material.icons.outlined.Tag
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import love.yinlin.common.Device
import love.yinlin.common.LocalDevice
import love.yinlin.common.ThemeValue
import love.yinlin.ui.component.image.ClickIcon
import love.yinlin.ui.component.layout.ActionScope

@Stable
private object RichEditorParser {
    private fun RichContainer.parseStyle(args: List<String>): Boolean {
        val type = args.getOrNull(0) ?: return false
        when (type) {
            RICH_TYPE_EMOJI -> emoji(
                id = args.getOrNull(1)?.toIntOrNull() ?: return false
            )
            RICH_TYPE_IMAGE -> image(
                uri = args.getOrNull(1) ?: return false,
                width = args.getOrNull(2)?.toFloatOrNull() ?: 1f,
                height = args.getOrNull(3)?.toFloatOrNull() ?: 1f
            )
            RICH_TYPE_LINK -> link(
                uri = args.getOrNull(1) ?: return false,
                text = args.getOrNull(2) ?: return false
            )
            RICH_TYPE_TOPIC -> topic(
                uri = args.getOrNull(1) ?: return false,
                text = args.getOrNull(2) ?: return false
            )
            RICH_TYPE_AT -> at(
                uri = args.getOrNull(1) ?: return false,
                text = args.getOrNull(2) ?: return false
            )
            else -> return false
        }
        return true
    }

    fun parse(data: String): RichString = buildRichString {
        var index = 0
        val length = data.length
        val currentNormal = StringBuilder()
        while (index < length) {
            val normalStart = index
            while (index < length && data[index] != '[') index++
            if (index > normalStart) currentNormal.append(data, normalStart, index)
            if (index < length && data[index] == '[') {
                val markStart = index
                index++
                val contentStart = index
                var found = false
                while (index < length) {
                    if (data[index] == ']') {
                        found = true
                        break
                    }
                    index++
                }
                if (found) {
                    if (currentNormal.isNotEmpty()) {
                        text(currentNormal.toString())
                        currentNormal.clear()
                    }
                    val content = data.substring(contentStart, index)
                    if (!parseStyle(content.split("|"))) text(content)
                    index++
                }
                else {
                    currentNormal.append(data.substring(markStart, length))
                    index = length // 结束循环
                }
            }
        }
        if (currentNormal.isNotEmpty()) text(currentNormal.toString())
    }
}

@Stable
class RichEditorState {
    val inputState = TextInputState()

    var enablePreview by mutableStateOf(false)

    val richString: RichString get() = RichEditorParser.parse(inputState.value.text)
}

@Composable
private fun RichEditorContent(
    state: RichEditorState,
    maxLength: Int = 0,
    maxLines: Int = 1,
    minLines: Int = maxLines,
    modifier: Modifier = Modifier
) {
    TextInput(
        state = state.inputState,
        maxLength = maxLength,
        maxLines = maxLines,
        minLines = minLines,
        clearButton = false,
        modifier = modifier
    )
    if (state.enablePreview) {
        RichText(
            text = remember(state.inputState.text) { state.richString },
            maxLines = maxLines,
            canSelected = false,
            modifier = modifier.verticalScroll(rememberScrollState())
        )
    }
}

@Composable
fun RichEditor(
    state: RichEditorState,
    maxLength: Int = 0,
    maxLines: Int = 1,
    minLines: Int = maxLines,
    modifier: Modifier = Modifier
) {
    Surface(modifier = modifier) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(ThemeValue.Padding.Value),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(ThemeValue.Padding.VerticalSpace)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(ThemeValue.Padding.HorizontalSpace),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ClickIcon(
                    icon = Icons.Outlined.Preview,
                    color = if (state.enablePreview) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    onClick = { state.enablePreview = !state.enablePreview }
                )
                ActionScope.Right.ActionLayout(modifier = Modifier.weight(1f)) {
                    Action(Icons.Outlined.AddReaction) {

                    }
                    Action(Icons.Outlined.AddPhotoAlternate) {

                    }
                    Action(Icons.Outlined.Link) {

                    }
                    Action(Icons.Outlined.AlternateEmail) {

                    }
                    Action(Icons.Outlined.Tag) {

                    }
                }
            }
            if (LocalDevice.current.type == Device.Type.PORTRAIT) {
                RichEditorContent(
                    state = state,
                    maxLength = maxLength,
                    maxLines = maxLines,
                    minLines = minLines,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(ThemeValue.Padding.HorizontalSpace)
                ) {
                    RichEditorContent(
                        state = state,
                        maxLength = maxLength,
                        maxLines = maxLines,
                        minLines = minLines,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}