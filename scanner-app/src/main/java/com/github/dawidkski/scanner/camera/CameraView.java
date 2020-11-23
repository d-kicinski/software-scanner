package com.github.dawidkski.scanner.camera;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.github.dawidkski.scanner.camera.frame.CameraFrame;

import org.opencv.android.FpsMeter;
import org.opencv.android.Utils;
import org.opencv.core.Mat;

public class CameraView extends SurfaceView
        implements SurfaceHolder.Callback, Camera.PreviewFrameListener {

    private final Matrix matrix = new Matrix();
    private Bitmap cacheBitmap;
    private CameraViewListener listener;
    private SurfaceListener surfaceListener;
    private FpsMeter fpsMeter = null;

    public CameraView(Context context, AttributeSet attrs) {
        super(context, attrs);

        getHolder().addCallback(this);
    }

    @Override
    public void surfaceChanged(SurfaceHolder arg0, int arg1, int arg2, int arg3) {
        Log.d(this.getClass().getSimpleName(), "surfaceChanged");
        surfaceListener.surfaceChanged();
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        Log.d(this.getClass().getSimpleName(), "surfaceCreated");
        /* Do nothing. Wait until surfaceChanged delivered */
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.d(this.getClass().getSimpleName(), "surfaceDestroyed");
        surfaceListener.surfaceDestroyed();
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

    @Override
    public void onPreviewFrame(CameraFrame frame) {
        deliverAndDrawFrame(frame);
    }

    public void setSurfaceListener(SurfaceListener surfaceListener) {
        this.surfaceListener = surfaceListener;
    }


    public void enableFpsMeter() {
        if (fpsMeter == null) {
            fpsMeter = new FpsMeter();
            fpsMeter.setResolution(getWidth(), getHeight());
        }
    }

    public void disableFpsMeter() {
        fpsMeter = null;
    }

    public void setListener(CameraViewListener listener) {
        this.listener = listener;
    }

    private void updateMatrix() {
        matrix.reset();
        matrix.preTranslate(this.getWidth(), 0);
        matrix.preRotate(90);
    }

    private void deliverAndDrawFrame(CameraFrame frame) {
        Mat modified;
        if (listener != null) {
            modified = listener.onCameraFrame(frame);
        } else {
            modified = frame.get();
        }

        if (cacheBitmap == null) {
            cacheBitmap = Bitmap.createBitmap(modified.width(), modified.height(), Bitmap.Config.ARGB_8888);
        }

        try {
            Utils.matToBitmap(modified, cacheBitmap);
        } catch (Exception e) {
            Log.e(this.getClass().getSimpleName(), "Utils.matToBitmap() throws an exception: " + e.getMessage());
        }

        Canvas canvas = getHolder().lockHardwareCanvas();
        int saveCount = canvas.save();
        canvas.drawBitmap(cacheBitmap, matrix, null);

        //Restore canvas after draw bitmap
        canvas.restoreToCount(saveCount);

        if (fpsMeter != null) {
            fpsMeter.measure();
            fpsMeter.draw(canvas, 20, 30);
        }
        getHolder().unlockCanvasAndPost(canvas);
    }

    interface SurfaceListener {

        void surfaceChanged();

        void surfaceDestroyed();

    }

}
