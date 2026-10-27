package androidx.compose.ui.platform

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.*

internal class TestTextInputRequest : PlatformTextInputMethodRequest {
    var currentValue = TextFieldValue()
    private val processor = EditProcessor()
    override val value: () -> TextFieldValue = { currentValue }
    override val state = object : TextEditorState {
        override val selection: TextRange get() = currentValue.selection
        override val composition: TextRange? get() = currentValue.composition
        override val text: String get() = currentValue.text
        override val length: Int get() = text.length
        override fun get(index: Int): Char = text[index]
        override fun subSequence(startIndex: Int, endIndex: Int): CharSequence = text.subSequence(startIndex, endIndex)
    }
    override val imeOptions = ImeOptions(singleLine = true)
    override val onEditCommand: (List<EditCommand>) -> Unit = {
        processor.reset(currentValue, null)
        currentValue = processor.apply(it)
    }
    override val onImeAction: ((ImeAction) -> Unit)? = null
    override val textLayoutResult: () -> TextLayoutResult? = { null }
    override val focusedRectInRoot: () -> Rect? = { Rect(2f, 4f, 20f, 24f) }
    override val textFieldRectInRoot: () -> Rect? = focusedRectInRoot
    override val textClippingRectInRoot: () -> Rect? = focusedRectInRoot
    override val unclippedTextOffsetInRoot: () -> Offset? = { Offset(2f, 4f) }
    override val editText: (TextEditingScope.() -> Unit) -> Unit = { block ->
        val commands = mutableListOf<EditCommand>()
        val editing = object : TextEditingScope {
            override fun deleteSurroundingTextInCodePoints(lengthBeforeCursor: Int, lengthAfterCursor: Int) { commands.add(DeleteSurroundingTextInCodePointsCommand(lengthBeforeCursor, lengthAfterCursor)) }
            override fun setSelection(start: Int, end: Int) { commands.add(SetSelectionCommand(start, end)) }
            override fun commitText(text: CharSequence, newCursorPosition: Int) { commands.add(CommitTextCommand(text.toString(), newCursorPosition)) }
            override fun setComposingRegion(start: Int, end: Int) { commands.add(SetComposingRegionCommand(start, end)) }
            override fun setComposingText(text: CharSequence, newCursorPosition: Int) { commands.add(SetComposingTextCommand(text.toString(), newCursorPosition)) }
            override fun finishComposingText() { commands.add(FinishComposingTextCommand()) }
        }
        editing.block()
        onEditCommand(commands)
    }
}
