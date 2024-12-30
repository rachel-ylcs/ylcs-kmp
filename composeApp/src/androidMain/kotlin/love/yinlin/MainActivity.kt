package love.yinlin

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import love.yinlin.model.AppModel
import love.yinlin.platform.AndroidContext
import love.yinlin.platform.KV

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val context = AndroidContext(this)
        val kv = KV(this)
        setContent {
            AppWrapper(context) {
                App(viewModel { AppModel(kv) })
            }
        }
    }
}