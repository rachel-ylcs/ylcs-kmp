package love.yinlin.platform.ffi

@Suppress("UnusedReceiverParameter")
inline val FFI.foundation: FoundationLibrary get() = FoundationLibrary.Instance

@Suppress("PropertyName", "unused", "SpellCheckingInspection")
class FoundationLibrary private constructor() : NativeLibrary("/System/Library/Frameworks/Foundation.framework/Foundation") {
    @PublishedApi
    internal companion object {
        @PublishedApi
        internal val Instance = FoundationLibrary()

        private val boolean = NativeType.Boolean
        private val int16 = NativeType.Short
        private val int32 = NativeType.Int
        private val int64 = NativeType.Long
        private val float64 = NativeType.Double
        private val pointer = NativeType.Pointer
    }

    val NSAllHashTableObjects by func { declare(pointer, retType = pointer) }
    val NSAllMapTableKeys by func { declare(pointer, retType = pointer) }
    val NSAllMapTableValues by func { declare(pointer, retType = pointer) }
    val NSAllocateCollectable by func { declare(int64, int64, retType = pointer) }
    val NSAllocateMemoryPages by func { declare(int64, retType = pointer) }
    val NSClassFromString by func { declare(pointer, retType = pointer) }
    val NSCompareHashTables by func { declare(pointer, pointer, retType = boolean) }
    val NSCompareMapTables by func { declare(pointer, pointer, retType = boolean) }
    val NSContainsRect by func { declare(MacOSDef.CGRect, MacOSDef.CGRect, retType = boolean) }
    val NSCopyHashTableWithZone by func { declare(pointer, pointer, retType = pointer) }
    val NSCopyMapTableWithZone by func { declare(pointer, pointer, retType = pointer) }
    val NSCopyMemoryPages by func { declare(pointer, pointer, int64) }
    val NSCountFrames by func { declare(retType = int64) }
    val NSCountHashTable by func { declare(pointer, retType = int64) }
    val NSCountMapTable by func { declare(pointer, retType = int64) }
    val NSCreateHashTable by func { declare(MacOSDef.NSHashTableCallBacks, int64, retType = pointer) }
    val NSCreateHashTableWithZone by func { declare(MacOSDef.NSHashTableCallBacks, int64, pointer, retType = pointer) }
    val NSCreateMapTable by func { declare(MacOSDef.NSMapTableKeyCallBacks, MacOSDef.NSMapTableValueCallBacks, int64, retType = pointer) }
    val NSCreateMapTableWithZone by func { declare(MacOSDef.NSMapTableKeyCallBacks, MacOSDef.NSMapTableValueCallBacks, int64, pointer, retType = pointer) }
    val NSCreateZone by func { declare(int64, int64, boolean, retType = pointer) }
    val NSDeallocateMemoryPages by func { declare(pointer, int64) }
    val NSDecimalAdd by func { declare(pointer, pointer, pointer, int64, retType = int64) }
    val NSDecimalCompact by func { declare(pointer) }
    val NSDecimalCompare by func { declare(pointer, pointer, retType = int64) }
    val NSDecimalCopy by func { declare(pointer, pointer) }
    val NSDecimalDivide by func { declare(pointer, pointer, pointer, int64, retType = int64) }
    val NSDecimalMultiply by func { declare(pointer, pointer, pointer, int64, retType = int64) }
    val NSDecimalMultiplyByPowerOf10 by func { declare(pointer, pointer, int16, int64, retType = int64) }
    val NSDecimalNormalize by func { declare(pointer, pointer, int64, retType = int64) }
    val NSDecimalPower by func { declare(pointer, pointer, int64, int64, retType = int64) }
    val NSDecimalRound by func { declare(pointer, pointer, int64, int64) }
    val NSDecimalString by func { declare(pointer, pointer, retType = pointer) }
    val NSDecimalSubtract by func { declare(pointer, pointer, pointer, int64, retType = int64) }
    val NSDefaultMallocZone by func { declare(retType = pointer) }
    val NSDivideRect by func { declare(MacOSDef.CGRect, pointer, pointer, float64, int64) }
    val NSEdgeInsetsEqual by func { declare(MacOSDef.NSEdgeInsets, MacOSDef.NSEdgeInsets, retType = boolean) }
    val NSEndHashTableEnumeration by func { declare(pointer) }
    val NSEndMapTableEnumeration by func { declare(pointer) }
    val NSEnumerateHashTable by func { declare(pointer, retType = MacOSDef.NSHashEnumerator) }
    val NSEnumerateMapTable by func { declare(pointer, retType = MacOSDef.NSMapEnumerator) }
    val NSEqualPoints by func { declare(MacOSDef.CGPoint, MacOSDef.CGPoint, retType = boolean) }
    val NSEqualRects by func { declare(MacOSDef.CGRect, MacOSDef.CGRect, retType = boolean) }
    val NSEqualSizes by func { declare(MacOSDef.CGSize, MacOSDef.CGSize, retType = boolean) }
    val NSFileTypeForHFSTypeCode by func { declare(int32, retType = pointer) }
    val NSFrameAddress by func { declare(int64, retType = pointer) }
    val NSFreeHashTable by func { declare(pointer) }
    val NSFreeMapTable by func { declare(pointer) }
    val NSFullUserName by func { declare(retType = pointer) }
    val NSGetUncaughtExceptionHandler by func { declare(retType = pointer) }
    val NSHFSTypeCodeFromFileType by func { declare(pointer, retType = int32) }
    val NSHFSTypeOfFile by func { declare(pointer, retType = pointer) }
    val NSHashGet by func { declare(pointer, pointer, retType = pointer) }
    val NSHashInsert by func { declare(pointer, pointer) }
    val NSHashInsertIfAbsent by func { declare(pointer, pointer, retType = pointer) }
    val NSHashInsertKnownAbsent by func { declare(pointer, pointer) }
    val NSHashRemove by func { declare(pointer, pointer) }
    val NSHomeDirectory by func { declare(retType = pointer) }
    val NSHomeDirectoryForUser by func { declare(pointer, retType = pointer) }
    val NSInsetRect by func { declare(MacOSDef.CGRect, float64, float64, retType = MacOSDef.CGRect) }
    val NSIntegralRect by func { declare(MacOSDef.CGRect, retType = MacOSDef.CGRect) }
    val NSIntegralRectWithOptions by func { declare(MacOSDef.CGRect, int64, retType = MacOSDef.CGRect) }
    val NSIntersectionRange by func { declare(MacOSDef.NSRange, MacOSDef.NSRange, retType = MacOSDef.NSRange) }
    val NSIntersectionRect by func { declare(MacOSDef.CGRect, MacOSDef.CGRect, retType = MacOSDef.CGRect) }
    val NSIntersectsRect by func { declare(MacOSDef.CGRect, MacOSDef.CGRect, retType = boolean) }
    val NSIsEmptyRect by func { declare(MacOSDef.CGRect, retType = boolean) }
    val NSIsFreedObject by func { declare(pointer, retType = boolean) }
    val NSLogPageSize by func { declare(retType = int64) }
    val NSMapGet by func { declare(pointer, pointer, retType = pointer) }
    val NSMapInsert by func { declare(pointer, pointer, pointer) }
    val NSMapInsertIfAbsent by func { declare(pointer, pointer, pointer, retType = pointer) }
    val NSMapInsertKnownAbsent by func { declare(pointer, pointer, pointer) }
    val NSMapMember by func { declare(pointer, pointer, pointer, pointer, retType = boolean) }
    val NSMapRemove by func { declare(pointer, pointer) }
    val NSMouseInRect by func { declare(MacOSDef.CGPoint, MacOSDef.CGRect, boolean, retType = boolean) }
    val NSNextHashEnumeratorItem by func { declare(pointer, retType = pointer) }
    val NSNextMapEnumeratorPair by func { declare(pointer, pointer, pointer, retType = boolean) }
    val NSOffsetRect by func { declare(MacOSDef.CGRect, float64, float64, retType = MacOSDef.CGRect) }
    val NSOpenStepRootDirectory by func { declare(retType = pointer) }
    val NSPageSize by func { declare(retType = int64) }
    val NSPointFromString by func { declare(pointer, retType = MacOSDef.CGPoint) }
    val NSPointInRect by func { declare(MacOSDef.CGPoint, MacOSDef.CGRect, retType = boolean) }
    val NSProtocolFromString by func { declare(pointer, retType = pointer) }
    val NSRangeFromString by func { declare(pointer, retType = MacOSDef.NSRange) }
    val NSRealMemoryAvailable by func { declare(retType = int64) }
    val NSReallocateCollectable by func { declare(pointer, int64, int64, retType = pointer) }
    val NSRecordAllocationEvent by func { declare(int32, pointer) }
    val NSRectFromString by func { declare(pointer, retType = MacOSDef.CGRect) }
    val NSRecycleZone by func { declare(pointer) }
    val NSResetHashTable by func { declare(pointer) }
    val NSResetMapTable by func { declare(pointer) }
    val NSReturnAddress by func { declare(int64, retType = pointer) }
    val NSRoundDownToMultipleOfPageSize by func { declare(int64, retType = int64) }
    val NSRoundUpToMultipleOfPageSize by func { declare(int64, retType = int64) }
    val NSSearchPathForDirectoriesInDomains by func { declare(int64, int64, boolean, retType = pointer) }
    val NSSelectorFromString by func { declare(pointer, retType = pointer) }
    val NSSetUncaughtExceptionHandler by func { declare(pointer) }
    val NSSetZoneName by func { declare(pointer, pointer) }
    val NSSizeFromString by func { declare(pointer, retType = MacOSDef.CGSize) }
    val NSStringFromClass by func { declare(pointer, retType = pointer) }
    val NSStringFromHashTable by func { declare(pointer, retType = pointer) }
    val NSStringFromMapTable by func { declare(pointer, retType = pointer) }
    val NSStringFromPoint by func { declare(MacOSDef.CGPoint, retType = pointer) }
    val NSStringFromProtocol by func { declare(pointer, retType = pointer) }
    val NSStringFromRange by func { declare(MacOSDef.NSRange, retType = pointer) }
    val NSStringFromRect by func { declare(MacOSDef.CGRect, retType = pointer) }
    val NSStringFromSelector by func { declare(pointer, retType = pointer) }
    val NSStringFromSize by func { declare(MacOSDef.CGSize, retType = pointer) }
    val NSTemporaryDirectory by func { declare(retType = pointer) }
    val NSUnionRange by func { declare(MacOSDef.NSRange, MacOSDef.NSRange, retType = MacOSDef.NSRange) }
    val NSUnionRect by func { declare(MacOSDef.CGRect, MacOSDef.CGRect, retType = MacOSDef.CGRect) }
    val NSUserName by func { declare(retType = pointer) }
    val NSZoneCalloc by func { declare(pointer, int64, int64, retType = pointer) }
    val NSZoneFree by func { declare(pointer, pointer) }
    val NSZoneFromPointer by func { declare(pointer, retType = pointer) }
    val NSZoneMalloc by func { declare(pointer, int64, retType = pointer) }
    val NSZoneName by func { declare(pointer, retType = pointer) }
    val NSZoneRealloc by func { declare(pointer, pointer, int64, retType = pointer) }
    val NXReadNSObjectFromCoder by func { declare(pointer, retType = pointer) }
}
