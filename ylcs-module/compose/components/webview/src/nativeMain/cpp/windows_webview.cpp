#include "rachel_webview.h"

#include <windows.h>
#include <commctrl.h>
#include <shlobj.h>
#include <WebView2.h>
#include <new>
#include <string>
#include <utility>

namespace {

constexpr wchar_t windowClass[] = L"Rachel.Native.WebView2";

template <typename T>
class ComPtr {
    T *value_ = nullptr;
public:
    ComPtr() = default;
    explicit ComPtr(T *value) : value_(value) {}
    ComPtr(const ComPtr &) = delete;
    ComPtr &operator=(const ComPtr &) = delete;
    ComPtr(ComPtr &&other) noexcept : value_(std::exchange(other.value_, nullptr)) {}
    ~ComPtr() { reset(); }
    void reset(T *value = nullptr) {
        T *previous = std::exchange(value_, value);
        if (previous) previous->Release();
    }
    void retain(T *value) {
        if (value) value->AddRef();
        reset(value);
    }
    T *get() const { return value_; }
    T *operator->() const { return value_; }
    explicit operator bool() const { return value_ != nullptr; }
};

std::wstring wide(const char *text) {
    if (!text || !*text) return {};
    int length = MultiByteToWideChar(CP_UTF8, MB_ERR_INVALID_CHARS, text, -1, nullptr, 0);
    if (length <= 0) return {};
    std::wstring result(static_cast<size_t>(length), L'\0');
    if (!MultiByteToWideChar(CP_UTF8, MB_ERR_INVALID_CHARS, text, -1, result.data(), length)) return {};
    result.resize(static_cast<size_t>(length - 1));
    return result;
}

std::string utf8(const wchar_t *text) {
    if (!text || !*text) return {};
    int length = WideCharToMultiByte(CP_UTF8, WC_ERR_INVALID_CHARS, text, -1, nullptr, 0, nullptr, nullptr);
    if (length <= 0) return {};
    std::string result(static_cast<size_t>(length), '\0');
    if (!WideCharToMultiByte(CP_UTF8, WC_ERR_INVALID_CHARS, text, -1, result.data(), length, nullptr, nullptr)) return {};
    result.resize(static_cast<size_t>(length - 1));
    return result;
}

bool runtimeVersion(const wchar_t *value) {
    if (!value || !*value) return false;
    bool nonzero = false;
    for (const wchar_t *p = value; *p; ++p) {
        if (*p != L'.' && (*p < L'0' || *p > L'9')) return false;
        if (*p >= L'1' && *p <= L'9') nonzero = true;
    }
    return nonzero;
}

bool installedRuntime() {
    LPWSTR version = nullptr;
    HRESULT result = GetAvailableCoreWebView2BrowserVersionString(nullptr, &version);
    bool available = SUCCEEDED(result) && runtimeVersion(version);
    CoTaskMemFree(version);
    return available;
}

std::wstring profileDirectory() {
    PWSTR base = nullptr;
    HRESULT result = SHGetKnownFolderPath(FOLDERID_LocalAppData, KF_FLAG_CREATE, nullptr, &base);
    if (FAILED(result) || !base) return {};
    std::wstring directory;
    try { directory = base; }
    catch (...) { CoTaskMemFree(base); throw; }
    CoTaskMemFree(base);
    wchar_t executable[32768];
    DWORD count = GetModuleFileNameW(nullptr, executable, 32768);
    if (count == 0 || count >= 32768) return {};
    const wchar_t *name = wcsrchr(executable, L'\\');
    directory += L"\\Rachel\\WebView2\\";
    directory += name ? name + 1 : executable;
    int created = SHCreateDirectoryExW(nullptr, directory.c_str(), nullptr);
    if (created != ERROR_SUCCESS && created != ERROR_ALREADY_EXISTS && created != ERROR_FILE_EXISTS) return {};
    return directory;
}

class WebViewHost;

template <typename Interface, typename... Args>
class Callback final : public Interface {
    LONG references_ = 1;
    WebViewHost *owner_;
    HRESULT (WebViewHost::*method_)(Args...);
public:
    Callback(WebViewHost *owner, HRESULT (WebViewHost::*method)(Args...));
    ~Callback();
    HRESULT STDMETHODCALLTYPE QueryInterface(REFIID iid, void **value) override {
        if (!value) return E_POINTER;
        *value = nullptr;
        if (iid != __uuidof(IUnknown) && iid != __uuidof(Interface)) return E_NOINTERFACE;
        *value = static_cast<Interface *>(this);
        AddRef();
        return S_OK;
    }
    ULONG STDMETHODCALLTYPE AddRef() override { return static_cast<ULONG>(InterlockedIncrement(&references_)); }
    ULONG STDMETHODCALLTYPE Release() override {
        LONG count = InterlockedDecrement(&references_);
        if (count == 0) delete this;
        return static_cast<ULONG>(count);
    }
    HRESULT STDMETHODCALLTYPE Invoke(Args... args) override;
};

class WebViewHost {
    LONG references_ = 1;
    HWND window_ = nullptr;
    HWND root_ = nullptr;
    bool closed_ = false;
    bool started_ = false;
    bool initializedCom_ = false;
    bool javaScript_ = true;
    RachelWebViewListener listener_;
    RachelWebViewRelease release_;
    void *context_;
    std::wstring pendingUrl_;
    ComPtr<ICoreWebView2Environment> environment_;
    ComPtr<ICoreWebView2Controller> controller_;
    ComPtr<ICoreWebView2> webview_;
    EventRegistrationToken starting_{}, content_{}, completed_{}, source_{}, title_{}, history_{}, popup_{}, process_{};
    unsigned registered_ = 0;

