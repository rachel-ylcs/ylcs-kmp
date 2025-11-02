#include "platform.h"

#include "CoreFoundation/CoreFoundation.h"

static CFMessagePortRef appMessagePort = NULL;

static CFDataRef MessageCallback(CFMessagePortRef port, SInt32 msgid, CFDataRef data, void *info) {
    return NULL;
}

bool ylcs_single_instance_try_lock() {
    CFStringRef name = CFSTR("love.yinlin.ylcs-desktop");
    appMessagePort = CFMessagePortCreateLocal(nil, name, MessageCallback, nil, nil);
    if (appMessagePort != NULL) {
        return true;
    }
    return false;
}

void ylcs_single_instance_unlock() {
    if (appMessagePort != NULL) {
        CFMessagePortInvalidate(appMessagePort);
        CFRelease(appMessagePort);
        appMessagePort = NULL;
    }
}

extern "C" {
    JNIEXPORT jboolean JNICALL Java_love_yinlin_platform_NativeOSKt_requestSingleInstance(JNIEnv* env, jclass) {
        return (jboolean)ylcs_single_instance_try_lock();
    }

    JNIEXPORT void JNICALL Java_love_yinlin_platform_NativeOSKt_releaseSingleInstance(JNIEnv* env, jclass) {
        ylcs_single_instance_unlock();
    }
}