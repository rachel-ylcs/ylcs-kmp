package love.yinlin.screen.account.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import love.yinlin.Contributor
import love.yinlin.ContributorGroup
import love.yinlin.Local
import love.yinlin.UpdateInfo
import love.yinlin.compose.*
import love.yinlin.extension.DateEx
import love.yinlin.compose.ui.image.MiniIcon
import love.yinlin.compose.ui.input.NormalText
import love.yinlin.compose.ui.layout.Space
import love.yinlin.screen.community.BoxText

@Composable
internal fun ContributorList(
    contributors: Array<ContributorGroup>,
    modifier: Modifier = Modifier,
    onClick: (Contributor) -> Unit
) {
    Column(modifier = modifier) {
        for (contributorGroup in contributors) {
            Row(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.weight(1f)
                        .background(contributorGroup.color.copy(alpha = 0.7f))
                        .padding(CustomTheme.padding.value),
                    horizontalArrangement = Arrangement.spacedBy(CustomTheme.padding.littleSpace, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MiniIcon(
                        icon = contributorGroup.icon,
                        size = CustomTheme.size.microIcon
                    )
                    Text(
                        text = contributorGroup.title,
                        style = MaterialTheme.typography.labelMedium,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Clip
                    )
                }
                Column(modifier = Modifier.padding(vertical = CustomTheme.border.small / 2).weight(2f).border(
                    width = CustomTheme.border.small,
                    color = contributorGroup.color.copy(0.7f)
                )) {
                    for (contributor in contributorGroup.names) {
                        Text(
                            text = contributor.name,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                                .clickable { onClick(contributor) }
                                .padding(CustomTheme.padding.extraValue),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun UpdateInfoLayout(
    updateInfo: UpdateInfo,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(CustomTheme.padding.verticalExtraSpace),
    ) {
        Text(
            text = "${Local.info.name} ${Local.info.versionName} 更新日志",
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth()
        )
        Space()
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(CustomTheme.padding.horizontalSpace),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (updateInfo.force) BoxText(text = "强制更新", color = MaterialTheme.colorScheme.primary)
            if (updateInfo.maintenance) BoxText(text = "停服维护", color = MaterialTheme.colorScheme.secondary)
            Text(
                text = remember { DateEx.Formatter.standardDate.format(updateInfo.date) ?: "" },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.End,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        }
        Text(
            text = updateInfo.platform,
            modifier = Modifier.fillMaxWidth()
        )
        updateInfo.title?.let { title ->
            Text(
                text = title,
                color = CustomTheme.colorScheme.warning,
                modifier = Modifier.fillMaxWidth()
            )
        }
        for (group in updateInfo.groups) {
            if (group.records.isNotEmpty()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    color = group.background.copy(alpha = 0.2f)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(CustomTheme.padding.extraValue),
                        verticalArrangement = Arrangement.spacedBy(CustomTheme.padding.verticalSpace),
                    ) {
                        NormalText(
                            text = group.type,
                            icon = group.icon,
                            color = group.color,
                            style = MaterialTheme.typography.labelMedium,
                            padding = CustomTheme.padding.zeroValue
                        )
                        Space()
                        for (record in group.records) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(CustomTheme.padding.horizontalSpace),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(modifier = Modifier
                                    .size(CustomTheme.size.dotHeight)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.onSurface)
                                )
                                Text(text = record)
                            }
                        }
                    }
                }
            }
        }
    }
}