#include "JPAGSurface.h"

using namespace pag;

static inline std::shared_ptr<PAGSurface> obj_cast(jlong handle) {
    auto jPagSurface = reinterpret_cast<JPAGSurface*>(handle);
    return jPagSurface ? jPagSurface->get() : nullptr;
}

extern "C" {
    JNIEXPORT void JNICALL Java_org_libpag_PAGSurface_nativeClear(JNIEnv* env, jclass, jlong handle) {
        auto jPagSurface = reinterpret_cast<JPAGSurface*>(handle);
        if (jPagSurface) jPagSurface->clear();
    }

    JNIEXPORT void JNICALL Java_org_libpag_PAGSurface_nativeRelease(JNIEnv* env, jclass, jlong handle) {
        auto jPagSurface = reinterpret_cast<JPAGSurface*>(handle);
        if (jPagSurface) delete jPagSurface;
    }

    JNIEXPORT jint JNICALL Java_org_libpag_PAGSurface_nativeWidth(JNIEnv* env, jclass, jlong handle) {
        auto pagSurface = obj_cast(handle);
        return pagSurface ? pagSurface->width() : 0;
    }

    JNIEXPORT jint JNICALL Java_org_libpag_PAGSurface_nativeHeight(JNIEnv* env, jclass, jlong handle) {
        auto pagSurface = obj_cast(handle);
        return pagSurface ? pagSurface->height() : 0;
    }

    JNIEXPORT void JNICALL Java_org_libpag_PAGSurface_nativeUpdateSize(JNIEnv* env, jclass, jlong handle) {
        auto pagSurface = obj_cast(handle);
        if (pagSurface) pagSurface->updateSize();
    }

    JNIEXPORT void JNICALL Java_org_libpag_PAGSurface_nativeClearAll(JNIEnv* env, jclass, jlong handle) {
        auto pagSurface = obj_cast(handle);
        if (pagSurface) pagSurface->clearAll();
    }

    JNIEXPORT void JNICALL Java_org_libpag_PAGSurface_nativeFreeCache(JNIEnv* env, jclass, jlong handle) {
        auto pagSurface = obj_cast(handle);
        if (pagSurface) pagSurface->freeCache();
    }

    JNIEXPORT jboolean JNICALL Java_org_libpag_PAGSurface_nativeReadPixels(JNIEnv* env, jclass, jlong handle, jint color_type, jint alpha_type, jbyteArray bytes) {
        auto pagSurface = obj_cast(handle);
        if (pagSurface) {
            auto data = env->GetPrimitiveArrayCritical(bytes, nullptr);
            if (data) {
                pagSurface->readPixels(static_cast<ColorType>(color_type), static_cast<AlphaType>(alpha_type), data, static_cast<size_t>(pagSurface->width()));
                env->ReleasePrimitiveArrayCritical(bytes, data, 0);
                return JNI_TRUE;
            }
        }
        return JNI_FALSE;
    }
}