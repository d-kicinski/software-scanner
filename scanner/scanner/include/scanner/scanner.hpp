#pragma once

//#include <opencv2/core/ocl.hpp>
//#include <opencv2/opencv.hpp>
//#include <vector>
#include <cstdint>

// Because we target native android opencv and as of opencv 3.4.1 doesn't
// support ndk 17 (AFAIK) we have to use ndk16 which only support c++1z
//#if __has_include(<optional>)
//#include <optional>
//#elif __has_include(<experimental/optional>)
//#include <experimental/optional>
//namespace std {
//using namespace experimental;
//}
//#else
//#error !
//#endif

namespace scanner {

void draw_contour(unsigned long int input_ptr);

void software_scanner(unsigned long int input_ptr, unsigned long int output_ptr);

} // namespace scanner
