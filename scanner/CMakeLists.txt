cmake_minimum_required(VERSION 3.7)

list(APPEND CMAKE_MODULE_PATH "${CMAKE_CURRENT_SOURCE_DIR}/cmake")

option(ANDROID "whether to build for android (arm64-v8a) platform" OFF)

set(CMAKE_EXPORT_COMPILE_COMMANDS ON)
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_EXTENSIONS OFF)

if (ANDROID)
    # Use OpenCV build specifically for Android
    set(OpenCV_DIR "${CMAKE_SOURCE_DIR}/deps/opencv/sdk/native/jni")

    if (NOT DEFINED ENV{ANDROID_NDK})
        message("ANDROID_NDK is not set!")
        return()
    endif()

    set(CMAKE_TOOLCHAIN_FILE "$ENV{ANDROID_NDK}/build/cmake/android.toolchain.cmake")
    set(ANDROID_NDK_ABI_NAME "arm64-v8a")
    set(ANDROID_ABI "arm64-v8a")
    set(ANDROID_PLATFORM 30)

    # Fixes FindJNI errors, for details see the following:
    # https://stackoverflow.com/questions/51047978/cmake-could-not-find-jni/56132225
    set(JAVA_AWT_LIBRARY NotNeeded)
    set(JAVA_JVM_LIBRARY NotNeeded)
    set(JAVA_INCLUDE_PATH2 NotNeeded)
    set(JAVA_AWT_INCLUDE_PATH NotNeeded)
endif ()

project(scanner)

# Force default build type to Release
if(NOT CMAKE_BUILD_TYPE)
    set(CMAKE_BUILD_TYPE "Release" CACHE STRING
            "Choose the type of build, options are: Debug, Release (default), RelWithDebInfo and MinSizeRel."
            FORCE)
endif(NOT CMAKE_BUILD_TYPE)

# Layout build dir like install dir
include(GNUInstallDirs)
option(BUILD_SHARED_LIBS "Build shared libraries(.so or .dyld)." ON)
set(CMAKE_BUILD_WITH_INSTALL_RPATH TRUE)
set(CMAKE_LIBRARY_OUTPUT_DIRECTORY ${CMAKE_BINARY_DIR}/${CMAKE_INSTALL_LIBDIR})
set(CMAKE_ARCHIVE_OUTPUT_DIRECTORY ${CMAKE_BINARY_DIR}/${CMAKE_INSTALL_LIBDIR})
set(CMAKE_RUNTIME_OUTPUT_DIRECTORY ${CMAKE_BINARY_DIR}/${CMAKE_INSTALL_BINDIR})

## for multi-config build system (e.g. Xcode, ninja Multi-Config)
foreach(OUTPUTCONFIG IN LISTS CMAKE_CONFIGURATION_TYPES)
    string(TOUPPER ${OUTPUTCONFIG} OUTPUTCONFIG)
    set(CMAKE_LIBRARY_OUTPUT_DIRECTORY_${OUTPUTCONFIG} ${CMAKE_BINARY_DIR}/${OUTPUTCONFIG}/${CMAKE_INSTALL_LIBDIR})
    set(CMAKE_ARCHIVE_OUTPUT_DIRECTORY_${OUTPUTCONFIG} ${CMAKE_BINARY_DIR}/${OUTPUTCONFIG}/${CMAKE_INSTALL_LIBDIR})
    set(CMAKE_RUNTIME_OUTPUT_DIRECTORY_${OUTPUTCONFIG} ${CMAKE_BINARY_DIR}/${OUTPUTCONFIG}/${CMAKE_INSTALL_BINDIR})
endforeach()

include(java)

# Build main executable
find_package(OpenCV REQUIRED)
add_subdirectory(scanner)
add_executable(out
        app/src/main.cpp
        )
target_link_libraries(out scanner ${OpenCV_LIBS})
