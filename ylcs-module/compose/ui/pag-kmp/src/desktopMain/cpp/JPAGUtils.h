#include <pag.h>

inline jint MakeColorInt(uint32_t red, uint32_t green, uint32_t blue) {
    uint32_t color = (255 << 24) | (red << 16) | (green << 8) | (blue << 0);
    return static_cast<int>(color);
}

inline pag::Color ToColor(jint value) {
    auto color = static_cast<uint32_t>(value);
    auto red = (((color) >> 16) & 0xFF);
    auto green = (((color) >> 8) & 0xFF);
    auto blue = (((color) >> 0) & 0xFF);
    return { static_cast<uint8_t>(red), static_cast<uint8_t>(green), static_cast<uint8_t>(blue) };
}