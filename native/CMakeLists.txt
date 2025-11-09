cmake_minimum_required(VERSION 3.10.0)

# Disable in-source builds to prevent source tree corruption.
if("${CMAKE_SOURCE_DIR}" STREQUAL "${CMAKE_BINARY_DIR}")
    message(FATAL_ERROR "不允许直接在源码树中构建, 请在build目录中构建, 或使用build.bat/build.sh脚本")
endif()

project(ylcs-native)

if(MSVC)
    set(LINK_STATIC_MSVC_LIBS ON CACHE BOOL "")
    if(LINK_STATIC_MSVC_LIBS)
        set(CMAKE_MSVC_RUNTIME_LIBRARY "MultiThreaded$<$<CONFIG:Debug>:Debug>")
    endif()
    add_compile_options("/utf-8")
endif()

set(CMAKE_CXX_STANDARD 20)
set(CMAKE_CXX_FLAGS_RELEASE "${CMAKE_CXX_FLAGS_RELEASE} -O2 -DNDEBUG")
set(CMAKE_RUNTIME_OUTPUT_DIRECTORY "${CMAKE_CURRENT_SOURCE_DIR}/libs") # .exe and .dll
set(CMAKE_LIBRARY_OUTPUT_DIRECTORY "${CMAKE_CURRENT_SOURCE_DIR}/libs") # .so and .dylib
# set(CMAKE_ARCHIVE_OUTPUT_DIRECTORY "${CMAKE_CURRENT_SOURCE_DIR}/libs") # .lib and .a

find_package(Java COMPONENTS Runtime Development)
if(NOT Java_FOUND)
    message(FATAL_ERROR "ylcs native lib requires Java to build!")
    return()
endif()
find_package(JNI REQUIRED)

set(BUILD_SHARED_LIBS OFF)
add_subdirectory(mmkv)
add_subdirectory(nativefiledialog)

set(PLATFORM_DIR "")
if(WIN32 OR CYGWIN)
    set(PLATFORM_DIR "windows")
elseif(APPLE)
    set(PLATFORM_DIR "macos")
elseif(UNIX AND NOT APPLE)
    set(PLATFORM_DIR "linux")
else()
    message(FATAL_ERROR "Unsupported platform: ${CMAKE_SYSTEM_NAME}")
endif()

file(GLOB_RECURSE PLATFORM_SRC
    "${CMAKE_CURRENT_SOURCE_DIR}/platform/${PLATFORM_DIR}/*"
)

add_library(ylcs_native SHARED
    ylcs_jni.h
    ylcs_jni.cpp
    ylcs_mmkv.cpp
    ylcs_nativefiledialog.cpp
    ${PLATFORM_SRC}
)

target_include_directories(ylcs_native PRIVATE
    ${CMAKE_CURRENT_SOURCE_DIR}
    ${JNI_INCLUDE_DIRS}
)

target_link_libraries(ylcs_native
    ${JNI_LIBRARIES}
    mmkv
    nfd
)

if(WIN32)
    target_compile_definitions(ylcs_native PRIVATE UNICODE _UNICODE)
endif()

add_dependencies(ylcs_native
    mmkv
    nfd
)

set_target_properties(ylcs_native PROPERTIES
    CXX_STANDARD 20
    CXX_EXTENSIONS OFF
    POSITION_INDEPENDENT_CODE ON
)
