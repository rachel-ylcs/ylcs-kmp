package love.yinlin.compose.platform

import androidx.compose.ui.platform.InspectorInfo

class InspectorScope @PublishedApi internal constructor(info: InspectorInfo) {
    val properties = info.properties
    infix fun String.bind(value: Any?) {
        properties[this] = value
    }
}

inline fun InspectorInfo.inspector(name: String, block: InspectorScope.() -> Unit = {}) {
    this.name = name
    InspectorScope(this).block()
}