    template <typename Interface, typename... Args>
    ComPtr<Interface> callback(HRESULT (WebViewHost::*method)(Args...)) {
        return ComPtr<Interface>(new(std::nothrow) Callback<Interface, Args...>(this, method));
    }

    template <typename Interface, typename Args>
    HRESULT listen(HRESULT (STDMETHODCALLTYPE ICoreWebView2::*add)(Interface *, EventRegistrationToken *), HRESULT (WebViewHost::*method)(ICoreWebView2 *, Args *), EventRegistrationToken &token, unsigned flag) {
        auto handler = callback<Interface>(method);
        if (!handler) return E_OUTOFMEMORY;
        HRESULT result = (webview_.get()->*add)(handler.get(), &token);
        if (SUCCEEDED(result)) registered_ |= flag;
        return result;
    }

    void emit(int event, int64_t value = 0, const wchar_t *text = nullptr) {
        if (closed_ || !listener_) return;
        std::string converted = utf8(text);
        listener_(context_, window_, event, value, text ? converted.c_str() : nullptr);
    }

    void fail(HRESULT result) { emit(RACHEL_WEBVIEW_UNAVAILABLE, static_cast<int64_t>(result)); }

    void text(int event, HRESULT (STDMETHODCALLTYPE ICoreWebView2::*getter)(LPWSTR *)) {
        LPWSTR value = nullptr;
        if (SUCCEEDED((webview_.get()->*getter)(&value))) {
            try { emit(event, 0, value); }
            catch (...) { CoTaskMemFree(value); throw; }
        }
        CoTaskMemFree(value);
    }

    void history() {
        BOOL back = FALSE, forward = FALSE;
        webview_->get_CanGoBack(&back);
        webview_->get_CanGoForward(&forward);
        emit(RACHEL_WEBVIEW_HISTORY, (back ? 1 : 0) | (forward ? 2 : 0));
    }

    void bounds() {
        if (!controller_ || !window_) return;
        RECT client{};
        if (GetClientRect(window_, &client)) controller_->put_Bounds(client);
        controller_->NotifyParentWindowPositionChanged();
    }

    HRESULT onEnvironment(HRESULT result, ICoreWebView2Environment *environment) {
        if (closed_) return S_OK;
        if (FAILED(result) || !environment) { fail(FAILED(result) ? result : E_FAIL); return S_OK; }
        LPWSTR version = nullptr;
        environment->get_BrowserVersionString(&version);
        bool available = runtimeVersion(version);
        CoTaskMemFree(version);
        if (!available) { fail(HRESULT_FROM_WIN32(ERROR_NOT_SUPPORTED)); return S_OK; }
        environment_.retain(environment);
        auto handler = callback<ICoreWebView2CreateCoreWebView2ControllerCompletedHandler>(&WebViewHost::onController);
        if (!handler) { fail(E_OUTOFMEMORY); return S_OK; }
        result = environment_->CreateCoreWebView2Controller(window_, handler.get());
        if (FAILED(result)) fail(result);
        return S_OK;
    }

