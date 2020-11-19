package com.github.dawidkski.scanner.camera.frame;

import android.media.Image;

import org.opencv.core.CvType;
import org.opencv.core.Mat;

import java.nio.ByteBuffer;

public class GrayCameraFrame implements CameraFrame {

    private final Image mImage;
    private Mat mFrame;

    public GrayCameraFrame(Image image) {
        super();
        mImage = image;
        mFrame = new Mat();
    }

    @Override
    public Mat get() {
        int width = mImage.getWidth();
        int height = mImage.getHeight();

        Image.Plane[] planes = mImage.getPlanes();
        ByteBuffer yPlane = planes[0].getBuffer();
        int yPlaneStep = planes[0].getRowStride();
        mFrame = new Mat(height, width, CvType.CV_8UC1, yPlane, yPlaneStep);
        return mFrame;
    }

    @Override
    public void close() {
        mImage.close();
        mFrame.release();
    }

}
