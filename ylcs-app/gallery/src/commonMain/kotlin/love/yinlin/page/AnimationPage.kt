package love.yinlin.page

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import love.yinlin.Page
import love.yinlin.PageGroup
import love.yinlin.compose.Theme
import love.yinlin.compose.extension.rememberFalse
import love.yinlin.compose.ui.animation.CircleLoading
import love.yinlin.compose.ui.animation.ExpandableContent
import love.yinlin.compose.ui.animation.WaveLoading
import love.yinlin.compose.ui.container.Surface
import love.yinlin.compose.ui.icon.Icons
import love.yinlin.compose.ui.input.PrimaryButton
import love.yinlin.compose.ui.status.CircleProgress
import love.yinlin.compose.ui.status.LinearProgress
import love.yinlin.compose.ui.text.Text

@Stable
object AnimationPage : Page(
    id = "animation",
    icon = Icons.Animation,
    title = "动画",
    english = "Animation",
    description = "用动画表现加载、进度和内容展开。",
    keywords = "加载 进度 过渡",
    group = PageGroup.Interaction,
) {
    private val demoLoading = component("Loading", "用环形或波浪动画提示正在加载。", "CircleLoading WaveLoading")
    private val demoProgress = component("Progress", "用线条或圆环显示任务进度。", "LinearProgress CircleProgress")
    private val demoExpandable = component("Expander", "带过渡地展开或收起内容。", "折叠")

    @Composable
    override fun Content() {
        ComponentColumn {
            Component(demoLoading) {
                ExampleRow {
                    Example("CircleLoading") {
                        CircleLoading.Content()
                    }
                    Example("WaveLoading") {
                        WaveLoading.Content()
                    }
                }
            }

            Component(demoProgress) {
                ExampleRow {
                    val transition = rememberInfiniteTransition()
                    val progress by transition.animateFloat(0f, 1f, infiniteRepeatable(
                        animation = tween(durationMillis = Theme.animation.duration.v1, easing = LinearEasing)
                    ))
                    Example("Linear") {
                        LinearProgress(progress)
                    }
                    Example("Circle") {
                        CircleProgress(progress)
                    }
                }
            }

            Component(demoExpandable) {
                var isExpanded by rememberFalse()

                PrimaryButton(if (isExpanded) "收起内容" else "展开内容", onClick = {
                    isExpanded = !isExpanded
                })
                ExpandableContent(isExpanded) {
                    Surface(
                        modifier = Modifier.fillMaxWidth().height(Theme.size.cell4),
                        contentPadding = Theme.padding.value10
                    ) {
                        Text("hello world", style = Theme.typography.v4)
                    }
                }
            }
        }
    }
}