    HRESULT onController(HRESULT result, ICoreWebView2Controller *controller) {
        if (closed_) {
            if (controller) controller->Close();
            return S_OK;
        }
        if (FAILED(result) || !controller) { fail(FAILED(result) ? result : E_FAIL); return S_OK; }
        controller_.retain(controller);
        ICoreWebView2 *webview = nullptr;
        result = controller_->get_CoreWebView2(&webview);
        if (FAILED(result) || !webview) { fail(FAILED(result) ? result : E_FAIL); return S_OK; }
        webview_.reset(webview);
        result = listen(&ICoreWebView2::add_NavigationStarting, &WebViewHost::onStarting, starting_, 1);
        if (SUCCEEDED(result)) result = listen(&ICoreWebView2::add_ContentLoading, &WebViewHost::onContent, content_, 2);
        if (SUCCEEDED(result)) result = listen(&ICoreWebView2::add_NavigationCompleted, &WebViewHost::onCompleted, completed_, 4);
        if (SUCCEEDED(result)) result = listen(&ICoreWebView2::add_SourceChanged, &WebViewHost::onSource, source_, 8);
        if (SUCCEEDED(result)) result = listen(&ICoreWebView2::add_DocumentTitleChanged, &WebViewHost::onTitle, title_, 16);
        if (SUCCEEDED(result)) result = listen(&ICoreWebView2::add_HistoryChanged, &WebViewHost::onHistory, history_, 32);
        if (SUCCEEDED(result)) result = listen(&ICoreWebView2::add_NewWindowRequested, &WebViewHost::onPopup, popup_, 64);
        if (SUCCEEDED(result)) result = listen(&ICoreWebView2::add_ProcessFailed, &WebViewHost::onProcess, process_, 128);
        if (FAILED(result)) { fail(result); return S_OK; }
        configure(javaScript_);
        bounds();
        controller_->put_IsVisible(TRUE);
        emit(RACHEL_WEBVIEW_READY);
        if (!pendingUrl_.empty()) {
            std::wstring target = std::move(pendingUrl_);
            result = webview_->Navigate(target.c_str());
            if (FAILED(result)) emit(RACHEL_WEBVIEW_ERROR, result, L"Cannot navigate to the URL");
        }
        return S_OK;
    }

    HRESULT onStarting(ICoreWebView2 *, ICoreWebView2NavigationStartingEventArgs *) { emit(RACHEL_WEBVIEW_STARTED); return S_OK; }
    HRESULT onContent(ICoreWebView2 *, ICoreWebView2ContentLoadingEventArgs *) { emit(RACHEL_WEBVIEW_CONTENT); return S_OK; }
    HRESULT onCompleted(ICoreWebView2 *, ICoreWebView2NavigationCompletedEventArgs *args) {
        if (closed_) return S_OK;
        BOOL success = FALSE;
        args->get_IsSuccess(&success);
        if (!success) {
            COREWEBVIEW2_WEB_ERROR_STATUS error;
            if (SUCCEEDED(args->get_WebErrorStatus(&error)) && error != COREWEBVIEW2_WEB_ERROR_STATUS_OPERATION_CANCELED) emit(RACHEL_WEBVIEW_ERROR, error, L"WebView2 navigation failed");
        }
        text(RACHEL_WEBVIEW_URL, &ICoreWebView2::get_Source);
        text(RACHEL_WEBVIEW_TITLE, &ICoreWebView2::get_DocumentTitle);
        history();
        emit(RACHEL_WEBVIEW_FINISHED);
        return S_OK;
    }
    HRESULT onSource(ICoreWebView2 *, ICoreWebView2SourceChangedEventArgs *) { if (!closed_) text(RACHEL_WEBVIEW_URL, &ICoreWebView2::get_Source); return S_OK; }
    HRESULT onTitle(ICoreWebView2 *, IUnknown *) { if (!closed_) text(RACHEL_WEBVIEW_TITLE, &ICoreWebView2::get_DocumentTitle); return S_OK; }
    HRESULT onHistory(ICoreWebView2 *, IUnknown *) { if (!closed_) history(); return S_OK; }
    HRESULT onPopup(ICoreWebView2 *, ICoreWebView2NewWindowRequestedEventArgs *args) { args->put_Handled(TRUE); return S_OK; }
    HRESULT onProcess(ICoreWebView2 *, ICoreWebView2ProcessFailedEventArgs *) { emit(RACHEL_WEBVIEW_ERROR, E_FAIL, L"The WebView2 process failed"); emit(RACHEL_WEBVIEW_FINISHED); return S_OK; }

