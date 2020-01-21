package com.example.che.mobilenetssd_demo;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;
import android.hardware.Camera;
import android.graphics.ImageFormat;
import android.view.Surface;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;



public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getName();
    private static final int USE_PHOTO = 1001;
    private ImageView show_image;
//    private SurfaceView surface_view;
    private Camera camera;
    Button use_photo;
    private boolean load_result = false;
    private int[] ddims = {1, 3, 320, 256}; //这里的维度的值要和train model的input 一一对应
    private List<String> resultLabel = new ArrayList<>();
    private FastSal fastsal = new FastSal(); //java接口实例化　下面直接利用java函数调用NDK c++函数


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        show_image = (ImageView) findViewById(R.id.show_image);
        use_photo = (Button) findViewById(R.id.use_photo);
        try
        {
            initMobileNetSSD();
        } catch (IOException e) {
            Log.e("MainActivity", "initMobileNetSSD error");
        }
        init_view();
        readCacheLabelFromLocalFile();
}

    /**
     *
     * MobileNetssd初始化，也就是把model文件进行加载
     */
    private void initMobileNetSSD() throws IOException {
        byte[] param = null;
        byte[] bin = null;
        {
            //用io流读取二进制文件，最后存入到byte[]数组中
//            InputStream assetsInputStream = getAssets().open("MobileNetSSD_deploy.param.bin");// param：  网络结构文件
            InputStream assetsInputStream = getAssets().open("fastsal.param.bin");// param：  网络结构文件
            int available = assetsInputStream.available();
            param = new byte[available];
            int byteCode = assetsInputStream.read(param);
            assetsInputStream.close();
        }
        {
            //用io流读取二进制文件，最后存入到byte上，转换为int型
//            InputStream assetsInputStream = getAssets().open("MobileNetSSD_deploy.bin");//bin：   model文件
            InputStream assetsInputStream = getAssets().open("fastsal.bin");//bin：   model文件
            int available = assetsInputStream.available();
            bin = new byte[available];
            int byteCode = assetsInputStream.read(bin);
            assetsInputStream.close();
        }

        load_result = fastsal.Init(param, bin);// 再将文件传入java的NDK接口(c++ 代码中的init接口 )
        Log.d("load model", "fastsal load:" + load_result);
    }


    // initialize view
    private void init_view() {

        request_permissions();
        // use photo click
        use_photo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!load_result) {
                    Toast.makeText(MainActivity.this, "never load model", Toast.LENGTH_SHORT).show();
                    return;
                }
                PhotoUtil.use_photo(MainActivity.this, USE_PHOTO);
            }
        });
    }

    private void initCamera(){
        Camera.Parameters parameters = camera.getParameters();
        parameters.setPreviewFormat(ImageFormat.NV21);
        camera.setParameters(parameters);
        setCameraDisplayOrientation(this);
        camera.startPreview();
    }

    public void setCameraDisplayOrientation(Activity activity) {
        Camera.CameraInfo info = new Camera.CameraInfo();
        Camera.getCameraInfo(0, info);
        int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
        }
        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360; // compensate the mirror
        } else { // back-facing
            result = (info.orientation - degrees + 360) % 360;
        }
        camera.setDisplayOrientation(result);
    }

    private void detectionLoop(){

    }

    // load label's name
    private void readCacheLabelFromLocalFile() {
        try {
            AssetManager assetManager = getApplicationContext().getAssets();
            BufferedReader reader = new BufferedReader(new InputStreamReader(assetManager.open("words.txt")));//这里是label的文件
            String readLine = null;
            while ((readLine = reader.readLine()) != null) {
                resultLabel.add(readLine);
            }
            reader.close();
        } catch (Exception e) {
            Log.e("labelCache", "error " + e);
        }
    }


    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        String image_path;
        if (resultCode == Activity.RESULT_OK) {
            switch (requestCode) {
                case USE_PHOTO:
                    if (data == null) {
                        Log.w(TAG, "user photo data is null");
                        return;
                    }
                    Uri image_uri = data.getData();
                    image_path = PhotoUtil.get_path_from_URI(MainActivity.this, image_uri);
                    inference_saliency(image_path);
                    break;
            }
        }
    }

