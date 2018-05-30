cmake_minimum_required(VERSION 3.10)
project(libcorelib VERSION 1.0.0 LANGUAGES CXX)

#list(INSERT CMAKE_MODULE_PATH 0 ${CMAKE_SOURCE_DIR}/cmake)


## Creating library

# Create target and set properties
add_library(corelib
  src/img_utils
  src/detect
)

#Add an alias so that library can be used inside the build tree, e.g. when testing
add_library(CoreLib::corelib ALIAS corelib)

#Set target properties
target_include_directories(corelib
    PUBLIC
        $<INSTALL_INTERFACE:include>
        $<BUILD_INTERFACE:${CMAKE_CURRENT_SOURCE_DIR}/include>
    PRIVATE
        ${CMAKE_CURRENT_SOURCE_DIR}/src
)

target_compile_features(corelib PRIVATE cxx_auto_type)
target_compile_options(corelib PRIVATE -Werror)

# import external targets
find_package(OpenCV)

# link dependencies
target_link_libraries(corelib
  PRIVATE
    ${OpenCV_LIBS}
)

##############################################
# Installation instructions
include(GNUInstallDirs)
set(INSTALL_CONFIGDIR ${CMAKE_INSTALL_LIBDIR}/cmake/CoreLib)

install(TARGETS corelib
    EXPORT corelib-targets
    LIBRARY DESTINATION ${CMAKE_INSTALL_LIBDIR}
    ARCHIVE DESTINATION ${CMAKE_INSTALL_LIBDIR}
)

#This is required so that the exported target has the name CoreLib and not corelib
set_target_properties(corelib PROPERTIES EXPORT_NAME CoreLib)

install(DIRECTORY include/ DESTINATION ${CMAKE_INSTALL_INCLUDEDIR})

#Export the targets to a script
install(EXPORT corelib-targets
  FILE
    CoreLibTargets.cmake
  NAMESPACE
    CoreLib::
  DESTINATION
    ${INSTALL_CONFIGDIR}
)

#Create a ConfigVersion.cmake file
include(CMakePackageConfigHelpers)
write_basic_package_version_file(
    ${CMAKE_CURRENT_BINARY_DIR}/CoreLibConfigVersion.cmake
    VERSION ${PROJECT_VERSION}
    COMPATIBILITY AnyNewerVersion
)

configure_package_config_file(${CMAKE_CURRENT_LIST_DIR}/cmake/CoreLibConfig.cmake.in
    ${CMAKE_CURRENT_BINARY_DIR}/CoreLibConfig.cmake
    INSTALL_DESTINATION ${INSTALL_CONFIGDIR}
)

#Install the config, configversion and custom find modules
install(FILES
    #${CMAKE_CURRENT_LIST_DIR}/cmake/FindRapidJSON.cmake
    ${CMAKE_CURRENT_BINARY_DIR}/CoreLibConfig.cmake
    ${CMAKE_CURRENT_BINARY_DIR}/CoreLibConfigVersion.cmake
    DESTINATION ${INSTALL_CONFIGDIR}
)


## Exporting from the build tree
export(EXPORT corelib-targets FILE ${CMAKE_CURRENT_BINARY_DIR}/CoreLibTargets.cmake NAMESPACE CoreLib::)

#Register package in user's package registry
export(PACKAGE CoreLib)
