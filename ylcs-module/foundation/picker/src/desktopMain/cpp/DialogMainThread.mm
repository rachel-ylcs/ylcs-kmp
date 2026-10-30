#include <SDL3/SDL.h>
#import <Cocoa/Cocoa.h>
#import <dispatch/dispatch.h>

void rachel_picker_present(SDL_FileDialogType type, SDL_DialogFileCallback callback, void *userdata, SDL_PropertiesID properties) {
    dispatch_async(dispatch_get_main_queue(), ^{
        [NSApplication sharedApplication];
        SDL_ShowFileDialogWithProperties(type, callback, userdata, properties);
    });
}
