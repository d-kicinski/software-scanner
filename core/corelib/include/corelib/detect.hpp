#pragma once

#include <opencv2/core/ocl.hpp>
#include <opencv2/opencv.hpp>
#include <vector>

// Because we target native android opencv and as of opencv 3.4.1 doesn't
// support ndk 17 (AFAIK) we have to use ndk16 which only support c++1z
#if __has_include(<optional>)
# include <optional>
#elif __has_include(<experimental/optional>)
# include <experimental/optional>
namespace std
{
  using namespace experimental;
}
#else
#error !
#endif



namespace corelib {

float median(cv::Mat Input, int nVals=256);


void auto_canny(cv::Mat const & img, cv::Mat & out, float sigma=0.33);


cv::Mat four_point_transform(cv::Mat const & img, std::vector<cv::Point> points);


std::vector<cv::Point> get_main_contour(cv::Mat const & img);


std::vector<std::vector<cv::Point>> detect_white_objects(cv::Mat const & img, bool sort=true);


std::optional<std::vector<cv::Point>>
find_rect(std::vector<std::vector<cv::Point>> const & contours);


std::optional<cv::Mat> soft_scanner(cv::Mat const & org);

}
