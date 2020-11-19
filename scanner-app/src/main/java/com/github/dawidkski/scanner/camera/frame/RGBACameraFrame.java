package com.github.dawidkski.scanner.camera.frame;

import android.media.Image;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import java.nio.ByteBuffer;

public class RGBACameraFrame implements CameraFrame {

    private final Image mImage;
    private Mat mFrame;

    public RGBACameraFrame(Image image) {
        super();
        mImage = image;
        mFrame = new Mat();
    }

    @Override
    public Mat get() {
        int width = mImage.getWidth();
        int height = mImage.getHeight();

        Image.Plane[] planes = mImage.getPlanes();
        int chromaPixelStride = planes[1].getPixelStride();

        if (chromaPixelStride == 2) {
            mFrame = chromaChannelsInterleaved(planes, width, height);

        } else {
            mFrame = chromaChannelsNotInterleaved(planes, width, height);
        }
        return mFrame;
    }

    @Override
    public void close() {
        mImage.close();
        mFrame.release();
    }

    private Mat chromaChannelsInterleaved(Image.Plane[] planes, int width, int height) {
        ByteBuffer yPlane = planes[0].getBuffer();
        int yPlaneStep = planes[0].getRowStride();

        ByteBuffer uvPlane1 = planes[1].getBuffer();
        int uvPlane1Step = planes[1].getRowStride();

        ByteBuffer uvPlane2 = planes[2].getBuffer();
        int uvPlane2Step = planes[2].getRowStride();

        Mat rgbaMat = new Mat();
        Mat yMat = new Mat(height, width, CvType.CV_8UC1, yPlane, yPlaneStep);
        Mat uvMat1 = new Mat(height / 2, width / 2, CvType.CV_8UC2, uvPlane1, uvPlane1Step);
        Mat uvMat2 = new Mat(height / 2, width / 2, CvType.CV_8UC2, uvPlane2, uvPlane2Step);

        long addressDistance = uvMat2.dataAddr() - uvMat1.dataAddr();
        if (addressDistance > 0) {
            Imgproc.cvtColorTwoPlane(yMat, uvMat1, rgbaMat, Imgproc.COLOR_YUV2RGBA_NV12);
        } else {
            Imgproc.cvtColorTwoPlane(yMat, uvMat2, rgbaMat, Imgproc.COLOR_YUV2RGBA_NV21);
        }
        return rgbaMat;
    }

    private Mat chromaChannelsNotInterleaved(Image.Plane[] planes, int width, int height) {
        byte[] yuvBytes = new byte[width * (height + height / 2)];
        ByteBuffer yPlane = planes[0].getBuffer();
        ByteBuffer uPlane = planes[1].getBuffer();
        ByteBuffer vPlane = planes[2].getBuffer();

        int yuvBytesOffset = 0;

        int yPlaneStep = planes[0].getRowStride();
        if (yPlaneStep == width) {
            yPlane.get(yuvBytes, 0, width * height);
            yuvBytesOffset = width * height;
        } else {
            int padding = yPlaneStep - width;
            for (int i = 0; i < height; i++) {
                yPlane.get(yuvBytes, yuvBytesOffset, width);
                yuvBytesOffset += width;
                if (i < height - 1) {
                    yPlane.position(yPlane.position() + padding);
                }
            }
        }

        int chromaRowStride = planes[1].getRowStride();
        int chromaRowPadding = chromaRowStride - width / 2;

        if (chromaRowPadding == 0) {
            // When the row stride of the chroma channels equals their width, we can copy
            // the entire channels in one go
            uPlane.get(yuvBytes, yuvBytesOffset, width * height / 4);
            yuvBytesOffset += width * height / 4;
            vPlane.get(yuvBytes, yuvBytesOffset, width * height / 4);
        } else {
            // When not equal, we need to copy the channels row by row
            for (int i = 0; i < height / 2; i++) {
                uPlane.get(yuvBytes, yuvBytesOffset, width / 2);
                yuvBytesOffset += width / 2;
                if (i < height / 2 - 1) {
                    uPlane.position(uPlane.position() + chromaRowPadding);
                }
            }
            for (int i = 0; i < height / 2; i++) {
                vPlane.get(yuvBytes, yuvBytesOffset, width / 2);
                yuvBytesOffset += width / 2;
                if (i < height / 2 - 1) {
                    vPlane.position(vPlane.position() + chromaRowPadding);
                }
            }
        }

        Mat rgbaMa = new Mat();
        Mat yuvMat = new Mat(height + height / 2, width, CvType.CV_8UC1);
        yuvMat.put(0, 0, yuvBytes);
        Imgproc.cvtColor(yuvMat, rgbaMa, Imgproc.COLOR_YUV2RGBA_I420, 4);
        return rgbaMa;
    }

}
