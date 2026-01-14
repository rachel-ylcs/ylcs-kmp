cmake_minimum_required(VERSION 3.10.0)

# Disable in-source builds to prevent source tree corruption.
if("${CMAKE_SOURCE_DIR}" STREQUAL "${CMAKE_BINARY_DIR}")
    message(FATAL_ERROR "不允许直接在源码树中构建, 请在build目录中构建, 或使用build.bat/build.sh脚本")
endif()

project(player)

if(MSVC)
    set(LINK_STATIC_MSVC_LIBS ON CACHE BOOL "")
    if(LINK_STATIC_MSVC_LIBS)
        set(CMAKE_MSVC_RUNTIME_LIBRARY "MultiThreaded$<$<CONFIG:Debug>:Debug>")
    endif()
    add_compile_options("/utf-8")
endif()

set(CMAKE_CXX_STANDARD 20)
set(CMAKE_CXX_FLAGS_RELEASE "${CMAKE_CXX_FLAGS_RELEASE} -O2 -DNDEBUG")
set(CMAKE_RUNTIME_OUTPUT_DIRECTORY "${NATIVE_OUTPUT_DIR}") # .exe and .dll
set(CMAKE_LIBRARY_OUTPUT_DIRECTORY "${NATIVE_OUTPUT_DIR}") # .so and .dylib
# set(CMAKE_ARCHIVE_OUTPUT_DIRECTORY "${NATIVE_OUTPUT_DIR}") # .lib and .a

find_package(Java COMPONENTS Runtime Development)
if(NOT Java_FOUND)
    message(FATAL_ERROR "ylcs native lib requires Java to build!")
    return()
endif()
find_package(JNI REQUIRED)

set(PLATFORM_NAME "")
if(WIN32 OR CYGWIN)
    set(PLATFORM_NAME "windows")
elseif(APPLE)
    set(PLATFORM_NAME "macos")
elseif(UNIX AND NOT APPLE)
    set(PLATFORM_NAME "linux")
else()
    message(FATAL_ERROR "Unsupported platform: ${CMAKE_SYSTEM_NAME}")
endif()

add_library(player SHARED
    ${PLATFORM_NAME}.cpp
)

target_include_directories(player PRIVATE
    ${CMAKE_CURRENT_SOURCE_DIR}
    ${JNI_INCLUDE_DIRS}
    ${NATIVE_JNI_DIR}
)

target_link_libraries(player
    ${JNI_LIBRARIES}
)

if(WIN32)
    target_compile_definitions(player PRIVATE UNICODE _UNICODE)
endif()

set_target_properties(player PROPERTIES
    CXX_STANDARD 20
    CXX_EXTENSIONS OFF
    POSITION_INDEPENDENT_CODE ON
    OUTPUT_NAME ${NATIVE_OUTPUT_NAME}
)