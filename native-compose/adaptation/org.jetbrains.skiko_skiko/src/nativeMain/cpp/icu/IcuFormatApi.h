#pragma once

#include "unicode/utypes.h"

// Skia distributes the ICU formatting library but only the common public headers.
// These opaque C ABI declarations match ICU's stable udat/udatpg/unum/ucal APIs.
// Symbol renaming and calling conventions come from that same SDK's utypes.h.
U_CAPI void* U_EXPORT2 udat_open(int32_t, int32_t, const char*, const UChar*, int32_t, const UChar*, int32_t, UErrorCode*);
U_CAPI void U_EXPORT2 udat_close(void*);
U_CAPI void U_EXPORT2 udat_setLenient(void*, UBool);
U_CAPI int32_t U_EXPORT2 udat_format(const void*, double, UChar*, int32_t, void*, UErrorCode*);
U_CAPI double U_EXPORT2 udat_parse(const void*, const UChar*, int32_t, int32_t*, UErrorCode*);
U_CAPI int32_t U_EXPORT2 udat_toPattern(const void*, UBool, UChar*, int32_t, UErrorCode*);
U_CAPI int32_t U_EXPORT2 udat_getSymbols(const void*, int32_t, int32_t, UChar*, int32_t, UErrorCode*);
U_CAPI const void* U_EXPORT2 udat_getCalendar(const void*);
U_CAPI void U_EXPORT2 udat_setCalendar(void*, const void*);
U_CAPI void* U_EXPORT2 ucal_open(const UChar*, int32_t, const char*, int32_t, UErrorCode*);
U_CAPI void U_EXPORT2 ucal_close(void*);
U_CAPI void U_EXPORT2 ucal_setGregorianChange(void*, double, UErrorCode*);
U_CAPI int32_t U_EXPORT2 ucal_getAttribute(const void*, int32_t);
U_CAPI void* U_EXPORT2 udatpg_open(const char*, UErrorCode*);
U_CAPI void U_EXPORT2 udatpg_close(void*);
U_CAPI int32_t U_EXPORT2 udatpg_getBestPattern(void*, const UChar*, int32_t, UChar*, int32_t, UErrorCode*);
U_CAPI void* U_EXPORT2 unum_open(int32_t, const UChar*, int32_t, const char*, void*, UErrorCode*);
U_CAPI void U_EXPORT2 unum_close(void*);
U_CAPI void U_EXPORT2 unum_setAttribute(void*, int32_t, int32_t);
U_CAPI int32_t U_EXPORT2 unum_formatInt64(const void*, int64_t, UChar*, int32_t, void*, UErrorCode*);
