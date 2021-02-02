#include <android/bitmap.h>
#include <android/log.h>
#include <jni.h>
#include <string>
#include <vector>
#include <iostream>
#include "fastsal.id.h"
#include <sys/time.h>
#include <unistd.h>

#include "layer.h"
#include "net.h"
#include "benchmark.h"

static ncnn::UnlockedPoolAllocator g_blob_pool_allocator;
static ncnn::PoolAllocator g_workspace_pool_allocator;

static ncnn::Mat ncnn_param;
static ncnn::Mat ncnn_bin;
static ncnn::Net ncnn_net;


extern "C" {

JNIEXPORT jboolean JNICALL
Java_com_example_blankproject_FastSal_Init(JNIEnv *env, jobject obj, jbyteArray param, jbyteArray bin) {
    __android_log_print(ANDROID_LOG_DEBUG, "FastSal", "enter the jni func");
    // init param
    {
        int len = env->GetArrayLength(param);
        ncnn_param.create(len, (size_t) 1u);
        env->GetByteArrayRegion(param, 0, len, (jbyte *) ncnn_param);
        int ret = ncnn_net.load_param((const unsigned char *) ncnn_param);
        __android_log_print(ANDROID_LOG_DEBUG, "FastSal", "load_param %d %d", ret, len);
    }

    // init bin
    {
        int len = env->GetArrayLength(bin);
        ncnn_bin.create(len, (size_t) 1u);
        env->GetByteArrayRegion(bin, 0, len, (jbyte *) ncnn_bin);
        int ret = ncnn_net.load_model((const unsigned char *) ncnn_bin);
        __android_log_print(ANDROID_LOG_DEBUG, "FastSal", "load_model %d %d", ret, len);
    }

    ncnn::Option opt;
    opt.lightmode = true;
    opt.num_threads = 4;   //线程 这里可以修改
    opt.blob_allocator = &g_blob_pool_allocator;
    opt.workspace_allocator = &g_workspace_pool_allocator;

    ncnn_net.opt = opt;

    return JNI_TRUE;
}

JNIEXPORT jboolean JNICALL
Java_com_example_blankproject_FastSal_Detect(JNIEnv* env, jobject thiz, jobject bitmap, jobject bitmapOut)
{
    ncnn::Mat in;
    AndroidBitmapInfo info;
    AndroidBitmap_getInfo(env, bitmap, &info);
    int width = info.width;
    int height = info.height;
    void* indata;

    AndroidBitmap_lockPixels(env, bitmap, &indata);
    in = ncnn::Mat::from_pixels((const unsigned char*)indata, ncnn::Mat::PIXEL_RGBA2RGB, width, height);
    AndroidBitmap_unlockPixels(env, bitmap);


    // ncnn_net
    const float mean_vals[3] = {255*0.485f, 255*0.456f, 255*0.406f};
    const float std_vals[3] = {1.0f/(255 * 0.229f), 1.0f/(255 * 0.224f), 1.0f/(255 * 0.225f)};
    in.substract_mean_normalize(mean_vals, std_vals);// 归一化


    clock_t start,end;
    ncnn::Mat out;
    ncnn::Extractor ex = ncnn_net.create_extractor();//前向传播

    start = clock();
    ex.input(fastsal_param_id::BLOB_input0, in);
    ex.extract(fastsal_param_id::BLOB_output0, out);
    end = clock();
    __android_log_print(ANDROID_LOG_DEBUG, "FastSal", "model runtime: %f", ((float)(end-start)*1000/CLOCKS_PER_SEC));


    float min = 999.0f;
    float max = -999.0f;
    for(int i=0; i<out.h; i++){
        for(int j=0; j<out.w; j++){
            if(out[i*out.w + j] < min){
                min = out[i*out.w + j];
            }
            if(out[i*out.w + j] > max){
                max = out[i*out.w + j];
            }
        }
    }
    for(int i=0; i<out.h; i++){
        for(int j=0; j<out.w; j++){
            out[i*out.w + j] = 250 * (out[i*out.w + j] - min) / (max - min);
        }
    }

//        out.to_android_bitmap(env, bitmap, ncnn::Mat::PIXEL_GRAY);

    int type = ncnn::Mat::PIXEL_GRAY;
    void* data;
    AndroidBitmap_lockPixels(env, bitmapOut, &data);
    out.to_pixels_resize((unsigned char*)data, type, width, height);
    AndroidBitmap_unlockPixels(env, bitmapOut);
    return JNI_TRUE;
}

}