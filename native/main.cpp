#include <string>
#include <string_view>
#include <jni.h>
#define FORCE_POSIX
#include "mmkv/MMKV.h"
#include "nfd.hpp"
#include "platform.h"

using namespace mmkv;

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
	std::wstring result{ reinterpret_cast<wchar_t const*>(p), (std::size_t)length };
	env->ReleaseStringChars(str, p);
	return result;
}

inline jstring w2j(JNIEnv* env, std::wstring_view str) {
	return env->NewString(reinterpret_cast<jchar const*>(str.data()), (jsize)str.length());
}

inline MMKV* kv_cast(jlong handle)
{
	return reinterpret_cast<MMKV*>(handle);
}

inline std::string nfd_convert_filter(const std::string &filter)
{
	std::string result = filter;
	std::string::size_type pos = 0;
	while ((pos = result.find("*.")) != std::string::npos) {
		result.replace(pos, 2, "");
	}
	for (auto &c : result) {
		if (c == ';') {
			c = ',';
		}
	}
	return result;
}

inline nfdwindowhandle_t nfd_get_window(void* handle)
{
	nfdwindowhandle_t window{};
	if (handle) {
#if defined(_WIN32)
		window.type = NFD_WINDOW_HANDLE_TYPE_WINDOWS;
#elif defined(__APPLE__)
		window.type = NFD_WINDOW_HANDLE_TYPE_COCOA;
#else
		window.type = NFD_WINDOW_HANDLE_TYPE_X11;
#endif
		window.handle = handle;
	}
	return window;
}

