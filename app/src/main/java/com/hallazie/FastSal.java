package com.example.che.mobilenetssd_demo;

import android.graphics.Bitmap;

/**
 * Created by halla on 2020/1/1.
 */

public class FastSal {

    public native boolean Init(byte[] param, byte[] bin); // 初始化函数
    public native boolean Detect(Bitmap bitmap, Bitmap bitmapOut); // 检测函数
    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("fastsal");
    }
}