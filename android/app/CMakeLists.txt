cmake_minimum_required(VERSION 3.4.1)
# android studio has its own fork of cmake and newest version is only 3.6.2

# no idea what this does and i don't care, should work
set(CMAKE_SHARED_LINKER_FLAGS "${CMAKE_SHARED_LINKER_FLAGS} -Wl,--exclude-libs,libippicv.a -Wl,--exclude-libs,libippiw.a")

#  fuck of android studio i should not have to do this
set(CORELIB_HOME ../../core/corelib)
set(OPENCV_HOME ../../deps/opencv)
set(DJINNI_HOME ../../deps/djinni)

# Android
find_library(log-lib log)

# OpenCV
set(OpenCV_STATIC ON)
set(OpenCV_DIR ${OPENCV_HOME}/sdk/native/jni)
find_package (OpenCV REQUIRED)

# Custom CoreLib
include_directories(${CORELIB_HOME}/include)
include_directories(../../../deps/OpenCV-android-sdk/sdk/native/jni/include)

# Djinni
file(GLOB_RECURSE DJINNI_SRC_FILES FOLLOW_SYMLINKS
     ${CMAKE_CURRENT_SOURCE_DIR}/src/main/cpp/jni/*.cpp
     ../../../deps/djinni/support-lib/*.cpp)

include_directories(${DJINNI_HOME}/support-lib/jni)
include_directories(${CMAKE_CURRENT_SOURCE_DIR}/src/main/cpp/interfaces)
include_directories(${CMAKE_CURRENT_SOURCE_DIR}/src/main/cpp/jni)

add_library(software_scanner SHARED
  # Core library (custom lib)
  ${CORELIB_HOME}/src/img_utils.cpp
  ${CORELIB_HOME}/src/detect.cpp

  # Sources (hand written)
  src/main/cpp/ImgTransformImpl.cpp
  src/main/cpp/CoreLibImpl.cpp

  # Generated JNI
  ${DJINNI_SRC_FILES}
  # Djinni
  ${DJINNI_HOME}/support-lib/jni/djinni_main.cpp
  ${DJINNI_HOME}/support-lib/jni/djinni_support.cpp
)

set_target_properties(software_scanner PROPERTIES
            CXX_STANDARD_REQUIRED 17
            CXX_EXTENSIONS OFF
            )
  #target_compile_features(imgtransoform_android PRIVATE cxx_auto_type)


target_link_libraries(software_scanner
  ${log-lib}
  ${OpenCV_LIBS}
)
