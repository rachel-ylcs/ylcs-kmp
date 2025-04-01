package love.yinlin

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import love.yinlin.platform.appNative

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        appNative.activityResultRegistry = activityResultRegistry
        setContent {
            AppWrapper {
                App()
            }
        }
    }
}