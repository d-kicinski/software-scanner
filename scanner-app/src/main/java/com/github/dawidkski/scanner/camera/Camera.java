package com.github.dawidkski.scanner.camera;

import android.graphics.ImageFormat;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Surface;

import com.github.dawidkski.scanner.camera.frame.CameraFrame;
import com.github.dawidkski.scanner.camera.frame.RGBACameraFrame;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;


public class Camera {

    private final static int PREVIEW_FORMAT = ImageFormat.YUV_420_888;
    private final static int MAX_UNSPECIFIED = -1;

    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();

    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    CameraDevice cameraDevice;
    CameraCaptureSession cameraCaptureSession;
    CaptureRequest.Builder previewRequestBuilder;
    CaptureRequest previewRequest;
    Handler backgroundHandler;
    CameraState state = CameraState.STATE_PREVIEW;
    CameraCaptureCaptureCallback cameraCaptureCaptureCallback;


    private final CameraDeviceStateCallback cameraDeviceStateCallback;
    private final Size maxPreviewSize;
    private final ImageReader.OnImageAvailableListener onImageAvailableListener
            = new ImageReader.OnImageAvailableListener() {
        @Override
        public void onImageAvailable(ImageReader reader) {
            Log.d(this.getClass().getSimpleName(), "Posting saving to file: " + file.toString());
            backgroundHandler.post(new ImageSaver(reader.acquireNextImage(), file, stillImageListener));
        }
    };

    private HandlerThread backgroundThread;
    private CameraCharacteristics characteristics;
    private PreviewFrameListener previewFrameListener;
    private StillImageListener stillImageListener;
    private ImageReader stillImageReader;
    private ImageReader previewImageReader;
    private File file;
    private int screenOrientation;
    private int sensorOrientation;


    public void setPreviewFrameListener(PreviewFrameListener previewFrameListener) {
        this.previewFrameListener = previewFrameListener;
    }

    public void setStillImageListener(StillImageListener stillImageListener) {
        this.stillImageListener = stillImageListener;
    }


    public Camera() {
        this.cameraCaptureCaptureCallback = new CameraCaptureCaptureCallback(this);
        this.cameraDeviceStateCallback = new CameraDeviceStateCallback(this);
        this.maxPreviewSize = new Size(-1, -1);
    }


    public void takePicture(File file, int screenOrientation) {
        this.file = file;
        this.screenOrientation = screenOrientation;
        lockFocus();
    }

    public void open(CameraManager manager) {
        startBackgroundThread();

        try {
            for (String cameraId : manager.getCameraIdList()) {
                this.characteristics = manager.getCameraCharacteristics(cameraId);

                Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
                if (facing != null && facing == CameraCharacteristics.LENS_FACING_FRONT) {
                    continue;
                }

                StreamConfigurationMap map = characteristics.get(
                        CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                if (map == null) {
                    continue;
                }

                // For still image captures, we use the largest available size.
                Size largest = Collections.max(
                        Arrays.asList(map.getOutputSizes(ImageFormat.JPEG)),
                        new CompareSizesByArea());
                stillImageReader = ImageReader.newInstance(largest.getWidth(), largest.getHeight(),
                        ImageFormat.JPEG, 1);
                stillImageReader.setOnImageAvailableListener(
                        onImageAvailableListener, backgroundHandler);
                sensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);

                manager.openCamera(cameraId, cameraDeviceStateCallback, backgroundHandler);
                return;
            }

        } catch (CameraAccessException e) {
            Log.e(this.getClass().getSimpleName(), "OpenCamera - Camera Access Exception", e);
        } catch (IllegalArgumentException e) {
            Log.e(this.getClass().getSimpleName(), "OpenCamera - Illegal Argument Exception", e);
        } catch (SecurityException e) {
            Log.e(this.getClass().getSimpleName(), "OpenCamera - Security Exception", e);
        }
    }


