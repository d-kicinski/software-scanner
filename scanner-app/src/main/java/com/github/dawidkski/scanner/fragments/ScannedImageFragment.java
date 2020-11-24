package com.github.dawidkski.scanner.fragments;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.RequiresApi;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.github.dawidkski.scanner.R;
import com.github.dawidkski.scanner.jni.Scanner;

import org.jetbrains.annotations.NotNull;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Objects;

import static android.provider.MediaStore.Images.Media.TITLE;
import static android.provider.MediaStore.Images.Media.DISPLAY_NAME;
import static android.provider.MediaStore.Images.Media.DESCRIPTION;
import static android.provider.MediaStore.Images.Media.MIME_TYPE;
import static android.provider.MediaStore.Images.Media.IS_PENDING;
import static android.provider.MediaStore.Images.Media.DATE_ADDED;
import static android.provider.MediaStore.Images.Media.DATE_TAKEN;

public class ScannedImageFragment extends Fragment {

    private static final String ARG_IMAGE_PATH = "imagePath";

    private String imagePath;
    private Mat rawImage;
    private Mat scannedImage;
    private Bitmap scannedBitmap;
    private ImageView imageView;

    public ScannedImageFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            imagePath = getArguments().getString(ARG_IMAGE_PATH);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.scanned_image_fragment, container, false);
    }

    @Override
    public void onViewCreated(@NotNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        imageView = view.findViewById(R.id.scanned_image_view);
        view.findViewById(R.id.save_button).setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.Q)
            @Override
            public void onClick(View v) {
                onSaveButton();
            }
        });
        view.findViewById(R.id.close_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onCloseButton();
            }
        });

        rawImage = loadImage(imagePath);
        scannedImage = scanImage(rawImage);
        displayImage(scannedImage);
    }

    private Mat scanImage(Mat image) {
        Mat scan = new Mat();
        Scanner.softwareScanner(image.getNativeObjAddr(), scan.getNativeObjAddr());
        return scan;
    }

    private Mat loadImage(String imagePath) {
        Mat rgbImage = new Mat();
        Imgproc.cvtColor(Imgcodecs.imread(imagePath), rgbImage, Imgproc.COLOR_BGR2RGB);
        return rgbImage;
    }

    private void displayImage(Mat image) {
        scannedBitmap = Bitmap.createBitmap(image.cols(), image.rows(), Bitmap.Config.RGB_565);
        Utils.matToBitmap(image, scannedBitmap);
        imageView.setImageBitmap(scannedBitmap);
    }

    private void onCloseButton() {
        releaseAll();
        Navigation.findNavController(requireActivity(), R.id.fragment_container).navigate(
                ScannedImageFragmentDirections.actionScannedImageViewToCameraFragment()
                        .setIsScanAccepted(false));
    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    private void onSaveButton() {
        releaseAll();
        saveImage(scannedBitmap);
        Navigation.findNavController(requireActivity(), R.id.fragment_container).navigate(
                ScannedImageFragmentDirections.actionScannedImageViewToCameraFragment()
                        .setIsScanAccepted(true));
    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    private void saveImage(Bitmap bitmap) {
        ContentValues values = new ContentValues();
        values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES);
        values.put(TITLE, "title");
        values.put(DISPLAY_NAME, "display name");
        values.put(DESCRIPTION, "description");
        values.put(MIME_TYPE, "image/jpeg");
        values.put(IS_PENDING, 1);
        values.put(DATE_ADDED, System.currentTimeMillis());
        values.put(DATE_TAKEN, System.currentTimeMillis());

        ContentResolver resolver = requireContext().getContentResolver();

        Uri url = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

        try (OutputStream outputStream = resolver.openOutputStream(Objects.requireNonNull(url))) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void releaseAll() {
        rawImage.release();
        scannedImage.release();
    }

}