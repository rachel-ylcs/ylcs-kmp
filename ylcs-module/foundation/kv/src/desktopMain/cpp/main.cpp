#include <native_jni.h>
#define FORCE_POSIX
#include <MMKV.h>
#include <rachel_kv.h>
#include <codecvt>
#include <locale>

using namespace mmkv;

static std::string string_from_java(JNIEnv *env, jstring value) {
    if (!value) return {};
    const jchar *chars = env->GetStringChars(value, nullptr);
    if (!chars) return {};
    std::u16string text(reinterpret_cast<const char16_t *>(chars), env->GetStringLength(value));
    env->ReleaseStringChars(value, chars);
    return std::wstring_convert<std::codecvt_utf8_utf16<char16_t>, char16_t>().to_bytes(text);
}

inline MMKV* kv_cast(jlong handle) {
    return reinterpret_cast<MMKV*>(handle);
}

extern "C" {
    JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* vm, void* reserved) {
        return JNI_VERSION_1_6;
    }

    JNIEXPORT jlong JNICALL Java_love_yinlin_kv_NativeKV_nativeInit(JNIEnv* env, jclass, jstring path) {
        return reinterpret_cast<jlong>(rachel_kv_open(string_from_java(env, path).c_str()));
    }

    JNIEXPORT void JNICALL Java_love_yinlin_kv_NativeKV_nativeClose(JNIEnv*, jclass, jlong handle) {
        rachel_kv_close(reinterpret_cast<void*>(handle));
    }

    JNIEXPORT void JNICALL Java_love_yinlin_kv_NativeKV_nativeSetBoolean(JNIEnv* env, jclass, jlong handle, jstring key, jboolean value, jint expire) {
        if (auto kv = kv_cast(handle); kv && key) {
            kv->set((bool)value, string_from_java(env, key), (uint32_t)expire);
        }
    }

    JNIEXPORT void JNICALL Java_love_yinlin_kv_NativeKV_nativeSetInt(JNIEnv* env, jclass, jlong handle, jstring key, jint value, jint expire) {
        if (auto kv = kv_cast(handle); kv && key) {
            kv->set((int32_t)value, string_from_java(env, key), (uint32_t)expire);
        }
    }

    JNIEXPORT void JNICALL Java_love_yinlin_kv_NativeKV_nativeSetLong(JNIEnv* env, jclass, jlong handle, jstring key, jlong value, jint expire) {
        if (auto kv = kv_cast(handle); kv && key) {
            kv->set((int64_t)value, string_from_java(env, key), (uint32_t)expire);
        }
    }

    JNIEXPORT void JNICALL Java_love_yinlin_kv_NativeKV_nativeSetFloat(JNIEnv* env, jclass, jlong handle, jstring key, jfloat value, jint expire) {
        if (auto kv = kv_cast(handle); kv && key) {
            kv->set((float)value, string_from_java(env, key), (uint32_t)expire);
        }
    }

    JNIEXPORT void JNICALL Java_love_yinlin_kv_NativeKV_nativeSetDouble(JNIEnv* env, jclass, jlong handle, jstring key, jdouble value, jint expire) {
        if (auto kv = kv_cast(handle); kv && key) {
            kv->set((double)value, string_from_java(env, key), (uint32_t)expire);
        }
    }

    JNIEXPORT void JNICALL Java_love_yinlin_kv_NativeKV_nativeSetByteArray(JNIEnv* env, jclass, jlong handle, jstring key, jbyteArray value, jint expire) {
        if (auto kv = kv_cast(handle); kv && key) {
            auto length = env->GetArrayLength(value);
            if (!length) {
                kv->set(std::string(), string_from_java(env, key), (uint32_t)expire);
                return;
            }
            auto data = env->GetPrimitiveArrayCritical(value, nullptr);
            if (data) {
                mmkv::MMBuffer buffer(data, length);
                env->ReleasePrimitiveArrayCritical(value, data, JNI_ABORT);
                kv->set(buffer, string_from_java(env, key), (uint32_t)expire);
            }
        }
    }

    JNIEXPORT jboolean JNICALL Java_love_yinlin_kv_NativeKV_nativeGetBoolean(JNIEnv* env, jclass, jlong handle, jstring key, jboolean def) {
        if (auto kv = kv_cast(handle); kv && key) {
            return (jboolean)kv->getBool(string_from_java(env, key), (bool)def);
        }
        return def;
    }

    JNIEXPORT jint JNICALL Java_love_yinlin_kv_NativeKV_nativeGetInt(JNIEnv* env, jclass, jlong handle, jstring key, jint def) {
        if (auto kv = kv_cast(handle); kv && key) {
            return (jint)kv->getInt32(string_from_java(env, key), (int32_t)def);
        }
        return def;
    }

    JNIEXPORT jlong JNICALL Java_love_yinlin_kv_NativeKV_nativeGetLong(JNIEnv* env, jclass, jlong handle, jstring key, jlong def) {
        if (auto kv = kv_cast(handle); kv && key) {
            return (jlong)kv->getInt64(string_from_java(env, key), (int64_t)def);
        }
        return def;
    }

    JNIEXPORT jfloat JNICALL Java_love_yinlin_kv_NativeKV_nativeGetFloat(JNIEnv* env, jclass, jlong handle, jstring key, jfloat def) {
        if (auto kv = kv_cast(handle); kv && key) {
            return (jfloat)kv->getFloat(string_from_java(env, key), (float)def);
        }
        return def;
    }

    JNIEXPORT jdouble JNICALL Java_love_yinlin_kv_NativeKV_nativeGetDouble(JNIEnv* env, jclass, jlong handle, jstring key, jdouble def) {
        if (auto kv = kv_cast(handle); kv && key) {
            return (jdouble)kv->getDouble(string_from_java(env, key), (double)def);
        }
        return def;
    }

    JNIEXPORT jbyteArray JNICALL Java_love_yinlin_kv_NativeKV_nativeGetByteArray(JNIEnv* env, jclass, jlong handle, jstring key, jbyteArray def) {
        if (auto kv = kv_cast(handle); kv && key) {
            mmkv::MMBuffer buffer;
            auto hasValue = kv->getBytes(string_from_java(env, key), buffer);
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

    JNIEXPORT jboolean JNICALL Java_love_yinlin_kv_NativeKV_nativeContains(JNIEnv* env, jclass, jlong handle, jstring key) {
        if (auto kv = kv_cast(handle); kv && key) {
            return (jboolean)kv->containsKey(string_from_java(env, key));
        }
        return false;
    }

    JNIEXPORT void JNICALL Java_love_yinlin_kv_NativeKV_nativeRemove(JNIEnv* env, jclass, jlong handle, jstring key) {
        if (auto kv = kv_cast(handle); kv && key) {
            kv->removeValueForKey(string_from_java(env, key));
        }
    }
}
