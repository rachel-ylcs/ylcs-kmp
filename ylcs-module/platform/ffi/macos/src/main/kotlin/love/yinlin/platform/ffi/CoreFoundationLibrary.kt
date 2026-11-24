package love.yinlin.platform.ffi

@Suppress("UnusedReceiverParameter")
inline val FFI.coreFoundation: CoreFoundationLibrary get() = CoreFoundationLibrary.Instance

@Suppress("PropertyName", "unused", "SpellCheckingInspection")
class CoreFoundationLibrary private constructor() : NativeLibrary("/System/Library/Frameworks/CoreFoundation.framework/CoreFoundation") {
    @PublishedApi
    internal companion object {
        @PublishedApi
        internal val Instance = CoreFoundationLibrary()

        private val byte = NativeType.Byte
        private val int16 = NativeType.Short
        private val int32 = NativeType.Int
        private val int64 = NativeType.Long
        private val float64 = NativeType.Double
        private val pointer = NativeType.Pointer
    }

    val CFAbsoluteTimeAddGregorianUnits by func { declare(float64, pointer, MacOSDef.CFGregorianUnits, retType = float64) }
    val CFAbsoluteTimeGetCurrent by func { declare(retType = float64) }
    val CFAbsoluteTimeGetDayOfWeek by func { declare(float64, pointer, retType = int32) }
    val CFAbsoluteTimeGetDayOfYear by func { declare(float64, pointer, retType = int32) }
    val CFAbsoluteTimeGetDifferenceAsGregorianUnits by func { declare(float64, float64, pointer, int64, retType = MacOSDef.CFGregorianUnits) }
    val CFAbsoluteTimeGetGregorianDate by func { declare(float64, pointer, retType = MacOSDef.CFGregorianDate) }
    val CFAbsoluteTimeGetWeekOfYear by func { declare(float64, pointer, retType = int32) }
    val CFAllocatorAllocate by func { declare(pointer, int64, int64, retType = pointer) }
    val CFAllocatorCreate by func { declare(pointer, pointer, retType = pointer) }
    val CFAllocatorDeallocate by func { declare(pointer, pointer) }
    val CFAllocatorGetContext by func { declare(pointer, pointer) }
    val CFAllocatorGetDefault by func { declare(retType = pointer) }
    val CFAllocatorGetPreferredSizeForSize by func { declare(pointer, int64, int64, retType = int64) }
    val CFAllocatorGetTypeID by func { declare(retType = int64) }
    val CFAllocatorReallocate by func { declare(pointer, pointer, int64, int64, retType = pointer) }
    val CFAllocatorSetDefault by func { declare(pointer) }
    val CFArrayAppendArray by func { declare(pointer, pointer, MacOSDef.CFRange) }
    val CFArrayAppendValue by func { declare(pointer, pointer) }
    val CFArrayApplyFunction by func { declare(pointer, MacOSDef.CFRange, pointer, pointer) }
    val CFArrayBSearchValues by func { declare(pointer, MacOSDef.CFRange, pointer, pointer, pointer, retType = int64) }
    val CFArrayContainsValue by func { declare(pointer, MacOSDef.CFRange, pointer, retType = byte) }
    val CFArrayCreate by func { declare(pointer, pointer, int64, pointer, retType = pointer) }
    val CFArrayCreateCopy by func { declare(pointer, pointer, retType = pointer) }
    val CFArrayCreateMutable by func { declare(pointer, int64, pointer, retType = pointer) }
    val CFArrayCreateMutableCopy by func { declare(pointer, int64, pointer, retType = pointer) }
    val CFArrayExchangeValuesAtIndices by func { declare(pointer, int64, int64) }
    val CFArrayGetCount by func { declare(pointer, retType = int64) }
    val CFArrayGetCountOfValue by func { declare(pointer, MacOSDef.CFRange, pointer, retType = int64) }
    val CFArrayGetFirstIndexOfValue by func { declare(pointer, MacOSDef.CFRange, pointer, retType = int64) }
    val CFArrayGetLastIndexOfValue by func { declare(pointer, MacOSDef.CFRange, pointer, retType = int64) }
    val CFArrayGetTypeID by func { declare(retType = int64) }
    val CFArrayGetValueAtIndex by func { declare(pointer, int64, retType = pointer) }
    val CFArrayGetValues by func { declare(pointer, MacOSDef.CFRange, pointer) }
    val CFArrayInsertValueAtIndex by func { declare(pointer, int64, pointer) }
    val CFArrayRemoveAllValues by func { declare(pointer) }
    val CFArrayRemoveValueAtIndex by func { declare(pointer, int64) }
    val CFArrayReplaceValues by func { declare(pointer, MacOSDef.CFRange, pointer, int64) }
    val CFArraySetValueAtIndex by func { declare(pointer, int64, pointer) }
    val CFArraySortValues by func { declare(pointer, MacOSDef.CFRange, pointer, pointer) }
    val CFAttributedStringBeginEditing by func { declare(pointer) }
    val CFAttributedStringCreate by func { declare(pointer, pointer, pointer, retType = pointer) }
    val CFAttributedStringCreateCopy by func { declare(pointer, pointer, retType = pointer) }
    val CFAttributedStringCreateMutable by func { declare(pointer, int64, retType = pointer) }
    val CFAttributedStringCreateMutableCopy by func { declare(pointer, int64, pointer, retType = pointer) }
    val CFAttributedStringCreateWithSubstring by func { declare(pointer, pointer, MacOSDef.CFRange, retType = pointer) }
    val CFAttributedStringEndEditing by func { declare(pointer) }
    val CFAttributedStringGetAttribute by func { declare(pointer, int64, pointer, pointer, retType = pointer) }
    val CFAttributedStringGetAttributeAndLongestEffectiveRange by func { declare(pointer, int64, pointer, MacOSDef.CFRange, pointer, retType = pointer) }
    val CFAttributedStringGetAttributes by func { declare(pointer, int64, pointer, retType = pointer) }
    val CFAttributedStringGetAttributesAndLongestEffectiveRange by func { declare(pointer, int64, MacOSDef.CFRange, pointer, retType = pointer) }
    val CFAttributedStringGetBidiLevelsAndResolvedDirections by func { declare(pointer, MacOSDef.CFRange, byte, pointer, pointer, retType = byte) }
    val CFAttributedStringGetLength by func { declare(pointer, retType = int64) }
    val CFAttributedStringGetMutableString by func { declare(pointer, retType = pointer) }
    val CFAttributedStringGetString by func { declare(pointer, retType = pointer) }
    val CFAttributedStringGetTypeID by func { declare(retType = int64) }
    val CFAttributedStringRemoveAttribute by func { declare(pointer, MacOSDef.CFRange, pointer) }
    val CFAttributedStringReplaceAttributedString by func { declare(pointer, MacOSDef.CFRange, pointer) }
    val CFAttributedStringReplaceString by func { declare(pointer, MacOSDef.CFRange, pointer) }
    val CFAttributedStringSetAttribute by func { declare(pointer, MacOSDef.CFRange, pointer, pointer) }
    val CFAttributedStringSetAttributes by func { declare(pointer, MacOSDef.CFRange, pointer, byte) }
    val CFAutorelease by func { declare(pointer, retType = pointer) }
    val CFBagAddValue by func { declare(pointer, pointer) }
    val CFBagApplyFunction by func { declare(pointer, pointer, pointer) }
    val CFBagContainsValue by func { declare(pointer, pointer, retType = byte) }
    val CFBagCreate by func { declare(pointer, pointer, int64, pointer, retType = pointer) }
    val CFBagCreateCopy by func { declare(pointer, pointer, retType = pointer) }
    val CFBagCreateMutable by func { declare(pointer, int64, pointer, retType = pointer) }
    val CFBagCreateMutableCopy by func { declare(pointer, int64, pointer, retType = pointer) }
    val CFBagGetCount by func { declare(pointer, retType = int64) }
    val CFBagGetCountOfValue by func { declare(pointer, pointer, retType = int64) }
    val CFBagGetTypeID by func { declare(retType = int64) }
    val CFBagGetValue by func { declare(pointer, pointer, retType = pointer) }
    val CFBagGetValueIfPresent by func { declare(pointer, pointer, pointer, retType = byte) }
    val CFBagGetValues by func { declare(pointer, pointer) }
    val CFBagRemoveAllValues by func { declare(pointer) }
    val CFBagRemoveValue by func { declare(pointer, pointer) }
    val CFBagReplaceValue by func { declare(pointer, pointer) }
    val CFBagSetValue by func { declare(pointer, pointer) }
    val CFBinaryHeapAddValue by func { declare(pointer, pointer) }
    val CFBinaryHeapApplyFunction by func { declare(pointer, pointer, pointer) }
    val CFBinaryHeapContainsValue by func { declare(pointer, pointer, retType = byte) }
    val CFBinaryHeapCreate by func { declare(pointer, int64, pointer, pointer, retType = pointer) }
    val CFBinaryHeapCreateCopy by func { declare(pointer, int64, pointer, retType = pointer) }
    val CFBinaryHeapGetCount by func { declare(pointer, retType = int64) }
    val CFBinaryHeapGetCountOfValue by func { declare(pointer, pointer, retType = int64) }
    val CFBinaryHeapGetMinimum by func { declare(pointer, retType = pointer) }
    val CFBinaryHeapGetMinimumIfPresent by func { declare(pointer, pointer, retType = byte) }
    val CFBinaryHeapGetTypeID by func { declare(retType = int64) }
    val CFBinaryHeapGetValues by func { declare(pointer, pointer) }
    val CFBinaryHeapRemoveAllValues by func { declare(pointer) }
    val CFBinaryHeapRemoveMinimumValue by func { declare(pointer) }
    val CFBitVectorContainsBit by func { declare(pointer, MacOSDef.CFRange, int32, retType = byte) }
    val CFBitVectorCreate by func { declare(pointer, pointer, int64, retType = pointer) }
    val CFBitVectorCreateCopy by func { declare(pointer, pointer, retType = pointer) }
    val CFBitVectorCreateMutable by func { declare(pointer, int64, retType = pointer) }
    val CFBitVectorCreateMutableCopy by func { declare(pointer, int64, pointer, retType = pointer) }
    val CFBitVectorFlipBitAtIndex by func { declare(pointer, int64) }
    val CFBitVectorFlipBits by func { declare(pointer, MacOSDef.CFRange) }
    val CFBitVectorGetBitAtIndex by func { declare(pointer, int64, retType = int32) }
    val CFBitVectorGetBits by func { declare(pointer, MacOSDef.CFRange, pointer) }
    val CFBitVectorGetCount by func { declare(pointer, retType = int64) }
    val CFBitVectorGetCountOfBit by func { declare(pointer, MacOSDef.CFRange, int32, retType = int64) }
    val CFBitVectorGetFirstIndexOfBit by func { declare(pointer, MacOSDef.CFRange, int32, retType = int64) }
    val CFBitVectorGetLastIndexOfBit by func { declare(pointer, MacOSDef.CFRange, int32, retType = int64) }
    val CFBitVectorGetTypeID by func { declare(retType = int64) }
    val CFBitVectorSetAllBits by func { declare(pointer, int32) }
    val CFBitVectorSetBitAtIndex by func { declare(pointer, int64, int32) }
    val CFBitVectorSetBits by func { declare(pointer, MacOSDef.CFRange, int32) }
    val CFBitVectorSetCount by func { declare(pointer, int64) }
    val CFBooleanGetTypeID by func { declare(retType = int64) }
    val CFBooleanGetValue by func { declare(pointer, retType = byte) }
    val CFBundleCloseBundleResourceMap by func { declare(pointer, int32) }
    val CFBundleCopyAuxiliaryExecutableURL by func { declare(pointer, pointer, retType = pointer) }
    val CFBundleCopyBuiltInPlugInsURL by func { declare(pointer, retType = pointer) }
    val CFBundleCopyBundleLocalizations by func { declare(pointer, retType = pointer) }
    val CFBundleCopyBundleURL by func { declare(pointer, retType = pointer) }
    val CFBundleCopyExecutableArchitectures by func { declare(pointer, retType = pointer) }
    val CFBundleCopyExecutableArchitecturesForURL by func { declare(pointer, retType = pointer) }
    val CFBundleCopyExecutableURL by func { declare(pointer, retType = pointer) }
    val CFBundleCopyInfoDictionaryForURL by func { declare(pointer, retType = pointer) }
    val CFBundleCopyInfoDictionaryInDirectory by func { declare(pointer, retType = pointer) }
    val CFBundleCopyLocalizationsForPreferences by func { declare(pointer, pointer, retType = pointer) }
    val CFBundleCopyLocalizationsForURL by func { declare(pointer, retType = pointer) }
    val CFBundleCopyLocalizedString by func { declare(pointer, pointer, pointer, pointer, retType = pointer) }
    val CFBundleCopyPreferredLocalizationsFromArray by func { declare(pointer, retType = pointer) }
    val CFBundleCopyPrivateFrameworksURL by func { declare(pointer, retType = pointer) }
    val CFBundleCopyResourceURL by func { declare(pointer, pointer, pointer, pointer, retType = pointer) }
    val CFBundleCopyResourceURLForLocalization by func { declare(pointer, pointer, pointer, pointer, pointer, retType = pointer) }
    val CFBundleCopyResourceURLInDirectory by func { declare(pointer, pointer, pointer, pointer, retType = pointer) }
    val CFBundleCopyResourceURLsOfType by func { declare(pointer, pointer, pointer, retType = pointer) }
    val CFBundleCopyResourceURLsOfTypeForLocalization by func { declare(pointer, pointer, pointer, pointer, retType = pointer) }
    val CFBundleCopyResourceURLsOfTypeInDirectory by func { declare(pointer, pointer, pointer, retType = pointer) }
    val CFBundleCopyResourcesDirectoryURL by func { declare(pointer, retType = pointer) }
    val CFBundleCopySharedFrameworksURL by func { declare(pointer, retType = pointer) }
    val CFBundleCopySharedSupportURL by func { declare(pointer, retType = pointer) }
    val CFBundleCopySupportFilesDirectoryURL by func { declare(pointer, retType = pointer) }
    val CFBundleCreate by func { declare(pointer, pointer, retType = pointer) }
    val CFBundleCreateBundlesFromDirectory by func { declare(pointer, pointer, pointer, retType = pointer) }
    val CFBundleGetAllBundles by func { declare(retType = pointer) }
    val CFBundleGetBundleWithIdentifier by func { declare(pointer, retType = pointer) }
    val CFBundleGetDataPointerForName by func { declare(pointer, pointer, retType = pointer) }
    val CFBundleGetDataPointersForNames by func { declare(pointer, pointer, pointer) }
    val CFBundleGetDevelopmentRegion by func { declare(pointer, retType = pointer) }
    val CFBundleGetFunctionPointerForName by func { declare(pointer, pointer, retType = pointer) }
    val CFBundleGetFunctionPointersForNames by func { declare(pointer, pointer, pointer) }
    val CFBundleGetIdentifier by func { declare(pointer, retType = pointer) }
    val CFBundleGetInfoDictionary by func { declare(pointer, retType = pointer) }
    val CFBundleGetLocalInfoDictionary by func { declare(pointer, retType = pointer) }
    val CFBundleGetMainBundle by func { declare(retType = pointer) }
    val CFBundleGetPackageInfo by func { declare(pointer, pointer, pointer) }
    val CFBundleGetPackageInfoInDirectory by func { declare(pointer, pointer, pointer, retType = byte) }
    val CFBundleGetPlugIn by func { declare(pointer, retType = pointer) }
    val CFBundleGetTypeID by func { declare(retType = int64) }
    val CFBundleGetValueForInfoDictionaryKey by func { declare(pointer, pointer, retType = pointer) }
    val CFBundleGetVersionNumber by func { declare(pointer, retType = int32) }
    val CFBundleIsArchitectureLoadable by func { declare(int32, retType = byte) }
    val CFBundleIsExecutableLoadable by func { declare(pointer, retType = byte) }
    val CFBundleIsExecutableLoadableForURL by func { declare(pointer, retType = byte) }
    val CFBundleIsExecutableLoaded by func { declare(pointer, retType = byte) }
    val CFBundleLoadExecutable by func { declare(pointer, retType = byte) }
    val CFBundleLoadExecutableAndReturnError by func { declare(pointer, pointer, retType = byte) }
    val CFBundleOpenBundleResourceFiles by func { declare(pointer, pointer, pointer, retType = int32) }
    val CFBundleOpenBundleResourceMap by func { declare(pointer, retType = int32) }
    val CFBundlePreflightExecutable by func { declare(pointer, pointer, retType = byte) }
    val CFBundleUnloadExecutable by func { declare(pointer) }
    val CFCalendarCopyCurrent by func { declare(retType = pointer) }
    val CFCalendarCopyLocale by func { declare(pointer, retType = pointer) }
    val CFCalendarCopyTimeZone by func { declare(pointer, retType = pointer) }
    val CFCalendarCreateWithIdentifier by func { declare(pointer, pointer, retType = pointer) }
    val CFCalendarGetFirstWeekday by func { declare(pointer, retType = int64) }
    val CFCalendarGetIdentifier by func { declare(pointer, retType = pointer) }
    val CFCalendarGetMaximumRangeOfUnit by func { declare(pointer, int64, retType = MacOSDef.CFRange) }
    val CFCalendarGetMinimumDaysInFirstWeek by func { declare(pointer, retType = int64) }
    val CFCalendarGetMinimumRangeOfUnit by func { declare(pointer, int64, retType = MacOSDef.CFRange) }
    val CFCalendarGetOrdinalityOfUnit by func { declare(pointer, int64, int64, float64, retType = int64) }
    val CFCalendarGetRangeOfUnit by func { declare(pointer, int64, int64, float64, retType = MacOSDef.CFRange) }
    val CFCalendarGetTimeRangeOfUnit by func { declare(pointer, int64, float64, pointer, pointer, retType = byte) }
    val CFCalendarGetTypeID by func { declare(retType = int64) }
    val CFCalendarSetFirstWeekday by func { declare(pointer, int64) }
    val CFCalendarSetLocale by func { declare(pointer, pointer) }
    val CFCalendarSetMinimumDaysInFirstWeek by func { declare(pointer, int64) }
    val CFCalendarSetTimeZone by func { declare(pointer, pointer) }
    val CFCharacterSetAddCharactersInRange by func { declare(pointer, MacOSDef.CFRange) }
    val CFCharacterSetAddCharactersInString by func { declare(pointer, pointer) }
    val CFCharacterSetCreateBitmapRepresentation by func { declare(pointer, pointer, retType = pointer) }
    val CFCharacterSetCreateCopy by func { declare(pointer, pointer, retType = pointer) }
    val CFCharacterSetCreateInvertedSet by func { declare(pointer, pointer, retType = pointer) }
    val CFCharacterSetCreateMutable by func { declare(pointer, retType = pointer) }
    val CFCharacterSetCreateMutableCopy by func { declare(pointer, pointer, retType = pointer) }
    val CFCharacterSetCreateWithBitmapRepresentation by func { declare(pointer, pointer, retType = pointer) }
    val CFCharacterSetCreateWithCharactersInRange by func { declare(pointer, MacOSDef.CFRange, retType = pointer) }
    val CFCharacterSetCreateWithCharactersInString by func { declare(pointer, pointer, retType = pointer) }
    val CFCharacterSetGetPredefined by func { declare(int64, retType = pointer) }
    val CFCharacterSetGetTypeID by func { declare(retType = int64) }
    val CFCharacterSetHasMemberInPlane by func { declare(pointer, int64, retType = byte) }
    val CFCharacterSetIntersect by func { declare(pointer, pointer) }
    val CFCharacterSetInvert by func { declare(pointer) }
    val CFCharacterSetIsCharacterMember by func { declare(pointer, int16, retType = byte) }
    val CFCharacterSetIsLongCharacterMember by func { declare(pointer, int32, retType = byte) }
    val CFCharacterSetIsSupersetOfSet by func { declare(pointer, pointer, retType = byte) }
    val CFCharacterSetRemoveCharactersInRange by func { declare(pointer, MacOSDef.CFRange) }
    val CFCharacterSetRemoveCharactersInString by func { declare(pointer, pointer) }
    val CFCharacterSetUnion by func { declare(pointer, pointer) }
    val CFCopyDescription by func { declare(pointer, retType = pointer) }
    val CFCopyTypeIDDescription by func { declare(int64, retType = pointer) }
    val CFDataAppendBytes by func { declare(pointer, pointer, int64) }
    val CFDataCreate by func { declare(pointer, pointer, int64, retType = pointer) }
    val CFDataCreateCopy by func { declare(pointer, pointer, retType = pointer) }
    val CFDataCreateMutable by func { declare(pointer, int64, retType = pointer) }
    val CFDataCreateMutableCopy by func { declare(pointer, int64, pointer, retType = pointer) }
    val CFDataCreateWithBytesNoCopy by func { declare(pointer, pointer, int64, pointer, retType = pointer) }
    val CFDataDeleteBytes by func { declare(pointer, MacOSDef.CFRange) }
    val CFDataFind by func { declare(pointer, pointer, MacOSDef.CFRange, int64, retType = MacOSDef.CFRange) }
    val CFDataGetBytePtr by func { declare(pointer, retType = pointer) }
    val CFDataGetBytes by func { declare(pointer, MacOSDef.CFRange, pointer) }
    val CFDataGetLength by func { declare(pointer, retType = int64) }
    val CFDataGetMutableBytePtr by func { declare(pointer, retType = pointer) }
    val CFDataGetTypeID by func { declare(retType = int64) }
    val CFDataIncreaseLength by func { declare(pointer, int64) }
    val CFDataReplaceBytes by func { declare(pointer, MacOSDef.CFRange, pointer, int64) }
    val CFDataSetLength by func { declare(pointer, int64) }
    val CFDateCompare by func { declare(pointer, pointer, pointer, retType = int64) }
    val CFDateCreate by func { declare(pointer, float64, retType = pointer) }
    val CFDateFormatterCopyProperty by func { declare(pointer, pointer, retType = pointer) }
    val CFDateFormatterCreate by func { declare(pointer, pointer, int64, int64, retType = pointer) }
    val CFDateFormatterCreateDateFormatFromTemplate by func { declare(pointer, pointer, int64, pointer, retType = pointer) }
    val CFDateFormatterCreateDateFromString by func { declare(pointer, pointer, pointer, pointer, retType = pointer) }
    val CFDateFormatterCreateISO8601Formatter by func { declare(pointer, int64, retType = pointer) }
    val CFDateFormatterCreateStringWithAbsoluteTime by func { declare(pointer, pointer, float64, retType = pointer) }
    val CFDateFormatterCreateStringWithDate by func { declare(pointer, pointer, pointer, retType = pointer) }
    val CFDateFormatterGetAbsoluteTimeFromString by func { declare(pointer, pointer, pointer, pointer, retType = byte) }
    val CFDateFormatterGetDateStyle by func { declare(pointer, retType = int64) }
    val CFDateFormatterGetFormat by func { declare(pointer, retType = pointer) }
    val CFDateFormatterGetLocale by func { declare(pointer, retType = pointer) }
    val CFDateFormatterGetTimeStyle by func { declare(pointer, retType = int64) }
    val CFDateFormatterGetTypeID by func { declare(retType = int64) }
    val CFDateFormatterSetFormat by func { declare(pointer, pointer) }
    val CFDateFormatterSetProperty by func { declare(pointer, pointer, pointer) }
    val CFDateGetAbsoluteTime by func { declare(pointer, retType = float64) }
    val CFDateGetTimeIntervalSinceDate by func { declare(pointer, pointer, retType = float64) }
    val CFDateGetTypeID by func { declare(retType = int64) }
    val CFDictionaryAddValue by func { declare(pointer, pointer, pointer) }
    val CFDictionaryApplyFunction by func { declare(pointer, pointer, pointer) }
    val CFDictionaryContainsKey by func { declare(pointer, pointer, retType = byte) }
    val CFDictionaryContainsValue by func { declare(pointer, pointer, retType = byte) }
    val CFDictionaryCreate by func { declare(pointer, pointer, pointer, int64, pointer, pointer, retType = pointer) }
    val CFDictionaryCreateCopy by func { declare(pointer, pointer, retType = pointer) }
    val CFDictionaryCreateMutable by func { declare(pointer, int64, pointer, pointer, retType = pointer) }
    val CFDictionaryCreateMutableCopy by func { declare(pointer, int64, pointer, retType = pointer) }
    val CFDictionaryGetCount by func { declare(pointer, retType = int64) }
    val CFDictionaryGetCountOfKey by func { declare(pointer, pointer, retType = int64) }
    val CFDictionaryGetCountOfValue by func { declare(pointer, pointer, retType = int64) }
    val CFDictionaryGetKeysAndValues by func { declare(pointer, pointer, pointer) }
    val CFDictionaryGetTypeID by func { declare(retType = int64) }
    val CFDictionaryGetValue by func { declare(pointer, pointer, retType = pointer) }
    val CFDictionaryGetValueIfPresent by func { declare(pointer, pointer, pointer, retType = byte) }
    val CFDictionaryRemoveAllValues by func { declare(pointer) }
    val CFDictionaryRemoveValue by func { declare(pointer, pointer) }
    val CFDictionaryReplaceValue by func { declare(pointer, pointer, pointer) }
    val CFDictionarySetValue by func { declare(pointer, pointer, pointer) }
    val CFEqual by func { declare(pointer, pointer, retType = byte) }
    val CFErrorCopyDescription by func { declare(pointer, retType = pointer) }
    val CFErrorCopyFailureReason by func { declare(pointer, retType = pointer) }
    val CFErrorCopyRecoverySuggestion by func { declare(pointer, retType = pointer) }
    val CFErrorCopyUserInfo by func { declare(pointer, retType = pointer) }
    val CFErrorCreate by func { declare(pointer, pointer, int64, pointer, retType = pointer) }
    val CFErrorCreateWithUserInfoKeysAndValues by func { declare(pointer, pointer, int64, pointer, pointer, int64, retType = pointer) }
    val CFErrorGetCode by func { declare(pointer, retType = int64) }
    val CFErrorGetDomain by func { declare(pointer, retType = pointer) }
    val CFErrorGetTypeID by func { declare(retType = int64) }
    val CFFileDescriptorCreate by func { declare(pointer, int32, byte, pointer, pointer, retType = pointer) }
    val CFFileDescriptorCreateRunLoopSource by func { declare(pointer, pointer, int64, retType = pointer) }
    val CFFileDescriptorDisableCallBacks by func { declare(pointer, int64) }
    val CFFileDescriptorEnableCallBacks by func { declare(pointer, int64) }
    val CFFileDescriptorGetContext by func { declare(pointer, pointer) }
    val CFFileDescriptorGetNativeDescriptor by func { declare(pointer, retType = int32) }
    val CFFileDescriptorGetTypeID by func { declare(retType = int64) }
    val CFFileDescriptorInvalidate by func { declare(pointer) }
    val CFFileDescriptorIsValid by func { declare(pointer, retType = byte) }
    val CFFileSecurityClearProperties by func { declare(pointer, int64, retType = byte) }
    val CFFileSecurityCopyAccessControlList by func { declare(pointer, pointer, retType = byte) }
    val CFFileSecurityCopyGroupUUID by func { declare(pointer, pointer, retType = byte) }
    val CFFileSecurityCopyOwnerUUID by func { declare(pointer, pointer, retType = byte) }
    val CFFileSecurityCreate by func { declare(pointer, retType = pointer) }
    val CFFileSecurityCreateCopy by func { declare(pointer, pointer, retType = pointer) }
    val CFFileSecurityGetGroup by func { declare(pointer, pointer, retType = byte) }
    val CFFileSecurityGetMode by func { declare(pointer, pointer, retType = byte) }
    val CFFileSecurityGetOwner by func { declare(pointer, pointer, retType = byte) }
    val CFFileSecurityGetTypeID by func { declare(retType = int64) }
    val CFFileSecuritySetAccessControlList by func { declare(pointer, pointer, retType = byte) }
    val CFFileSecuritySetGroup by func { declare(pointer, int32, retType = byte) }
    val CFFileSecuritySetGroupUUID by func { declare(pointer, pointer, retType = byte) }
    val CFFileSecuritySetMode by func { declare(pointer, int16, retType = byte) }
    val CFFileSecuritySetOwner by func { declare(pointer, int32, retType = byte) }
    val CFFileSecuritySetOwnerUUID by func { declare(pointer, pointer, retType = byte) }
    val CFGetAllocator by func { declare(pointer, retType = pointer) }
    val CFGetRetainCount by func { declare(pointer, retType = int64) }
    val CFGetTypeID by func { declare(pointer, retType = int64) }
    val CFGregorianDateGetAbsoluteTime by func { declare(MacOSDef.CFGregorianDate, pointer, retType = float64) }
    val CFGregorianDateIsValid by func { declare(MacOSDef.CFGregorianDate, int64, retType = byte) }
    val CFHash by func { declare(pointer, retType = int64) }
    val CFLocaleCopyAvailableLocaleIdentifiers by func { declare(retType = pointer) }
    val CFLocaleCopyCommonISOCurrencyCodes by func { declare(retType = pointer) }
    val CFLocaleCopyCurrent by func { declare(retType = pointer) }
    val CFLocaleCopyDisplayNameForPropertyValue by func { declare(pointer, pointer, pointer, retType = pointer) }
    val CFLocaleCopyISOCountryCodes by func { declare(retType = pointer) }
    val CFLocaleCopyISOCurrencyCodes by func { declare(retType = pointer) }
    val CFLocaleCopyISOLanguageCodes by func { declare(retType = pointer) }
    val CFLocaleCopyPreferredLanguages by func { declare(retType = pointer) }
    val CFLocaleCreate by func { declare(pointer, pointer, retType = pointer) }
    val CFLocaleCreateCanonicalLanguageIdentifierFromString by func { declare(pointer, pointer, retType = pointer) }
    val CFLocaleCreateCanonicalLocaleIdentifierFromScriptManagerCodes by func { declare(pointer, int16, int16, retType = pointer) }
    val CFLocaleCreateCanonicalLocaleIdentifierFromString by func { declare(pointer, pointer, retType = pointer) }
    val CFLocaleCreateComponentsFromLocaleIdentifier by func { declare(pointer, pointer, retType = pointer) }
    val CFLocaleCreateCopy by func { declare(pointer, pointer, retType = pointer) }
    val CFLocaleCreateLocaleIdentifierFromComponents by func { declare(pointer, pointer, retType = pointer) }
    val CFLocaleCreateLocaleIdentifierFromWindowsLocaleCode by func { declare(pointer, int32, retType = pointer) }
    val CFLocaleGetIdentifier by func { declare(pointer, retType = pointer) }
    val CFLocaleGetLanguageCharacterDirection by func { declare(pointer, retType = int64) }
    val CFLocaleGetLanguageLineDirection by func { declare(pointer, retType = int64) }
    val CFLocaleGetSystem by func { declare(retType = pointer) }
    val CFLocaleGetTypeID by func { declare(retType = int64) }
    val CFLocaleGetValue by func { declare(pointer, pointer, retType = pointer) }
    val CFLocaleGetWindowsLocaleCodeFromLocaleIdentifier by func { declare(pointer, retType = int32) }
    val CFMachPortCreate by func { declare(pointer, pointer, pointer, pointer, retType = pointer) }
    val CFMachPortCreateRunLoopSource by func { declare(pointer, pointer, int64, retType = pointer) }
    val CFMachPortCreateWithPort by func { declare(pointer, int32, pointer, pointer, pointer, retType = pointer) }
    val CFMachPortGetContext by func { declare(pointer, pointer) }
    val CFMachPortGetInvalidationCallBack by func { declare(pointer, retType = pointer) }
    val CFMachPortGetPort by func { declare(pointer, retType = int32) }
    val CFMachPortGetTypeID by func { declare(retType = int64) }
    val CFMachPortInvalidate by func { declare(pointer) }
    val CFMachPortIsValid by func { declare(pointer, retType = byte) }
    val CFMachPortSetInvalidationCallBack by func { declare(pointer, pointer) }
    val CFMessagePortCreateLocal by func { declare(pointer, pointer, pointer, pointer, pointer, retType = pointer) }
    val CFMessagePortCreateRemote by func { declare(pointer, pointer, retType = pointer) }
    val CFMessagePortCreateRunLoopSource by func { declare(pointer, pointer, int64, retType = pointer) }
    val CFMessagePortGetContext by func { declare(pointer, pointer) }
    val CFMessagePortGetInvalidationCallBack by func { declare(pointer, retType = pointer) }
    val CFMessagePortGetName by func { declare(pointer, retType = pointer) }
    val CFMessagePortGetTypeID by func { declare(retType = int64) }
    val CFMessagePortInvalidate by func { declare(pointer) }
    val CFMessagePortIsRemote by func { declare(pointer, retType = byte) }
    val CFMessagePortIsValid by func { declare(pointer, retType = byte) }
    val CFMessagePortSendRequest by func { declare(pointer, int32, pointer, float64, float64, pointer, pointer, retType = int32) }
    val CFMessagePortSetDispatchQueue by func { declare(pointer, pointer) }
    val CFMessagePortSetInvalidationCallBack by func { declare(pointer, pointer) }
    val CFMessagePortSetName by func { declare(pointer, pointer, retType = byte) }
    val CFNotificationCenterAddObserver by func { declare(pointer, pointer, pointer, pointer, pointer, int64) }
    val CFNotificationCenterGetDarwinNotifyCenter by func { declare(retType = pointer) }
    val CFNotificationCenterGetDistributedCenter by func { declare(retType = pointer) }
    val CFNotificationCenterGetLocalCenter by func { declare(retType = pointer) }
    val CFNotificationCenterGetTypeID by func { declare(retType = int64) }
    val CFNotificationCenterPostNotification by func { declare(pointer, pointer, pointer, pointer, byte) }
    val CFNotificationCenterPostNotificationWithOptions by func { declare(pointer, pointer, pointer, pointer, int64) }
    val CFNotificationCenterRemoveEveryObserver by func { declare(pointer, pointer) }
    val CFNotificationCenterRemoveObserver by func { declare(pointer, pointer, pointer, pointer) }
    val CFNullGetTypeID by func { declare(retType = int64) }
    val CFNumberCompare by func { declare(pointer, pointer, pointer, retType = int64) }
    val CFNumberCreate by func { declare(pointer, int64, pointer, retType = pointer) }
    val CFNumberFormatterCopyProperty by func { declare(pointer, pointer, retType = pointer) }
    val CFNumberFormatterCreate by func { declare(pointer, pointer, int64, retType = pointer) }
    val CFNumberFormatterCreateNumberFromString by func { declare(pointer, pointer, pointer, pointer, int64, retType = pointer) }
    val CFNumberFormatterCreateStringWithNumber by func { declare(pointer, pointer, pointer, retType = pointer) }
    val CFNumberFormatterCreateStringWithValue by func { declare(pointer, pointer, int64, pointer, retType = pointer) }
    val CFNumberFormatterGetDecimalInfoForCurrencyCode by func { declare(pointer, pointer, pointer, retType = byte) }
    val CFNumberFormatterGetFormat by func { declare(pointer, retType = pointer) }
    val CFNumberFormatterGetLocale by func { declare(pointer, retType = pointer) }
    val CFNumberFormatterGetStyle by func { declare(pointer, retType = int64) }
    val CFNumberFormatterGetTypeID by func { declare(retType = int64) }
    val CFNumberFormatterGetValueFromString by func { declare(pointer, pointer, pointer, int64, pointer, retType = byte) }
    val CFNumberFormatterSetFormat by func { declare(pointer, pointer) }
    val CFNumberFormatterSetProperty by func { declare(pointer, pointer, pointer) }
    val CFNumberGetByteSize by func { declare(pointer, retType = int64) }
    val CFNumberGetType by func { declare(pointer, retType = int64) }
    val CFNumberGetTypeID by func { declare(retType = int64) }
    val CFNumberGetValue by func { declare(pointer, int64, pointer, retType = byte) }
    val CFNumberIsFloatType by func { declare(pointer, retType = byte) }
    val CFPlugInAddInstanceForFactory by func { declare(pointer) }
    val CFPlugInCreate by func { declare(pointer, pointer, retType = pointer) }
    val CFPlugInFindFactoriesForPlugInType by func { declare(pointer, retType = pointer) }
    val CFPlugInFindFactoriesForPlugInTypeInPlugIn by func { declare(pointer, pointer, retType = pointer) }
    val CFPlugInGetBundle by func { declare(pointer, retType = pointer) }
    val CFPlugInGetTypeID by func { declare(retType = int64) }
    val CFPlugInInstanceCreate by func { declare(pointer, pointer, pointer, retType = pointer) }
    val CFPlugInInstanceCreateWithInstanceDataSize by func { declare(pointer, int64, pointer, pointer, pointer, retType = pointer) }
    val CFPlugInInstanceGetFactoryName by func { declare(pointer, retType = pointer) }
    val CFPlugInInstanceGetInstanceData by func { declare(pointer, retType = pointer) }
    val CFPlugInInstanceGetInterfaceFunctionTable by func { declare(pointer, pointer, pointer, retType = byte) }
    val CFPlugInInstanceGetTypeID by func { declare(retType = int64) }
    val CFPlugInIsLoadOnDemand by func { declare(pointer, retType = byte) }
    val CFPlugInRegisterFactoryFunction by func { declare(pointer, pointer, retType = byte) }
    val CFPlugInRegisterFactoryFunctionByName by func { declare(pointer, pointer, pointer, retType = byte) }
    val CFPlugInRegisterPlugInType by func { declare(pointer, pointer, retType = byte) }
    val CFPlugInRemoveInstanceForFactory by func { declare(pointer) }
    val CFPlugInSetLoadOnDemand by func { declare(pointer, byte) }
    val CFPlugInUnregisterFactory by func { declare(pointer, retType = byte) }
    val CFPlugInUnregisterPlugInType by func { declare(pointer, pointer, retType = byte) }
    val CFPreferencesAddSuitePreferencesToApp by func { declare(pointer, pointer) }
    val CFPreferencesAppSynchronize by func { declare(pointer, retType = byte) }
    val CFPreferencesAppValueIsForced by func { declare(pointer, pointer, retType = byte) }
    val CFPreferencesCopyAppValue by func { declare(pointer, pointer, retType = pointer) }
    val CFPreferencesCopyApplicationList by func { declare(pointer, pointer, retType = pointer) }
    val CFPreferencesCopyKeyList by func { declare(pointer, pointer, pointer, retType = pointer) }
    val CFPreferencesCopyMultiple by func { declare(pointer, pointer, pointer, pointer, retType = pointer) }
    val CFPreferencesCopyValue by func { declare(pointer, pointer, pointer, pointer, retType = pointer) }
    val CFPreferencesGetAppBooleanValue by func { declare(pointer, pointer, pointer, retType = byte) }
    val CFPreferencesGetAppIntegerValue by func { declare(pointer, pointer, pointer, retType = int64) }
    val CFPreferencesRemoveSuitePreferencesFromApp by func { declare(pointer, pointer) }
    val CFPreferencesSetAppValue by func { declare(pointer, pointer, pointer) }
    val CFPreferencesSetMultiple by func { declare(pointer, pointer, pointer, pointer, pointer) }
    val CFPreferencesSetValue by func { declare(pointer, pointer, pointer, pointer, pointer) }
    val CFPreferencesSynchronize by func { declare(pointer, pointer, pointer, retType = byte) }
    val CFPropertyListCreateData by func { declare(pointer, pointer, int64, int64, pointer, retType = pointer) }
    val CFPropertyListCreateDeepCopy by func { declare(pointer, pointer, int64, retType = pointer) }
    val CFPropertyListCreateFromStream by func { declare(pointer, pointer, int64, int64, pointer, pointer, retType = pointer) }
    val CFPropertyListCreateFromXMLData by func { declare(pointer, pointer, int64, pointer, retType = pointer) }
    val CFPropertyListCreateWithData by func { declare(pointer, pointer, int64, pointer, pointer, retType = pointer) }
    val CFPropertyListCreateWithStream by func { declare(pointer, pointer, int64, int64, pointer, pointer, retType = pointer) }
    val CFPropertyListCreateXMLData by func { declare(pointer, pointer, retType = pointer) }
    val CFPropertyListIsValid by func { declare(pointer, int64, retType = byte) }
    val CFPropertyListWrite by func { declare(pointer, pointer, int64, int64, pointer, retType = int64) }
    val CFPropertyListWriteToStream by func { declare(pointer, pointer, int64, pointer, retType = int64) }
    val CFReadStreamClose by func { declare(pointer) }
    val CFReadStreamCopyDispatchQueue by func { declare(pointer, retType = pointer) }
    val CFReadStreamCopyError by func { declare(pointer, retType = pointer) }
    val CFReadStreamCopyProperty by func { declare(pointer, pointer, retType = pointer) }
    val CFReadStreamCreateWithBytesNoCopy by func { declare(pointer, pointer, int64, pointer, retType = pointer) }
    val CFReadStreamCreateWithFile by func { declare(pointer, pointer, retType = pointer) }
    val CFReadStreamGetBuffer by func { declare(pointer, int64, pointer, retType = pointer) }
    val CFReadStreamGetError by func { declare(pointer, retType = MacOSDef.CFStreamError) }
    val CFReadStreamGetStatus by func { declare(pointer, retType = int64) }
    val CFReadStreamGetTypeID by func { declare(retType = int64) }
    val CFReadStreamHasBytesAvailable by func { declare(pointer, retType = byte) }
    val CFReadStreamOpen by func { declare(pointer, retType = byte) }
    val CFReadStreamRead by func { declare(pointer, pointer, int64, retType = int64) }
    val CFReadStreamScheduleWithRunLoop by func { declare(pointer, pointer, pointer) }
    val CFReadStreamSetClient by func { declare(pointer, int64, pointer, pointer, retType = byte) }
    val CFReadStreamSetDispatchQueue by func { declare(pointer, pointer) }
    val CFReadStreamSetProperty by func { declare(pointer, pointer, pointer, retType = byte) }
    val CFReadStreamUnscheduleFromRunLoop by func { declare(pointer, pointer, pointer) }
    val CFRelease by func { declare(pointer) }
    val CFRetain by func { declare(pointer, retType = pointer) }
    val CFRunLoopAddCommonMode by func { declare(pointer, pointer) }
    val CFRunLoopAddObserver by func { declare(pointer, pointer, pointer) }
    val CFRunLoopAddSource by func { declare(pointer, pointer, pointer) }
    val CFRunLoopAddTimer by func { declare(pointer, pointer, pointer) }
    val CFRunLoopContainsObserver by func { declare(pointer, pointer, pointer, retType = byte) }
    val CFRunLoopContainsSource by func { declare(pointer, pointer, pointer, retType = byte) }
    val CFRunLoopContainsTimer by func { declare(pointer, pointer, pointer, retType = byte) }
    val CFRunLoopCopyAllModes by func { declare(pointer, retType = pointer) }
    val CFRunLoopCopyCurrentMode by func { declare(pointer, retType = pointer) }
    val CFRunLoopGetCurrent by func { declare(retType = pointer) }
    val CFRunLoopGetMain by func { declare(retType = pointer) }
    val CFRunLoopGetNextTimerFireDate by func { declare(pointer, pointer, retType = float64) }
    val CFRunLoopGetTypeID by func { declare(retType = int64) }
    val CFRunLoopIsWaiting by func { declare(pointer, retType = byte) }
    val CFRunLoopObserverCreate by func { declare(pointer, int64, byte, int64, pointer, pointer, retType = pointer) }
    val CFRunLoopObserverCreateWithHandler by func { declare(pointer, int64, byte, int64, pointer, retType = pointer) }
    val CFRunLoopObserverDoesRepeat by func { declare(pointer, retType = byte) }
    val CFRunLoopObserverGetActivities by func { declare(pointer, retType = int64) }
    val CFRunLoopObserverGetContext by func { declare(pointer, pointer) }
    val CFRunLoopObserverGetOrder by func { declare(pointer, retType = int64) }
    val CFRunLoopObserverGetTypeID by func { declare(retType = int64) }
    val CFRunLoopObserverInvalidate by func { declare(pointer) }
    val CFRunLoopObserverIsValid by func { declare(pointer, retType = byte) }
    val CFRunLoopPerformBlock by func { declare(pointer, pointer, pointer) }
    val CFRunLoopRemoveObserver by func { declare(pointer, pointer, pointer) }
    val CFRunLoopRemoveSource by func { declare(pointer, pointer, pointer) }
    val CFRunLoopRemoveTimer by func { declare(pointer, pointer, pointer) }
    val CFRunLoopRun by func { declare() }
    val CFRunLoopRunInMode by func { declare(pointer, float64, byte, retType = int32) }
    val CFRunLoopSourceCreate by func { declare(pointer, int64, pointer, retType = pointer) }
    val CFRunLoopSourceGetContext by func { declare(pointer, pointer) }
    val CFRunLoopSourceGetOrder by func { declare(pointer, retType = int64) }
    val CFRunLoopSourceGetTypeID by func { declare(retType = int64) }
    val CFRunLoopSourceInvalidate by func { declare(pointer) }
    val CFRunLoopSourceIsValid by func { declare(pointer, retType = byte) }
    val CFRunLoopSourceSignal by func { declare(pointer) }
    val CFRunLoopStop by func { declare(pointer) }
    val CFRunLoopTimerCreate by func { declare(pointer, float64, float64, int64, int64, pointer, pointer, retType = pointer) }
    val CFRunLoopTimerCreateWithHandler by func { declare(pointer, float64, float64, int64, int64, pointer, retType = pointer) }
    val CFRunLoopTimerDoesRepeat by func { declare(pointer, retType = byte) }
    val CFRunLoopTimerGetContext by func { declare(pointer, pointer) }
    val CFRunLoopTimerGetInterval by func { declare(pointer, retType = float64) }
    val CFRunLoopTimerGetNextFireDate by func { declare(pointer, retType = float64) }
    val CFRunLoopTimerGetOrder by func { declare(pointer, retType = int64) }
    val CFRunLoopTimerGetTolerance by func { declare(pointer, retType = float64) }
    val CFRunLoopTimerGetTypeID by func { declare(retType = int64) }
    val CFRunLoopTimerInvalidate by func { declare(pointer) }
    val CFRunLoopTimerIsValid by func { declare(pointer, retType = byte) }
    val CFRunLoopTimerSetNextFireDate by func { declare(pointer, float64) }
    val CFRunLoopTimerSetTolerance by func { declare(pointer, float64) }
    val CFRunLoopWakeUp by func { declare(pointer) }
    val CFSetAddValue by func { declare(pointer, pointer) }
    val CFSetApplyFunction by func { declare(pointer, pointer, pointer) }
    val CFSetContainsValue by func { declare(pointer, pointer, retType = byte) }
    val CFSetCreate by func { declare(pointer, pointer, int64, pointer, retType = pointer) }
    val CFSetCreateCopy by func { declare(pointer, pointer, retType = pointer) }
    val CFSetCreateMutable by func { declare(pointer, int64, pointer, retType = pointer) }
    val CFSetCreateMutableCopy by func { declare(pointer, int64, pointer, retType = pointer) }
    val CFSetGetCount by func { declare(pointer, retType = int64) }
    val CFSetGetCountOfValue by func { declare(pointer, pointer, retType = int64) }
    val CFSetGetTypeID by func { declare(retType = int64) }
    val CFSetGetValue by func { declare(pointer, pointer, retType = pointer) }
    val CFSetGetValueIfPresent by func { declare(pointer, pointer, pointer, retType = byte) }
    val CFSetGetValues by func { declare(pointer, pointer) }
    val CFSetRemoveAllValues by func { declare(pointer) }
    val CFSetRemoveValue by func { declare(pointer, pointer) }
    val CFSetReplaceValue by func { declare(pointer, pointer) }
    val CFSetSetValue by func { declare(pointer, pointer) }
    val CFShow by func { declare(pointer) }
    val CFShowStr by func { declare(pointer) }
    val CFSocketConnectToAddress by func { declare(pointer, pointer, float64, retType = int64) }
    val CFSocketCopyAddress by func { declare(pointer, retType = pointer) }
    val CFSocketCopyPeerAddress by func { declare(pointer, retType = pointer) }
    val CFSocketCopyRegisteredSocketSignature by func { declare(pointer, float64, pointer, pointer, pointer, retType = int64) }
    val CFSocketCopyRegisteredValue by func { declare(pointer, float64, pointer, pointer, pointer, retType = int64) }
    val CFSocketCreate by func { declare(pointer, int32, int32, int32, int64, pointer, pointer, retType = pointer) }
    val CFSocketCreateConnectedToSocketSignature by func { declare(pointer, pointer, int64, pointer, pointer, float64, retType = pointer) }
    val CFSocketCreateRunLoopSource by func { declare(pointer, pointer, int64, retType = pointer) }
    val CFSocketCreateWithNative by func { declare(pointer, int32, int64, pointer, pointer, retType = pointer) }
    val CFSocketCreateWithSocketSignature by func { declare(pointer, pointer, int64, pointer, pointer, retType = pointer) }
    val CFSocketDisableCallBacks by func { declare(pointer, int64) }
    val CFSocketEnableCallBacks by func { declare(pointer, int64) }
    val CFSocketGetContext by func { declare(pointer, pointer) }
    val CFSocketGetDefaultNameRegistryPortNumber by func { declare(retType = int16) }
    val CFSocketGetNative by func { declare(pointer, retType = int32) }
    val CFSocketGetSocketFlags by func { declare(pointer, retType = int64) }
    val CFSocketGetTypeID by func { declare(retType = int64) }
    val CFSocketInvalidate by func { declare(pointer) }
    val CFSocketIsValid by func { declare(pointer, retType = byte) }
    val CFSocketRegisterSocketSignature by func { declare(pointer, float64, pointer, pointer, retType = int64) }
    val CFSocketRegisterValue by func { declare(pointer, float64, pointer, pointer, retType = int64) }
    val CFSocketSendData by func { declare(pointer, pointer, pointer, float64, retType = int64) }
    val CFSocketSetAddress by func { declare(pointer, pointer, retType = int64) }
    val CFSocketSetDefaultNameRegistryPortNumber by func { declare(int16) }
    val CFSocketSetSocketFlags by func { declare(pointer, int64) }
    val CFSocketUnregister by func { declare(pointer, float64, pointer, retType = int64) }
    val CFStreamCreateBoundPair by func { declare(pointer, pointer, pointer, int64) }
    val CFStreamCreatePairWithPeerSocketSignature by func { declare(pointer, pointer, pointer, pointer) }
    val CFStreamCreatePairWithSocket by func { declare(pointer, int32, pointer, pointer) }
    val CFStreamCreatePairWithSocketToHost by func { declare(pointer, pointer, int32, pointer, pointer) }
    val CFStringAppend by func { declare(pointer, pointer) }
    val CFStringAppendCString by func { declare(pointer, pointer, int32) }
    val CFStringAppendCharacters by func { declare(pointer, pointer, int64) }
    val CFStringAppendPascalString by func { declare(pointer, pointer, int32) }
    val CFStringCapitalize by func { declare(pointer, pointer) }
    val CFStringCompare by func { declare(pointer, pointer, int64, retType = int64) }
    val CFStringCompareWithOptions by func { declare(pointer, pointer, MacOSDef.CFRange, int64, retType = int64) }
    val CFStringCompareWithOptionsAndLocale by func { declare(pointer, pointer, MacOSDef.CFRange, int64, pointer, retType = int64) }
    val CFStringConvertEncodingToIANACharSetName by func { declare(int32, retType = pointer) }
    val CFStringConvertEncodingToNSStringEncoding by func { declare(int32, retType = int64) }
    val CFStringConvertEncodingToWindowsCodepage by func { declare(int32, retType = int32) }
    val CFStringConvertIANACharSetNameToEncoding by func { declare(pointer, retType = int32) }
    val CFStringConvertNSStringEncodingToEncoding by func { declare(int64, retType = int32) }
    val CFStringConvertWindowsCodepageToEncoding by func { declare(int32, retType = int32) }
    val CFStringCreateArrayBySeparatingStrings by func { declare(pointer, pointer, pointer, retType = pointer) }
    val CFStringCreateArrayWithFindResults by func { declare(pointer, pointer, pointer, MacOSDef.CFRange, int64, retType = pointer) }
    val CFStringCreateByCombiningStrings by func { declare(pointer, pointer, pointer, retType = pointer) }
    val CFStringCreateCopy by func { declare(pointer, pointer, retType = pointer) }
    val CFStringCreateExternalRepresentation by func { declare(pointer, pointer, int32, byte, retType = pointer) }
    val CFStringCreateFromExternalRepresentation by func { declare(pointer, pointer, int32, retType = pointer) }
    val CFStringCreateMutable by func { declare(pointer, int64, retType = pointer) }
    val CFStringCreateMutableCopy by func { declare(pointer, int64, pointer, retType = pointer) }
    val CFStringCreateMutableWithExternalCharactersNoCopy by func { declare(pointer, pointer, int64, int64, pointer, retType = pointer) }
    val CFStringCreateWithBytes by func { declare(pointer, pointer, int64, int32, byte, retType = pointer) }
    val CFStringCreateWithBytesNoCopy by func { declare(pointer, pointer, int64, int32, byte, pointer, retType = pointer) }
    val CFStringCreateWithCString by func { declare(pointer, pointer, int32, retType = pointer) }
    val CFStringCreateWithCStringNoCopy by func { declare(pointer, pointer, int32, pointer, retType = pointer) }
    val CFStringCreateWithCharacters by func { declare(pointer, pointer, int64, retType = pointer) }
    val CFStringCreateWithCharactersNoCopy by func { declare(pointer, pointer, int64, pointer, retType = pointer) }
    val CFStringCreateWithFileSystemRepresentation by func { declare(pointer, pointer, retType = pointer) }
    val CFStringCreateWithPascalString by func { declare(pointer, pointer, int32, retType = pointer) }
    val CFStringCreateWithPascalStringNoCopy by func { declare(pointer, pointer, int32, pointer, retType = pointer) }
    val CFStringCreateWithSubstring by func { declare(pointer, pointer, MacOSDef.CFRange, retType = pointer) }
    val CFStringDelete by func { declare(pointer, MacOSDef.CFRange) }
    val CFStringFind by func { declare(pointer, pointer, int64, retType = MacOSDef.CFRange) }
    val CFStringFindAndReplace by func { declare(pointer, pointer, pointer, MacOSDef.CFRange, int64, retType = int64) }
    val CFStringFindCharacterFromSet by func { declare(pointer, pointer, MacOSDef.CFRange, int64, pointer, retType = byte) }
    val CFStringFindWithOptions by func { declare(pointer, pointer, MacOSDef.CFRange, int64, pointer, retType = byte) }
    val CFStringFindWithOptionsAndLocale by func { declare(pointer, pointer, MacOSDef.CFRange, int64, pointer, pointer, retType = byte) }
    val CFStringFold by func { declare(pointer, int64, pointer) }
    val CFStringGetBytes by func { declare(pointer, MacOSDef.CFRange, int32, byte, byte, pointer, int64, pointer, retType = int64) }
    val CFStringGetCString by func { declare(pointer, pointer, int64, int32, retType = byte) }
    val CFStringGetCStringPtr by func { declare(pointer, int32, retType = pointer) }
    val CFStringGetCharacterAtIndex by func { declare(pointer, int64, retType = int16) }
    val CFStringGetCharacters by func { declare(pointer, MacOSDef.CFRange, pointer) }
    val CFStringGetCharactersPtr by func { declare(pointer, retType = pointer) }
    val CFStringGetDoubleValue by func { declare(pointer, retType = float64) }
    val CFStringGetFastestEncoding by func { declare(pointer, retType = int32) }
    val CFStringGetFileSystemRepresentation by func { declare(pointer, pointer, int64, retType = byte) }
    val CFStringGetHyphenationLocationBeforeIndex by func { declare(pointer, int64, MacOSDef.CFRange, int64, pointer, pointer, retType = int64) }
    val CFStringGetIntValue by func { declare(pointer, retType = int32) }
    val CFStringGetLength by func { declare(pointer, retType = int64) }
    val CFStringGetLineBounds by func { declare(pointer, MacOSDef.CFRange, pointer, pointer, pointer) }
    val CFStringGetListOfAvailableEncodings by func { declare(retType = pointer) }
    val CFStringGetMaximumSizeForEncoding by func { declare(int64, int32, retType = int64) }
    val CFStringGetMaximumSizeOfFileSystemRepresentation by func { declare(pointer, retType = int64) }
    val CFStringGetMostCompatibleMacStringEncoding by func { declare(int32, retType = int32) }
    val CFStringGetNameOfEncoding by func { declare(int32, retType = pointer) }
    val CFStringGetParagraphBounds by func { declare(pointer, MacOSDef.CFRange, pointer, pointer, pointer) }
    val CFStringGetPascalString by func { declare(pointer, pointer, int64, int32, retType = byte) }
    val CFStringGetPascalStringPtr by func { declare(pointer, int32, retType = pointer) }
    val CFStringGetRangeOfComposedCharactersAtIndex by func { declare(pointer, int64, retType = MacOSDef.CFRange) }
    val CFStringGetSmallestEncoding by func { declare(pointer, retType = int32) }
    val CFStringGetSystemEncoding by func { declare(retType = int32) }
    val CFStringGetTypeID by func { declare(retType = int64) }
    val CFStringHasPrefix by func { declare(pointer, pointer, retType = byte) }
    val CFStringHasSuffix by func { declare(pointer, pointer, retType = byte) }
    val CFStringInsert by func { declare(pointer, int64, pointer) }
    val CFStringIsEncodingAvailable by func { declare(int32, retType = byte) }
    val CFStringIsHyphenationAvailableForLocale by func { declare(pointer, retType = byte) }
    val CFStringLowercase by func { declare(pointer, pointer) }
    val CFStringNormalize by func { declare(pointer, int64) }
    val CFStringPad by func { declare(pointer, pointer, int64, int64) }
    val CFStringReplace by func { declare(pointer, MacOSDef.CFRange, pointer) }
    val CFStringReplaceAll by func { declare(pointer, pointer) }
    val CFStringSetExternalCharactersNoCopy by func { declare(pointer, pointer, int64, int64) }
    val CFStringTokenizerAdvanceToNextToken by func { declare(pointer, retType = int64) }
    val CFStringTokenizerCopyBestStringLanguage by func { declare(pointer, MacOSDef.CFRange, retType = pointer) }
    val CFStringTokenizerCopyCurrentTokenAttribute by func { declare(pointer, int64, retType = pointer) }
    val CFStringTokenizerCreate by func { declare(pointer, pointer, MacOSDef.CFRange, int64, pointer, retType = pointer) }
    val CFStringTokenizerGetCurrentSubTokens by func { declare(pointer, pointer, int64, pointer, retType = int64) }
    val CFStringTokenizerGetCurrentTokenRange by func { declare(pointer, retType = MacOSDef.CFRange) }
    val CFStringTokenizerGetTypeID by func { declare(retType = int64) }
    val CFStringTokenizerGoToTokenAtIndex by func { declare(pointer, int64, retType = int64) }
    val CFStringTokenizerSetString by func { declare(pointer, pointer, MacOSDef.CFRange) }
    val CFStringTransform by func { declare(pointer, pointer, pointer, byte, retType = byte) }
    val CFStringTrim by func { declare(pointer, pointer) }
    val CFStringTrimWhitespace by func { declare(pointer) }
    val CFStringUppercase by func { declare(pointer, pointer) }
    val CFTimeZoneCopyAbbreviation by func { declare(pointer, float64, retType = pointer) }
    val CFTimeZoneCopyAbbreviationDictionary by func { declare(retType = pointer) }
    val CFTimeZoneCopyDefault by func { declare(retType = pointer) }
    val CFTimeZoneCopyKnownNames by func { declare(retType = pointer) }
    val CFTimeZoneCopyLocalizedName by func { declare(pointer, int64, pointer, retType = pointer) }
    val CFTimeZoneCopySystem by func { declare(retType = pointer) }
    val CFTimeZoneCreate by func { declare(pointer, pointer, pointer, retType = pointer) }
    val CFTimeZoneCreateWithName by func { declare(pointer, pointer, byte, retType = pointer) }
    val CFTimeZoneCreateWithTimeIntervalFromGMT by func { declare(pointer, float64, retType = pointer) }
    val CFTimeZoneGetData by func { declare(pointer, retType = pointer) }
    val CFTimeZoneGetDaylightSavingTimeOffset by func { declare(pointer, float64, retType = float64) }
    val CFTimeZoneGetName by func { declare(pointer, retType = pointer) }
    val CFTimeZoneGetNextDaylightSavingTimeTransition by func { declare(pointer, float64, retType = float64) }
    val CFTimeZoneGetSecondsFromGMT by func { declare(pointer, float64, retType = float64) }
    val CFTimeZoneGetTypeID by func { declare(retType = int64) }
    val CFTimeZoneIsDaylightSavingTime by func { declare(pointer, float64, retType = byte) }
    val CFTimeZoneResetSystem by func { declare() }
    val CFTimeZoneSetAbbreviationDictionary by func { declare(pointer) }
    val CFTimeZoneSetDefault by func { declare(pointer) }
    val CFTreeAppendChild by func { declare(pointer, pointer) }
    val CFTreeApplyFunctionToChildren by func { declare(pointer, pointer, pointer) }
    val CFTreeCreate by func { declare(pointer, pointer, retType = pointer) }
    val CFTreeFindRoot by func { declare(pointer, retType = pointer) }
    val CFTreeGetChildAtIndex by func { declare(pointer, int64, retType = pointer) }
    val CFTreeGetChildCount by func { declare(pointer, retType = int64) }
    val CFTreeGetChildren by func { declare(pointer, pointer) }
    val CFTreeGetContext by func { declare(pointer, pointer) }
    val CFTreeGetFirstChild by func { declare(pointer, retType = pointer) }
    val CFTreeGetNextSibling by func { declare(pointer, retType = pointer) }
    val CFTreeGetParent by func { declare(pointer, retType = pointer) }
    val CFTreeGetTypeID by func { declare(retType = int64) }
    val CFTreeInsertSibling by func { declare(pointer, pointer) }
    val CFTreePrependChild by func { declare(pointer, pointer) }
    val CFTreeRemove by func { declare(pointer) }
    val CFTreeRemoveAllChildren by func { declare(pointer) }
    val CFTreeSetContext by func { declare(pointer, pointer) }
    val CFTreeSortChildren by func { declare(pointer, pointer, pointer) }
    val CFURLCanBeDecomposed by func { declare(pointer, retType = byte) }
    val CFURLClearResourcePropertyCache by func { declare(pointer) }
    val CFURLClearResourcePropertyCacheForKey by func { declare(pointer, pointer) }
    val CFURLCopyAbsoluteURL by func { declare(pointer, retType = pointer) }
    val CFURLCopyFileSystemPath by func { declare(pointer, int64, retType = pointer) }
    val CFURLCopyFragment by func { declare(pointer, pointer, retType = pointer) }
    val CFURLCopyHostName by func { declare(pointer, retType = pointer) }
    val CFURLCopyLastPathComponent by func { declare(pointer, retType = pointer) }
    val CFURLCopyNetLocation by func { declare(pointer, retType = pointer) }
    val CFURLCopyParameterString by func { declare(pointer, pointer, retType = pointer) }
    val CFURLCopyPassword by func { declare(pointer, retType = pointer) }
    val CFURLCopyPath by func { declare(pointer, retType = pointer) }
    val CFURLCopyPathExtension by func { declare(pointer, retType = pointer) }
    val CFURLCopyQueryString by func { declare(pointer, pointer, retType = pointer) }
    val CFURLCopyResourcePropertiesForKeys by func { declare(pointer, pointer, pointer, retType = pointer) }
    val CFURLCopyResourcePropertyForKey by func { declare(pointer, pointer, pointer, pointer, retType = byte) }
    val CFURLCopyResourceSpecifier by func { declare(pointer, retType = pointer) }
    val CFURLCopyScheme by func { declare(pointer, retType = pointer) }
    val CFURLCopyStrictPath by func { declare(pointer, pointer, retType = pointer) }
    val CFURLCopyUserName by func { declare(pointer, retType = pointer) }
    val CFURLCreateAbsoluteURLWithBytes by func { declare(pointer, pointer, int64, int32, pointer, byte, retType = pointer) }
    val CFURLCreateBookmarkData by func { declare(pointer, pointer, int64, pointer, pointer, pointer, retType = pointer) }
    val CFURLCreateBookmarkDataFromAliasRecord by func { declare(pointer, pointer, retType = pointer) }
    val CFURLCreateBookmarkDataFromFile by func { declare(pointer, pointer, pointer, retType = pointer) }
    val CFURLCreateByResolvingBookmarkData by func { declare(pointer, pointer, int64, pointer, pointer, pointer, pointer, retType = pointer) }
    val CFURLCreateCopyAppendingPathComponent by func { declare(pointer, pointer, pointer, byte, retType = pointer) }
    val CFURLCreateCopyAppendingPathExtension by func { declare(pointer, pointer, pointer, retType = pointer) }
    val CFURLCreateCopyDeletingLastPathComponent by func { declare(pointer, pointer, retType = pointer) }
    val CFURLCreateCopyDeletingPathExtension by func { declare(pointer, pointer, retType = pointer) }
    val CFURLCreateData by func { declare(pointer, pointer, int32, byte, retType = pointer) }
    val CFURLCreateDataAndPropertiesFromResource by func { declare(pointer, pointer, pointer, pointer, pointer, pointer, retType = byte) }
    val CFURLCreateFilePathURL by func { declare(pointer, pointer, pointer, retType = pointer) }
    val CFURLCreateFileReferenceURL by func { declare(pointer, pointer, pointer, retType = pointer) }
    val CFURLCreateFromFSRef by func { declare(pointer, pointer, retType = pointer) }
    val CFURLCreateFromFileSystemRepresentation by func { declare(pointer, pointer, int64, byte, retType = pointer) }
    val CFURLCreateFromFileSystemRepresentationRelativeToBase by func { declare(pointer, pointer, int64, byte, pointer, retType = pointer) }
    val CFURLCreatePropertyFromResource by func { declare(pointer, pointer, pointer, pointer, retType = pointer) }
    val CFURLCreateResourcePropertiesForKeysFromBookmarkData by func { declare(pointer, pointer, pointer, retType = pointer) }
    val CFURLCreateResourcePropertyForKeyFromBookmarkData by func { declare(pointer, pointer, pointer, retType = pointer) }
    val CFURLCreateStringByAddingPercentEscapes by func { declare(pointer, pointer, pointer, pointer, int32, retType = pointer) }
    val CFURLCreateStringByReplacingPercentEscapes by func { declare(pointer, pointer, pointer, retType = pointer) }
    val CFURLCreateStringByReplacingPercentEscapesUsingEncoding by func { declare(pointer, pointer, pointer, int32, retType = pointer) }
    val CFURLCreateWithBytes by func { declare(pointer, pointer, int64, int32, pointer, retType = pointer) }
    val CFURLCreateWithFileSystemPath by func { declare(pointer, pointer, int64, byte, retType = pointer) }
    val CFURLCreateWithFileSystemPathRelativeToBase by func { declare(pointer, pointer, int64, byte, pointer, retType = pointer) }
    val CFURLCreateWithString by func { declare(pointer, pointer, pointer, retType = pointer) }
    val CFURLDestroyResource by func { declare(pointer, pointer, retType = byte) }
    val CFURLEnumeratorCreateForDirectoryURL by func { declare(pointer, pointer, int64, pointer, retType = pointer) }
    val CFURLEnumeratorCreateForMountedVolumes by func { declare(pointer, int64, pointer, retType = pointer) }
    val CFURLEnumeratorGetDescendentLevel by func { declare(pointer, retType = int64) }
    val CFURLEnumeratorGetNextURL by func { declare(pointer, pointer, pointer, retType = int64) }
    val CFURLEnumeratorGetSourceDidChange by func { declare(pointer, retType = byte) }
    val CFURLEnumeratorGetTypeID by func { declare(retType = int64) }
    val CFURLEnumeratorSkipDescendents by func { declare(pointer) }
    val CFURLGetBaseURL by func { declare(pointer, retType = pointer) }
    val CFURLGetByteRangeForComponent by func { declare(pointer, int64, pointer, retType = MacOSDef.CFRange) }
    val CFURLGetBytes by func { declare(pointer, pointer, int64, retType = int64) }
    val CFURLGetFSRef by func { declare(pointer, pointer, retType = byte) }
    val CFURLGetFileSystemRepresentation by func { declare(pointer, byte, pointer, int64, retType = byte) }
    val CFURLGetPortNumber by func { declare(pointer, retType = int32) }
    val CFURLGetString by func { declare(pointer, retType = pointer) }
    val CFURLGetTypeID by func { declare(retType = int64) }
    val CFURLHasDirectoryPath by func { declare(pointer, retType = byte) }
    val CFURLIsFileReferenceURL by func { declare(pointer, retType = byte) }
    val CFURLResourceIsReachable by func { declare(pointer, pointer, retType = byte) }
    val CFURLSetResourcePropertiesForKeys by func { declare(pointer, pointer, pointer, retType = byte) }
    val CFURLSetResourcePropertyForKey by func { declare(pointer, pointer, pointer, pointer, retType = byte) }
    val CFURLSetTemporaryResourcePropertyForKey by func { declare(pointer, pointer, pointer) }
    val CFURLStartAccessingSecurityScopedResource by func { declare(pointer, retType = byte) }
    val CFURLStopAccessingSecurityScopedResource by func { declare(pointer) }
    val CFURLWriteBookmarkDataToFile by func { declare(pointer, pointer, int64, pointer, retType = byte) }
    val CFURLWriteDataAndPropertiesToResource by func { declare(pointer, pointer, pointer, pointer, retType = byte) }
    val CFUUIDCreate by func { declare(pointer, retType = pointer) }
    val CFUUIDCreateFromString by func { declare(pointer, pointer, retType = pointer) }
    val CFUUIDCreateFromUUIDBytes by func { declare(pointer, MacOSDef.CFUUIDBytes, retType = pointer) }
    val CFUUIDCreateString by func { declare(pointer, pointer, retType = pointer) }
    val CFUUIDCreateWithBytes by func { declare(pointer, byte, byte, byte, byte, byte, byte, byte, byte, byte, byte, byte, byte, byte, byte, byte, byte, retType = pointer) }
    val CFUUIDGetConstantUUIDWithBytes by func { declare(pointer, byte, byte, byte, byte, byte, byte, byte, byte, byte, byte, byte, byte, byte, byte, byte, byte, retType = pointer) }
    val CFUUIDGetTypeID by func { declare(retType = int64) }
    val CFUUIDGetUUIDBytes by func { declare(pointer, retType = MacOSDef.CFUUIDBytes) }
    val CFUserNotificationCancel by func { declare(pointer, retType = int32) }
    val CFUserNotificationCreate by func { declare(pointer, float64, int64, pointer, pointer, retType = pointer) }
    val CFUserNotificationCreateRunLoopSource by func { declare(pointer, pointer, pointer, int64, retType = pointer) }
    val CFUserNotificationDisplayAlert by func { declare(float64, int64, pointer, pointer, pointer, pointer, pointer, pointer, pointer, pointer, pointer, retType = int32) }
    val CFUserNotificationDisplayNotice by func { declare(float64, int64, pointer, pointer, pointer, pointer, pointer, pointer, retType = int32) }
    val CFUserNotificationGetResponseDictionary by func { declare(pointer, retType = pointer) }
    val CFUserNotificationGetResponseValue by func { declare(pointer, pointer, int64, retType = pointer) }
    val CFUserNotificationGetTypeID by func { declare(retType = int64) }
    val CFUserNotificationReceiveResponse by func { declare(pointer, float64, pointer, retType = int32) }
    val CFUserNotificationUpdate by func { declare(pointer, float64, int64, pointer, retType = int32) }
    val CFWriteStreamCanAcceptBytes by func { declare(pointer, retType = byte) }
    val CFWriteStreamClose by func { declare(pointer) }
    val CFWriteStreamCopyDispatchQueue by func { declare(pointer, retType = pointer) }
    val CFWriteStreamCopyError by func { declare(pointer, retType = pointer) }
    val CFWriteStreamCopyProperty by func { declare(pointer, pointer, retType = pointer) }
    val CFWriteStreamCreateWithAllocatedBuffers by func { declare(pointer, pointer, retType = pointer) }
    val CFWriteStreamCreateWithBuffer by func { declare(pointer, pointer, int64, retType = pointer) }
    val CFWriteStreamCreateWithFile by func { declare(pointer, pointer, retType = pointer) }
    val CFWriteStreamGetError by func { declare(pointer, retType = MacOSDef.CFStreamError) }
    val CFWriteStreamGetStatus by func { declare(pointer, retType = int64) }
    val CFWriteStreamGetTypeID by func { declare(retType = int64) }
    val CFWriteStreamOpen by func { declare(pointer, retType = byte) }
    val CFWriteStreamScheduleWithRunLoop by func { declare(pointer, pointer, pointer) }
    val CFWriteStreamSetClient by func { declare(pointer, int64, pointer, pointer, retType = byte) }
    val CFWriteStreamSetDispatchQueue by func { declare(pointer, pointer) }
    val CFWriteStreamSetProperty by func { declare(pointer, pointer, pointer, retType = byte) }
    val CFWriteStreamUnscheduleFromRunLoop by func { declare(pointer, pointer, pointer) }
    val CFWriteStreamWrite by func { declare(pointer, pointer, int64, retType = int64) }
    val CFXMLCreateStringByEscapingEntities by func { declare(pointer, pointer, pointer, retType = pointer) }
    val CFXMLCreateStringByUnescapingEntities by func { declare(pointer, pointer, pointer, retType = pointer) }
    val CFXMLNodeCreate by func { declare(pointer, int64, pointer, pointer, int64, retType = pointer) }
    val CFXMLNodeCreateCopy by func { declare(pointer, pointer, retType = pointer) }
    val CFXMLNodeGetInfoPtr by func { declare(pointer, retType = pointer) }
    val CFXMLNodeGetString by func { declare(pointer, retType = pointer) }
    val CFXMLNodeGetTypeCode by func { declare(pointer, retType = int64) }
    val CFXMLNodeGetTypeID by func { declare(retType = int64) }
    val CFXMLNodeGetVersion by func { declare(pointer, retType = int64) }
    val CFXMLParserAbort by func { declare(pointer, int64, pointer) }
    val CFXMLParserCopyErrorDescription by func { declare(pointer, retType = pointer) }
    val CFXMLParserCreate by func { declare(pointer, pointer, pointer, int64, int64, pointer, pointer, retType = pointer) }
    val CFXMLParserCreateWithDataFromURL by func { declare(pointer, pointer, int64, int64, pointer, pointer, retType = pointer) }
    val CFXMLParserGetCallBacks by func { declare(pointer, pointer) }
    val CFXMLParserGetContext by func { declare(pointer, pointer) }
    val CFXMLParserGetDocument by func { declare(pointer, retType = pointer) }
    val CFXMLParserGetLineNumber by func { declare(pointer, retType = int64) }
    val CFXMLParserGetLocation by func { declare(pointer, retType = int64) }
    val CFXMLParserGetSourceURL by func { declare(pointer, retType = pointer) }
    val CFXMLParserGetStatusCode by func { declare(pointer, retType = int64) }
    val CFXMLParserGetTypeID by func { declare(retType = int64) }
    val CFXMLParserParse by func { declare(pointer, retType = byte) }
    val CFXMLTreeCreateFromData by func { declare(pointer, pointer, pointer, int64, int64, retType = pointer) }
    val CFXMLTreeCreateFromDataWithError by func { declare(pointer, pointer, pointer, int64, int64, pointer, retType = pointer) }
    val CFXMLTreeCreateWithDataFromURL by func { declare(pointer, pointer, int64, int64, retType = pointer) }
    val CFXMLTreeCreateWithNode by func { declare(pointer, pointer, retType = pointer) }
    val CFXMLTreeCreateXMLData by func { declare(pointer, pointer, retType = pointer) }
    val CFXMLTreeGetNode by func { declare(pointer, retType = pointer) }
    val __CFRangeMake by func { declare(int64, int64, retType = MacOSDef.CFRange) }
    val __CFStringMakeConstantString by func { declare(pointer, retType = pointer) }
}
