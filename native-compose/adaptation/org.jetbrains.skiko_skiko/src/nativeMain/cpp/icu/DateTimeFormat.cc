#ifndef U_STATIC_IMPLEMENTATION
#define U_STATIC_IMPLEMENTATION
#endif

#include "common.h"
#include "IcuFormatApi.h"
#include "unicode/locid.h"
#include "unicode/stringpiece.h"
#include "unicode/unistr.h"
#include <cmath>
#include <memory>
#include <mutex>
#include <string>
#include <utility>
#include <vector>

namespace {

constexpr int32_t kPattern = -2;
constexpr int32_t kNoStyle = -1;
constexpr int32_t kShortStyle = 3;
constexpr int32_t kStandaloneWeekdays = 13;
constexpr int32_t kStandaloneNarrowWeekdays = 15;
constexpr int32_t kFirstDayOfWeek = 1;
constexpr int32_t kDecimal = 1;
constexpr int32_t kGroupingUsed = 1;
constexpr int32_t kMaxIntegerDigits = 3;
constexpr int32_t kMinIntegerDigits = 4;
constexpr int32_t kMaximumResult = 16384;

using DatePointer = std::unique_ptr<void, decltype(&udat_close)>;
using CalendarPointer = std::unique_ptr<void, decltype(&ucal_close)>;
using PatternPointer = std::unique_ptr<void, decltype(&udatpg_close)>;
using NumberPointer = std::unique_ptr<void, decltype(&unum_close)>;

std::string localeName(KInteropPointer pointer, UErrorCode& status) {
    static const bool loaded = SkLoadICU();
    if (!loaded) {
        status = U_FILE_ACCESS_ERROR;
        return {};
    }
    const std::string tag = skString(pointer).c_str();
    auto locale = icu::Locale::forLanguageTag(icu::StringPiece(tag), status);
    if (locale.isBogus()) status = U_ILLEGAL_ARGUMENT_ERROR;
    // Material3's calendar arithmetic is Gregorian regardless of the display language.
    if (U_SUCCESS(status)) locale.setKeywordValue("calendar", "gregorian", status);
    return locale.getName();
}

icu::UnicodeString inputString(KInteropPointer pointer) {
    const auto& input = skString(pointer);
    return icu::UnicodeString::fromUTF8(icu::StringPiece(input.c_str(), input.size()));
}

template <typename Operation>
icu::UnicodeString stringResult(UErrorCode& status, Operation operation) {
    if (U_FAILURE(status)) return {};
    int32_t size = operation(nullptr, 0, &status);
    if (status == U_BUFFER_OVERFLOW_ERROR) status = U_ZERO_ERROR;
    if (U_FAILURE(status)) return {};
    if (size < 0 || size > kMaximumResult) {
        status = U_INDEX_OUTOFBOUNDS_ERROR;
        return {};
    }
    std::vector<UChar> result(size + 1);
    int32_t written = operation(result.data(), static_cast<int32_t>(result.size()), &status);
    if (U_FAILURE(status) || written < 0 || written > size) {
        if (U_SUCCESS(status)) status = U_INTERNAL_PROGRAM_ERROR;
        return {};
    }
    return icu::UnicodeString(result.data(), written);
}

KNativePointer ownedString(const icu::UnicodeString& text, UErrorCode status) {
    if (U_FAILURE(status)) return 0;
    std::string utf8;
    text.toUTF8String(utf8);
    return reinterpret_cast<KNativePointer>(new SkString(utf8));
}

DatePointer openDate(const std::string& locale, const icu::UnicodeString* pattern, UErrorCode& status) {
    static const UChar utc[] = {0x55, 0x54, 0x43};
    if (U_FAILURE(status)) return DatePointer(nullptr, udat_close);
    void* value = udat_open(pattern ? kPattern : kNoStyle, pattern ? kPattern : kShortStyle, locale.c_str(), utc, 3,
        pattern ? pattern->getBuffer() : nullptr, pattern ? pattern->length() : 0, &status);
    if (U_SUCCESS(status) && !value) status = U_INTERNAL_PROGRAM_ERROR;
    if (value && U_SUCCESS(status)) {
        CalendarPointer calendar(ucal_open(utc, 3, locale.c_str(), 1, &status), ucal_close);
        if (U_SUCCESS(status) && !calendar) status = U_INTERNAL_PROGRAM_ERROR;
        if (U_SUCCESS(status)) {
            ucal_setGregorianChange(calendar.get(), -8640000000000000.0, &status);
            if (U_SUCCESS(status)) udat_setCalendar(value, calendar.get());
        }
        udat_setLenient(value, false);
    }
    return DatePointer(value, udat_close);
}

icu::UnicodeString bestPattern(const std::string& locale, const icu::UnicodeString& skeleton, UErrorCode& status) {
    if (U_FAILURE(status)) return {};
    PatternPointer generator(udatpg_open(locale.c_str(), &status), udatpg_close);
    if (U_SUCCESS(status) && !generator) status = U_INTERNAL_PROGRAM_ERROR;
    return stringResult(status, [&](UChar* output, int32_t capacity, UErrorCode* error) {
        return udatpg_getBestPattern(generator.get(), skeleton.getBuffer(), skeleton.length(), output, capacity, error);
    });
}

struct Formatter {
    DatePointer date;
    std::mutex mutex;
    explicit Formatter(DatePointer value) : date(std::move(value)) {}
};

void finalizeFormatter(Formatter* formatter) { delete formatter; }

} // namespace

