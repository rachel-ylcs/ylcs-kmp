#pragma once

#include <native_jni.h>
#include <pag.h>

inline jint MakeColorInt(uint8_t red, uint8_t green, uint8_t blue) {
    uint32_t color = (255U << 24) | (red << 16) | (green << 8) | (blue << 0);
    return static_cast<jint>(color);
}

inline pag::Color ToColor(jint value) {
    auto color = static_cast<uint32_t>(value);
    auto red = (color >> 16) & 0xFFU;
    auto green = (color >> 8) & 0xFFU;
    auto blue = (color >> 0) & 0xFFU;
    return { static_cast<uint8_t>(red), static_cast<uint8_t>(green), static_cast<uint8_t>(blue) };
}

std::pair<jlong, int> JPAGLayerInstance(std::shared_ptr<pag::PAGLayer> pagLayer);

std::shared_ptr<pag::PAGLayer> PAGLayerInstance(jlong layerHandle, jint type);