cmake_minimum_required(VERSION 3.10.0)

project(ylcs)

find_package(Java COMPONENTS Runtime Development)
if(NOT Java_FOUND)
    message(FATAL_ERROR "ylcs requires Java to build!")
    return()
endif()
find_package(JNI REQUIRED)

add_library(ylcs SHARED
    main.cpp
    picker.cpp
)

target_include_directories(ylcs PRIVATE
    ${CMAKE_CURRENT_SOURCE_DIR}
    ${JNI_INCLUDE_DIRS}
)

IF (WIN32)
    target_compile_definitions(ylcs PUBLIC UNICODE)
    target_compile_definitions(ylcs PUBLIC _UNICODE)
ENDIF()

set_target_properties(ylcs PROPERTIES
    CXX_STANDARD 20
    CXX_EXTENSIONS OFF
    POSITION_INDEPENDENT_CODE ON
)

target_link_libraries(ylcs ${JNI_LIBRARIES})