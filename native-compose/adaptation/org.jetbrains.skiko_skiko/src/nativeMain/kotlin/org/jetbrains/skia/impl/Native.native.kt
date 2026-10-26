@file:OptIn(kotlin.concurrent.atomics.ExperimentalAtomicApi::class)

package org.jetbrains.skia.impl

import kotlinx.cinterop.*
import org.jetbrains.skia.ExternalSymbolName
import org.jetbrains.skiko.InternalSkikoApi
import org.jetbrains.skiko.internal.fastForEach
import kotlin.native.internal.NativePtr
import kotlin.native.concurrent.ThreadLocal
import kotlin.concurrent.atomics.AtomicReference

actual abstract class Native actual constructor(ptr: NativePointer) {
    internal actual var _ptr: NativePointer

    @InternalSkikoApi
    actual val nativePtr: NativePointer
        get() = _ptr

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (null == other) return false
        if (other !is Native) return false
        return if (_ptr == other._ptr) true else nativeEquals(other)
    }

    override fun hashCode(): Int {
        return _ptr.toLong().hashCode()
    }

    internal actual open fun nativeEquals(other: Native?): Boolean {
        return false
    }

    actual companion object {
        init {
            initCallbacks(
                staticCFunction(::callBooleanCallback),
                staticCFunction(::callIntCallback),
                staticCFunction(::callNativePtrCallback),
                staticCFunction(::callVoidCallback),
                staticCFunction(::disposeCallback),
            )
        }

        actual val NullPointer: NativePointer
            get() = NativePtr.NULL
    }

    actual override fun toString(): String {
        return this::class.simpleName + "(_ptr=0x" + _ptr.toString() + ")"
    }

    init {
        if (ptr == NativePtr.NULL) throw RuntimeException("Can't wrap nullptr")
        _ptr = ptr
    }
}

actual typealias NativePointer = NativePtr

@InternalSkikoApi
actual typealias InteropPointer = NativePtr

// An observable managed-reference use after an FFI call keeps its argument live through that call.
// The thread-local slot avoids contention and is cleared immediately instead of retaining resources.
@ThreadLocal
private val reachabilitySink = AtomicReference<Any?>(null)

@InternalSkikoApi
actual fun reachabilityBarrier(obj: Any?) {
    if (obj == null) return
    reachabilitySink.store(obj)
    reachabilitySink.store(null)
}

@InternalSkikoApi
actual inline fun <T> interopScope(block: InteropScope.() -> T): T {
    val scope = InteropScope()
    try {
        return scope.block()
    } finally {
        scope.release()
    }
}

