#include "../../ylcs_jni.h"

#include <string>
#include <stdio.h>
#include <fcntl.h>
#include <unistd.h>
#include <sys/file.h>

#define LOCK_FILE_PATH "/tmp/ylcs_desktop.lock"

static int appLockFd = -1;

bool ylcs_single_instance_try_lock() {
    appLockFd = open(LOCK_FILE_PATH, O_CREAT | O_RDWR, 0666);
    if (appLockFd < 0) {
        return false;
    }

    int result = flock(appLockFd, LOCK_EX | LOCK_NB);
    if (result < 0) {
        close(appLockFd);
        appLockFd = -1;
        return false;
    }

    std::string pid = std::to_string(getpid());
    ftruncate(appLockFd, 0);
    write(appLockFd, pid.c_str(), pid.length());
    return true;
}

void ylcs_single_instance_unlock() {
    if (appLockFd >= 0) {
        flock(appLockFd, LOCK_UN);
        close(appLockFd);
        appLockFd = -1;
        unlink(LOCK_FILE_PATH);
    }
}

extern "C" {
    JNIEXPORT jboolean JNICALL Java_love_yinlin_platform_OS_1desktopKt_requestSingleInstance(JNIEnv* env, jobject) {
        return (jboolean)ylcs_single_instance_try_lock();
    }

    JNIEXPORT void JNICALL Java_love_yinlin_platform_OS_1desktopKt_releaseSingleInstance(JNIEnv* env, jobject) {
        ylcs_single_instance_unlock();
    }
}