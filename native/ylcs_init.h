#ifndef PLATFORM_JNI_INIT_H
#define PLATFORM_JNI_INIT_H

#include <jni.h>

extern "C" {
	void JNICALL Initialize_AudioPlayer(JavaVM* vm, JNIEnv* env);
	void JNICALL Initialize_VideoPlayer(JavaVM* vm, JNIEnv* env);
}

#endif