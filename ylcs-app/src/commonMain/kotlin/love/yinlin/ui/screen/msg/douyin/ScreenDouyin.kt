package love.yinlin.ui.screen.msg.douyin

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.Modifier
import love.yinlin.AppModel
import love.yinlin.common.Device
import love.yinlin.ui.component.platform.HeadlessBrowser
import love.yinlin.ui.component.screen.CommonSubScreen

@Stable
class ScreenDouyin(model: AppModel) : CommonSubScreen(model) {
    override val title: String = "抖音"

    private val items = mutableStateListOf<String>()
    private val browser = object : HeadlessBrowser() {
        override fun onRequest(url: String, response: String) {
            if (url.contains("aweme/v1/web/aweme/post/")) {
                items += "url=$url, response=$response"
            }
        }
    }

    override suspend fun initialize() {
        browser.load("https://www.douyin.com/user/MS4wLjABAAAATAf7yHksdW6CBPSjl9CW8k3c_x_drbwg0CVLTowlwzE")
    }

    override fun finalize() {
        browser.destroy()
    }

    @Composable
    override fun SubContent(device: Device) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
        ) {
            items(items) {
                Text(it)
            }
        }
    }
}