import sys
import cv2
import numpy as np
import os
import matplotlib.pyplot as plt
from scipy import signal
import copy



def get_testset():
    IMG_PATH = "./test_images"

    #IMG_PATH = "./test.jpg"
    t1_pth = os.path.join(IMG_PATH, "1.jpg")
    t2_pth = os.path.join(IMG_PATH, "2.jpg")
    t3_pth = os.path.join(IMG_PATH, "3.jpg")
    t4_pth = os.path.join(IMG_PATH, "4.jpg")
    t5_pth = os.path.join(IMG_PATH, "5.jpg")
    t6_pth = os.path.join(IMG_PATH, "6.jpg")

    recip_pth = os.path.join(IMG_PATH, "./recip_str.jpg")
    recip_not_pth = os.path.join(IMG_PATH, "./recip_not_str.jpg")

    # load test images
    image = cv2.imread(IMG_PATH)  # read into bgr space
    t1 = cv2.imread(t1_pth)  # read into bgr space
    t2 = cv2.imread(t2_pth)  # read into bgr space
    #t3 = cv2.imread(t3_pth)  # read into bgr space
    t4 = cv2.imread(t4_pth)  # read into bgr space
    t5 = cv2.imread(t5_pth)  # read into bgr space
    t6 = cv2.imread(t6_pth)  # read into bgr space
    recip_str = cv2.imread(recip_pth)  # read into bgr space
    recip_not_str = cv2.imread(recip_not_pth)  # read into bgr space

    # rescaling images [h x w]
    images = [recip_str, recip_not_str, t2, t4, t5, t6]
    ratios = list(map(lambda img: img.shape[1]/500, images))

    resized_images = []
    for img, ratio in zip(images, ratios):
        resized_images.append(cv2.resize(img, (500, int(img.shape[0]/ratio))))

    return resized_images



def show_image(image):
    """Open new window showing given image Press 'q' to close."""
    cv2.namedWindow('result', cv2.WINDOW_NORMAL)
    cv2.imshow('result', image)
    while(True):
        if (cv2.waitKey(0) & 0xFF == ord('q')):
            cv2.destroyWindow('result')
            break



def show_images(images):
    """Open new window showing one image concatenated from given list of
    images. NOTE: Press 'q' to close.

    Args
    ----
    images: list(lists[])
           each entry in list is one row in output image grid, one row is just
           another list of images(numpy array)
    """
    rows = []
    # cast to bgr image to add color text

    for row in images.copy():
        if type(row) is dict:
            for text, img in row.items():
                cv2.cvtColor(img, cv2.COLOR_GRAY2BGR)
                cv2.putText(
                        img=img,
                        text=text,
                        org=(200, 200),
                        fontFace=cv2.FONT_HERSHEY_SIMPLEX,
                        fontScale=4,
                        color=(255, 255, 255))

                #cv2.putText(img, text, (10,10), cv2.FONT_HERSHEY_SIMPLEX, 2, cv2.BGR_COMMON['green'])

            rows.append(np.concatenate(list(row.values()), axis=1))
        else:
            rows.append(np.concatenate(row, axis=1))
            #print(row.shape)

    output_image = np.concatenate(rows, axis=0)

    cv2.namedWindow('result', cv2.WINDOW_NORMAL)
    cv2.imshow('result', output_image)

    # wait for 'q' key press to exit and destroy image window
    while(True):
        if (cv2.waitKey(0) & 0xFF == ord('q')):
            cv2.destroyWindow('result')
            break



def auto_canny(image, sigma=0.33):
    # compute the median of the single channel pixel intensities
    v = np.median(image)

    # apply automatic Canny edge detection using the computed median
    lower = int(max(0, (1.0 - sigma) * v))
    upper = int(min(255, (1.0 + sigma) * v))
    edged = cv2.Canny(image, lower, upper)

    # return the edged image
    return edged



