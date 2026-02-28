#ifndef PLATFORM_JNI_H
#define PLATFORM_JNI_H

#include <jni.h>

#ifdef __cplusplus // objc not support c++

#include <string>
#include <string_view>
#include <stdexcept>

namespace JVM {
    inline JavaVM* vm = nullptr;

    struct JniEnvGuard {
        JNIEnv* env = nullptr;
        bool attached = false;

        JniEnvGuard() {
            if (vm->GetEnv(reinterpret_cast<void**>(&env), JNI_VERSION_1_6) == JNI_EDETACHED) {
                if (vm->AttachCurrentThread((void**)&env, nullptr) == JNI_OK) attached = true;
            }
        }

        ~JniEnvGuard() {
            if (attached && env) vm->DetachCurrentThread();
        }

        JNIEnv* operator -> () {
            return env;
        }

        explicit operator bool() const {
            return env != nullptr;
        }

        void checkException() const {
            if (env && env->ExceptionCheck()) {
                env->ExceptionDescribe();
                env->ExceptionClear();
            }
        }
    };
}

inline std::string j2s(JNIEnv* env, jstring str) {
    if (!str) return "";
    char const* p{ env->GetStringUTFChars(str, nullptr) };
    if (!p) return "";
    jsize length = env->GetStringUTFLength(str);
    std::string result{ p, static_cast<std::size_t>(length) };
    env->ReleaseStringUTFChars(str, p);
    return result;
}

inline jstring s2j(JNIEnv* env, std::string_view str) {
    return env->NewStringUTF(str.data());
}

inline std::wstring j2w(JNIEnv* env, jstring str) {
#if defined(_WIN32)
    if (!str) return L"";
    jchar const* p{ env->GetStringChars(str, nullptr) };
    if (!p) return L"";
    jsize length = env->GetStringLength(str);
    std::wstring result{ reinterpret_cast<wchar_t const*>(p), static_cast<std::size_t>(length) };
    env->ReleaseStringChars(str, p);
    return result;
#else
    throw std::runtime_error { "You should not use std::wstring in non Windows environments" };
#endif
}

inline jstring w2j(JNIEnv* env, std::wstring_view str) {
#if defined(_WIN32)
    return env->NewString(reinterpret_cast<jchar const*>(str.data()), static_cast<jsize>(str.length()));
#else
    throw std::runtime_error { "You should not use std::wstring in non Windows environments" };
#endif
}

#endif

#endif