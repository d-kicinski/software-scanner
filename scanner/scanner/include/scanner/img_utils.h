#include <string>

#include <opencv2/core/core.hpp>
#include <opencv2/highgui/highgui.hpp>
#include <opencv2/imgproc/imgproc.hpp>

namespace scanner {
void show_img(const std::string &file_name);

void show_image(const std::vector<cv::Mat> &images);

void show_image(const cv::Mat &image);

void to_gray(cv::Mat &img);

cv::Mat android2opencv(const std::vector<uint8_t> &image, int32_t frame_width, int32_t frame_height,
                       int32_t rotation_degrees);

std::vector<uint8_t> opencv2android(const cv::Mat &image);

void rotate_image(cv::Mat &image, int32_t rotation_degrees);

} // namespace scanner
