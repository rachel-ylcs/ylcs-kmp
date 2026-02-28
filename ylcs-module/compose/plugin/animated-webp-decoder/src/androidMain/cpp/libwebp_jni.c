#include <jni.h>
#include <demux.h>
#include <stdlib.h>

struct AnimatedWebpDecoder {
    unsigned char* data;
    WebPAnimDecoder* decoder;
    int width;
    int height;
    int frameCount;
};

typedef struct AnimatedWebpDecoder AnimatedWebpDecoder;

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* vm, void* reserved) {
    JNIEnv* env = NULL;
    if ((*vm)->GetEnv(vm, (void**)&env, JNI_VERSION_1_6) != JNI_OK) return -1;
    return JNI_VERSION_1_6;
}

JNIEXPORT jlong JNICALL Java_love_yinlin_compose_graphics_NativeAnimatedWebpKt_nativeAnimatedWebpCreate(JNIEnv* env, jclass clz, jbyteArray data) {
    WebPAnimDecoderOptions options;
    WebPAnimDecoderOptionsInit(&options);
    options.color_mode = MODE_rgbA;
    options.use_threads = 1;

    jint size = (*env)->GetArrayLength(env, data);
    unsigned char* buffer = (unsigned char*)malloc(sizeof(unsigned char) * size);
    (*env)->GetByteArrayRegion(env, data, 0, size, (jbyte*)buffer);
    WebPData webp_data;
    webp_data.bytes = buffer;
    webp_data.size = (size_t)size;
    WebPAnimDecoder* decoder = WebPAnimDecoderNew(&webp_data, &options);
    if (decoder) {
        WebPAnimInfo info;
        if (WebPAnimDecoderGetInfo(decoder, &info)) {
            AnimatedWebpDecoder* instance = (AnimatedWebpDecoder*)malloc(sizeof(AnimatedWebpDecoder));
            instance->data = buffer;
            instance->decoder = decoder;
            instance->width = info.canvas_width;
            instance->height = info.canvas_height;
            instance->frameCount = info.frame_count;
            return (jlong)instance;
        }
    }
    free(buffer);
    return 0L;
}

JNIEXPORT void JNICALL Java_love_yinlin_compose_graphics_NativeAnimatedWebpKt_nativeAnimatedWebpRelease(JNIEnv* env, jclass clz, jlong handle) {
    AnimatedWebpDecoder* instance = (AnimatedWebpDecoder*)handle;
    if (instance) {
        if (instance->data) free(instance->data);
        if (instance->decoder) WebPAnimDecoderDelete(instance->decoder);
        free(instance);
    }
}

JNIEXPORT jint JNICALL Java_love_yinlin_compose_graphics_NativeAnimatedWebpKt_nativeAnimatedWebpGetWidth(JNIEnv* env, jclass clz, jlong handle) {
    AnimatedWebpDecoder* instance = (AnimatedWebpDecoder*)handle;
    return instance ? instance->width : 0;
}

JNIEXPORT jint JNICALL Java_love_yinlin_compose_graphics_NativeAnimatedWebpKt_nativeAnimatedWebpGetHeight(JNIEnv* env, jclass clz, jlong handle) {
    AnimatedWebpDecoder* instance = (AnimatedWebpDecoder*)handle;
    return instance ? instance->height : 0;
}

JNIEXPORT jint JNICALL Java_love_yinlin_compose_graphics_NativeAnimatedWebpKt_nativeAnimatedWebpGetFrameCount(JNIEnv* env, jclass clz, jlong handle) {
    AnimatedWebpDecoder* instance = (AnimatedWebpDecoder*)handle;
    return instance ? instance->frameCount : 0;
}

JNIEXPORT jboolean JNICALL Java_love_yinlin_compose_graphics_NativeAnimatedWebpKt_nativeAnimatedWebpHasMoreFrames(JNIEnv* env, jclass clz, jlong handle) {
    AnimatedWebpDecoder* instance = (AnimatedWebpDecoder*)handle;
    return (jboolean)(instance ? WebPAnimDecoderHasMoreFrames(instance->decoder) : 0);
}

JNIEXPORT jboolean JNICALL Java_love_yinlin_compose_graphics_NativeAnimatedWebpKt_nativeAnimatedWebpGetNext(JNIEnv* env, jclass clz, jlong handle, jobject directBuffer) {
    AnimatedWebpDecoder* instance = (AnimatedWebpDecoder*)handle;
    if (instance) {
        uint8_t* buf = NULL;
        int timestamp = 0;
        WebPAnimDecoderGetNext(instance->decoder, &buf, &timestamp);
        if (buf) {
            void* dst = (*env)->GetDirectBufferAddress(env, directBuffer);
            size_t size = (size_t)((*env)->GetDirectBufferCapacity(env, directBuffer));
            memcpy(dst, buf, size);
            return 1;
        }
    }
    return 0;
}

JNIEXPORT void JNICALL Java_love_yinlin_compose_graphics_NativeAnimatedWebpKt_nativeAnimatedWebpReset(JNIEnv* env, jclass clz, jlong handle) {
    AnimatedWebpDecoder* instance = (AnimatedWebpDecoder*)handle;
    if (instance) WebPAnimDecoderReset(instance->decoder);
}