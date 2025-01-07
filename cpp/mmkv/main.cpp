#include <jni.h>
#include "MMKV.h"

std::string jstring2string(JNIEnv* env, jstring str)
{
	if (str)
	{
		const char* kstr = env->GetStringUTFChars(str, nullptr);
		if (kstr)
		{
			std::string result(kstr);
			env->ReleaseStringUTFChars(str, kstr);
			return result;
		}
	}
	return "";
}

inline MMKV* kv_cast(jlong handle)
{
	return reinterpret_cast<MMKV*>(handle);
}

extern "C" {
	JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* vm, void* reserved)
	{
		JNIEnv* env{ };
		if (vm->GetEnv(reinterpret_cast<void**>(&env), JNI_VERSION_1_6) != JNI_OK)
		{
			return -1;
		}
		return JNI_VERSION_1_6;
	}

	JNIEXPORT jlong JNICALL Java_love_yinlin_platform_KV_init(JNIEnv* env, jobject, jstring path)
	{
		MMKV::initializeMMKV(string2MMKVPath_t(jstring2string(env, path)), MMKVLogLevel::MMKVLogNone);
		return reinterpret_cast<jlong>(MMKV::defaultMMKV());
	}

	JNIEXPORT void JNICALL Java_love_yinlin_platform_KV_setBoolean(JNIEnv* env, jobject, jlong handle, jstring key, jboolean value, jint expire)
	{
		if (auto kv = kv_cast(handle); kv && key)
		{
			kv->set((bool)value, jstring2string(env, key), (uint32_t)expire);
		}
	}

	JNIEXPORT void JNICALL Java_love_yinlin_platform_KV_setInt(JNIEnv* env, jobject, jlong handle, jstring key, jint value, jint expire)
	{
		if (auto kv = kv_cast(handle); kv && key)
		{
			kv->set((int32_t)value, jstring2string(env, key), (uint32_t)expire);
		}
	}

	JNIEXPORT void JNICALL Java_love_yinlin_platform_KV_setLong(JNIEnv* env, jobject, jlong handle, jstring key, jlong value, jint expire)
	{
		if (auto kv = kv_cast(handle); kv && key)
		{
			kv->set((int64_t)value, jstring2string(env, key), (uint32_t)expire);
		}
	}

	JNIEXPORT void JNICALL Java_love_yinlin_platform_KV_setFloat(JNIEnv* env, jobject, jlong handle, jstring key, jfloat value, jint expire)
	{
		if (auto kv = kv_cast(handle); kv && key)
		{
			kv->set((float)value, jstring2string(env, key), (uint32_t)expire);
		}
	}

	JNIEXPORT void JNICALL Java_love_yinlin_platform_KV_setDouble(JNIEnv* env, jobject, jlong handle, jstring key, jdouble value, jint expire)
	{
		if (auto kv = kv_cast(handle); kv && key)
		{
			kv->set((double)value, jstring2string(env, key), (uint32_t)expire);
		}
	}

	JNIEXPORT void JNICALL Java_love_yinlin_platform_KV_setString(JNIEnv* env, jobject, jlong handle, jstring key, jstring value, jint expire)
	{
		if (auto kv = kv_cast(handle); kv && key)
		{
			kv->set(jstring2string(env, value), jstring2string(env, key), (uint32_t)expire);
		}
	}

	JNIEXPORT void JNICALL Java_love_yinlin_platform_KV_setByteArray(JNIEnv* env, jobject, jlong handle, jstring key, jbyteArray value, jint expire)
	{
		if (auto kv = kv_cast(handle); kv && key)
		{
			auto length = env->GetArrayLength(value);
			auto data = env->GetPrimitiveArrayCritical(value, nullptr);
			if (data)
			{
				mmkv::MMBuffer buffer(data, length);
				env->ReleasePrimitiveArrayCritical(value, data, JNI_ABORT);
				kv->set(buffer, jstring2string(env, key), (uint32_t)expire);
			}
		}
	}

	JNIEXPORT jboolean JNICALL Java_love_yinlin_platform_KV_getBoolean(JNIEnv* env, jobject, jlong handle, jstring key, jboolean def)
	{
		if (auto kv = kv_cast(handle); kv && key)
		{
			return (jboolean)kv->getBool(jstring2string(env, key), (bool)def);
		}
		return def;
	}

	JNIEXPORT jint JNICALL Java_love_yinlin_platform_KV_getInt(JNIEnv* env, jobject, jlong handle, jstring key, jint def)
	{
		if (auto kv = kv_cast(handle); kv && key)
		{
			return (jint)kv->getInt32(jstring2string(env, key), (int32_t)def);
		}
		return def;
	}

	JNIEXPORT jlong JNICALL Java_love_yinlin_platform_KV_getLong(JNIEnv* env, jobject, jlong handle, jstring key, jlong def)
	{
		if (auto kv = kv_cast(handle); kv && key)
		{
			return (jlong)kv->getInt64(jstring2string(env, key), (int64_t)def);
		}
		return def;
	}

	JNIEXPORT jfloat JNICALL Java_love_yinlin_platform_KV_getFloat(JNIEnv* env, jobject, jlong handle, jstring key, jfloat def)
	{
		if (auto kv = kv_cast(handle); kv && key)
		{
			return (jfloat)kv->getFloat(jstring2string(env, key), (float)def);
		}
		return def;
	}

	JNIEXPORT jdouble JNICALL Java_love_yinlin_platform_KV_getDouble(JNIEnv* env, jobject, jlong handle, jstring key, jdouble def)
	{
		if (auto kv = kv_cast(handle); kv && key)
		{
			return (jdouble)kv->getDouble(jstring2string(env, key), (double)def);
		}
		return def;
	}

	JNIEXPORT jstring JNICALL Java_love_yinlin_platform_KV_getString(JNIEnv* env, jobject, jlong handle, jstring key, jstring def)
	{
		if (auto kv = kv_cast(handle); kv && key)
		{
			std::string value;
			bool hasValue = kv->getString(jstring2string(env, key), value);
			if (hasValue)
			{
				return env->NewStringUTF(value.data());
			}
		}
		return def;
	}

	JNIEXPORT jbyteArray JNICALL Java_love_yinlin_platform_KV_getByteArray(JNIEnv* env, jobject, jlong handle, jstring key, jbyteArray def)
	{
		if (auto kv = kv_cast(handle); kv && key)
		{
			mmkv::MMBuffer buffer;
			auto hasValue = kv->getBytes(jstring2string(env, key), buffer);
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

	JNIEXPORT jboolean JNICALL Java_love_yinlin_platform_KV_has(JNIEnv* env, jobject, jlong handle, jstring key)
	{
		if (auto kv = kv_cast(handle); kv && key)
		{
			return (jboolean)kv->containsKey(jstring2string(env, key));
		}
		return false;
	}

	JNIEXPORT void JNICALL Java_love_yinlin_platform_KV_remove(JNIEnv* env, jobject, jlong handle, jstring key)
	{
		if (auto kv = kv_cast(handle); kv && key)
		{
			kv->removeValueForKey(jstring2string(env, key));
		}
	}
}