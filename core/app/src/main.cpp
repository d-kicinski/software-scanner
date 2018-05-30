#include <opencv2/core/core.hpp>
#include <opencv2/imgproc/imgproc.hpp>
#include <opencv2/highgui/highgui.hpp>

#include <iostream>
#include <corelib/img_utils.h>
#include <corelib/detect.hpp>

using namespace std;


//std::string TEST_IMG = "/home/dave/code/main/handy/core/app/7.jpg";
std::string TEST_IMG = "/home/dave/code/main/handy/core/app/1.jpg";

cv::Mat read_image(std::string const & img_path)
{
  cv::Mat image = cv::imread(img_path, CV_LOAD_IMAGE_COLOR);
  if (!image.data){
    std::cout << "Could not open or find the image \n";
  }
  return image;
}


int main()
{
  // auto image {read_image(TEST_IMG)};

  std::vector<int> v_point = {0, 1};
  cv::Point point(v_point)

  //auto contours = detect_white_objects(image);

  // corelib::show_image(image);
  // auto idk  = detect_white_objects(image);


  // if (auto warped_img = corelib::soft_scanner(image)) {
  //   corelib::show_image(*warped_img);
  // }
}
