#include "macos_webview.h"
#import <WebKit/WebKit.h>

static char observationContext;

@interface RachelWebViewObservation : NSObject
@property(nonatomic, strong) WKWebView *view;
@property(nonatomic) RachelWebViewObservationListener listener;
@property(nonatomic) RachelWebViewObservationRelease releaseContext;
@property(nonatomic) void *context;
@property(nonatomic) NSUInteger observing;
- (void)close;
@end

@implementation RachelWebViewObservation
+ (NSArray<NSString *> *)keys {
    return @[@"estimatedProgress", @"title", @"URL", @"canGoBack", @"canGoForward"];
}

- (void)observeValueForKeyPath:(NSString *)keyPath ofObject:(id)object change:(NSDictionary *)change context:(void *)context {
    if (context == &observationContext) {
        if (self.listener) self.listener(self.context);
    }
    else [super observeValueForKeyPath:keyPath ofObject:object change:change context:context];
}

- (void)close {
    self.listener = NULL;
    NSArray<NSString *> *keys = RachelWebViewObservation.keys;
    while (self.observing > 0) {
        self.observing--;
        [self.view removeObserver:self forKeyPath:keys[self.observing] context:&observationContext];
    }
    self.view = nil;
}

- (void)dealloc {
    [self close];
    if (self.releaseContext) self.releaseContext(self.context);
}
@end

void *rachel_webview_observe(void *view, RachelWebViewObservationListener listener, RachelWebViewObservationRelease release, void *context) {
    RachelWebViewObservation *observer = [RachelWebViewObservation new];
    if (!observer) {
        if (release) release(context);
        return NULL;
    }
    observer.view = (__bridge WKWebView *)view;
    observer.listener = listener;
    observer.releaseContext = release;
    observer.context = context;
    for (NSString *key in RachelWebViewObservation.keys) {
        [observer.view addObserver:observer forKeyPath:key options:NSKeyValueObservingOptionNew context:&observationContext];
        observer.observing++;
    }
    return (__bridge_retained void *)observer;
}

void rachel_webview_unobserve(void *observation) {
    if (!observation) return;
    RachelWebViewObservation *observer = (__bridge_transfer RachelWebViewObservation *)observation;
    [observer close];
}
