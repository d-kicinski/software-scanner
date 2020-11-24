package com.github.dawidkski.scanner.camera;

import android.hardware.camera2.CameraDevice;


import org.jetbrains.annotations.NotNull;

class CameraDeviceStateCallback extends CameraDevice.StateCallback {

    private final Camera camera;

    public CameraDeviceStateCallback(Camera camera) {
        this.camera = camera;
    }

    @Override
    public void onOpened(@NotNull CameraDevice cameraDevice) {
        camera.cameraDevice = cameraDevice;
    }

    @Override
    public void onDisconnected(CameraDevice cameraDevice) {
        cameraDevice.close();
        camera.cameraDevice = null;
    }

    @Override
    public void onError(CameraDevice cameraDevice, int error) {
        cameraDevice.close();
        camera.cameraDevice = null;
    }

}
