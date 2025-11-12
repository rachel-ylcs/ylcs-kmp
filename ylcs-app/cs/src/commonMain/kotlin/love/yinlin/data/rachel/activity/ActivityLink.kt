package love.yinlin.data.rachel.activity

import androidx.compose.runtime.Stable
import kotlinx.serialization.Serializable

@Stable
@Serializable
data class ActivityLink(
    val showstart: String? = null, // [秀动链接]
    val damai: String? = null, // [大麦ID]
    val maoyan: String? = null, // [猫眼ID]
    val link: String? = null, // [网页链接]
    val qqGroupPhone: String? = null, // [手机端QQ官群链接]
    val qqGroupLink: String? = null, // [网页端QQ官群链接]
) {
    val enabled: Boolean by lazy { showstart != null || damai != null || maoyan != null || link != null || qqGroupPhone != null || qqGroupLink != null }
}