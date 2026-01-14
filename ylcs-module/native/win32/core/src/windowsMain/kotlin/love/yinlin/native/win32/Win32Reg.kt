@file:OptIn(ExperimentalForeignApi::class)

package love.yinlin.native.win32

import kotlinx.cinterop.*
import love.yinlin.extension.Reference
import platform.windows.*

// 注册表操作
class Reg(
    private var handle: HKEY? = null,
    var wow64: Boolean = true
) : AutoCloseable {
    enum class KeyBase(val value: ULong) {
        ClassesRoot(0x80000000UL),
        CurrentUser(0x80000001UL),
        LocalMachine(0x80000002UL),
        Users(0x80000003UL),
        CurrentConfig(0x80000005UL);

        internal val key: HKEY get() = when (this) {
            ClassesRoot -> HKEY_CLASSES_ROOT
            CurrentUser -> HKEY_CURRENT_USER
            LocalMachine -> HKEY_LOCAL_MACHINE
            Users -> HKEY_USERS
            CurrentConfig -> HKEY_CURRENT_CONFIG
        }!!
    }

    @Suppress("EnumEntryName")
    enum class KeyType(val value: UInt) {
        None(0U), // 空
        String(1U), // 字符串
        _ES(2U),
        Binary(3U), // 二进制
        Number(4U), // 数字
        _BN(5U),
        _Link(6U),
        MultiLineString(7U), // 多行字符串
        _RL(8U),
        _FRD(9U),
        _RRL(10U),
        _N64(11U);

        companion object {
            fun fromUInt(value: UInt): KeyType = when (value) {
                String.value -> String
                _ES.value -> _ES
                Binary.value -> Binary
                Number.value -> Number
                _BN.value -> _BN
                _Link.value -> _Link
                MultiLineString.value -> MultiLineString
                _RL.value -> _RL
                _FRD.value -> _FRD
                _RRL.value -> _RRL
                _N64.value -> _N64
                else -> None
            }
        }
    }

    constructor(base: KeyBase, route: String, wow64: Boolean = true) : this(null, wow64) {
        open(base, route)
    }

    private val regSAM: UInt get() = if (wow64) 0xF013FU else 0xF023FU

    private inline fun <R> withMemory(block: MemScope.(HKEY) -> R): R? = handle?.let {
        memScoped { block(it) }
    }

    // 打开项
    fun open(base: KeyBase, route: String): Boolean = memScoped {
        if (handle != null) close()
        val pTmpKey = alloc<HKEYVar>()
        if (base == KeyBase.CurrentUser) {
            val pUserKey = alloc<HKEYVar>()
            if (RegOpenCurrentUser(regSAM, pUserKey.ptr) == ERROR_SUCCESS) {
                val userKey = pUserKey.value
                val result = RegOpenKeyW(userKey, route, pTmpKey.ptr) == ERROR_SUCCESS
                RegCloseKey(userKey)
                if (result) {
                    handle = pTmpKey.value
                    return@memScoped true
                }
            }
        }
        else {
            if (RegOpenKeyW(base.key, route, pTmpKey.ptr) == ERROR_SUCCESS) {
                handle = pTmpKey.value
                return@memScoped true
            }
        }
        handle = null
        false
    }

    override fun close() {
        handle?.let(::RegCloseKey)
        handle = null
    }

    // 创建项
    fun createItem(route: String, ret: Reference<Reg?>? = null): Boolean = withMemory { hKey ->
        val pTmpKey = alloc<HKEYVar>()
        if (RegCreateKeyExW(hKey, route, 0U, null, 0U, regSAM, null, pTmpKey.ptr, null) == ERROR_SUCCESS) {
            val tmpKey = pTmpKey.value
            if (ret != null) {
                ret.value?.close()
                ret.value?.handle = tmpKey
            }
            else RegCloseKey(tmpKey)
            true
        }
        else false
    } ?: false

    // 删除项
    fun deleteItem(route: String): Boolean = withMemory { hKey ->
        RegDeleteKeyExW(hKey, route, regSAM, 0U) == ERROR_SUCCESS
    } ?: false

    // 枚举项
    val allItems: List<String> get() = withMemory { hKey ->
        val container = mutableListOf<String>()
        val buffer = allocArray<UShortVar>(MAX_PATH)
        while (true) {
            val result = RegEnumKeyW(hKey, container.size.convert(), buffer, MAX_PATH.convert())
            if (result != ERROR_SUCCESS) break
            container += buffer.toKString()
        }
        return@withMemory container
    } ?: emptyList()

    // 是否有键
    operator fun contains(key: String): Boolean = withMemory { hKey ->
        RegQueryValueExW(hKey, key, null, null, null, null) == ERROR_SUCCESS
    } ?: false

    // 取键类型
    fun keyType(key: String): KeyType = withMemory { hKey ->
        val pType = alloc<UIntVar>()
        val pSize = alloc<UIntVar>()
        if (RegQueryValueExW(hKey, key, null, pType.ptr, null, pSize.ptr) == ERROR_SUCCESS) KeyType.fromUInt(pType.value)
        else KeyType.None
    } ?: KeyType.None

    // 取二进制值
    fun binaryValue(key: String): ByteArray = withMemory { hKey ->
        val pType = alloc<UIntVar>()
        val pSize = alloc<UIntVar>()
        if (RegQueryValueExW(hKey, key, null, pType.ptr, null, pSize.ptr) == ERROR_SUCCESS) {
            val size = pSize.value.toInt()
            if (pType.value == KeyType.Binary.value) {
                val buffer = ByteArray(size)
                val result = buffer.usePinned { pinned ->
                    RegQueryValueExW(hKey, key, null, null, pinned.addressOf(0).reinterpret(), pSize.ptr)
                }
                if (result == ERROR_SUCCESS) return@withMemory buffer
            }
        }
        return@withMemory byteArrayOf()
    } ?: byteArrayOf()

    // 取数字值
    fun numberValue(key: String): ULong = withMemory { hKey ->
        val pType = alloc<UIntVar>()
        val pSize = alloc<UIntVar>()
        if (RegQueryValueExW(hKey, key, null, pType.ptr, null, pSize.ptr) == ERROR_SUCCESS) {
            val type = pType.value
            if (type == KeyType.Number.value || type == KeyType._N64.value) {
                val buffer = alloc<ULongVar>()
                val result = RegQueryValueExW(hKey, key, null, null, buffer.ptr.reinterpret(), pSize.ptr)
                if (result == ERROR_SUCCESS) return@withMemory buffer.value
            }
        }
        return@withMemory 0UL
    } ?: 0UL

    // 取字符串值
    fun stringValue(key: String): String = withMemory { hKey ->
        val pType = alloc<UIntVar>()
        val pSize = alloc<UIntVar>()
        if (RegQueryValueExW(hKey, key, null, pType.ptr, null, pSize.ptr) == ERROR_SUCCESS) {
            val type = pType.value
            val size = pSize.value
            when (type) {
                KeyType.String.value, KeyType._ES.value, KeyType.MultiLineString.value -> {
                    val actualSize = (if (size < 2U) 2U else size).let { (it - 2U) shr 1 }.toInt()
                    val buffer = allocArray<UShortVar>(actualSize) { value = 0U }
                    val result = RegQueryValueExW(hKey, key, null, null, buffer.reinterpret(), pSize.ptr)
                    if (result == ERROR_SUCCESS && type == KeyType.MultiLineString.value) {
                        for (i in 0 ..< actualSize) {
                            if (buffer[i] == 0U.toUShort()) buffer[i] = '\n'.code.convert()
                        }
                    }
                }
                KeyType.Number.value, KeyType._N64.value -> {
                    val buffer = alloc<ULongVar>()
                    val result = RegQueryValueExW(hKey, key, null, null, buffer.ptr.reinterpret(), pSize.ptr)
                    if (result == ERROR_SUCCESS) return@withMemory buffer.value.toString()
                }
            }
        }
        return@withMemory ""
    } ?: ""

    // 置二进制值
    fun binaryValue(key: String, value: ByteArray): Boolean = withMemory { hKey ->
        value.usePinned { pinned ->
            RegSetValueExW(hKey, key, 0U, KeyType.Binary.value, pinned.addressOf(0).reinterpret(), value.size.convert()) == ERROR_SUCCESS
        }
    } ?: false

    // 置数字值
    fun numberValue(key: String, value: ULong): Boolean = withMemory { hKey ->
        if (value > 0xFFFFFFFFUL) {
            val pValue = alloc<ULongVar>()
            pValue.value = value
            RegSetValueExW(hKey, key, 0U, KeyType._N64.value, pValue.ptr.reinterpret(), sizeOf<ULongVar>().convert()) == ERROR_SUCCESS
        }
        else {
            val pValue = alloc<UIntVar>()
            pValue.value = value.convert()
            RegSetValueExW(hKey, key, 0U, KeyType.Number.value, pValue.ptr.reinterpret(), sizeOf<UIntVar>().convert()) == ERROR_SUCCESS
        }
    } ?: false

    // 置字符串值
    fun stringValue(key: String, value: String, multiline: Boolean = false): Boolean = withMemory { hKey ->
        val size = value.length.toUInt() shl 1
        if (multiline) {
            val tmpValue = value.replace('\n', 0.toChar())
            tmpValue.usePinned { pinned ->
                RegSetValueExW(hKey, key, 0U, KeyType.MultiLineString.value, pinned.addressOf(0).reinterpret(), size) == ERROR_SUCCESS
            }
        }
        else {
            value.usePinned { pinned ->
                RegSetValueExW(hKey, key, 0U, KeyType.String.value, pinned.addressOf(0).reinterpret(), size) == ERROR_SUCCESS
            }
        }
    } ?: false

    // 删除键
    fun deleteKey(key: String): Boolean = withMemory { hKey ->
        RegDeleteValueW(hKey, key) == ERROR_SUCCESS
    } ?: false

    // 枚举键值
    val allKeys: List<Pair<String, KeyType>> get() = withMemory { hKey ->
        val container = mutableListOf<Pair<String, KeyType>>()
        val pMaxKeySize = alloc<UIntVar>()
        val pMaxValueSize = alloc<UIntVar>()
        if (RegQueryInfoKeyW(hKey, null, null, null, null, null, null, null, pMaxKeySize.ptr, pMaxValueSize.ptr, null, null) == ERROR_SUCCESS) {
            val maxKeySize = pMaxKeySize.value.toInt()
            val pType = alloc<UIntVar>()
            val buffer = allocArray<UShortVar>(maxKeySize) { value = 0U }
            while (true) {
                val result = RegEnumValueW(hKey, container.size.convert(), buffer, pMaxKeySize.ptr, null, pType.ptr, null, null)
                if (result != ERROR_SUCCESS) break
                container += buffer.toKString() to KeyType.fromUInt(pType.value)
            }
        }
        return@withMemory container
    } ?: emptyList()

    // 刷新环境变量
    companion object {
        fun flushEnvironment() = memScoped {
            val dwReturnValue = alloc<ULongVar>()
            "Environment".usePinned { pinned ->
                SendMessageTimeoutW(0xFFFFL.toCPointer(), 0x001AU, 0UL, pinned.addressOf(0).toLong(), 0x0002U, 5000U, dwReturnValue.ptr)
            }
        }
    }
}