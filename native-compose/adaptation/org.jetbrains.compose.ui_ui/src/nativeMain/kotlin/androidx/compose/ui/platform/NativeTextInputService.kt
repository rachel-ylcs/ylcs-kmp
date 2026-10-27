package androidx.compose.ui.platform

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.text.input.*
import kotlinx.coroutines.awaitCancellation
import org.jetbrains.skiko.window.NativeWindowEvent
import org.jetbrains.skiko.window.NativeWindowScope
import kotlin.math.ceil
import kotlin.math.floor

/** Bridges both Compose text-input contracts to the SDL host's IME session. */
@Suppress("DEPRECATION")
internal class NativeTextInputService : PlatformTextInputService {
    private var window: NativeWindowScope? = null
    private var request: PlatformTextInputMethodRequest? = null
    private var legacyValue = TextFieldValue()
    private var legacyOptions = ImeOptions.Default
    private var legacyEdit: ((List<EditCommand>) -> Unit)? = null
    private var legacyAction: ((ImeAction) -> Unit)? = null

    fun attach(window: NativeWindowScope) {
        this.window = window
    }

    suspend fun startInputMethod(request: PlatformTextInputMethodRequest): Nothing {
        this.request = request
        try {
            window?.startTextInput()
            updateInputArea()
            awaitCancellation()
        }
        finally {
            if (this.request === request) {
                this.request = null
                if (legacyEdit == null) window?.stopTextInput()
            }
        }
    }

    override fun startInput(value: TextFieldValue, imeOptions: ImeOptions, onEditCommand: (List<EditCommand>) -> Unit, onImeActionPerformed: (ImeAction) -> Unit) {
        legacyValue = value
        legacyOptions = imeOptions
        legacyEdit = onEditCommand
        legacyAction = onImeActionPerformed
        window?.startTextInput()
    }

    override fun startInput() {
        window?.startTextInput()
    }

    override fun stopInput() {
        legacyEdit = null
        legacyAction = null
        if (request == null) window?.stopTextInput()
    }

    override fun showSoftwareKeyboard() {
        window?.startTextInput()
    }

    // Desktop IME text events must stay enabled when a software keyboard is hidden.
    override fun hideSoftwareKeyboard() = Unit

    override fun updateState(oldValue: TextFieldValue?, newValue: TextFieldValue) {
        legacyValue = newValue
    }

    override fun notifyFocusedRect(rect: Rect) {
        val host = window ?: return
        if (!rect.left.isFinite() || !rect.top.isFinite() || !rect.right.isFinite() || !rect.bottom.isFinite()) return
        val scale = host.pixelDensity
        val left = floor(rect.left / scale).toInt()
        val top = floor(rect.top / scale).toInt()
        host.setTextInputArea(left, top, (ceil(rect.right / scale).toInt() - left).coerceAtLeast(0), (ceil(rect.bottom / scale).toInt() - top).coerceAtLeast(0))
    }

    fun updateInputArea() {
        request?.focusedRectInRoot?.invoke()?.let(::notifyFocusedRect)
    }

    fun onTextInput(text: String) {
        edit([CommitTextCommand(text, 1)])
    }

    fun onTextEditing(event: NativeWindowEvent.TextEditing) {
        val value = request?.value?.invoke() ?: legacyValue
        if (event.text.isEmpty()) {
            // An empty preedit cancels the pending composition; committed text has no composition.
            edit(if (value.composition == null) [FinishComposingTextCommand()] else [SetComposingTextCommand("", 1), FinishComposingTextCommand()])
            return
        }
        val start = value.composition?.start ?: value.selection.min
        val commands = mutableListOf<EditCommand>(SetComposingTextCommand(event.text, 1))
        if (event.start >= 0) {
            val end = (event.start.toLong() + event.length.coerceAtLeast(0)).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
            commands.add(SetSelectionCommand(start + nativeUtf16Offset(event.text, event.start), start + nativeUtf16Offset(event.text, end)))
        }
        edit(commands)
    }

    fun onEnter(): Boolean {
        if (request == null && legacyEdit == null) return false
        val options = request?.imeOptions ?: legacyOptions
        if (!options.singleLine || options.imeAction == ImeAction.None) return false
        val action = request?.onImeAction ?: legacyAction ?: return false
        action(options.imeAction)
        return true
    }

    fun close() {
        try {
            if (request != null || legacyEdit != null) window?.stopTextInput()
        }
        finally {
            window = null
            request = null
            legacyEdit = null
            legacyAction = null
        }
    }

    private fun edit(commands: List<EditCommand>) {
        val callback = request?.onEditCommand ?: legacyEdit ?: return
        callback(commands)
    }
}
