package com.github.dawidkski.scanner.camera.frame;

import org.opencv.core.Mat;


/**
 * This class interface is abstract representation of single frame from camera for onCameraFrame callback
 * Attention: Do not use objects, that represents this interface out of onCameraFrame callback!
 */
public interface CameraFrame extends AutoCloseable {

    Mat get();

    @Override
    void close();

};
