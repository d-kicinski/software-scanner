package com.github.dawidkski.scanner.camera;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Size;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import org.opencv.android.FpsMeter;
import org.opencv.android.Utils;
import org.opencv.core.Mat;

import java.util.List;

/**
 * This is a basic class, implementing the interaction with Camera and OpenCV library.
 * The main responsibility of it - is to control when camera can be enabled, process the frame,
 * call external listener to make any adjustments to the frame and then draw the resulting
 * frame to the screen.
 * The clients shall implement CvCameraViewListener.
 */
public abstract class CameraBridgeViewBase extends SurfaceView implements SurfaceHolder.Callback {

    private static final String TAG = "CameraBridge";
    protected static final int MAX_UNSPECIFIED = -1;
    private static final int STOPPED = 0;
    private static final int STARTED = 1;

    private final Object mSyncObject = new Object();
    private final Matrix mMatrix = new Matrix();
    private int mState = STOPPED;
    private Bitmap mCacheBitmap;
    private boolean mSurfaceExist;

    protected CameraIndex mCameraIndex = CameraIndex.BACK;
    protected boolean mCameraPermissionGranted = false;
    protected FpsMeter mFpsMeter = null;
    protected CvCameraViewListener mListener;
    protected int mFrameWidth;
    protected int mFrameHeight;
    protected int mMaxHeight;
    protected int mMaxWidth;
    protected float mScale = 1;
    protected boolean mEnabled;

    public CameraBridgeViewBase(Context context, AttributeSet attrs) {
        super(context, attrs);

        int count = attrs.getAttributeCount();
        Log.d(TAG, "Attr count: " + count);

        getHolder().addCallback(this);
        mMaxWidth = MAX_UNSPECIFIED;
        mMaxHeight = MAX_UNSPECIFIED;
    }

    /**
     * Sets the camera index
     * @param cameraIndex new camera index
     */
    public void setCameraIndex(CameraIndex cameraIndex) {
        this.mCameraIndex = cameraIndex;
    }


    public void surfaceChanged(SurfaceHolder arg0, int arg1, int arg2, int arg3) {
        Log.d(TAG, "call surfaceChanged event");
        synchronized(mSyncObject) {
            if (!mSurfaceExist) {
                mSurfaceExist = true;
                checkCurrentState();
            } else {
                /** Surface changed. We need to stop camera and restart with new parameters */
                /* Pretend that old surface has been destroyed */
                mSurfaceExist = false;
                checkCurrentState();
                /* Now use new surface. Say we have it now */
                mSurfaceExist = true;
                checkCurrentState();
            }
        }
    }

    public void surfaceCreated(SurfaceHolder holder) {
        /* Do nothing. Wait until surfaceChanged delivered */
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        synchronized(mSyncObject) {
            mSurfaceExist = false;
            checkCurrentState();
        }
    }


    /**
     * This method is provided for clients, so they can signal camera permission has been granted.
     * The actual onCameraViewStarted callback will be delivered only after setCameraPermissionGranted
     * and enableView have been called and surface is available
     */
    public void setCameraPermissionGranted() {
        synchronized(mSyncObject) {
            mCameraPermissionGranted = true;
            checkCurrentState();
        }
    }


    /**
     * This method is provided for clients, so they can enable the camera connection.
     * The actual onCameraViewStarted callback will be delivered only after setCameraPermissionGranted
     * and enableView have been called and surface is available
     */
    public void enableView() {
        synchronized(mSyncObject) {
            mEnabled = true;
            checkCurrentState();
        }
    }

    /**
     * This method is provided for clients, so they can disable camera connection and stop
     * the delivery of frames even though the surface view itself is not destroyed and still stays on the screen
     */
    public void disableView() {
        synchronized(mSyncObject) {
            mEnabled = false;
            checkCurrentState();
        }
    }

    /**
     * This method enables label with fps value on the screen
     */
    public void enableFpsMeter() {
        if (mFpsMeter == null) {
            mFpsMeter = new FpsMeter();
            mFpsMeter.setResolution(mFrameWidth, mFrameHeight);
        }
    }

    public void disableFpsMeter() {
            mFpsMeter = null;
    }

    public void setCvCameraViewListener(CvCameraViewListener listener) {
        mListener = listener;
    }

