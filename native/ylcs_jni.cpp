#include "ylcs_jni.h"
#include "ylcs_init.h"

extern "C" {
    JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* vm, void* reserved)
    {
        JVM::vm = vm;
        JVM::JniEnvGuard guard;
        if (guard) {
#ifdef WIN32
            Initialize_AudioPlayer(vm, guard.env);
            Initialize_VideoPlayer(vm, guard.env);
#endif
            return JNI_VERSION_1_6;
        }
        return -1;
    }
}