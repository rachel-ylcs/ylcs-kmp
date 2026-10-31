file(GLOB WINDOWS_SDK_CPPWINRT
    "$ENV{WindowsSdkDir}/Include/*/cppwinrt"
    "C:/Program Files (x86)/Windows Kits/10/Include/*/cppwinrt"
)
list(SORT WINDOWS_SDK_CPPWINRT COMPARE NATURAL ORDER DESCENDING)
find_path(WINDOWS_CPPWINRT_INCLUDE winrt/base.h PATHS ${WINDOWS_SDK_CPPWINRT} NO_CMAKE_FIND_ROOT_PATH REQUIRED)
target_include_directories(rachel_audio_objects SYSTEM PRIVATE "${WINDOWS_CPPWINRT_INCLUDE}")

if(MINGW)
    # Kotlin/Native's GCC 9 headers need a coroutine header and a C++17 view of the SDK.
    set(WINRT_COMPAT "${CMAKE_CURRENT_BINARY_DIR}/winrt-compat")
    file(MAKE_DIRECTORY "${WINRT_COMPAT}/winrt")
    set(COROUTINE_HEADER "${WINRT_COMPAT}/coroutine")
    if(NOT EXISTS "${COROUTINE_HEADER}")
        file(DOWNLOAD
            "https://raw.githubusercontent.com/gcc-mirror/gcc/releases/gcc-10.5.0/libstdc++-v3/include/std/coroutine"
            "${COROUTINE_HEADER}"
            EXPECTED_HASH SHA256=39a804b2bf474b8e14e66c97d5e0a7892cabb357d3313c97c782b4a7033ce4a8
            TLS_VERIFY ON
        )
        file(DOWNLOAD "https://raw.githubusercontent.com/gcc-mirror/gcc/releases/gcc-10.5.0/COPYING3" "${WINRT_COMPAT}/COPYING3" TLS_VERIFY ON)
        file(DOWNLOAD "https://raw.githubusercontent.com/gcc-mirror/gcc/releases/gcc-10.5.0/COPYING.RUNTIME" "${WINRT_COMPAT}/COPYING.RUNTIME" TLS_VERIFY ON)
    endif()
    file(READ "${WINDOWS_CPPWINRT_INCLUDE}/winrt/base.h" WINRT_BASE)
    string(REPLACE "#include <intrin.h>" "#include <sstream>\n#include <locale>\n#include <limits>\n#include <intrin.h>" WINRT_BASE "${WINRT_BASE}")
    set(WINRT_FLOAT_FORMAT "result = std::to_chars(std::begin(temp), std::end(temp), value, std::chars_format::general);")
    string(FIND "${WINRT_BASE}" "${WINRT_FLOAT_FORMAT}" WINRT_FLOAT_POSITION)
    if(WINRT_FLOAT_POSITION EQUAL -1)
        message(FATAL_ERROR "The installed C++/WinRT SDK needs its MinGW formatting compatibility checked")
    endif()
    string(REPLACE "${WINRT_FLOAT_FORMAT}" "std::wostringstream stream; stream.imbue(std::locale::classic()); stream.precision(std::numeric_limits<T>::max_digits10); stream << value; return hstring{stream.str()};" WINRT_BASE "${WINRT_BASE}")
    file(CONFIGURE OUTPUT "${WINRT_COMPAT}/winrt/base.h" CONTENT "@WINRT_BASE@" @ONLY)
    get_filename_component(WINDOWS_SDK_INCLUDE "${WINDOWS_CPPWINRT_INCLUDE}" DIRECTORY)
    target_include_directories(rachel_audio_objects SYSTEM BEFORE PRIVATE "${WINRT_COMPAT}")
    target_compile_definitions(rachel_audio_objects PRIVATE "$<$<COMPILE_LANGUAGE:CXX>:__cplusplus=201703L;__INTRIN_H_>")
    target_compile_options(rachel_audio_objects PRIVATE -fms-extensions -Wno-builtin-macro-redefined
        "SHELL:-idirafter \"${WINDOWS_SDK_INCLUDE}/um\""
        "SHELL:-idirafter \"${WINDOWS_SDK_INCLUDE}/shared\""
    )
endif()
