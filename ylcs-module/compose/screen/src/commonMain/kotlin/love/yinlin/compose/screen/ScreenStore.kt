package love.yinlin.compose.screen

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.ViewModelStoreProvider
import androidx.lifecycle.viewmodel.viewModelFactory

internal class ScreenStore : AutoCloseable {
    private class Entry(
        val storeKey: String,
        val screen: ScreenModel,
        val reference: ScreenStoreReference,
        var removed: Boolean = false
    )

    internal companion object {
        const val DEFAULT_SCREEN_VIEWMODEL_KEY = "love.yinlin.screen"
    }

    internal val provider: ViewModelStoreProvider = ViewModelStoreProvider(parentStore = null)

    private val scopeReference = ScreenStoreReference(provider.acquireToken(ViewModelStoreProvider.ProviderMarkerKey))
    private val entries: MutableMap<ScreenID, Entry> = mutableMapOf()
    private val creating: MutableSet<ScreenID> = []

    var isClosed: Boolean = false
        private set

    operator fun contains(id: ScreenID): Boolean = entries[id]?.removed == false

    operator fun get(id: ScreenID): ScreenModel? = entries[id]?.screen

    fun create(id: ScreenID, factory: () -> ScreenModel): ScreenModel {
        require(!isClosed) { "The screen store is closed." }
        require(id !in entries) { "The screen entry already exists: $id." }
        require(creating.add(id)) { "The screen entry is already being created: $id." }

        val storeKey = id.toString()
        var reference: ScreenStoreReference? = null

        try {
            val stackReference = ScreenStoreReference(provider.acquireToken(storeKey))
            reference = stackReference
            val vmProvider = ViewModelProvider.create(
                store = provider.getOrCreate(storeKey),
                factory = viewModelFactory {
                    addInitializer(ScreenModel::class) { factory() }
                }
            )
            val screen = vmProvider[DEFAULT_SCREEN_VIEWMODEL_KEY, ScreenModel::class]

            require(!isClosed) { "The screen store was closed during creation." }

            val entry = Entry(storeKey, screen, stackReference)
            entries[id] = entry
            screen.addCloseable(AutoCloseable {
                if (entries[id] === entry) {
                    entries.remove(id)
                    provider.clearKey(storeKey)
                    entry.reference.close()
                }
            })
            require(entries[id] === entry) { "The factory returned a cleared screen." }

            return screen
        }
        catch (failure: Throwable) {
            entries.remove(id)

            runCatching {
                provider.clearKey(storeKey)
            }.exceptionOrNull()?.let {
                if (it !== failure) failure.addSuppressed(it)
            }

            runCatching {
                reference?.close()
            }.exceptionOrNull()?.let {
                if (it !== failure) failure.addSuppressed(it)
            }

            throw failure
        }
        finally {
            creating.remove(id)
        }
    }

    fun retain(id: ScreenID): ScreenStoreReference {
        require(!isClosed) { "The screen store is closed." }
        val entry = requireNotNull(entries[id]) { "The screen entry does not exist: $id." }
        require(!entry.removed) { "The screen entry has been removed: $id." }
        return ScreenStoreReference(provider.acquireToken(entry.storeKey))
    }

    fun remove(id: ScreenID) {
        val entry = entries[id] ?: return
        if (entry.removed) return
        entry.removed = true
        provider.clearKey(entry.storeKey)
        entry.reference.close()
    }

    override fun close() {
        if (isClosed) return
        isClosed = true

        var failure: Throwable? = null

        for (id in entries.keys.toList()) {
            val error = runCatching { remove(id) }.exceptionOrNull()
            failure = failure ?: error
        }
        val referenceFailure = runCatching { scopeReference.close() }.exceptionOrNull()
        val providerFailure = runCatching { provider.clearAllKeys() }.exceptionOrNull()
        (failure ?: referenceFailure ?: providerFailure)?.let { throw it }
    }
}
