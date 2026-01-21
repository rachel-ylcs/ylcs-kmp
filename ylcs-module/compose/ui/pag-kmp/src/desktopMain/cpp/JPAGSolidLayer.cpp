#include "JPAGSolidLayer.h"

using namespace pag;

static inline std::shared_ptr<PAGSolidLayer> obj_cast(jlong handle) {
    auto jPagLayer = reinterpret_cast<JPAGSolidLayer*>(handle);
    return jPagLayer ? jPagLayer->get() : nullptr;
}

extern "C" {
    JNIEXPORT jlong JNICALL Java_org_libpag_PAGSolidLayer_nativeMake(JNIEnv* env, jclass, jlong duration, jint width, jint height, jint solid_color, jint opacity) {
        auto jPagLayer = PAGSolidLayer::Make(duration, width, height, ToColor(solid_color), opacity);
        return jPagLayer ? reinterpret_cast<jlong>(new JPAGSolidLayer(jPagLayer)) : 0LL;
    }

    JNIEXPORT void JNICALL Java_org_libpag_PAGSolidLayer_nativeRelease(JNIEnv* env, jclass, jlong handle) {
        auto jPagLayer = reinterpret_cast<JPAGSolidLayer*>(handle);
        if (jPagLayer) delete jPagLayer;
    }

    JNIEXPORT jint JNICALL Java_org_libpag_PAGSolidLayer_nativeSolidColor(JNIEnv* env, jclass, jlong handle) {
        auto pagLayer = obj_cast(handle);
        if (pagLayer) {
            auto color = pagLayer->solidColor();
            return MakeColorInt(color.red, color.green, color.blue);
        }
        return 0;
    }

    JNIEXPORT void JNICALL Java_org_libpag_PAGSolidLayer_nativeSetSolidColor(JNIEnv* env, jclass, jlong handle, jint color) {
        auto pagLayer = obj_cast(handle);
        if (pagLayer) pagLayer->setSolidColor(ToColor(color));
    }
}