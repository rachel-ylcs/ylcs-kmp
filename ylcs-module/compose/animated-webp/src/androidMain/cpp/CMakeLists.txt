cmake_minimum_required(VERSION 3.17)

project(webp_jni)

add_subdirectory(libwebp)

set(CMAKE_CXX_STANDARD 20)
set(CMAKE_CXX_FLAGS_RELEASE "${CMAKE_CXX_FLAGS_RELEASE} -O2 -DNDEBUG")

add_library(webp_jni SHARED libwebp_jni.cpp)

target_include_directories(webp_jni PRIVATE libwebp/src)

target_link_libraries(webp_jni webp)