def get_contours(image, cnts_num=5):
    gray = cv2.cvtColor(image, cv2.COLOR_BGR2GRAY)
    hls = cv2.cvtColor(image, cv2.COLOR_BGR2HLS)  # convert to hsv spaced
    h, l, s = cv2.split(hls)  # split to separate chanels


    hb,lb,sb = list(map(lambda x : cv2.GaussianBlur(x, (7, 7), 0), (h,l,s)))

    # threshold the saturation channel

    #th, th100 = cv2.threshold(l, 100, 255, cv2.THRESH_BINARY)
    th, th150 = cv2.threshold(l, 150, 255, cv2.THRESH_BINARY)
    #th, th180 = cv2.threshold(l, 180, 255, cv2.THRESH_BINARY)
    #th, th200 = cv2.threshold(l, 200, 255, cv2.THRESH_BINARY)

    #show_images([[gray,threshed2, lb, threshed]])
    #return th100, th150, th180, th200


    _, cnts, _ = cv2.findContours(th150, cv2.RETR_EXTERNAL, cv2.CHAIN_APPROX_NONE)


    ## sort and choose the largest contour
    cnts = sorted(cnts, key = cv2.contourArea)
    if len(cnts) < 5: cnts = len(cnts)
    cnts = cnts[len(cnts)-cnts_num:]
    for cnt in cnts:
        if len(cnt) < 200: cnts.remove(cnt)


    return cnts

    cv2.drawContours(canvas, cnt, -1, (0,0,255), 5)
    #show_image(canvas)

    # ## approx the contour, so the get the corner points
    arclen = cv2.arcLength(cnt, True)
    approx = cv2.approxPolyDP(cnt, 0.02* arclen, True)
    cv2.drawContours(canvas, [cnt], -1, (255,0,0), 1, cv2.LINE_AA)
    cv2.drawContours(canvas, [approx], -1, (0, 255, 0), 10, cv2.LINE_AA)
    show_image(canvas)



def calc_that(cnt, magic=0.1, margin=0.05):
    global img
    M = cv2.moments(cnt)
    cX = int(M["m10"] / M["m00"])
    cY = int(M["m01"] / M["m00"])
    centroid = np.array([cX, cY])

    dist = []
    for p in cnt:
        dist.append(np.linalg.norm(p - centroid))

    dist = np.array(dist)
    #dist = signal.medfilt(dist)

    # find maxima
    #peaks = signal.argrelextrema(dist, np.greater)[0]
    peakind = signal.find_peaks_cwt(dist, np.arange(1, magic*len(dist)))

    if peakind[0] < margin * len(cnt) and peakind[-1] > margin * len(cnt):
        peakind = peakind[0:-1]



    #cv2.drawContours(img, cnt, -1, (0,0,255), 5)
    #cv2.circle(img, (cX, cY), 7, (0,0,0), -1)
    plt.scatter(range(len(dist)), dist)
    plt.scatter(peakind, dist[peakind])
    plt.show()

    return cnt[peakind]



def test_centroid_dist(testset):
    RECT_COLOR = (0, 255, 0)
    OTHER_COLOR = (0, 0, 0)
    images = copy.deepcopy(testset)

    for img in images:
        cnts = get_contours(img, cnts_num=2)
        for cnt in cnts:
            print("cnt_num: ", len(cnt))
            corners = calc_that(cnt)
            if len(corners) == 4:
                list(map(lambda p: cv2.circle(img, tuple(p[0]), 7, RECT_COLOR, -1), corners))
            else:
                list(map(lambda p: cv2.circle(img, tuple(p[0]), 7, OTHER_COLOR, -1), corners))

    show_images([images])



def main():
    testset = get_testset()
    testset_gray = list(map(lambda img: cv2.cvtColor(img, cv2.COLOR_BGR2GRAY), testset))
    global img
    img = testset[0]
    #img = cv2.cvtColor(img, cv2.COLOR_BGR2GRAY)
    #show_image(img)


    #cnts = [preprocess(x) for x  in testset]

    #list(map(lambda p: cv2.circle(img, tuple(p[0]), 7, (0,255,0), -1), corners))

    #for p in corners:
        #print(p[0])
        #cv2.circle(img, (p[0][0], p[0][1]), 7, (255,255,255), -1)


    test_centroid_dist(testset)



if __name__ == '__main__':
    main()
