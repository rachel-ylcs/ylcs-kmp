#define SDL_MAIN_HANDLED
#include <jni.h>
#include <SDL3/SDL.h>
#include <SDL3/SDL_main.h>
#include <chrono>
#include <codecvt>
#include <condition_variable>
#include <locale>
#include <mutex>
#include <string>
#include <vector>
#ifdef _WIN32
#include <windows.h>
#endif

#ifdef __APPLE__
void rachel_picker_present(SDL_FileDialogType type, SDL_DialogFileCallback callback, void *userdata, SDL_PropertiesID properties);
#else
static void rachel_picker_present(SDL_FileDialogType type, SDL_DialogFileCallback callback, void *userdata, SDL_PropertiesID properties) {
    SDL_ShowFileDialogWithProperties(type, callback, userdata, properties);
}
#endif

namespace {
    struct Selection {
        std::mutex mutex;
        std::condition_variable completed;
        bool ready = false;
        std::vector<std::string> files;
    };
    bool sdl_initialized = false;

    static void SDLCALL selected(void *userdata, const char *const *files, int) noexcept {
        auto *result = static_cast<Selection *>(userdata);
        std::lock_guard<std::mutex> lock(result->mutex);
        try {
            if (files) for (size_t i = 0; files[i]; ++i) result->files.emplace_back(files[i]);
        }
        catch (...) {
            result->files.clear();
        }
        result->ready = true;
        result->completed.notify_one();
    }

    static std::string string_from_java(JNIEnv *env, jstring value) {
        if (!value) return {};
        const jchar *chars = env->GetStringChars(value, nullptr);
        if (!chars) return {};
        std::u16string text(reinterpret_cast<const char16_t *>(chars), env->GetStringLength(value));
        env->ReleaseStringChars(value, chars);
        return std::wstring_convert<std::codecvt_utf8_utf16<char16_t>, char16_t>().to_bytes(text);
    }

    static jstring string_to_java(JNIEnv *env, const std::string &value) {
        auto text = std::wstring_convert<std::codecvt_utf8_utf16<char16_t>, char16_t>().from_bytes(value);
        return env->NewString(reinterpret_cast<const jchar *>(text.data()), static_cast<jsize>(text.size()));
    }

    struct Session {
        SDL_PropertiesID properties = 0;
#ifdef _WIN32
        HWND parent = nullptr;
        bool restore = false;
#endif
        explicit Session(jlong owner) {
#ifdef _WIN32
            parent = reinterpret_cast<HWND>(owner);
            restore = parent && IsWindow(parent) && IsWindowEnabled(parent);
            if (restore) EnableWindow(parent, FALSE);
#endif
        }
        ~Session() {
            if (properties) SDL_DestroyProperties(properties);
#ifdef _WIN32
            if (restore && IsWindow(parent)) {
                EnableWindow(parent, TRUE);
                SetForegroundWindow(parent);
            }
#endif
        }
    };

    static std::vector<std::string> dialog(JNIEnv *env, jlong owner, jstring title, jstring filterName, jstring filter, jstring filename, bool many) {
        Session session(owner);
        if (!sdl_initialized) {
            SDL_SetMainReady();
            if (!SDL_Init(SDL_INIT_EVENTS)) return {};
            sdl_initialized = true;
        }
        session.properties = SDL_CreateProperties();
        if (!session.properties) return {};
        auto titleText = string_from_java(env, title);
        auto name = string_from_java(env, filterName);
        auto pattern = string_from_java(env, filter);
        for (size_t pos = 0; (pos = pattern.find("*.", pos)) != std::string::npos;) pattern.erase(pos, 2);
        if (pattern.empty() || pattern == "*") pattern = "*";
        SDL_DialogFileFilter item {name.c_str(), pattern.c_str()};
        SDL_SetPointerProperty(session.properties, SDL_PROP_FILE_DIALOG_FILTERS_POINTER, &item);
        SDL_SetNumberProperty(session.properties, SDL_PROP_FILE_DIALOG_NFILTERS_NUMBER, 1);
        SDL_SetBooleanProperty(session.properties, SDL_PROP_FILE_DIALOG_MANY_BOOLEAN, many);
        SDL_SetStringProperty(session.properties, SDL_PROP_FILE_DIALOG_TITLE_STRING, titleText.c_str());
        if (filename) SDL_SetStringProperty(session.properties, SDL_PROP_FILE_DIALOG_LOCATION_STRING, string_from_java(env, filename).c_str());

        Selection result;
        rachel_picker_present(filename ? SDL_FILEDIALOG_SAVEFILE : SDL_FILEDIALOG_OPENFILE, selected, &result, session.properties);
        std::unique_lock<std::mutex> lock(result.mutex);
        while (!result.ready) {
            lock.unlock();
            SDL_PumpEvents();
            lock.lock();
            result.completed.wait_for(lock, std::chrono::milliseconds(10), [&result] { return result.ready; });
        }
        return std::move(result.files);
    }
}

extern "C" {
    JNIEXPORT jstring JNICALL Java_love_yinlin_picker_NativePicker_openFileDialog(JNIEnv *env, jclass, jlong owner, jstring title, jstring filterName, jstring filter) {
        try {
            auto paths = dialog(env, owner, title, filterName, filter, nullptr, false);
            return paths.empty() ? nullptr : string_to_java(env, paths.front());
        }
        catch (...) {
            return nullptr;
        }
    }

    JNIEXPORT jobjectArray JNICALL Java_love_yinlin_picker_NativePicker_openMultipleFileDialog(JNIEnv *env, jclass, jlong owner, jint maxNum, jstring title, jstring filterName, jstring filter) {
        try {
            auto paths = dialog(env, owner, title, filterName, filter, nullptr, maxNum > 1);
            auto stringClass = env->FindClass("java/lang/String");
            if (!stringClass) return nullptr;
            auto result = env->NewObjectArray(static_cast<jsize>(paths.size()), stringClass, nullptr);
            env->DeleteLocalRef(stringClass);
            if (!result) return nullptr;
            for (size_t i = 0; i < paths.size(); ++i) {
                auto path = string_to_java(env, paths[i]);
                if (!path) return nullptr;
                env->SetObjectArrayElement(result, static_cast<jsize>(i), path);
                env->DeleteLocalRef(path);
            }
            return result;
        }
        catch (...) {
            return nullptr;
        }
    }

    JNIEXPORT jstring JNICALL Java_love_yinlin_picker_NativePicker_saveFileDialog(JNIEnv *env, jclass, jlong owner, jstring title, jstring filename, jstring filter, jstring filterName) {
        try {
            auto paths = dialog(env, owner, title, filterName, filter, filename, false);
            return paths.empty() ? nullptr : string_to_java(env, paths.front());
        }
        catch (...) {
            return nullptr;
        }
    }
}
