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
        return pagLayer ? static_cast<jint>(pagLayer->layerType()) : static_cast<jint>(LayerType::Unknown);
    }

    JNIEXPORT jstring JNICALL Java_org_libpag_PAGLayer_nativeLayerName(JNIEnv* env, jclass, jlong handle) {
        auto pagLayer = obj_cast(handle);
        return s2j(env, pagLayer ? pagLayer->layerName() : std::string{});
    }

    JNIEXPORT void JNICALL Java_org_libpag_PAGLayer_nativeGetMatrix(JNIEnv* env, jclass, jlong handle, jfloatArray arr) {
        auto pagLayer = obj_cast(handle);
        float values[9];
        if (pagLayer) {
            auto matrix = pagLayer->matrix();
            matrix.get9(values);
        }
        else {
            Matrix matrix{ };
            matrix.setIdentity();
            matrix.get9(values);
        }
        env->SetFloatArrayRegion(arr, 0, 9, values);
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

    JNIEXPORT void JNICALL Java_org_libpag_PAGLayer_nativeGetTotalMatrix(JNIEnv* env, jclass, jlong handle, jfloatArray arr) {
        auto pagLayer = obj_cast(handle);
        float values[9];
        if (pagLayer) {
            auto matrix = pagLayer->getTotalMatrix();
            matrix.get9(values);
        }
        else {
            Matrix matrix{ };
            matrix.setIdentity();
            matrix.get9(values);
        }
        env->SetFloatArrayRegion(arr, 0, 9, values);
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
}