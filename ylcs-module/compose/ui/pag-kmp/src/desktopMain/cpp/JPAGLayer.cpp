#include "JPAGLayer.h"

using namespace pag;

static inline std::shared_ptr<PAGLayer> obj_cast(jlong handle) {
    auto jPagLayer = reinterpret_cast<JPAGLayer*>(handle);
    return jPagLayer ? jPagLayer->get() : nullptr;
}

extern "C" {
    JNIEXPORT void JNICALL Java_org_libpag_PAGLayer_nativeRelease(JNIEnv* env, jclass, jlong handle) {
        auto jPagLayer = reinterpret_cast<JPAGLayer*>(handle);
        if (jPagLayer) delete jPagLayer;
    }

    JNIEXPORT jint JNICALL Java_org_libpag_PAGLayer_nativeLayerType(JNIEnv* env, jclass, jlong handle) {
        auto pagLayer = obj_cast(handle);
        if (pagLayer) {
            auto type = pagLayer->layerType();
            if (type == LayerType::PreCompose && std::static_pointer_cast<PAGComposition>(pagLayer)->isPAGFile()) return 114514;
            return static_cast<jint>(type);
        }
        return static_cast<jint>(LayerType::Unknown);
    }

    JNIEXPORT jstring JNICALL Java_org_libpag_PAGLayer_nativeLayerName(JNIEnv* env, jclass, jlong handle) {
        auto pagLayer = obj_cast(handle);
        return s2j(env, pagLayer ? pagLayer->layerName() : std::string{});
    }

    JNIEXPORT jfloatArray JNICALL Java_org_libpag_PAGLayer_nativeGetMatrix(JNIEnv* env, jclass, jlong handle) {
        auto pagLayer = obj_cast(handle);
        float values[9];
        auto result = env->NewFloatArray(9);
        if (pagLayer) {
            auto matrix = pagLayer->matrix();
            matrix.get9(values);
        }
        else {
            Matrix matrix{ };
            matrix.setIdentity();
            matrix.get9(values);
        }
        env->SetFloatArrayRegion(result, 0, 9, values);
        return result;
    }

    JNIEXPORT void JNICALL Java_org_libpag_PAGLayer_nativeSetMatrix(JNIEnv* env, jclass, jlong handle, jfloatArray arr) {
        auto pagLayer = obj_cast(handle);
        if (pagLayer) {
            float values[9];
            env->GetFloatArrayRegion(arr, 0, 9, values);
            Matrix matrix{ };
            matrix.set9(values);
            pagLayer->setMatrix(matrix);
        }
    }

    JNIEXPORT void JNICALL Java_org_libpag_PAGLayer_nativeResetMatrix(JNIEnv* env, jclass, jlong handle) {
        auto pagLayer = obj_cast(handle);
        if (pagLayer) pagLayer->resetMatrix();
    }

    JNIEXPORT jfloatArray JNICALL Java_org_libpag_PAGLayer_nativeGetTotalMatrix(JNIEnv* env, jclass, jlong handle) {
        auto pagLayer = obj_cast(handle);
        float values[9];
        auto result = env->NewFloatArray(9);
        if (pagLayer) {
            auto matrix = pagLayer->getTotalMatrix();
            matrix.get9(values);
        }
        else {
            Matrix matrix{ };
            matrix.setIdentity();
            matrix.get9(values);
        }
        env->SetFloatArrayRegion(result, 0, 9, values);
        return result;
    }

    JNIEXPORT jboolean JNICALL Java_org_libpag_PAGLayer_nativeVisible(JNIEnv* env, jclass, jlong handle) {
        auto pagLayer = obj_cast(handle);
        return pagLayer ? static_cast<jboolean>(pagLayer->visible()) : JNI_FALSE;
    }

    JNIEXPORT void JNICALL Java_org_libpag_PAGLayer_nativeSetVisible(JNIEnv* env, jclass, jlong handle, jboolean value) {
        auto pagLayer = obj_cast(handle);
        if (pagLayer) pagLayer->setVisible(value);
    }

    JNIEXPORT jint JNICALL Java_org_libpag_PAGLayer_nativeEditableIndex(JNIEnv* env, jclass, jlong handle) {
        auto pagLayer = obj_cast(handle);
        return pagLayer ? pagLayer->editableIndex() : -1;
    }

    JNIEXPORT jlong JNICALL Java_org_libpag_PAGLayer_nativeLocalTimeToGlobal(JNIEnv* env, jclass, jlong handle, jlong time) {
        auto pagLayer = obj_cast(handle);
        return pagLayer ? pagLayer->localTimeToGlobal(time) : time;
    }

    JNIEXPORT jlong JNICALL Java_org_libpag_PAGLayer_nativeGlobalToLocalTime(JNIEnv* env, jclass, jlong handle, jlong time) {
        auto pagLayer = obj_cast(handle);
        return pagLayer ? pagLayer->globalToLocalTime(time) : time;
    }

    JNIEXPORT jlong JNICALL Java_org_libpag_PAGLayer_nativeDuration(JNIEnv* env, jclass, jlong handle) {
        auto pagLayer = obj_cast(handle);
        return pagLayer ? pagLayer->duration() : 0LL;
    }

    JNIEXPORT jfloat JNICALL Java_org_libpag_PAGLayer_nativeFrameRate(JNIEnv* env, jclass, jlong handle) {
        auto pagLayer = obj_cast(handle);
        return pagLayer ? pagLayer->frameRate() : 60.0F;
    }

    JNIEXPORT jlong JNICALL Java_org_libpag_PAGLayer_nativeStartTime(JNIEnv* env, jclass, jlong handle) {
        auto pagLayer = obj_cast(handle);
        return pagLayer ? pagLayer->startTime() : 0LL;
    }

    JNIEXPORT void JNICALL Java_org_libpag_PAGLayer_nativeSetStartTime(JNIEnv* env, jclass, jlong handle, jlong time) {
        auto pagLayer = obj_cast(handle);
        if (pagLayer) pagLayer->setStartTime(time);
    }

    JNIEXPORT jlong JNICALL Java_org_libpag_PAGLayer_nativeCurrentTime(JNIEnv* env, jclass, jlong handle) {
        auto pagLayer = obj_cast(handle);
        return pagLayer ? pagLayer->currentTime() : 0LL;
    }

    JNIEXPORT void JNICALL Java_org_libpag_PAGLayer_nativeSetCurrentTime(JNIEnv* env, jclass, jlong handle, jlong time) {
        auto pagLayer = obj_cast(handle);
        if (pagLayer) pagLayer->setCurrentTime(time);
    }

    JNIEXPORT jdouble JNICALL Java_org_libpag_PAGLayer_nativeGetProgress(JNIEnv* env, jclass, jlong handle) {
        auto pagLayer = obj_cast(handle);
        return pagLayer ? pagLayer->getProgress() : 0.0;
    }

    JNIEXPORT void JNICALL Java_org_libpag_PAGLayer_nativeSetProgress(JNIEnv* env, jclass, jlong handle, jdouble progress) {
        auto pagLayer = obj_cast(handle);
        if (pagLayer) pagLayer->setProgress(progress);
    }

    JNIEXPORT jfloatArray JNICALL Java_org_libpag_PAGLayer_nativeGetBounds(JNIEnv* env, jclass, jlong handle) {
        auto pagLayer = obj_cast(handle);
        auto result = env->NewFloatArray(4);
        if (pagLayer) {
            auto rect = pagLayer->getBounds();
            float values[4] = { rect.x(), rect.y(), rect.width(), rect.height() };
            env->SetFloatArrayRegion(result, 0, 4, values);
        }
        return result;
    }

    JNIEXPORT jboolean JNICALL Java_org_libpag_PAGLayer_nativeExcludedFromTimeline(JNIEnv* env, jclass, jlong handle) {
        auto pagLayer = obj_cast(handle);
        return pagLayer ? static_cast<jboolean>(pagLayer->excludedFromTimeline()) : JNI_FALSE;
    }

    JNIEXPORT void JNICALL Java_org_libpag_PAGLayer_nativeSetExcludedFromTimeline(JNIEnv* env, jclass, jlong handle, jboolean value) {
        auto pagLayer = obj_cast(handle);
        if (pagLayer) pagLayer->setExcludedFromTimeline(value);
    }

    JNIEXPORT jfloat JNICALL Java_org_libpag_PAGLayer_nativeAlpha(JNIEnv* env, jclass, jlong handle) {
        auto pagLayer = obj_cast(handle);
        return pagLayer ? pagLayer->alpha() : 0.0f;
    }

    JNIEXPORT void JNICALL Java_org_libpag_PAGLayer_nativeSetAlpha(JNIEnv* env, jclass, jlong handle, jfloat alpha) {
        auto pagLayer = obj_cast(handle);
        if (pagLayer) pagLayer->setAlpha(alpha);
    }
}