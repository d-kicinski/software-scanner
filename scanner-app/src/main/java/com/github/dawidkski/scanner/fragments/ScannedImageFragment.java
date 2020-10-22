package com.github.dawidkski.scanner.fragments;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

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

public class ScannedImageFragment extends Fragment {

    private static final String ARG_IMAGE_PATH = "imagePath";

    private String mImagePath;
    private Mat mRawImage;
    private Mat mScannedImage;
    private Bitmap mScannedBitmap;
    private ImageView mImageView;

    public ScannedImageFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mImagePath = getArguments().getString(ARG_IMAGE_PATH);
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
        mImageView = view.findViewById(R.id.scanned_image_view);
        view.findViewById(R.id.save_button).setOnClickListener(new View.OnClickListener() {
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

        mRawImage = loadImage(mImagePath);
        mScannedImage = scanImage(mRawImage);
        displayImage(mScannedImage);
    }

    private Mat scanImage(Mat image) {
        Mat scannedImage = new Mat();
        Scanner.softwareScanner(image.getNativeObjAddr(), scannedImage.getNativeObjAddr());
        return scannedImage;
    }

    private Mat loadImage(String imagePath) {
        Mat rgbImage = new Mat();
        Imgproc.cvtColor(Imgcodecs.imread(imagePath), rgbImage, Imgproc.COLOR_BGR2RGB);
        return rgbImage;
    }

    private void displayImage(Mat image) {
        mScannedBitmap = Bitmap.createBitmap(image.cols(), image.rows(), Bitmap.Config.RGB_565);
        Utils.matToBitmap(image, mScannedBitmap);
        mImageView.setImageBitmap(mScannedBitmap);
    }

    private void onCloseButton() {
        releaseAll();
        Navigation.findNavController(requireActivity(), R.id.fragment_container).navigate(
                ScannedImageFragmentDirections.actionScannedImageViewToCameraFragment()
                        .setIsScanAccepted(false));
    }

    private void onSaveButton() {
        releaseAll();
        saveImage(mScannedBitmap);
        Navigation.findNavController(requireActivity(), R.id.fragment_container).navigate(
                ScannedImageFragmentDirections.actionScannedImageViewToCameraFragment()
                        .setIsScanAccepted(true));
    }

    private void saveImage(Bitmap bitmap) {
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.TITLE, "title");
        values.put(MediaStore.Images.Media.DISPLAY_NAME, "display name");
        values.put(MediaStore.Images.Media.DESCRIPTION, "description");
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
        values.put(MediaStore.Images.Media.IS_PENDING, 1);
        values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES);
        values.put(MediaStore.Images.Media.DATE_ADDED, System.currentTimeMillis());
        values.put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis());

        ContentResolver resolver = requireContext().getContentResolver();

        Uri url = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

        try (OutputStream outputStream = resolver.openOutputStream(Objects.requireNonNull(url))) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void releaseAll() {
        mRawImage.release();
        mScannedImage.release();
    }

}