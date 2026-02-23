package love.yinlin.compose.ui.node

import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.semantics.*

fun Modifier.semantics(role: Role? = null, contentType: ContentType? = null, description: String? = null): Modifier = this.semantics {
    var status = false
    if (role != null) {
        this.role = role
        status = true
    }
    if (contentType != null) {
        this.contentType = contentType
        status = true
    }
    if (description != null) {
        this.contentDescription = description
        status = true
    }
    if (!status) hideFromAccessibility()
}