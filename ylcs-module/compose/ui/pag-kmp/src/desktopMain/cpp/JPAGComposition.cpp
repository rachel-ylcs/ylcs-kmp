#include "JPAGComposition.h"

using namespace pag;

static inline std::shared_ptr<PAGComposition> obj_cast(jlong handle) {
    auto jPagLayer = reinterpret_cast<JPAGComposition*>(handle);
    return jPagLayer ? jPagLayer->get() : nullptr;
}

extern "C" {
    JNIEXPORT jlong JNICALL Java_org_libpag_PAGComposition_nativeMake(JNIEnv* env, jclass, jint width, jint height) {
        auto jPagLayer = PAGComposition::Make(width, height);
        return jPagLayer ? reinterpret_cast<jlong>(new JPAGComposition(jPagLayer)) : 0LL;
    }

    JNIEXPORT void JNICALL Java_org_libpag_PAGComposition_nativeRelease(JNIEnv* env, jclass, jlong handle) {
        auto jPagLayer = reinterpret_cast<JPAGComposition*>(handle);
        if (jPagLayer) delete jPagLayer;
    }

    JNIEXPORT jint JNICALL Java_org_libpag_PAGComposition_nativeWidth(JNIEnv* env, jclass, jlong handle) {
        auto pagLayer = obj_cast(handle);
        return pagLayer ? pagLayer->width() : 0;
    }

    JNIEXPORT jint JNICALL Java_org_libpag_PAGComposition_nativeHeight(JNIEnv* env, jclass, jlong handle) {
        auto pagLayer = obj_cast(handle);
        return pagLayer ? pagLayer->height() : 0;
    }

    JNIEXPORT void JNICALL Java_org_libpag_PAGComposition_nativeSetContentSize(JNIEnv* env, jclass, jlong handle, jint width, jint height) {
        auto pagLayer = obj_cast(handle);
        if (pagLayer) pagLayer->setContentSize(width, height);
    }

    JNIEXPORT jint JNICALL Java_org_libpag_PAGComposition_nativeNumChildren(JNIEnv* env, jclass, jlong handle) {
        auto pagLayer = obj_cast(handle);
        return pagLayer ? pagLayer->numChildren() : 0;
    }

    JNIEXPORT jlongArray JNICALL Java_org_libpag_PAGComposition_nativeGetLayerAt(JNIEnv* env, jclass, jlong handle, jint index) {
        auto pagLayer = obj_cast(handle);
        auto result = env->NewLongArray(2);
        if (pagLayer) env->SetLongArrayRegion(result, 0, 2, JPAGLayerInstance(pagLayer->getLayerAt(index)));
        return result;
    }

    JNIEXPORT jint JNICALL Java_org_libpag_PAGComposition_nativeGetLayerIndex(JNIEnv* env, jclass, jlong handle, jlong layerHandle, jlong type) {
        auto pagLayer = obj_cast(handle);
        if (pagLayer) {
            auto subLayer = PAGLayerInstance(LayerInfo(layerHandle, type));
            if (subLayer) return pagLayer->getLayerIndex(subLayer);
        }
        return -1;
    }

    JNIEXPORT void JNICALL Java_org_libpag_PAGComposition_nativeSetLayerIndex(JNIEnv* env, jclass, jlong handle, jlong layerHandle, jlong type, jint index) {
        auto pagLayer = obj_cast(handle);
        if (pagLayer) {
            auto subLayer = PAGLayerInstance(LayerInfo(layerHandle, type));
            if (subLayer) pagLayer->setLayerIndex(subLayer, index);
        }
    }

    JNIEXPORT void JNICALL Java_org_libpag_PAGComposition_nativeAddLayer(JNIEnv* env, jclass, jlong handle, jlong layerHandle, jlong type) {
        auto pagLayer = obj_cast(handle);
        if (pagLayer) {
            auto subLayer = PAGLayerInstance(LayerInfo(layerHandle, type));
            if (subLayer) pagLayer->addLayer(subLayer);
        }
    }

    JNIEXPORT void JNICALL Java_org_libpag_PAGComposition_nativeAddLayerAt(JNIEnv* env, jclass, jlong handle, jlong layerHandle, jlong type, jint index) {
        auto pagLayer = obj_cast(handle);
        if (pagLayer) {
            auto subLayer = PAGLayerInstance(LayerInfo(layerHandle, type));
            if (subLayer) pagLayer->addLayerAt(subLayer, index);
        }
    }

    JNIEXPORT jboolean JNICALL Java_org_libpag_PAGComposition_nativeContains(JNIEnv* env, jclass, jlong handle, jlong layerHandle, jlong type) {
        auto pagLayer = obj_cast(handle);
        if (pagLayer) {
            auto subLayer = PAGLayerInstance(LayerInfo(layerHandle, type));
            if (subLayer) static_cast<jboolean>(pagLayer->contains(subLayer));
        }
        return JNI_FALSE;
    }

    JNIEXPORT jlongArray JNICALL Java_org_libpag_PAGComposition_nativeRemoveLayer(JNIEnv* env, jclass, jlong handle, jlong layerHandle, jlong type) {
        auto pagLayer = obj_cast(handle);
        auto result = env->NewLongArray(2);
        if (pagLayer) {
            auto subLayer = PAGLayerInstance(LayerInfo(layerHandle, type));
            if (subLayer) env->SetLongArrayRegion(result, 0, 2, JPAGLayerInstance(pagLayer->removeLayer(subLayer)));
        }
        return result;
    }

    JNIEXPORT jlongArray JNICALL Java_org_libpag_PAGComposition_nativeRemoveLayerAt(JNIEnv* env, jclass, jlong handle, jint index) {
        auto pagLayer = obj_cast(handle);
        auto result = env->NewLongArray(2);
        if (pagLayer) env->SetLongArrayRegion(result, 0, 2, JPAGLayerInstance(pagLayer->removeLayerAt(index)));
        return result;
    }

    JNIEXPORT void JNICALL Java_org_libpag_PAGComposition_nativeRemoveAllLayers(JNIEnv* env, jclass, jlong handle) {
        auto pagLayer = obj_cast(handle);
        if (pagLayer) pagLayer->removeAllLayers();
    }

    JNIEXPORT void JNICALL Java_org_libpag_PAGComposition_nativeSwapLayer(JNIEnv* env, jclass, jlong handle, jlong layerHandle1, jlong type1, jlong layerHandle2, jlong type2) {
        auto pagLayer = obj_cast(handle);
        if (pagLayer) {
            auto subLayer1 = PAGLayerInstance(LayerInfo(layerHandle1, type1));
            if (subLayer1) {
                auto subLayer2 = PAGLayerInstance(LayerInfo(layerHandle2, type2));
                if (subLayer2) {
                    pagLayer->swapLayer(subLayer1, subLayer2);
                }
            }
        }
    }

    JNIEXPORT void JNICALL Java_org_libpag_PAGComposition_nativeSwapLayerAt(JNIEnv* env, jclass, jlong handle, jint index1, jint index2) {
        auto pagLayer = obj_cast(handle);
        if (pagLayer) pagLayer->swapLayerAt(index1, index2);
    }

    JNIEXPORT jbyteArray JNICALL Java_org_libpag_PAGComposition_nativeAudioBytes(JNIEnv* env, jclass, jlong handle) {
        auto pagLayer = obj_cast(handle);
        if (pagLayer) {
            auto audio = pagLayer->audioBytes();
            auto length = static_cast<jsize>(audio->length());
            if (audio) {
                jbyteArray result = env->NewByteArray(length);
                env->SetByteArrayRegion(result, 0, length, reinterpret_cast<const jbyte*>(audio->data()));
                return result;
            }
        }
        return nullptr;
    }

    JNIEXPORT jlong JNICALL Java_org_libpag_PAGComposition_nativeAudioStartTime(JNIEnv* env, jclass, jlong handle) {
        auto pagLayer = obj_cast(handle);
        return pagLayer ? pagLayer->audioStartTime() : 0LL;
    }
}