extern "C" {
	JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* vm, void* reserved)
	{
		JNIEnv* env = nullptr;
		if (vm->GetEnv(reinterpret_cast<void**>(&env), JNI_VERSION_1_6) != JNI_OK)
		{
			return -1;
		}
		return JNI_VERSION_1_6;
	}

	JNIEXPORT jlong JNICALL Java_love_yinlin_platform_KV_init(JNIEnv* env, jobject, jstring path)
	{
		MMKV::initializeMMKV(string2MMKVPath_t(j2s(env, path)), MMKVLogLevel::MMKVLogNone);
		return reinterpret_cast<jlong>(MMKV::defaultMMKV());
	}

	JNIEXPORT void JNICALL Java_love_yinlin_platform_KV_setBoolean(JNIEnv* env, jobject, jlong handle, jstring key, jboolean value, jint expire)
	{
		if (auto kv = kv_cast(handle); kv && key)
		{
			kv->set((bool)value, j2s(env, key), (uint32_t)expire);
		}
	}

	JNIEXPORT void JNICALL Java_love_yinlin_platform_KV_setInt(JNIEnv* env, jobject, jlong handle, jstring key, jint value, jint expire)
	{
		if (auto kv = kv_cast(handle); kv && key)
		{
			kv->set((int32_t)value, j2s(env, key), (uint32_t)expire);
		}
	}

	JNIEXPORT void JNICALL Java_love_yinlin_platform_KV_setLong(JNIEnv* env, jobject, jlong handle, jstring key, jlong value, jint expire)
	{
		if (auto kv = kv_cast(handle); kv && key)
		{
			kv->set((int64_t)value, j2s(env, key), (uint32_t)expire);
		}
	}

	JNIEXPORT void JNICALL Java_love_yinlin_platform_KV_setFloat(JNIEnv* env, jobject, jlong handle, jstring key, jfloat value, jint expire)
	{
		if (auto kv = kv_cast(handle); kv && key)
		{
			kv->set((float)value, j2s(env, key), (uint32_t)expire);
		}
	}

	JNIEXPORT void JNICALL Java_love_yinlin_platform_KV_setDouble(JNIEnv* env, jobject, jlong handle, jstring key, jdouble value, jint expire)
	{
		if (auto kv = kv_cast(handle); kv && key)
		{
			kv->set((double)value, j2s(env, key), (uint32_t)expire);
		}
	}

	JNIEXPORT void JNICALL Java_love_yinlin_platform_KV_setString(JNIEnv* env, jobject, jlong handle, jstring key, jstring value, jint expire)
	{
		if (auto kv = kv_cast(handle); kv && key)
		{
			kv->set(j2s(env, value), j2s(env, key), (uint32_t)expire);
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
				kv->set(buffer, j2s(env, key), (uint32_t)expire);
			}
		}
	}

	JNIEXPORT jboolean JNICALL Java_love_yinlin_platform_KV_getBoolean(JNIEnv* env, jobject, jlong handle, jstring key, jboolean def)
	{
		if (auto kv = kv_cast(handle); kv && key)
		{
			return (jboolean)kv->getBool(j2s(env, key), (bool)def);
		}
		return def;
	}

	JNIEXPORT jint JNICALL Java_love_yinlin_platform_KV_getInt(JNIEnv* env, jobject, jlong handle, jstring key, jint def)
	{
		if (auto kv = kv_cast(handle); kv && key)
		{
			return (jint)kv->getInt32(j2s(env, key), (int32_t)def);
		}
		return def;
	}

	JNIEXPORT jlong JNICALL Java_love_yinlin_platform_KV_getLong(JNIEnv* env, jobject, jlong handle, jstring key, jlong def)
	{
		if (auto kv = kv_cast(handle); kv && key)
		{
			return (jlong)kv->getInt64(j2s(env, key), (int64_t)def);
		}
		return def;
	}

	JNIEXPORT jfloat JNICALL Java_love_yinlin_platform_KV_getFloat(JNIEnv* env, jobject, jlong handle, jstring key, jfloat def)
	{
		if (auto kv = kv_cast(handle); kv && key)
		{
			return (jfloat)kv->getFloat(j2s(env, key), (float)def);
		}
		return def;
	}

	JNIEXPORT jdouble JNICALL Java_love_yinlin_platform_KV_getDouble(JNIEnv* env, jobject, jlong handle, jstring key, jdouble def)
	{
		if (auto kv = kv_cast(handle); kv && key)
		{
			return (jdouble)kv->getDouble(j2s(env, key), (double)def);
		}
		return def;
	}

	JNIEXPORT jstring JNICALL Java_love_yinlin_platform_KV_getString(JNIEnv* env, jobject, jlong handle, jstring key, jstring def)
	{
		if (auto kv = kv_cast(handle); kv && key)
		{
			std::string value;
			bool hasValue = kv->getString(j2s(env, key), value);
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

	JNIEXPORT jboolean JNICALL Java_love_yinlin_platform_KV_has(JNIEnv* env, jobject, jlong handle, jstring key)
	{
		if (auto kv = kv_cast(handle); kv && key)
		{
			return (jboolean)kv->containsKey(j2s(env, key));
		}
		return false;
	}

	JNIEXPORT void JNICALL Java_love_yinlin_platform_KV_remove(JNIEnv* env, jobject, jlong handle, jstring key)
	{
		if (auto kv = kv_cast(handle); kv && key)
		{
			kv->removeValueForKey(j2s(env, key));
		}
	}

	JNIEXPORT jstring JNICALL Java_love_yinlin_platform_PickerKt_openFileDialog(
		JNIEnv* env,
		jobject,
		jlong parent,
		jstring title,
		jstring filterName,
		jstring filter
	) {
		auto titleStr = j2s(env, title);
		auto filterNameStr = j2s(env, filterName);
		auto filterStr = nfd_convert_filter(j2s(env, filter));

		NFD::Guard nfdGuard;
		NFD::UniquePath outPath;
		nfdresult_t result;
		if (!filterNameStr.empty() && !filterStr.empty()) {
			nfdfilteritem_t filterItem[1] = {{filterNameStr.data(), filterStr.data()}};
			result = NFD::OpenDialog(outPath, filterItem, 1, nullptr, nfd_get_window((void*)parent));
		} else {
			result = NFD::OpenDialog(outPath, nullptr, 0, nullptr, nfd_get_window((void*)parent));
		}
		if (result == NFD_OKAY) {
			auto pathStr = std::string{ outPath.get() };
			return s2j(env, pathStr);
		}
		return nullptr;
	}

	JNIEXPORT jobjectArray JNICALL Java_love_yinlin_platform_PickerKt_openMultipleFileDialog(
		JNIEnv* env,
		jobject,
		jlong parent,
		jint maxNum,
		jstring title,
		jstring filterName,
		jstring filter
	) {
		auto titleStr = j2s(env, title);
		auto filterNameStr = j2s(env, filterName);
		auto filterStr = nfd_convert_filter(j2s(env, filter));

		NFD::Guard nfdGuard;
		NFD::UniquePathSet outPaths;
		nfdresult_t result;
		if (!filterNameStr.empty() && !filterStr.empty()) {
			nfdfilteritem_t filterItem[1] = {{filterNameStr.data(), filterStr.data()}};
			result = NFD::OpenDialogMultiple(outPaths, filterItem, 1, nullptr, nfd_get_window((void*)parent));
		} else {
			result = NFD::OpenDialogMultiple(outPaths, (nfdfilteritem_t*)nullptr, 0, nullptr, nfd_get_window((void*)parent));
		}
		nfdpathsetsize_t numPaths = 0;
		if (result == NFD_OKAY) {
			NFD::PathSet::Count(outPaths, numPaths);
		}
		jclass cls = env->GetObjectClass(title);
		auto arr = env->NewObjectArray((jsize)numPaths, cls, nullptr);
		if (numPaths > 0) {
			for (nfdpathsetsize_t i = 0; i < numPaths && i < maxNum; i++) {
				NFD::UniquePathSetPath path;
				NFD::PathSet::GetPath(outPaths, i, path);
				auto pathStr = std::string{ path.get() };
				env->SetObjectArrayElement(arr, i, s2j(env, pathStr));
			}
		}
		return arr;
	}

	JNIEXPORT jstring JNICALL Java_love_yinlin_platform_PickerKt_saveFileDialog(
		JNIEnv* env,
		jobject,
		jlong parent,
		jstring title,
		jstring filename,
		jstring ext,
		jstring filterName
	) {
		auto titleStr = j2s(env, title);
		auto filenameStr = j2s(env, filename);
		auto extStr = nfd_convert_filter(j2s(env, ext));
		auto filterNameStr = j2s(env, filterName);

		NFD::Guard nfdGuard;
		NFD::UniquePath outPath;
		nfdresult_t result;
		if (!filterNameStr.empty() && !extStr.empty()) {
			nfdfilteritem_t filterItem[1] = {{filterNameStr.data(), extStr.data()}};
			result = NFD::SaveDialog(outPath, filterItem, 1, nullptr, filenameStr.data(), nfd_get_window((void*)parent));
		} else {
			result = NFD::SaveDialog(outPath, nullptr, 0, nullptr, filenameStr.data(), nfd_get_window((void*)parent));
		}
		if (result == NFD_OKAY) {
			auto pathStr = std::string{ outPath.get() };
			return s2j(env, pathStr);
		}
		return nullptr;
	}

	JNIEXPORT void JNICALL Java_love_yinlin_platform_ActualFloatingLyrics_modifyWindow(JNIEnv* env, jobject, jlong window, jboolean clickThrough) {
		ylcs_window_set_click_through((void*)window, (bool)clickThrough);
	}

	JNIEXPORT jboolean JNICALL Java_love_yinlin_MainKt_requestSingleInstance(JNIEnv* env, jobject) {
		return (jboolean)ylcs_single_instance_try_lock();
	}

	JNIEXPORT void JNICALL Java_love_yinlin_MainKt_releaseSingleInstance(JNIEnv* env, jobject) {
		ylcs_single_instance_unlock();
	}
}
