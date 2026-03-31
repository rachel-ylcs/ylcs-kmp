package love.yinlin.compose.game.asset

import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.ImageBitmap
import love.yinlin.compose.graphics.decode
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.getDrawableResourceBytes
import org.jetbrains.compose.resources.getSystemResourceEnvironment

@Stable
class ResourceImageLoader(vararg res: DrawableResource) : AssetLoader<DrawableResource>() {
    private val environment by lazy { getSystemResourceEnvironment() }

    override val assetIDList: List<DrawableResource> = res.toList()
    override suspend fun load(raw: Any): Asset = load(raw) { resource ->
        Asset(ImageBitmap.decode(getDrawableResourceBytes(environment, resource))!!)
    }
}