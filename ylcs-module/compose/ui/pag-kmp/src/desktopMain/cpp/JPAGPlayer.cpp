#include "JPAGPlayer.h"
#include "JPAGSurface.h"
#include "JPAGComposition.h"
#include "JPAGUtils.h"

using namespace pag;

static inline std::shared_ptr<PAGPlayer> obj_cast(jlong handle) {
    auto jPagPlayer = reinterpret_cast<JPAGPlayer*>(handle);
    return jPagPlayer ? jPagPlayer->get() : nullptr;
}

extern "C" {
    JNIEXPORT jlong JNICALL Java_org_libpag_PAGPlayer_nativeCreate(JNIEnv* env, jclass) {
        return reinterpret_cast<jlong>(new JPAGPlayer(std::make_shared<PAGPlayer>()));
    }

    JNIEXPORT void JNICALL Java_org_libpag_PAGPlayer_nativeClear(JNIEnv* env, jclass, jlong handle) {
        auto jPagPlayer = reinterpret_cast<JPAGPlayer*>(handle);
        if (jPagPlayer) jPagPlayer->clear();
    }

    JNIEXPORT void JNICALL Java_org_libpag_PAGPlayer_nativeRelease(JNIEnv* env, jclass, jlong handle) {
        auto jPagPlayer = reinterpret_cast<JPAGPlayer*>(handle);
        if (jPagPlayer) delete jPagPlayer;
    }

    JNIEXPORT void JNICALL Java_org_libpag_PAGPlayer_nativeSetSurface(JNIEnv* env, jclass, jlong handle, jlong surfaceHandle) {
        auto pagPlayer = obj_cast(handle);
        if (pagPlayer) {
            auto surface = reinterpret_cast<JPAGSurface*>(surfaceHandle);
            if (surface) pagPlayer->setSurface(surface->get());
            else pagPlayer->setSurface(nullptr);
        }
    }

    JNIEXPORT jlongArray JNICALL Java_org_libpag_PAGPlayer_nativeGetComposition(JNIEnv* env, jclass, jlong handle) {
        auto pagPlayer = obj_cast(handle);
        auto result = env->NewLongArray(2);
        if (pagPlayer) env->SetLongArrayRegion(result, 0, 2, JPAGLayerInstance(pagPlayer->getComposition()));
        return result;
    }

    JNIEXPORT void JNICALL Java_org_libpag_PAGPlayer_nativeSetComposition(JNIEnv* env, jclass, jlong handle, jlong compositionHandle, jlong type) {
        auto pagPlayer = obj_cast(handle);
        if (pagPlayer) {
            auto composition = PAGLayerInstance(LayerInfo(compositionHandle, type));
            if (composition) pagPlayer->setComposition(std::static_pointer_cast<PAGComposition>(composition));
        }
    }

    JNIEXPORT jboolean JNICALL Java_org_libpag_PAGPlayer_nativeVideoEnabled(JNIEnv* env, jclass, jlong handle) {
        auto pagPlayer = obj_cast(handle);
        return pagPlayer ? static_cast<jboolean>(pagPlayer->videoEnabled()) : JNI_FALSE;
    }

    JNIEXPORT void JNICALL Java_org_libpag_PAGPlayer_nativeSetVideoEnabled(JNIEnv* env, jclass, jlong handle, jboolean value) {
        auto pagPlayer = obj_cast(handle);
        if (pagPlayer) pagPlayer->setVideoEnabled(value);
    }

    JNIEXPORT jboolean JNICALL Java_org_libpag_PAGPlayer_nativeCacheEnabled(JNIEnv* env, jclass, jlong handle) {
        auto pagPlayer = obj_cast(handle);
        return pagPlayer ? static_cast<jboolean>(pagPlayer->cacheEnabled()) : JNI_FALSE;
    }

    JNIEXPORT void JNICALL Java_org_libpag_PAGPlayer_nativeSetCacheEnabled(JNIEnv* env, jclass, jlong handle, jboolean value) {
        auto pagPlayer = obj_cast(handle);
        if (pagPlayer) pagPlayer->setCacheEnabled(value);
    }

    JNIEXPORT jboolean JNICALL Java_org_libpag_PAGPlayer_nativeUseDiskCache(JNIEnv* env, jclass, jlong handle) {
        auto pagPlayer = obj_cast(handle);
        return pagPlayer ? static_cast<jboolean>(pagPlayer->useDiskCache()) : JNI_FALSE;
    }

    JNIEXPORT void JNICALL Java_org_libpag_PAGPlayer_nativeSetUseDiskCache(JNIEnv* env, jclass, jlong handle, jboolean value) {
        auto pagPlayer = obj_cast(handle);
        if (pagPlayer) pagPlayer->setUseDiskCache(value);
    }

    JNIEXPORT jfloat JNICALL Java_org_libpag_PAGPlayer_nativeCacheScale(JNIEnv* env, jclass, jlong handle) {
        auto pagPlayer = obj_cast(handle);
        return pagPlayer ? pagPlayer->cacheScale() : 0.0F;
    }

    JNIEXPORT void JNICALL Java_org_libpag_PAGPlayer_nativeSetCacheScale(JNIEnv* env, jclass, jlong handle, jfloat value) {
        auto pagPlayer = obj_cast(handle);
        if (pagPlayer) pagPlayer->setCacheScale(value);
    }

    JNIEXPORT jfloat JNICALL Java_org_libpag_PAGPlayer_nativeMaxFrameRate(JNIEnv* env, jclass, jlong handle) {
        auto pagPlayer = obj_cast(handle);
        return pagPlayer ? pagPlayer->maxFrameRate() : 0.0F;
    }

    JNIEXPORT void JNICALL Java_org_libpag_PAGPlayer_nativeSetMaxFrameRate(JNIEnv* env, jclass, jlong handle, jfloat value) {
        auto pagPlayer = obj_cast(handle);
        if (pagPlayer) pagPlayer->setMaxFrameRate(value);
    }

    JNIEXPORT jint JNICALL Java_org_libpag_PAGPlayer_nativeScaleMode(JNIEnv* env, jclass, jlong handle) {
        auto pagPlayer = obj_cast(handle);
        return pagPlayer ? static_cast<jint>(pagPlayer->scaleMode()) : 0;
    }

    JNIEXPORT void JNICALL Java_org_libpag_PAGPlayer_nativeSetScaleMode(JNIEnv* env, jclass, jlong handle, jint value) {
        auto pagPlayer = obj_cast(handle);
        if (pagPlayer) pagPlayer->setScaleMode(static_cast<PAGScaleMode>(value));
    }

    JNIEXPORT jfloatArray JNICALL Java_org_libpag_PAGPlayer_nativeGetMatrix(JNIEnv* env, jclass, jlong handle) {
        auto pagPlayer = obj_cast(handle);
        float values[9];
        jfloatArray result = env->NewFloatArray(9);
        if (pagPlayer) {
            auto matrix = pagPlayer->matrix();
            matrix.get9(values);
        }
        else {
            Matrix matrix{ };
            matrix.setIdentity();
            matrix.get9(values);
        }
        env->SetFloatArrayRegion(result, 0, 9, values);
        return result;
    }

    JNIEXPORT void JNICALL Java_org_libpag_PAGPlayer_nativeSetMatrix(JNIEnv* env, jclass, jlong handle, jfloatArray arr) {
        auto pagPlayer = obj_cast(handle);
        if (pagPlayer) {
            float values[9];
            env->GetFloatArrayRegion(arr, 0, 9, values);
            Matrix matrix{ };
            matrix.set9(values);
            pagPlayer->setMatrix(matrix);
        }
    }

    JNIEXPORT jlong JNICALL Java_org_libpag_PAGPlayer_nativeDuration(JNIEnv* env, jclass, jlong handle) {
        auto pagPlayer = obj_cast(handle);
        return pagPlayer ? pagPlayer->duration() : 0;
    }

    JNIEXPORT jdouble JNICALL Java_org_libpag_PAGPlayer_nativeGetProgress(JNIEnv* env, jclass, jlong handle) {
        auto pagPlayer = obj_cast(handle);
        return pagPlayer ? pagPlayer->getProgress() : 0.0;
    }

    JNIEXPORT void JNICALL Java_org_libpag_PAGPlayer_nativeSetProgress(JNIEnv* env, jclass, jlong handle, jdouble value) {
        auto pagPlayer = obj_cast(handle);
        if (pagPlayer) pagPlayer->setProgress(value);
    }

    JNIEXPORT jlong JNICALL Java_org_libpag_PAGPlayer_nativeCurrentFrame(JNIEnv* env, jclass, jlong handle) {
        auto pagPlayer = obj_cast(handle);
        return pagPlayer ? pagPlayer->currentFrame() : 0LL;
    }

    JNIEXPORT void JNICALL Java_org_libpag_PAGPlayer_nativePrepare(JNIEnv* env, jclass, jlong handle) {
        auto pagPlayer = obj_cast(handle);
        if (pagPlayer) pagPlayer->prepare();
    }

    JNIEXPORT jboolean JNICALL Java_org_libpag_PAGPlayer_nativeFlush(JNIEnv* env, jclass, jlong handle) {
        auto pagPlayer = obj_cast(handle);
        return pagPlayer ? static_cast<jboolean>(pagPlayer->flush()) : JNI_FALSE;
    }

    JNIEXPORT jboolean JNICALL Java_org_libpag_PAGPlayer_nativeFlushAndFenceSync(JNIEnv* env, jclass, jlong handle, jlongArray syncArray) {
        auto pagPlayer = obj_cast(handle);
        if (pagPlayer) {
            auto size = env->GetArrayLength(syncArray);
            if (size == 0) return static_cast<jboolean>(pagPlayer->flush());
            auto array = env->GetLongArrayElements(syncArray, nullptr);
            if (array == nullptr) return static_cast<jboolean>(pagPlayer->flush());
            BackendSemaphore semaphore;
            auto result = pagPlayer->flushAndSignalSemaphore(&semaphore);
            array[0] = semaphore.isInitialized() ? reinterpret_cast<jlong>(semaphore.glSync()) : 0LL;
            env->ReleaseLongArrayElements(syncArray, array, 0);
            return static_cast<jboolean>(result);
        }
        return JNI_FALSE;
    }

    JNIEXPORT jboolean JNICALL Java_org_libpag_PAGPlayer_nativeWaitSync(JNIEnv* env, jclass, jlong handle, jlong sync) {
        auto pagPlayer = obj_cast(handle);
        if (pagPlayer) {
            BackendSemaphore semaphore;
            semaphore.initGL(reinterpret_cast<void*>(sync));
            return pagPlayer->wait(semaphore);
        }
        return JNI_FALSE;
    }

    JNIEXPORT jboolean JNICALL Java_org_libpag_PAGPlayer_nativeHitTestPoint(JNIEnv* env, jclass, jlong handle, jlong layerHandle, jlong type, jfloat x, jfloat y, jboolean pixelHitTest) {
        auto pagPlayer = obj_cast(handle);
        if (pagPlayer) {
            auto layer = PAGLayerInstance(LayerInfo(layerHandle, type));
            return static_cast<jboolean>(pagPlayer->hitTestPoint(layer, x, y, pixelHitTest));
        }
        return JNI_FALSE;
    }
}