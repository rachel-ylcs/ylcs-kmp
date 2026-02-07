package love.yinlin.page

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import love.yinlin.Page
import love.yinlin.compose.Theme
import love.yinlin.compose.bold
import love.yinlin.compose.ui.text.Text

@Stable
object ThemePage : Page() {
    @Composable
    override fun Content() {
        Column(
            modifier = Modifier.fillMaxSize()
                .padding(Theme.padding.e)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(Theme.padding.v1),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("primary", color = Theme.color.primary, style = Theme.typography.v4.bold, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
                Text("secondary", color = Theme.color.secondary, style = Theme.typography.v4.bold, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
                Text("tertiary", color = Theme.color.tertiary, style = Theme.typography.v4.bold, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
            }

            Column(verticalArrangement = Arrangement.spacedBy(Theme.padding.v6)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("|primaryContainer|", style = Theme.typography.v4.bold, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
                    Text("|secondaryContainer|", style = Theme.typography.v4.bold, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
                    Text("|tertiaryContainer|",style = Theme.typography.v4.bold, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(
                        modifier = Modifier.weight(1f).background(Theme.color.primaryContainer).padding(Theme.padding.value),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(Theme.padding.v)
                    ) {
                        Text(text = "onContainer", color = Theme.color.onContainer, style = Theme.typography.v4.bold)
                        Text(text = "onContainerVariant", color = Theme.color.onContainerVariant, style = Theme.typography.v4.bold)
                    }
                    Column(
                        modifier = Modifier.weight(1f).background(Theme.color.secondaryContainer).padding(Theme.padding.value),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(Theme.padding.v)
                    ) {
                        Text(text = "onContainer", color = Theme.color.onContainer, style = Theme.typography.v4.bold)
                        Text(text = "onContainerVariant", color = Theme.color.onContainerVariant, style = Theme.typography.v4.bold)
                    }
                    Column(
                        modifier = Modifier.weight(1f).background(Theme.color.tertiaryContainer).padding(Theme.padding.value),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(Theme.padding.v)
                    ) {
                        Text(text = "onContainer", color = Theme.color.onContainer, style = Theme.typography.v4.bold)
                        Text(text = "onContainerVariant", color = Theme.color.onContainerVariant, style = Theme.typography.v4.bold)
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(Theme.padding.v6)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("|background|", style = Theme.typography.v4.bold, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
                    Text("|backgroundVariant|", style = Theme.typography.v4.bold, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
                    Text("|surface|",style = Theme.typography.v4.bold, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(
                        modifier = Modifier.weight(1f).background(Theme.color.background).padding(Theme.padding.value),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(Theme.padding.v)
                    ) {
                        Text(text = "onBackground", color = Theme.color.onBackground, style = Theme.typography.v4.bold)
                        Text(text = "onBackgroundVariant", color = Theme.color.onBackgroundVariant, style = Theme.typography.v4.bold)
                    }
                    Column(
                        modifier = Modifier.weight(1f).background(Theme.color.backgroundVariant).padding(Theme.padding.value),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(Theme.padding.v)
                    ) {
                        Text(text = "onBackground", color = Theme.color.onBackground, style = Theme.typography.v4.bold)
                        Text(text = "onBackgroundVariant", color = Theme.color.onBackgroundVariant, style = Theme.typography.v4.bold)
                    }
                    Column(
                        modifier = Modifier.weight(1f).background(Theme.color.surface).padding(Theme.padding.value),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(Theme.padding.v)
                    ) {
                        Text(text = "onSurface", color = Theme.color.onSurface, style = Theme.typography.v4.bold)
                        Text(text = "onSurfaceVariant", color = Theme.color.onSurfaceVariant, style = Theme.typography.v4.bold)
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(Theme.padding.v6)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("|error|", style = Theme.typography.v4.bold, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
                    Text("|warning|", style = Theme.typography.v4.bold, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
                    Text("|scrim|", style = Theme.typography.v4.bold, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier.weight(1f).background(Theme.color.error).padding(Theme.padding.v),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(text = "onError", color = Theme.color.onError, style = Theme.typography.v4.bold)
                    }
                    Box(
                        modifier = Modifier.weight(1f).background(Theme.color.warning).padding(Theme.padding.v),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(text = "onWarning", color = Theme.color.onWarning, style = Theme.typography.v4.bold)
                    }
                    Box(
                        modifier = Modifier.weight(1f).background(Theme.color.primaryContainer).padding(Theme.padding.v),
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(
                            modifier = Modifier.fillMaxWidth().background(Theme.color.scrim.copy(alpha = 0.5f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(text = "scrim", color = Theme.color.onContainer, style = Theme.typography.v4.bold)
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier.weight(1f).border(Theme.border.v5, Theme.color.outline).padding(Theme.padding.v),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(text = "outline", style = Theme.typography.v4.bold)
                }
                Box(
                    modifier = Modifier.weight(1f).border(Theme.border.v5, Theme.color.disabledContainer).padding(Theme.padding.v),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(text = "disabled", color = Theme.color.disabledContent, style = Theme.typography.v4.bold)
                }
            }
        }
    }
}