SKIKO_EXPORT KNativePointer org_jetbrains_skia_icu_DateTimeFormat__1nMake(KInteropPointer pattern, KInteropPointer locale, KInt* error) {
    UErrorCode status = U_ZERO_ERROR;
    const auto identifier = localeName(locale, status);
    const auto source = inputString(pattern);
    auto format = openDate(identifier, &source, status);
    *error = status;
    return U_FAILURE(status) ? 0 : reinterpret_cast<KNativePointer>(new Formatter(std::move(format)));
}

SKIKO_EXPORT KNativePointer org_jetbrains_skia_icu_DateTimeFormat__1nGetFinalizer() {
    return reinterpret_cast<KNativePointer>(&finalizeFormatter);
}

SKIKO_EXPORT KNativePointer org_jetbrains_skia_icu_DateTimeFormat__1nFormat(KNativePointer pointer, KLong timestamp, KInt* error) {
    auto* formatter = reinterpret_cast<Formatter*>(pointer);
    if (!formatter || timestamp < -8640000000000000LL || timestamp > 8640000000000000LL) {
        *error = U_ILLEGAL_ARGUMENT_ERROR;
        return 0;
    }
    std::lock_guard<std::mutex> lock(formatter->mutex);
    UErrorCode status = U_ZERO_ERROR;
    const auto output = stringResult(status, [&](UChar* target, int32_t capacity, UErrorCode* code) {
        return udat_format(formatter->date.get(), static_cast<double>(timestamp), target, capacity, nullptr, code);
    });
    *error = status;
    return ownedString(output, status);
}

SKIKO_EXPORT KDouble org_jetbrains_skia_icu_DateTimeFormat__1nParse(KNativePointer pointer, KInteropPointer text, KInt* error) {
    auto* formatter = reinterpret_cast<Formatter*>(pointer);
    if (!formatter) {
        *error = U_ILLEGAL_ARGUMENT_ERROR;
        return 0;
    }
    const auto source = inputString(text);
    if (source.length() > 1024) {
        *error = U_PARSE_ERROR;
        return 0;
    }
    std::lock_guard<std::mutex> lock(formatter->mutex);
    UErrorCode status = U_ZERO_ERROR;
    int32_t position = 0;
    const double value = udat_parse(formatter->date.get(), source.getBuffer(), source.length(), &position, &status);
    if (U_SUCCESS(status) && (position != source.length() || !std::isfinite(value))) status = U_PARSE_ERROR;
    *error = status;
    return value;
}

SKIKO_EXPORT KNativePointer org_jetbrains_skia_icu_DateTimeUtils__1nBestPattern(KInteropPointer skeleton, KInteropPointer locale, KInt* error) {
    UErrorCode status = U_ZERO_ERROR;
    const auto identifier = localeName(locale, status);
    const auto output = bestPattern(identifier, inputString(skeleton), status);
    *error = status;
    return ownedString(output, status);
}

