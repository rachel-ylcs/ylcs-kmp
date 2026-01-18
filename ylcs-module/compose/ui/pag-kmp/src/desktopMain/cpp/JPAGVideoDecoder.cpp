#include <native_jni.h>
#include <pag.h>

using namespace pag;

extern "C" {
    JNIEXPORT void Java_org_libpag_PAGVideoDecoder_nativeSetMaxHardwareDecoderCount(JNIEnv*, jclass, jint maxCount) {
        PAGVideoDecoder::SetMaxHardwareDecoderCount(maxCount);
    }
}