    /**
     * This method sets the maximum size that camera frame is allowed to be. When selecting
     * size - the biggest size which less or equal the size set will be selected.
     * As an example - we set setMaxFrameSize(200,200) and we have 176x152 and 320x240 sizes. The
     * preview frame will be selected with 176x152 size.
     * This method is useful when need to restrict the size of preview frame for some reason (for example for video recording)
     * @param maxWidth - the maximum width allowed for camera frame.
     * @param maxHeight - the maximum height allowed for camera frame
     */
    public void setMaxFrameSize(int maxWidth, int maxHeight) {
        mMaxWidth = maxWidth;
        mMaxHeight = maxHeight;
    }

    /**
     * Called when mSyncObject lock is held
     */
    private void checkCurrentState() {
        Log.d(TAG, "call checkCurrentState");
        int targetState;

        if (mEnabled && mCameraPermissionGranted && mSurfaceExist && getVisibility() == VISIBLE) {
            targetState = STARTED;
        } else {
            targetState = STOPPED;
        }

        if (targetState != mState) {
            /* The state change detected. Need to exit the current state and enter target state */
            processExitState(mState);
            mState = targetState;
            processEnterState(mState);
        }
    }

    private void processEnterState(int state) {
        Log.d(TAG, "call processEnterState: " + state);
        switch(state) {
        case STARTED:
            onEnterStartedState();
            if (mListener != null) {
                mListener.onCameraViewStarted(mFrameWidth, mFrameHeight);
            }
            break;
        case STOPPED:
            onEnterStoppedState();
            if (mListener != null) {
                mListener.onCameraViewStopped();
            }
            break;
        }
    }

    private void processExitState(int state) {
        Log.d(TAG, "call processExitState: " + state);
        switch(state) {
        case STARTED:
            onExitStartedState();
            break;
        case STOPPED:
            onExitStoppedState();
            break;
        }
    }

    private void onEnterStoppedState() {
        /* nothing to do */
    }

    private void onExitStoppedState() {
        /* nothing to do */
    }

