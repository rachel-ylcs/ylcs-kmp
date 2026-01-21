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

struct LayerInfo {
    jlong data[2];

    LayerInfo() : data{ } { }
    LayerInfo(void* addr, bool) : data{ reinterpret_cast<jlong>(addr), static_cast<jlong>(-1) } { }
    LayerInfo(jlong handle, jlong type) : data{ handle, type } { }
    LayerInfo(void* addr, pag::LayerType rawType) : data{ reinterpret_cast<jlong>(addr), static_cast<jlong>(rawType) } { }
    template<typename T = void>
    inline T* handle() { return reinterpret_cast<T*>(data[0]); }
    inline jlong type() const { return data[1]; }
    inline pag::LayerType rawType() const { return data[1] == -1LL ? pag::LayerType::PreCompose : static_cast<pag::LayerType>(data[1]); }
    inline bool empty() const { return data[0] == 0LL;}
    inline bool file() const { return data[1] == -1LL; }
    inline operator jlong* () { return data; }
    inline operator const jlong* () const { return data; }
};

LayerInfo JPAGLayerInstance(std::shared_ptr<pag::PAGLayer> pagLayer);

std::shared_ptr<pag::PAGLayer> PAGLayerInstance(LayerInfo info);