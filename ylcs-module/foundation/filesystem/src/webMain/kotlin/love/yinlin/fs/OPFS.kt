@file:OptIn(ExperimentalWasmJsInterop::class)
@file:Suppress("unused")
package love.yinlin.fs

import org.w3c.dom.Navigator
import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.js

private fun getNavigatorStorage(navigator: Navigator): StorageManager = js("navigator.storage")

val Navigator.storage: StorageManager get() = getNavigatorStorage(this)