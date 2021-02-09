package com.example.blankproject;

import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Camera;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.media.Image;
import android.os.Build;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
//import android.graphics.Camera;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.SystemClock;
import android.util.Log;
import android.util.Size;
import android.view.OrientationEventListener;
import android.view.OrientationListener;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.common.util.concurrent.ListenableFuture;

import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.logging.Logger;

public class MainActivity extends AppCompatActivity {

    private static final String[] CAMERA_PERMISSION = new String[]{Manifest.permission.CAMERA};
    private static final int CAMERA_REQUEST_CODE = 10;

    private TextView orientView;
    private PreviewView cameraView;
    private ImageView attentionView;
    private Button switcher;
    private Boolean showAttentionFlag = false;

    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;

    private Bitmap cameraFrame;
    private Bitmap attentionMap;

    private int[] ddims = {1, 3, 320, 256};
    private static Handler updateHandler;
    private FastSal fastsal = new FastSal();


    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        System.out.println("*********** INFO START ***********");
        System.out.println(Build.CPU_ABI);
        System.out.println(Build.CPU_ABI2);
        System.out.println("*********** INFO END ***********");

        setContentView(R.layout.activity_main);

        cameraView = findViewById(R.id.camera_view);
        attentionView = findViewById(R.id.attention);
        orientView = findViewById(R.id.oriented);
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
                     startBackgroundThread();
                }else{
                    switcher.setText("开始检测");
                    // attentionView.setImageDrawable(null);
                    int retCode = stopBackgroundThread();
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

    private Bitmap inferenceSaliency(Bitmap bmp){
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

            return setAlpha(output_bmp);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
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

    private void startBackgroundThread(){
        cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(new Runnable() {
            @Override
            public void run() {
                try{
                    ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                    bindSaliencyInference(cameraProvider);
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private int stopBackgroundThread(){
        try{
            cameraProviderFuture.cancel(true);
            Toast.makeText(this, "stop success...", Toast.LENGTH_SHORT).show();
        }catch (Exception e){
            Toast.makeText(this, "stop failed, you may need to restart the app...", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
            return -1;
        }
        return 0;
    }

    private void bindOrientAnalysis(@NotNull ProcessCameraProvider cameraProvider){
        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder().setTargetResolution(new Size(1280, 720)).setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build();
        imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(this), new ImageAnalysis.Analyzer(){
            @Override
            public void analyze(@NotNull ImageProxy image){
                image.close();
            }
        });

        OrientationEventListener orientationEventListener = new OrientationEventListener(this) {
            @SuppressLint("SetTextI18n")
            @Override
            public void onOrientationChanged(int orientation) {
                orientView.setText(Integer.toString(orientation));
            }
        };
        orientationEventListener.enable();
        Preview preview = new Preview.Builder().build();
        CameraSelector cameraSelector = new CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_BACK).build();
        preview.setSurfaceProvider(cameraView.createSurfaceProvider());

        cameraProvider.bindToLifecycle((LifecycleOwner) this, cameraSelector, imageAnalysis, preview);

    }

    private void bindSaliencyInference(@NotNull ProcessCameraProvider cameraProvider){
        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder().setTargetResolution(new Size(480, 640)).setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build();
        imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(this), new ImageAnalysis.Analyzer(){

            @Override
            public void analyze(@NotNull ImageProxy image){
                try{

                    Bitmap bitmapImage = convertImageProxyToBitmap(image);
                    if(bitmapImage != null){
                        Bitmap saliencyMap = inferenceSaliency(bitmapImage);
                        attentionView.setImageBitmap(saliencyMap);
                    }else{
                        System.out.println("------------------- bitmap is null -------------------");
                        orientView.setText("NULL Bitmap");
                    }

                    image.close();

                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        });

        Preview preview = new Preview.Builder().build();
        CameraSelector cameraSelector = new CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_BACK).build();
        preview.setSurfaceProvider(cameraView.createSurfaceProvider());

        cameraProvider.bindToLifecycle((LifecycleOwner) this, cameraSelector, imageAnalysis, preview);
    }

    @SuppressLint("UnsafeExperimentalUsageError")
    private Bitmap convertImageProxyToBitmap(ImageProxy image) {
        ByteBuffer yBuffer = image.getPlanes()[0].getBuffer();
        ByteBuffer uBuffer = image.getPlanes()[1].getBuffer();
        ByteBuffer vBuffer = image.getPlanes()[2].getBuffer();

        int ySize = yBuffer.remaining();
        int uSize = uBuffer.remaining();
        int vSize = vBuffer.remaining();

        byte[] bytes = new byte[ySize + uSize + vSize];
        yBuffer.get(bytes, 0, ySize);
        uBuffer.get(bytes, ySize + vSize, uSize);
        vBuffer.get(bytes, ySize, vSize);

        YuvImage yuvImage = new YuvImage(bytes, ImageFormat.NV21, image.getWidth(), image.getHeight(), null);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        yuvImage.compressToJpeg(new Rect(0, 0, yuvImage.getWidth(), yuvImage.getHeight()), 100, outputStream);
        byte[] imageBytes = outputStream.toByteArray();
        Bitmap bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
        return rotateBitmap(bitmap, 90);

    }

    private Bitmap rotateBitmap(Bitmap origin, float alpha) {
        if (origin == null) {
            return null;
        }
        int width = origin.getWidth();
        int height = origin.getHeight();
        Matrix matrix = new Matrix();
        matrix.setRotate(alpha);
        // 围绕原地进行旋转
        Bitmap newBM = Bitmap.createBitmap(origin, 0, 0, width, height, matrix, false);
        if (newBM.equals(origin)) {
            return newBM;
        }
        origin.recycle();
        return newBM;
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NotNull String[] permissions, @NotNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

}

