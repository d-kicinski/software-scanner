#include "CoreLibInterface.hpp"

#include <opencv2/core/core.hpp>
#include <corelib/img_utils.h>
#include <corelib/detect.hpp>

using namespace generated;

void CoreLibInterface::cvtGray(int64_t image_ptr) {
    cv::Mat * image = (cv::Mat*) image_ptr;
    cv::cvtColor(*image, *image, cv::COLOR_RGBA2GRAY);
}

void CoreLibInterface::draw_contour(int64_t input_ptr) {
  //cv::Mat & image = *(cv::Mat*) image_ptr;
  cv::Mat * input_image = (cv::Mat*) input_ptr;
    if (input_image->empty()) return;
  //cv::Mat * output_image = (cv::Mat*) output_ptr;

  //cv::resize(image, image, cv::Size(400, 400));
  //auto contour {corelib::get_main_contour(image)};

    auto white_cnts { corelib::detect_white_objects(*input_image) };
    if (auto rect = corelib::find_rect(white_cnts)) {
        std::vector<std::vector<cv::Point>> contour;    // fuk with this
        contour.push_back(*rect);                       // and this also
        // Why it doesn't work?
//        auto val {rect.value()};
//        cv::drawContours(image, std::vector{val}, 0, {0, 255, 0}, 3);

        cv::drawContours(*input_image, contour, 0, {0, 255, 0}, 3);

    }
}


std::optional<std::vector<std::vector<int64_t>>> CoreLibInterface::find_document(int64_t
                                                                                 input_ptr) {
  cv::Mat * input_image = (cv::Mat*) input_ptr;

  auto img(input_image);
  double ratio = (double)(img->size().width) / 500.0;
  cv::resize(*img, *img, {500, (int)(img->size().height / ratio)});

  auto white_cnts { corelib::detect_white_objects(*img) };
  auto rect { corelib::find_rect(white_cnts) };
  if (auto rect = corelib::find_rect(white_cnts)) {
      std::for_each(rect->begin(), rect->end(), [&ratio](auto &p) { p *= ratio; });

      std::vector<std::vector<int64_t >> output;
      for (auto &p: *rect) {
          std::vector<int64_t> point {p.x, p.y};
          output.push_back(point);
      }
      return std::make_optional(output);
  }
  else {
    return std::nullopt;
  }
}



void CoreLibInterface::software_scanner(int64_t input_ptr, int64_t output_ptr)
{
    cv::Mat * input_image = (cv::Mat*) input_ptr;
    cv::Mat * output_image = (cv::Mat*) output_ptr;

    if (auto scan = corelib::soft_scanner(*input_image)) {
        *output_image = *scan;
    } else {
        *output_image = *input_image;
    }
}


std::string CoreLibInterface::hello(const std::string & name) {
  return std::string {"Hello " + name};
}
