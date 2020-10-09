cmake_minimum_required(VERSION 3.7)
project(scanner)
list(APPEND CMAKE_MODULE_PATH "${CMAKE_CURRENT_SOURCE_DIR}/cmake")

set(CMAKE_EXPORT_COMPILE_COMMANDS ON)
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_EXTENSIONS OFF)

find_package(OpenCV REQUIRED)

add_subdirectory(scanner)

add_executable(out
        app/src/main.cpp
        )

target_link_libraries(out scanner jniScanner ${OpenCV_LIBS})


include(java)
