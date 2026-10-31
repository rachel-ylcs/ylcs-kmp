#ifndef RACHEL_AUDIO_H
#define RACHEL_AUDIO_H

#include <stdbool.h>
#include <stdint.h>

#ifdef __cplusplus
extern "C" {
#endif

enum { RACHEL_AUDIO_EVENT_STATE, RACHEL_AUDIO_EVENT_DURATION, RACHEL_AUDIO_EVENT_SOURCE, RACHEL_AUDIO_EVENT_ENDED, RACHEL_AUDIO_EVENT_ERROR, RACHEL_AUDIO_EVENT_FRAME };

typedef struct RachelAudioEvent {
    int64_t revision;
    int kind;
    int64_t value;
    const char *message;
} RachelAudioEvent;

typedef void (*RachelAudioCallback)(void *context, const RachelAudioEvent *event);
typedef void (*RachelAudioCallbackRelease)(void *context);

typedef struct RachelAudioSnapshot {
    bool playing;
    int64_t position;
    int64_t duration;
    int64_t error;
} RachelAudioSnapshot;

typedef struct RachelVideoFrameInfo {
    int width;
    int height;
    int64_t serial;
    void *texture;
} RachelVideoFrameInfo;

void *rachel_audio_create(bool music, RachelAudioCallback callback, RachelAudioCallbackRelease release, void *context);
void rachel_audio_release(void *handle);
bool rachel_audio_load(void *handle, const char *path, int64_t revision);
bool rachel_audio_play(void *handle, int64_t revision);
bool rachel_audio_pause(void *handle, int64_t revision);
bool rachel_audio_seek(void *handle, int64_t position, int64_t revision);
void rachel_audio_stop(void *handle, int64_t revision);
void rachel_audio_query(void *handle, int64_t revision, RachelAudioSnapshot *snapshot);
void *rachel_video_create(RachelAudioCallback callback, RachelAudioCallbackRelease release, void *context);
bool rachel_video_info(void *handle, int64_t revision, RachelVideoFrameInfo *info);
bool rachel_video_copy_frame(void *handle, int64_t revision, void *pixels, int64_t capacity, int width, int height);
void *rachel_video_gpu_frame(void *handle, int64_t revision, void *context, RachelVideoFrameInfo *info);
bool rachel_video_gpu_complete(void *frame, void *renderTarget);
void rachel_video_gpu_release(void *frame);
void *rachel_sound_create(void);
void rachel_sound_release(void *handle);
bool rachel_sound_add_file(void *handle, const char *path);
bool rachel_sound_add_memory(void *handle, const void *data, int64_t size);
bool rachel_sound_play(void *handle, int index);

#ifdef __cplusplus
}
#endif

#endif
