package com.github.dawidkski.scanner.camera;

import com.github.dawidkski.scanner.camera.frame.CameraFrame;

import org.opencv.core.Mat;

public interface CameraViewListener {

    Mat onCameraFrame(CameraFrame inputFrame);

    void onPictureTaken();

}
