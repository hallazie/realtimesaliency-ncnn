package com.example.blankproject;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Camera;
import android.os.Bundle;
import android.os.Environment;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import com.camerakit.CameraKitView;

import java.io.File;
import java.io.FileOutputStream;

public class MainActivity extends AppCompatActivity {

    private CameraKitView cameraKitView;
//    private ImageView attentionView;
    private Button switcher;

    private View.OnClickListener photoOnClickListener = new View.OnClickListener(){
        @Override
        public void onClick(View v) {
            System.out.println("click out...");
            cameraKitView.captureImage(new CameraKitView.ImageCallback() {
                @Override
                public void onImage(CameraKitView cameraKitView, final byte[] capturedImage) {
                    File savePhoto = new File(Environment.getExternalStorageDirectory(), "photo.jpg");
                    System.out.println("click detected!!");
                    try{
                        FileOutputStream outputStream = new FileOutputStream(savePhoto);
                        outputStream.write(capturedImage);
                        outputStream.close();
                    }catch (java.io.IOException e){
                        e.printStackTrace();
                    }
                }
            });
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        cameraKitView = findViewById(R.id.camera);
//        attentionView = findViewById(R.id.attention);
        switcher = findViewById(R.id.switcher);
        cameraKitView.setOnClickListener(photoOnClickListener);
    }


    @Override
    protected void onStart() {
        super.onStart();
        cameraKitView.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        cameraKitView.onResume();
    }

    @Override
    protected void onPause() {
        cameraKitView.onPause();
        super.onPause();
    }

    @Override
    protected void onStop() {
        cameraKitView.onStop();
        super.onStop();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        cameraKitView.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

}