    public void start(int surfaceWidth, int surfaceHeight) {
        Log.d(this.getClass().getSimpleName(), "start");
        createCameraPreviewSession(surfaceWidth, surfaceHeight);
    }

    public void close() {
        Log.d(this.getClass().getSimpleName(), "close camera");
        try {
            CameraDevice c = cameraDevice;
            cameraDevice = null;
            if (null != cameraCaptureSession) {
                cameraCaptureSession.close();
                cameraCaptureSession = null;
            }
            if (null != c) {
                c.close();
            }
        } finally {
            stopBackgroundThread();
            if (null != previewImageReader) {
                previewImageReader.close();
                previewImageReader = null;
            }
        }
        Log.d(this.getClass().getSimpleName(), "camera closed");
    }

    private void startBackgroundThread() {
        stopBackgroundThread();
        backgroundThread = new HandlerThread("Camera");
        backgroundThread.start();
        backgroundHandler = new Handler(backgroundThread.getLooper());
    }

    private void stopBackgroundThread() {
        if (backgroundThread == null)
            return;
        backgroundThread.quitSafely();
        try {
            backgroundThread.join();
            backgroundThread = null;
            backgroundHandler = null;
        } catch (InterruptedException e) {
            Log.e(this.getClass().getSimpleName(), "stopBackgroundThread", e);
        }
    }


    private void createCameraPreviewSession(int surfaceWidth, int surfaceHeight) {
        StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
        List<Size> sizes = Arrays.asList(Objects.requireNonNull(map).getOutputSizes(ImageReader.class));
        Size previewSize = calculateCameraFrameSize(sizes, surfaceWidth, surfaceHeight);
        Log.d(this.getClass().getSimpleName(), " Using preview size = " + previewSize);

        final int width = previewSize.getWidth();
        final int height = previewSize.getHeight();


        try {
            if (cameraDevice == null) {
                Log.e(this.getClass().getSimpleName(),
                        "createCameraPreviewSession: camera isn't opened");
                return;
            }
            if (null != cameraCaptureSession) {
                Log.e(this.getClass().getSimpleName(),
                        "createCameraPreviewSession: mCaptureSession is already started");
                return;
            }

            previewImageReader = ImageReader.newInstance(width, height, PREVIEW_FORMAT, 2);
            previewImageReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
                @Override
                public void onImageAvailable(ImageReader reader) {
                    Image image = reader.acquireLatestImage();
                    if (image == null)
                        return;

                    try (CameraFrame frame = new RGBACameraFrame(image)) {
                        previewFrameListener.onPreviewFrame(frame);
                    }

                }
            }, backgroundHandler);
            Surface surface = previewImageReader.getSurface();

            previewRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            previewRequestBuilder.addTarget(surface);

