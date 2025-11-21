#include "ylcs_jni.h"
#define FORCE_POSIX
#include "mmkv/MMKV.h"

using namespace mmkv;

inline MMKV* kv_cast(jlong handle)
{
    return reinterpret_cast<MMKV*>(handle);
}

extern "C" {
    JNIEXPORT jlong JNICALL Java_love_yinlin_platform_NativeKVKt_nativeInit(JNIEnv* env, jclass, jstring path)
	{
		MMKV::initializeMMKV(string2MMKVPath_t(j2s(env, path)), MMKVLogLevel::MMKVLogNone);
		return reinterpret_cast<jlong>(MMKV::defaultMMKV());
	}

	JNIEXPORT void JNICALL Java_love_yinlin_platform_NativeKVKt_nativeSetBoolean(JNIEnv* env, jclass, jlong handle, jstring key, jboolean value, jint expire)
	{
		if (auto kv = kv_cast(handle); kv && key)
		{
			kv->set((bool)value, j2s(env, key), (uint32_t)expire);
		}
	}

	JNIEXPORT void JNICALL Java_love_yinlin_platform_NativeKVKt_nativeSetInt(JNIEnv* env, jclass, jlong handle, jstring key, jint value, jint expire)
	{
		if (auto kv = kv_cast(handle); kv && key)
		{
			kv->set((int32_t)value, j2s(env, key), (uint32_t)expire);
		}
	}

	JNIEXPORT void JNICALL Java_love_yinlin_platform_NativeKVKt_nativeSetLong(JNIEnv* env, jclass, jlong handle, jstring key, jlong value, jint expire)
	{
		if (auto kv = kv_cast(handle); kv && key)
		{
			kv->set((int64_t)value, j2s(env, key), (uint32_t)expire);
		}
	}

	JNIEXPORT void JNICALL Java_love_yinlin_platform_NativeKVKt_nativeSetFloat(JNIEnv* env, jclass, jlong handle, jstring key, jfloat value, jint expire)
	{
		if (auto kv = kv_cast(handle); kv && key)
		{
			kv->set((float)value, j2s(env, key), (uint32_t)expire);
		}
	}

	JNIEXPORT void JNICALL Java_love_yinlin_platform_NativeKVKt_nativeSetDouble(JNIEnv* env, jclass, jlong handle, jstring key, jdouble value, jint expire)
	{
		if (auto kv = kv_cast(handle); kv && key)
		{
			kv->set((double)value, j2s(env, key), (uint32_t)expire);
		}
	}

	JNIEXPORT void JNICALL Java_love_yinlin_platform_NativeKVKt_nativeSetString(JNIEnv* env, jclass, jlong handle, jstring key, jstring value, jint expire)
	{
		if (auto kv = kv_cast(handle); kv && key)
		{
			kv->set(j2s(env, value), j2s(env, key), (uint32_t)expire);
		}
	}

	JNIEXPORT void JNICALL Java_love_yinlin_platform_NativeKVKt_nativeSetByteArray(JNIEnv* env, jclass, jlong handle, jstring key, jbyteArray value, jint expire)
	{
		if (auto kv = kv_cast(handle); kv && key)
		{
			auto length = env->GetArrayLength(value);
			auto data = env->GetPrimitiveArrayCritical(value, nullptr);
			if (data)
			{
				mmkv::MMBuffer buffer(data, length);
				env->ReleasePrimitiveArrayCritical(value, data, JNI_ABORT);
				kv->set(buffer, j2s(env, key), (uint32_t)expire);
			}
		}
	}

	JNIEXPORT jboolean JNICALL Java_love_yinlin_platform_NativeKVKt_nativeGetBoolean(JNIEnv* env, jclass, jlong handle, jstring key, jboolean def)
	{
		if (auto kv = kv_cast(handle); kv && key)
		{
			return (jboolean)kv->getBool(j2s(env, key), (bool)def);
		}
		return def;
	}

	JNIEXPORT jint JNICALL Java_love_yinlin_platform_NativeKVKt_nativeGetInt(JNIEnv* env, jclass, jlong handle, jstring key, jint def)
	{
		if (auto kv = kv_cast(handle); kv && key)
		{
			return (jint)kv->getInt32(j2s(env, key), (int32_t)def);
		}
		return def;
	}

	JNIEXPORT jlong JNICALL Java_love_yinlin_platform_NativeKVKt_nativeGetLong(JNIEnv* env, jclass, jlong handle, jstring key, jlong def)
	{
		if (auto kv = kv_cast(handle); kv && key)
		{
			return (jlong)kv->getInt64(j2s(env, key), (int64_t)def);
		}
		return def;
	}

	JNIEXPORT jfloat JNICALL Java_love_yinlin_platform_NativeKVKt_nativeGetFloat(JNIEnv* env, jclass, jlong handle, jstring key, jfloat def)
	{
		if (auto kv = kv_cast(handle); kv && key)
		{
			return (jfloat)kv->getFloat(j2s(env, key), (float)def);
		}
		return def;
	}

	JNIEXPORT jdouble JNICALL Java_love_yinlin_platform_NativeKVKt_nativeGetDouble(JNIEnv* env, jclass, jlong handle, jstring key, jdouble def)
	{
		if (auto kv = kv_cast(handle); kv && key)
		{
			return (jdouble)kv->getDouble(j2s(env, key), (double)def);
		}
		return def;
	}

	JNIEXPORT jstring JNICALL Java_love_yinlin_platform_NativeKVKt_nativeGetString(JNIEnv* env, jclass, jlong handle, jstring key, jstring def)
	{
		if (auto kv = kv_cast(handle); kv && key)
		{
			std::string value;
			bool hasValue = kv->getString(j2s(env, key), value);
			if (hasValue) return s2j(env, value);
		}
		return def;
	}

	JNIEXPORT jbyteArray JNICALL Java_love_yinlin_platform_NativeKVKt_nativeGetByteArray(JNIEnv* env, jclass, jlong handle, jstring key, jbyteArray def)
	{
		if (auto kv = kv_cast(handle); kv && key)
		{
			mmkv::MMBuffer buffer;
			auto hasValue = kv->getBytes(j2s(env, key), buffer);
			auto data = reinterpret_cast<jbyte const*>(buffer.getPtr());
			auto size = (jsize)buffer.length();
			if (hasValue)
			{
				jbyteArray value = env->NewByteArray(size);
				env->SetByteArrayRegion(value, 0, size, data);
				return value;
			}
		}
		return def;
	}

	JNIEXPORT jboolean JNICALL Java_love_yinlin_platform_NativeKVKt_nativeContains(JNIEnv* env, jclass, jlong handle, jstring key)
	{
		if (auto kv = kv_cast(handle); kv && key)
		{
			return (jboolean)kv->containsKey(j2s(env, key));
		}
		return false;
	}

	JNIEXPORT void JNICALL Java_love_yinlin_platform_NativeKVKt_nativeRemove(JNIEnv* env, jclass, jlong handle, jstring key)
	{
		if (auto kv = kv_cast(handle); kv && key)
		{
			kv->removeValueForKey(j2s(env, key));
		}
	}
}