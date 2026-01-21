#include <native_jni.h>
#include <pag.h>
#include <rendering/caches/DiskCache.h>

using namespace pag;

extern "C" {
    JNIEXPORT jlong JNICALL Java_org_libpag_PAGDiskCache_nativeMaxDiskSize(JNIEnv* env, jclass) {
        return static_cast<jlong>(PAGDiskCache::MaxDiskSize());
    }

    JNIEXPORT void JNICALL Java_org_libpag_PAGDiskCache_nativeSetMaxDiskSize(JNIEnv* env, jclass, jlong size) {
        PAGDiskCache::SetMaxDiskSize(size);
    }

    JNIEXPORT void JNICALL Java_org_libpag_PAGDiskCache_nativeRemoveAll(JNIEnv* env, jclass) {
        PAGDiskCache::RemoveAll();
    }

    JNIEXPORT jbyteArray JNICALL Java_org_libpag_PAGDiskCache_nativeReadFile(JNIEnv* env, jclass, jstring key) {
        auto data = DiskCache::ReadFile(j2s(env, key));
        if (data == nullptr) return nullptr;
        auto length = static_cast<jsize>(data->size());
        auto bytes = env->NewByteArray(length);
        env->SetByteArrayRegion(bytes, 0, length, reinterpret_cast<const jbyte*>(data->data()));
        return bytes;
    }

    JNIEXPORT jboolean JNICALL Java_org_libpag_PAGDiskCache_nativeWriteFile(JNIEnv* env, jclass, jstring jkey, jbyteArray bytes) {
        auto key = j2s(env, jkey);
        if (bytes == nullptr || key.empty()) return JNI_FALSE;
        auto data = env->GetByteArrayElements(bytes, nullptr);
        auto length = env->GetArrayLength(bytes);
        auto byteData = tgfx::Data::MakeWithoutCopy(data, length);
        auto result = DiskCache::WriteFile(key, byteData);
        env->ReleaseByteArrayElements(bytes, data, JNI_ABORT);
        return result;
    }
}