package com.example.blankproject;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Build;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Camera;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import com.camerakit.CameraKitView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Logger;

public class MainActivity extends AppCompatActivity {

    private CameraKitView cameraKitView;
    private ImageView attentionView;
    private Button switcher;
    private Boolean showAttentionFlag = false;

    private boolean runSaliency;
    private HandlerThread backgroundThread;
    private Handler backgroundHandler;
    private final Object lock = new Object();

    private int[] ddims = {1, 3, 320, 256};
    private static Handler updateHandler;
    private FastSal fastsal = new FastSal();


    @SuppressLint("HandlerLeak")
    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        System.out.println("*********** INFO START ***********");
        System.out.println(Build.CPU_ABI);
        System.out.println(Build.CPU_ABI2);
        System.out.println("*********** INFO END ***********");

        setContentView(R.layout.activity_main);
        cameraKitView = findViewById(R.id.camera);
        attentionView = findViewById(R.id.attention);
        attentionView.setAlpha(0.5f);
        switcher = findViewById(R.id.switcher);

        try {
            initFastSal();
        } catch (IOException e) {
            Log.e("MainActivity", "init FastSal Module error");
        }

        switcher.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                showAttentionFlag = !showAttentionFlag;
                if(showAttentionFlag){
                    switcher.setText("停止检测");
                    // startBackgroundThread();
                    cameraKitView.captureImage(new CameraKitView.ImageCallback() {
                        @Override
                        public void onImage(CameraKitView cameraKitView, byte[] bytes) {
                            Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                            inferenceSaliency(bitmap);
                        }
                    });
                }else{
                    switcher.setText("开始检测");
                    // stopBackgroundThread();
                    // handler.removeCallbacks(runnable);
                    attentionView.setImageDrawable(null);
                }
            }
        });
    }

    private void initFastSal() throws IOException {
        byte[] param = null;
        byte[] bin = null;
        {
            InputStream assetsInputStream = getAssets().open("fastsal.param.bin");// param：  网络结构文件
            int available = assetsInputStream.available();
            param = new byte[available];
            int byteCode = assetsInputStream.read(param);
            assetsInputStream.close();
        }
        {
            InputStream assetsInputStream = getAssets().open("fastsal.bin");//bin：   model文件
            int available = assetsInputStream.available();
            bin = new byte[available];
            int byteCode = assetsInputStream.read(bin);
            assetsInputStream.close();
        }

        boolean load_result = fastsal.Init(param, bin);
        Log.d("load model", "fastsal load:" + load_result);
    }

    private void inferenceSaliency(Bitmap bmp){
        long startTime;
        long endTime;
        Bitmap rgba = bmp.copy(Bitmap.Config.ARGB_8888, true);
        Bitmap input_bmp = Bitmap.createScaledBitmap(rgba, ddims[2], ddims[3], false);
        Bitmap output_bmp = Bitmap.createBitmap(input_bmp.getWidth(), input_bmp.getHeight(), Bitmap.Config.ALPHA_8);
        try {
            startTime = System.currentTimeMillis();
            fastsal.Detect(input_bmp, output_bmp);
            endTime = System.currentTimeMillis();
            Log.d("runtime", "detect: "+(endTime - startTime));

            startTime = System.currentTimeMillis();
            attentionView.setImageBitmap(setAlpha(output_bmp));
            endTime = System.currentTimeMillis();
            Log.d("runtime", "showing: "+(endTime - startTime));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void startBackgroundThread() {
        backgroundThread = new HandlerThread("saliency");
        backgroundThread.start();
        backgroundHandler = new Handler(backgroundThread.getLooper());
        synchronized (lock) {
            runSaliency = true;
        }
        backgroundHandler.post(runAsyncSaliencyUpdate);
    }

    private Runnable runAsyncSaliencyUpdate =
            new Runnable() {
                @Override
                public void run() {
                    synchronized (lock) {
                        if (runSaliency) {
                            runSaliency();
                        }
                    }
                    backgroundHandler.postDelayed(runAsyncSaliencyUpdate, 200);
                }
            };

    private void runSaliency() {
        cameraKitView.captureImage(new CameraKitView.ImageCallback() {
            @Override
            public void onImage(CameraKitView cameraKitView, byte[] bytes) {
                Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                inferenceSaliency(bitmap);
            }
        });
    }

    private void stopBackgroundThread() {
        backgroundThread.quitSafely();
        try {
            backgroundThread.join();
            backgroundThread = null;
            backgroundHandler = null;
            synchronized (lock) {
                runSaliency = false;
            }
        } catch (InterruptedException e) {
            Log.e("TAG", "Interrupted when stopping background thread", e);
        }
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

