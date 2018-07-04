#! /usr/bin/env bash
deps/djinni/src/run-assume-built \
    --java-out android/app/src/main/java/dying/slowly/generated \
    --java-package dying.slowly.generated \
    --ident-java-field mFooBar \
    \
    --jni-out android/app/src/main/cpp/jni \
    --ident-jni-class NativeFooBar \
    --ident-jni-file NativeFooBar \
    \
    --cpp-out android/app/src/main/cpp/interfaces \
    --cpp-namespace generated \
    --ident-cpp-enum-type foo_bar \
    \
    --idl native_api.djinni
