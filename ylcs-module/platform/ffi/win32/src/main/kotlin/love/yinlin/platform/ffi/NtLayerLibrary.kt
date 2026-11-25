package love.yinlin.platform.ffi

@Suppress("UnusedReceiverParameter")
inline val FFI.ntdll: NtLayerLibrary get() = NtLayerLibrary.Instance

@Suppress("PropertyName", "unused", "SpellCheckingInspection")
class NtLayerLibrary private constructor() : NativeLibrary("ntdll") {
    @PublishedApi
    internal companion object {
        @PublishedApi
        internal val Instance = NtLayerLibrary()

        private val pointer = NativeType.Pointer
        private val handle = Win32Type.HANDLE
        private val byte = NativeType.Byte
        private val int16 = NativeType.Short
        private val int32 = NativeType.Int
        private val int64 = NativeType.Long
        private val float64 = NativeType.Double
    }

    val EtwEventActivityIdControl by func { declare(int32, pointer, retType = int32) }
    val EtwEventEnabled by func { declare(int64, pointer, retType = byte) }
    val EtwEventProviderEnabled by func { declare(int64, byte, int64, retType = byte) }
    val EtwEventRegister by func { declare(pointer, pointer, pointer, pointer, retType = int32) }
    val EtwEventSetInformation by func { declare(int64, int32, pointer, int32, retType = int32) }
    val EtwEventUnregister by func { declare(int64, retType = int32) }
    val EtwEventWrite by func { declare(int64, pointer, int32, pointer, retType = int32) }
    val EtwEventWriteEx by func { declare(int64, pointer, int64, int32, pointer, pointer, int32, pointer, retType = int32) }
    val EtwEventWriteFull by func { declare(int64, pointer, int16, pointer, pointer, int32, pointer, retType = int32) }
    val EtwEventWriteNoRegistration by func { declare(pointer, pointer, int32, pointer, retType = int32) }
    val EtwEventWriteString by func { declare(int64, byte, int64, pointer, retType = int32) }
    val EtwEventWriteTransfer by func { declare(int64, pointer, pointer, pointer, int32, pointer, retType = int32) }

    val LdrAccessResource by func { declare(pointer, pointer, pointer, pointer, retType = int32) }
    val LdrAddDllDirectory by func { declare(pointer, pointer, retType = int32) }
    val LdrAddRefDll by func { declare(int32, pointer, retType = int32) }
    val LdrDisableThreadCalloutsForDll by func { declare(pointer, retType = int32) }
    val LdrEnumResources by func { declare(pointer, pointer, int32, pointer, pointer, retType = int32) }
    val LdrFindEntryForAddress by func { declare(pointer, pointer, retType = int32) }
    val LdrFindResourceDirectory_U by func { declare(pointer, pointer, int32, pointer, retType = int32) }
    val LdrFindResourceEx_U by func { declare(int32, pointer, pointer, int32, pointer, retType = int32) }
    val LdrFindResource_U by func { declare(pointer, pointer, int32, pointer, retType = int32) }
    val LdrGetDllDirectory by func { declare(pointer, retType = int32) }
    val LdrGetDllFullName by func { declare(pointer, pointer, retType = int32) }
    val LdrGetDllHandle by func { declare(pointer, pointer, pointer, pointer, retType = int32) }
    val LdrGetDllHandleEx by func { declare(int32, pointer, pointer, pointer, pointer, retType = int32) }
    val LdrGetDllPath by func { declare(pointer, int32, pointer, pointer, retType = int32) }
    val LdrGetProcedureAddress by func { declare(pointer, pointer, int32, pointer, retType = int32) }
    val LdrLoadDll by func { declare(pointer, pointer, pointer, pointer, retType = int32) }
    val LdrLockLoaderLock by func { declare(int32, pointer, pointer, retType = int32) }
    val LdrQueryProcessModuleInformation by func { declare(pointer, int32, pointer, retType = int32) }
    val LdrRegisterDllNotification by func { declare(int32, pointer, pointer, pointer, retType = int32) }
    val LdrRemoveDllDirectory by func { declare(pointer, retType = int32) }
    val LdrSetDefaultDllDirectories by func { declare(int32, retType = int32) }
    val LdrSetDllDirectory by func { declare(pointer, retType = int32) }
    val LdrStandardizeSystemPath by func { declare(pointer, retType = pointer) }
    val LdrUnloadDll by func { declare(pointer, retType = int32) }
    val LdrUnlockLoaderLock by func { declare(int32, pointer, retType = int32) }
    val LdrUnregisterDllNotification by func { declare(pointer, retType = int32) }

