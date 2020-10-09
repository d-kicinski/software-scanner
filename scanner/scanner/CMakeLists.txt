cmake_minimum_required(VERSION 3.10)
project(libscanner VERSION 1.0.0 LANGUAGES CXX)

#list(INSERT CMAKE_MODULE_PATH 0 ${CMAKE_SOURCE_DIR}/cmake)


## Creating library

# Create target and set properties
add_library(scanner SHARED
        src/img_utils
        src/scanner
        )

#add_library(jnijavanative SHARED "")
# eh?
set_target_properties(scanner PROPERTIES
        POSITION_INDEPENDENT_CODE ON)

#Add an alias so that library can be used inside the build tree, e.g. when testing
add_library(Scanner::scanner ALIAS scanner)

#Set target properties
target_include_directories(scanner
        PUBLIC
        $<INSTALL_INTERFACE:include>
        $<BUILD_INTERFACE:${CMAKE_CURRENT_SOURCE_DIR}/include>
        PRIVATE
        ${CMAKE_CURRENT_SOURCE_DIR}/src
        )

target_compile_features(scanner PRIVATE cxx_auto_type)
target_compile_options(scanner PRIVATE -Werror)

# import external targets
find_package(OpenCV)

# link dependencies
target_link_libraries(scanner
        PRIVATE
        ${OpenCV_LIBS}
        )

##############################################
# Installation instructions
include(GNUInstallDirs)
set(INSTALL_CONFIGDIR ${CMAKE_INSTALL_LIBDIR}/cmake/Scanner)

install(TARGETS scanner
        EXPORT scanner-targets
        LIBRARY DESTINATION ${CMAKE_INSTALL_LIBDIR}
        ARCHIVE DESTINATION ${CMAKE_INSTALL_LIBDIR}
        )

#This is required so that the exported target has the name Scanner and not scanner
set_target_properties(scanner PROPERTIES EXPORT_NAME Scanner)

install(DIRECTORY include/ DESTINATION ${CMAKE_INSTALL_INCLUDEDIR})

#Export the targets to a script
install(EXPORT scanner-targets
        FILE
        ScannerTargets.cmake
        NAMESPACE
        Scanner::
        DESTINATION
        ${INSTALL_CONFIGDIR}
        )

#Create a ConfigVersion.cmake file
include(CMakePackageConfigHelpers)
write_basic_package_version_file(
        ${CMAKE_CURRENT_BINARY_DIR}/ScannerConfigVersion.cmake
        VERSION ${PROJECT_VERSION}
        COMPATIBILITY AnyNewerVersion
)

configure_package_config_file(${CMAKE_CURRENT_LIST_DIR}/cmake/ScannerConfig.cmake.in
        ${CMAKE_CURRENT_BINARY_DIR}/ScannerConfig.cmake
        INSTALL_DESTINATION ${INSTALL_CONFIGDIR}
        )

#Install the config, configversion and custom find modules
install(FILES
        #${CMAKE_CURRENT_LIST_DIR}/cmake/FindRapidJSON.cmake
        ${CMAKE_CURRENT_BINARY_DIR}/ScannerConfig.cmake
        ${CMAKE_CURRENT_BINARY_DIR}/ScannerConfigVersion.cmake
        DESTINATION ${INSTALL_CONFIGDIR}
        )


## Exporting from the build tree
export(EXPORT scanner-targets FILE ${CMAKE_CURRENT_BINARY_DIR}/ScannerTargets.cmake NAMESPACE Scanner::)

#Register package in user's package registry
export(PACKAGE Scanner)
