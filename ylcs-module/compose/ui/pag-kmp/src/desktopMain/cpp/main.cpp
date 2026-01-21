#include <native_jni.h>
#include <pag.h>

extern "C" {
    JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* vm, void* reserved) {
        return JNI_VERSION_1_6;
    }

    JNIEXPORT jstring JNICALL Java_org_libpag_PAG_nativeSDKVersion(JNIEnv* env, jclass) {
        return s2j(env, pag::PAG::SDKVersion());
    }
}