cmake_minimum_required(VERSION 3.20)
project(rachel_ios_audio LANGUAGES C CXX OBJC)

find_path(AUDIO_SHARED_CPP_DIR rachel_audio.h PATHS ${RACHEL_CPP_SOURCE_DIRS} NO_DEFAULT_PATH REQUIRED)
add_subdirectory("${AUDIO_SHARED_CPP_DIR}" "${CMAKE_CURRENT_BINARY_DIR}/shared-audio")
