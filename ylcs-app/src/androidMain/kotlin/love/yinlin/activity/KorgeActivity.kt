package love.yinlin.activity

import android.content.Intent
import android.os.Bundle
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import korlibs.korge.Korge
import korlibs.korge.android.KorgeAndroidView

class KorgeActivity : ComponentActivity() {
    companion object {
        var GlobalKorge: Korge? = null
    }

    private lateinit var view: KorgeAndroidView
    private var korge: Korge? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        korge = GlobalKorge
        GlobalKorge = null
        view = KorgeAndroidView(this).apply {
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        }
        setContentView(view)
        korge?.let {
            view.loadModule(it)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        korge?.let {
            view.unloadModule()
            korge = null
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        GlobalKorge = null
    }
}