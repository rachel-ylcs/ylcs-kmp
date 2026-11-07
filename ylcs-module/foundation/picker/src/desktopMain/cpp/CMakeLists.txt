cmake_minimum_required(VERSION 3.20)
project(picker LANGUAGES C CXX)

if(APPLE)
    enable_language(OBJC OBJCXX)
endif()

set(CMAKE_CXX_STANDARD 20)
set(CMAKE_POSITION_INDEPENDENT_CODE ON)
find_path(SDL_SOURCE_ROOT include/SDL3/SDL_dialog.h PATHS ${RACHEL_CPP_SOURCE_DIRS} NO_DEFAULT_PATH REQUIRED)
set(SDL_SHARED OFF CACHE BOOL "" FORCE)
set(SDL_STATIC ON CACHE BOOL "" FORCE)
set(SDL_TEST_LIBRARY OFF CACHE BOOL "" FORCE)
set(SDL_TESTS OFF CACHE BOOL "" FORCE)
set(SDL_EXAMPLES OFF CACHE BOOL "" FORCE)
set(SDL_UNIX_CONSOLE_BUILD ON CACHE BOOL "" FORCE)
foreach(feature VIDEO RENDER GPU AUDIO CAMERA JOYSTICK HAPTIC HIDAPI POWER SENSOR TRAY)
    set(SDL_${feature} OFF CACHE BOOL "" FORCE)
endforeach()
set(SDL_DIALOG ON CACHE BOOL "" FORCE)
add_subdirectory("${SDL_SOURCE_ROOT}" "${CMAKE_CURRENT_BINARY_DIR}/sdl")

add_library(picker SHARED main.cpp)
target_include_directories(picker PRIVATE ${RACHEL_JNI_INCLUDE_DIRS} ${RACHEL_CPP_INCLUDE_DIR})
target_link_libraries(picker PRIVATE SDL3::SDL3-static)
if(MINGW AND CMAKE_CXX_COMPILER_ID STREQUAL "Clang")
    # Neither target uses C++ modules, and Kotlin/Native's LLVM bundle lacks clang-scan-deps.
    set_target_properties(picker SDL3-static PROPERTIES CXX_SCAN_FOR_MODULES OFF)
    # Let the older MinGW intrin.h use Clang's SIMD definitions for C++ and its PCH.
    foreach(target picker SDL3-static)
        target_compile_definitions(${target} PRIVATE "$<$<COMPILE_LANGUAGE:CXX>:__MINGW_FORCE_SYS_INTRINS>")
    endforeach()
endif()
if(APPLE)
    target_sources(picker PRIVATE DialogMainThread.mm)
    target_link_libraries(picker PRIVATE "-framework Cocoa")
endif()
if(WIN32)
    set_target_properties(picker PROPERTIES PREFIX "")
endif()
set_target_properties(picker PROPERTIES
    OUTPUT_NAME ${NATIVE_OUTPUT_NAME}
    RUNTIME_OUTPUT_DIRECTORY "${NATIVE_OUTPUT_DIR}"
    LIBRARY_OUTPUT_DIRECTORY "${NATIVE_OUTPUT_DIR}"
)
