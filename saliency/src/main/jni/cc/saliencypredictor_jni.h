//
// Created by tencent on 2020-04-30.
//

#ifndef ANDROID_SALIENCYPREDICTOR_JNI_H
#define ANDROID_SALIENCYPREDICTOR_JNI_H
#include <jni.h>
#define TNN_SALIENCY_PREDICTOR(sig) Java_com_tencent_tnn_demo_SaliencyPredictor_##sig
#ifdef __cplusplus
extern "C" {
#endif
JNIEXPORT JNICALL jint TNN_SALIENCY_PREDICTOR(init)(JNIEnv *env, jobject thiz, jstring modelPath, jint width, jint height);
JNIEXPORT JNICALL jint TNN_SALIENCY_PREDICTOR(deinit)(JNIEnv *env, jobject thiz);
JNIEXPORT JNICALL jint TNN_SALIENCY_PREDICTOR(predictFromStream)(JNIEnv *env, jobject thiz, jbyteArray yuv420sp, jint width, jint height, jobject bitmapOut);

#ifdef __cplusplus
}
#endif
#endif //ANDROID_FACEDETECTOR_JNI_H
