#include <native_jni.h>
#define FORCE_POSIX
#include <MMBuffer.h>
#include <rachel_kv.h>
#include <rachel_kv_native.h>
#include <limits>
#include <cstring>
#include <string_view>

class JavaString {
    JNIEnv *env;
    jstring source;
    const char *borrowed = nullptr;
    std::string_view value;
public:
    JavaString(JNIEnv *environment, jstring string) : env(environment), source(string) {
        if (!source) return;
        borrowed = env->GetStringUTFChars(source, nullptr);
        if (!borrowed) return;
        value = std::string_view(borrowed, std::strlen(borrowed));
    }

    JavaString(const JavaString &) = delete;
    JavaString &operator=(const JavaString &) = delete;

    ~JavaString() {
        if (borrowed) env->ReleaseStringUTFChars(source, borrowed);
    }

    const char *data() const { return value.data(); }
    const char *c_str() const { return value.empty() ? "" : value.data(); }
    size_t size() const { return value.size(); }
};

extern "C" {
    JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *, void *) {
        return JNI_VERSION_1_6;
    }

    JNIEXPORT jlong JNICALL Java_love_yinlin_foundation_kv_NativeKV_nativeInit(JNIEnv *env, jclass, jstring path) {
        JavaString directory(env, path);
        if (env->ExceptionCheck()) return 0;
        return reinterpret_cast<jlong>(rachel_kv_open(directory.c_str()));
    }

    JNIEXPORT void JNICALL Java_love_yinlin_foundation_kv_NativeKV_nativeClose(JNIEnv *, jclass, jlong handle) {
        rachel_kv_close(reinterpret_cast<void *>(handle));
    }

    JNIEXPORT void JNICALL Java_love_yinlin_foundation_kv_NativeKV_nativeSetBoolean(JNIEnv *env, jclass, jlong handle, jstring key, jboolean value, jint expire) {
        JavaString name(env, key);
        if (!env->ExceptionCheck()) rachel_kv_set_bool(reinterpret_cast<void *>(handle), name.data(), name.size(), value, static_cast<uint32_t>(expire));
    }

    JNIEXPORT jboolean JNICALL Java_love_yinlin_foundation_kv_NativeKV_nativeGetBoolean(JNIEnv *env, jclass, jlong handle, jstring key, jboolean fallback) {
        JavaString name(env, key);
        if (env->ExceptionCheck()) return fallback;
        return rachel_kv_get_bool(reinterpret_cast<void *>(handle), name.data(), name.size(), fallback);
    }

    JNIEXPORT void JNICALL Java_love_yinlin_foundation_kv_NativeKV_nativeSetInt(JNIEnv *env, jclass, jlong handle, jstring key, jint value, jint expire) {
        JavaString name(env, key);
        if (!env->ExceptionCheck()) rachel_kv_set_int(reinterpret_cast<void *>(handle), name.data(), name.size(), value, static_cast<uint32_t>(expire));
    }

    JNIEXPORT jint JNICALL Java_love_yinlin_foundation_kv_NativeKV_nativeGetInt(JNIEnv *env, jclass, jlong handle, jstring key, jint fallback) {
        JavaString name(env, key);
        if (env->ExceptionCheck()) return fallback;
        return rachel_kv_get_int(reinterpret_cast<void *>(handle), name.data(), name.size(), fallback);
    }

    JNIEXPORT void JNICALL Java_love_yinlin_foundation_kv_NativeKV_nativeSetLong(JNIEnv *env, jclass, jlong handle, jstring key, jlong value, jint expire) {
        JavaString name(env, key);
        if (!env->ExceptionCheck()) rachel_kv_set_long(reinterpret_cast<void *>(handle), name.data(), name.size(), value, static_cast<uint32_t>(expire));
    }

    JNIEXPORT jlong JNICALL Java_love_yinlin_foundation_kv_NativeKV_nativeGetLong(JNIEnv *env, jclass, jlong handle, jstring key, jlong fallback) {
        JavaString name(env, key);
        if (env->ExceptionCheck()) return fallback;
        return rachel_kv_get_long(reinterpret_cast<void *>(handle), name.data(), name.size(), fallback);
    }

    JNIEXPORT void JNICALL Java_love_yinlin_foundation_kv_NativeKV_nativeSetFloat(JNIEnv *env, jclass, jlong handle, jstring key, jfloat value, jint expire) {
        JavaString name(env, key);
        if (!env->ExceptionCheck()) rachel_kv_set_float(reinterpret_cast<void *>(handle), name.data(), name.size(), value, static_cast<uint32_t>(expire));
    }

    JNIEXPORT jfloat JNICALL Java_love_yinlin_foundation_kv_NativeKV_nativeGetFloat(JNIEnv *env, jclass, jlong handle, jstring key, jfloat fallback) {
        JavaString name(env, key);
        if (env->ExceptionCheck()) return fallback;
        return rachel_kv_get_float(reinterpret_cast<void *>(handle), name.data(), name.size(), fallback);
    }

    JNIEXPORT void JNICALL Java_love_yinlin_foundation_kv_NativeKV_nativeSetDouble(JNIEnv *env, jclass, jlong handle, jstring key, jdouble value, jint expire) {
        JavaString name(env, key);
        if (!env->ExceptionCheck()) rachel_kv_set_double(reinterpret_cast<void *>(handle), name.data(), name.size(), value, static_cast<uint32_t>(expire));
    }

    JNIEXPORT jdouble JNICALL Java_love_yinlin_foundation_kv_NativeKV_nativeGetDouble(JNIEnv *env, jclass, jlong handle, jstring key, jdouble fallback) {
        JavaString name(env, key);
        if (env->ExceptionCheck()) return fallback;
        return rachel_kv_get_double(reinterpret_cast<void *>(handle), name.data(), name.size(), fallback);
    }

    JNIEXPORT void JNICALL Java_love_yinlin_foundation_kv_NativeKV_nativeSetString(JNIEnv *env, jclass, jlong handle, jstring key, jstring value, jint expire) {
        JavaString name(env, key);
        if (env->ExceptionCheck()) return;
        JavaString text(env, value);
        if (!env->ExceptionCheck()) rachel_kv_set_string(reinterpret_cast<void *>(handle), name.data(), name.size(), reinterpret_cast<const uint8_t *>(text.data()), text.size(), static_cast<uint32_t>(expire));
    }

    JNIEXPORT jstring JNICALL Java_love_yinlin_foundation_kv_NativeKV_nativeGetString(JNIEnv *env, jclass, jlong handle, jstring key, jstring fallback) {
        JavaString name(env, key);
        if (env->ExceptionCheck()) return fallback;
        std::string value;
        if (!rachel_kv_read_string(reinterpret_cast<void *>(handle), name.data(), name.size(), value)) return fallback;
        return env->NewStringUTF(value.c_str());
    }

    JNIEXPORT void JNICALL Java_love_yinlin_foundation_kv_NativeKV_nativeSetByteArray(JNIEnv *env, jclass, jlong handle, jstring key, jbyteArray value, jint expire) {
        JavaString name(env, key);
        if (env->ExceptionCheck()) return;
        auto size = env->GetArrayLength(value);
        if (!size) {
            rachel_kv_set_bytes(reinterpret_cast<void *>(handle), name.data(), name.size(), nullptr, 0, static_cast<uint32_t>(expire));
            return;
        }
        auto *data = env->GetByteArrayElements(value, nullptr);
        if (!data) return;
        rachel_kv_set_bytes(reinterpret_cast<void *>(handle), name.data(), name.size(), reinterpret_cast<const uint8_t *>(data), static_cast<size_t>(size), static_cast<uint32_t>(expire));
        env->ReleaseByteArrayElements(value, data, JNI_ABORT);
    }

    JNIEXPORT jbyteArray JNICALL Java_love_yinlin_foundation_kv_NativeKV_nativeGetByteArray(JNIEnv *env, jclass, jlong handle, jstring key, jbyteArray fallback) {
        JavaString name(env, key);
        if (env->ExceptionCheck()) return fallback;
        mmkv::MMBuffer value;
        if (!rachel_kv_read_bytes(reinterpret_cast<void *>(handle), name.data(), name.size(), value)) return fallback;
        jbyteArray result = fallback;
        if (value.length() <= static_cast<size_t>(std::numeric_limits<jsize>::max())) {
            result = env->NewByteArray(static_cast<jsize>(value.length()));
            if (result && value.length()) env->SetByteArrayRegion(result, 0, static_cast<jsize>(value.length()), static_cast<const jbyte *>(value.getPtr()));
        }
        return result;
    }

    JNIEXPORT jboolean JNICALL Java_love_yinlin_foundation_kv_NativeKV_nativeContains(JNIEnv *env, jclass, jlong handle, jstring key) {
        JavaString name(env, key);
        return !env->ExceptionCheck() && rachel_kv_contains(reinterpret_cast<void *>(handle), name.data(), name.size());
    }

    JNIEXPORT void JNICALL Java_love_yinlin_foundation_kv_NativeKV_nativeRemove(JNIEnv *env, jclass, jlong handle, jstring key) {
        JavaString name(env, key);
        if (!env->ExceptionCheck()) rachel_kv_remove(reinterpret_cast<void *>(handle), name.data(), name.size());
    }
}
