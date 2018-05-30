#pragma once

#include <opencv2/core/types.hpp>
#include "djinni_support.hpp"

namespace cv {
namespace djinni {
namespace jni {

class NativePoint final : ::djinni::JniInterface<::cv::Point, NativePoint> {
public:
    using CppType = ::cv::Point;
    using JniType = jobject;

    using Boxed = NativePoint;

    ~NativePoint();

    static CppType toCpp(JNIEnv* jniEnv, JniType pointObj) {
        auto pointClass = jniEnv->GetObjectClass(pointObj);
        jfieldID nativeObj = jniEnv->GetFieldID(pointClass, "nativeObj", "J");
        long pointPtr = jniEnv->GetLongField(pointObj, nativeObj);
        ::cv::Point& point = *((::cv::Point*)pointPtr);
        return point;

    }

//    static ::djinni::LocalRef<JniType> fromCpp(JNIEnv* jniEnv, const CppType& c) {
//        construct the Java Mat with a pointer to the C++ Mat
//    }

private:
    NativePoint();
    friend ::djinni::JniClass<NativePoint>;
    friend ::djinni::JniInterface<::cv::Point, NativePoint>;
};

}  // namespace jni
}  // namespace djinni
}  // namespace cv
