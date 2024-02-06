package com.qrcodereader;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.common.StringUtils;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.google.zxing.qrcode.QRCodeReader;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

public class MainActivity extends AppCompatActivity {
    private Button scanBtn,chooseBtn,saveBtn;
    private static final int REQUEST_PICK_FILE = 1;
    private static final int WRITE_REQUEST_CODE = 2;
    private String config;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        scanBtn = findViewById(R.id.scanBtn);
        chooseBtn = findViewById(R.id.chooseBtn);
        saveBtn = findViewById(R.id.saveBtn);
        requestCameraPermission();
        scanBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                initQRCodeScanner();
            }
        });

        chooseBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                requestPickFilePermission();
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("image/*");
                startActivityForResult(Intent.createChooser(intent, "Select File"), REQUEST_PICK_FILE);
            }
        });

        saveBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(config==null || config=="")
                {
                    Toast.makeText(MainActivity.this, "no info for create file found", Toast.LENGTH_SHORT).show();
                    return;
                }
                requestSaveFilePermission();
                Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("*/*");
                intent.putExtra(Intent.EXTRA_TITLE, "config.conf");
                startActivityForResult(intent, WRITE_REQUEST_CODE);
            }
        });
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (result != null && requestCode!=REQUEST_PICK_FILE && requestCode!=WRITE_REQUEST_CODE) {
            if (result.getContents() == null) {
                Toast.makeText(this, "Scan cancelled", Toast.LENGTH_LONG).show();
            } else {
                try{
                    InputStream inputStream = getContentResolver().openInputStream(data.getData());
                    Bitmap bitmap = BitmapFactory.decodeStream(inputStream);

                    ImageView imageView = findViewById(R.id.imageView);
                    imageView.setImageBitmap(bitmap);
                    config = parseQRCode(bitmap);
                    saveBtn.setVisibility(View.VISIBLE);
                    inputStream.close();

                }catch (FileNotFoundException e){
                    Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
                    saveBtn.setVisibility(View.INVISIBLE);
                }catch (IOException e){
                    Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
                    saveBtn.setVisibility(View.INVISIBLE);
                }
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }

        if(requestCode==REQUEST_PICK_FILE && resultCode==RESULT_OK && data!=null){
            Uri selectedFileUri = data.getData();
            try {
                InputStream inputStream = getContentResolver().openInputStream(selectedFileUri);
                Bitmap bitmap = BitmapFactory.decodeStream(inputStream);

                // Display or use the loaded bitmap as needed
                ImageView imageView = findViewById(R.id.imageView);
                imageView.setImageBitmap(bitmap);
                saveBtn.setVisibility(View.VISIBLE);
                String qrResult = parseQRCode(bitmap);
                config = qrResult;
                inputStream.close();
            } catch (FileNotFoundException e) {
                Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
                saveBtn.setVisibility(View.INVISIBLE);
            }catch (IOException e){
                Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
                saveBtn.setVisibility(View.INVISIBLE);

            }
        }
        if(requestCode==WRITE_REQUEST_CODE && resultCode==RESULT_OK && data!=null){
            Uri selectedFileUri = data.getData();
            String res=saveFile(config,selectedFileUri);
            Toast.makeText(this, res, Toast.LENGTH_SHORT).show();
        }
    }
    private String saveFile(String str,Uri selectedFileUri){
        try {
            OutputStream stream = getContentResolver().openOutputStream(selectedFileUri);
            OutputStreamWriter osw=new OutputStreamWriter(stream);
            osw.write(str);
            osw.flush();
            stream.close();
            return "successfull";
        } catch (Exception e) {
            return e.getMessage();
        }

    }
    private void initQRCodeScanner() {
        IntentIntegrator integrator = new IntentIntegrator(this);
        integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE);
        integrator.setOrientationLocked(true);
        integrator.setPrompt("Scan a QR code");
        integrator.initiateScan();
    }
    private  void requestPickFilePermission(){
        if(ActivityCompat.checkSelfPermission(this,Manifest.permission.READ_EXTERNAL_STORAGE)!=PackageManager.PERMISSION_GRANTED)
            ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},1);;
    }
    private void requestCameraPermission(){
        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA)!= PackageManager.PERMISSION_GRANTED)
            ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.CAMERA},1);
    }
    private void requestSaveFilePermission(){
        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)!= PackageManager.PERMISSION_GRANTED)
            ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},1);
    }
    private String parseQRCode(Bitmap bitmap) {
        try {
            int[] intArray = new int[bitmap.getWidth() * bitmap.getHeight()];
            // Copy pixel data from the Bitmap into the intArray
            bitmap.getPixels(intArray, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());

            RGBLuminanceSource source = new RGBLuminanceSource(bitmap.getWidth(), bitmap.getHeight(), intArray);
            BinaryBitmap binaryBitmap = new BinaryBitmap(new HybridBinarizer(source));
            QRCodeReader reader = new QRCodeReader();
            Result result = reader.decode(binaryBitmap);
            return result.getText();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}