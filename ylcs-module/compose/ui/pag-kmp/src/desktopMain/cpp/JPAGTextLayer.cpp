#include "JPAGTextLayer.h"

using namespace pag;

static inline std::shared_ptr<PAGTextLayer> obj_cast(jlong handle) {
    auto jPagLayer = reinterpret_cast<JPAGTextLayer*>(handle);
    return jPagLayer ? jPagLayer->get() : nullptr;
}

extern "C" {
    JNIEXPORT jlong JNICALL Java_org_libpag_PAGTextLayer_nativeMake(JNIEnv* env, jclass, jlong duration, jstring text, jfloat font_size, jstring font_family, jstring font_style) {
        auto jPagLayer = PAGTextLayer::Make(duration, j2s(env, text), font_size, j2s(env, font_family), j2s(env, font_style));
        return jPagLayer ? reinterpret_cast<jlong>(new JPAGTextLayer(jPagLayer)) : 0LL;
    }

    JNIEXPORT void JNICALL Java_org_libpag_PAGTextLayer_nativeRelease(JNIEnv* env, jclass, jlong handle) {
        auto jPagLayer = reinterpret_cast<JPAGTextLayer*>(handle);
        if (jPagLayer) delete jPagLayer;
    }

    JNIEXPORT jint JNICALL Java_org_libpag_PAGTextLayer_nativeFillColor(JNIEnv* env, jclass, jlong handle) {
        auto pagLayer = obj_cast(handle);
        if (pagLayer) {
            auto color = pagLayer->fillColor();
            return MakeColorInt(color.red, color.green, color.blue);
        }
        return 0;
    }

    JNIEXPORT void JNICALL Java_org_libpag_PAGTextLayer_nativeSetFillColor(JNIEnv* env, jclass, jlong handle, jint color) {
        auto pagLayer = obj_cast(handle);
        if (pagLayer) pagLayer->setFillColor(ToColor(color));
    }

    JNIEXPORT jobjectArray JNICALL Java_org_libpag_PAGTextLayer_nativeFont(JNIEnv* env, jclass, jlong handle) {
        auto pagLayer = obj_cast(handle);
        if (pagLayer) {
            auto font = pagLayer->font();
            auto family = s2j(env, font.fontFamily);
            auto style = s2j(env, font.fontStyle);
            auto clz = env->GetObjectClass(family);
            auto result = env->NewObjectArray(2, clz, nullptr);
            env->SetObjectArrayElement(result, 0, family);
            env->SetObjectArrayElement(result, 1, style);
            env->DeleteLocalRef(family);
            env->DeleteLocalRef(style);
            env->DeleteLocalRef(clz);
            return result;
        }
        return nullptr;
    }

    JNIEXPORT void JNICALL Java_org_libpag_PAGTextLayer_nativeSetFont(JNIEnv* env, jclass, jlong handle, jstring font_family, jstring font_style) {
        auto pagLayer = obj_cast(handle);
        if (pagLayer) pagLayer->setFont(PAGFont(j2s(env, font_family), j2s(env, font_style)));
    }

    JNIEXPORT jfloat JNICALL Java_org_libpag_PAGTextLayer_nativeFontSize(JNIEnv* env, jclass, jlong handle) {
        auto pagLayer = obj_cast(handle);
        return pagLayer ? pagLayer->fontSize() : 0.0F;
    }

    JNIEXPORT void JNICALL Java_org_libpag_PAGTextLayer_nativeSetFontSize(JNIEnv* env, jclass, jlong handle, jfloat font_size) {
        auto pagLayer = obj_cast(handle);
        if (pagLayer) pagLayer->setFontSize(font_size);
    }

    JNIEXPORT jint JNICALL Java_org_libpag_PAGTextLayer_nativeStrokeColor(JNIEnv* env, jclass, jlong handle) {
        auto pagLayer = obj_cast(handle);
        if (pagLayer) {
            auto color = pagLayer->strokeColor();
            return MakeColorInt(color.red, color.green, color.blue);
        }
        return 0;
    }

    JNIEXPORT void JNICALL Java_org_libpag_PAGTextLayer_nativeSetStrokeColor(JNIEnv* env, jclass, jlong handle, jint color) {
        auto pagLayer = obj_cast(handle);
        if (pagLayer) pagLayer->setStrokeColor(ToColor(color));
    }

    JNIEXPORT jstring JNICALL Java_org_libpag_PAGTextLayer_nativeText(JNIEnv* env, jclass, jlong handle) {
        auto pagLayer = obj_cast(handle);
        return s2j(env, pagLayer ? pagLayer->text() : std::string{ });
    }

    JNIEXPORT void JNICALL Java_org_libpag_PAGTextLayer_nativeSetText(JNIEnv* env, jclass, jlong handle, jstring text) {
        auto pagLayer = obj_cast(handle);
        if (pagLayer) pagLayer->setText(j2s(env, text));
    }

    JNIEXPORT void JNICALL Java_org_libpag_PAGTextLayer_nativeReset(JNIEnv* env, jclass, jlong handle) {
        auto pagLayer = obj_cast(handle);
        if (pagLayer) pagLayer->reset();
    }
}