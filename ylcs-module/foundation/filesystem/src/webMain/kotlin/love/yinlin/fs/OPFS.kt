@file:OptIn(ExperimentalWasmJsInterop::class)
@file:Suppress("unused")
package love.yinlin.fs

import org.w3c.dom.Navigator
import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.Promise
import kotlin.js.js

private fun getNavigatorStorage(navigator: Navigator): StorageManager = js("navigator.storage")

val Navigator.storage: StorageManager get() = getNavigatorStorage(this)

internal fun openOpfs(): Promise<FileSystemDirectoryHandle> = js("""
    (function() {
        const storage = typeof navigator !== 'undefined' && navigator.storage;
        if (!storage || typeof storage.getDirectory !== 'function') {
            return Promise.reject(new Error('OPFS requires a supported browser and a secure context'));
        }
        return storage.getDirectory();
    })()
""")

internal fun <T : JsAny> lookupEntry(promise: Promise<T>): Promise<T?> = js("""
    promise.catch(error => {
        if (error.name === 'NotFoundError' || error.name === 'TypeMismatchError') return null;
        throw error;
    })
""")

internal fun ignoreMissingEntry(promise: Promise<JsAny?>): Promise<JsAny?> = js("""
    promise.catch(error => {
        if (error.name !== 'NotFoundError') throw error;
    })
""")

internal fun moveOpfsFile(
    file: FileSystemFileHandle,
    parent: FileSystemDirectoryHandle,
    name: String
): Promise<JsBoolean> = js("""
    (function() {
        if (typeof file.move !== 'function') return Promise.resolve(false);
        return file.move(parent, name).then(
            function() { return true; },
            function(error) {
                if (error.name === 'NotSupportedError') return false;
                throw error;
            }
        );
    })()
""")
