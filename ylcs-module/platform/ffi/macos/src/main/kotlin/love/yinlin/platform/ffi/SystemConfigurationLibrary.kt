package love.yinlin.platform.ffi

@Suppress("UnusedReceiverParameter")
inline val FFI.systemConfiguration: SystemConfigurationLibrary get() = SystemConfigurationLibrary.Instance

@Suppress("PropertyName", "unused", "SpellCheckingInspection")
class SystemConfigurationLibrary private constructor() : NativeLibrary("/System/Library/Frameworks/SystemConfiguration.framework/SystemConfiguration") {
    @PublishedApi
    internal companion object {
        @PublishedApi
        internal val Instance = SystemConfigurationLibrary()

        private val pointer = NativeType.Pointer
        private val byte = NativeType.Byte
        private val int32 = NativeType.Int
        private val int64 = NativeType.Long
    }

    val SCCopyLastError by func { declare(retType = pointer) }
    val SCDynamicStoreCopyComputerName by func { declare(pointer, pointer, retType = pointer) }
    val SCDynamicStoreCopyConsoleUser by func { declare(pointer, pointer, pointer, retType = pointer) }
    val SCDynamicStoreCopyKeyList by func { declare(pointer, pointer, retType = pointer) }
    val SCDynamicStoreCopyLocalHostName by func { declare(pointer, retType = pointer) }
    val SCDynamicStoreCopyMultiple by func { declare(pointer, pointer, pointer, retType = pointer) }
    val SCDynamicStoreCopyNotifiedKeys by func { declare(pointer, retType = pointer) }
    val SCDynamicStoreCopyProxies by func { declare(pointer, retType = pointer) }
    val SCDynamicStoreCopyValue by func { declare(pointer, pointer, retType = pointer) }
    val SCDynamicStoreCreate by func { declare(pointer, pointer, pointer, pointer, retType = pointer) }
    val SCDynamicStoreCreateRunLoopSource by func { declare(pointer, pointer, int64, retType = pointer) }
    val SCDynamicStoreGetTypeID by func { declare(retType = int64) }
    val SCDynamicStoreKeyCreateNetworkGlobalEntity by func { declare(pointer, pointer, pointer, retType = pointer) }
    val SCDynamicStoreKeyCreateNetworkInterfaceEntity by func { declare(pointer, pointer, pointer, pointer, retType = pointer) }
    val SCDynamicStoreKeyCreateNetworkServiceEntity by func { declare(pointer, pointer, pointer, pointer, retType = pointer) }
    val SCDynamicStoreSetDispatchQueue by func { declare(pointer, pointer, retType = byte) }
    val SCDynamicStoreSetNotificationKeys by func { declare(pointer, pointer, pointer, retType = byte) }
    val SCError by func { declare(retType = int32) }
    val SCErrorString by func { declare(int32, retType = pointer) }
    val SCNetworkInterfaceCopyAll by func { declare(retType = pointer) }
    val SCNetworkInterfaceGetBSDName by func { declare(pointer, retType = pointer) }
    val SCNetworkInterfaceGetHardwareAddressString by func { declare(pointer, retType = pointer) }
    val SCNetworkInterfaceGetInterface by func { declare(pointer, retType = pointer) }
    val SCNetworkInterfaceGetInterfaceType by func { declare(pointer, retType = pointer) }
    val SCNetworkInterfaceGetLocalizedDisplayName by func { declare(pointer, retType = pointer) }
    val SCNetworkInterfaceGetSupportedProtocolTypes by func { declare(pointer, retType = pointer) }
    val SCNetworkInterfaceGetTypeID by func { declare(retType = int64) }
    val SCNetworkServiceCopyAll by func { declare(pointer, retType = pointer) }
    val SCNetworkServiceGetEnabled by func { declare(pointer, retType = byte) }
    val SCNetworkServiceGetInterface by func { declare(pointer, retType = pointer) }
    val SCNetworkServiceGetName by func { declare(pointer, retType = pointer) }
    val SCNetworkServiceGetServiceID by func { declare(pointer, retType = pointer) }
    val SCNetworkSetCopyCurrent by func { declare(pointer, retType = pointer) }
    val SCNetworkSetCopyServices by func { declare(pointer, retType = pointer) }
    val SCNetworkSetGetName by func { declare(pointer, retType = pointer) }
    val SCPreferencesApplyChanges by func { declare(pointer, retType = byte) }
    val SCPreferencesCommitChanges by func { declare(pointer, retType = byte) }
    val SCPreferencesCopyKeyList by func { declare(pointer, retType = pointer) }
    val SCPreferencesCreate by func { declare(pointer, pointer, pointer, retType = pointer) }
    val SCPreferencesGetTypeID by func { declare(retType = int64) }
    val SCPreferencesGetValue by func { declare(pointer, pointer, retType = pointer) }
    val SCPreferencesRemoveValue by func { declare(pointer, pointer, retType = byte) }
    val SCPreferencesSetValue by func { declare(pointer, pointer, pointer, retType = byte) }
    val SCPreferencesSynchronize by func { declare(pointer) }
}
