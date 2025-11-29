cmake_minimum_required(VERSION 3.17)

project(webp_jni)

set(CMAKE_CXX_STANDARD 20)
set(CMAKE_BUILD_TYPE, Release)
set(CMAKE_C_FLAGS_RELEASE "${CMAKE_C_FLAGS_RELEASE} -O3 -DNDEBUG")
set(CMAKE_CXX_FLAGS_RELEASE "${CMAKE_CXX_FLAGS_RELEASE} -O3 -DNDEBUG")

add_subdirectory(libwebp)
add_library(webp_jni SHARED libwebp_jni.cpp)
target_include_directories(webp_jni PRIVATE libwebp/src)
target_link_libraries(webp_jni webpdemux)