//
// Created by tencent on 2020-04-29.
//
#include "saliencypredictor_jni.h"
#include "SaliencyPredictor.h"
#include "kannarotate.h"
#include "yuv420sp_to_rgb_fast_asm.h"
#include <jni.h>
#include "helper_jni.h"
#include <android/bitmap.h>

static std::shared_ptr<SaliencyPredictor> gDetector;
static int gComputeUnitType = 0; // 0 is cpu, 1 is gpu
static jclass clsFaceInfo;
static jmethodID midconstructorFaceInfo;
static jfieldID fidx1;
static jfieldID fidy1;
static jfieldID fidx2;
static jfieldID fidy2;
static jfieldID fidscore;
static jfieldID fidlandmarks;

JNIEXPORT JNICALL jint TNN_SALIENCY_PREDICTOR(init)(JNIEnv *env, jobject thiz, jstring modelPath, jint width, jint height)
{
    // Reset bench description
    setBenchResult("");
    std::vector<int> nchw = {1, 3, height, width};
    gDetector = std::make_shared<SaliencyPredictor>(width, height, 1, 0.7);
    std::string protoContent, modelContent;
    std::string modelPathStr(jstring2string(env, modelPath));
    protoContent = fdLoadFile(modelPathStr + "/fastsal.tnnproto");
    modelContent = fdLoadFile(modelPathStr + "/fastsal.tnnmodel");
    LOGI("proto content size %d model content size %d", protoContent.length(), modelContent.length());

    TNN_NS::Status status;
    gComputeUnitType = computUnitType;
    if (gComputeUnitType == 0 ) {
        gDetector->Init(protoContent, modelContent, "", TNN_NS::TNNComputeUnitsCPU, nchw);
    } else {
        gDetector->Init(protoContent, modelContent, "", TNN_NS::TNNComputeUnitsGPU, nchw);
    }
    if (status != TNN_NS::TNN_OK) {
        LOGE("detector init failed %d", (int)status);
        return -1;
    }

    return 0;
}

JNIEXPORT JNICALL jint TNN_SALIENCY_PREDICTOR(deinit)(JNIEnv *env, jobject thiz)
{

    gDetector = nullptr;
    return 0;
}


JNIEXPORT JNICALL jint TNN_SALIENCY_PREDICTOR(predictFromStream)(JNIEnv *env, jobject thiz, jbyteArray yuv420sp, jint width, jint height, jobject bitmapOut)
{
    jobjectArray faceInfoArray;
    auto asyncRefDetector = gDetector;
    std::vector<FaceInfo> faceInfoList;
    // Convert yuv to rgb
    LOGI("detect from stream %d x %d r %d", width, height, rotate);
    unsigned char *yuvData = new unsigned char[height * width * 3 / 2];
    jbyte *yuvDataRef = env->GetByteArrayElements(yuv420sp, 0);
    int ret = kannarotate_yuv420sp((const unsigned char*)yuvDataRef, (int)width, (int)height, (unsigned char*)yuvData, (int)rotate);
    env->ReleaseByteArrayElements(yuv420sp, yuvDataRef, 0);
    unsigned char *rgbaData = new unsigned char[height * width * 4];
    unsigned char *rgbData = new unsigned char[height * width * 3];
    yuv420sp_to_rgba_fast_asm((const unsigned char*)yuvData, height, width, (unsigned char*)rgbaData);

    TNN_NS::Mat retMap;
    TNN_NS::DeviceType dt = TNN_NS::DEVICE_ARM;
    TNN_NS::DimsVector target_dims = {1, 3, height, width};
    auto rgbTNN = std::make_shared<TNN_NS::Mat>(dt, TNN_NS::N8UC4, target_dims, rgbaData);
    TNN_NS::Status status = asyncRefDetector->Detect(rgbTNN, width, height, retMap);
    LOGE("!!!!!!!!!!!!!ret map: %d", retMap.size());
    delete [] yuvData;
    delete [] rgbaData;
    if (status != TNN_NS::TNN_OK) {
        LOGE("failed to detect %d", (int)status);
        return 0;
    }



}