#include "JPAGFile.h"
#include "JPAGImage.h"

using namespace pag;

static inline std::shared_ptr<PAGFile> obj_cast(jlong handle) {
    auto jPagLayer = reinterpret_cast<JPAGFile*>(handle);
    return jPagLayer ? jPagLayer->get() : nullptr;
}

extern "C" {
    JNIEXPORT jshort JNICALL Java_org_libpag_PAGFile_nativeMaxSupportedTagLevel(JNIEnv* env, jclass) {
        return static_cast<jshort>(PAGFile::MaxSupportedTagLevel());
    }

    JNIEXPORT jlong JNICALL Java_org_libpag_PAGFile_nativeLoadFromPath(JNIEnv* env, jclass, jstring path) {
        auto pathStr = j2s(env, path);
        if (pathStr.empty()) return 0LL;
        auto jPagLayer = PAGFile::Load(pathStr);
        return jPagLayer ? reinterpret_cast<jlong>(new JPAGFile(jPagLayer)) : 0LL;
    }

    JNIEXPORT jlong JNICALL Java_org_libpag_PAGFile_nativeLoadFromBytes(JNIEnv* env, jclass, jbyteArray bytes) {
        auto length = env->GetArrayLength(bytes);
        auto data = env->GetPrimitiveArrayCritical(bytes, nullptr);
        if (!data) return 0LL;
        auto jPagLayer = PAGFile::Load(data, static_cast<size_t>(length), "");
        env->ReleasePrimitiveArrayCritical(bytes, data, JNI_ABORT);
        return jPagLayer ? reinterpret_cast<jlong>(new JPAGFile(jPagLayer)) : 0LL;
    }

    JNIEXPORT void JNICALL Java_org_libpag_PAGFile_nativeRelease(JNIEnv* env, jclass, jlong handle) {
        auto jPagLayer = reinterpret_cast<JPAGFile*>(handle);
        if (jPagLayer) delete jPagLayer;
    }

    JNIEXPORT jint JNICALL Java_org_libpag_PAGFile_nativeTagLevel(JNIEnv* env, jclass, jlong handle) {
        auto pagLayer = obj_cast(handle);
        return pagLayer ? pagLayer->tagLevel() : 0;
    }

    JNIEXPORT jint JNICALL Java_org_libpag_PAGFile_nativeNumTexts(JNIEnv* env, jclass, jlong handle) {
        auto pagLayer = obj_cast(handle);
        return pagLayer ? pagLayer->numTexts() : 0;
    }

    JNIEXPORT jint JNICALL Java_org_libpag_PAGFile_nativeNumImages(JNIEnv* env, jclass, jlong handle) {
        auto pagLayer = obj_cast(handle);
        return pagLayer ? pagLayer->numImages() : 0;
    }

    JNIEXPORT jint JNICALL Java_org_libpag_PAGFile_nativeNumVideos(JNIEnv* env, jclass, jlong handle) {
        auto pagLayer = obj_cast(handle);
        return pagLayer ? pagLayer->numVideos() : 0;
    }

    JNIEXPORT jstring JNICALL Java_org_libpag_PAGFile_nativePath(JNIEnv* env, jclass, jlong handle) {
        auto pagLayer = obj_cast(handle);
        return s2j(env, pagLayer ? pagLayer->path() : std::string{ });
    }

    JNIEXPORT void JNICALL Java_org_libpag_PAGFile_nativeReplaceImage(JNIEnv* env, jclass, jlong handle, jint index, jlong imageHandle) {
        auto pagLayer = obj_cast(handle);
        if (pagLayer) {
            auto image = reinterpret_cast<JPAGImage*>(imageHandle);
            if (image) pagLayer->replaceImage(index, image->get());
            else pagLayer->replaceImage(index, nullptr);
        }
    }

    JNIEXPORT void JNICALL Java_org_libpag_PAGFile_nativeReplaceImageByName(JNIEnv* env, jclass, jlong handle, jstring layer_name, jlong imageHandle) {
        auto pagLayer = obj_cast(handle);
        if (pagLayer) {
            auto name = j2s(env, layer_name);
            auto image = reinterpret_cast<JPAGImage*>(imageHandle);
            if (image) pagLayer->replaceImageByName(name, image->get());
            else pagLayer->replaceImageByName(name, nullptr);
        }
    }

    JNIEXPORT jintArray JNICALL Java_org_libpag_PAGFile_nativeGetEditableIndices(JNIEnv* env, jclass, jlong handle, jint layer_type) {
        auto pagLayer = obj_cast(handle);
        if (pagLayer) {
            auto indices = pagLayer->getEditableIndices(static_cast<LayerType>(layer_type));
            auto size = static_cast<jint>(indices.size());
            auto result = env->NewIntArray(size);
            env->SetIntArrayRegion(result, 0, size, indices.data());
            return result;
        }
        return env->NewIntArray(0);
    }

    JNIEXPORT jint JNICALL Java_org_libpag_PAGFile_nativeTimeStretchMode(JNIEnv* env, jclass, jlong handle) {
        auto pagLayer = obj_cast(handle);
        return pagLayer ? static_cast<jint>(pagLayer->timeStretchMode()) : 0;
    }

    JNIEXPORT void JNICALL Java_org_libpag_PAGFile_nativeSetTimeStretchMode(JNIEnv* env, jclass, jlong handle, jint mode) {
        auto pagLayer = obj_cast(handle);
        if (pagLayer) pagLayer->setTimeStretchMode(static_cast<PAGTimeStretchMode>(mode));
    }

    JNIEXPORT void JNICALL Java_org_libpag_PAGFile_nativeSetDuration(JNIEnv* env, jclass, jlong handle, jlong duration) {
        auto pagLayer = obj_cast(handle);
        if (pagLayer) pagLayer->setDuration(duration);
    }

    JNIEXPORT jlong JNICALL Java_org_libpag_PAGFile_nativeCopyOriginal(JNIEnv* env, jclass, jlong handle) {
        auto pagLayer = obj_cast(handle);
        return pagLayer ? reinterpret_cast<jlong>(new JPAGFile(pagLayer->copyOriginal())) : 0LL;
    }
}