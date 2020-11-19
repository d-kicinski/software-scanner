package com.github.dawidkski.scanner.camera;

import com.github.dawidkski.scanner.camera.frame.CameraFrame;

import org.opencv.core.Mat;

public interface CvCameraViewListener {
    /**
     * This method is invoked when camera preview has started. After this method is invoked
     * the frames will start to be delivered to client via the onCameraFrame() callback.
     * @param width -  the width of the frames that will be delivered
     * @param height - the height of the frames that will be delivered
     */
    void onCameraViewStarted(int width, int height);

    /**
     * This method is invoked when camera preview has been stopped for some reason.
     * No frames will be delivered via onCameraFrame() callback after this method is called.
     */
    void onCameraViewStopped();

    /**
     * This method is invoked when delivery of the frame needs to be done.
     * The returned values - is a modified frame which needs to be displayed on the screen.
     * TODO: pass the parameters specifying the format of the frame (BPP, YUV or RGB and etc)
     */
    Mat onCameraFrame(CameraFrame inputFrame);

    void onPictureTaken();
}
