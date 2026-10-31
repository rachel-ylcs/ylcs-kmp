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

find_path(AUDIO_SHARED_CPP_DIR rachel_audio.h PATHS ${RACHEL_CPP_SOURCE_DIRS} NO_DEFAULT_PATH REQUIRED)
add_subdirectory("${AUDIO_SHARED_CPP_DIR}" "${CMAKE_CURRENT_BINARY_DIR}/shared-audio")

set(PLAYER_SOURCES audio_jni.cpp video_jni.cpp sound_jni.cpp)

add_library(player SHARED
    ${PLAYER_SOURCES}
    $<TARGET_OBJECTS:rachel_audio_objects>
)

target_include_directories(player PRIVATE
    ${CMAKE_CURRENT_SOURCE_DIR}
    ${JNI_INCLUDE_DIRS}
    ${NATIVE_JNI_DIR}
)

target_link_libraries(player PRIVATE
    ${JNI_LIBRARIES}
    rachel_audio
)

if(UNIX)
    find_package(Threads REQUIRED)
    target_link_libraries(player PRIVATE Threads::Threads)
    if(CMAKE_DL_LIBS)
        target_link_libraries(player PRIVATE ${CMAKE_DL_LIBS})
    endif()
    if(NOT APPLE)
        target_link_libraries(player PRIVATE m)
    endif()
endif()

if(WIN32)
    target_compile_definitions(player PRIVATE UNICODE _UNICODE)
endif()

set_target_properties(player PROPERTIES
    CXX_STANDARD 20
    CXX_EXTENSIONS OFF
    POSITION_INDEPENDENT_CODE ON
    OUTPUT_NAME ${NATIVE_OUTPUT_NAME}
)
