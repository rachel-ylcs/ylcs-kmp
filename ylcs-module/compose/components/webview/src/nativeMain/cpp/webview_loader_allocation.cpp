#include <stdlib.h>
#include <vcruntime_new.h>

namespace std {
    const nothrow_t nothrow{};
}

void *operator new(size_t size, const std::nothrow_t &) noexcept {
    return malloc(size == 0 ? 1 : size);
}

void operator delete(void *value, size_t) noexcept {
    free(value);
}
