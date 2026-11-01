#ifndef RACHEL_WEBVIEW_H
#define RACHEL_WEBVIEW_H

#include <stdbool.h>
#include <stdint.h>

#ifdef __cplusplus
extern "C" {
#endif

enum {
    RACHEL_WEBVIEW_READY = 1,
    RACHEL_WEBVIEW_UNAVAILABLE = 2,
    RACHEL_WEBVIEW_STARTED = 3,
    RACHEL_WEBVIEW_CONTENT = 4,
    RACHEL_WEBVIEW_FINISHED = 5,
    RACHEL_WEBVIEW_URL = 6,
    RACHEL_WEBVIEW_TITLE = 7,
    RACHEL_WEBVIEW_HISTORY = 8,
    RACHEL_WEBVIEW_ERROR = 9
};

typedef void (*RachelWebViewListener)(void *context, void *window, int32_t event, int64_t value, const char *text);
typedef void (*RachelWebViewRelease)(void *context);

bool rachel_webview_available(void);
void *rachel_webview_create(void *parent, RachelWebViewListener listener, RachelWebViewRelease release, void *context);
void rachel_webview_start(void *window);
void rachel_webview_configure(void *window, bool java_script);
void rachel_webview_load(void *window, const char *url);
void rachel_webview_back(void *window);
void rachel_webview_forward(void *window);
void rachel_webview_script(void *window, const char *script);
void rachel_webview_close(void *window);

#ifdef __cplusplus
}
#endif

#endif
