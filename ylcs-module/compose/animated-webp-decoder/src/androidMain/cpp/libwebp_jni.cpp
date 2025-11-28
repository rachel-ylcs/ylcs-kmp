#include <jni.h>
#include <webp/demux.h>

struct AnimatedWebpDecoder {
    unsigned char* data = nullptr;
    WebPDemuxer* demux = nullptr;

    AnimatedWebpDecoder(unsigned char* buffer, WebPDemuxer* instance) {
        data = buffer;
        demux = instance;
    }

    ~AnimatedWebpDecoder() {
        if (data) delete[] data;
        if (demux) WebPDemuxDelete(demux);
    }
};

extern "C" {
    JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* vm, void* reserved) {
        JNIEnv* env = nullptr;
        if (vm->GetEnv((void**)&env, JNI_VERSION_1_6) != JNI_OK) return -1;
        return JNI_VERSION_1_6;
    }

    JNIEXPORT jlong JNICALL Java_love_yinlin_compose_graphics_NativeAnimatedWebpKt_nativeAnimatedWebpCreate(JNIEnv* env, jclass, jbyteArray data) {
        auto size = env->GetArrayLength(data);
        auto* buffer = new unsigned char[size];
        env->GetByteArrayRegion(data, 0, size, reinterpret_cast<jbyte*>(buffer));
        WebPData webpData { buffer, static_cast<size_t>(size) };
        WebPDemuxer* demux = WebPDemux(&webpData);
        if (demux) return reinterpret_cast<jlong>(new AnimatedWebpDecoder(buffer, demux));
        delete[] buffer;
        return 0L;
    }

    JNIEXPORT void JNICALL Java_love_yinlin_compose_graphics_NativeAnimatedWebpKt_nativeAnimatedWebpRelease(JNIEnv* env, jclass, jlong handle) {
        auto* decoder = reinterpret_cast<AnimatedWebpDecoder*>(handle);
        if (decoder) delete decoder;
    }

    JNIEXPORT jint JNICALL Java_love_yinlin_compose_graphics_NativeAnimatedWebpKt_nativeAnimatedWebpGetWidth(JNIEnv* env, jclass, jlong handle) {
        auto* decoder = reinterpret_cast<AnimatedWebpDecoder*>(handle);
        if (decoder) return static_cast<jint>(WebPDemuxGetI(decoder->demux, WEBP_FF_CANVAS_WIDTH));
        return 0;
    }

    JNIEXPORT jint JNICALL Java_love_yinlin_compose_graphics_NativeAnimatedWebpKt_nativeAnimatedWebpGetHeight(JNIEnv* env, jclass, jlong handle) {
        auto* decoder = reinterpret_cast<AnimatedWebpDecoder*>(handle);
        if (decoder) return static_cast<jint>(WebPDemuxGetI(decoder->demux, WEBP_FF_CANVAS_HEIGHT));
        return 0;
    }

    JNIEXPORT jint JNICALL Java_love_yinlin_compose_graphics_NativeAnimatedWebpKt_nativeAnimatedWebpGetFrameCount(JNIEnv* env, jclass, jlong handle) {
        auto* decoder = reinterpret_cast<AnimatedWebpDecoder*>(handle);
        if (decoder) return static_cast<jint>(WebPDemuxGetI(decoder->demux, WEBP_FF_FRAME_COUNT));
        return 0;
    }

    JNIEXPORT jlong JNICALL Java_love_yinlin_compose_graphics_NativeAnimatedWebpKt_nativeAnimatedWebpCreateIterator(JNIEnv* env, jclass) {
        return reinterpret_cast<jlong>(new WebPIterator);
    }

    JNIEXPORT void JNICALL Java_love_yinlin_compose_graphics_NativeAnimatedWebpKt_nativeAnimatedWebpReleaseIterator(JNIEnv* env, jclass, jlong iterator_handle) {
        auto* iterator = reinterpret_cast<WebPIterator*>(iterator_handle);
        if (iterator) {
            WebPDemuxReleaseIterator(iterator);
            delete iterator;
        }
    }

    JNIEXPORT jboolean JNICALL Java_love_yinlin_compose_graphics_NativeAnimatedWebpKt_nativeAnimatedWebpFirstFrame(JNIEnv* env, jclass, jlong handle, jlong iterator_handle) {
        auto* decoder = reinterpret_cast<AnimatedWebpDecoder*>(handle);
        auto* iterator = reinterpret_cast<WebPIterator*>(iterator_handle);
        return static_cast<jboolean>(decoder && iterator ? WebPDemuxGetFrame(decoder->demux, 1, iterator) : false);
    }

    JNIEXPORT jboolean JNICALL Java_love_yinlin_compose_graphics_NativeAnimatedWebpKt_nativeAnimatedWebpNextFrame(JNIEnv* env, jclass, jlong iterator_handle) {
        auto* iterator = reinterpret_cast<WebPIterator*>(iterator_handle);
        return static_cast<jboolean>(iterator ? WebPDemuxNextFrame(iterator) : false);
    }
}