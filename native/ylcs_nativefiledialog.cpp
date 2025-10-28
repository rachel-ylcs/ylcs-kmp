#include "ylcs_jni.h"
#include "nfd.hpp"

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
			for (nfdpathsetsize_t i = 0; i < numPaths && i < static_cast<nfdpathsetsize_t>(maxNum); i++) {
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
}