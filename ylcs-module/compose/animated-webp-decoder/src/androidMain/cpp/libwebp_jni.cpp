#include <jni.h>
#include <webp/demux.h>

struct AnimatedWebpDecoder {
    unsigned char* data = nullptr;
    WebPAnimDecoder* decoder = nullptr;
    int width = 0;
    int height = 0;
    int frameCount = 0;

    AnimatedWebpDecoder(unsigned char* data, WebPAnimDecoder* decoder, int width, int height, int frameCount) :
        data(data), decoder(decoder), width(width), height(height), frameCount(frameCount) { }

    ~AnimatedWebpDecoder() {
        if (data) delete[] data;
        if (decoder) WebPAnimDecoderDelete(decoder);
    }
};

extern "C" {
    JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* vm, void* reserved) {
        JNIEnv* env = nullptr;
        if (vm->GetEnv((void**)&env, JNI_VERSION_1_6) != JNI_OK) return -1;
        return JNI_VERSION_1_6;
    }

    JNIEXPORT jlong JNICALL Java_love_yinlin_compose_graphics_NativeAnimatedWebpKt_nativeAnimatedWebpCreate(JNIEnv* env, jclass, jbyteArray data) {
        WebPAnimDecoderOptions options;
        WebPAnimDecoderOptionsInit(&options);
        options.color_mode = MODE_RGBA;
        options.use_threads = 1;

        auto size = env->GetArrayLength(data);
        auto* buffer = new unsigned char[size];
        env->GetByteArrayRegion(data, 0, size, reinterpret_cast<jbyte*>(buffer));
        WebPData webp_data { buffer, static_cast<size_t>(size) };
        WebPAnimDecoder* decoder = WebPAnimDecoderNew(&webp_data, &options);
        if (decoder) {
            WebPAnimInfo info;
            if (WebPAnimDecoderGetInfo(decoder, &info)) {
                return reinterpret_cast<jlong>(new AnimatedWebpDecoder(buffer, decoder, info.canvas_width, info.canvas_height, info.frame_count));
            }
        }
        delete[] buffer;
        return 0L;
    }

    JNIEXPORT void JNICALL Java_love_yinlin_compose_graphics_NativeAnimatedWebpKt_nativeAnimatedWebpRelease(JNIEnv* env, jclass, jlong handle) {
        auto* instance = reinterpret_cast<AnimatedWebpDecoder*>(handle);
        if (instance) delete instance;
    }

    JNIEXPORT jint JNICALL Java_love_yinlin_compose_graphics_NativeAnimatedWebpKt_nativeAnimatedWebpGetWidth(JNIEnv* env, jclass, jlong handle) {
        auto* instance = reinterpret_cast<AnimatedWebpDecoder*>(handle);
        return instance ? instance->width : 0;
    }

    JNIEXPORT jint JNICALL Java_love_yinlin_compose_graphics_NativeAnimatedWebpKt_nativeAnimatedWebpGetHeight(JNIEnv* env, jclass, jlong handle) {
        auto* instance = reinterpret_cast<AnimatedWebpDecoder*>(handle);
        return instance ? instance->height : 0;
    }

    JNIEXPORT jint JNICALL Java_love_yinlin_compose_graphics_NativeAnimatedWebpKt_nativeAnimatedWebpGetFrameCount(JNIEnv* env, jclass, jlong handle) {
        auto* instance = reinterpret_cast<AnimatedWebpDecoder*>(handle);
        return instance ? instance->frameCount : 0;
    }

    JNIEXPORT jboolean JNICALL Java_love_yinlin_compose_graphics_NativeAnimatedWebpKt_nativeAnimatedWebpHasMoreFrames(JNIEnv* env, jclass, jlong handle) {
        auto* instance = reinterpret_cast<AnimatedWebpDecoder*>(handle);
        return static_cast<jboolean>(instance ? WebPAnimDecoderHasMoreFrames(instance->decoder) : false);
    }

    JNIEXPORT jboolean JNICALL Java_love_yinlin_compose_graphics_NativeAnimatedWebpKt_nativeAnimatedWebpGetNext(JNIEnv* env, jclass, jlong handle, jobject directBuffer) {
        auto* instance = reinterpret_cast<AnimatedWebpDecoder*>(handle);
        if (instance) {
            uint8_t* buf = nullptr;
            int timestamp = 0;
            WebPAnimDecoderGetNext(instance->decoder, &buf, &timestamp);
            if (buf) {
                auto* dst = env->GetDirectBufferAddress(directBuffer);
                auto size = static_cast<size_t>(env->GetDirectBufferCapacity(directBuffer));
                memcpy(dst, buf, size);
                return true;
            }
        }
        return false;
    }

    JNIEXPORT void JNICALL Java_love_yinlin_compose_graphics_NativeAnimatedWebpKt_nativeAnimatedWebpReset(JNIEnv* env, jclass, jlong handle) {
        auto* instance = reinterpret_cast<AnimatedWebpDecoder*>(handle);
        if (instance) WebPAnimDecoderReset(instance->decoder);
    }
}