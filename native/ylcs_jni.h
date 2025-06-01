#ifndef PLATFORM_JNI_H
#define PLATFORM_JNI_H

#include <jni.h>

#include <string>
#include <string_view>
#include <stdexcept>

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