    // NOTE: The order of bitmap constructor and camera connection is important for android 4.1.x
    // Bitmap must be constructed before surface
    private void onEnterStartedState() {
        Log.d(TAG, "call onEnterStartedState");
        /* Connect camera */
        if (!connectCamera(getWidth(), getHeight())) {
            AlertDialog ad = new AlertDialog.Builder(getContext()).create();
            ad.setCancelable(false); // This blocks the 'BACK' button
            ad.setMessage("It seems that you device does not support camera (or it is locked). Application will be closed.");
            ad.setButton(DialogInterface.BUTTON_NEUTRAL,  "OK", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                    ((Activity) getContext()).finish();
                }
            });
            ad.show();

        }
    }

    private void onExitStartedState() {
        disconnectCamera();
        if (mCacheBitmap != null) {
            mCacheBitmap.recycle();
        }
    }


    /**
     * This method is invoked shall perform concrete operation to initialize the camera.
     * CONTRACT: as a result of this method variables mFrameWidth and mFrameHeight MUST be
     * initialized with the size of the Camera frames that will be delivered to external processor.
     * @param width - the width of this SurfaceView
     * @param height - the height of this SurfaceView
     */
    protected abstract boolean connectCamera(int width, int height);

    /**
     * Disconnects and release the particular camera object being connected to this surface view.
     * Called when syncObject lock is held
     */
    protected abstract void disconnectCamera();

    // NOTE: On Android 4.1.x the function must be called before SurfaceTexture constructor!
    protected void allocateCache()
    {
        mCacheBitmap = Bitmap.createBitmap(mFrameWidth, mFrameHeight, Bitmap.Config.ARGB_8888);
    }

    /**
     * This helper method can be called by subclasses to select camera preview size.
     * It goes over the list of the supported preview sizes and selects the maximum one which
     * fits both values set via setMaxFrameSize() and surface frame allocated for this view
     * @param supportedSizes
     * @param surfaceWidth
     * @param surfaceHeight
     * @return optimal frame size
     */
    protected Size calculateCameraFrameSize(List<android.util.Size> supportedSizes, int surfaceWidth, int surfaceHeight) {
        int calcWidth = 0;
        int calcHeight = 0;

        int maxAllowedWidth = (mMaxWidth != MAX_UNSPECIFIED && mMaxWidth < surfaceWidth)? mMaxWidth : surfaceWidth;
        int maxAllowedHeight = (mMaxHeight != MAX_UNSPECIFIED && mMaxHeight < surfaceHeight)? mMaxHeight : surfaceHeight;

        for (android.util.Size size : supportedSizes) {
            int width = size.getWidth();
            int height = size.getHeight();
            Log.d(TAG, "trying size: " + width + "x" + height);

            if (width <= maxAllowedWidth && height <= maxAllowedHeight) {
                if (width >= calcWidth && height >= calcHeight) {
                    calcWidth = width;
                    calcHeight = height;
                }
            }
        }
        if ((calcWidth == 0 || calcHeight == 0) && supportedSizes.size() > 0)
        {
            Log.i(TAG, "fallback to the first frame size");
            android.util.Size size = supportedSizes.get(0);
            calcWidth = size.getWidth();
            calcHeight = size.getHeight();
        }

        return new Size(calcWidth, calcHeight);
    }

    private void updateMatrix() {
        float mw = this.getWidth();
        float mh = this.getHeight();

        float hw = this.getWidth() / 2.0f;
        float hh = this.getHeight() / 2.0f;

        float cw  = (float) Resources.getSystem().getDisplayMetrics().widthPixels;
        float ch  = (float) Resources.getSystem().getDisplayMetrics().heightPixels;

        float scale = cw / (float)mh;
        float scale2 = ch / (float)mw;
        if(scale2 > scale){
            scale = scale2;
        }

        boolean isFrontCamera = mCameraIndex == CameraIndex.FRONT;

        mMatrix.reset();
        if (isFrontCamera) {
            mMatrix.preScale(-1, 1, hw, hh); // MH - this will mirror the camera
        }
        mMatrix.preTranslate(hw, hh);
        if (isFrontCamera){
            mMatrix.preRotate(270);
        } else {
            mMatrix.preRotate(90);
        }
        mMatrix.preTranslate(-hw, -hh);
        mMatrix.preScale(scale, scale, hw, hh);
    }

    @Override
    public void layout(int l, int t, int r, int b) {
        super.layout(l, t, r, b);
        updateMatrix();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        updateMatrix();
    }

    /**
     * This method shall be called by the subclasses when they have valid
     * object and want it to be delivered to external client (via callback) and
     * then displayed on the screen.
     * @param frame - the current frame to be delivered
     */
    protected void deliverAndDrawFrame(CvCameraViewFrame frame) { //replaces existing deliverAndDrawFrame
        Mat modified;

        if (mListener != null) {
            modified = mListener.onCameraFrame(frame);
        } else {
            modified = frame.rgba();
        }

        boolean bmpValid = true;
        if (modified != null) {
            try {
                Utils.matToBitmap(modified, mCacheBitmap);
            } catch(Exception e) {
                Log.e(TAG, "Mat type: " + modified);
                Log.e(TAG, "Bitmap type: " + mCacheBitmap.getWidth() + "*" + mCacheBitmap.getHeight());
                Log.e(TAG, "Utils.matToBitmap() throws an exception: " + e.getMessage());
                bmpValid = false;
            }
        }

        if (bmpValid && mCacheBitmap != null) {
            Canvas canvas = getHolder().lockCanvas();
            if (canvas != null) {
                canvas.drawColor(0, android.graphics.PorterDuff.Mode.CLEAR);
                int saveCount = canvas.save();
                canvas.setMatrix(mMatrix);

                canvas.drawBitmap(mCacheBitmap, new Rect(0,0,mCacheBitmap.getWidth(), mCacheBitmap.getHeight()),
                        new Rect((int)((canvas.getWidth() - mScale*mCacheBitmap.getWidth()) / 2),
                                (int)((canvas.getHeight() - mScale*mCacheBitmap.getHeight()) / 2),
                                (int)((canvas.getWidth() - mScale*mCacheBitmap.getWidth()) / 2 + mScale*mCacheBitmap.getWidth()),
                                (int)((canvas.getHeight() - mScale*mCacheBitmap.getHeight()) / 2 + mScale*mCacheBitmap.getHeight())), null);

                //Restore canvas after draw bitmap
                canvas.restoreToCount(saveCount);

                if (mFpsMeter != null) {
                    mFpsMeter.measure();
                    mFpsMeter.draw(canvas, 20, 30);
                }
                getHolder().unlockCanvasAndPost(canvas);
            }
        }
    }

}