@InternalSkikoApi
actual class InteropScope actual constructor() {
    actual fun toInterop(string: String?): InteropPointer {
        return if (string != null) {
            val pinned = convertToZeroTerminatedString(string).pin()
            elements.add(pinned)
            val result = pinned.addressOf(0).rawValue
            result
        }
        else {
            NativePtr.NULL
        }
    }

    actual fun toInterop(array: ByteArray?): InteropPointer {
        return if (array != null && array.isNotEmpty()) {
            val pinned = array.pin()
            elements.add(pinned)
            val result = pinned.addressOf(0).rawValue
            result
        }
        else {
            NativePtr.NULL
        }
    }

    actual fun toInteropForResult(array: ByteArray?): InteropPointer = toInterop(array)

    actual fun InteropPointer.fromInterop(result: ByteArray) {}

    actual fun toInterop(array: ShortArray?): InteropPointer {
        return if (array != null && array.isNotEmpty()) {
            val pinned = array.pin()
            elements.add(pinned)
            val result = pinned.addressOf(0).rawValue
            result
        }
        else {
            NativePtr.NULL
        }
    }

    actual fun toInteropForResult(array: ShortArray?): InteropPointer = toInterop(array)

    actual fun InteropPointer.fromInterop(result: ShortArray) {}

    actual fun toInterop(array: IntArray?): InteropPointer {
        return if (array != null && array.isNotEmpty()) {
            val pinned = array.pin()
            elements.add(pinned)
            val result = pinned.addressOf(0).rawValue
            result
        }
        else {
            NativePtr.NULL
        }
    }

    actual fun toInteropForResult(array: IntArray?): InteropPointer = toInterop(array)

    actual fun InteropPointer.fromInterop(result: IntArray) {}

    actual fun toInterop(array: LongArray?): InteropPointer {
        return if (array != null && array.isNotEmpty()) {
            val pinned = array.pin()
            elements.add(pinned)
            val result = pinned.addressOf(0).rawValue
            result
        }
        else {
            NativePtr.NULL
        }
    }

    actual fun InteropPointer.fromInterop(result: LongArray) {}

    actual fun toInterop(array: FloatArray?): InteropPointer {
        return if (array != null && array.isNotEmpty()) {
            val pinned = array.pin()
            elements.add(pinned)
            val result = pinned.addressOf(0).rawValue
            result
        }
        else {
            NativePtr.NULL
        }
    }

    actual fun toInteropForResult(array: FloatArray?): InteropPointer = toInterop(array)

    actual fun InteropPointer.fromInterop(result: FloatArray) {}

    actual fun toInterop(array: DoubleArray?): InteropPointer {
        return if (array != null && array.isNotEmpty()) {
            val pinned = array.pin()
            elements.add(pinned)
            val result = pinned.addressOf(0).rawValue
            result
        }
        else {
            NativePtr.NULL
        }
    }

    actual fun toInteropForResult(array: DoubleArray?): InteropPointer = toInterop(array)

    actual fun InteropPointer.fromInterop(result: DoubleArray) {}

    actual fun toInterop(array: NativePointerArray?): InteropPointer {
        return if (array != null && array.size > 0) {
            // The ABI is a pinned 64-bit pointer buffer; retain its length for bounded readback.
            val pinned = array.backing.pin()
            elements.add(pinned)
            val result = pinned.addressOf(0).rawValue
            val sizes = pointerArraySizes ?: HashMap<InteropPointer, Int>().also { pointerArraySizes = it }
            sizes[result] = array.size
            result
        }
        else {
            NativePtr.NULL
        }
    }

    actual fun toInteropForResult(array: NativePointerArray?): InteropPointer = toInterop(array)

    actual fun InteropPointer.fromInterop(result: NativePointerArray) {}

    actual fun toInterop(stringArray: Array<String>?): InteropPointer {
        if (stringArray.isNullOrEmpty()) return NativePtr.NULL

        val nativePointerArray = NativePointerArray(stringArray.size)
        stringArray.forEachIndexed { index, str ->
            val pin = convertToZeroTerminatedString(str).pin()
            elements.add(pin)
            nativePointerArray[index] = pin.addressOf(0).rawValue
        }
        return toInterop(nativePointerArray)
    }

    actual inline fun <reified T> InteropPointer.fromInterop(decoder: ArrayInteropDecoder<T>): Array<T> {
        try {
            val size = decoder.getArraySize(this)
            require(size >= 0) { "Native array returned a negative size" }
            return Array<T>(size) { decoder.getArrayElement(this, it) }
        }
        finally { decoder.disposeArray(this) }
    }

    actual fun InteropPointer.fromInteropNativePointerArray(): NativePointerArray {
        if (this == NativePtr.NULL) return NativePointerArray(0)
        val size = requireNotNull(pointerArraySizes?.get(this)) { "An external pointer buffer needs an explicit element count" }
        return fromInteropNativePointerArray(size)
    }

    /** Copies an external Native pointer buffer; its owner must keep [size] elements readable. */
    fun InteropPointer.fromInteropNativePointerArray(size: Int): NativePointerArray {
        require(size >= 0) { "Native pointer count must be non-negative" }
        if (size == 0) return NativePointerArray(0)
        require(this != NativePtr.NULL) { "A non-empty Native pointer buffer cannot be null" }
        val pointers = interpretCPointer<LongVar>(this)!!
        return NativePointerArray(size).also { result ->
            for (index in 0 ..< size) result.backing[index] = pointers[index]
        }
    }

    actual fun toInteropForArraysOfPointers(interopPointers: Array<InteropPointer>): InteropPointer {
        if (interopPointers.isEmpty()) return NativePtr.NULL
        val result = toInterop(LongArray(interopPointers.size) { interopPointers[it].toLong() })
        val sizes = pointerArraySizes ?: HashMap<InteropPointer, Int>().also { pointerArraySizes = it }
        sizes[result] = interopPointers.size
        return result
    }

    actual fun callback(callback: (() -> Unit)?) = callbackImpl(callback)
    actual fun intCallback(callback: (() -> Int)?) = callbackImpl(callback)
    actual fun nativePointerCallback(callback: (() -> NativePointer)?) = callbackImpl(callback)
    actual fun interopPointerCallback(callback: (() -> InteropPointer)?) = callbackImpl(callback)
    actual fun booleanCallback(callback: (() -> Boolean)?) = callbackImpl(callback)

    actual fun virtual(method: () -> Unit) = callbackImpl(method)
    actual fun virtualInt(method: () -> Int) = callbackImpl(method)
    actual fun virtualNativePointer(method: () -> NativePointer) = callbackImpl(method)
    actual fun virtualInteropPointer(method: () -> InteropPointer) = callbackImpl(method)
    actual fun virtualBoolean(method: () -> Boolean) = callbackImpl(method)

    actual fun release() {
        elements.fastForEach { it.unpin() }
        elements.clear()
        pointerArraySizes?.clear()
    }

    private fun <T> callbackImpl(callback: (() -> T)?): InteropPointer = callback?.let {
        val ptr = StableRef.create(it).asCPointer()
        NativePtr.NULL.plus(ptr.toLong())
    } ?: NativePtr.NULL

    private val elements = ArrayList<Pinned<*>>()
    private var pointerArraySizes: MutableMap<InteropPointer, Int>? = null
}

// Ugly! NativePtrArray in stdlib is unfortunately internal, don't have ctor and cannot be used.
actual class NativePointerArray actual constructor(size: Int) {
    internal val backing = LongArray(size)
    actual operator fun get(index: Int): NativePointer {
        return NativePtr.NULL + backing[index]
    }

    actual operator fun set(index: Int, value: NativePointer) {
        backing[index] = value.toLong()
    }

    actual val size: Int
        get() = backing.size
}

// Callbacks support

private fun callVoidCallback(ptr: COpaquePointer) {
    ptr.asStableRef<() -> Unit>().get().invoke()
}

private fun callBooleanCallback(ptr: COpaquePointer): Boolean {
    return ptr.asStableRef<() -> Boolean>().get().invoke()
}

private fun callIntCallback(ptr: COpaquePointer): Int {
    return ptr.asStableRef<() -> Int>().get().invoke()
}

private fun callNativePtrCallback(ptr: COpaquePointer): Long {
    return ptr.asStableRef<() -> NativePointer>().get().invoke().toLong()
}

private fun disposeCallback(ptr: COpaquePointer) {
    ptr.asStableRef<Any>().dispose()
}

@ExternalSymbolName("skiko_initCallbacks")
private external fun initCallbacks(
    callBoolean: COpaquePointer,
    callInt: COpaquePointer,
    callNativePointer: COpaquePointer,
    callVoid: COpaquePointer,
    dispose: COpaquePointer
)
