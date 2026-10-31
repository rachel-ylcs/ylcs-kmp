#pragma once
#include "rachel_audio_internal.h"
#include <gst/app/gstappsink.h>
#include <gst/video/video.h>
#include <dlfcn.h>

class LinuxGstreamer {
    void *core = nullptr;
    void *app = nullptr;
    void *video = nullptr;

    template <typename T> T symbol(const char *name) {
        auto value = dlsym(core, name);
        if (!value) value = dlsym(app, name);
        if (!value) value = dlsym(video, name);
        if (!value) throw RachelAudioError(-1, std::string("Missing GStreamer function: ") + name);
        return reinterpret_cast<T>(value);
    }

    LinuxGstreamer();
public:
#define RACHEL_GST_FUNCTIONS(F) \
    F(gst_segtrap_set_enabled) \
    F(gst_init_check) \
    F(gst_element_factory_make) \
    F(gst_object_ref_sink) \
    F(gst_object_unref) \
    F(gst_element_set_state) \
    F(gst_element_get_bus) \
    F(gst_bus_get_pollfd) \
    F(gst_bus_timed_pop) \
    F(gst_bus_set_flushing) \
    F(gst_message_parse_error) \
    F(gst_message_parse_state_changed) \
    F(gst_message_parse_buffering) \
    F(gst_element_query_duration) \
    F(gst_element_query_position) \
    F(gst_element_seek_simple) \
    F(gst_filename_to_uri) \
    F(gst_caps_from_string) \
    F(gst_app_sink_set_caps) \
    F(gst_app_sink_set_callbacks) \
    F(gst_app_sink_pull_preroll) \
    F(gst_app_sink_pull_sample) \
    F(gst_sample_get_caps) \
    F(gst_sample_get_buffer) \
    F(gst_mini_object_ref) \
    F(gst_mini_object_unref) \
    F(gst_video_info_from_caps) \
    F(gst_video_frame_map) \
    F(gst_video_frame_unmap) \
    F(g_object_set) \
    F(g_error_free) \
    F(g_free)
#define RACHEL_GST_DECLARE(name) decltype(&::name) name = nullptr;
    RACHEL_GST_FUNCTIONS(RACHEL_GST_DECLARE)
#undef RACHEL_GST_DECLARE

    static LinuxGstreamer &instance();
};
