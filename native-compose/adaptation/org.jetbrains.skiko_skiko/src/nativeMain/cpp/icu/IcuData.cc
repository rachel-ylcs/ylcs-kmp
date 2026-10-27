#ifndef U_STATIC_IMPLEMENTATION
#define U_STATIC_IMPLEMENTATION
#endif

#include "common.h"
#include "unicode/udata.h"

#if !defined(SK_BUILD_FOR_WIN)
#include <fcntl.h>
#include <mutex>
#include <sys/mman.h>
#include <sys/stat.h>
#include <unistd.h>
#endif

SKIKO_EXPORT KInt org_jetbrains_skia_icu_IcuData__1nLoad(KInteropPointer path) {
#if defined(SK_BUILD_FOR_WIN)
    // Skia's Windows loader already maps icudtl.dat beside the executable.
    return SkLoadICU() ? U_ZERO_ERROR : U_FILE_ACCESS_ERROR;
#else
    static std::mutex mutex;
    static const void* registered = nullptr;
    std::lock_guard<std::mutex> lock(mutex);
    if (registered) return U_ZERO_ERROR;
    const auto filename = skString(path);
    const int descriptor = open(filename.c_str(), O_RDONLY | O_CLOEXEC);
    if (descriptor < 0) return U_FILE_ACCESS_ERROR;
    struct stat information;
    if (fstat(descriptor, &information) != 0 || information.st_size < 144 || information.st_size > 128LL * 1024 * 1024) {
        close(descriptor);
        return U_INVALID_FORMAT_ERROR;
    }
    const size_t size = static_cast<size_t>(information.st_size);
    void* mapping = mmap(nullptr, size, PROT_READ, MAP_PRIVATE, descriptor, 0);
    close(descriptor);
    if (mapping == MAP_FAILED) return U_FILE_ACCESS_ERROR;
    UErrorCode status = U_ZERO_ERROR;
    udata_setCommonData(mapping, &status);
    if (U_FAILURE(status)) munmap(mapping, size);
    else registered = mapping; // ICU can retain pointers into this data for the process lifetime.
    return status;
#endif
}
