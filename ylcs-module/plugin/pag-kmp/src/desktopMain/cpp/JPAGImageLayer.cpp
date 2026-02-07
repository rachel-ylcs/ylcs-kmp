#include "JPAGImageLayer.h"
#include "JPAGImage.h"

using namespace pag;

static inline std::shared_ptr<PAGImageLayer> obj_cast(jlong handle) {
    auto jPagLayer = reinterpret_cast<JPAGImageLayer*>(handle);
    return jPagLayer ? jPagLayer->get() : nullptr;
}

extern "C" {
    JNIEXPORT jlong JNICALL Java_org_libpag_PAGImageLayer_nativeMake(JNIEnv* env, jclass, jint width, jint height, jlong duration) {
        auto jPagLayer = PAGImageLayer::Make(width, height, duration);
        return jPagLayer ? reinterpret_cast<jlong>(new JPAGImageLayer(jPagLayer)) : 0LL;
    }

    JNIEXPORT void JNICALL Java_org_libpag_PAGImageLayer_nativeRelease(JNIEnv* env, jclass, jlong handle) {
        auto jPagLayer = reinterpret_cast<JPAGImageLayer*>(handle);
        if (jPagLayer) delete jPagLayer;
    }

    JNIEXPORT jlong JNICALL Java_org_libpag_PAGImageLayer_nativeContentDuration(JNIEnv* env, jclass, jlong handle) {
        auto pagLayer = obj_cast(handle);
        return pagLayer ? pagLayer->contentDuration() : 0LL;
    }

    JNIEXPORT void JNICALL Java_org_libpag_PAGImageLayer_nativeReplaceImage(JNIEnv* env, jclass, jlong handle, jlong imageHandle) {
        auto pagLayer = obj_cast(handle);
        if (pagLayer) {
            auto image = reinterpret_cast<JPAGImage*>(imageHandle);
            pagLayer->replaceImage(image ? image->get() : nullptr);
        }
    }

    JNIEXPORT void JNICALL Java_org_libpag_PAGImageLayer_nativeSetImage(JNIEnv* env, jclass, jlong handle, jlong imageHandle) {
        auto pagLayer = obj_cast(handle);
        if (pagLayer) {
            auto image = reinterpret_cast<JPAGImage*>(imageHandle);
            pagLayer->setImage(image ? image->get() : nullptr);
        }
    }

    JNIEXPORT jlong JNICALL Java_org_libpag_PAGImageLayer_nativeLayerTimeToContent(JNIEnv* env, jclass, jlong handle, jlong time) {
        auto pagLayer = obj_cast(handle);
        return pagLayer ? pagLayer->layerTimeToContent(time) : 0LL;
    }

    JNIEXPORT jlong JNICALL Java_org_libpag_PAGImageLayer_nativeContentTimeToLayer(JNIEnv* env, jclass, jlong handle, jlong time) {
        auto pagLayer = obj_cast(handle);
        return pagLayer ? pagLayer->contentTimeToLayer(time) : 0LL;
    }

    JNIEXPORT jobject JNICALL Java_org_libpag_PAGImageLayer_nativeImageBytes(JNIEnv* env, jclass, jlong handle) {
        auto pagLayer = obj_cast(handle);
        if (pagLayer) {
            auto bytes = pagLayer->imageBytes();
            return env->NewDirectByteBuffer(bytes->data(), bytes->length());
        }
        return nullptr;
    }
}