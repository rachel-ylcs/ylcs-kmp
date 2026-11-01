#ifndef RACHEL_MACOS_WEBVIEW_H
#define RACHEL_MACOS_WEBVIEW_H

#ifdef __cplusplus
extern "C" {
#endif

typedef void (*RachelWebViewObservationListener)(void *context);
typedef void (*RachelWebViewObservationRelease)(void *context);

void *rachel_webview_observe(void *view, RachelWebViewObservationListener listener, RachelWebViewObservationRelease release, void *context);
void rachel_webview_unobserve(void *observation);

#ifdef __cplusplus
}
#endif

#endif
