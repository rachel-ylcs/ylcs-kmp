#include "JPAGDecoder.h"
#include "JPAGComposition.h"

using namespace pag;

static inline std::shared_ptr<PAGDecoder> obj_cast(jlong handle) {
    auto jPagDecoder = reinterpret_cast<JPAGDecoder*>(handle);
    return jPagDecoder ? jPagDecoder->get() : nullptr;
}

extern "C" {
    JNIEXPORT jlong JNICALL Java_org_libpag_PAGDecoder_nativeMakeFrom(JNIEnv* env, jclass, jlong composition_handle, jfloat max_frame_rate, jfloat scale) {
        auto composition = reinterpret_cast<JPAGComposition*>(composition_handle);
        if (composition) {
            auto jPagDecoder = PAGDecoder::MakeFrom(composition->get(), max_frame_rate, scale);
            if (jPagDecoder) return reinterpret_cast<jlong>(new JPAGDecoder(jPagDecoder));
        }
        return 0LL;
    }

    JNIEXPORT void JNICALL Java_org_libpag_PAGDecoder_nativeClear(JNIEnv* env, jclass, jlong handle) {
        auto jPagDecoder = reinterpret_cast<JPAGDecoder*>(handle);
        if (jPagDecoder) jPagDecoder->clear();
    }

    JNIEXPORT void JNICALL Java_org_libpag_PAGDecoder_nativeRelease(JNIEnv* env, jclass, jlong handle) {
        auto jPagDecoder = reinterpret_cast<JPAGDecoder*>(handle);
        if (jPagDecoder) delete jPagDecoder;
    }

    JNIEXPORT jint JNICALL Java_org_libpag_PAGDecoder_nativeWidth(JNIEnv* env, jclass, jlong handle) {
        auto pagDecoder = obj_cast(handle);
        return pagDecoder ? pagDecoder->width() : 0;
    }

    JNIEXPORT jint JNICALL Java_org_libpag_PAGDecoder_nativeHeight(JNIEnv* env, jclass, jlong handle) {
        auto pagDecoder = obj_cast(handle);
        return pagDecoder ? pagDecoder->height() : 0;
    }

    JNIEXPORT jint JNICALL Java_org_libpag_PAGDecoder_nativeNumFrames(JNIEnv* env, jclass, jlong handle) {
        auto pagDecoder = obj_cast(handle);
        return pagDecoder ? pagDecoder->numFrames() : 0;
    }

    JNIEXPORT jfloat JNICALL Java_org_libpag_PAGDecoder_nativeFrameRate(JNIEnv* env, jclass, jlong handle) {
        auto pagDecoder = obj_cast(handle);
        return pagDecoder ? pagDecoder->frameRate() : 0.0F;
    }

    JNIEXPORT jboolean JNICALL Java_org_libpag_PAGDecoder_nativeCheckFrameChanged(JNIEnv* env, jclass, jlong handle, jint index) {
        auto pagDecoder = obj_cast(handle);
        return pagDecoder ? static_cast<jboolean>(pagDecoder->checkFrameChanged(index)) : JNI_FALSE;
    }

    JNIEXPORT jboolean JNICALL Java_org_libpag_PAGSurface_nativeReadFrame(JNIEnv* env, jclass, jlong handle, jint index, jint color_type, jint alpha_type, jlong row_bytes, jbyteArray container) {
        auto pagDecoder = obj_cast(handle);
        if (pagDecoder) {
            auto data = env->GetPrimitiveArrayCritical(container, nullptr);
            if (data) {
                pagDecoder->readFrame(index, data, row_bytes, static_cast<ColorType>(color_type), static_cast<AlphaType>(alpha_type));
                env->ReleasePrimitiveArrayCritical(container, data, 0);
                return JNI_TRUE;
            }
        }
        return JNI_FALSE;
    }
}