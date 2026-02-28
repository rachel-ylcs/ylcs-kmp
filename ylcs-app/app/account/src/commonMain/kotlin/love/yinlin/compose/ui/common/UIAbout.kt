package love.yinlin.compose.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import love.yinlin.Local
import love.yinlin.compose.LocalColor
import love.yinlin.compose.LocalColorVariant
import love.yinlin.compose.Theme
import love.yinlin.compose.bold
import love.yinlin.compose.ui.container.Surface
import love.yinlin.compose.ui.container.ThemeContainer
import love.yinlin.compose.ui.image.Icon
import love.yinlin.compose.ui.image.WebImage
import love.yinlin.compose.ui.text.SelectionBox
import love.yinlin.compose.ui.text.SimpleEllipsisText
import love.yinlin.compose.ui.text.Text
import love.yinlin.cs.ServerRes
import love.yinlin.cs.url
import love.yinlin.data.rachel.literal.AppContributor
import love.yinlin.data.rachel.literal.AppContributorGroup
import love.yinlin.data.rachel.literal.AppUpdateInfo
import love.yinlin.extension.DateEx

@Composable
internal fun ContributorLayout(
    contributors: List<AppContributorGroup>,
    modifier: Modifier = Modifier,
    onClick: (AppContributor) -> Unit
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(Theme.padding.v7),
    ) {
        for (contributorGroup in contributors) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(Theme.padding.h),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ThemeContainer(contributorGroup.color) {
                    Icon(icon = contributorGroup.icon)
                    SimpleEllipsisText(text = contributorGroup.title, style = Theme.typography.v6.bold)
                }
            }
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Theme.padding.h),
                verticalArrangement = Arrangement.spacedBy(Theme.padding.v),
                maxItemsInEachRow = 3
            ) {
                for (contributor in contributorGroup.names) {
                    Box(
                        modifier = Modifier.fillMaxWidth(0.3f).clip(Theme.shape.v7).clickable { onClick(contributor) },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            modifier = Modifier.padding(Theme.padding.eValue),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(Theme.padding.v9)
                        ) {
                            WebImage(
                                uri = ServerRes.Users.User(contributor.uid).avatar.url,
                                key = remember { DateEx.TodayString },
                                circle = true,
                                modifier = Modifier.size(Theme.size.image8)
                            )
                            SimpleEllipsisText(text = contributor.name)
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun UpdateInfoLayout(updateInfo: AppUpdateInfo, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(Theme.padding.v8),
    ) {
        SimpleEllipsisText(
            text = "${Local.info.name} ${Local.info.versionName} 更新日志",
            style = Theme.typography.v6.bold,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (updateInfo.force) BoxText(text = "强制更新", color = Theme.color.primary)
                if (updateInfo.maintenance) BoxText(text = "停服维护", color = Theme.color.secondary)
            }

            SimpleEllipsisText(
                text = remember { DateEx.Formatter.standardDate.format(updateInfo.date) ?: "" },
                color = LocalColorVariant.current
            )
        }

        Text(text = updateInfo.platform)
        updateInfo.title?.let { title ->
            Text(text = title, color = Theme.color.warning)
        }

        for (group in updateInfo.groups) {
            if (group.records.isNotEmpty()) {
                Surface(modifier = Modifier.fillMaxWidth(), shape = Theme.shape.v5) {
                    Column(
                        modifier = Modifier.fillMaxWidth().background(group.background.copy(alpha = 0.2f)).padding(Theme.padding.value9),
                        verticalArrangement = Arrangement.spacedBy(Theme.padding.v9),
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(Theme.padding.h),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ThemeContainer(group.color) {
                                Icon(icon = group.icon)
                                SimpleEllipsisText(text = group.type, style = Theme.typography.v6.bold)
                            }
                        }

                        for (record in group.records) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(Theme.padding.h)
                            ) {
                                Box(modifier = Modifier.offset(y = Theme.size.box4).size(Theme.size.box3).clip(Theme.shape.circle).background(LocalColor.current))
                                SelectionBox { Text(text = record) }
                            }
                        }
                    }
                }
            }
        }
    }
}