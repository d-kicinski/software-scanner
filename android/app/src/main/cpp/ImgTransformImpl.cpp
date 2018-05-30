#include "ImgTransformWrapper.hpp"

#include <corelib/img_utils.h>
#include <opencv2/core/core.hpp>

using namespace generated;
std::vector<uint8_t> ImgTransformWrapper::cvtGray(const std::vector<uint8_t> & image, int32_t
frame_height, int32_t frame_width, int32_t rotation_degrees)
{

  cv::Mat img = corelib::android2opencv(image,
                          frame_width,
                          frame_height,
                          rotation_degrees);

  corelib::to_gray(img);


  return corelib::opencv2android(img);
}

std::string ImgTransformWrapper::hello(const std::string & name) {
  return std::string {"Hello " + name};
}
