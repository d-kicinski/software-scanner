package com.github.dawidkski.scanner.camera;


public class CameraViewController
        implements CameraView.SurfaceListener, Camera.StillImageListener {

    private static final int STOPPED = 0;
    private static final int STARTED = 1;

    private final Object syncObject = new Object();
    private final CameraView cameraView;
    private final Camera camera;

    private int currentState = STOPPED;
    private boolean isCameraPermissionGranted = false;
    private boolean isEnabled;
    private boolean surfaceExist;
    private CameraViewListener listener;


    public CameraViewController(CameraView cameraView, Camera camera) {
        cameraView.setSurfaceListener(this);
        camera.setStillImageListener(this);
        camera.setPreviewFrameListener(cameraView);

        this.cameraView = cameraView;
        this.camera = camera;

    }

    public void setListener(CameraViewListener listener) {
        this.listener = listener;
        cameraView.setListener(listener);
    }

    public void setCameraPermissionGranted() {
        synchronized (syncObject) {
            isCameraPermissionGranted = true;
            checkCurrentState();
        }
    }

    public void enableView() {
        synchronized (syncObject) {
            isEnabled = true;
            checkCurrentState();
        }
    }

    public void disableView() {
        synchronized (syncObject) {
            isEnabled = false;
            checkCurrentState();
        }
    }

    private void checkCurrentState() {
        int targetState;

        if (isEnabled && isCameraPermissionGranted && surfaceExist) {
            targetState = STARTED;
        } else {
            targetState = STOPPED;
        }

        if (targetState != currentState) {
            /* The state change detected. Need to exit the current state and enter target state */
            processExitState(currentState);
            currentState = targetState;
            processEnterState(currentState);
        }
    }

    private void processEnterState(int state) {
        if (state == STARTED) {
            onEnterStartedState();
        }
    }

    private void processExitState(int state) {
        if (state == STARTED) {
            onExitStartedState();
        }
    }

    private void onEnterStartedState() {
        camera.start(cameraView.getHeight(), cameraView.getWidth());
    }

    private void onExitStartedState() {
        camera.close();
    }

    @Override
    public void surfaceChanged() {
        synchronized (syncObject) {
            if (surfaceExist) {
                // Surface changed. We need to stop camera and restart with new parameters
                surfaceExist = false;
                checkCurrentState();
            }
            surfaceExist = true;
            checkCurrentState();
        }
    }

    @Override
    public void surfaceDestroyed() {
        synchronized (syncObject) {
            surfaceExist = false;
            checkCurrentState();
        }
    }

    @Override
    public void onStillImage() {
        listener.onPictureTaken();
    }

}
