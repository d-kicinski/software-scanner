package com.github.dawidkski.scanner.fragments;

import android.content.Context;
import android.hardware.camera2.CameraManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import androidx.annotation.RequiresApi;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.github.dawidkski.scanner.R;
import com.github.dawidkski.scanner.camera.Camera;
import com.github.dawidkski.scanner.camera.CameraView;
import com.github.dawidkski.scanner.camera.CameraViewController;
import com.github.dawidkski.scanner.camera.CameraViewListener;
import com.github.dawidkski.scanner.camera.frame.CameraFrame;
import com.github.dawidkski.scanner.jni.Scanner;

import org.jetbrains.annotations.NotNull;
import org.opencv.core.Mat;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;

public class CameraFragment extends Fragment implements CameraViewListener {

    private static final int FRAME_PROCESS_DELAY = 500;

    private Camera camera;
    private CameraView cameraView;
    private CameraViewController cameraViewController;
    private CameraManager cameraManager;

    private ProgressBar progressBar;

    private File file;
    private SwitchCompat switchCompat;
    private NavController navController;
    private boolean isFrameProcessingEnabled = false;

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
        cameraView = view.findViewById(R.id.camera_view);
        progressBar = view.findViewById(R.id.progress_bar);
        camera = new Camera();
        cameraViewController = new CameraViewController(cameraView, camera);
        cameraManager = (CameraManager) requireContext().getSystemService(Context.CAMERA_SERVICE);

        view.findViewById(R.id.capture_button).setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.R)
            @Override
            public void onClick(View v) {
                v.setEnabled(true);
                Log.d(this.getClass().getSimpleName(), "Requesting image capture from UI.");
                file = createFile(requireContext());
                int rotation = requireContext().getDisplay().getRotation();
                camera.takePicture(file, rotation);
                progressBar.setVisibility(View.VISIBLE);
            }
        });
        switchCompat = view.findViewById(R.id.hint_switch);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        navController = Navigation.findNavController(requireActivity(), R.id.fragment_container);
        cameraViewController.setCameraPermissionGranted();
        cameraViewController.setListener(this);

        // Let's wait some time before starting analyzing each frame with native code
        final Handler handler = new Handler(Objects.requireNonNull(Looper.myLooper()));
        handler.postDelayed(new Runnable() {
            public void run() {
                isFrameProcessingEnabled = true;
            }
        }, FRAME_PROCESS_DELAY);
    }

    @Override
    public void onResume() {
        Log.d(this.getClass().getSimpleName(), "onResume");
        super.onResume();
        System.loadLibrary("opencv_java4");
        System.loadLibrary("jniscanner");

        camera.open(cameraManager);
        cameraViewController.enableView();
    }

    @Override
    public void onPause() {
        Log.d(this.getClass().getSimpleName(), "onPause");
        super.onPause();
        if (cameraView != null)
            cameraViewController.disableView();
    }

    @Override
    public void onDestroy() {
        Log.d(this.getClass().getSimpleName(), "onDestroy");
        super.onDestroy();
        if (cameraView != null)
            cameraViewController.disableView();
    }

    @Override
    public Mat onCameraFrame(CameraFrame inputFrame) {
        Mat frame = inputFrame.get();
        if (switchCompat.isChecked() && isFrameProcessingEnabled) {
            Scanner.drawContour(frame.getNativeObjAddr());
        }
        return frame;
    }

    @Override
    public void onPictureTaken() {
        navController.navigate(CameraFragmentDirections.actionCameraToJpegViewer(file.getAbsolutePath()));
        progressBar.setVisibility(View.GONE);
    }

    private File createFile(Context context) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss_SSS", Locale.ENGLISH);
        return new File(context.getFilesDir(), "IMG_" + sdf.format(new Date()) + "." + "jpg");
    }

}