SKIKO_EXPORT KNativePointer org_jetbrains_skia_icu_DateTimeUtils__1nInfo(KInteropPointer locale, KInt* error) {
    UErrorCode status = U_ZERO_ERROR;
    const auto identifier = localeName(locale, status);
    thread_local std::vector<std::pair<std::string, icu::UnicodeString>> cache;
    if (U_SUCCESS(status)) {
        for (const auto& entry : cache) {
            if (entry.first == identifier) {
                *error = status;
                return ownedString(entry.second, status);
            }
        }
    }
    auto date = openDate(identifier, nullptr, status);
    if (U_FAILURE(status)) {
        *error = status;
        return 0;
    }
    const auto pattern = stringResult(status, [&](UChar* target, int32_t capacity, UErrorCode* code) {
        return udat_toPattern(date.get(), false, target, capacity, code);
    });
    const auto hourPattern = bestPattern(identifier, icu::UnicodeString(u"j"), status);
    bool hour24 = false;
    bool quoted = false;
    for (int32_t index = 0; index < hourPattern.length(); ++index) {
        const UChar ch = hourPattern.charAt(index);
        if (ch == 0x27) quoted = !quoted;
        else if (!quoted && (ch == 0x48 || ch == 0x6b)) hour24 = true;
    }
    const int32_t first = ucal_getAttribute(udat_getCalendar(date.get()), kFirstDayOfWeek);
    icu::UnicodeString result = icu::UnicodeString::fromUTF8(std::to_string((first + 5) % 7 + 1));
    result.append(u'\n').append(pattern).append(u'\n').append(hour24 ? u'1' : u'0');
    for (int day = 0; day < 7; ++day) {
        const int32_t symbol = (day + 1) % 7 + 1;
        for (int32_t kind : {kStandaloneWeekdays, kStandaloneNarrowWeekdays}) {
            const auto name = stringResult(status, [&](UChar* target, int32_t capacity, UErrorCode* code) {
                return udat_getSymbols(date.get(), kind, symbol, target, capacity, code);
            });
            result.append(u'\n').append(name);
        }
    }
    if (U_SUCCESS(status)) {
        if (cache.size() >= 16) cache.erase(cache.begin());
        cache.emplace_back(identifier, result);
    }
    *error = status;
    return ownedString(result, status);
}

SKIKO_EXPORT KNativePointer org_jetbrains_skia_icu_DateTimeUtils__1nFormatInteger(KLong value, KInt minimum, KInt maximum, KBoolean grouping, KInteropPointer locale, KInt* error) {
    UErrorCode status = U_ZERO_ERROR;
    if (minimum < 0 || maximum < minimum || maximum > 100) {
        *error = U_ILLEGAL_ARGUMENT_ERROR;
        return 0;
    }
    const auto identifier = localeName(locale, status);
    if (U_FAILURE(status)) {
        *error = status;
        return 0;
    }
    // DatePicker formats many day labels per frame; retain a small per-thread cache.
    thread_local std::vector<std::pair<std::string, NumberPointer>> cache;
    const std::string key = identifier + ":" + std::to_string(minimum) + ":" + std::to_string(maximum) + (grouping ? ":g" : ":n");
    void* number = nullptr;
    for (const auto& entry : cache) {
        if (entry.first == key) {
            number = entry.second.get();
            break;
        }
    }
    if (!number) {
        NumberPointer created(unum_open(kDecimal, nullptr, 0, identifier.c_str(), nullptr, &status), unum_close);
        if (U_SUCCESS(status) && !created) status = U_INTERNAL_PROGRAM_ERROR;
        if (U_FAILURE(status)) {
            *error = status;
            return 0;
        }
        unum_setAttribute(created.get(), kMinIntegerDigits, minimum);
        unum_setAttribute(created.get(), kMaxIntegerDigits, maximum);
        unum_setAttribute(created.get(), kGroupingUsed, grouping != 0);
        number = created.get();
        if (cache.size() >= 16) cache.erase(cache.begin());
        cache.emplace_back(key, std::move(created));
    }
    const auto result = stringResult(status, [&](UChar* target, int32_t capacity, UErrorCode* code) {
        return unum_formatInt64(number, value, target, capacity, nullptr, code);
    });
    *error = status;
    return ownedString(result, status);
}
