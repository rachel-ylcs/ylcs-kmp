package love.yinlin.compose.ui

import java.awt.BorderLayout
import java.awt.Component
import javax.swing.JPanel

internal class PlatformSwingContainer(view: Component, private var dispose: (() -> Unit)?) : JPanel(BorderLayout()) {
    private var retired = false

    init {
        isOpaque = false
        add(view, BorderLayout.CENTER)
    }

    fun retire() {
        retired = true
        if (!isDisplayable) finish()
    }

    override fun removeNotify() {
        try { super.removeNotify() }
        finally { if (retired) finish() }
    }

    private fun finish() {
        val action = dispose ?: return
        dispose = null
        try { removeAll() }
        finally { action() }
    }
}
