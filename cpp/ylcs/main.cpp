#include "../include/platform.h"

extern "C" {
	JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* vm, void* reserved) {
		JNIEnv* env{ };
		if (vm->GetEnv(reinterpret_cast<void**>(&env), JNI_VERSION_1_6) != JNI_OK)
		{
			return -1;
		}
		return JNI_VERSION_1_6;
	}
}