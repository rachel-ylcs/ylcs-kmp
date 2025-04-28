#ifndef CPP_PLATFORM_WIN_H
#define CPP_PLATFORM_WIN_H

inline std::string j2s(JNIEnv* env, jstring str) {
	if (!str) return "";
	char const* p{ env->GetStringUTFChars(str, nullptr) };
	if (!p) return "";
	jsize length = env->GetStringUTFLength(str);
	std::string result{ p, (std::size_t)length };
	env->ReleaseStringUTFChars(str, p);
	return result;
}

inline jstring s2j(JNIEnv* env, std::string_view str) {
	return env->NewStringUTF(str.data());
}

inline std::wstring j2w(JNIEnv* env, jstring str) {
	if (!str) return L"";
	jchar const* p{ env->GetStringChars(str, nullptr) };
	if (!p) return L"";
	jsize length = env->GetStringLength(str);
	std::wstring result{ reinterpret_cast<wchar_t const*>(p), (std::size_t)length};
	env->ReleaseStringChars(str, p);
	return result;
}

inline jstring w2j(JNIEnv* env, std::wstring_view str) {
	return env->NewString(reinterpret_cast<jchar const*>(str.data()), (jsize)str.length());
}

#endif