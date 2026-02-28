package love.yinlin

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.FlowRowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import love.yinlin.compose.Theme
import love.yinlin.compose.bold
import love.yinlin.compose.ui.layout.Divider
import love.yinlin.compose.ui.text.Text

@Stable
abstract class Page {
    @Composable
    abstract fun Content()

    @Composable
    protected inline fun ComponentColumn(content: @Composable ColumnScope.() -> Unit) {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(Theme.padding.v7),
        ) {
            content()
        }
    }

    @Composable
    protected inline fun ColumnScope.Component(title: String, content: @Composable ColumnScope.() -> Unit) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(Theme.padding.value),
            verticalArrangement = Arrangement.spacedBy(Theme.padding.v7),
        ) {
            Text(text = title, color = Theme.color.primary, style = Theme.typography.v3.bold)
            content()
        }
        Divider()
    }

    @Composable
    protected fun ExampleRow(content: @Composable FlowRowScope.() -> Unit) {
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Theme.padding.h),
            verticalArrangement = Arrangement.spacedBy(Theme.padding.v),
            content = content,
        )
    }

    @Composable
    protected inline fun Example(title: String, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
        Column(
            modifier = modifier.border(Theme.border.v6, Theme.color.outline).padding(Theme.padding.value),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Theme.padding.v),
        ) {
            content()
            Text(text = title, style = Theme.typography.v6)
        }
    }
}