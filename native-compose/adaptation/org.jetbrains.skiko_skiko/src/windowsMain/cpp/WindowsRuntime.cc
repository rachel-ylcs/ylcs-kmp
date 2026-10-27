// Skia uses /MT while Kotlin/Native supplies MinGW's executable startup. Initialize
// the embedded MSVC runtime before C++ constructors (XCU), without starting a second
// C runtime or replacing MinGW's heap, thread-local storage and process entry point.
#include <process.h>

extern "C" bool __cdecl __vcrt_initialize();

static __declspec(safebuffers) int __cdecl initializeMsvcRuntime() {
    __security_init_cookie();
    return __vcrt_initialize() ? 0 : -1;
}

#pragma section(".CRT$XIB", read)
#pragma comment(linker, "/include:skikoInitializeMsvcRuntime")
extern "C" __declspec(allocate(".CRT$XIB")) int (__cdecl * const skikoInitializeMsvcRuntime)() = initializeMsvcRuntime;

// The MSVC on-exit table is separate from MinGW's. These Skia static caches live
// for the process; their destructors must not enter the incompatible on-exit table.
extern "C" int __wrap_atexit(void (*)(void)) { return 0; }
