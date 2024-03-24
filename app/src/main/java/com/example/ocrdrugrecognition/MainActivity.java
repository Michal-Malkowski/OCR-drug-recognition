package com.example.ocrdrugrecognition;

import android.Manifest;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;

import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.text.TextBlock;
import com.google.android.gms.vision.text.TextRecognizer;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.provider.MediaStore;
import android.util.SparseArray;
import android.view.View;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;


public class MainActivity extends AppCompatActivity {
    Button capture, copy;
    TextView viewData, description_db, text_dose, drug_name_db, inn_text;
    private static final int CAMERA_CODE = 100;
    Bitmap map;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        capture = findViewById(R.id.button_capture);
        copy = findViewById(R.id.button_copy);
        description_db = findViewById(R.id.TextView_drug_description_db);
        text_dose = findViewById(R.id.textView_drug_description);
        drug_name_db =  findViewById(R.id.textView_drug_name_inn_db);
        inn_text = findViewById(R.id.TextView_drug_name_inn);
        viewData = findViewById(R.id.textView_drug_name_db);

        if(ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)
        {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{
                    Manifest.permission.CAMERA
            }, CAMERA_CODE);
        }

        capture.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v)
            {
                CropImage.activity().setGuidelines(CropImageView.Guidelines.ON).start(MainActivity.this);
            }
        });

        copy.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v)
            {
                String s = drug_name_db.getText().toString();
                copyText(s);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);
        copy = findViewById(R.id.button_copy);
        if(requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE)
        {
            CropImage.ActivityResult r = CropImage.getActivityResult(data);
            if(resultCode == RESULT_OK)
            {
                assert r != null;
                Uri u = r.getUri();
                try
                {
                    map = MediaStore.Images.Media.getBitmap(this.getContentResolver(), u);
                    getImageText(map);
                }
                catch (IOException e)
                {
                    e.printStackTrace();
                }
            }
        }
    }

    private void getImageText(Bitmap bitmap)
    {
        TextRecognizer rec = new TextRecognizer.Builder(this).build();
        String str_to_string;
        if(!rec.isOperational())
        {
            Toast.makeText(MainActivity.this, "Błąd!", Toast.LENGTH_SHORT).show();
        }
        else
        {
            StringBuilder line = new StringBuilder();
            Frame f = new Frame.Builder().setBitmap(bitmap).build();
            SparseArray<TextBlock> textBlock = rec.detect(f);
            StringBuilder str = new StringBuilder();
            for(int i=0; i<textBlock.size(); i++)
            {
                TextBlock tb = textBlock.valueAt(i);
                str.append(tb.getValue());
                str.append("\n");
            }
            str_to_string = str.toString();
            if (!str_to_string.equals("")){
                capture.setText(R.string.wybierz_kolejne);
                copy.setVisibility(View.VISIBLE);
                description_db.setVisibility(View.VISIBLE);
                text_dose.setVisibility(View.VISIBLE);
                drug_name_db.setVisibility(View.VISIBLE);
                inn_text.setVisibility(View.VISIBLE);
                drug_name_db.setText(str_to_string);
                String s = drug_name_db.getText().toString();
                try
                {
                    String url_string = String.format("http://proz.pythonanywhere.com/getDescription?name=%s", str);
                    URL url = new URL(url_string);
                    HttpURLConnection httpCon = (HttpURLConnection) url.openConnection();
                    httpCon.setDoOutput(true);
                    httpCon.setRequestMethod("POST");

                    httpCon.setRequestProperty("Content-Type", "application/json");
                    httpCon.setRequestProperty("Accept", "application/json");

                    httpCon.getResponseMessage();
                    Integer code = httpCon.getResponseCode();
                    DataOutputStream out = new DataOutputStream(httpCon.getOutputStream());
                    out.write(s.getBytes(StandardCharsets.UTF_8));
                    if(code == 200)
                    {
                        Scanner sc = new Scanner(url.openStream());
                        while(sc.hasNext())
                        {
                            line.append(sc.nextLine());
                        }
                        sc.close();
                    }
                    httpCon.disconnect();
                }
                catch(Exception e)
                {
                }

                drug_name_db.setText(s);
                description_db.setText(line.toString());
            }
        }
    }

    private void copyText(String str)
    {
        ClipboardManager clipBoard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clipData = ClipData.newPlainText(getString(R.string.skopiowano), str);
        clipBoard.setPrimaryClip(clipData);
        Toast.makeText(MainActivity.this, getString(R.string.skopiowano), Toast.LENGTH_SHORT).show();
    }
}