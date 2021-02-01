package com.example.blankproject;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Camera;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import com.camerakit.CameraKitView;

import java.io.File;
import java.io.FileOutputStream;
import java.util.logging.Logger;

public class MainActivity extends AppCompatActivity {

    private CameraKitView cameraKitView;
    private ImageView attentionView;
    private Button switcher;
    private Boolean showAttentionFlag = false;
    private Integer globalCount = 0;
    private Bitmap sampleSaliencyBitmap = BitmapFactory.decodeStream(getClass().getResourceAsStream("/res/drawable/saliency.jpeg"));
    private Bitmap sampleLEDBitmap = BitmapFactory.decodeStream(getClass().getResourceAsStream("/res/drawable/led.png"));

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        cameraKitView = findViewById(R.id.camera);
        attentionView = findViewById(R.id.attention);
        attentionView.setAlpha(0.5f);
        switcher = findViewById(R.id.switcher);

        switcher.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                showAttentionFlag = !showAttentionFlag;
                if(showAttentionFlag){
                    switcher.setText("停止检测");
//                    Bitmap newSample = setAlpha(sampleSaliencyBitmap);
//                    attentionView.setImageBitmap(newSample);
                    startAsyncImageUpdate();
                }else{
                    switcher.setText("开始检测");
                    attentionView.setImageDrawable(null);
                }
            }
        });



    }

    static Bitmap setAlpha(Bitmap bitmap){
        int[] pixels = new int[bitmap.getWidth() * bitmap.getHeight()];
        bitmap.getPixels(pixels, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());
        for (int i = 0; i < pixels.length; i++) {
            int pixel = pixels[i];
            int R = (pixel >> 16) & 0xff;
            int newPixel = (R << 24) | R;
            pixels[i] = newPixel;
        }
        bitmap = bitmap.copy( Bitmap.Config.ARGB_8888 , true);
        bitmap.setPixels(pixels, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());
        return bitmap;
    }

    static Bitmap convert(Bitmap bitmap){
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        Bitmap newBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        for(int i=0; i<height; i++){
            for(int j=0; j<width; j++){
                newBitmap.setPixel(j, i, bitmap.getPixel(j, height-i-1));
            }
        }
        return newBitmap;
    }

    void startAsyncImageUpdate(){

        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            int tick = 0;
            public void run() {
                if (tick % 2 == 0) {
                    Bitmap newSample = setAlpha(sampleSaliencyBitmap);
                    attentionView.setImageBitmap(newSample);
                    Log.d("TAG", "startAsyncImageUpdate: using saliency map");
                } else {
                    Bitmap newSample = setAlpha(sampleLEDBitmap);
                    attentionView.setImageBitmap(newSample);
                    Log.d("TAG", "startAsyncImageUpdate: using LED map");
                }
                tick++;
                handler.postDelayed(this, 250);
            }
        }, 0);
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