//    private void inference_saliency(String image_path){
//        Bitmap bmp = PhotoUtil.getScaleBitmap(image_path);
//        Bitmap rgba = bmp.copy(Bitmap.Config.ARGB_8888, true);
//        Bitmap input_bmp = Bitmap.createScaledBitmap(rgba, ddims[2], ddims[3], false);
//        Bitmap output_bmp = Bitmap.createBitmap(input_bmp.getWidth(), input_bmp.getHeight(), Bitmap.Config.ALPHA_8);
//        Bitmap show_bmp = Bitmap.createBitmap(input_bmp.getWidth(), input_bmp.getHeight(), Bitmap.Config.ARGB_8888);
//        Bitmap final_bmp = Bitmap.createBitmap(input_bmp.getWidth(), input_bmp.getHeight(), Bitmap.Config.ARGB_8888);
//        show_bmp.eraseColor(Color.RED);
//        Canvas showCanvas = new Canvas(show_bmp);
//        Paint paint = new Paint();
//        paint.setFilterBitmap(false);
//        showCanvas.drawBitmap(input_bmp, 0, 0, paint);
//        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_OUT));
//        showCanvas.drawBitmap(output_bmp, 0, 0, paint);
//        paint.setXfermode(null);
//        try {
//            fastsal.Detect(input_bmp, output_bmp);
//            Log.d("run detection model", "detection finished");
////            show_image.setImageBitmap(output_bmp);
//            show_image.setImageDrawable(new BitmapDrawable(getResources(), show_bmp));
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }

    private void inference_saliency(String image_path){
        long startTime;
        long endTime;
        Bitmap bmp = PhotoUtil.getScaleBitmap(image_path);
        Bitmap rgba = bmp.copy(Bitmap.Config.ARGB_8888, true);
        Bitmap input_bmp = Bitmap.createScaledBitmap(rgba, ddims[2], ddims[3], false);
        Bitmap output_bmp = Bitmap.createBitmap(input_bmp.getWidth(), input_bmp.getHeight(), Bitmap.Config.ALPHA_8);
        try {
            startTime = System.currentTimeMillis();
            fastsal.Detect(input_bmp, output_bmp);
            endTime = System.currentTimeMillis();
            Log.d("runtime", "detect: "+(endTime - startTime));

            startTime = System.currentTimeMillis();
            Bitmap show_bmp = PhotoUtil.mergeAlpha(input_bmp, output_bmp);
            endTime = System.currentTimeMillis();
            Log.d("runtime", "merge: "+(endTime - startTime));

            startTime = System.currentTimeMillis();
            show_image.setImageBitmap(show_bmp);
            endTime = System.currentTimeMillis();
            Log.d("runtime", "showing: "+(endTime - startTime));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //一维数组转化为二维数组
    public static float[][] TwoArry(float[] inputfloat){
        int n = inputfloat.length;
        int num = inputfloat.length/6;
        float[][] outputfloat = new float[num][6];
        int k = 0;
        for(int i = 0; i < num ; i++)
        {
            int j = 0;

            while(j<6)
            {
                outputfloat[i][j] =  inputfloat[k];
                k++;
                j++;
            }

        }

        return outputfloat;
    }

    private void request_permissions() {
        List<String> permissionList = new ArrayList<>();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            permissionList.add(Manifest.permission.CAMERA);
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            permissionList.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            permissionList.add(Manifest.permission.READ_EXTERNAL_STORAGE);
        }
        // if list is not empty will request permissions
        if (!permissionList.isEmpty()) {
            ActivityCompat.requestPermissions(this, permissionList.toArray(new String[permissionList.size()]), 1);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case 1:
                if (grantResults.length > 0) {
                    for (int i = 0; i < grantResults.length; i++) {
                        int grantResult = grantResults[i];
                        if (grantResult == PackageManager.PERMISSION_DENIED) {
                            String s = permissions[i];
                            Toast.makeText(this, s + "permission was denied", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
                break;
        }
    }
}