            cameraDevice.createCaptureSession(Arrays.asList(surface, stillImageReader.getSurface()),
                    new CameraCaptureStateCallback(this), null
            );

        } catch (CameraAccessException e) {
            Log.e(this.getClass().getSimpleName(), "createCameraPreviewSession", e);
        }
    }

    private Size calculateCameraFrameSize(List<Size> supportedSizes, int surfaceWidth, int surfaceHeight) {
        int calcWidth = 0;
        int calcHeight = 0;

        int maxWidth = maxPreviewSize.getWidth();
        int maxHeight = maxPreviewSize.getHeight();

        int maxAllowedWidth = (maxWidth != MAX_UNSPECIFIED && maxWidth < surfaceWidth) ? maxWidth : surfaceWidth;
        int maxAllowedHeight = (maxHeight != MAX_UNSPECIFIED && maxHeight < surfaceHeight) ? maxHeight : surfaceHeight;

        for (Size size : supportedSizes) {
            int width = size.getWidth();
            int height = size.getHeight();
            Log.d(this.getClass().getSimpleName(), "trying size: " + width + "x" + height);

            if (width <= maxAllowedWidth && height <= maxAllowedHeight) {
                if (width >= calcWidth && height >= calcHeight) {
                    calcWidth = width;
                    calcHeight = height;
                }
            }
        }
        if ((calcWidth == 0 || calcHeight == 0) && supportedSizes.size() > 0) {
            Size size = supportedSizes.get(0);
            calcWidth = size.getWidth();
            calcHeight = size.getHeight();
        }

        return new Size(calcWidth, calcHeight);
    }


    void lockFocus() {
        Log.d(this.getClass().getSimpleName(), "lockFocus");
        try {
            previewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER,
                    CameraMetadata.CONTROL_AF_TRIGGER_START);
            state = CameraState.STATE_WAITING_LOCK;
            cameraCaptureSession.capture(previewRequestBuilder.build(), cameraCaptureCaptureCallback,
                    backgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    void runPrecaptureSequence() {
        Log.d(this.getClass().getSimpleName(), "runPrecaptureSequence");
        try {
            // This is how to tell the camera to trigger.
            previewRequestBuilder.set(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER,
                    CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER_START);
            state = CameraState.STATE_WAITING_PRECAPTURE;
            cameraCaptureSession.capture(previewRequestBuilder.build(), cameraCaptureCaptureCallback,
                    backgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    void unlockFocus() {
        Log.d(this.getClass().getSimpleName(), "unlockFocus");
        try {
            // Reset the auto-focus trigger
            previewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER,
                    CameraMetadata.CONTROL_AF_TRIGGER_CANCEL);
            cameraCaptureSession.capture(previewRequestBuilder.build(), cameraCaptureCaptureCallback,
                    backgroundHandler);
            // After this, the camera will go back to the normal state of preview.
            state = CameraState.STATE_PREVIEW;
            cameraCaptureSession.setRepeatingRequest(previewRequest, cameraCaptureCaptureCallback,
                    backgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    void captureStillPicture() {
        Log.d(this.getClass().getSimpleName(), "captureStillPicture");
        try {
            if (cameraDevice == null) {
                return;
            }
            // This is the CaptureRequest.Builder that we use to take a picture.
            final CaptureRequest.Builder captureBuilder =
                    cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureBuilder.addTarget(stillImageReader.getSurface());

            // Use the same AE and AF modes as the preview.
            captureBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                    CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);

            // Orientation
            captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, getOrientation(screenOrientation));

            CameraCaptureSession.CaptureCallback captureCallback
                    = new CameraCaptureSession.CaptureCallback() {

                @Override
                public void onCaptureCompleted(@NotNull CameraCaptureSession session,
                                               @NotNull CaptureRequest request,
                                               @NotNull TotalCaptureResult result) {
                    Log.d(this.getClass().getSimpleName(), file.toString());
                    unlockFocus();
                }
            };

            cameraCaptureSession.stopRepeating();
            cameraCaptureSession.abortCaptures();
            cameraCaptureSession.capture(captureBuilder.build(), captureCallback, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private int getOrientation(int rotation) {
        // Sensor orientation is 90 for most devices, or 270 for some devices (eg. Nexus 5X)
        // We have to take that into account and rotate JPEG properly.
        // For devices with orientation of 90, we simply return our mapping from ORIENTATIONS.
        // For devices with orientation of 270, we need to rotate the JPEG 180 degrees.
        return (ORIENTATIONS.get(rotation) + sensorOrientation + 270) % 360;
    }

    static class CompareSizesByArea implements Comparator<Size> {

        @Override
        public int compare(Size lhs, Size rhs) {
            // We cast here to ensure the multiplications won't overflow
            return Long.signum((long) lhs.getWidth() * lhs.getHeight() -
                    (long) rhs.getWidth() * rhs.getHeight());
        }
    }

    public interface PreviewFrameListener {
        void onPreviewFrame(CameraFrame frame);
    }

    public interface StillImageListener {
        void onStillImage();
    }

}
