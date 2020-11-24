package com.github.dawidkski.scanner.camera;

import android.hardware.camera2.CameraCaptureSession;
import android.util.Log;


import org.jetbrains.annotations.NotNull;

class CameraCaptureStateCallback extends CameraCaptureSession.StateCallback {

    private final Camera camera;

    public CameraCaptureStateCallback(Camera camera) {
        this.camera = camera;
    }

    @Override
    public void onConfigured(@NotNull CameraCaptureSession cameraCaptureSession) {
        Log.d(this.getClass().getSimpleName(), "onConfigured");
        if (camera.cameraDevice == null) {
            return; // camera is already closed
        }
        camera.cameraCaptureSession = cameraCaptureSession;
        try {
            camera.previewRequest = camera.previewRequestBuilder.build();
            camera.cameraCaptureSession.setRepeatingRequest(camera.previewRequest, null,
                    camera.backgroundHandler);
        } catch (Exception e) {
            Log.e(this.getClass().getSimpleName(), "onConfigured failed", e);
        }
    }

    @Override
    public void onConfigureFailed(@NotNull CameraCaptureSession cameraCaptureSession) {
        Log.e(this.getClass().getSimpleName(), "onConfigureFailed");
    }

}
