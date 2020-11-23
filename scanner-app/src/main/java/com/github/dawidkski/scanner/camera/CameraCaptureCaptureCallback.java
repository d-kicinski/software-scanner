package com.github.dawidkski.scanner.camera;

import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.os.Build;
import android.util.Log;

import androidx.annotation.RequiresApi;

import org.jetbrains.annotations.NotNull;

class CameraCaptureCaptureCallback extends CameraCaptureSession.CaptureCallback {

    private final Camera camera;

    public CameraCaptureCaptureCallback(Camera camera) {
        this.camera = camera;
    }


    @RequiresApi(api = Build.VERSION_CODES.R)
    @Override
    public void onCaptureProgressed(@NotNull CameraCaptureSession session,
                                    @NotNull CaptureRequest request,
                                    @NotNull CaptureResult partialResult) {
        process(partialResult);
    }

    @RequiresApi(api = Build.VERSION_CODES.R)
    @Override
    public void onCaptureCompleted(@NotNull CameraCaptureSession session,
                                   @NotNull CaptureRequest request,
                                   @NotNull TotalCaptureResult result) {
        process(result);
    }

    @RequiresApi(api = Build.VERSION_CODES.R)
    private void process(CaptureResult result) {
        switch (camera.state) {
            case STATE_PREVIEW: {
                // We have nothing to do when the camera preview is working normally.
                break;
            }
            case STATE_WAITING_LOCK: {
                Integer afState = result.get(CaptureResult.CONTROL_AF_STATE);
                Log.d(this.getClass().getSimpleName(), "STATE_WAITING_LOCK, afState = " + afState);
                if (afState == null) {
                    camera.captureStillPicture();
                } else if (CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED == afState ||
                        CaptureResult.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED == afState) {
                    Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
                    Log.d(this.getClass().getSimpleName(), "STATE_WAITING_LOCK, aeState = " + aeState);
                    if (aeState == null ||
                            aeState == CaptureResult.CONTROL_AE_STATE_CONVERGED) {
                        camera.state = CameraState.STATE_PICTURE_TAKEN;
                        camera.captureStillPicture();
                    } else {
                        camera.runPrecaptureSequence();
                    }
                }
                break;
            }
            case STATE_WAITING_PRECAPTURE: {
                Log.d(this.getClass().getSimpleName(), "STATE_WAITING_PRECAPTURE");
                // CONTROL_AE_STATE can be null on some devices
                Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
                if (aeState == null ||
                        aeState == CaptureResult.CONTROL_AE_STATE_PRECAPTURE ||
                        aeState == CaptureRequest.CONTROL_AE_STATE_FLASH_REQUIRED) {
                    camera.state = CameraState.STATE_WAITING_NON_PRECAPTURE;
                }
                break;
            }
            case STATE_WAITING_NON_PRECAPTURE: {
                Log.d(this.getClass().getSimpleName(), "STATE_WAITING_NON_PRECAPTURE");
                // CONTROL_AE_STATE can be null on some devices
                Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
                if (aeState == null || aeState != CaptureResult.CONTROL_AE_STATE_PRECAPTURE) {
                    camera.state = CameraState.STATE_PICTURE_TAKEN;
                    camera.captureStillPicture();
                }
                break;
            }
        }
    }

}
