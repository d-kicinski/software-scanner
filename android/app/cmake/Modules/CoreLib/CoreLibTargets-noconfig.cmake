#----------------------------------------------------------------
# Generated CMake target import file.
#----------------------------------------------------------------

# Commands may need to know the format version.
set(CMAKE_IMPORT_FILE_VERSION 1)

# Import target "CoreLib::CoreLib" for configuration ""
set_property(TARGET CoreLib::CoreLib APPEND PROPERTY IMPORTED_CONFIGURATIONS NOCONFIG)
set_target_properties(CoreLib::CoreLib PROPERTIES
  IMPORTED_LINK_INTERFACE_LANGUAGES_NOCONFIG "CXX"
  IMPORTED_LOCATION_NOCONFIG "${_IMPORT_PREFIX}/lib64/libcorelib.a"
  )

list(APPEND _IMPORT_CHECK_TARGETS CoreLib::CoreLib )
list(APPEND _IMPORT_CHECK_FILES_FOR_CoreLib::CoreLib "${_IMPORT_PREFIX}/lib64/libcorelib.a" )

# Commands beyond this point should not need to know the version.
set(CMAKE_IMPORT_FILE_VERSION)
