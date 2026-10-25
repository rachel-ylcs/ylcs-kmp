#ifndef U_STATIC_IMPLEMENTATION
#define U_STATIC_IMPLEMENTATION
#endif

#include "common.h"
#include "unicode/locid.h"
#include "unicode/stringpiece.h"
#include "unicode/uloc.h"
#include "unicode/ustring.h"
#include <string>

static icu::Locale readLocale(KInteropPointer pointer, UErrorCode& status) {
    static const bool loaded = SkLoadICU();
    if (!loaded) {
        status = U_FILE_ACCESS_ERROR;
        return icu::Locale::getRoot();
    }
    std::string identifier = skString(pointer).c_str();
    if (identifier == "C" || identifier == "POSIX" || identifier.compare(0, 2, "C.") == 0) return icu::Locale::getRoot();
    icu::Locale locale;
    if (identifier.find_first_of("_.@") != std::string::npos) {
        // Strip a POSIX codeset while retaining ICU keyword modifiers.
        const size_t codeset = identifier.find('.');
        if (codeset != std::string::npos) {
            const size_t modifier = identifier.find('@', codeset);
            identifier.erase(codeset, modifier == std::string::npos ? std::string::npos : modifier - codeset);
        }
        locale = icu::Locale::createCanonical(identifier.c_str());
    }
    else locale = icu::Locale::forLanguageTag(icu::StringPiece(identifier), status);
    if (locale.isBogus()) status = U_ILLEGAL_ARGUMENT_ERROR;
    if (U_SUCCESS(status)) locale.canonicalize(status);
    return locale;
}

SKIKO_EXPORT KNativePointer org_jetbrains_skia_icu_LocaleUtils__1nLocaleInfo(KInteropPointer identifier, KInt* error) {
    UErrorCode status = U_ZERO_ERROR;
    const icu::Locale locale = readLocale(identifier, status);
    std::string result;
    if (U_SUCCESS(status)) result = locale.toLanguageTag<std::string>(status);
    if (U_SUCCESS(status)) {
        result += '\n';
        result += locale.getLanguage();
        result += '\n';
        result += locale.getScript();
        result += '\n';
        result += locale.getCountry();
        result += '\n';
        result += uloc_isRightToLeft(locale.getName()) ? "rtl" : "ltr";
    }
    *error = status;
    return U_FAILURE(status) ? 0 : reinterpret_cast<KNativePointer>(new SkString(result));
}

SKIKO_EXPORT KInt org_jetbrains_skia_icu_LocaleUtils__1nMapCase(KInteropPointer source, KInt length, KInteropPointer identifier, KInt mode, KInteropPointer destination, KInt capacity, KInt* error) {
    UErrorCode status = U_ZERO_ERROR;
    if (length < 0 || capacity < 0 || source == nullptr || (capacity > 0 && destination == nullptr)) {
        *error = U_ILLEGAL_ARGUMENT_ERROR;
        return 0;
    }
    const icu::Locale locale = readLocale(identifier, status);
    int32_t result = 0;
    if (U_SUCCESS(status)) {
        const auto* input = reinterpret_cast<const UChar*>(source);
        auto* output = reinterpret_cast<UChar*>(destination);
        if (mode == 0) result = u_strToUpper(output, capacity, input, length, locale.getName(), &status);
        else if (mode == 1) result = u_strToLower(output, capacity, input, length, locale.getName(), &status);
        else if (mode == 2) result = u_strToTitle(output, capacity, input, length, nullptr, locale.getName(), &status);
        else status = U_ILLEGAL_ARGUMENT_ERROR;
    }
    *error = status;
    return result;
}
