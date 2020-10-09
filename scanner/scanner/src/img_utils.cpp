#include "scanner/img_utils.h"
#include <iostream>

namespace scanner {

void show_image(const std::string &file_name)
{
    cv::Mat image = cv::imread(file_name, cv::IMREAD_COLOR);

    if (!image.data) {
        std::cout << "Could not open or find the image \n";
    }

    cv::namedWindow("Image", cv::WINDOW_NORMAL);
    cv::imshow("Image", image);

    // cv::waitKey(0);

    while (true) {
        if (char k = cv::waitKey(0); k == 'q')
            break;
    }
}

void show_image(const cv::Mat &image)
{
    cv::namedWindow("Image", cv::WINDOW_NORMAL);
    cv::imshow("Image", image);

    // cv::waitKey(0);

    while (true) {
        if (char k = cv::waitKey(0); k == 'q')
            break;
    }
}

void show_image(const std::vector<cv::Mat> &images)
{
    cv::namedWindow("Image", cv::WINDOW_NORMAL);
    // for (cv::Mat const & image : images)
    //{
    // cv::imshow("Image", image);
    //}

    for (int i = 0; i < images.size(); i++) {
        char label[50];
        sprintf(label, "Image: %d ", i);
        cv::imshow(label, images[i]);
    }

    while (true) {
        if (char k = cv::waitKey(0); k == 'q')
            break;
    }
}

void to_gray(cv::Mat &img) { cv::cvtColor(img, img, cv::COLOR_RGBA2GRAY); }

cv::Mat android2opencv(const std::vector<uint8_t> &image, int32_t frame_width, int32_t frame_height,
                       int32_t rotation_degrees)
{
    cv::Mat nv21(frame_height + frame_height / 2, frame_width, CV_8UC1, (uchar *)image.data());
    cv::cvtColor(nv21, nv21, cv::COLOR_YUV2RGB_NV21);
    rotate_image(nv21, rotation_degrees);

    return nv21;
}

std::vector<uint8_t> opencv2android(const cv::Mat &image)
{
    cv::Mat mat;
    cv::cvtColor(image, mat, cv::COLOR_RGB2YUV);

    // no idea wtf is going on, just copying from stackoverflow
    std::vector<uchar> array;
    if (mat.isContinuous()) {
        array.assign(mat.datastart, mat.dataend);
    } else {
        for (int i = 0; i < mat.rows; ++i) {
            array.insert(array.end(), mat.ptr<uchar>(i), mat.ptr<uchar>(i) + mat.cols);
        }
    }
    return array;
}

void rotate_image(cv::Mat &image, int32_t rotation_degrees)
{
    switch (rotation_degrees) {
    case 90:
        cv::rotate(image, image, cv::ROTATE_90_COUNTERCLOCKWISE);
        break;
    case 180:
        cv::rotate(image, image, cv::ROTATE_180);
        break;
    case 270:
        cv::rotate(image, image, cv::ROTATE_90_CLOCKWISE);
        break;
    default:
        break;
    }
}

} // namespace scanner