    val NtAcceptConnectPort by func { declare(pointer, pointer, pointer, byte, pointer, pointer, retType = int32) }
    val NtAccessCheck by func { declare(pointer, handle, int32, pointer, pointer, pointer, pointer, pointer, retType = int32) }
    val NtAccessCheckAndAuditAlarm by func { declare(pointer, pointer, pointer, pointer, pointer, int32, pointer, byte, pointer, pointer, pointer, retType = int32) }
    val NtAccessCheckByType by func { declare(pointer, pointer, handle, int32, pointer, int32, pointer, pointer, pointer, pointer, pointer, retType = int32) }
    val NtAccessCheckByTypeAndAuditAlarm by func { declare(pointer, pointer, pointer, pointer, pointer, pointer, int32, int32, int32, pointer, int32, pointer, byte, pointer, pointer, pointer, retType = int32) }
    val NtAccessCheckByTypeResultList by func { declare(pointer, pointer, handle, int32, pointer, int32, pointer, pointer, pointer, pointer, pointer, retType = int32) }
    val NtAccessCheckByTypeResultListAndAuditAlarm by func { declare(pointer, pointer, pointer, pointer, pointer, pointer, int32, int32, int32, pointer, int32, pointer, byte, pointer, pointer, pointer, retType = int32) }
    val NtAccessCheckByTypeResultListAndAuditAlarmByHandle by func { declare(pointer, pointer, handle, pointer, pointer, pointer, pointer, int32, int32, int32, pointer, int32, pointer, byte, pointer, pointer, pointer, retType = int32) }
    val NtAddAtom by func { declare(pointer, int32, pointer, retType = int32) }
    val NtAdjustGroupsToken by func { declare(handle, byte, pointer, int32, pointer, pointer, retType = int32) }
    val NtAdjustPrivilegesToken by func { declare(handle, byte, pointer, int32, pointer, pointer, retType = int32) }
    val NtAdjustTokenClaimsAndDeviceGroups by func { declare(handle, byte, byte, byte, pointer, pointer, pointer, int32, pointer, int32, pointer, int32, pointer, pointer, pointer, pointer, retType = int32) }
    val NtAlertResumeThread by func { declare(handle, pointer, retType = int32) }
    val NtAlertThread by func { declare(handle, retType = int32) }
    val NtAlertThreadByThreadId by func { declare(handle, retType = int32) }
    val NtAllocateLocallyUniqueId by func { declare(pointer, retType = int32) }
    val NtAllocateReserveObject by func { declare(pointer, pointer, int32, retType = int32) }
    val NtAllocateUserPhysicalPages by func { declare(handle, pointer, pointer, retType = int32) }
    val NtAllocateUserPhysicalPagesEx by func { declare(handle, pointer, pointer, pointer, int32, retType = int32) }
    val NtAllocateUuids by func { declare(pointer, pointer, pointer, pointer, retType = int32) }
    val NtAllocateVirtualMemory by func { declare(handle, pointer, int64, pointer, int32, int32, retType = int32) }
    val NtAllocateVirtualMemoryEx by func { declare(handle, pointer, pointer, int32, int32, pointer, int32, retType = int32) }
    val NtAreMappedFilesTheSame by func { declare(pointer, pointer, retType = int32) }
    val NtAssignProcessToJobObject by func { declare(handle, handle, retType = int32) }
    val NtAssociateWaitCompletionPacket by func { declare(handle, handle, handle, pointer, pointer, int32, int64, pointer, retType = int32) }
    val NtCallbackReturn by func { declare(pointer, int32, int32, retType = int32) }
    val NtCancelIoFile by func { declare(handle, pointer, retType = int32) }
    val NtCancelIoFileEx by func { declare(handle, pointer, pointer, retType = int32) }
    val NtCancelSynchronousIoFile by func { declare(handle, pointer, pointer, retType = int32) }
    val NtCancelTimer by func { declare(handle, pointer, retType = int32) }
    val NtCancelTimer2 by func { declare(handle, pointer, retType = int32) }
    val NtCancelWaitCompletionPacket by func { declare(handle, byte, retType = int32) }
    val NtClearEvent by func { declare(handle, retType = int32) }
    val NtClose by func { declare(handle, retType = int32) }
    val NtCloseObjectAuditAlarm by func { declare(pointer, pointer, byte, retType = int32) }
    val NtCommitComplete by func { declare(handle, pointer, retType = int32) }
    val NtCommitEnlistment by func { declare(handle, pointer, retType = int32) }
    val NtCommitRegistryTransaction by func { declare(handle, int32, retType = int32) }
    val NtCommitTransaction by func { declare(handle, byte, retType = int32) }
    val NtCompactKeys by func { declare(int32, pointer, retType = int32) }
    val NtCompareObjects by func { declare(handle, handle, retType = int32) }
    val NtCompareTokens by func { declare(handle, handle, pointer, retType = int32) }
    val NtCompleteConnectPort by func { declare(handle, retType = int32) }
    val NtCompressKey by func { declare(handle, retType = int32) }
    val NtConnectPort by func { declare(pointer, pointer, pointer, pointer, pointer, pointer, pointer, pointer, retType = int32) }
    val NtConvertBetweenAuxiliaryCounterAndPerformanceCounter by func { declare(byte, pointer, pointer, pointer, retType = int32) }
    val NtCreateDebugObject by func { declare(pointer, int32, pointer, int32, retType = int32) }
    val NtCreateDirectoryObject by func { declare(pointer, int32, pointer, retType = int32) }
    val NtCreateDirectoryObjectEx by func { declare(pointer, int32, pointer, handle, int32, retType = int32) }
    val NtCreateEnlistment by func { declare(pointer, int32, handle, handle, pointer, int32, int32, pointer, retType = int32) }
    val NtCreateEvent by func { declare(pointer, int32, pointer, int32, byte, retType = int32) }
    val NtCreateEventPair by func { declare(pointer, int32, pointer, retType = int32) }
    val NtCreateFile by func { declare(pointer, int32, pointer, pointer, pointer, int32, int32, int32, int32, pointer, int32, retType = int32) }
    val NtCreateIoCompletion by func { declare(pointer, int32, pointer, int32, retType = int32) }
    val NtCreateJobObject by func { declare(pointer, int32, pointer, retType = int32) }
    val NtCreateJobSet by func { declare(int32, pointer, int32, retType = int32) }
    val NtCreateKey by func { declare(pointer, int32, pointer, int32, pointer, int32, pointer, retType = int32) }
    val NtCreateKeyTransacted by func { declare(pointer, int32, pointer, int32, pointer, int32, handle, pointer, retType = int32) }
    val NtCreateKeyedEvent by func { declare(pointer, int32, pointer, int32, retType = int32) }
    val NtCreateLowBoxToken by func { declare(pointer, handle, int32, pointer, pointer, int32, pointer, int32, pointer, retType = int32) }
    val NtCreateMailslotFile by func { declare(pointer, int32, pointer, pointer, int32, int32, int32, pointer, retType = int32) }
    val NtCreateMutant by func { declare(pointer, int32, pointer, byte, retType = int32) }
    val NtCreateNamedPipeFile by func { declare(pointer, int32, pointer, pointer, int32, int32, int32, int32, int32, int32, int32, int32, int32, pointer, retType = int32) }
    val NtCreatePrivateNamespace by func { declare(pointer, int32, pointer, pointer, retType = int32) }
    val NtCreateProcess by func { declare(pointer, int32, pointer, handle, byte, handle, handle, handle, retType = int32) }
    val NtCreateProcessEx by func { declare(pointer, int32, pointer, handle, int32, handle, handle, handle, int32, retType = int32) }
    val NtCreateProfile by func { declare(pointer, handle, pointer, int64, int32, pointer, int32, int32, int64, retType = int32) }
    val NtCreateProfileEx by func { declare(pointer, handle, pointer, int64, int32, pointer, int32, int32, int16, pointer, retType = int32) }
    val NtCreateRegistryTransaction by func { declare(pointer, int32, pointer, int32, retType = int32) }
    val NtCreateResourceManager by func { declare(pointer, int32, handle, pointer, pointer, int32, pointer, retType = int32) }
    val NtCreateSection by func { declare(pointer, int32, pointer, pointer, int32, int32, handle, retType = int32) }
    val NtCreateSectionEx by func { declare(pointer, int32, pointer, pointer, int32, int32, handle, pointer, int32, retType = int32) }
    val NtCreateSemaphore by func { declare(pointer, int32, pointer, int32, int32, retType = int32) }
    val NtCreateSymbolicLinkObject by func { declare(pointer, int32, pointer, pointer, retType = int32) }
    val NtCreateThread by func { declare(pointer, int32, pointer, handle, pointer, pointer, pointer, byte, retType = int32) }
    val NtCreateThreadEx by func { declare(pointer, int32, pointer, handle, pointer, pointer, int32, int64, int64, int64, pointer, retType = int32) }
    val NtCreateTimer by func { declare(pointer, int32, pointer, int32, retType = int32) }
    val NtCreateTimer2 by func { declare(pointer, pointer, pointer, int32, int32, retType = int32) }
    val NtCreateToken by func { declare(pointer, int32, pointer, int32, pointer, pointer, pointer, pointer, pointer, pointer, pointer, pointer, pointer, retType = int32) }
    val NtCreateTokenEx by func { declare(pointer, int32, pointer, int32, pointer, pointer, pointer, pointer, pointer, pointer, pointer, pointer, pointer, pointer, pointer, pointer, pointer, retType = int32) }
    val NtCreateTransaction by func { declare(pointer, int32, pointer, pointer, handle, int32, int32, int32, pointer, pointer, retType = int32) }
    val NtCreateTransactionManager by func { declare(pointer, int32, pointer, pointer, int32, int32, retType = int32) }
    val NtCreateUserProcess by func { declare(pointer, pointer, int32, int32, pointer, pointer, int32, int32, pointer, pointer, pointer, retType = int32) }
    val NtCreateWaitCompletionPacket by func { declare(pointer, int32, pointer, retType = int32) }
    val NtCreateWaitablePort by func { declare(pointer, pointer, int32, int32, int32, retType = int32) }
    val NtDebugActiveProcess by func { declare(handle, handle, retType = int32) }
    val NtDebugContinue by func { declare(handle, pointer, int32, retType = int32) }
    val NtDelayExecution by func { declare(byte, pointer, retType = int32) }
    val NtDeleteAtom by func { declare(int16, retType = int32) }
    val NtDeleteFile by func { declare(pointer, retType = int32) }
    val NtDeleteKey by func { declare(handle, retType = int32) }
    val NtDeleteObjectAuditAlarm by func { declare(pointer, pointer, byte, retType = int32) }
    val NtDeletePrivateNamespace by func { declare(handle, retType = int32) }
    val NtDeleteValueKey by func { declare(handle, pointer, retType = int32) }
    val NtDeviceIoControlFile by func { declare(handle, handle, pointer, pointer, pointer, int32, pointer, int32, pointer, int32, retType = int32) }
    val NtDisableLastKnownGood by func { declare(retType = int32) }
    val NtDisplayString by func { declare(pointer, retType = int32) }
    val NtDuplicateObject by func { declare(handle, handle, handle, pointer, int32, int32, int32, retType = int32) }
    val NtDuplicateToken by func { declare(handle, int32, pointer, byte, int32, pointer, retType = int32) }
    val NtEnableLastKnownGood by func { declare(retType = int32) }
    val NtEnumerateKey by func { declare(handle, int32, int32, pointer, int32, pointer, retType = int32) }
    val NtEnumerateSystemEnvironmentValuesEx by func { declare(int32, pointer, pointer, retType = int32) }
    val NtEnumerateTransactionObject by func { declare(handle, int32, pointer, int32, pointer, retType = int32) }
    val NtEnumerateValueKey by func { declare(handle, int32, int32, pointer, int32, pointer, retType = int32) }
    val NtExtendSection by func { declare(handle, pointer, retType = int32) }
    val NtFilterToken by func { declare(handle, int32, pointer, pointer, pointer, pointer, retType = int32) }
    val NtFilterTokenEx by func { declare(handle, int32, pointer, pointer, pointer, int32, pointer, int32, pointer, pointer, pointer, pointer, pointer, pointer, retType = int32) }
    val NtFindAtom by func { declare(pointer, int32, pointer, retType = int32) }
    val NtFlushBuffersFile by func { declare(handle, pointer, retType = int32) }
    val NtFlushBuffersFileEx by func { declare(handle, int32, pointer, int32, pointer, retType = int32) }
    val NtFlushInstallUILanguage by func { declare(int16, int32, retType = int32) }
    val NtFlushInstructionCache by func { declare(handle, pointer, int64, retType = int32) }
    val NtFlushKey by func { declare(handle, retType = int32) }
    val NtFlushProcessWriteBuffers by func { declare(retType = int32) }
    val NtFlushVirtualMemory by func { declare(handle, pointer, pointer, pointer, retType = int32) }
    val NtFlushWriteBuffer by func { declare(retType = int32) }
    val NtFreeUserPhysicalPages by func { declare(handle, pointer, pointer, retType = int32) }
    val NtFreeVirtualMemory by func { declare(handle, pointer, pointer, int32, retType = int32) }
    val NtFreezeRegistry by func { declare(int32, retType = int32) }
    val NtFreezeTransactions by func { declare(pointer, pointer, retType = int32) }
    val NtFsControlFile by func { declare(handle, handle, pointer, pointer, pointer, int32, pointer, int32, pointer, int32, retType = int32) }
    val NtGetCachedSigningLevel by func { declare(handle, pointer, pointer, pointer, pointer, pointer, retType = int32) }
    val NtGetContextThread by func { declare(handle, pointer, retType = int32) }
    val NtGetCurrentProcessorNumber by func { declare(retType = int32) }
    val NtGetCurrentProcessorNumberEx by func { declare(pointer, retType = int32) }
    val NtGetDevicePowerState by func { declare(handle, pointer, retType = int32) }
    val NtGetNextProcess by func { declare(handle, int32, int32, int32, pointer, retType = int32) }
    val NtGetNextThread by func { declare(handle, handle, int32, int32, int32, pointer, retType = int32) }
    val NtGetNotificationResourceManager by func { declare(handle, pointer, int32, pointer, pointer, int32, int64, retType = int32) }
    val NtGetTickCount by func { declare(retType = int32) }
    val NtGetWriteWatch by func { declare(handle, int32, pointer, int64, pointer, pointer, pointer, retType = int32) }
    val NtImpersonateAnonymousToken by func { declare(handle, retType = int32) }
    val NtImpersonateClientOfPort by func { declare(handle, pointer, retType = int32) }
    val NtImpersonateThread by func { declare(handle, handle, pointer, retType = int32) }
    val NtInitializeRegistry by func { declare(int16, retType = int32) }
    val NtInitiatePowerAction by func { declare(int32, int32, int32, byte, retType = int32) }
    val NtIsProcessInJob by func { declare(handle, handle, retType = int32) }
    val NtIsSystemResumeAutomatic by func { declare(retType = byte) }
    val NtIsUILanguageComitted by func { declare(retType = int32) }
    val NtListenPort by func { declare(handle, pointer, retType = int32) }
    val NtLoadDriver by func { declare(pointer, retType = int32) }
    val NtLoadKey by func { declare(pointer, pointer, retType = int32) }
    val NtLoadKey2 by func { declare(pointer, pointer, int32, retType = int32) }
    val NtLoadKeyEx by func { declare(pointer, pointer, int32, handle, handle, int32, pointer, pointer, retType = int32) }
    val NtLockProductActivationKeys by func { declare(pointer, pointer, retType = int32) }
    val NtLockRegistryKey by func { declare(handle, retType = int32) }
    val NtLockVirtualMemory by func { declare(handle, pointer, pointer, int32, retType = int32) }
    val NtMakePermanentObject by func { declare(handle, retType = int32) }
    val NtMakeTemporaryObject by func { declare(handle, retType = int32) }
    val NtMapUserPhysicalPages by func { declare(pointer, int64, pointer, retType = int32) }
    val NtMapUserPhysicalPagesScatter by func { declare(pointer, int64, pointer, retType = int32) }
    val NtMapViewOfSection by func { declare(handle, handle, pointer, int64, int64, pointer, pointer, int32, int32, int32, retType = int32) }
    val NtMapViewOfSectionEx by func { declare(handle, handle, pointer, pointer, pointer, int32, int32, pointer, int32, retType = int32) }
    val NtNotifyChangeDirectoryFile by func { declare(handle, handle, pointer, pointer, pointer, pointer, int32, int32, byte, retType = int32) }
    val NtNotifyChangeDirectoryFileEx by func { declare(handle, handle, pointer, pointer, pointer, pointer, int32, int32, byte, int32, retType = int32) }
    val NtNotifyChangeKey by func { declare(handle, handle, pointer, pointer, pointer, int32, byte, pointer, int32, byte, retType = int32) }
    val NtNotifyChangeMultipleKeys by func { declare(handle, int32, pointer, handle, pointer, pointer, pointer, int32, byte, pointer, int32, byte, retType = int32) }
    val NtNotifyChangeSession by func { declare(handle, int32, pointer, int32, int32, int32, pointer, int32, retType = int32) }
    val NtOpenDirectoryObject by func { declare(pointer, int32, pointer, retType = int32) }
    val NtOpenEnlistment by func { declare(pointer, int32, handle, pointer, pointer, retType = int32) }
    val NtOpenEvent by func { declare(pointer, int32, pointer, retType = int32) }
    val NtOpenEventPair by func { declare(pointer, int32, pointer, retType = int32) }
    val NtOpenFile by func { declare(pointer, int32, pointer, pointer, int32, int32, retType = int32) }
    val NtOpenIoCompletion by func { declare(pointer, int32, pointer, retType = int32) }
    val NtOpenJobObject by func { declare(pointer, int32, pointer, retType = int32) }
    val NtOpenKey by func { declare(pointer, int32, pointer, retType = int32) }
    val NtOpenKeyEx by func { declare(pointer, int32, pointer, int32, retType = int32) }
    val NtOpenKeyTransacted by func { declare(pointer, int32, pointer, handle, retType = int32) }
    val NtOpenKeyTransactedEx by func { declare(pointer, int32, pointer, int32, handle, retType = int32) }
    val NtOpenKeyedEvent by func { declare(pointer, int32, pointer, retType = int32) }
    val NtOpenMutant by func { declare(pointer, int32, pointer, retType = int32) }
    val NtOpenObjectAuditAlarm by func { declare(pointer, pointer, pointer, pointer, pointer, handle, int32, int32, pointer, byte, byte, pointer, retType = int32) }
    val NtOpenPrivateNamespace by func { declare(pointer, int32, pointer, pointer, retType = int32) }
    val NtOpenProcess by func { declare(pointer, int32, pointer, pointer, retType = int32) }
    val NtOpenProcessToken by func { declare(handle, int32, pointer, retType = int32) }
    val NtOpenProcessTokenEx by func { declare(handle, int32, int32, pointer, retType = int32) }
    val NtOpenRegistryTransaction by func { declare(pointer, int32, pointer, retType = int32) }
    val NtOpenResourceManager by func { declare(pointer, int32, handle, pointer, pointer, retType = int32) }
    val NtOpenSection by func { declare(pointer, int32, pointer, retType = int32) }
    val NtOpenSemaphore by func { declare(pointer, int32, pointer, retType = int32) }
    val NtOpenSession by func { declare(pointer, int32, pointer, retType = int32) }
    val NtOpenSymbolicLinkObject by func { declare(pointer, int32, pointer, retType = int32) }
    val NtOpenThread by func { declare(pointer, int32, pointer, pointer, retType = int32) }
    val NtOpenThreadToken by func { declare(handle, int32, byte, pointer, retType = int32) }
    val NtOpenThreadTokenEx by func { declare(handle, int32, byte, int32, pointer, retType = int32) }
    val NtOpenTimer by func { declare(pointer, int32, pointer, retType = int32) }
    val NtOpenTransaction by func { declare(pointer, int32, pointer, pointer, handle, retType = int32) }
    val NtOpenTransactionManager by func { declare(pointer, int32, pointer, pointer, pointer, int32, retType = int32) }
    val NtPowerInformation by func { declare(int32, pointer, int32, pointer, int32, retType = int32) }
    val NtPrePrepareComplete by func { declare(handle, pointer, retType = int32) }
    val NtPrePrepareEnlistment by func { declare(handle, pointer, retType = int32) }
    val NtPrepareComplete by func { declare(handle, pointer, retType = int32) }
    val NtPrepareEnlistment by func { declare(handle, pointer, retType = int32) }
    val NtPrivilegeCheck by func { declare(handle, pointer, pointer, retType = int32) }
    val NtPrivilegeObjectAuditAlarm by func { declare(pointer, pointer, handle, int32, pointer, byte, retType = int32) }
    val NtPrivilegedServiceAuditAlarm by func { declare(pointer, pointer, handle, pointer, byte, retType = int32) }
    val NtPropagationComplete by func { declare(handle, int32, int32, pointer, retType = int32) }
    val NtPropagationFailed by func { declare(handle, int32, int32, retType = int32) }
    val NtProtectVirtualMemory by func { declare(handle, pointer, pointer, int32, pointer, retType = int32) }
    val NtPulseEvent by func { declare(handle, pointer, retType = int32) }
    val NtQueryAttributesFile by func { declare(pointer, pointer, retType = int32) }
    val NtQueryDebugFilterState by func { declare(int32, int32, retType = int32) }
    val NtQueryDefaultLocale by func { declare(byte, pointer, retType = int32) }
    val NtQueryDefaultUILanguage by func { declare(pointer, retType = int32) }
    val NtQueryDirectoryFile by func { declare(handle, handle, pointer, pointer, pointer, pointer, int32, int32, byte, pointer, byte, retType = int32) }
    val NtQueryDirectoryFileEx by func { declare(handle, handle, pointer, pointer, pointer, pointer, int32, int32, int32, pointer, retType = int32) }
    val NtQueryDirectoryObject by func { declare(handle, pointer, int32, byte, byte, pointer, pointer, retType = int32) }
    val NtQueryDriverEntryOrder by func { declare(pointer, pointer, retType = int32) }
    val NtQueryEaFile by func { declare(handle, pointer, pointer, int32, byte, pointer, int32, pointer, byte, retType = int32) }
    val NtQueryEvent by func { declare(handle, int32, pointer, int32, pointer, retType = int32) }
    val NtQueryFullAttributesFile by func { declare(pointer, pointer, retType = int32) }
    val NtQueryInformationAtom by func { declare(int16, int32, pointer, int32, pointer, retType = int32) }
    val NtQueryInformationByName by func { declare(pointer, pointer, pointer, int32, int32, retType = int32) }
    val NtQueryInformationEnlistment by func { declare(handle, int32, pointer, int32, pointer, retType = int32) }
    val NtQueryInformationFile by func { declare(handle, pointer, pointer, int32, int32, retType = int32) }
    val NtQueryInformationJobObject by func { declare(handle, int32, pointer, int32, pointer, retType = int32) }
    val NtQueryInformationPort by func { declare(handle, int32, pointer, int32, pointer, retType = int32) }
    val NtQueryInformationProcess by func { declare(handle, int32, pointer, int32, pointer, retType = int32) }
    val NtQueryInformationResourceManager by func { declare(handle, int32, pointer, int32, pointer, retType = int32) }
    val NtQueryInformationThread by func { declare(handle, int32, pointer, int32, pointer, retType = int32) }
    val NtQueryInformationToken by func { declare(handle, int32, pointer, int32, pointer, retType = int32) }
    val NtQueryInformationTransaction by func { declare(handle, int32, pointer, int32, pointer, retType = int32) }
    val NtQueryInformationTransactionManager by func { declare(handle, int32, pointer, int32, pointer, retType = int32) }
    val NtQueryInstallUILanguage by func { declare(pointer, retType = int32) }
    val NtQueryIntervalProfile by func { declare(int32, pointer, retType = int32) }
    val NtQueryIoCompletion by func { declare(handle, int32, pointer, int32, pointer, retType = int32) }
    val NtQueryKey by func { declare(handle, int32, pointer, int32, pointer, retType = int32) }
    val NtQueryLicenseValue by func { declare(pointer, pointer, pointer, int32, pointer, retType = int32) }
    val NtQueryMultipleValueKey by func { declare(handle, pointer, int32, pointer, pointer, pointer, retType = int32) }
    val NtQueryMutant by func { declare(handle, int32, pointer, int32, pointer, retType = int32) }
    val NtQueryObject by func { declare(handle, int32, pointer, int32, pointer, retType = int32) }
    val NtQueryOpenSubKeys by func { declare(pointer, pointer, retType = int32) }
    val NtQueryOpenSubKeysEx by func { declare(pointer, int32, pointer, pointer, retType = int32) }
    val NtQueryPerformanceCounter by func { declare(pointer, pointer, retType = int32) }
    val NtQueryPortInformationProcess by func { declare(retType = int32) }
    val NtQueryQuotaInformationFile by func { declare(handle, pointer, pointer, int32, byte, pointer, int32, pointer, byte, retType = int32) }
    val NtQuerySection by func { declare(handle, int32, pointer, int64, pointer, retType = int32) }
    val NtQuerySecurityAttributesToken by func { declare(handle, pointer, int32, pointer, int32, pointer, retType = int32) }
    val NtQuerySecurityObject by func { declare(handle, int32, pointer, int32, pointer, retType = int32) }
    val NtQuerySemaphore by func { declare(handle, int32, pointer, int32, pointer, retType = int32) }
    val NtQuerySymbolicLinkObject by func { declare(handle, pointer, pointer, retType = int32) }
    val NtQuerySystemEnvironmentValue by func { declare(pointer, pointer, int16, pointer, retType = int32) }
    val NtQuerySystemEnvironmentValueEx by func { declare(pointer, pointer, pointer, pointer, pointer, retType = int32) }
    val NtQuerySystemInformation by func { declare(int32, pointer, int32, pointer, retType = int32) }
    val NtQuerySystemInformationEx by func { declare(int32, pointer, int32, pointer, int32, pointer, retType = int32) }
    val NtQuerySystemTime by func { declare(pointer, retType = int32) }
    val NtQueryTimer by func { declare(handle, int32, pointer, int32, pointer, retType = int32) }
    val NtQueryTimerResolution by func { declare(pointer, pointer, pointer, retType = int32) }
    val NtQueryValueKey by func { declare(handle, pointer, int32, pointer, int32, pointer, retType = int32) }
    val NtQueryVirtualMemory by func { declare(handle, pointer, int32, pointer, int64, pointer, retType = int32) }
    val NtQueryVolumeInformationFile by func { declare(handle, pointer, pointer, int32, int32, retType = int32) }
    val NtQueueApcThread by func { declare(handle, pointer, pointer, pointer, pointer, retType = int32) }
    val NtQueueApcThreadEx by func { declare(handle, handle, pointer, pointer, pointer, pointer, retType = int32) }
    val NtReadFile by func { declare(handle, handle, pointer, pointer, pointer, pointer, int32, pointer, pointer, retType = int32) }
    val NtReadFileScatter by func { declare(handle, handle, pointer, pointer, pointer, pointer, int32, pointer, pointer, retType = int32) }
    val NtReadOnlyEnlistment by func { declare(handle, pointer, retType = int32) }
    val NtReadRequestData by func { declare(handle, pointer, int32, pointer, int64, pointer, retType = int32) }
    val NtReadVirtualMemory by func { declare(handle, pointer, pointer, int64, pointer, retType = int32) }
    val NtRecoverEnlistment by func { declare(handle, pointer, retType = int32) }
    val NtRecoverResourceManager by func { declare(handle, retType = int32) }
    val NtRecoverTransactionManager by func { declare(handle, retType = int32) }
    val NtRegisterProtocolAddressInformation by func { declare(handle, pointer, int32, pointer, int32, retType = int32) }
    val NtRegisterThreadTerminatePort by func { declare(handle, retType = int32) }
    val NtReleaseKeyedEvent by func { declare(handle, pointer, byte, pointer, retType = int32) }
    val NtReleaseMutant by func { declare(handle, pointer, retType = int32) }
    val NtReleaseSemaphore by func { declare(handle, int32, pointer, retType = int32) }
    val NtRemoveIoCompletion by func { declare(handle, pointer, pointer, pointer, pointer, retType = int32) }
    val NtRemoveIoCompletionEx by func { declare(handle, pointer, int32, pointer, pointer, byte, retType = int32) }
    val NtRemoveProcessDebug by func { declare(handle, handle, retType = int32) }
    val NtRenameKey by func { declare(handle, pointer, retType = int32) }
    val NtRenameTransactionManager by func { declare(pointer, pointer, retType = int32) }
    val NtReplaceKey by func { declare(pointer, handle, pointer, retType = int32) }
    val NtReplyPort by func { declare(handle, pointer, retType = int32) }
    val NtReplyWaitReceivePort by func { declare(handle, pointer, pointer, pointer, retType = int32) }
    val NtReplyWaitReceivePortEx by func { declare(handle, pointer, pointer, pointer, pointer, retType = int32) }
    val NtReplyWaitReplyPort by func { declare(handle, pointer, retType = int32) }
    val NtRequestPort by func { declare(handle, pointer, retType = int32) }
    val NtRequestWaitReplyPort by func { declare(handle, pointer, pointer, retType = int32) }
    val NtResetEvent by func { declare(handle, pointer, retType = int32) }
    val NtResetWriteWatch by func { declare(handle, pointer, int64, retType = int32) }
    val NtRestoreKey by func { declare(handle, handle, int32, retType = int32) }
    val NtResumeProcess by func { declare(handle, retType = int32) }
    val NtResumeThread by func { declare(handle, pointer, retType = int32) }
    val NtRevertContainerImpersonation by func { declare(retType = int32) }
    val NtRollbackComplete by func { declare(handle, pointer, retType = int32) }
    val NtRollbackEnlistment by func { declare(handle, pointer, retType = int32) }
    val NtRollbackRegistryTransaction by func { declare(handle, int32, retType = int32) }
    val NtRollbackTransaction by func { declare(handle, byte, retType = int32) }
    val NtRollforwardTransactionManager by func { declare(handle, pointer, retType = int32) }
    val NtSaveKey by func { declare(handle, handle, retType = int32) }
    val NtSaveKeyEx by func { declare(handle, handle, int32, retType = int32) }
    val NtSaveMergedKeys by func { declare(handle, handle, handle, retType = int32) }
    val NtSecureConnectPort by func { declare(pointer, pointer, pointer, pointer, pointer, pointer, pointer, pointer, pointer, retType = int32) }
    val NtSetCachedSigningLevel by func { declare(int32, byte, pointer, int32, handle, retType = int32) }
    val NtSetContextThread by func { declare(handle, pointer, retType = int32) }
    val NtSetDebugFilterState by func { declare(int32, int32, byte, retType = int32) }
    val NtSetDefaultLocale by func { declare(byte, int32, retType = int32) }
    val NtSetDefaultUILanguage by func { declare(int16, retType = int32) }
    val NtSetDriverEntryOrder by func { declare(pointer, int32, retType = int32) }
    val NtSetEaFile by func { declare(handle, pointer, pointer, int32, retType = int32) }
    val NtSetEvent by func { declare(handle, pointer, retType = int32) }
    val NtSetEventBoostPriority by func { declare(handle, retType = int32) }
    val NtSetHighEventPair by func { declare(handle, retType = int32) }
    val NtSetHighWaitLowEventPair by func { declare(handle, retType = int32) }
    val NtSetIRTimer by func { declare(handle, pointer, retType = int32) }
    val NtSetInformationDebugObject by func { declare(handle, int32, pointer, int32, pointer, retType = int32) }
    val NtSetInformationEnlistment by func { declare(handle, int32, pointer, int32, retType = int32) }
    val NtSetInformationFile by func { declare(handle, pointer, pointer, int32, int32, retType = int32) }
    val NtSetInformationJobObject by func { declare(handle, int32, pointer, int32, retType = int32) }
    val NtSetInformationKey by func { declare(handle, int32, pointer, int32, retType = int32) }
    val NtSetInformationObject by func { declare(handle, int32, pointer, int32, retType = int32) }
    val NtSetInformationProcess by func { declare(handle, int32, pointer, int32, retType = int32) }
    val NtSetInformationResourceManager by func { declare(handle, int32, pointer, int32, retType = int32) }
    val NtSetInformationSymbolicLink by func { declare(handle, int32, pointer, int32, retType = int32) }
    val NtSetInformationThread by func { declare(handle, int32, pointer, int32, retType = int32) }
    val NtSetInformationToken by func { declare(handle, int32, pointer, int32, retType = int32) }
    val NtSetInformationTransaction by func { declare(handle, int32, pointer, int32, retType = int32) }
    val NtSetInformationTransactionManager by func { declare(handle, int32, pointer, int32, retType = int32) }
    val NtSetIntervalProfile by func { declare(int32, int32, retType = int32) }
    val NtSetIoCompletion by func { declare(handle, pointer, pointer, int32, int64, retType = int32) }
    val NtSetIoCompletionEx by func { declare(handle, handle, pointer, pointer, int32, int64, retType = int32) }
    val NtSetLowEventPair by func { declare(handle, retType = int32) }
    val NtSetLowWaitHighEventPair by func { declare(handle, retType = int32) }
    val NtSetQuotaInformationFile by func { declare(handle, pointer, pointer, int32, retType = int32) }
    val NtSetSecurityObject by func { declare(handle, int32, pointer, retType = int32) }
    val NtSetSystemEnvironmentValue by func { declare(pointer, pointer, retType = int32) }
    val NtSetSystemEnvironmentValueEx by func { declare(pointer, pointer, pointer, int32, int32, retType = int32) }
    val NtSetSystemInformation by func { declare(int32, pointer, int32, retType = int32) }
    val NtSetSystemPowerState by func { declare(int32, int32, int32, retType = int32) }
    val NtSetSystemTime by func { declare(pointer, pointer, retType = int32) }
    val NtSetThreadExecutionState by func { declare(int32, pointer, retType = int32) }
    val NtSetTimer by func { declare(handle, pointer, pointer, pointer, byte, int32, pointer, retType = int32) }
    val NtSetTimer2 by func { declare(handle, pointer, pointer, pointer, retType = int32) }
    val NtSetTimerEx by func { declare(handle, int32, pointer, int32, retType = int32) }
    val NtSetTimerResolution by func { declare(int32, byte, pointer, retType = int32) }
    val NtSetUuidSeed by func { declare(pointer, retType = int32) }
    val NtSetValueKey by func { declare(handle, pointer, int32, int32, pointer, int32, retType = int32) }
    val NtSetVolumeInformationFile by func { declare(handle, pointer, pointer, int32, int32, retType = int32) }
    val NtShutdownSystem by func { declare(int32, retType = int32) }
    val NtSignalAndWaitForSingleObject by func { declare(handle, handle, byte, pointer, retType = int32) }
    val NtSinglePhaseReject by func { declare(handle, pointer, retType = int32) }
    val NtStartProfile by func { declare(handle, retType = int32) }
    val NtStopProfile by func { declare(handle, retType = int32) }
    val NtSuspendProcess by func { declare(handle, retType = int32) }
    val NtSuspendThread by func { declare(handle, pointer, retType = int32) }
    val NtSystemDebugControl by func { declare(int32, pointer, int32, pointer, int32, pointer, retType = int32) }
    val NtTerminateJobObject by func { declare(handle, int32, retType = int32) }
    val NtTerminateProcess by func { declare(handle, int32, retType = int32) }
    val NtTerminateThread by func { declare(handle, int32, retType = int32) }
    val NtThawRegistry by func { declare(retType = int32) }
    val NtThawTransactions by func { declare(retType = int32) }
    val NtTraceControl by func { declare(int32, pointer, int32, pointer, int32, pointer, retType = int32) }
    val NtTraceEvent by func { declare(handle, int32, int32, pointer, retType = int32) }
    val NtTranslateFilePath by func { declare(pointer, int32, pointer, pointer, retType = int32) }
    val NtUmsThreadYield by func { declare(pointer, retType = int32) }
    val NtUnloadDriver by func { declare(pointer, retType = int32) }
    val NtUnloadKey by func { declare(pointer, retType = int32) }
    val NtUnloadKey2 by func { declare(pointer, int32, retType = int32) }
    val NtUnloadKeyEx by func { declare(pointer, handle, retType = int32) }
    val NtUnlockVirtualMemory by func { declare(handle, pointer, pointer, int32, retType = int32) }
    val NtUnmapViewOfSection by func { declare(handle, pointer, retType = int32) }
    val NtUnmapViewOfSectionEx by func { declare(handle, pointer, int32, retType = int32) }
    val NtWaitForAlertByThreadId by func { declare(pointer, pointer, retType = int32) }
    val NtWaitForDebugEvent by func { declare(handle, byte, pointer, pointer, retType = int32) }
    val NtWaitForKeyedEvent by func { declare(handle, pointer, byte, pointer, retType = int32) }
    val NtWaitForMultipleObjects by func { declare(int32, pointer, int32, byte, pointer, retType = int32) }
    val NtWaitForMultipleObjects32 by func { declare(int32, pointer, int32, byte, pointer, retType = int32) }
    val NtWaitForSingleObject by func { declare(handle, byte, pointer, retType = int32) }
    val NtWaitHighEventPair by func { declare(handle, retType = int32) }
    val NtWaitLowEventPair by func { declare(handle, retType = int32) }
    val NtWriteFile by func { declare(handle, handle, pointer, pointer, pointer, pointer, int32, pointer, pointer, retType = int32) }
    val NtWriteFileGather by func { declare(handle, handle, pointer, pointer, pointer, pointer, int32, pointer, pointer, retType = int32) }
    val NtWriteRequestData by func { declare(handle, pointer, int32, pointer, int64, pointer, retType = int32) }
    val NtWriteVirtualMemory by func { declare(handle, pointer, pointer, int64, pointer, retType = int32) }
    val NtYieldExecution by func { declare(retType = int32) }

