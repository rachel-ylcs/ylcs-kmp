package love.yinlin.platform.ffi

@Suppress("UnusedReceiverParameter")
inline val FFI.applicationServices: ApplicationServicesLibrary get() = ApplicationServicesLibrary.Instance

@Suppress("PropertyName", "unused", "SpellCheckingInspection")
class ApplicationServicesLibrary private constructor() : NativeLibrary("/System/Library/Frameworks/ApplicationServices.framework/ApplicationServices") {
    @PublishedApi
    internal companion object {
        @PublishedApi
        internal val Instance = ApplicationServicesLibrary()

        private val pointer = NativeType.Pointer
        private val byte = NativeType.Byte
        private val int32 = NativeType.Int
        private val int64 = NativeType.Long
        private val float32 = NativeType.Float
    }

    val AXIsProcessTrusted by func { declare(retType = byte) }
    val AXIsProcessTrustedWithOptions by func { declare(pointer, retType = byte) }
    val AXObserverAddNotification by func { declare(pointer, pointer, pointer, pointer, retType = int32) }
    val AXObserverCreate by func { declare(int32, pointer, pointer, retType = int32) }
    val AXObserverCreateWithInfoCallback by func { declare(int32, pointer, pointer, retType = int32) }
    val AXObserverGetRunLoopSource by func { declare(pointer, retType = pointer) }
    val AXObserverGetTypeID by func { declare(retType = int64) }
    val AXObserverRemoveNotification by func { declare(pointer, pointer, pointer, retType = int32) }
    val AXUIElementCopyActionDescription by func { declare(pointer, pointer, pointer, retType = int32) }
    val AXUIElementCopyActionNames by func { declare(pointer, pointer, retType = int32) }
    val AXUIElementCopyAttributeNames by func { declare(pointer, pointer, retType = int32) }
    val AXUIElementCopyAttributeValue by func { declare(pointer, pointer, pointer, retType = int32) }
    val AXUIElementCopyAttributeValues by func { declare(pointer, pointer, int64, int64, pointer, retType = int32) }
    val AXUIElementCopyElementAtPosition by func { declare(pointer, float32, float32, pointer, retType = int32) }
    val AXUIElementCopyMultipleAttributeValues by func { declare(pointer, pointer, int32, pointer, retType = int32) }
    val AXUIElementCopyParameterizedAttributeNames by func { declare(pointer, pointer, retType = int32) }
    val AXUIElementCopyParameterizedAttributeValue by func { declare(pointer, pointer, pointer, pointer, retType = int32) }
    val AXUIElementCreateApplication by func { declare(int32, retType = pointer) }
    val AXUIElementCreateSystemWide by func { declare(retType = pointer) }
    val AXUIElementGetAttributeValueCount by func { declare(pointer, pointer, pointer, retType = int32) }
    val AXUIElementGetPid by func { declare(pointer, pointer, retType = int32) }
    val AXUIElementGetTypeID by func { declare(retType = int64) }
    val AXUIElementIsAttributeSettable by func { declare(pointer, pointer, pointer, retType = int32) }
    val AXUIElementPerformAction by func { declare(pointer, pointer, retType = int32) }
    val AXUIElementSetAttributeValue by func { declare(pointer, pointer, pointer, retType = int32) }
    val AXUIElementSetMessagingTimeout by func { declare(pointer, float32, retType = int32) }
    val AXValueCreate by func { declare(int32, pointer, retType = pointer) }
    val AXValueGetType by func { declare(pointer, retType = int32) }
    val AXValueGetTypeID by func { declare(retType = int64) }
    val AXValueGetValue by func { declare(pointer, int32, pointer, retType = byte) }
    val PMCreatePageFormat by func { declare(pointer, retType = int32) }
    val PMCreatePrintSettings by func { declare(pointer, retType = int32) }
    val PMCreateSession by func { declare(pointer, retType = int32) }
    val PMGetAdjustedPageRect by func { declare(pointer, pointer, retType = int32) }
    val PMGetAdjustedPaperRect by func { declare(pointer, pointer, retType = int32) }
    val PMGetCopies by func { declare(pointer, pointer, retType = int32) }
    val PMGetUnadjustedPageRect by func { declare(pointer, pointer, retType = int32) }
    val PMGetUnadjustedPaperRect by func { declare(pointer, pointer, retType = int32) }
    val PMPrinterGetID by func { declare(pointer, retType = pointer) }
    val PMPrinterGetName by func { declare(pointer, retType = pointer) }
    val PMRelease by func { declare(pointer, retType = int32) }
    val PMRetain by func { declare(pointer, retType = int32) }
    val PMSessionCreatePrinterList by func { declare(pointer, pointer, pointer, pointer, retType = int32) }
    val PMSessionDefaultPageFormat by func { declare(pointer, pointer, retType = int32) }
    val PMSessionDefaultPrintSettings by func { declare(pointer, pointer, retType = int32) }
    val PMSessionGetCurrentPrinter by func { declare(pointer, pointer, retType = int32) }
    val PMSetCopies by func { declare(pointer, int32, byte, retType = int32) }
}
