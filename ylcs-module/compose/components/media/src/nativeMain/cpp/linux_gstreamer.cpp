#include "linux_gstreamer.h"

LinuxGstreamer::LinuxGstreamer() {
    core = dlopen("libgstreamer-1.0.so.0", RTLD_NOW | RTLD_LOCAL);
    app = dlopen("libgstapp-1.0.so.0", RTLD_NOW | RTLD_LOCAL);
    video = dlopen("libgstvideo-1.0.so.0", RTLD_NOW | RTLD_LOCAL);
    try {
        if (!core || !app || !video) throw RachelAudioError(-1, "Video playback requires system GStreamer 1.18+ and its base and codec plugins");
#define RACHEL_GST_LOAD(name) name = symbol<decltype(name)>(#name);
        RACHEL_GST_FUNCTIONS(RACHEL_GST_LOAD)
#undef RACHEL_GST_LOAD
        gst_segtrap_set_enabled(FALSE);
        GError *failure = nullptr;
        if (!gst_init_check(nullptr, nullptr, &failure)) {
            const std::string message = failure ? failure->message : "Cannot initialize GStreamer";
            const auto code = failure ? failure->code : -1;
            if (failure) g_error_free(failure);
            throw RachelAudioError(code, message);
        }
    }
    catch (...) {
        if (video) dlclose(video);
        if (app) dlclose(app);
        if (core) dlclose(core);
        throw;
    }
}

LinuxGstreamer &LinuxGstreamer::instance() {
    static LinuxGstreamer api;
    return api;
}