    val PfxFindPrefix by func { declare(pointer, pointer, retType = pointer) }
    val PfxInitialize by func { declare(pointer) }
    val PfxInsertPrefix by func { declare(pointer, pointer, pointer, retType = byte) }
    val PfxRemovePrefix by func { declare(pointer, pointer) }

    val RtlAbsoluteToSelfRelativeSD by func { declare(pointer, pointer, pointer, retType = int32) }
    val RtlAcquirePebLock by func { declare(retType = int32) }
    val RtlAcquirePrivilege by func { declare(pointer, int32, int32, pointer, retType = int32) }
    val RtlAcquireResourceExclusive by func { declare(pointer, byte, retType = byte) }
    val RtlAcquireResourceShared by func { declare(pointer, byte, retType = byte) }
    val RtlAcquireSRWLockExclusive by func { declare(pointer) }
    val RtlAcquireSRWLockShared by func { declare(pointer) }
    val RtlAddAccessAllowedAce by func { declare(pointer, int32, int32, pointer, retType = int32) }
    val RtlAddAccessAllowedAceEx by func { declare(pointer, int32, int32, int32, pointer, retType = int32) }
    val RtlAddAccessAllowedObjectAce by func { declare(pointer, int32, int32, int32, pointer, pointer, pointer, retType = int32) }
    val RtlAddAccessDeniedAce by func { declare(pointer, int32, int32, pointer, retType = int32) }
    val RtlAddAccessDeniedAceEx by func { declare(pointer, int32, int32, int32, pointer, retType = int32) }
    val RtlAddAccessDeniedObjectAce by func { declare(pointer, int32, int32, int32, pointer, pointer, pointer, retType = int32) }
    val RtlAddAce by func { declare(pointer, int32, int32, pointer, int32, retType = int32) }
    val RtlAddAtomToAtomTable by func { declare(pointer, pointer, pointer, retType = int32) }
    val RtlAddAuditAccessAce by func { declare(pointer, int32, int32, pointer, byte, byte, retType = int32) }
    val RtlAddAuditAccessAceEx by func { declare(pointer, int32, int32, int32, pointer, byte, byte, retType = int32) }
    val RtlAddAuditAccessObjectAce by func { declare(pointer, int32, int32, int32, pointer, pointer, pointer, byte, byte, retType = int32) }
    val RtlAddCompoundAce by func { declare(pointer, int32, byte, int32, pointer, pointer, retType = int32) }
    val RtlAddFunctionTable by func { declare(pointer, int32, int64, retType = byte) }
    val RtlAddGrowableFunctionTable by func { declare(pointer, pointer, int32, int32, int64, int64, retType = int32) }
    val RtlAddProcessTrustLabelAce by func { declare(pointer, int32, int32, pointer, byte, int32, retType = int32) }
    val RtlAddResourceAttributeAce by func { declare(pointer, int32, int32, int32, pointer, pointer, pointer, retType = int32) }
    val RtlAddSIDToBoundaryDescriptor by func { declare(pointer, pointer, retType = int32) }
    val RtlAddScopedPolicyIDAce by func { declare(pointer, int32, int32, int32, pointer, retType = int32) }
    val RtlAddVectoredContinueHandler by func { declare(int32, pointer, retType = pointer) }
    val RtlAddVectoredExceptionHandler by func { declare(int32, pointer, retType = pointer) }
    val RtlAddressInSectionTable by func { declare(pointer, pointer, int32, retType = pointer) }
    val RtlAdjustPrivilege by func { declare(int32, byte, byte, pointer, retType = int32) }
    val RtlAllocateAndInitializeSid by func { declare(pointer, byte, int32, int32, int32, int32, int32, int32, int32, int32, pointer, retType = int32) }
    val RtlAllocateAndInitializeSidEx by func { declare(pointer, byte, pointer, pointer, retType = int32) }
    val RtlAllocateHandle by func { declare(pointer, pointer, retType = pointer) }
    val RtlAllocateHeap by func { declare(handle, int32, int64, retType = pointer) }
    val RtlAnsiCharToUnicodeChar by func { declare(pointer, retType = int16) }
    val RtlAnsiStringToUnicodeString by func { declare(pointer, pointer, byte, retType = int32) }
    val RtlAppendAsciizToString by func { declare(pointer, pointer, retType = int32) }
    val RtlAppendPathElement by func { declare(int32, pointer, pointer, retType = int32) }
    val RtlAppendStringToString by func { declare(pointer, pointer, retType = int32) }
    val RtlAppendUnicodeStringToString by func { declare(pointer, pointer, retType = int32) }
    val RtlAppendUnicodeToString by func { declare(pointer, pointer, retType = int32) }
    val RtlAreAllAccessesGranted by func { declare(int32, int32, retType = byte) }
    val RtlAreAnyAccessesGranted by func { declare(int32, int32, retType = byte) }
    val RtlAreBitsClear by func { declare(pointer, int32, int32, retType = byte) }
    val RtlAreBitsSet by func { declare(pointer, int32, int32, retType = byte) }
    val RtlCaptureContext by func { declare(pointer) }
    val RtlCaptureStackBackTrace by func { declare(int32, int32, pointer, pointer, retType = int16) }
    val RtlCharToInteger by func { declare(pointer, int32, pointer, retType = int32) }
    val RtlCheckForOrphanedCriticalSections by func { declare(handle) }
    val RtlCheckRegistryKey by func { declare(int32, pointer, retType = int32) }
    val RtlClearAllBits by func { declare(pointer) }
    val RtlClearBit by func { declare(pointer, int32) }
    val RtlClearBits by func { declare(pointer, int32, int32) }
    val RtlCompactHeap by func { declare(handle, int32, retType = int64) }
    val RtlCompareAltitudes by func { declare(pointer, pointer, retType = int32) }
    val RtlCompareMemory by func { declare(pointer, pointer, int64, retType = int64) }
    val RtlCompareMemoryUlong by func { declare(pointer, int64, int32, retType = int64) }
    val RtlCompareString by func { declare(pointer, pointer, byte, retType = int32) }
    val RtlCompareUnicodeString by func { declare(pointer, pointer, byte, retType = int32) }
    val RtlCompareUnicodeStrings by func { declare(pointer, int64, pointer, int64, byte, retType = int32) }
    val RtlCompressBuffer by func { declare(int16, pointer, int32, pointer, int32, int32, pointer, pointer, retType = int32) }
    val RtlComputeCrc32 by func { declare(int32, pointer, int32, retType = int32) }
    val RtlComputeImportTableHash by func { declare(handle, pointer, int32, retType = int32) }
    val RtlConsoleMultiByteToUnicodeN by func { declare(pointer, int32, pointer, pointer, int32, pointer, retType = int32) }
    val RtlContractHashTable by func { declare(pointer, retType = byte) }
    val RtlConvertDeviceFamilyInfoToString by func { declare(pointer, pointer, pointer, pointer, retType = int32) }
    val RtlConvertExclusiveToShared by func { declare(pointer) }
    val RtlConvertSharedToExclusive by func { declare(pointer) }
    val RtlConvertSidToUnicodeString by func { declare(pointer, pointer, byte, retType = int32) }
    val RtlConvertToAutoInheritSecurityObject by func { declare(pointer, pointer, pointer, pointer, byte, pointer, retType = int32) }
    val RtlCopyBitMap by func { declare(pointer, pointer, int32) }
    val RtlCopyContext by func { declare(pointer, int32, pointer, retType = int32) }
    val RtlCopyExtendedContext by func { declare(pointer, int32, pointer, retType = int32) }
    val RtlCopyLuid by func { declare(pointer, pointer) }
    val RtlCopyLuidAndAttributesArray by func { declare(int32, pointer, pointer) }
    val RtlCopyMappedMemory by func { declare(pointer, pointer, int64, retType = int32) }
    val RtlCopyMemoryNonTemporal by func { declare(pointer, pointer, int64) }
    val RtlCopySecurityDescriptor by func { declare(pointer, pointer, retType = int32) }
    val RtlCopySid by func { declare(int32, pointer, pointer, retType = int32) }
    val RtlCopySidAndAttributesArray by func { declare(int32, pointer, int32, pointer, pointer, pointer, pointer, retType = int32) }
    val RtlCopyString by func { declare(pointer, pointer) }
    val RtlCopyUnicodeString by func { declare(pointer, pointer) }
    val RtlCrc32 by func { declare(pointer, int64, int32, retType = int32) }
    val RtlCrc64 by func { declare(pointer, int64, int64, retType = int64) }
    val RtlCreateAcl by func { declare(pointer, int32, int32, retType = int32) }
    val RtlCreateAndSetSD by func { declare(pointer, int32, pointer, pointer, pointer, retType = int32) }
    val RtlCreateAtomTable by func { declare(int32, pointer, retType = int32) }
    val RtlCreateEnvironment by func { declare(byte, pointer, retType = int32) }
    val RtlCreateHashTable by func { declare(pointer, int32, int32, retType = byte) }
    val RtlCreateHashTableEx by func { declare(pointer, int32, int32, int32, retType = byte) }
    val RtlCreateHeap by func { declare(int32, pointer, int64, int64, pointer, pointer, retType = handle) }
    val RtlCreateProcessParameters by func { declare(pointer, pointer, pointer, pointer, pointer, pointer, pointer, pointer, pointer, pointer, retType = int32) }
    val RtlCreateQueryDebugBuffer by func { declare(int32, byte, retType = pointer) }
    val RtlCreateRegistryKey by func { declare(int32, pointer, retType = int32) }
    val RtlCreateSecurityDescriptor by func { declare(pointer, int32, retType = int32) }
    val RtlCreateServiceSid by func { declare(pointer, pointer, pointer, retType = int32) }
    val RtlCreateTagHeap by func { declare(handle, int32, pointer, pointer, retType = int32) }
    val RtlCreateTimer by func { declare(handle, pointer, pointer, pointer, int32, int32, int32, retType = int32) }
    val RtlCreateTimerQueue by func { declare(pointer, retType = int32) }
    val RtlCreateUnicodeString by func { declare(pointer, pointer, retType = byte) }
    val RtlCreateUnicodeStringFromAsciiz by func { declare(pointer, pointer, retType = byte) }
    val RtlCreateUserProcessEx by func { declare(pointer, pointer, byte, pointer, pointer, retType = int32) }
    val RtlCreateUserSecurityObject by func { declare(pointer, int32, pointer, pointer, byte, pointer, pointer, retType = int32) }
    val RtlCreateUserStack by func { declare(int64, int64, int64, int64, int64, pointer, retType = int32) }
    val RtlCreateUserThread by func { declare(handle, pointer, byte, int32, int64, int64, pointer, pointer, pointer, pointer, retType = int32) }
    val RtlCreateVirtualAccountSid by func { declare(pointer, int32, pointer, pointer, retType = int32) }
    val RtlCustomCPToUnicodeN by func { declare(pointer, pointer, int32, pointer, pointer, int32, retType = int32) }
    val RtlCutoverTimeToSystemTime by func { declare(pointer, pointer, pointer, byte, retType = byte) }
    val RtlDeNormalizeProcessParams by func { declare(pointer, retType = pointer) }
    val RtlDecodePointer by func { declare(pointer, retType = pointer) }
    val RtlDecodeSystemPointer by func { declare(pointer, retType = pointer) }
    val RtlDecompressBuffer by func { declare(int16, pointer, int32, pointer, int32, pointer, retType = int32) }
    val RtlDecompressBufferEx by func { declare(int16, pointer, int32, pointer, int32, pointer, pointer, retType = int32) }
    val RtlDecompressFragment by func { declare(int16, pointer, int32, pointer, int32, int32, pointer, pointer, retType = int32) }
    val RtlDefaultNpAcl by func { declare(pointer, retType = int32) }
    val RtlDelayExecution by func { declare(byte, pointer, retType = int32) }
    val RtlDelete by func { declare(pointer, retType = pointer) }
    val RtlDeleteAce by func { declare(pointer, int32, retType = int32) }
    val RtlDeleteAtomFromAtomTable by func { declare(pointer, int16, retType = int32) }
    val RtlDeleteBoundaryDescriptor by func { declare(pointer) }
    val RtlDeleteCriticalSection by func { declare(pointer, retType = int32) }
    val RtlDeleteElementGenericTable by func { declare(pointer, pointer, retType = byte) }
    val RtlDeleteElementGenericTableAvl by func { declare(pointer, pointer, retType = byte) }
    val RtlDeleteFunctionTable by func { declare(pointer, retType = byte) }
    val RtlDeleteGrowableFunctionTable by func { declare(pointer) }
    val RtlDeleteHashTable by func { declare(pointer, retType = int32) }
    val RtlDeleteNoSplay by func { declare(pointer, pointer) }
    val RtlDeleteRegistryValue by func { declare(int32, pointer, pointer, retType = int32) }
    val RtlDeleteResource by func { declare(pointer) }
    val RtlDeleteSecurityObject by func { declare(pointer, retType = int32) }
    val RtlDeleteTimer by func { declare(handle, handle, handle, retType = int32) }
    val RtlDeleteTimerQueue by func { declare(handle, retType = int32) }
    val RtlDeleteTimerQueueEx by func { declare(handle, handle, retType = int32) }
    val RtlDeregisterSecureMemoryCacheCallback by func { declare(pointer, retType = int32) }
    val RtlDeregisterWait by func { declare(handle, retType = int32) }
    val RtlDeregisterWaitEx by func { declare(handle, handle, retType = int32) }
    val RtlDestroyAtomTable by func { declare(pointer, retType = int32) }
    val RtlDestroyEnvironment by func { declare(pointer, retType = int32) }
    val RtlDestroyHandleTable by func { declare(pointer, retType = int32) }
    val RtlDestroyHeap by func { declare(handle, retType = pointer) }
    val RtlDestroyProcessParameters by func { declare(pointer, retType = int32) }
    val RtlDestroyQueryDebugBuffer by func { declare(pointer, retType = int32) }
    val RtlDetectHeapLeaks by func { declare() }
    val RtlDetermineDosPathNameType_U by func { declare(pointer, retType = int32) }
    val RtlDllShutdownInProgress by func { declare(retType = byte) }
    val RtlDnsHostNameToComputerName by func { declare(pointer, pointer, byte, retType = int32) }
    val RtlDoesFileExists_U by func { declare(pointer, retType = byte) }
    val RtlDoesNameContainWildCards by func { declare(pointer, retType = byte) }
    val RtlDosApplyFileIsolationRedirection_Ustr by func { declare(int32, pointer, pointer, pointer, pointer, pointer, pointer, pointer, pointer, retType = int32) }
    val RtlDosPathNameToNtPathName_U by func { declare(pointer, pointer, pointer, pointer, retType = byte) }
    val RtlDosPathNameToNtPathName_U_WithStatus by func { declare(pointer, pointer, pointer, pointer, retType = int32) }
    val RtlDosPathNameToRelativeNtPathName_U by func { declare(pointer, pointer, pointer, pointer, retType = byte) }
    val RtlDosPathNameToRelativeNtPathName_U_WithStatus by func { declare(pointer, pointer, pointer, pointer, retType = int32) }
    val RtlDosSearchPath_U by func { declare(pointer, pointer, pointer, int32, pointer, pointer, retType = int32) }
    val RtlDosSearchPath_Ustr by func { declare(int32, pointer, pointer, pointer, pointer, pointer, pointer, pointer, pointer, retType = int32) }
    val RtlDowncaseUnicodeChar by func { declare(int16, retType = int16) }
    val RtlDowncaseUnicodeString by func { declare(pointer, pointer, byte, retType = int32) }
    val RtlDumpResource by func { declare(pointer, retType = int32) }
    val RtlDuplicateUnicodeString by func { declare(int32, pointer, pointer, retType = int32) }
    val RtlEmptyAtomTable by func { declare(pointer, byte, retType = int32) }
    val RtlEnableEarlyCriticalSectionEventCreation by func { declare(retType = pointer) }
    val RtlEnableThreadProfiling by func { declare(handle, int32, int64, pointer, retType = int32) }
    val RtlEncodePointer by func { declare(pointer, retType = pointer) }
    val RtlEncodeSystemPointer by func { declare(pointer, retType = pointer) }
    val RtlEndEnumerationHashTable by func { declare(pointer, pointer) }
    val RtlEndStrongEnumerationHashTable by func { declare(pointer, pointer) }
    val RtlEndWeakEnumerationHashTable by func { declare(pointer, pointer) }
    val RtlEnterCriticalSection by func { declare(pointer, retType = int32) }
    val RtlEnumProcessHeaps by func { declare(pointer, pointer, retType = int32) }
    val RtlEnumerateEntryHashTable by func { declare(pointer, pointer, retType = pointer) }
    val RtlEnumerateGenericTable by func { declare(pointer, byte, retType = pointer) }
    val RtlEnumerateGenericTableAvl by func { declare(pointer, byte, retType = pointer) }
    val RtlEnumerateGenericTableLikeADirectory by func { declare(pointer, pointer, pointer, int32, pointer, pointer, pointer, retType = pointer) }
    val RtlEnumerateGenericTableWithoutSplaying by func { declare(pointer, pointer, retType = pointer) }
    val RtlEnumerateGenericTableWithoutSplayingAvl by func { declare(pointer, pointer, retType = pointer) }
    val RtlEqualComputerName by func { declare(pointer, pointer, retType = byte) }
    val RtlEqualDomainName by func { declare(pointer, pointer, retType = byte) }
    val RtlEqualPrefixSid by func { declare(pointer, pointer, retType = byte) }
    val RtlEqualSid by func { declare(pointer, pointer, retType = byte) }
    val RtlEqualString by func { declare(pointer, pointer, byte, retType = byte) }
    val RtlEqualUnicodeString by func { declare(pointer, pointer, byte, retType = byte) }
    val RtlEraseUnicodeString by func { declare(pointer) }
    val RtlExitUserProcess by func { declare(int32) }
    val RtlExitUserThread by func { declare(int32) }
    val RtlExpandEnvironmentStrings_U by func { declare(pointer, pointer, pointer, pointer, retType = int32) }
    val RtlExpandHashTable by func { declare(pointer, retType = byte) }
    val RtlExtendCorrelationVector by func { declare(pointer, retType = int32) }
    val RtlExtractBitMap by func { declare(pointer, pointer, int32, int32) }
    val RtlFillMemoryNonTemporal by func { declare(pointer, int64, byte) }
    val RtlFindCharInUnicodeString by func { declare(int32, pointer, pointer, pointer, retType = int32) }
    val RtlFindClearBits by func { declare(pointer, int32, int32, retType = int32) }
    val RtlFindClearBitsAndSet by func { declare(pointer, int32, int32, retType = int32) }
    val RtlFindClearRuns by func { declare(pointer, pointer, int32, byte, retType = int32) }
    val RtlFindClosestEncodableLength by func { declare(int64, pointer, retType = int32) }
    val RtlFindExportedRoutineByName by func { declare(pointer, pointer, retType = pointer) }
    val RtlFindLastBackwardRunClear by func { declare(pointer, int32, pointer, retType = int32) }
    val RtlFindLeastSignificantBit by func { declare(int64, retType = byte) }
    val RtlFindLongestRunClear by func { declare(pointer, pointer, retType = int32) }
    val RtlFindMessage by func { declare(pointer, int32, int32, int32, pointer, retType = int32) }
    val RtlFindMostSignificantBit by func { declare(int64, retType = byte) }
    val RtlFindNextForwardRunClear by func { declare(pointer, int32, pointer, retType = int32) }
    val RtlFindSetBits by func { declare(pointer, int32, int32, retType = int32) }
    val RtlFindSetBitsAndClear by func { declare(pointer, int32, int32, retType = int32) }
    val RtlFindSetBitsAndClearEx by func { declare(pointer, int64, int64, retType = int64) }
    val RtlFindUnicodeSubstring by func { declare(pointer, pointer, byte, retType = pointer) }
    val RtlFirstEntrySList by func { declare(pointer, retType = pointer) }
    val RtlFirstFreeAce by func { declare(pointer, pointer, retType = byte) }
    val RtlFlsAlloc by func { declare(pointer, pointer, retType = int32) }
    val RtlFlsFree by func { declare(int32, retType = int32) }
    val RtlFlsGetValue by func { declare(int32, pointer, retType = int32) }
    val RtlFlsGetValue2 by func { declare(int32, retType = pointer) }
    val RtlFlsSetValue by func { declare(int32, pointer, retType = int32) }
    val RtlFlushHeaps by func { declare() }
    val RtlFlushSecureMemoryCache by func { declare(pointer, int64, retType = byte) }
    val RtlFormatCurrentUserKeyPath by func { declare(pointer, retType = int32) }
    val RtlFormatMessage by func { declare(pointer, int32, byte, byte, byte, pointer, pointer, int32, pointer, retType = int32) }
    val RtlFormatMessageEx by func { declare(pointer, int32, byte, byte, byte, pointer, pointer, int32, pointer, pointer, retType = int32) }
    val RtlFreeAnsiString by func { declare(pointer) }
    val RtlFreeHandle by func { declare(pointer, pointer, retType = byte) }
    val RtlFreeHeap by func { declare(handle, int32, pointer, retType = int32) }
    val RtlFreeOemString by func { declare(pointer) }
    val RtlFreeSid by func { declare(pointer, retType = pointer) }
    val RtlFreeUTF8String by func { declare(pointer) }
    val RtlFreeUnicodeString by func { declare(pointer) }
    val RtlFreeUserStack by func { declare(pointer, retType = int32) }
    val RtlGUIDFromString by func { declare(pointer, pointer, retType = int32) }
    val RtlGetAce by func { declare(pointer, int32, pointer, retType = int32) }
    val RtlGetCallersAddress by func { declare(pointer, pointer) }
    val RtlGetCompressionWorkSpaceSize by func { declare(int16, pointer, pointer, retType = int32) }
    val RtlGetControlSecurityDescriptor by func { declare(pointer, pointer, pointer, retType = int32) }
    val RtlGetCriticalSectionRecursionCount by func { declare(pointer, retType = int32) }
    val RtlGetCurrentDirectory_U by func { declare(int32, pointer, retType = int32) }
    val RtlGetCurrentPeb by func { declare(retType = pointer) }
    val RtlGetCurrentProcessorNumberEx by func { declare(pointer) }
    val RtlGetDaclSecurityDescriptor by func { declare(pointer, pointer, pointer, pointer, retType = int32) }
    val RtlGetDeviceFamilyInfoEnum by func { declare(pointer, pointer, pointer) }
    val RtlGetElementGenericTable by func { declare(pointer, int32, retType = pointer) }
    val RtlGetElementGenericTableAvl by func { declare(pointer, int32, retType = pointer) }
    val RtlGetEnabledExtendedFeatures by func { declare(int64, retType = int64) }
    val RtlGetExePath by func { declare(pointer, pointer, retType = int32) }
    val RtlGetExtendedContextLength by func { declare(int32, pointer, retType = int32) }
    val RtlGetExtendedContextLength2 by func { declare(int32, pointer, int64, retType = int32) }
    val RtlGetExtendedFeaturesMask by func { declare(pointer, retType = int64) }
    val RtlGetFileMUIPath by func { declare(int32, pointer, pointer, pointer, pointer, pointer, pointer, retType = int32) }
    val RtlGetFrame by func { declare(retType = pointer) }
    val RtlGetFullPathName_U by func { declare(pointer, int32, pointer, pointer, retType = int32) }
    val RtlGetFullPathName_UstrEx by func { declare(pointer, pointer, pointer, pointer, pointer, pointer, pointer, pointer, retType = int32) }
    val RtlGetGroupSecurityDescriptor by func { declare(pointer, pointer, pointer, retType = int32) }
    val RtlGetInterruptTimePrecise by func { declare(pointer, retType = int64) }
    val RtlGetLastNtStatus by func { declare(retType = int32) }
    val RtlGetLastWin32Error by func { declare(retType = int32) }
    val RtlGetLengthWithoutLastFullDosOrNtPathElement by func { declare(int32, pointer, pointer, retType = int32) }
    val RtlGetLengthWithoutTrailingPathSeperators by func { declare(int32, pointer, pointer, retType = int32) }
    val RtlGetLongestNtPathLength by func { declare(retType = int32) }
    val RtlGetNativeSystemInformation by func { declare(int32, pointer, int32, pointer, retType = int32) }
    val RtlGetNextEntryHashTable by func { declare(pointer, pointer, retType = pointer) }
    val RtlGetNtGlobalFlags by func { declare(retType = int32) }
    val RtlGetNtProductType by func { declare(pointer, retType = byte) }
    val RtlGetNtVersionNumbers by func { declare(pointer, pointer, pointer) }
    val RtlGetOwnerSecurityDescriptor by func { declare(pointer, pointer, pointer, retType = int32) }
    val RtlGetProcessHeaps by func { declare(int32, pointer, retType = int32) }
    val RtlGetProcessPreferredUILanguages by func { declare(int32, pointer, pointer, pointer, retType = int32) }
    val RtlGetProductInfo by func { declare(int32, int32, int32, int32, pointer, retType = byte) }
    val RtlGetReturnAddressHijackTarget by func { declare(retType = int64) }
    val RtlGetSaclSecurityDescriptor by func { declare(pointer, pointer, pointer, pointer, retType = int32) }
    val RtlGetSearchPath by func { declare(pointer, retType = int32) }
    val RtlGetSecurityDescriptorRMControl by func { declare(pointer, pointer, retType = byte) }
    val RtlGetSystemGlobalData by func { declare(int32, pointer, int32, retType = int32) }
    val RtlGetSystemPreferredUILanguages by func { declare(int32, pointer, pointer, pointer, pointer, retType = int32) }
    val RtlGetSystemTimePrecise by func { declare(retType = int64) }
    val RtlGetThreadErrorMode by func { declare(retType = int32) }
    val RtlGetThreadPreferredUILanguages by func { declare(int32, pointer, pointer, pointer, retType = int32) }
    val RtlGetUserInfoHeap by func { declare(handle, int32, pointer, pointer, pointer, retType = byte) }
    val RtlGetUserPreferredUILanguages by func { declare(int32, pointer, pointer, pointer, pointer, retType = int32) }
    val RtlGetVersion by func { declare(pointer, retType = int32) }
    val RtlGrowFunctionTable by func { declare(pointer, int32) }
    val RtlHashUnicodeString by func { declare(pointer, byte, int32, pointer, retType = int32) }
    val RtlIdentifierAuthoritySid by func { declare(pointer, retType = pointer) }
    val RtlIdnToAscii by func { declare(int32, pointer, int32, pointer, pointer, retType = int32) }
    val RtlIdnToNameprepUnicode by func { declare(int32, pointer, int32, pointer, pointer, retType = int32) }
    val RtlIdnToUnicode by func { declare(int32, pointer, int32, pointer, pointer, retType = int32) }
    val RtlImageDirectoryEntryToData by func { declare(pointer, byte, int16, pointer, retType = pointer) }
    val RtlImageNtHeader by func { declare(pointer, retType = pointer) }
    val RtlImageNtHeaderEx by func { declare(int32, pointer, int64, pointer, retType = int32) }
    val RtlImageRvaToSection by func { declare(pointer, pointer, int32, retType = pointer) }
    val RtlImageRvaToVa by func { declare(pointer, pointer, int32, pointer, retType = pointer) }
    val RtlImpersonateSelf by func { declare(int32, retType = int32) }
    val RtlIncrementCorrelationVector by func { declare(pointer, retType = int32) }
    val RtlInitAnsiString by func { declare(pointer, pointer) }
    val RtlInitAnsiStringEx by func { declare(pointer, pointer, retType = int32) }
    val RtlInitCodePageTable by func { declare(pointer, pointer) }
    val RtlInitEnumerationHashTable by func { declare(pointer, pointer, retType = byte) }
    val RtlInitNlsTables by func { declare(pointer, pointer, pointer, pointer) }
    val RtlInitString by func { declare(pointer, pointer) }
    val RtlInitStringEx by func { declare(pointer, pointer, retType = int32) }
    val RtlInitStrongEnumerationHashTable by func { declare(pointer, pointer, retType = byte) }
    val RtlInitUTF8String by func { declare(pointer, pointer) }
    val RtlInitUTF8StringEx by func { declare(pointer, pointer, retType = int32) }
    val RtlInitUnicodeString by func { declare(pointer, pointer) }
    val RtlInitUnicodeStringEx by func { declare(pointer, pointer, retType = int32) }
    val RtlInitWeakEnumerationHashTable by func { declare(pointer, pointer, retType = byte) }
    val RtlInitializeBitMap by func { declare(pointer, pointer, int32) }
    val RtlInitializeConditionVariable by func { declare(pointer) }
    val RtlInitializeContext by func { declare(handle, pointer, pointer, pointer, pointer, retType = int32) }
    val RtlInitializeCorrelationVector by func { declare(pointer, int32, pointer, retType = int32) }
    val RtlInitializeCriticalSection by func { declare(pointer, retType = int32) }
    val RtlInitializeCriticalSectionAndSpinCount by func { declare(pointer, int32, retType = int32) }
    val RtlInitializeCriticalSectionEx by func { declare(pointer, int32, int32, retType = int32) }
    val RtlInitializeExtendedContext by func { declare(pointer, int32, pointer, retType = int32) }
    val RtlInitializeExtendedContext2 by func { declare(pointer, int32, pointer, int64, retType = int32) }
    val RtlInitializeGenericTable by func { declare(pointer, pointer, pointer, pointer, pointer) }
    val RtlInitializeGenericTableAvl by func { declare(pointer, pointer, pointer, pointer, pointer) }
    val RtlInitializeHandleTable by func { declare(int32, int32, pointer) }
    val RtlInitializeResource by func { declare(pointer) }
    val RtlInitializeSListHead by func { declare(pointer) }
    val RtlInitializeSRWLock by func { declare(pointer) }
    val RtlInitializeSid by func { declare(pointer, pointer, byte, retType = int32) }
    val RtlInsertElementGenericTable by func { declare(pointer, pointer, int32, pointer, retType = pointer) }
    val RtlInsertElementGenericTableAvl by func { declare(pointer, pointer, int32, pointer, retType = pointer) }
    val RtlInsertElementGenericTableFull by func { declare(pointer, pointer, int32, pointer, pointer, int32, retType = pointer) }
    val RtlInsertElementGenericTableFullAvl by func { declare(pointer, pointer, int32, pointer, pointer, int32, retType = pointer) }
    val RtlInsertEntryHashTable by func { declare(pointer, pointer, int64, pointer, retType = byte) }
    val RtlInstallFunctionTableCallback by func { declare(int64, int64, int32, pointer, pointer, pointer, retType = byte) }
    val RtlInt64ToUnicodeString by func { declare(int64, int32, pointer, retType = int32) }
    val RtlIntegerToChar by func { declare(int32, int32, int32, pointer, retType = int32) }
    val RtlIntegerToUnicodeString by func { declare(int32, int32, pointer, retType = int32) }
    val RtlInterlockedFlushSList by func { declare(pointer, retType = pointer) }
    val RtlInterlockedPopEntrySList by func { declare(pointer, retType = pointer) }
    val RtlInterlockedPushEntrySList by func { declare(pointer, pointer, retType = pointer) }
    val RtlInterlockedPushListSListEx by func { declare(pointer, pointer, pointer, int32, retType = pointer) }
    val RtlIpv4AddressToStringA by func { declare(pointer, pointer, retType = pointer) }
    val RtlIpv4AddressToStringExA by func { declare(pointer, int16, pointer, pointer, retType = int32) }
    val RtlIpv4AddressToStringExW by func { declare(pointer, int16, pointer, pointer, retType = int32) }
    val RtlIpv4AddressToStringW by func { declare(pointer, pointer, retType = pointer) }
    val RtlIpv4StringToAddressA by func { declare(pointer, byte, pointer, pointer, retType = int32) }
    val RtlIpv4StringToAddressExA by func { declare(pointer, byte, pointer, pointer, retType = int32) }
    val RtlIpv4StringToAddressExW by func { declare(pointer, byte, pointer, pointer, retType = int32) }
    val RtlIpv4StringToAddressW by func { declare(pointer, byte, pointer, pointer, retType = int32) }
    val RtlIpv6AddressToStringA by func { declare(pointer, pointer, retType = pointer) }
    val RtlIpv6AddressToStringExA by func { declare(pointer, int32, int16, pointer, pointer, retType = int32) }
    val RtlIpv6AddressToStringExW by func { declare(pointer, int32, int16, pointer, pointer, retType = int32) }
    val RtlIpv6AddressToStringW by func { declare(pointer, pointer, retType = pointer) }
    val RtlIpv6StringToAddressA by func { declare(pointer, pointer, pointer, retType = int32) }
    val RtlIpv6StringToAddressExA by func { declare(pointer, pointer, pointer, pointer, retType = int32) }
    val RtlIpv6StringToAddressExW by func { declare(pointer, pointer, pointer, pointer, retType = int32) }
    val RtlIpv6StringToAddressW by func { declare(pointer, pointer, pointer, retType = int32) }
    val RtlIsCloudFilesPlaceholder by func { declare(int32, int32, retType = byte) }
    val RtlIsCriticalSectionLocked by func { declare(pointer, retType = int32) }
    val RtlIsCriticalSectionLockedByThread by func { declare(pointer, retType = int32) }
    val RtlIsCurrentProcess by func { declare(handle, retType = byte) }
    val RtlIsCurrentThread by func { declare(handle, retType = byte) }
    val RtlIsCurrentThreadAttachExempt by func { declare(retType = byte) }
    val RtlIsDosDeviceName_U by func { declare(pointer, retType = int32) }
    val RtlIsGenericTableEmpty by func { declare(pointer, retType = byte) }
    val RtlIsGenericTableEmptyAvl by func { declare(pointer, retType = byte) }
    val RtlIsNameInExpression by func { declare(pointer, pointer, byte, pointer, retType = byte) }
    val RtlIsNameLegalDOS8Dot3 by func { declare(pointer, pointer, pointer, retType = byte) }
    val RtlIsNormalizedString by func { declare(int32, pointer, int32, pointer, retType = int32) }
    val RtlIsPartialPlaceholder by func { declare(int32, int32, retType = byte) }
    val RtlIsPartialPlaceholderFileHandle by func { declare(handle, pointer, retType = int32) }
    val RtlIsPartialPlaceholderFileInfo by func { declare(pointer, int32, pointer, retType = int32) }
    val RtlIsProcessorFeaturePresent by func { declare(int32, retType = byte) }
    val RtlIsTextUnicode by func { declare(pointer, int32, pointer, retType = byte) }
    val RtlIsThreadWithinLoaderCallout by func { declare(retType = byte) }
    val RtlIsValidHandle by func { declare(pointer, pointer, retType = byte) }
    val RtlIsValidIndexHandle by func { declare(pointer, int32, pointer, retType = byte) }
    val RtlIsZeroMemory by func { declare(pointer, int64, retType = byte) }
    val RtlLargeIntegerToChar by func { declare(pointer, int32, int32, pointer, retType = int32) }
    val RtlLeaveCriticalSection by func { declare(pointer, retType = int32) }
    val RtlLengthRequiredSid by func { declare(int32, retType = int32) }
    val RtlLengthSecurityDescriptor by func { declare(pointer, retType = int32) }
    val RtlLengthSid by func { declare(pointer, retType = int32) }
    val RtlLengthSidAsUnicodeString by func { declare(pointer, pointer, retType = int32) }
    val RtlLocalTimeToSystemTime by func { declare(pointer, pointer, retType = int32) }
    val RtlLocateExtendedFeature by func { declare(pointer, int32, pointer, retType = pointer) }
    val RtlLocateLegacyContext by func { declare(pointer, pointer, retType = pointer) }
    val RtlLockHeap by func { declare(handle, retType = byte) }
    val RtlLookupAtomInAtomTable by func { declare(pointer, pointer, pointer, retType = int32) }
    val RtlLookupElementGenericTable by func { declare(pointer, pointer, retType = pointer) }
    val RtlLookupElementGenericTableAvl by func { declare(pointer, pointer, retType = pointer) }
    val RtlLookupElementGenericTableFull by func { declare(pointer, pointer, pointer, pointer, retType = pointer) }
    val RtlLookupElementGenericTableFullAvl by func { declare(pointer, pointer, pointer, pointer, retType = pointer) }
    val RtlLookupEntryHashTable by func { declare(pointer, int64, pointer, retType = pointer) }
    val RtlLookupFirstMatchingElementGenericTableAvl by func { declare(pointer, pointer, pointer, retType = pointer) }
    val RtlLookupFunctionEntry by func { declare(int64, pointer, pointer, retType = pointer) }
    val RtlMakeSelfRelativeSD by func { declare(pointer, pointer, pointer, retType = int32) }
    val RtlMapGenericMask by func { declare(pointer, pointer) }
    val RtlMultiAppendUnicodeStringBuffer by func { declare(pointer, int32, pointer, retType = int32) }
    val RtlMultiByteToUnicodeN by func { declare(pointer, int32, pointer, pointer, int32, retType = int32) }
    val RtlMultiByteToUnicodeSize by func { declare(pointer, pointer, int32, retType = int32) }
    val RtlMultipleAllocateHeap by func { declare(handle, int32, int64, int32, pointer, retType = int32) }
    val RtlMultipleFreeHeap by func { declare(handle, int32, int32, pointer, retType = int32) }
    val RtlNewInstanceSecurityObject by func { declare(byte, byte, pointer, pointer, pointer, pointer, pointer, byte, handle, pointer, retType = int32) }
    val RtlNewSecurityGrantedAccess by func { declare(int32, pointer, pointer, handle, pointer, pointer, retType = int32) }
    val RtlNewSecurityObject by func { declare(pointer, pointer, pointer, byte, handle, pointer, retType = int32) }
    val RtlNewSecurityObjectEx by func { declare(pointer, pointer, pointer, pointer, byte, int32, handle, pointer, retType = int32) }
    val RtlNewSecurityObjectWithMultipleInheritance by func { declare(pointer, pointer, pointer, pointer, int32, byte, int32, handle, pointer, retType = int32) }
    val RtlNormalizeProcessParams by func { declare(pointer, retType = pointer) }
    val RtlNormalizeSecurityDescriptor by func { declare(pointer, int32, pointer, pointer, byte, retType = byte) }
    val RtlNormalizeString by func { declare(int32, pointer, int32, pointer, pointer, retType = int32) }
    val RtlNtStatusToDosError by func { declare(int32, retType = int32) }
    val RtlNtStatusToDosErrorNoTeb by func { declare(int32, retType = int32) }
    val RtlNumberGenericTableElements by func { declare(pointer, retType = int32) }
    val RtlNumberGenericTableElementsAvl by func { declare(pointer, retType = int32) }
    val RtlNumberOfClearBits by func { declare(pointer, retType = int32) }
    val RtlNumberOfClearBitsInRange by func { declare(pointer, int32, int32, retType = int32) }
    val RtlNumberOfSetBits by func { declare(pointer, retType = int32) }
    val RtlNumberOfSetBitsInRange by func { declare(pointer, int32, int32, retType = int32) }
    val RtlNumberOfSetBitsUlongPtr by func { declare(int64, retType = int32) }
    val RtlOemStringToUnicodeString by func { declare(pointer, pointer, byte, retType = int32) }
    val RtlOemToUnicodeN by func { declare(pointer, int32, pointer, pointer, int32, retType = int32) }
    val RtlOpenCurrentUser by func { declare(int32, pointer, retType = int32) }
    val RtlOsDeploymentState by func { declare(int32, retType = int32) }
    val RtlPcToFileHeader by func { declare(pointer, pointer, retType = pointer) }
    val RtlPinAtomInAtomTable by func { declare(pointer, int16, retType = int32) }
    val RtlPopFrame by func { declare(pointer) }
    val RtlPrefixString by func { declare(pointer, pointer, byte, retType = byte) }
    val RtlPrefixUnicodeString by func { declare(pointer, pointer, byte, retType = byte) }
    val RtlProcessFlsData by func { declare(pointer, int32) }
    val RtlProtectHeap by func { declare(handle, byte) }
    val RtlPushFrame by func { declare(pointer) }
    val RtlQueryActivationContextApplicationSettings by func { declare(int32, pointer, pointer, pointer, pointer, int64, pointer, retType = int32) }
    val RtlQueryAtomInAtomTable by func { declare(pointer, int16, pointer, pointer, pointer, pointer, retType = int32) }
    val RtlQueryCriticalSectionOwner by func { declare(handle, byte, retType = handle) }
    val RtlQueryDepthSList by func { declare(pointer, retType = int16) }
    val RtlQueryEnvironmentVariable_U by func { declare(pointer, pointer, pointer, retType = int32) }
    val RtlQueryHeapInformation by func { declare(handle, int32, pointer, int64, pointer, retType = int32) }
    val RtlQueryInformationAcl by func { declare(pointer, pointer, int32, int32, retType = int32) }
    val RtlQueryPackageClaims by func { declare(handle, pointer, pointer, pointer, pointer, pointer, pointer, pointer, retType = int32) }
    val RtlQueryPackageIdentity by func { declare(handle, pointer, pointer, pointer, pointer, pointer, retType = int32) }
    val RtlQueryPackageIdentityEx by func { declare(handle, pointer, pointer, pointer, pointer, pointer, pointer, retType = int32) }
    val RtlQueryPerformanceCounter by func { declare(pointer, retType = int32) }
    val RtlQueryPerformanceFrequency by func { declare(pointer, retType = int32) }
    val RtlQueryProcessDebugInformation by func { declare(handle, int32, pointer, retType = int32) }
    val RtlQueryProcessPlaceholderCompatibilityMode by func { declare(retType = byte) }
    val RtlQueryRegistryValueWithFallback by func { declare(handle, handle, pointer, int32, pointer, pointer, pointer, retType = int32) }
    val RtlQueryRegistryValues by func { declare(int32, pointer, pointer, pointer, pointer, retType = int32) }
    val RtlQueryRegistryValuesEx by func { declare(int32, pointer, pointer, pointer, pointer, retType = int32) }
    val RtlQuerySecurityObject by func { declare(pointer, int32, pointer, int32, pointer, retType = int32) }
    val RtlQueryTagHeap by func { declare(handle, int32, int16, byte, pointer, retType = pointer) }
    val RtlQueryThreadPlaceholderCompatibilityMode by func { declare(retType = byte) }
    val RtlQueryThreadProfiling by func { declare(handle, pointer, retType = int32) }
    val RtlQueryTimeZoneInformation by func { declare(pointer, retType = int32) }
    val RtlQueryUnbiasedInterruptTime by func { declare(pointer, retType = int64) }
    val RtlQueryValidationRunlevel by func { declare(pointer, retType = int32) }
    val RtlQueueWorkItem by func { declare(pointer, pointer, int32, retType = int32) }
    val RtlRaiseCustomSystemEventTrigger by func { declare(pointer, retType = int32) }
    val RtlRaiseStatus by func { declare(int32) }
    val RtlRandom by func { declare(pointer, retType = int32) }
    val RtlRandomEx by func { declare(pointer, retType = int32) }
    val RtlRbInsertNodeEx by func { declare(pointer, pointer, byte, pointer, retType = byte) }
    val RtlRbRemoveNode by func { declare(pointer, pointer, retType = byte) }
    val RtlReAllocateHeap by func { declare(handle, int32, pointer, int64, retType = pointer) }
    val RtlRealPredecessor by func { declare(pointer, retType = pointer) }
    val RtlRealSuccessor by func { declare(pointer, retType = pointer) }
    val RtlRegisterSecureMemoryCacheCallback by func { declare(pointer, retType = int32) }
    val RtlRegisterWait by func { declare(pointer, handle, pointer, pointer, int32, int32, retType = int32) }
    val RtlReleasePath by func { declare(pointer) }
    val RtlReleasePebLock by func { declare(retType = int32) }
    val RtlReleasePrivilege by func { declare(pointer) }
    val RtlReleaseRelativeName by func { declare(pointer) }
    val RtlReleaseResource by func { declare(pointer) }
    val RtlReleaseSRWLockExclusive by func { declare(pointer) }
    val RtlReleaseSRWLockShared by func { declare(pointer) }
    val RtlRemoteCall by func { declare(handle, handle, pointer, int32, pointer, byte, byte, retType = int32) }
    val RtlRemoveEntryHashTable by func { declare(pointer, pointer, pointer, retType = byte) }
    val RtlRemoveVectoredContinueHandler by func { declare(pointer, retType = int32) }
    val RtlRemoveVectoredExceptionHandler by func { declare(pointer, retType = int32) }
    val RtlReplaceSidInSd by func { declare(pointer, pointer, pointer, pointer, retType = int32) }
    val RtlReportException by func { declare(pointer, pointer, int32, retType = int32) }
    val RtlResetRtlTranslations by func { declare(pointer) }
    val RtlRestoreLastWin32Error by func { declare(int32) }
    val RtlRunDecodeUnicodeString by func { declare(byte, pointer) }
    val RtlRunEncodeUnicodeString by func { declare(pointer, pointer) }
    val RtlRunOnceBeginInitialize by func { declare(pointer, int32, pointer, retType = int32) }
    val RtlRunOnceComplete by func { declare(pointer, int32, pointer, retType = int32) }
    val RtlRunOnceExecuteOnce by func { declare(pointer, pointer, pointer, pointer, retType = int32) }
    val RtlRunOnceInitialize by func { declare(pointer) }
    val RtlSecondsSince1970ToTime by func { declare(int32, pointer) }
    val RtlSecondsSince1980ToTime by func { declare(int32, pointer) }
    val RtlSelfRelativeToAbsoluteSD by func { declare(pointer, pointer, pointer, pointer, pointer, pointer, pointer, pointer, pointer, pointer, pointer, retType = int32) }
    val RtlSetAllBits by func { declare(pointer) }
    val RtlSetAttributesSecurityDescriptor by func { declare(pointer, int16, pointer, retType = int32) }
    val RtlSetBit by func { declare(pointer, int32) }
    val RtlSetBits by func { declare(pointer, int32, int32) }
    val RtlSetControlSecurityDescriptor by func { declare(pointer, int16, int16, retType = int32) }
    val RtlSetCriticalSectionSpinCount by func { declare(pointer, int32, retType = int32) }
    val RtlSetCurrentDirectory_U by func { declare(pointer, retType = int32) }
    val RtlSetCurrentEnvironment by func { declare(pointer, pointer, retType = int32) }
    val RtlSetDaclSecurityDescriptor by func { declare(pointer, byte, pointer, byte, retType = int32) }
    val RtlSetEnvironmentStrings by func { declare(pointer, int64, retType = int32) }
    val RtlSetEnvironmentVariable by func { declare(pointer, pointer, pointer, retType = int32) }
    val RtlSetExtendedFeaturesMask by func { declare(pointer, int64) }
    val RtlSetGroupSecurityDescriptor by func { declare(pointer, pointer, byte, retType = int32) }
    val RtlSetHeapInformation by func { declare(handle, int32, pointer, int64, retType = int32) }
    val RtlSetInformationAcl by func { declare(pointer, pointer, int32, int32, retType = int32) }
    val RtlSetIoCompletionCallback by func { declare(handle, pointer, int32, retType = int32) }
    val RtlSetLastWin32Error by func { declare(int32) }
    val RtlSetLastWin32ErrorAndNtStatusFromNtStatus by func { declare(int32) }
    val RtlSetOwnerSecurityDescriptor by func { declare(pointer, pointer, byte, retType = int32) }
    val RtlSetProcessIsCritical by func { declare(byte, pointer, byte, retType = int32) }
    val RtlSetProcessPlaceholderCompatibilityMode by func { declare(byte, retType = byte) }
    val RtlSetSaclSecurityDescriptor by func { declare(pointer, byte, pointer, byte, retType = int32) }
    val RtlSetSearchPathMode by func { declare(int32, retType = int32) }
    val RtlSetSecurityDescriptorRMControl by func { declare(pointer, pointer) }
    val RtlSetSecurityObject by func { declare(int32, pointer, pointer, pointer, handle, retType = int32) }
    val RtlSetSecurityObjectEx by func { declare(int32, pointer, pointer, int32, pointer, handle, retType = int32) }
    val RtlSetThreadErrorMode by func { declare(int32, pointer, retType = int32) }
    val RtlSetThreadIsCritical by func { declare(byte, pointer, byte, retType = int32) }
    val RtlSetThreadPlaceholderCompatibilityMode by func { declare(byte, retType = byte) }
    val RtlSetTimeZoneInformation by func { declare(pointer, retType = int32) }
    val RtlSetTimer by func { declare(handle, pointer, pointer, pointer, int32, int32, int32, retType = int32) }
    val RtlSetUnhandledExceptionFilter by func { declare(pointer) }
    val RtlSetUserFlagsHeap by func { declare(handle, int32, pointer, int32, int32, retType = byte) }
    val RtlSetUserValueHeap by func { declare(handle, int32, pointer, pointer, retType = byte) }
    val RtlSizeHeap by func { declare(handle, int32, pointer, retType = int64) }
    val RtlSplay by func { declare(pointer, retType = pointer) }
    val RtlStringFromGUID by func { declare(pointer, pointer, retType = int32) }
    val RtlStronglyEnumerateEntryHashTable by func { declare(pointer, pointer, retType = pointer) }
    val RtlSubAuthorityCountSid by func { declare(pointer, retType = pointer) }
    val RtlSubAuthoritySid by func { declare(pointer, int32, retType = pointer) }
    val RtlSubtreePredecessor by func { declare(pointer, retType = pointer) }
    val RtlSubtreeSuccessor by func { declare(pointer, retType = pointer) }
    val RtlSwitchedVVI by func { declare(pointer, int32, int64, retType = int32) }
    val RtlSystemTimeToLocalTime by func { declare(pointer, pointer, retType = int32) }
    val RtlTestBit by func { declare(pointer, int32, retType = byte) }
    val RtlTimeFieldsToTime by func { declare(pointer, pointer, retType = byte) }
    val RtlTimeToElapsedTimeFields by func { declare(pointer, pointer) }
    val RtlTimeToSecondsSince1970 by func { declare(pointer, pointer, retType = byte) }
    val RtlTimeToSecondsSince1980 by func { declare(pointer, pointer, retType = byte) }
    val RtlTimeToTimeFields by func { declare(pointer, pointer) }
    val RtlTryAcquireSRWLockExclusive by func { declare(pointer, retType = byte) }
    val RtlTryAcquireSRWLockShared by func { declare(pointer, retType = byte) }
    val RtlTryEnterCriticalSection by func { declare(pointer, retType = int32) }
    val RtlUTF8StringToUnicodeString by func { declare(pointer, pointer, byte, retType = int32) }
    val RtlUTF8ToUnicodeN by func { declare(pointer, int32, pointer, pointer, int32, retType = int32) }
    val RtlUnicodeStringToAnsiSize by func { declare(pointer, retType = int32) }
    val RtlUnicodeStringToAnsiString by func { declare(pointer, pointer, byte, retType = int32) }
    val RtlUnicodeStringToCountedOemString by func { declare(pointer, pointer, byte, retType = int32) }
    val RtlUnicodeStringToInteger by func { declare(pointer, int32, pointer, retType = int32) }
    val RtlUnicodeStringToOemString by func { declare(pointer, pointer, byte, retType = int32) }
    val RtlUnicodeStringToUTF8String by func { declare(pointer, pointer, byte, retType = int32) }
    val RtlUnicodeToCustomCPN by func { declare(pointer, pointer, int32, pointer, pointer, int32, retType = int32) }
    val RtlUnicodeToMultiByteN by func { declare(pointer, int32, pointer, pointer, int32, retType = int32) }
    val RtlUnicodeToMultiByteSize by func { declare(pointer, pointer, int32, retType = int32) }
    val RtlUnicodeToOemN by func { declare(pointer, int32, pointer, pointer, int32, retType = int32) }
    val RtlUnicodeToUTF8N by func { declare(pointer, int32, pointer, pointer, int32, retType = int32) }
    val RtlUniform by func { declare(pointer, retType = int32) }
    val RtlUnlockHeap by func { declare(handle, retType = byte) }
    val RtlUpcaseUnicodeChar by func { declare(int16, retType = int16) }
    val RtlUpcaseUnicodeString by func { declare(pointer, pointer, byte, retType = int32) }
    val RtlUpcaseUnicodeStringToAnsiString by func { declare(pointer, pointer, byte, retType = int32) }
    val RtlUpcaseUnicodeStringToCountedOemString by func { declare(pointer, pointer, byte, retType = int32) }
    val RtlUpcaseUnicodeStringToOemString by func { declare(pointer, pointer, byte, retType = int32) }
    val RtlUpcaseUnicodeToCustomCPN by func { declare(pointer, pointer, int32, pointer, pointer, int32, retType = int32) }
    val RtlUpcaseUnicodeToMultiByteN by func { declare(pointer, int32, pointer, pointer, int32, retType = int32) }
    val RtlUpcaseUnicodeToOemN by func { declare(pointer, int32, pointer, pointer, int32, retType = int32) }
    val RtlUpdateTimer by func { declare(handle, handle, int32, int32, retType = int32) }
    val RtlUpperChar by func { declare(byte, retType = byte) }
    val RtlUpperString by func { declare(pointer, pointer) }
    val RtlValidAcl by func { declare(pointer, retType = byte) }
    val RtlValidRelativeSecurityDescriptor by func { declare(pointer, int32, int32, retType = byte) }
    val RtlValidSecurityDescriptor by func { declare(pointer, retType = byte) }
    val RtlValidSid by func { declare(pointer, retType = byte) }
    val RtlValidateCorrelationVector by func { declare(pointer, retType = int32) }
    val RtlValidateHeap by func { declare(handle, int32, pointer, retType = byte) }
    val RtlValidateProcessHeaps by func { declare(retType = byte) }
    val RtlValidateUnicodeString by func { declare(int32, pointer, retType = int32) }
    val RtlVerifyVersionInfo by func { declare(pointer, int32, int64, retType = int32) }
    val RtlVirtualUnwind by func { declare(int32, int64, int64, pointer, pointer, pointer, pointer, pointer, retType = pointer) }
    val RtlWaitOnAddress by func { declare(pointer, pointer, int64, pointer, retType = int32) }
    val RtlWakeAddressAll by func { declare(pointer) }
    val RtlWakeAddressAllNoFence by func { declare(pointer) }
    val RtlWakeAddressSingle by func { declare(pointer) }
    val RtlWakeAddressSingleNoFence by func { declare(pointer) }
    val RtlWakeAllConditionVariable by func { declare(pointer) }
    val RtlWakeConditionVariable by func { declare(pointer) }
    val RtlWalkHeap by func { declare(handle, pointer, retType = int32) }
    val RtlWeaklyEnumerateEntryHashTable by func { declare(pointer, pointer, retType = pointer) }
    val RtlWerpReportException by func { declare(int32, handle, int32, pointer, retType = int32) }
    val RtlWriteRegistryValue by func { declare(int32, pointer, pointer, int32, pointer, int32, retType = int32) }
    val RtlZeroHeap by func { declare(handle, int32, retType = int32) }