    void close() {
        if (closed_) return;
        closed_ = true;
        if (root_ && IsWindow(root_)) RemoveWindowSubclass(root_, rootProcedure, reinterpret_cast<UINT_PTR>(this));
        root_ = nullptr;
        if (webview_) {
            if (registered_ & 1) webview_->remove_NavigationStarting(starting_);
            if (registered_ & 2) webview_->remove_ContentLoading(content_);
            if (registered_ & 4) webview_->remove_NavigationCompleted(completed_);
            if (registered_ & 8) webview_->remove_SourceChanged(source_);
            if (registered_ & 16) webview_->remove_DocumentTitleChanged(title_);
            if (registered_ & 32) webview_->remove_HistoryChanged(history_);
            if (registered_ & 64) webview_->remove_NewWindowRequested(popup_);
            if (registered_ & 128) webview_->remove_ProcessFailed(process_);
        }
        registered_ = 0;
        if (controller_) controller_->Close();
        webview_.reset();
        controller_.reset();
        environment_.reset();
    }

    static LRESULT CALLBACK rootProcedure(HWND window, UINT message, WPARAM wparam, LPARAM lparam, UINT_PTR, DWORD_PTR data) {
        auto *host = reinterpret_cast<WebViewHost *>(data);
        if (!host->closed_ && host->controller_ && (message == WM_MOVE || message == WM_MOVING || message == WM_WINDOWPOSCHANGED)) host->controller_->NotifyParentWindowPositionChanged();
        if (message == WM_NCDESTROY) host->root_ = nullptr;
        return DefSubclassProc(window, message, wparam, lparam);
    }

public:
    WebViewHost(RachelWebViewListener listener, RachelWebViewRelease release, void *context) : listener_(listener), release_(release), context_(context) {}
    ~WebViewHost() {
        if (release_) release_(context_);
        if (initializedCom_) CoUninitialize();
    }
    void retain() { InterlockedIncrement(&references_); }
    void release() { if (InterlockedDecrement(&references_) == 0) delete this; }
    void unavailable() { fail(E_OUTOFMEMORY); }

    template <typename Interface, typename... Args> friend class Callback;

    void start() {
        if (closed_ || started_) return;
        started_ = true;
        HRESULT result = CoInitializeEx(nullptr, COINIT_APARTMENTTHREADED);
        if (FAILED(result)) { fail(result); return; }
        initializedCom_ = true;
        if (!installedRuntime()) { fail(HRESULT_FROM_WIN32(ERROR_FILE_NOT_FOUND)); return; }
        auto directory = profileDirectory();
        if (directory.empty()) { fail(E_ACCESSDENIED); return; }
        auto handler = callback<ICoreWebView2CreateCoreWebView2EnvironmentCompletedHandler>(&WebViewHost::onEnvironment);
        if (!handler) { fail(E_OUTOFMEMORY); return; }
        result = CreateCoreWebView2EnvironmentWithOptions(nullptr, directory.c_str(), nullptr, handler.get());
        if (FAILED(result)) fail(result);
    }

    void configure(bool script) {
        javaScript_ = script;
        if (!webview_ || closed_) return;
        ICoreWebView2Settings *settings = nullptr;
        if (SUCCEEDED(webview_->get_Settings(&settings)) && settings) {
            settings->put_IsScriptEnabled(script);
            settings->Release();
        }
    }

    void load(const char *url) {
        if (closed_) return;
        std::wstring target = wide(url);
        if (target.empty()) target = L"about:blank";
        if (!webview_) pendingUrl_ = std::move(target);
        else {
            HRESULT result = webview_->Navigate(target.c_str());
            if (FAILED(result)) emit(RACHEL_WEBVIEW_ERROR, result, L"Cannot navigate to the URL");
        }
    }
    void back() { if (!closed_ && webview_) webview_->GoBack(); }
    void forward() { if (!closed_ && webview_) webview_->GoForward(); }
    void script(const char *source) { if (!closed_ && webview_) webview_->ExecuteScript(wide(source).c_str(), nullptr); }

    static WebViewHost *from(HWND window) {
        if (!window || !IsWindow(window)) return nullptr;
        wchar_t name[64];
        if (!GetClassNameW(window, name, 64) || wcscmp(name, windowClass) != 0) return nullptr;
        return reinterpret_cast<WebViewHost *>(GetWindowLongPtrW(window, GWLP_USERDATA));
    }

