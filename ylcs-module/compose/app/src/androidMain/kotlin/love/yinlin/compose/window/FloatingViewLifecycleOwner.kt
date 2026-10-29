package love.yinlin.compose.window

import android.app.Application
import android.content.Context
import androidx.lifecycle.HasDefaultViewModelProviderFactory
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.SAVED_STATE_REGISTRY_OWNER_KEY
import androidx.lifecycle.SavedStateViewModelFactory
import androidx.lifecycle.VIEW_MODEL_STORE_OWNER_KEY
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.enableSavedStateHandles
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.MutableCreationExtras
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import love.yinlin.extension.cleaning

internal class FloatingViewLifecycleOwner(context: Context) : SavedStateRegistryOwner, ViewModelStoreOwner, HasDefaultViewModelProviderFactory {
    override val lifecycle = LifecycleRegistry(this)
    override val viewModelStore = ViewModelStore()
    private val controller = SavedStateRegistryController.create(this)
    override val savedStateRegistry: SavedStateRegistry get() = controller.savedStateRegistry
    override val defaultViewModelProviderFactory: ViewModelProvider.Factory = SavedStateViewModelFactory()
    override val defaultViewModelCreationExtras: CreationExtras = MutableCreationExtras().apply {
        set(SAVED_STATE_REGISTRY_OWNER_KEY, this@FloatingViewLifecycleOwner)
        set(VIEW_MODEL_STORE_OWNER_KEY, this@FloatingViewLifecycleOwner)
        (context.applicationContext as? Application)?.let { set(ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY, it) }
    }

    init {
        controller.performAttach()
        enableSavedStateHandles()
        controller.performRestore(null)
        lifecycle.currentState = Lifecycle.State.CREATED
    }

    fun start() { lifecycle.currentState = Lifecycle.State.RESUMED }

    fun destroy() = cleaning({ lifecycle.currentState = Lifecycle.State.DESTROYED }) { viewModelStore.clear() }
}
