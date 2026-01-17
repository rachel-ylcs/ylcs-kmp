#include "JPAGImage.h"

using namespace pag;

extern "C" {
    JNIEXPORT jlong JNICALL Java_org_libpag_PAGImage_nativeLoadFromPath(JNIEnv* env, jclass, jstring path) {
        auto pathStr = j2s(env, path);
        if (pathStr.empty()) return 0L;
        auto pagImage = PAGImage::FromPath(pathStr);
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
}