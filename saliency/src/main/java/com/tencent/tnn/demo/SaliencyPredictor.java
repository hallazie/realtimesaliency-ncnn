package com.tencent.tnn.demo;

import android.graphics.Bitmap;

public class SaliencyPredictor {
    public native int init(String modelPath, int width, int height);
    public native int deinit();
    public native int predictFromStream(byte[] yuv420sp, int width, int height, Bitmap outputMap);
}