    val TpAllocCleanupGroup by func { declare(pointer, retType = int32) }
    val TpAllocIoCompletion by func { declare(pointer, handle, pointer, pointer, pointer, retType = int32) }
    val TpAllocPool by func { declare(pointer, pointer, retType = int32) }
    val TpAllocTimer by func { declare(pointer, pointer, pointer, pointer, retType = int32) }
    val TpAllocWait by func { declare(pointer, pointer, pointer, pointer, retType = int32) }
    val TpAllocWork by func { declare(pointer, pointer, pointer, pointer, retType = int32) }
    val TpCallbackLeaveCriticalSectionOnCompletion by func { declare(pointer, pointer) }
    val TpCallbackMayRunLong by func { declare(pointer, retType = int32) }
    val TpCallbackReleaseMutexOnCompletion by func { declare(pointer, handle) }
    val TpCallbackReleaseSemaphoreOnCompletion by func { declare(pointer, handle, int32) }
    val TpCallbackSetEventOnCompletion by func { declare(pointer, handle) }
    val TpCallbackUnloadDllOnCompletion by func { declare(pointer, pointer) }
    val TpCancelAsyncIoOperation by func { declare(pointer) }
    val TpDisassociateCallback by func { declare(pointer) }
    val TpIsTimerSet by func { declare(pointer, retType = int32) }
    val TpPostWork by func { declare(pointer) }
    val TpQueryPoolStackInformation by func { declare(pointer, pointer, retType = int32) }
    val TpReleaseCleanupGroup by func { declare(pointer) }
    val TpReleaseCleanupGroupMembers by func { declare(pointer, int32, pointer) }
    val TpReleaseIoCompletion by func { declare(pointer) }
    val TpReleasePool by func { declare(pointer) }
    val TpReleaseTimer by func { declare(pointer) }
    val TpReleaseWait by func { declare(pointer) }
    val TpReleaseWork by func { declare(pointer) }
    val TpSetPoolMaxThreads by func { declare(pointer, int32) }
    val TpSetPoolMinThreads by func { declare(pointer, int32, retType = int32) }
    val TpSetPoolStackInformation by func { declare(pointer, pointer, retType = int32) }
    val TpSetTimer by func { declare(pointer, pointer, int32, int32) }
    val TpSetTimerEx by func { declare(pointer, pointer, int32, int32, retType = int32) }
    val TpSetWait by func { declare(pointer, handle, pointer) }
    val TpSetWaitEx by func { declare(pointer, handle, pointer, pointer, retType = int32) }
    val TpSimpleTryPost by func { declare(pointer, pointer, pointer, retType = int32) }
    val TpStartAsyncIoOperation by func { declare(pointer) }
    val TpWaitForIoCompletion by func { declare(pointer, int32) }
    val TpWaitForTimer by func { declare(pointer, int32) }
    val TpWaitForWait by func { declare(pointer, int32) }
    val TpWaitForWork by func { declare(pointer, int32) }

