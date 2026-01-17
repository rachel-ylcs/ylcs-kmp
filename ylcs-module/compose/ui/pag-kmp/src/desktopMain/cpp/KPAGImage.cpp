#include <native_jni.h>
#include <pag.h>

extern "C" {
    JNIEXPORT jlong JNICALL Java_org_libpag_PAGImage_nativeLoadFromPath(JNIEnv* env, jclass, jstring path) {
        auto pathStr = j2s(path);
        if (pathStr.empty()) return 0L;
        auto pagImage = PAGImage::FromPath(pathStr);
        return pagImage ? reinterpret_cast<jlong>(new JPAGImage(pagImage))
        if (pagImage) {
            return 0;
        }
        return ;
    }
}