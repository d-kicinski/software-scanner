package com.github.dawidkski.scanner.fragments;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.github.dawidkski.scanner.R;
import com.github.dawidkski.scanner.camera.CameraBridgeViewBase;
import com.github.dawidkski.scanner.camera.CvCameraViewFrame;
import com.github.dawidkski.scanner.camera.CvCameraViewListener;
import com.github.dawidkski.scanner.camera.JavaCamera2View;
import com.github.dawidkski.scanner.jni.Scanner;

import org.jetbrains.annotations.NotNull;
import org.opencv.core.CvType;
import org.opencv.core.Mat;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;

public class CameraFragment extends Fragment implements CvCameraViewListener {

    private static final int FRAME_PROCESS_DELAY = 500;

    private boolean mIsFrameProcessingEnabled = false;
    private Mat mCurrentFrame;
    private JavaCamera2View mOpenCvCameraView;
    private File mFile;
    private SwitchCompat mHintSwitch;
    private NavController mNavController;

    public CameraFragment() {
        Log.d(this.getClass().getSimpleName(), "Instantiated new " + this.getClass());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.camera_fragment, container, false);
    }

    @Override
    public void onViewCreated(@NotNull final View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mOpenCvCameraView = view.findViewById(R.id.java_camera2_view);
        view.findViewById(R.id.capture_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                v.setEnabled(true);
                Log.d(this.getClass().getSimpleName(), "Requesting image capture from UI.");
                mFile = createFile(requireContext());
                mOpenCvCameraView.takePicture(mFile);
            }
        });
        mHintSwitch = view.findViewById(R.id.hint_switch);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mNavController = Navigation.findNavController(requireActivity(), R.id.fragment_container);
        mOpenCvCameraView.setCameraPermissionGranted();
        mOpenCvCameraView.setVisibility(CameraBridgeViewBase.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);

        // Let's wait some time before starting analyzing each frame with native code
        final Handler handler = new Handler(Objects.requireNonNull(Looper.myLooper()));
        handler.postDelayed(new Runnable() {
            public void run() {
                mIsFrameProcessingEnabled = true;
            }
        }, FRAME_PROCESS_DELAY);
    }

    @Override
    public void onResume() {
        super.onResume();
        System.loadLibrary("opencv_java4");
        System.loadLibrary("jniscanner");
        mOpenCvCameraView.enableView();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        mCurrentFrame = new Mat(height, width, CvType.CV_8UC4);
    }

    @Override
    public void onCameraViewStopped() {
        mCurrentFrame.release();
    }

    @Override
    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
        mCurrentFrame = inputFrame.rgba();
        if (mHintSwitch.isChecked() && mIsFrameProcessingEnabled) {
            Scanner.drawContour(mCurrentFrame.getNativeObjAddr());
        }
        return mCurrentFrame;
    }

    @Override
    public void onPictureTaken() {
        mNavController.navigate(CameraFragmentDirections.actionCameraToJpegViewer(mFile.getAbsolutePath()));
    }

    private File createFile(Context context) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss_SSS", Locale.ENGLISH);
        return new File(context.getFilesDir(), "IMG_" + sdf.format(new Date()) + "." + "jpg");
    }

}