    static LRESULT CALLBACK procedure(HWND window, UINT message, WPARAM wparam, LPARAM lparam) {
        auto *host = reinterpret_cast<WebViewHost *>(GetWindowLongPtrW(window, GWLP_USERDATA));
        if (message == WM_NCCREATE) {
            host = static_cast<WebViewHost *>(reinterpret_cast<CREATESTRUCTW *>(lparam)->lpCreateParams);
            host->retain();
            host->window_ = window;
            SetWindowLongPtrW(window, GWLP_USERDATA, reinterpret_cast<LONG_PTR>(host));
            host->root_ = GetAncestor(window, GA_ROOT);
            if (host->root_ && !SetWindowSubclass(host->root_, rootProcedure, reinterpret_cast<UINT_PTR>(host), reinterpret_cast<DWORD_PTR>(host))) host->root_ = nullptr;
        }
        if (host) {
            if (message == WM_SIZE || message == WM_WINDOWPOSCHANGED) host->bounds();
            if (message == WM_SETFOCUS && host->controller_) host->controller_->MoveFocus(COREWEBVIEW2_MOVE_FOCUS_REASON_PROGRAMMATIC);
            if (message == WM_NCDESTROY) {
                SetWindowLongPtrW(window, GWLP_USERDATA, 0);
                host->close();
                host->window_ = nullptr;
                host->release();
            }
        }
        return DefWindowProcW(window, message, wparam, lparam);
    }
};

template <typename Interface, typename... Args>
Callback<Interface, Args...>::Callback(WebViewHost *owner, HRESULT (WebViewHost::*method)(Args...)) : owner_(owner), method_(method) { owner_->retain(); }

template <typename Interface, typename... Args>
Callback<Interface, Args...>::~Callback() { owner_->release(); }

template <typename Interface, typename... Args>
HRESULT STDMETHODCALLTYPE Callback<Interface, Args...>::Invoke(Args... args) {
    try { return (owner_->*method_)(args...); }
    catch (...) { owner_->unavailable(); return E_OUTOFMEMORY; }
}

}

extern "C" bool rachel_webview_available(void) {
    return installedRuntime();
}

extern "C" void *rachel_webview_create(void *parent, RachelWebViewListener listener, RachelWebViewRelease release, void *context) {
    auto *host = new(std::nothrow) WebViewHost(listener, release, context);
    if (!host) {
        if (release) release(context);
        return nullptr;
    }
    WNDCLASSEXW definition{};
    definition.cbSize = sizeof(definition);
    definition.lpfnWndProc = WebViewHost::procedure;
    definition.hInstance = GetModuleHandleW(nullptr);
    definition.lpszClassName = windowClass;
    definition.hCursor = LoadCursorW(nullptr, IDC_ARROW);
    HWND result = nullptr;
    if (RegisterClassExW(&definition) || GetLastError() == ERROR_CLASS_ALREADY_EXISTS) result = CreateWindowExW(WS_EX_CONTROLPARENT, windowClass, L"", WS_CHILD | WS_CLIPCHILDREN | WS_CLIPSIBLINGS, 0, 0, 0, 0, static_cast<HWND>(parent), nullptr, definition.hInstance, host);
    host->release();
    return result;
}

extern "C" void rachel_webview_start(void *window) {
    if (auto *host = WebViewHost::from(static_cast<HWND>(window))) {
        try { host->start(); }
        catch (...) { host->unavailable(); }
    }
}
extern "C" void rachel_webview_configure(void *window, bool script) {
    if (auto *host = WebViewHost::from(static_cast<HWND>(window))) host->configure(script);
}
extern "C" void rachel_webview_load(void *window, const char *url) {
    if (auto *host = WebViewHost::from(static_cast<HWND>(window))) {
        try { host->load(url); }
        catch (...) { host->unavailable(); }
    }
}
extern "C" void rachel_webview_back(void *window) { if (auto *host = WebViewHost::from(static_cast<HWND>(window))) host->back(); }
extern "C" void rachel_webview_forward(void *window) { if (auto *host = WebViewHost::from(static_cast<HWND>(window))) host->forward(); }
extern "C" void rachel_webview_script(void *window, const char *source) {
    if (auto *host = WebViewHost::from(static_cast<HWND>(window))) {
        try { host->script(source); }
        catch (...) { host->unavailable(); }
    }
}
extern "C" void rachel_webview_close(void *window) {
    auto handle = static_cast<HWND>(window);
    if (WebViewHost::from(handle)) DestroyWindow(handle);
}
