package love.yinlin.data.item

import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import ylcs_kmp.composeapp.generated.resources.Res
import ylcs_kmp.composeapp.generated.resources.home_nav_discovery
import ylcs_kmp.composeapp.generated.resources.home_nav_me
import ylcs_kmp.composeapp.generated.resources.home_nav_msg
import ylcs_kmp.composeapp.generated.resources.home_nav_music
import ylcs_kmp.composeapp.generated.resources.home_nav_world
import ylcs_kmp.composeapp.generated.resources.tab_discovery_active
import ylcs_kmp.composeapp.generated.resources.tab_discovery_normal
import ylcs_kmp.composeapp.generated.resources.tab_me_active
import ylcs_kmp.composeapp.generated.resources.tab_me_normal
import ylcs_kmp.composeapp.generated.resources.tab_msg_active
import ylcs_kmp.composeapp.generated.resources.tab_msg_normal
import ylcs_kmp.composeapp.generated.resources.tab_music_active
import ylcs_kmp.composeapp.generated.resources.tab_music_normal
import ylcs_kmp.composeapp.generated.resources.tab_world_active
import ylcs_kmp.composeapp.generated.resources.tab_world_normal

enum class TabItem(
	val title: StringResource,
	val iconNormal: DrawableResource,
	val iconActive: DrawableResource
) {
	WORLD(Res.string.home_nav_world, Res.drawable.tab_world_normal, Res.drawable.tab_world_active),
	MSG(Res.string.home_nav_msg, Res.drawable.tab_msg_normal, Res.drawable.tab_msg_active),
	MUSIC(Res.string.home_nav_music, Res.drawable.tab_music_normal, Res.drawable.tab_music_active),
	DISCOVERY(Res.string.home_nav_discovery, Res.drawable.tab_discovery_normal, Res.drawable.tab_discovery_active),
	ME(Res.string.home_nav_me, Res.drawable.tab_me_normal, Res.drawable.tab_me_active),
}