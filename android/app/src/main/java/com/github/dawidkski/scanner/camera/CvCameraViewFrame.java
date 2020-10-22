package com.github.dawidkski.scanner.camera;

import org.opencv.core.Mat;

/**
 * This class interface is abstract representation of single frame from camera for onCameraFrame callback
 * Attention: Do not use objects, that represents this interface out of onCameraFrame callback!
 */
public interface CvCameraViewFrame {

    /**
     * This method returns RGBA Mat with frame
     */
    Mat rgba();

    /**
     * This method returns single channel gray scale Mat with frame
     */
    Mat gray();
};
