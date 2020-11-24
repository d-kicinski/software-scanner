package com.github.dawidkski.scanner.camera;

import android.media.Image;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

class ImageSaver implements Runnable {

    private final Image image;
    private final File file;
    private final Camera.StillImageListener listener;

    ImageSaver(Image image, File file, Camera.StillImageListener listener) {
        this.image = image;
        this.file = file;
        this.listener = listener;
    }

    @Override
    public void run() {
        Log.d(ImageSaver.class.getSimpleName(), "Save image to " + file.toString());
        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);
        FileOutputStream output = null;
        try {
            output = new FileOutputStream(file);
            output.write(bytes);

            // Call callback on main thread
            Handler mainHandler = new Handler(Looper.getMainLooper());
            mainHandler.post(new Runnable() {
                @Override
                public void run() {
                    listener.onStillImage();
                }
            });

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            image.close();
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
