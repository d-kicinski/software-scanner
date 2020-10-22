package com.github.dawidkski.scanner.camera;

import android.media.Image;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

public class ImageSaver implements Runnable {

    private final Image mImage;
    private final File mFile;
    private final CvCameraViewListener mListener;

    ImageSaver(Image image, File file, CvCameraViewListener listener) {
        mImage = image;
        mFile = file;
        mListener =listener;
    }

    @Override
    public void run() {
        Log.d(ImageSaver.class.getSimpleName(), "Save image to " + mFile.toString());
        ByteBuffer buffer = mImage.getPlanes()[0].getBuffer();
        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);
        FileOutputStream output = null;
        try {
            output = new FileOutputStream(mFile);
            output.write(bytes);
            mListener.onPictureTaken();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            mImage.close();
            if (null != output) {
                try {
                    output.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

}
