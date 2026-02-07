package love.yinlin.compose.ui.node

import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics

fun Modifier.semantics(role: Role? = null, contentType: ContentType? = null): Modifier = this.semantics {
    var status = false
    if (role != null) {
        this.role = role
        status = true
    }
    if (contentType != null) {
        this.contentType = contentType
        status = true
    }
    if (!status) hideFromAccessibility()
}