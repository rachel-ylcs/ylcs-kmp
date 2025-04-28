#include "../include/platform.h"
#include <vector>

#if defined(_WIN32) // Windows

#include "../include/platform_win_com.h"

static void setOpenDialogInfo(IFileDialog* dialog, DWORD maxNum, std::wstring_view title, std::wstring_view filterName, std::wstring_view filter) {
	dialog->SetTitle(title.data());
	DWORD options;
	dialog->GetOptions(&options);
	options |= FOS_FORCEFILESYSTEM | FOS_PATHMUSTEXIST | FOS_FILEMUSTEXIST;
	if (maxNum > 1U) options |= FOS_ALLOWMULTISELECT;
	dialog->SetOptions(options);
	COMDLG_FILTERSPEC filterSpec[] = { {
		filterName.empty() ? L"所有文件" : filterName.data(),
		filter.empty() ? L"*.*" : filter.data()
	} };
	dialog->SetFileTypes(ARRAYSIZE(filterSpec), filterSpec);
}

static std::wstring openFileDialog(std::wstring_view title, std::wstring_view filterName, std::wstring_view filter) {
	COM_ENVIRONMENT env;
	if (!env) return L"";
	COM_OBJECT<IFileOpenDialog> dialog(CLSID_FileOpenDialog, IID_IFileOpenDialog);
	if (!dialog) return L"";
	setOpenDialogInfo(dialog.handler, 1, title, filterName, filter);
	if (!SUCCEEDED(dialog->Show(nullptr))) return L"";
	COM_ITEM<IShellItem> item;
	if (!SUCCEEDED(dialog->GetResult(item))) return L"";
	COM_MEM<LPWSTR> selectedPath;
	if (!SUCCEEDED(item->GetDisplayName(SIGDN_FILESYSPATH, selectedPath))) return L"";
	return selectedPath.handler;
}

static std::vector<std::wstring> openMultipleFileDialog(DWORD maxNum, std::wstring_view title, std::wstring_view filterName, std::wstring_view filter) {
	std::vector<std::wstring> result;

	COM_ENVIRONMENT env;
	if (!env) return result;
	COM_OBJECT<IFileOpenDialog> dialog(CLSID_FileOpenDialog, IID_IFileOpenDialog);
	if (!dialog) return result;
	setOpenDialogInfo(dialog.handler, maxNum, title, filterName, filter);
	if (!SUCCEEDED(dialog->Show(nullptr))) return result;
	COM_ITEM<IShellItemArray> items;
	if (!SUCCEEDED(dialog->GetResults(items))) return result;
	DWORD count{};
	items->GetCount(&count);
	if (count <= 0 || count > maxNum) return result;
	for (DWORD i = 0U; i < count; ++i) {
		COM_ITEM<IShellItem> item;
		items->GetItemAt(i, item);
		COM_MEM<LPWSTR> path;
		if (SUCCEEDED(item->GetDisplayName(SIGDN_FILESYSPATH, path))) result.emplace_back(path.handler);
	}

	return result;
}

static std::wstring saveFileDialog(std::wstring_view title, std::wstring_view filename, std::wstring_view ext, std::wstring_view filterName) {
	COM_ENVIRONMENT env;
	if (!env) return L"";
	COM_OBJECT<IFileOpenDialog> dialog(CLSID_FileSaveDialog, IID_IFileSaveDialog);
	setOpenDialogInfo(dialog.handler, 1, title, filterName, ext);
	dialog->SetFileName(filename.data());
	if (!ext.empty()) dialog->SetDefaultExtension(ext.data());
	if (!SUCCEEDED(dialog->Show(nullptr))) return L"";
	COM_ITEM<IShellItem> item;
	if (!SUCCEEDED(dialog->GetResult(item))) return L"";
	COM_MEM<LPWSTR> selectedPath;
	if (!SUCCEEDED(item->GetDisplayName(SIGDN_FILESYSPATH, selectedPath))) return L"";
	return selectedPath.handler;
}

extern "C" {
	JNIEXPORT jstring JNICALL Java_love_yinlin_platform_PickerKt_openFileDialog(
		JNIEnv* env,
		jobject,
		jstring title,
		jstring filterName,
		jstring filter
	) {
		std::wstring result = openFileDialog(j2w(env, title), j2w(env, filterName), j2w(env, filter));
		return result.empty() ? nullptr : w2j(env, result);
	}

	JNIEXPORT jobjectArray JNICALL Java_love_yinlin_platform_PickerKt_openMultipleFileDialog(
		JNIEnv* env,
		jobject,
		jint maxNum,
		jstring title,
		jstring filterName,
		jstring filter
	) {
		std::vector<std::wstring> result;
		if (maxNum > 0U) result = openMultipleFileDialog((DWORD)maxNum, j2w(env, title), j2w(env, filterName), j2w(env, filter));
		jclass cls = env->GetObjectClass(title);
		auto arr = env->NewObjectArray((jsize)result.size(), cls, nullptr);
		jsize length = (jsize)result.size();
		for (jsize i = 0; i < length; ++i) env->SetObjectArrayElement(arr, i, w2j(env, result[i]));
		return arr;
	}

	JNIEXPORT jstring JNICALL Java_love_yinlin_platform_PickerKt_saveFileDialog(
		JNIEnv* env,
		jobject,
		jstring title,
		jstring filename,
		jstring ext,
		jstring filterName
	) {
		std::wstring result = saveFileDialog(j2w(env, title), j2w(env, filename), j2w(env, ext), j2w(env, filterName));
		return result.empty() ? nullptr : w2j(env, result);
	}
}

#elif defined(__APPLE__) // Mac

#include "../include/platform_mac.h"

#else // Linux

#include "../include/platform_linux.h"

#endif