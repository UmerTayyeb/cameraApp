package com.firstapp.cameraapp;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.nfc.Tag;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.util.Log;
import androidx.appcompat.app.AppCompatActivity;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.widget.TextView;

import com.googlecode.tesseract.android.TessBaseAPI;


public class MainActivity extends AppCompatActivity {
    private TessBaseAPI tessBaseAPI;

    private static final int CAMERA_PERMISSION_REQUEST_CODE = 100;
    private static final int CAMERA_CAPTURE_REQUEST_CODE = 200;
    private static final String TAG = "app";

    private ImageView imageView;
    private TextView textView;
    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        copyAssets("eng.traineddata");
        imageView = findViewById(R.id.imageView);
        textView = findViewById(R.id.textView);
        Button btnCapture = findViewById(R.id.btnCapture);
        tessBaseAPI = new TessBaseAPI();
        tessBaseAPI.init(getFilesDir().getAbsolutePath(), "eng"); // Initialize Tesseract with the "eng" language data



        btnCapture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                requestCameraPermission();
            }
        });
    }

    private void copyAssets(String assetFileName) {
        AssetManager assetManager = getAssets();
        InputStream inStream = null;
        OutputStream outStream = null;

        try {
            inStream = assetManager.open(assetFileName);
            File tessdataFolder = new File(getFilesDir(), "tessdata"); // Create "tessdata" subfolder
            if (!tessdataFolder.exists()) {
                tessdataFolder.mkdirs(); // Create the subfolder if it doesn't exist
            }

            File outFile = new File(tessdataFolder, assetFileName);
            outStream = new FileOutputStream(outFile);

            byte[] buffer = new byte[1024];
            int read;
            while ((read = inStream.read(buffer)) != -1) {
                outStream.write(buffer, 0, read);
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (inStream != null) inStream.close();
                if (outStream != null) outStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }



    private void requestCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    CAMERA_PERMISSION_REQUEST_CODE);
        } else {
            capturePicture();
        }
    }

    private void capturePicture() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(intent, CAMERA_CAPTURE_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                capturePicture();
            } else {
                Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CAMERA_CAPTURE_REQUEST_CODE && resultCode == RESULT_OK) {
            Bitmap photo = (Bitmap) data.getExtras().get("data");
            if (photo != null) {
                imageView.setImageBitmap(photo);
                String extractedText = extractTextFromBitmap(photo);
                Log.d(TAG,extractedText);
                textView.setText(extractedText);
                // Toast.makeText(this,  extractedText, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Failed to capture picture", Toast.LENGTH_SHORT).show();
            }

        }
    }
    private String extractTextFromBitmap(Bitmap bitmap) {
        tessBaseAPI.setImage(bitmap);
        return tessBaseAPI.getUTF8Text();
    }
}
