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
import love.yinlin.compose.Theme
import love.yinlin.compose.extension.rememberFalse
import love.yinlin.compose.ui.animation.CircleLoading
import love.yinlin.compose.ui.animation.ExpandableContent
import love.yinlin.compose.ui.animation.WaveLoading
import love.yinlin.compose.ui.container.Surface
import love.yinlin.compose.ui.input.PrimaryButton
import love.yinlin.compose.ui.status.CircleProgress
import love.yinlin.compose.ui.status.LinearProgress
import love.yinlin.compose.ui.text.Text

@Stable
object AnimationPage : Page() {
    @Composable
    override fun Content() {
        ComponentColumn {
            Component("Loading") {
                ExampleRow {
                    Example("CircleLoading") {
                        CircleLoading.Content()
                    }
                    Example("WaveLoading") {
                        WaveLoading.Content()
                    }
                }
            }

            Component("Progress") {
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

            Component("ExpandableContent") {
                var isExpanded by rememberFalse()

                PrimaryButton(if (isExpanded) "expand" else "collapse", onClick = {
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