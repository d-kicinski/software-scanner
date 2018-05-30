get_filename_component(CoreLib_CMAKE_DIR "${CMAKE_CURRENT_LIST_FILE}" PATH)
include(CMakeFindDependencyMacro)

list(APPEND CMAKE_MODULE_PATH ${CoreLib_CMAKE_DIR})

# NOTE Had to use find_package because find_dependency does not support COMPONENTS or MODULE until 3.8.0

#find_dependency(Boost 1.55 REQUIRED COMPONENTS regex)
#find_dependency(RapidJSON 1.0 REQUIRED MODULE)
#find_package(Boost 1.55 REQUIRED COMPONENTS regex)
#find_package(RapidJSON 1.0 REQUIRED MODULE)
find_package(OpenCV REQUIRED)

list(REMOVE_AT CMAKE_MODULE_PATH -1)

if(NOT TARGET CoreLib::CoreLib)
    include("${CoreLib_CMAKE_DIR}/CoreLibTargets.cmake")
endif()

set(CoreLib_LIBRARIES CoreLib::CoreLib)
