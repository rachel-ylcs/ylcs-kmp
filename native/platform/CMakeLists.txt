cmake_minimum_required(VERSION 3.10.0)

set(PLATFORM_DIR "")
if(WIN32 OR CYGWIN)
    set(PLATFORM_DIR "windows")
elseif(APPLE)
    set(PLATFORM_DIR "macos")
elseif(LINUX)
    set(PLATFORM_DIR "linux")
else()
    message(FATAL_ERROR "Unsupported platform: ${CMAKE_SYSTEM_NAME}")
endif()

file(GLOB_RECURSE PLATFORM_SRC
    "${CMAKE_CURRENT_SOURCE_DIR}/${PLATFORM_DIR}/*"
)

add_library(platform STATIC ${PLATFORM_SRC})

target_include_directories(platform
    PUBLIC
        "${CMAKE_CURRENT_SOURCE_DIR}/include"
)
