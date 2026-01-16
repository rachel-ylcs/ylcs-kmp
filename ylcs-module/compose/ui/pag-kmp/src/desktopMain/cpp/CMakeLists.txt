cmake_minimum_required(VERSION 3.10.0)

# Disable in-source builds to prevent source tree corruption.
if("${CMAKE_SOURCE_DIR}" STREQUAL "${CMAKE_BINARY_DIR}")
    message(FATAL_ERROR "不允许直接在源码树中构建, 请在build目录中构建, 或使用build.bat/build.sh脚本")
endif()

project(pag4j)

if(MSVC)
    set(LINK_STATIC_MSVC_LIBS ON CACHE BOOL "")
    if(LINK_STATIC_MSVC_LIBS)
        set(CMAKE_MSVC_RUNTIME_LIBRARY "MultiThreaded$<$<CONFIG:Debug>:Debug>")
    endif()
    add_compile_options("/utf-8")
endif()

set(CMAKE_CXX_STANDARD 20)
set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -D_DISABLE_CONSTEXPR_MUTEX_CONSTRUCTOR")
set(CMAKE_CXX_FLAGS_RELEASE "${CMAKE_CXX_FLAGS_RELEASE} -O2 -DNDEBUG -D_DISABLE_CONSTEXPR_MUTEX_CONSTRUCTOR")
set(CMAKE_RUNTIME_OUTPUT_DIRECTORY "${NATIVE_OUTPUT_DIR}") # .exe and .dll
set(CMAKE_LIBRARY_OUTPUT_DIRECTORY "${NATIVE_OUTPUT_DIR}") # .so and .dylib
# set(CMAKE_ARCHIVE_OUTPUT_DIRECTORY "${NATIVE_OUTPUT_DIR}") # .lib and .a

find_package(Java COMPONENTS Runtime Development)
if(NOT Java_FOUND)
    message(FATAL_ERROR "ylcs native lib requires Java to build!")
    return()
endif()
find_package(JNI REQUIRED)

set(PAG_BUILD_SHARED OFF)
set(PAG_BUILD_FRAMEWORK OFF)

add_subdirectory(libpag)

add_library(pag4j SHARED
    main.cpp
)

target_include_directories(pag4j PRIVATE
    ${CMAKE_CURRENT_SOURCE_DIR}
    ${JNI_INCLUDE_DIRS}
    ${NATIVE_JNI_DIR}
)

target_link_libraries(pag4j
    ${JNI_LIBRARIES}
    pag
)

if(WIN32)
    target_compile_definitions(pag4j PRIVATE UNICODE _UNICODE)
endif()

set_target_properties(pag4j PROPERTIES
    CXX_STANDARD 20
    CXX_EXTENSIONS OFF
    POSITION_INDEPENDENT_CODE ON
    OUTPUT_NAME ${NATIVE_OUTPUT_NAME}
)