cmake_minimum_required(VERSION 3.17)

project(webp_jni)

add_library(webp STATIC IMPORTED)
set_target_properties(webp PROPERTIES
    IMPORTED_LOCATION ${CMAKE_CURRENT_SOURCE_DIR}/libs/libwebp.a
)

add_library(webpdemux STATIC IMPORTED)
set_target_properties(webpdemux PROPERTIES
    IMPORTED_LOCATION ${CMAKE_CURRENT_SOURCE_DIR}/libs/libwebpdemux.a
)

add_library(webp_jni SHARED libwebp_jni.c)
set_target_properties(webp_jni PROPERTIES
    OUTPUT_NAME webp
)

target_include_directories(webp_jni PRIVATE include)
target_compile_options(webp_jni PRIVATE -O2 -DNDEBUG)
target_link_options(webp_jni PRIVATE -Wl,--gc-sections -Wl,-s)
target_link_libraries(webp_jni webp webpdemux)

