#if defined(__linux__)

#include <cerrno>
#include <cfenv>
#include <charconv>
#include <cmath>
#include <cstdlib>
#include <locale.h>
#include <string>

// Skia m157 needs this libstdc++ symbol for its decimal JSON parser on Linux.
// Remove this file when the upstream SDK or the Kotlin/Native sysroot resolves
// the symbol without the shim. Keep the published Skottie consumer regression.
__attribute__((weak, visibility("hidden")))
std::from_chars_result skiaFromChars(const char* first, const char* last, float& value,
                                    std::chars_format format) noexcept
    asm("_ZSt10from_charsPKcS0_RfSt12chars_format");

std::from_chars_result skiaFromChars(const char* first, const char* last, float& value,
                                    std::chars_format format) noexcept {
    if (format != std::chars_format::general) return {first, std::errc::invalid_argument};

    // Copy only the potential decimal token so strtof_l stays within [first, last).
    const char* end = first;
    while (end != last && ((*end >= '0' && *end <= '9') || *end == '.' ||
                           *end == 'e' || *end == 'E' || *end == '+' || *end == '-')) {
        ++end;
    }
    if (end == first || *first == '+') return {first, std::errc::invalid_argument};

    const int savedErrno = errno;
    const std::string token(first, end);
    static const locale_t numericLocale = newlocale(LC_NUMERIC_MASK, "C", nullptr);
    if (!numericLocale) {
        errno = savedErrno;
        return {first, std::errc::invalid_argument};
    }

    const int rounding = std::fegetround();
    const bool restoreRounding = rounding != FE_TONEAREST && std::fesetround(FE_TONEAREST) == 0;
    char* parsedEnd = nullptr;
    errno = 0;
    const float parsed = strtof_l(token.c_str(), &parsedEnd, numericLocale);
    const bool outOfRange = errno == ERANGE && (parsed == 0.0f || std::isinf(parsed));
    if (restoreRounding) std::fesetround(rounding);
    errno = savedErrno;

    const auto consumed = parsedEnd - token.c_str();
    if (!consumed) return {first, std::errc::invalid_argument};
    if (outOfRange) return {first + consumed, std::errc::result_out_of_range};
    value = parsed;
    return {first + consumed, {}};
}

#endif
