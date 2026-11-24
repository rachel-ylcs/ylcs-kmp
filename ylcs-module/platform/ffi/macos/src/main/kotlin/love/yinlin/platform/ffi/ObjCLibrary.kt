package love.yinlin.platform.ffi

import java.lang.foreign.FunctionDescriptor

@Suppress("UnusedReceiverParameter")
inline val FFI.objc: ObjCLibrary get() = ObjCLibrary.Instance

@Suppress("PropertyName", "unused", "SpellCheckingInspection")
class ObjCLibrary private constructor() : NativeLibrary("/usr/lib/libobjc.A.dylib") {
    @PublishedApi
    internal companion object {
        @PublishedApi
        internal val Instance = ObjCLibrary()

        private val byte = NativeType.Byte
        private val boolean = NativeType.Boolean
        private val int32 = NativeType.Int
        private val int64 = NativeType.Long
        private val pointer = NativeType.Pointer
    }

    private val objcMsgSend by lazy { lookup("objc_msgSend") }

    fun msgSend(descriptor: NativeLibrary.() -> FunctionDescriptor) = lazy { downcall(objcMsgSend, descriptor()) }

    val NXCompareHashTables by func { declare(pointer, pointer, retType = boolean) }
    val NXCopyHashTable by func { declare(pointer, retType = pointer) }
    val NXCountHashTable by func { declare(pointer, retType = int32) }
    val NXCreateHashTable by func { declare(MacOSDef.NXHashTablePrototype, int32, pointer, retType = pointer) }
    val NXCreateHashTableFromZone by func { declare(MacOSDef.NXHashTablePrototype, int32, pointer, pointer, retType = pointer) }
    val NXEmptyHashTable by func { declare(pointer) }
    val NXFreeHashTable by func { declare(pointer) }
    val NXHashGet by func { declare(pointer, pointer, retType = pointer) }
    val NXHashInsert by func { declare(pointer, pointer, retType = pointer) }
    val NXHashInsertIfAbsent by func { declare(pointer, pointer, retType = pointer) }
    val NXHashMember by func { declare(pointer, pointer, retType = int32) }
    val NXHashRemove by func { declare(pointer, pointer, retType = pointer) }
    val NXInitHashState by func { declare(pointer, retType = MacOSDef.NXHashState) }
    val NXNextHashState by func { declare(pointer, pointer, pointer, retType = int32) }
    val NXNoEffectFree by func { declare(pointer, pointer) }
    val NXPtrHash by func { declare(pointer, pointer, retType = int64) }
    val NXPtrIsEqual by func { declare(pointer, pointer, pointer, retType = int32) }
    val NXReallyFree by func { declare(pointer, pointer) }
    val NXResetHashTable by func { declare(pointer) }
    val NXStrHash by func { declare(pointer, pointer, retType = int64) }
    val NXStrIsEqual by func { declare(pointer, pointer, pointer, retType = int32) }
    val _objc_flush_caches by func { declare(pointer) }
    val _objc_msgForward by func { declare() }
    val _objc_realizeClassFromSwift by func { declare(pointer, pointer, retType = pointer) }
    val class_addIvar by func { declare(pointer, pointer, int64, byte, pointer, retType = boolean) }
    val class_addMethod by func { declare(pointer, pointer, pointer, pointer, retType = boolean) }
    val class_addProperty by func { declare(pointer, pointer, pointer, int32, retType = boolean) }
    val class_addProtocol by func { declare(pointer, pointer, retType = boolean) }
    val class_conformsToProtocol by func { declare(pointer, pointer, retType = boolean) }
    val class_copyIvarList by func { declare(pointer, pointer, retType = pointer) }
    val class_copyMethodList by func { declare(pointer, pointer, retType = pointer) }
    val class_copyPropertyList by func { declare(pointer, pointer, retType = pointer) }
    val class_copyProtocolList by func { declare(pointer, pointer, retType = pointer) }
    val class_createInstance by func { declare(pointer, int64, retType = pointer) }
    val class_createInstanceFromZone by func { declare(pointer, int64, pointer, retType = pointer) }
    val class_getClassMethod by func { declare(pointer, pointer, retType = pointer) }
    val class_getClassVariable by func { declare(pointer, pointer, retType = pointer) }
    val class_getImageName by func { declare(pointer, retType = pointer) }
    val class_getInstanceMethod by func { declare(pointer, pointer, retType = pointer) }
    val class_getInstanceSize by func { declare(pointer, retType = int64) }
    val class_getInstanceVariable by func { declare(pointer, pointer, retType = pointer) }
    val class_getIvarLayout by func { declare(pointer, retType = pointer) }
    val class_getMethodImplementation by func { declare(pointer, pointer, retType = pointer) }
    val class_getName by func { declare(pointer, retType = pointer) }
    val class_getProperty by func { declare(pointer, pointer, retType = pointer) }
    val class_getSuperclass by func { declare(pointer, retType = pointer) }
    val class_getVersion by func { declare(pointer, retType = int32) }
    val class_getWeakIvarLayout by func { declare(pointer, retType = pointer) }
    val class_isMetaClass by func { declare(pointer, retType = boolean) }
    val class_lookupMethod by func { declare(pointer, pointer, retType = pointer) }
    val class_replaceMethod by func { declare(pointer, pointer, pointer, pointer, retType = pointer) }
    val class_replaceProperty by func { declare(pointer, pointer, pointer, int32) }
    val class_respondsToMethod by func { declare(pointer, pointer, retType = boolean) }
    val class_respondsToSelector by func { declare(pointer, pointer, retType = boolean) }
    val class_setIvarLayout by func { declare(pointer, pointer) }
    val class_setSuperclass by func { declare(pointer, pointer, retType = pointer) }
    val class_setVersion by func { declare(pointer, int32) }
    val class_setWeakIvarLayout by func { declare(pointer, pointer) }
    val imp_getBlock by func { declare(pointer, retType = pointer) }
    val imp_implementationWithBlock by func { declare(pointer, retType = pointer) }
    val imp_removeBlock by func { declare(pointer, retType = boolean) }
    val ivar_getName by func { declare(pointer, retType = pointer) }
    val ivar_getOffset by func { declare(pointer, retType = int64) }
    val ivar_getTypeEncoding by func { declare(pointer, retType = pointer) }
    val method_copyArgumentType by func { declare(pointer, int32, retType = pointer) }
    val method_copyReturnType by func { declare(pointer, retType = pointer) }
    val method_exchangeImplementations by func { declare(pointer, pointer) }
    val method_getArgumentType by func { declare(pointer, int32, pointer, int64) }
    val method_getDescription by func { declare(pointer, retType = pointer) }
    val method_getImplementation by func { declare(pointer, retType = pointer) }
    val method_getName by func { declare(pointer, retType = pointer) }
    val method_getNumberOfArguments by func { declare(pointer, retType = int32) }
    val method_getReturnType by func { declare(pointer, pointer, int64) }
    val method_getTypeEncoding by func { declare(pointer, retType = pointer) }
    val method_setImplementation by func { declare(pointer, pointer, retType = pointer) }
    val objc_addExceptionHandler by func { declare(pointer, pointer, retType = int64) }
    val objc_addLoadImageFunc by func { declare(pointer) }
    val objc_allocateClassPair by func { declare(pointer, pointer, int64, retType = pointer) }
    val objc_allocateProtocol by func { declare(pointer, retType = pointer) }
    val objc_autoreleasePoolPop by func { declare(pointer) }
    val objc_autoreleasePoolPush by func { declare(retType = pointer) }
    val objc_begin_catch by func { declare(pointer, retType = pointer) }
    val objc_copyClassList by func { declare(pointer, retType = pointer) }
    val objc_copyClassNamesForImage by func { declare(pointer, pointer, retType = pointer) }
    val objc_copyImageNames by func { declare(pointer, retType = pointer) }
    val objc_copyProtocolList by func { declare(pointer, retType = pointer) }
    val objc_disposeClassPair by func { declare(pointer) }
    val objc_duplicateClass by func { declare(pointer, pointer, int64, retType = pointer) }
    val objc_end_catch by func { declare() }
    val objc_enumerationMutation by func { declare(pointer) }
    val objc_getAssociatedObject by func { declare(pointer, pointer, retType = pointer) }
    val objc_getClass by func { declare(pointer, retType = pointer) }
    val objc_getClassList by func { declare(pointer, int32, retType = int32) }
    val objc_getMetaClass by func { declare(pointer, retType = pointer) }
    val objc_getProtocol by func { declare(pointer, retType = pointer) }
    val objc_getRequiredClass by func { declare(pointer, retType = pointer) }
    val objc_loadWeak by func { declare(pointer, retType = pointer) }
    val objc_lookUpClass by func { declare(pointer, retType = pointer) }
    val objc_registerClassPair by func { declare(pointer) }
    val objc_registerProtocol by func { declare(pointer) }
    val objc_removeAssociatedObjects by func { declare(pointer) }
    val objc_removeExceptionHandler by func { declare(int64) }
    val objc_setAssociatedObject by func { declare(pointer, pointer, pointer, int64) }
    val objc_setEnumerationMutationHandler by func { declare(pointer) }
    val objc_setExceptionMatcher by func { declare(pointer, retType = pointer) }
    val objc_setExceptionPreprocessor by func { declare(pointer, retType = pointer) }
    val objc_setForwardHandler by func { declare(pointer, pointer) }
    val objc_setHook_getClass by func { declare(pointer, pointer) }
    val objc_setHook_getImageName by func { declare(pointer, pointer) }
    val objc_setHook_lazyClassNamer by func { declare(pointer, pointer) }
    val objc_setUncaughtExceptionHandler by func { declare(pointer, retType = pointer) }
    val objc_storeWeak by func { declare(pointer, pointer, retType = pointer) }
    val objc_sync_enter by func { declare(pointer, retType = int32) }
    val objc_sync_exit by func { declare(pointer, retType = int32) }
    val objc_terminate by func { declare() }
    val object_copyFromZone by func { declare(pointer, int64, pointer, retType = pointer) }
    val object_getClass by func { declare(pointer, retType = pointer) }
    val object_getIvar by func { declare(pointer, pointer, retType = pointer) }
    val object_isClass by func { declare(pointer, retType = boolean) }
    val object_setClass by func { declare(pointer, pointer, retType = pointer) }
    val object_setIvar by func { declare(pointer, pointer, pointer) }
    val object_setIvarWithStrongDefault by func { declare(pointer, pointer, pointer) }
    val property_copyAttributeList by func { declare(pointer, pointer, retType = pointer) }
    val property_copyAttributeValue by func { declare(pointer, pointer, retType = pointer) }
    val property_getAttributes by func { declare(pointer, retType = pointer) }
    val property_getName by func { declare(pointer, retType = pointer) }
    val protocol_addMethodDescription by func { declare(pointer, pointer, pointer, boolean, boolean) }
    val protocol_addProperty by func { declare(pointer, pointer, pointer, int32, boolean, boolean) }
    val protocol_addProtocol by func { declare(pointer, pointer) }
    val protocol_conformsToProtocol by func { declare(pointer, pointer, retType = boolean) }
    val protocol_copyMethodDescriptionList by func { declare(pointer, boolean, boolean, pointer, retType = pointer) }
    val protocol_copyPropertyList by func { declare(pointer, pointer, retType = pointer) }
    val protocol_copyPropertyList2 by func { declare(pointer, pointer, boolean, boolean, retType = pointer) }
    val protocol_copyProtocolList by func { declare(pointer, pointer, retType = pointer) }
    val protocol_getMethodDescription by func { declare(pointer, pointer, boolean, boolean, retType = MacOSDef.ObjCMethodDescription) }
    val protocol_getName by func { declare(pointer, retType = pointer) }
    val protocol_getProperty by func { declare(pointer, pointer, boolean, boolean, retType = pointer) }
    val protocol_isEqual by func { declare(pointer, pointer, retType = boolean) }
    val sel_getName by func { declare(pointer, retType = pointer) }
    val sel_isEqual by func { declare(pointer, pointer, retType = boolean) }
    val sel_registerName by func { declare(pointer, retType = pointer) }
}
