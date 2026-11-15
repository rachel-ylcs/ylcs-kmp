package love.yinlin.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import love.yinlin.app
import love.yinlin.compose.Device
import love.yinlin.compose.LocalImmersivePadding
import love.yinlin.compose.Theme
import love.yinlin.compose.ds.DataSourceActivity
import love.yinlin.compose.extension.movableComposable
import love.yinlin.compose.rememberDeviceType
import love.yinlin.compose.screen.Screen
import love.yinlin.compose.ui.common.SettingsLayout
import love.yinlin.compose.ui.icon.Icons
import love.yinlin.data.rachel.profile.UserProfile

@Stable
class ScreenAdmin : Screen() {
    override val title: String = "超管空间"

    private fun addActivity(profile: UserProfile, hide: Boolean) {
        if (profile.hasPrivilegeVIPCalendar) {
            launch {
                DataSourceActivity.addActivity(hide).onSuccess {
                    navigate(::ScreenModifyActivity, it)
                }.exceptionOrNull().errorTip
            }
        }
        else slot.tip.warning("没有权限")
    }

    @Composable
    private fun ActivityPanel(profile: UserProfile, modifier: Modifier) {
        SettingsLayout(
            modifier = modifier,
            title = "活动",
            icon = Icons.Gift,
            color = Theme.color.secondaryContainer
        ) {
            ItemExpander(
                title = "添加公开活动",
                icon = Icons.Public,
                onClick = { addActivity(profile, false) }
            )

            ItemExpander(
                title = "添加私密活动",
                icon = Icons.Lock,
                onClick = { addActivity(profile, true) }
            )

            ItemExpander(
                title = "提取秀动链接",
                icon = Icons.Link,
                onClick = { navigate(::ScreenActivityLink) }
            )
        }
    }

    private val adminPanel = movableComposable { profile: UserProfile, modifier: Modifier ->
        ActivityPanel(profile, modifier)
    }

    @Composable
    private fun Portrait(profile: UserProfile) {
        Column(modifier = Modifier
            .padding(LocalImmersivePadding.current)
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
        ) {
            adminPanel(profile, Modifier.fillMaxWidth().padding(Theme.padding.eValue))
        }
    }

    @Composable
    private fun Landscape(profile: UserProfile) {
        Row(modifier = Modifier
            .padding(LocalImmersivePadding.current)
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
        ) {
            adminPanel(profile, Modifier.weight(1f).padding(Theme.padding.eValue))
        }
    }

    @Composable
    override fun Content() {
        val profile = app.config.userProfile

        if (profile?.hasPrivilegeVIPAccount == true) {
            val deviceType by rememberDeviceType()
            when (deviceType) {
                Device.Type.PORTRAIT, Device.Type.SQUARE -> Portrait(profile)
                Device.Type.LANDSCAPE -> Landscape(profile)
            }
        }
    }
}