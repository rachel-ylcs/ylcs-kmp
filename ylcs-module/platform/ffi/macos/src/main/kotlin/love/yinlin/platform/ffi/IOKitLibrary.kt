package love.yinlin.platform.ffi

@Suppress("UnusedReceiverParameter")
inline val FFI.iokit: IOKitLibrary get() = IOKitLibrary.Instance

@Suppress("PropertyName", "unused", "SpellCheckingInspection")
class IOKitLibrary private constructor() : NativeLibrary("/System/Library/Frameworks/IOKit.framework/IOKit") {
    @PublishedApi
    internal companion object {
        @PublishedApi
        internal val Instance = IOKitLibrary()

        private val byte = NativeType.Byte
        private val int32 = NativeType.Int
        private val int64 = NativeType.Long
        private val float32 = NativeType.Float
        private val float64 = NativeType.Double
        private val pointer = NativeType.Pointer
    }

    val IOAccelFindAccelerator by func { declare(int32, pointer, pointer, retType = int32) }
    val IOAllowPowerChange by func { declare(int32, int64, retType = int32) }
    val IOBSDNameMatching by func { declare(int32, int32, pointer, retType = pointer) }
    val IOCFSerialize by func { declare(pointer, int64, retType = pointer) }
    val IOCFUnserialize by func { declare(pointer, pointer, int64, pointer, retType = pointer) }
    val IOCFUnserializeBinary by func { declare(pointer, int64, pointer, int64, pointer, retType = pointer) }
    val IOCFUnserializeWithSize by func { declare(pointer, int64, pointer, int64, pointer, retType = pointer) }
    val IOCancelPowerChange by func { declare(int32, int64, retType = int32) }
    val IOCatalogueGetData by func { declare(int32, int32, pointer, pointer, retType = int32) }
    val IOCatalogueModuleLoaded by func { declare(int32, pointer, retType = int32) }
    val IOCatalogueReset by func { declare(int32, int32, retType = int32) }
    val IOCatalogueSendData by func { declare(int32, int32, pointer, int32, retType = int32) }
    val IOCatalogueTerminate by func { declare(int32, int32, pointer, retType = int32) }
    val IOConnectAddClient by func { declare(int32, int32, retType = int32) }
    val IOConnectAddRef by func { declare(int32, retType = int32) }
    val IOConnectCallAsyncMethod by func { declare(int32, int32, int32, pointer, int32, pointer, int32, pointer, int64, pointer, pointer, pointer, pointer, retType = int32) }
    val IOConnectCallAsyncScalarMethod by func { declare(int32, int32, int32, pointer, int32, pointer, int32, pointer, pointer, retType = int32) }
    val IOConnectCallAsyncStructMethod by func { declare(int32, int32, int32, pointer, int32, pointer, int64, pointer, pointer, retType = int32) }
    val IOConnectCallMethod by func { declare(int32, int32, pointer, int32, pointer, int64, pointer, pointer, pointer, pointer, retType = int32) }
    val IOConnectCallScalarMethod by func { declare(int32, int32, pointer, int32, pointer, pointer, retType = int32) }
    val IOConnectCallStructMethod by func { declare(int32, int32, pointer, int64, pointer, pointer, retType = int32) }
    val IOConnectGetService by func { declare(int32, pointer, retType = int32) }
    val IOConnectMapMemory by func { declare(int32, int32, int32, pointer, pointer, int32, retType = int32) }
    val IOConnectMapMemory64 by func { declare(int32, int32, int32, pointer, pointer, int32, retType = int32) }
    val IOConnectRelease by func { declare(int32, retType = int32) }
    val IOConnectSetCFProperties by func { declare(int32, pointer, retType = int32) }
    val IOConnectSetCFProperty by func { declare(int32, pointer, pointer, retType = int32) }
    val IOConnectSetNotificationPort by func { declare(int32, int32, int32, int64, retType = int32) }
    val IOConnectTrap0 by func { declare(int32, int32, retType = int32) }
    val IOConnectTrap1 by func { declare(int32, int32, int64, retType = int32) }
    val IOConnectTrap2 by func { declare(int32, int32, int64, int64, retType = int32) }
    val IOConnectTrap3 by func { declare(int32, int32, int64, int64, int64, retType = int32) }
    val IOConnectTrap4 by func { declare(int32, int32, int64, int64, int64, int64, retType = int32) }
    val IOConnectTrap5 by func { declare(int32, int32, int64, int64, int64, int64, int64, retType = int32) }
    val IOConnectTrap6 by func { declare(int32, int32, int64, int64, int64, int64, int64, int64, retType = int32) }
    val IOConnectUnmapMemory by func { declare(int32, int32, int32, int64, retType = int32) }
    val IOConnectUnmapMemory64 by func { declare(int32, int32, int32, int64, retType = int32) }
    val IOCopySystemLoadAdvisoryDetailed by func { declare(retType = pointer) }
    val IOCreatePlugInInterfaceForService by func { declare(int32, pointer, pointer, pointer, pointer, retType = int32) }
    val IOCreateReceivePort by func { declare(int32, pointer, retType = int32) }
    val IODataQueueAllocateNotificationPort by func { declare(retType = int32) }
    val IODataQueueDataAvailable by func { declare(pointer, retType = byte) }
    val IODataQueueDequeue by func { declare(pointer, pointer, pointer, retType = int32) }
    val IODataQueueEnqueue by func { declare(pointer, pointer, int32, retType = int32) }
    val IODataQueuePeek by func { declare(pointer, retType = pointer) }
    val IODataQueueSetNotificationPort by func { declare(pointer, int32, retType = int32) }
    val IODataQueueWaitForAvailableData by func { declare(pointer, int32, retType = int32) }
    val IODeregisterApp by func { declare(pointer, retType = int32) }
    val IODeregisterForSystemPower by func { declare(pointer, retType = int32) }
    val IODestroyPlugInInterface by func { declare(pointer, retType = int32) }
    val IODispatchCalloutFromMessage by func { declare(pointer, pointer, pointer) }
    val IODisplayCommitParameters by func { declare(int32, int32, retType = int32) }
    val IODisplayCopyFloatParameters by func { declare(int32, int32, pointer, retType = int32) }
    val IODisplayCopyParameters by func { declare(int32, int32, pointer, retType = int32) }
    val IODisplayCreateInfoDictionary by func { declare(int32, int32, retType = pointer) }
    val IODisplayForFramebuffer by func { declare(int32, int32, retType = int32) }
    val IODisplayGetFloatParameter by func { declare(int32, int32, pointer, pointer, retType = int32) }
    val IODisplayGetIntegerRangeParameter by func { declare(int32, int32, pointer, pointer, pointer, pointer, retType = int32) }
    val IODisplayMatchDictionaries by func { declare(pointer, pointer, int32, retType = int32) }
    val IODisplaySetFloatParameter by func { declare(int32, int32, pointer, float32, retType = int32) }
    val IODisplaySetIntegerParameter by func { declare(int32, int32, pointer, int32, retType = int32) }
    val IODisplaySetParameters by func { declare(int32, int32, pointer, retType = int32) }
    val IOFBCopyI2CInterfaceForBus by func { declare(int32, int32, pointer, retType = int32) }
    val IOFBGetI2CInterfaceCount by func { declare(int32, pointer, retType = int32) }
    val IOFramebufferOpen by func { declare(int32, int32, int32, pointer, retType = int32) }
    val IOGetSystemLoadAdvisory by func { declare(retType = int32) }
    val IOHIDCheckAccess by func { declare(int32, retType = int32) }
    val IOHIDCopyCFTypeParameter by func { declare(int32, pointer, pointer, retType = int32) }
    val IOHIDCreateSharedMemory by func { declare(int32, int32, retType = int32) }
    val IOHIDDeviceActivate by func { declare(pointer) }
    val IOHIDDeviceCancel by func { declare(pointer) }
    val IOHIDDeviceClose by func { declare(pointer, int32, retType = int32) }
    val IOHIDDeviceConformsTo by func { declare(pointer, int32, int32, retType = byte) }
    val IOHIDDeviceCopyMatchingElements by func { declare(pointer, pointer, int32, retType = pointer) }
    val IOHIDDeviceCopyValueMultiple by func { declare(pointer, pointer, pointer, retType = int32) }
    val IOHIDDeviceCopyValueMultipleWithCallback by func { declare(pointer, pointer, pointer, float64, pointer, pointer, retType = int32) }
    val IOHIDDeviceCreate by func { declare(pointer, int32, retType = pointer) }
    val IOHIDDeviceGetProperty by func { declare(pointer, pointer, retType = pointer) }
    val IOHIDDeviceGetReport by func { declare(pointer, int32, int64, pointer, pointer, retType = int32) }
    val IOHIDDeviceGetReportWithCallback by func { declare(pointer, int32, int64, pointer, pointer, float64, pointer, pointer, retType = int32) }
    val IOHIDDeviceGetService by func { declare(pointer, retType = int32) }
    val IOHIDDeviceGetTypeID by func { declare(retType = int64) }
    val IOHIDDeviceGetValue by func { declare(pointer, pointer, pointer, retType = int32) }
    val IOHIDDeviceGetValueWithCallback by func { declare(pointer, pointer, pointer, float64, pointer, pointer, retType = int32) }
    val IOHIDDeviceGetValueWithOptions by func { declare(pointer, pointer, pointer, int32, retType = int32) }
    val IOHIDDeviceOpen by func { declare(pointer, int32, retType = int32) }
    val IOHIDDeviceRegisterInputReportCallback by func { declare(pointer, pointer, int64, pointer, pointer) }
    val IOHIDDeviceRegisterInputReportWithTimeStampCallback by func { declare(pointer, pointer, int64, pointer, pointer) }
    val IOHIDDeviceRegisterInputValueCallback by func { declare(pointer, pointer, pointer) }
    val IOHIDDeviceRegisterRemovalCallback by func { declare(pointer, pointer, pointer) }
    val IOHIDDeviceScheduleWithRunLoop by func { declare(pointer, pointer, pointer) }
    val IOHIDDeviceSetCancelHandler by func { declare(pointer, pointer) }
    val IOHIDDeviceSetDispatchQueue by func { declare(pointer, pointer) }
    val IOHIDDeviceSetInputValueMatching by func { declare(pointer, pointer) }
    val IOHIDDeviceSetInputValueMatchingMultiple by func { declare(pointer, pointer) }
    val IOHIDDeviceSetProperty by func { declare(pointer, pointer, pointer, retType = byte) }
    val IOHIDDeviceSetReport by func { declare(pointer, int32, int64, pointer, int64, retType = int32) }
    val IOHIDDeviceSetReportWithCallback by func { declare(pointer, int32, int64, pointer, int64, float64, pointer, pointer, retType = int32) }
    val IOHIDDeviceSetValue by func { declare(pointer, pointer, pointer, retType = int32) }
    val IOHIDDeviceSetValueMultiple by func { declare(pointer, pointer, retType = int32) }
    val IOHIDDeviceSetValueMultipleWithCallback by func { declare(pointer, pointer, float64, pointer, pointer, retType = int32) }
    val IOHIDDeviceSetValueWithCallback by func { declare(pointer, pointer, pointer, float64, pointer, pointer, retType = int32) }
    val IOHIDDeviceUnscheduleFromRunLoop by func { declare(pointer, pointer, pointer) }
    val IOHIDElementAttach by func { declare(pointer, pointer) }
    val IOHIDElementCopyAttached by func { declare(pointer, retType = pointer) }
    val IOHIDElementCreateWithDictionary by func { declare(pointer, pointer, retType = pointer) }
    val IOHIDElementDetach by func { declare(pointer, pointer) }
    val IOHIDElementGetChildren by func { declare(pointer, retType = pointer) }
    val IOHIDElementGetCollectionType by func { declare(pointer, retType = int32) }
    val IOHIDElementGetCookie by func { declare(pointer, retType = int32) }
    val IOHIDElementGetDevice by func { declare(pointer, retType = pointer) }
    val IOHIDElementGetLogicalMax by func { declare(pointer, retType = int64) }
    val IOHIDElementGetLogicalMin by func { declare(pointer, retType = int64) }
    val IOHIDElementGetName by func { declare(pointer, retType = pointer) }
    val IOHIDElementGetParent by func { declare(pointer, retType = pointer) }
    val IOHIDElementGetPhysicalMax by func { declare(pointer, retType = int64) }
    val IOHIDElementGetPhysicalMin by func { declare(pointer, retType = int64) }
    val IOHIDElementGetProperty by func { declare(pointer, pointer, retType = pointer) }
    val IOHIDElementGetReportCount by func { declare(pointer, retType = int32) }
    val IOHIDElementGetReportID by func { declare(pointer, retType = int32) }
    val IOHIDElementGetReportSize by func { declare(pointer, retType = int32) }
    val IOHIDElementGetType by func { declare(pointer, retType = int32) }
    val IOHIDElementGetTypeID by func { declare(retType = int64) }
    val IOHIDElementGetUnit by func { declare(pointer, retType = int32) }
    val IOHIDElementGetUnitExponent by func { declare(pointer, retType = int32) }
    val IOHIDElementGetUsage by func { declare(pointer, retType = int32) }
    val IOHIDElementGetUsagePage by func { declare(pointer, retType = int32) }
    val IOHIDElementHasNullState by func { declare(pointer, retType = byte) }
    val IOHIDElementHasPreferredState by func { declare(pointer, retType = byte) }
    val IOHIDElementIsArray by func { declare(pointer, retType = byte) }
    val IOHIDElementIsNonLinear by func { declare(pointer, retType = byte) }
    val IOHIDElementIsRelative by func { declare(pointer, retType = byte) }
    val IOHIDElementIsVirtual by func { declare(pointer, retType = byte) }
    val IOHIDElementIsWrapping by func { declare(pointer, retType = byte) }
    val IOHIDElementSetProperty by func { declare(pointer, pointer, pointer, retType = byte) }
    val IOHIDEventSystemClientCopyProperty by func { declare(pointer, pointer, retType = pointer) }
    val IOHIDEventSystemClientCopyServices by func { declare(pointer, retType = pointer) }
    val IOHIDEventSystemClientCreateSimpleClient by func { declare(pointer, retType = pointer) }
    val IOHIDEventSystemClientGetTypeID by func { declare(retType = int64) }
    val IOHIDEventSystemClientSetProperty by func { declare(pointer, pointer, pointer, retType = byte) }
    val IOHIDGetAccelerationWithKey by func { declare(int32, pointer, pointer, retType = int32) }
    val IOHIDGetActivityState by func { declare(int32, pointer, retType = int32) }
    val IOHIDGetButtonEventNum by func { declare(int32, int32, pointer, retType = int32) }
    val IOHIDGetModifierLockState by func { declare(int32, int32, pointer, retType = int32) }
    val IOHIDGetMouseAcceleration by func { declare(int32, pointer, retType = int32) }
    val IOHIDGetMouseButtonMode by func { declare(int32, pointer, retType = int32) }
    val IOHIDGetParameter by func { declare(int32, pointer, int64, pointer, pointer, retType = int32) }
    val IOHIDGetScrollAcceleration by func { declare(int32, pointer, retType = int32) }
    val IOHIDGetStateForSelector by func { declare(int32, int32, pointer, retType = int32) }
    val IOHIDManagerActivate by func { declare(pointer) }
    val IOHIDManagerCancel by func { declare(pointer) }
    val IOHIDManagerClose by func { declare(pointer, int32, retType = int32) }
    val IOHIDManagerCopyDevices by func { declare(pointer, retType = pointer) }
    val IOHIDManagerCreate by func { declare(pointer, int32, retType = pointer) }
    val IOHIDManagerGetProperty by func { declare(pointer, pointer, retType = pointer) }
    val IOHIDManagerGetTypeID by func { declare(retType = int64) }
    val IOHIDManagerOpen by func { declare(pointer, int32, retType = int32) }
    val IOHIDManagerRegisterDeviceMatchingCallback by func { declare(pointer, pointer, pointer) }
    val IOHIDManagerRegisterDeviceRemovalCallback by func { declare(pointer, pointer, pointer) }
    val IOHIDManagerRegisterInputReportCallback by func { declare(pointer, pointer, pointer) }
    val IOHIDManagerRegisterInputReportWithTimeStampCallback by func { declare(pointer, pointer, pointer) }
    val IOHIDManagerRegisterInputValueCallback by func { declare(pointer, pointer, pointer) }
    val IOHIDManagerSaveToPropertyDomain by func { declare(pointer, pointer, pointer, pointer, int32) }
    val IOHIDManagerScheduleWithRunLoop by func { declare(pointer, pointer, pointer) }
    val IOHIDManagerSetCancelHandler by func { declare(pointer, pointer) }
    val IOHIDManagerSetDeviceMatching by func { declare(pointer, pointer) }
    val IOHIDManagerSetDeviceMatchingMultiple by func { declare(pointer, pointer) }
    val IOHIDManagerSetDispatchQueue by func { declare(pointer, pointer) }
    val IOHIDManagerSetInputValueMatching by func { declare(pointer, pointer) }
    val IOHIDManagerSetInputValueMatchingMultiple by func { declare(pointer, pointer) }
    val IOHIDManagerSetProperty by func { declare(pointer, pointer, pointer, retType = byte) }
    val IOHIDManagerUnscheduleFromRunLoop by func { declare(pointer, pointer, pointer) }
    val IOHIDPostEvent by func { declare(int32, int32, MacOSDef.IOGPoint, pointer, int32, int32, int32, retType = int32) }
    val IOHIDQueueActivate by func { declare(pointer) }
    val IOHIDQueueAddElement by func { declare(pointer, pointer) }
    val IOHIDQueueCancel by func { declare(pointer) }
    val IOHIDQueueContainsElement by func { declare(pointer, pointer, retType = byte) }
    val IOHIDQueueCopyNextValue by func { declare(pointer, retType = pointer) }
    val IOHIDQueueCopyNextValueWithTimeout by func { declare(pointer, float64, retType = pointer) }
    val IOHIDQueueCreate by func { declare(pointer, pointer, int64, int32, retType = pointer) }
    val IOHIDQueueGetDepth by func { declare(pointer, retType = int64) }
    val IOHIDQueueGetDevice by func { declare(pointer, retType = pointer) }
    val IOHIDQueueGetTypeID by func { declare(retType = int64) }
    val IOHIDQueueRegisterValueAvailableCallback by func { declare(pointer, pointer, pointer) }
    val IOHIDQueueRemoveElement by func { declare(pointer, pointer) }
    val IOHIDQueueScheduleWithRunLoop by func { declare(pointer, pointer, pointer) }
    val IOHIDQueueSetCancelHandler by func { declare(pointer, pointer) }
    val IOHIDQueueSetDepth by func { declare(pointer, int64) }
    val IOHIDQueueSetDispatchQueue by func { declare(pointer, pointer) }
    val IOHIDQueueStart by func { declare(pointer) }
    val IOHIDQueueStop by func { declare(pointer) }
    val IOHIDQueueUnscheduleFromRunLoop by func { declare(pointer, pointer, pointer) }
    val IOHIDRegisterVirtualDisplay by func { declare(int32, pointer, retType = int32) }
    val IOHIDRequestAccess by func { declare(int32, retType = byte) }
    val IOHIDServiceClientConformsTo by func { declare(pointer, int32, int32, retType = int32) }
    val IOHIDServiceClientCopyProperty by func { declare(pointer, pointer, retType = pointer) }
    val IOHIDServiceClientGetRegistryID by func { declare(pointer, retType = pointer) }
    val IOHIDServiceClientGetTypeID by func { declare(retType = int64) }
    val IOHIDServiceClientSetProperty by func { declare(pointer, pointer, pointer, retType = byte) }
    val IOHIDSetAccelerationWithKey by func { declare(int32, pointer, float64, retType = int32) }
    val IOHIDSetCFTypeParameter by func { declare(int32, pointer, pointer, retType = int32) }
    val IOHIDSetCursorEnable by func { declare(int32, int32, retType = int32) }
    val IOHIDSetEventsEnable by func { declare(int32, int32, retType = int32) }
    val IOHIDSetModifierLockState by func { declare(int32, int32, byte, retType = int32) }
    val IOHIDSetMouseAcceleration by func { declare(int32, float64, retType = int32) }
    val IOHIDSetMouseButtonMode by func { declare(int32, int32, retType = int32) }
    val IOHIDSetMouseLocation by func { declare(int32, int32, int32, retType = int32) }
    val IOHIDSetParameter by func { declare(int32, pointer, pointer, int64, retType = int32) }
    val IOHIDSetScrollAcceleration by func { declare(int32, float64, retType = int32) }
    val IOHIDSetStateForSelector by func { declare(int32, int32, int32, retType = int32) }
    val IOHIDSetVirtualDisplayBounds by func { declare(int32, int32, pointer, retType = int32) }
    val IOHIDTransactionAddElement by func { declare(pointer, pointer) }
    val IOHIDTransactionClear by func { declare(pointer) }
    val IOHIDTransactionCommit by func { declare(pointer, retType = int32) }
    val IOHIDTransactionCommitWithCallback by func { declare(pointer, float64, pointer, pointer, retType = int32) }
    val IOHIDTransactionContainsElement by func { declare(pointer, pointer, retType = byte) }
    val IOHIDTransactionCreate by func { declare(pointer, pointer, int32, int32, retType = pointer) }
    val IOHIDTransactionGetDevice by func { declare(pointer, retType = pointer) }
    val IOHIDTransactionGetDirection by func { declare(pointer, retType = int32) }
    val IOHIDTransactionGetTypeID by func { declare(retType = int64) }
    val IOHIDTransactionGetValue by func { declare(pointer, pointer, int32, retType = pointer) }
    val IOHIDTransactionRemoveElement by func { declare(pointer, pointer) }
    val IOHIDTransactionScheduleWithRunLoop by func { declare(pointer, pointer, pointer) }
    val IOHIDTransactionSetDirection by func { declare(pointer, int32) }
    val IOHIDTransactionSetValue by func { declare(pointer, pointer, pointer, int32) }
    val IOHIDTransactionUnscheduleFromRunLoop by func { declare(pointer, pointer, pointer) }
    val IOHIDUnregisterVirtualDisplay by func { declare(int32, int32, retType = int32) }
    val IOHIDUserDeviceActivate by func { declare(pointer) }
    val IOHIDUserDeviceCancel by func { declare(pointer) }
    val IOHIDUserDeviceCopyProperty by func { declare(pointer, pointer, retType = pointer) }
    val IOHIDUserDeviceCreateWithProperties by func { declare(pointer, pointer, int32, retType = pointer) }
    val IOHIDUserDeviceGetTypeID by func { declare(retType = int64) }
    val IOHIDUserDeviceHandleReportWithTimeStamp by func { declare(pointer, int64, pointer, int64, retType = int32) }
    val IOHIDUserDeviceRegisterGetReportBlock by func { declare(pointer, pointer) }
    val IOHIDUserDeviceRegisterSetReportBlock by func { declare(pointer, pointer) }
    val IOHIDUserDeviceSetCancelHandler by func { declare(pointer, pointer) }
    val IOHIDUserDeviceSetDispatchQueue by func { declare(pointer, pointer) }
    val IOHIDUserDeviceSetProperty by func { declare(pointer, pointer, pointer, retType = byte) }
    val IOHIDValueCreateWithBytes by func { declare(pointer, pointer, int64, pointer, int64, retType = pointer) }
    val IOHIDValueCreateWithBytesNoCopy by func { declare(pointer, pointer, int64, pointer, int64, retType = pointer) }
    val IOHIDValueCreateWithIntegerValue by func { declare(pointer, pointer, int64, int64, retType = pointer) }
    val IOHIDValueGetBytePtr by func { declare(pointer, retType = pointer) }
    val IOHIDValueGetElement by func { declare(pointer, retType = pointer) }
    val IOHIDValueGetIntegerValue by func { declare(pointer, retType = int64) }
    val IOHIDValueGetLength by func { declare(pointer, retType = int64) }
    val IOHIDValueGetScaledValue by func { declare(pointer, int32, retType = float64) }
    val IOHIDValueGetTimeStamp by func { declare(pointer, retType = int64) }
    val IOHIDValueGetTypeID by func { declare(retType = int64) }
    val IOI2CCopyInterfaceForID by func { declare(pointer, pointer, retType = int32) }
    val IOI2CInterfaceClose by func { declare(pointer, int32, retType = int32) }
    val IOI2CInterfaceOpen by func { declare(int32, int32, pointer, retType = int32) }
    val IOI2CSendRequest by func { declare(pointer, int32, pointer, retType = int32) }
    val IOIteratorIsValid by func { declare(int32, retType = int32) }
    val IOIteratorNext by func { declare(int32, retType = int32) }
    val IOIteratorReset by func { declare(int32) }
    val IOKitGetBusyState by func { declare(int32, pointer, retType = int32) }
    val IOKitWaitQuiet by func { declare(int32, pointer, retType = int32) }
    val IOMasterPort by func { declare(int32, pointer, retType = int32) }
    val IONetworkClose by func { declare(int32, retType = int32) }
    val IONetworkGetDataCapacity by func { declare(int32, int32, pointer, retType = int32) }
    val IONetworkGetDataHandle by func { declare(int32, pointer, pointer, retType = int32) }
    val IONetworkGetPacketFiltersMask by func { declare(int32, pointer, pointer, int32, retType = int32) }
    val IONetworkOpen by func { declare(int32, pointer, retType = int32) }
    val IONetworkReadData by func { declare(int32, int32, pointer, pointer, retType = int32) }
    val IONetworkResetData by func { declare(int32, int32, retType = int32) }
    val IONetworkSetPacketFiltersMask by func { declare(int32, pointer, int32, int32, retType = int32) }
    val IONetworkWriteData by func { declare(int32, int32, pointer, int32, retType = int32) }
    val IONotificationPortCreate by func { declare(int32, retType = pointer) }
    val IONotificationPortDestroy by func { declare(pointer) }
    val IONotificationPortGetMachPort by func { declare(pointer, retType = int32) }
    val IONotificationPortGetRunLoopSource by func { declare(pointer, retType = pointer) }
    val IONotificationPortSetDispatchQueue by func { declare(pointer, pointer) }
    val IONotificationPortSetImportanceReceiver by func { declare(pointer, retType = int32) }
    val IOObjectConformsTo by func { declare(int32, pointer, retType = int32) }
    val IOObjectCopyBundleIdentifierForClass by func { declare(pointer, retType = pointer) }
    val IOObjectCopyClass by func { declare(int32, retType = pointer) }
    val IOObjectCopySuperclassForClass by func { declare(pointer, retType = pointer) }
    val IOObjectGetClass by func { declare(int32, pointer, retType = int32) }
    val IOObjectGetKernelRetainCount by func { declare(int32, retType = int32) }
    val IOObjectGetRetainCount by func { declare(int32, retType = int32) }
    val IOObjectGetUserRetainCount by func { declare(int32, retType = int32) }
    val IOObjectIsEqualTo by func { declare(int32, int32, retType = int32) }
    val IOObjectRelease by func { declare(int32, retType = int32) }
    val IOObjectRetain by func { declare(int32, retType = int32) }
    val IOOpenFirmwarePathMatching by func { declare(int32, int32, pointer, retType = pointer) }
    val IOPMAssertionCopyProperties by func { declare(int32, retType = pointer) }
    val IOPMAssertionCreate by func { declare(pointer, int32, pointer, retType = int32) }
    val IOPMAssertionCreateWithDescription by func { declare(pointer, pointer, pointer, pointer, pointer, float64, pointer, pointer, retType = int32) }
    val IOPMAssertionCreateWithName by func { declare(pointer, int32, pointer, pointer, retType = int32) }
    val IOPMAssertionCreateWithProperties by func { declare(pointer, pointer, retType = int32) }
    val IOPMAssertionDeclareUserActivity by func { declare(pointer, int32, pointer, retType = int32) }
    val IOPMAssertionRelease by func { declare(int32, retType = int32) }
    val IOPMAssertionRetain by func { declare(int32) }
    val IOPMAssertionSetProperty by func { declare(int32, pointer, pointer, retType = int32) }
    val IOPMCancelScheduledPowerEvent by func { declare(pointer, pointer, pointer, retType = int32) }
    val IOPMCopyAssertionsByProcess by func { declare(pointer, retType = int32) }
    val IOPMCopyAssertionsStatus by func { declare(pointer, retType = int32) }
    val IOPMCopyBatteryInfo by func { declare(int32, pointer, retType = int32) }
    val IOPMCopyCPUPowerStatus by func { declare(pointer, retType = int32) }
    val IOPMCopyScheduledPowerEvents by func { declare(retType = pointer) }
    val IOPMDeclareNetworkClientActivity by func { declare(pointer, pointer, retType = int32) }
    val IOPMFindPowerManagement by func { declare(int32, retType = int32) }
    val IOPMGetAggressiveness by func { declare(int32, int64, pointer, retType = int32) }
    val IOPMGetThermalWarningLevel by func { declare(pointer, retType = int32) }
    val IOPMSchedulePowerEvent by func { declare(pointer, pointer, pointer, retType = int32) }
    val IOPMSetAggressiveness by func { declare(int32, int64, int64, retType = int32) }
    val IOPMSleepEnabled by func { declare(retType = int32) }
    val IOPMSleepSystem by func { declare(int32, retType = int32) }
    val IOPSCopyExternalPowerAdapterDetails by func { declare(retType = pointer) }
    val IOPSCopyPowerSourcesInfo by func { declare(retType = pointer) }
    val IOPSCopyPowerSourcesList by func { declare(pointer, retType = pointer) }
    val IOPSCreateLimitedPowerNotification by func { declare(pointer, pointer, retType = pointer) }
    val IOPSGetBatteryWarningLevel by func { declare(retType = int32) }
    val IOPSGetPowerSourceDescription by func { declare(pointer, pointer, retType = pointer) }
    val IOPSGetProvidingPowerSourceType by func { declare(pointer, retType = pointer) }
    val IOPSGetTimeRemainingEstimate by func { declare(retType = float64) }
    val IOPSNotificationCreateRunLoopSource by func { declare(pointer, pointer, retType = pointer) }
    val IORegisterApp by func { declare(pointer, int32, pointer, pointer, pointer, retType = int32) }
    val IORegisterForSystemPower by func { declare(pointer, pointer, pointer, pointer, retType = int32) }
    val IORegistryCreateIterator by func { declare(int32, pointer, int32, pointer, retType = int32) }
    val IORegistryEntryCopyFromPath by func { declare(int32, pointer, retType = int32) }
    val IORegistryEntryCopyPath by func { declare(int32, pointer, retType = pointer) }
    val IORegistryEntryCreateCFProperties by func { declare(int32, pointer, pointer, int32, retType = int32) }
    val IORegistryEntryCreateCFProperty by func { declare(int32, pointer, pointer, int32, retType = pointer) }
    val IORegistryEntryCreateIterator by func { declare(int32, pointer, int32, pointer, retType = int32) }
    val IORegistryEntryFromPath by func { declare(int32, pointer, retType = int32) }
    val IORegistryEntryGetChildEntry by func { declare(int32, pointer, pointer, retType = int32) }
    val IORegistryEntryGetChildIterator by func { declare(int32, pointer, pointer, retType = int32) }
    val IORegistryEntryGetLocationInPlane by func { declare(int32, pointer, pointer, retType = int32) }
    val IORegistryEntryGetName by func { declare(int32, pointer, retType = int32) }
    val IORegistryEntryGetNameInPlane by func { declare(int32, pointer, pointer, retType = int32) }
    val IORegistryEntryGetParentEntry by func { declare(int32, pointer, pointer, retType = int32) }
    val IORegistryEntryGetParentIterator by func { declare(int32, pointer, pointer, retType = int32) }
    val IORegistryEntryGetPath by func { declare(int32, pointer, pointer, retType = int32) }
    val IORegistryEntryGetProperty by func { declare(int32, pointer, pointer, pointer, retType = int32) }
    val IORegistryEntryGetRegistryEntryID by func { declare(int32, pointer, retType = int32) }
    val IORegistryEntryIDMatching by func { declare(int64, retType = pointer) }
    val IORegistryEntryInPlane by func { declare(int32, pointer, retType = int32) }
    val IORegistryEntrySearchCFProperty by func { declare(int32, pointer, pointer, pointer, int32, retType = pointer) }
    val IORegistryEntrySetCFProperties by func { declare(int32, pointer, retType = int32) }
    val IORegistryEntrySetCFProperty by func { declare(int32, pointer, pointer, retType = int32) }
    val IORegistryGetRootEntry by func { declare(int32, retType = int32) }
    val IORegistryIteratorEnterEntry by func { declare(int32, retType = int32) }
    val IORegistryIteratorExitEntry by func { declare(int32, retType = int32) }
    val IOServiceAddInterestNotification by func { declare(pointer, int32, pointer, pointer, pointer, pointer, retType = int32) }
    val IOServiceAddMatchingNotification by func { declare(pointer, pointer, pointer, pointer, pointer, pointer, retType = int32) }
    val IOServiceAddNotification by func { declare(int32, pointer, pointer, int32, int64, pointer, retType = int32) }
    val IOServiceAuthorize by func { declare(int32, int32, retType = int32) }
    val IOServiceClose by func { declare(int32, retType = int32) }
    val IOServiceGetBusyState by func { declare(int32, pointer, retType = int32) }
    val IOServiceGetMatchingService by func { declare(int32, pointer, retType = int32) }
    val IOServiceGetMatchingServices by func { declare(int32, pointer, pointer, retType = int32) }
    val IOServiceMatchPropertyTable by func { declare(int32, pointer, pointer, retType = int32) }
    val IOServiceMatching by func { declare(pointer, retType = pointer) }
    val IOServiceNameMatching by func { declare(pointer, retType = pointer) }
    val IOServiceOFPathToBSDName by func { declare(int32, pointer, pointer, retType = int32) }
    val IOServiceOpen by func { declare(int32, int32, int32, pointer, retType = int32) }
    val IOServiceOpenAsFileDescriptor by func { declare(int32, int32, retType = int32) }
    val IOServiceRequestProbe by func { declare(int32, int32, retType = int32) }
    val IOServiceWaitQuiet by func { declare(int32, pointer, retType = int32) }
    val IOURLCreateDataAndPropertiesFromResource by func { declare(pointer, pointer, pointer, pointer, pointer, pointer, retType = byte) }
    val IOURLCreatePropertyFromResource by func { declare(pointer, pointer, pointer, pointer, retType = pointer) }
    val KextManagerCopyLoadedKextInfo by func { declare(pointer, pointer, retType = pointer) }
    val KextManagerCreateURLForBundleIdentifier by func { declare(pointer, pointer, retType = pointer) }
    val KextManagerLoadKextWithIdentifier by func { declare(pointer, pointer, retType = int32) }
    val KextManagerLoadKextWithURL by func { declare(pointer, pointer, retType = int32) }
    val KextManagerUnloadKextWithIdentifier by func { declare(pointer, retType = int32) }
    val NXClickTime by func { declare(int32, retType = float64) }
    val NXCloseEventStatus by func { declare(int32) }
    val NXEventSystemInfo by func { declare(int32, pointer, pointer, pointer, retType = pointer) }
    val NXGetClickSpace by func { declare(int32, pointer) }
    val NXKeyRepeatInterval by func { declare(int32, retType = float64) }
    val NXKeyRepeatThreshold by func { declare(int32, retType = float64) }
    val NXOpenEventStatus by func { declare(retType = int32) }
    val NXResetKeyboard by func { declare(int32) }
    val NXResetMouse by func { declare(int32) }
    val NXSetClickSpace by func { declare(int32, pointer) }
    val NXSetClickTime by func { declare(int32, float64) }
    val NXSetKeyRepeatInterval by func { declare(int32, float64) }
    val NXSetKeyRepeatThreshold by func { declare(int32, float64) }
    val OSGetNotificationFromMessage by func { declare(pointer, int32, pointer, pointer, pointer, pointer, retType = int32) }
}