    val VerSetConditionMask by func { declare(int64, int32, byte, retType = int64) }

    val __isascii by func { declare(int32, retType = int32) }
    val __iscsym by func { declare(int32, retType = int32) }
    val __iscsymf by func { declare(int32, retType = int32) }
    val __toascii by func { declare(int32, retType = int32) }
    val _atoi64 by func { declare(pointer, retType = int64) }
    val _i64toa by func { declare(int64, pointer, int32, retType = pointer) }
    val _i64toa_s by func { declare(int64, pointer, int64, int32, retType = int32) }
    val _i64tow by func { declare(int64, pointer, int32, retType = pointer) }
    val _i64tow_s by func { declare(int64, pointer, int64, int32, retType = int32) }
    val _itoa by func { declare(int32, pointer, int32, retType = pointer) }
    val _itoa_s by func { declare(int32, pointer, int64, int32, retType = int32) }
    val _itow by func { declare(int32, pointer, int32, retType = pointer) }
    val _itow_s by func { declare(int32, pointer, int64, int32, retType = int32) }
    val _lfind by func { declare(pointer, pointer, pointer, int32, pointer, retType = pointer) }
    val _ltoa by func { declare(int32, pointer, int32, retType = pointer) }
    val _ltoa_s by func { declare(int32, pointer, int64, int32, retType = int32) }
    val _ltow by func { declare(int32, pointer, int32, retType = pointer) }
    val _ltow_s by func { declare(int32, pointer, int64, int32, retType = int32) }
    val _makepath_s by func { declare(pointer, int64, pointer, pointer, pointer, pointer, retType = int32) }
    val _memccpy by func { declare(pointer, pointer, int32, int64, retType = pointer) }
    val _memicmp by func { declare(pointer, pointer, int64, retType = int32) }
    val _splitpath by func { declare(pointer, pointer, pointer, pointer, pointer) }
    val _splitpath_s by func { declare(pointer, pointer, int64, pointer, int64, pointer, int64, pointer, int64, retType = int32) }
    val _strcmpi by func { declare(pointer, pointer, retType = int32) }
    val _stricmp by func { declare(pointer, pointer, retType = int32) }
    val _strlwr by func { declare(pointer, retType = pointer) }
    val _strlwr_s by func { declare(pointer, int64, retType = int32) }
    val _strnicmp by func { declare(pointer, pointer, int64, retType = int32) }
    val _strupr by func { declare(pointer, retType = pointer) }
    val _strupr_s by func { declare(pointer, int64, retType = int32) }
    val _ui64toa by func { declare(int64, pointer, int32, retType = pointer) }
    val _ui64toa_s by func { declare(int64, pointer, int64, int32, retType = int32) }
    val _ui64tow by func { declare(int64, pointer, int32, retType = pointer) }
    val _ui64tow_s by func { declare(int64, pointer, int64, int32, retType = int32) }
    val _ultoa by func { declare(int32, pointer, int32, retType = pointer) }
    val _ultoa_s by func { declare(int32, pointer, int64, int32, retType = int32) }
    val _ultow by func { declare(int32, pointer, int32, retType = pointer) }
    val _ultow_s by func { declare(int32, pointer, int64, int32, retType = int32) }
    val _wcsicmp by func { declare(pointer, pointer, retType = int32) }
    val _wcslwr by func { declare(pointer, retType = pointer) }
    val _wcslwr_s by func { declare(pointer, int64, retType = int32) }
    val _wcsnicmp by func { declare(pointer, pointer, int64, retType = int32) }
    val _wcstoi64 by func { declare(pointer, pointer, int32, retType = int64) }
    val _wcstoui64 by func { declare(pointer, pointer, int32, retType = int64) }
    val _wcsupr by func { declare(pointer, retType = pointer) }
    val _wcsupr_s by func { declare(pointer, int64, retType = int32) }
    val _wmakepath_s by func { declare(pointer, int64, pointer, pointer, pointer, pointer, retType = int32) }
    val _wsplitpath_s by func { declare(pointer, pointer, int64, pointer, int64, pointer, int64, pointer, int64, retType = int32) }
    val _wtoi by func { declare(pointer, retType = int32) }
    val _wtoi64 by func { declare(pointer, retType = int64) }
    val _wtol by func { declare(pointer, retType = int32) }
    val abs by func { declare(int32, retType = int32) }
    val atan by func { declare(float64, retType = float64) }
    val atan2 by func { declare(float64, float64, retType = float64) }
    val atoi by func { declare(pointer, retType = int32) }
    val atol by func { declare(pointer, retType = int32) }
    val bsearch by func { declare(pointer, pointer, int64, int64, pointer, retType = pointer) }
    val bsearch_s by func { declare(pointer, pointer, int64, int64, pointer, pointer, retType = pointer) }
    val ceil by func { declare(float64, retType = float64) }
    val cos by func { declare(float64, retType = float64) }
    val fabs by func { declare(float64, retType = float64) }
    val floor by func { declare(float64, retType = float64) }
    val isalnum by func { declare(int32, retType = int32) }
    val isalpha by func { declare(int32, retType = int32) }
    val iscntrl by func { declare(int32, retType = int32) }
    val isdigit by func { declare(int32, retType = int32) }
    val isgraph by func { declare(int32, retType = int32) }
    val islower by func { declare(int32, retType = int32) }
    val isprint by func { declare(int32, retType = int32) }
    val ispunct by func { declare(int32, retType = int32) }
    val isspace by func { declare(int32, retType = int32) }
    val isupper by func { declare(int32, retType = int32) }
    val iswalnum by func { declare(int16, retType = int32) }
    val iswalpha by func { declare(int16, retType = int32) }
    val iswascii by func { declare(int16, retType = int32) }
    val iswctype by func { declare(int16, int16, retType = int32) }
    val iswdigit by func { declare(int16, retType = int32) }
    val iswgraph by func { declare(int16, retType = int32) }
    val iswlower by func { declare(int16, retType = int32) }
    val iswprint by func { declare(int16, retType = int32) }
    val iswspace by func { declare(int16, retType = int32) }
    val iswxdigit by func { declare(int16, retType = int32) }
    val isxdigit by func { declare(int32, retType = int32) }
    val labs by func { declare(int32, retType = int32) }
    val log by func { declare(float64, retType = float64) }
    val mbstowcs by func { declare(pointer, pointer, int64, retType = int64) }
    val memchr by func { declare(pointer, int32, int64, retType = pointer) }
    val memcmp by func { declare(pointer, pointer, int64, retType = int32) }
    val memcpy by func { declare(pointer, pointer, int64, retType = pointer) }
    val memmove by func { declare(pointer, pointer, int64, retType = pointer) }
    val memset by func { declare(pointer, int32, int64, retType = pointer) }
    val pow by func { declare(float64, float64, retType = float64) }
    val qsort by func { declare(pointer, int64, int64, pointer) }
    val qsort_s by func { declare(pointer, int64, int64, pointer, pointer) }
    val sin by func { declare(float64, retType = float64) }
    val sqrt by func { declare(float64, retType = float64) }
    val strcat by func { declare(pointer, pointer, retType = pointer) }
    val strcat_s by func { declare(pointer, int64, pointer, retType = int32) }
    val strchr by func { declare(pointer, int32, retType = pointer) }
    val strcmp by func { declare(pointer, pointer, retType = int32) }
    val strcpy by func { declare(pointer, pointer, retType = pointer) }
    val strcpy_s by func { declare(pointer, int64, pointer, retType = int32) }
    val strcspn by func { declare(pointer, pointer, retType = int64) }
    val strlen by func { declare(pointer, retType = int64) }
    val strncat by func { declare(pointer, pointer, int64, retType = pointer) }
    val strncat_s by func { declare(pointer, int64, pointer, int64, retType = int32) }
    val strncmp by func { declare(pointer, pointer, int64, retType = int32) }
    val strncpy by func { declare(pointer, pointer, int64, retType = pointer) }
    val strncpy_s by func { declare(pointer, int64, pointer, int64, retType = int32) }
    val strnlen by func { declare(pointer, int64, retType = int64) }
    val strpbrk by func { declare(pointer, pointer, retType = pointer) }
    val strrchr by func { declare(pointer, int32, retType = pointer) }
    val strspn by func { declare(pointer, pointer, retType = int64) }
    val strstr by func { declare(pointer, pointer, retType = pointer) }
    val strtok_s by func { declare(pointer, pointer, pointer, retType = pointer) }
    val strtol by func { declare(pointer, pointer, int32, retType = int32) }
    val strtoul by func { declare(pointer, pointer, int32, retType = int32) }
    val tan by func { declare(float64, retType = float64) }
    val tolower by func { declare(int32, retType = int32) }
    val toupper by func { declare(int32, retType = int32) }
    val towlower by func { declare(int16, retType = int16) }
    val towupper by func { declare(int16, retType = int16) }
    val wcscat by func { declare(pointer, pointer, retType = pointer) }
    val wcscat_s by func { declare(pointer, int64, pointer, retType = int32) }
    val wcschr by func { declare(pointer, int16, retType = pointer) }
    val wcscmp by func { declare(pointer, pointer, retType = int32) }
    val wcscpy by func { declare(pointer, pointer, retType = pointer) }
    val wcscpy_s by func { declare(pointer, int64, pointer, retType = int32) }
    val wcscspn by func { declare(pointer, pointer, retType = int64) }
    val wcslen by func { declare(pointer, retType = int64) }
    val wcsncat by func { declare(pointer, pointer, int64, retType = pointer) }
    val wcsncat_s by func { declare(pointer, int64, pointer, int64, retType = int32) }
    val wcsncmp by func { declare(pointer, pointer, int64, retType = int32) }
    val wcsncpy by func { declare(pointer, pointer, int64, retType = pointer) }
    val wcsncpy_s by func { declare(pointer, int64, pointer, int64, retType = int32) }
    val wcsnlen by func { declare(pointer, int64, retType = int64) }
    val wcspbrk by func { declare(pointer, pointer, retType = pointer) }
    val wcsrchr by func { declare(pointer, int16, retType = pointer) }
    val wcsspn by func { declare(pointer, pointer, retType = int64) }
    val wcsstr by func { declare(pointer, pointer, retType = pointer) }
    val wcstok_s by func { declare(pointer, pointer, pointer, retType = pointer) }
    val wcstol by func { declare(pointer, pointer, int32, retType = int32) }
    val wcstombs by func { declare(pointer, pointer, int64, retType = int64) }
    val wcstoul by func { declare(pointer, pointer, int32, retType = int32) }
}
