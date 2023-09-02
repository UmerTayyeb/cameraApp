package com.firstapp.cameraapp;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;
import android.content.res.AssetManager;
import android.util.Log;
import androidx.appcompat.app.AppCompatActivity;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.widget.EditText;

import com.googlecode.tesseract.android.TessBaseAPI;




public class MainActivity extends AppCompatActivity {
    private TessBaseAPI tessBaseAPI;
    private EditText editTextVariable;
    private EditText editTextEditedText;

    private String extractedText;
    private Button btnUpdateText;

    private static final int CAMERA_PERMISSION_REQUEST_CODE = 100;
    private static final int CAMERA_CAPTURE_REQUEST_CODE = 200;
    private static final String TAG = "app";

    private ImageView imageView;
    private static final int targetWidth = 600;
    private static final int targetHeight = 800;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getSupportActionBar().hide();
        setContentView(R.layout.activity_main);
        copyAssets("eng.traineddata");
        imageView = findViewById(R.id.imageView);
        editTextEditedText = findViewById(R.id.editTextEditedText); // Initialize editTextEditedText
        Button btnCapture = findViewById(R.id.btnCapture);
        tessBaseAPI = new TessBaseAPI();
        tessBaseAPI.init(getFilesDir().getAbsolutePath(), "eng"); // Initialize Tesseract with the "eng" language data
        btnUpdateText = findViewById(R.id.btnUpdateText);

        btnCapture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                requestCameraPermission();
            }
        });

        btnUpdateText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                // Update the extracted text with edited text
                String editedText = editTextEditedText.getText().toString();
                String extractedText = editedText;
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
                int imageWidth = photo.getWidth();
                int imageHeight = photo.getHeight();

                // Log the size
                Log.d(TAG, "Original:\nImage Width: " + imageWidth + " pixels");
                Log.d(TAG, "Image Height: " + imageHeight + " pixels");
                Bitmap resizedPhoto = resizeBitmap(photo, targetWidth, targetHeight);   //calling resizeBitmap func to resize photo and to convert into grayscale in same func

                // Enhance contrast
                Bitmap enhancedBitmap = enhanceContrast(resizedPhoto);

                imageView.setImageBitmap(enhancedBitmap);    // Image show

                imageWidth = enhancedBitmap.getWidth();      // print width and height of image
                imageHeight = enhancedBitmap.getHeight();
                Log.d(TAG, "Resized:\nImage Width: " + imageWidth + " pixels");
                Log.d(TAG, "Image Height: " + imageHeight + " pixels");
                final String[] extractedText = {extractTextFromBitmap(enhancedBitmap)};
                Log.d(TAG, extractedText[0]);
                //editTextVariable.setText(extractedText[0]);

                // Set the extracted text for editing
                EditText editTextEditedText = findViewById(R.id.editTextEditedText);
                editTextEditedText.setText(extractedText[0]);

                btnUpdateText.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        //String newText = editTextVariable.getText().toString();

                        // Update the extracted text with edited text
                        String editedText = editTextEditedText.getText().toString();
                        extractedText[0] = editedText;
                        Log.d(TAG, extractedText[0]);
                    }
                });
            } else {
                Toast.makeText(this, "Failed to capture picture", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private String extractTextFromBitmap(Bitmap bitmap) {
        tessBaseAPI.setImage(bitmap);
        return tessBaseAPI.getUTF8Text();
    }
    private Bitmap resizeBitmap(Bitmap originalBitmap, int newWidth, int newHeight) {
        int width = originalBitmap.getWidth();
        int height = originalBitmap.getHeight();

        // Calculate the scale factors for resizing
        float scaleWidth = ((float) newWidth) / width;
        float scaleHeight = ((float) newHeight) / height;

        // Create a matrix for the scaling operation
        android.graphics.Matrix matrix = new android.graphics.Matrix();

        // Resize the bitmap using the matrix
        matrix.postScale(scaleWidth, scaleHeight);
        Log.d(TAG, "Image resized! ");

        // Applying grayscale conversion
        Bitmap grayScaleImage = Bitmap.createBitmap(originalBitmap,0,0,width,height,matrix,false);

        //Color matrix for grayscale conversion
        ColorMatrix colorMatrix = new ColorMatrix();
        colorMatrix.setSaturation(0); // 0 means fully grayscale

        // Create a ColorMatrixColorFilter with the grayscale matrix
        ColorMatrixColorFilter filter = new ColorMatrixColorFilter(colorMatrix);

        //Paint object with grascale filter
        Paint paint = new Paint();
        paint.setColorFilter(filter);

        //new bitmap for grayscale image
        Bitmap grayscaleBitmap = Bitmap.createBitmap(grayScaleImage.getWidth(),grayScaleImage.getHeight(),Bitmap.Config.ARGB_8888);

        //new canvas for grayscale image
        Canvas canvas = new Canvas(grayscaleBitmap);
        canvas.drawBitmap(grayScaleImage,0,0,paint);
        Log.d(TAG, "GrayScale conversion completed!");

        // Create and return the resized bitmap
        return grayscaleBitmap; //Bitmap.createBitmap(originalBitmap, 0, 0, width, height, matrix, false); //grayscaleBitmap;
    }

    private Bitmap enhanceContrast(Bitmap bitmap) {
        // Create a new bitmap for the enhanced image
        Bitmap enhancedBitmap = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_8888);

        Canvas canvas = new Canvas(enhancedBitmap);
        Paint paint = new Paint();

        // Create a ColorMatrix to apply contrast adjustment
        ColorMatrix colorMatrix = new ColorMatrix();
        colorMatrix.setSaturation(0); // 0 means fully grayscale

        // Create a ColorMatrixColorFilter with the grayscale matrix
        ColorMatrixColorFilter filter = new ColorMatrixColorFilter(colorMatrix);
        paint.setColorFilter(filter);

        // Apply the ColorMatrixColorFilter to the canvas
        canvas.drawBitmap(bitmap, 0, 0, paint);

        Log.d(TAG, "Contrast enhancement completed!");

        return enhancedBitmap;
    }
}