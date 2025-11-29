cmake_minimum_required(VERSION 3.17)

project(webp_jni)

set(CMAKE_BUILD_TYPE Release)
set(CMAKE_CXX_STANDARD 20)
set(CMAKE_CXX_FLAGS_RELEASE "${CMAKE_CXX_FLAGS_RELEASE} -O2 -DNDEBUG")

add_library(webp STATIC IMPORTED)
set_target_properties(webp PROPERTIES
    IMPORTED_LOCATION ${CMAKE_CURRENT_SOURCE_DIR}/libs/libwebp.a
)

add_library(webpdemux STATIC IMPORTED)
set_target_properties(webpdemux PROPERTIES
    IMPORTED_LOCATION ${CMAKE_CURRENT_SOURCE_DIR}/libs/libwebpdemux.a
)

add_library(webp_jni SHARED libwebp_jni.cpp)
target_include_directories(webp_jni PRIVATE include)
target_link_libraries(webp_jni webp webpdemux)