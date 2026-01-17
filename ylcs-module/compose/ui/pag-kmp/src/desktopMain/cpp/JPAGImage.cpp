#include "JPAGImage.h"

using namespace pag;

inline std::shared_ptr<PAGImage> obj_cast(jlong handle) {
    auto jPagImage = reinterpret_cast<JPAGImage*>(handle);
    return jPagImage ? jPagImage->get() : nullptr;
}

extern "C" {
    JNIEXPORT jlong JNICALL Java_org_libpag_PAGImage_nativeLoadFromPath(JNIEnv* env, jclass, jstring path) {
        auto pathStr = j2s(env, path);
        if (pathStr.empty()) return 0L;
        auto pagImage = PAGImage::FromPath(pathStr);
        return pagImage ? reinterpret_cast<jlong>(new JPAGImage(pagImage)) : 0L;
    }

    JNIEXPORT jlong JNICALL Java_org_libpag_PAGImage_nativeLoadFromBytes(JNIEnv* env, jclass, jbyteArray bytes) {
        auto length = env->GetArrayLength(bytes);
        auto data = env->GetPrimitiveArrayCritical(bytes, nullptr);
        if (!data) return 0L;
        auto pagImage = PAGImage::FromBytes(data, length);
        env->ReleasePrimitiveArrayCritical(bytes, data, JNI_ABORT);
        return pagImage ? reinterpret_cast<jlong>(new JPAGImage(pagImage)) : 0L;
    }

    JNIEXPORT void JNICALL Java_org_libpag_PAGImage_nativeClear(JNIEnv* env, jclass, jlong handle) {
        auto jPagImage = reinterpret_cast<JPAGImage*>(handle);
        if (jPagImage) jPagImage->clear();
    }

    JNIEXPORT void JNICALL Java_org_libpag_PAGImage_nativeRelease(JNIEnv* env, jclass, jlong handle) {
        auto jPagImage = reinterpret_cast<JPAGImage*>(handle);
        if (jPagImage) delete jPagImage;
    }

    JNIEXPORT jint JNICALL Java_org_libpag_PAGImage_nativeWidth(JNIEnv* env, jclass, jlong handle) {
        auto pagImage = obj_cast(handle);
        return pagImage ? pagImage->width() : 0;
    }

    JNIEXPORT jint JNICALL Java_org_libpag_PAGImage_nativeHeight(JNIEnv* env, jclass, jlong handle) {
        auto pagImage = obj_cast(handle);
        return pagImage ? pagImage->height() : 0;
    }

    JNIEXPORT jint JNICALL Java_org_libpag_PAGImage_nativeScaleMode(JNIEnv* env, jclass, jlong handle) {
        auto pagImage = obj_cast(handle);
        return pagImage ? static_cast<jint>(pagImage->scaleMode()) : 0;
    }

    JNIEXPORT void JNICALL Java_org_libpag_PAGImage_nativeSetScaleMode(JNIEnv* env, jclass, jlong handle, jint value) {
        auto pagImage = obj_cast(handle);
        if (pagImage) pagImage->setScaleMode(static_cast<PAGScaleMode>(value));
    }

    JNIEXPORT void JNICALL Java_org_libpag_PAGImage_nativeGetMatrix(JNIEnv* env, jclass, jlong handle, jfloatArray arr) {
        auto pagImage = obj_cast(handle);
        float values[9];
        if (pagImage) {
            auto matrix = pagImage->matrix();
            matrix.get9(values);
        }
        else {
            Matrix matrix{ };
            matrix.setIdentity();
            matrix.get9(values);
        }
        env->SetFloatArrayRegion(arr, 0, 9, values);
    }

    JNIEXPORT void JNICALL Java_org_libpag_PAGImage_nativeSetMatrix(JNIEnv* env, jclass, jlong handle, jfloatArray arr) {
        auto pagImage = obj_cast(handle);
        if (pagImage) {
            float values[9];
            env->GetFloatArrayRegion(arr, 0, 9, values);
            Matrix matrix{ };
            matrix.set9(values);
            pagImage->setMatrix(matrix);
        }
    }
}