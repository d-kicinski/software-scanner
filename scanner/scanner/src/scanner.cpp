#include <scanner/scanner.hpp>
#include <scanner/img_utils.h>

namespace scanner {
float median(cv::Mat Input, int nVals=256)
{
    // refer to
    // https://stackoverflow.com/questions/30078756/super-fast-median-of-matrix-in-opencv-as-fast-as-matlab
    // COMPUTE HISTOGRAM OF SINGLE CHANNEL MATRIX
    float range[] = {0, (float)nVals};
    const float *histRange = {range};
    bool uniform = true;
    bool accumulate = false;
    cv::Mat hist;
    cv::calcHist(&Input, 1, 0, cv::Mat(), hist, 1, &nVals, &histRange, uniform, accumulate);

    // COMPUTE CUMULATIVE DISTRIBUTION FUNCTION (CDF)
    cv::Mat cdf;
    hist.copyTo(cdf);
    for (int i = 1; i <= nVals - 1; i++) {
        cdf.at<float>(i) += cdf.at<float>(i - 1);
    }
    cdf /= Input.total();

    // COMPUTE MEDIAN
    float medianVal;
    for (int i = 0; i <= nVals - 1; i++) {
        if (cdf.at<float>(i) >= 0.5) {
            medianVal = i;
            break;
        }
    }
    // return medianVal/nVals;
    return medianVal;
}

void auto_canny(cv::Mat const &img, cv::Mat &out, float sigma=0.33)
{
    // compute the median
    float m = median(img);

    // apply automatic Canny edge detectoin using computed median
    int lower = std::max(0.0, (1.0 - sigma) * m);
    int upper = std::min(255.0, (1.0 + sigma) * m);

    return cv::Canny(img, out, lower, upper);
}

cv::Mat four_point_transform(cv::Mat const &img, std::vector<cv::Point> points)
{
    // auto [tl, tr, br, bl] = points;
    auto tl = points[0];
    auto tr = points[1];
    auto br = points[2];
    auto bl = points[3];

    // compute the width of the new image -> max of euqlidian distance between
    // horizontal aligment points
    auto width_bottom = std::sqrt(std::pow(br.x - bl.x, 2) + std::pow(br.y - bl.y, 2));
    auto width_top = std::sqrt(std::pow(tr.x - tl.x, 2) + std::pow(tr.y - tl.y, 2));
    auto width = std::max((int)width_top, (int)width_bottom);

    // compute the height of the new image
    auto height_right = std::sqrt(std::pow(tr.x - br.x, 2) + std::pow(tr.y - br.y, 2));
    auto height_left = std::sqrt(std::pow(tl.x - bl.x, 2) + std::pow(tl.y - bl.y, 2));
    auto height = std::max((int)height_right, (int)height_left);

    // compute destination points we want to get after perspective correction
    auto dst =
        std::vector<cv::Point>{{0, 0}, {width - 1, 0}, {width - 1, height - 1}, {0, height - 1}};

    std::vector<cv::Point2f> pointsF(points.begin(), points.end());
    std::vector<cv::Point2f> dstF(dst.begin(), dst.end());

    // compute the perspective transform and then applay it
//    std::cout << points.size() << dst.size() << std::endl;
    cv::Mat m = cv::getPerspectiveTransform(pointsF, dstF);

    cv::Mat out;
    cv::warpPerspective(img, out, m, {width, height});

    return out;
}

void order_points(std::vector<cv::Point> const &pts, std::vector<cv::Point> &dst)
{
    auto less_by_sum = [](cv::Point const &lhs, cv::Point const &rhs) {
        return (lhs.x + lhs.y) < (rhs.x + rhs.y);
    };

    auto less_by_diff = [](cv::Point const &lhs, cv::Point const &rhs) {
        return (lhs.x - lhs.y) < (rhs.x - rhs.y);
    };

    auto ptsc = pts;

    auto [tl_it, br_it] = std::minmax_element(ptsc.begin(), ptsc.end(), less_by_sum);
    auto [bl_it, tr_it] = std::minmax_element(ptsc.begin(), ptsc.end(), less_by_diff);

    dst[0] = *tl_it;
    dst[1] = *tr_it;
    dst[2] = *br_it;
    dst[3] = *bl_it;
}

std::vector<std::vector<cv::Point>> detect_white_objects(cv::Mat const &img, bool sort=true)
{
    cv::Mat img_hls;

    // Creating binary mask segmenting white colors.
    std::vector<cv::Mat> hls_channels;
    cv::cvtColor(img, img_hls, cv::COLOR_RGB2HLS);
    cv::split(img_hls, hls_channels);
    auto mask = hls_channels[1];

    cv::GaussianBlur(mask, mask, cv::Size(7, 7), 1.5,
                     1.5); // magic numbers are ok here :P
    cv::threshold(mask, mask, 150, 255, cv::THRESH_BINARY);

    // Looking for largest countour
    std::vector<std::vector<cv::Point>> contours;
    cv::findContours(mask, contours, {}, cv::RETR_LIST, cv::CHAIN_APPROX_NONE);

    if (sort) {
        // Sorting contours by size in descending order
        std::sort(contours.begin(), contours.end(), [](auto const &c1, auto const &c2) {
            return cv::contourArea(c1) > cv::contourArea(c2);
        });
    }

    return contours;
}

std::optional<std::vector<cv::Point>> find_rect(std::vector<std::vector<cv::Point>> const &contours)
{
    if (contours.size() == 0)
        return std::nullopt;

    // Approximate contour with only four points
    std::vector<cv::Point> scan_contour;
    std::vector<cv::Point> approx;

    for (auto c = contours.begin(); c != contours.end(); ++c) {
        auto peri = cv::arcLength(*c, true);
        cv::approxPolyDP(*c, approx, 0.05 * peri, true);

        if (approx.size() == 4) {
            scan_contour = approx;
            break;
        }
    }
    order_points(scan_contour, scan_contour);

    // In case of not succeded at aproximating with 4 points
    return (scan_contour.size() != 0) ? std::make_optional(scan_contour) : std::nullopt;
}

/* Deprecated */
std::vector<cv::Point> get_main_contour(cv::Mat const &img)
{
    // Detecting edges
    cv::Mat img_gray, h_blur, edges, img_hsv;
    // split the channels
    std::vector<cv::Mat> hsv_channels;
    cv::cvtColor(img, img_hsv, cv::COLOR_BGR2HLS);
    cv::split(img_hsv, hsv_channels);
    auto h = hsv_channels[1];

    cv::GaussianBlur(h, h_blur, cv::Size(7, 7), 1.5, 1.5);
    cv::threshold(h_blur, h_blur, 50, 200, cv::THRESH_BINARY);

    auto_canny(h_blur, edges, 0.40);

    // Looking for largest countour
    std::vector<std::vector<cv::Point>> contours;
    cv::findContours(edges, contours, {}, cv::RETR_LIST, cv::CHAIN_APPROX_SIMPLE);

    // Sorting contours by size in descending order
    std::sort(contours.begin(), contours.end(), [](auto const &c1, auto const &c2) {
        return cv::contourArea(c1) > cv::contourArea(c2);
    });

    // Approximate contour with only four points
    std::vector<cv::Point> scan_contour;
    std::vector<cv::Point> approx;
    {
        auto &s = contours;
        for (auto c = s.begin(); c != s.end(); ++c) {
            auto peri = cv::arcLength(*c, true);
            cv::approxPolyDP(*c, approx, 0.05 * peri, true);

            scan_contour = approx;
            break;
        }
    }

    if (scan_contour.size() == 0) {
        return {{0, 0}, {img.rows - 1, 0}, {img.rows - 1, img.cols - 1}, {0, img.cols - 1}};
    }

    return scan_contour;
}

std::optional<cv::Mat> soft_scanner(cv::Mat const &org)
{

    auto img(org);

    float ratio = (float)(img.size().width) / 500.0;
    cv::resize(img, img, {500, (int)(img.size().height / ratio)});

    auto img_cnts(img);
    auto img_rects(img);
    auto img_with_cntf(org);

    auto white_cnts{detect_white_objects(img)};
    if (auto rect = find_rect(white_cnts)) {
        std::for_each(rect->begin(), rect->end(), [&ratio](auto &p) { p *= ratio; });

        auto warped = four_point_transform(org, *rect);

        return std::make_optional(warped);
    } else {
        return std::nullopt;
    }
}

void software_scanner(unsigned long int input_ptr, unsigned long int output_ptr)
{
    cv::Mat *input_image = (cv::Mat *)input_ptr;
    cv::Mat *output_image = (cv::Mat *)output_ptr;

    if (auto scan = scanner::soft_scanner(*input_image)) {
        *output_image = *scan;
    } else {
        *output_image = *input_image;
    }
}

void draw_contour(unsigned long int input_ptr)
{
    // cv::Mat & image = *(cv::Mat*) image_ptr;
    cv::Mat *input_image = (cv::Mat *)input_ptr;
    if (input_image->empty())
        return;
    // cv::Mat * output_image = (cv::Mat*) output_ptr;

    // cv::resize(image, image, cv::Size(400, 400));
    // auto contour {scanner::get_main_contour(image)};

    auto white_cnts{scanner::detect_white_objects(*input_image)};
    if (auto rect = scanner::find_rect(white_cnts)) {
        if (cv::contourArea(*rect) > 0.10 * (input_image->rows * input_image->cols)) {
            std::vector<std::vector<cv::Point>> contour; // fuk with this
            contour.push_back(*rect);                    // and this also
            // Why it doesn't work?
            //        auto val {rect.value()};
            //        cv::drawContours(image, std::vector{val}, 0, {0, 255, 0}, 3);

            cv::drawContours(*input_image, contour, 0, {0, 255, 0}, 3);
        }
    }
}
} // namespace scanner
