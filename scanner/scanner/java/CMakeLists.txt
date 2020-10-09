set_property(SOURCE scanner.i PROPERTY CPLUSPLUS ON)
set_property(SOURCE scanner.i PROPERTY COMPILE_OPTIONS
        -package ${JAVA_PACKAGE}.scanner)
swig_add_library(jniScanner
        TYPE OBJECT
        LANGUAGE java
        OUTPUT_DIR
        ${PROJECT_BINARY_DIR}/java/${JAVA_PROJECT}/${JAVA_PACKAGE_PATH}/scanner
        SOURCES scanner.i)

set_target_properties(jniScanner PROPERTIES
        SWIG_INCLUDE_DIRECTORIES ${PROJECT_SOURCE_DIR}/java
        SWIG_USE_TARGET_INCLUDE_DIRECTORIES ON
        POSITION_INDEPENDENT_CODE ON)
target_include_directories(jniScanner PRIVATE ${JNI_INCLUDE_DIRS})
target_link_libraries(jniScanner PRIVATE scanner)
