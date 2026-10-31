#pragma once
#include <jni.h>
#include <rachel_audio.h>
#include <cstring>
#include <limits>

class JvmAudioEnvironment {
    JavaVM *vm;
    bool attached = false;
public:
    JNIEnv *env = nullptr;

    explicit JvmAudioEnvironment(JavaVM *machine) : vm(machine) {
        const auto result = vm->GetEnv(reinterpret_cast<void **>(&env), JNI_VERSION_1_8);
        if (result == JNI_EDETACHED) {
            attached = vm->AttachCurrentThreadAsDaemon(reinterpret_cast<void **>(&env), nullptr) == JNI_OK;
            if (!attached) env = nullptr;
        }
        else if (result != JNI_OK) env = nullptr;
    }

    ~JvmAudioEnvironment() {
        if (attached) vm->DetachCurrentThread();
    }
};

class JvmAudioCallbacks {
    JavaVM *vm;
    jobject consumer;
    jmethodID accept;
public:
    JvmAudioCallbacks(JavaVM *machine, jobject listener, jmethodID method) : vm(machine), consumer(listener), accept(method) { }

    ~JvmAudioCallbacks() {
        JvmAudioEnvironment environment(vm);
        if (environment.env) environment.env->DeleteGlobalRef(consumer);
    }

    static void notify(void *context, const RachelAudioEvent *event) {
        auto *owner = static_cast<JvmAudioCallbacks *>(context);
        JvmAudioEnvironment environment(owner->vm);
        JNIEnv *env = environment.env;
        if (!env || env->PushLocalFrame(4) != JNI_OK) return;
        jlongArray arguments = env->NewLongArray(3);
        const jlong values[] = {event->revision, event->kind, event->value};
        if (arguments) env->SetLongArrayRegion(arguments, 0, 3, values);
        jbyteArray message = nullptr;
        if (event->message && !env->ExceptionCheck()) {
            const auto size = std::strlen(event->message);
            if (size <= static_cast<size_t>(std::numeric_limits<jsize>::max())) {
                message = env->NewByteArray(static_cast<jsize>(size));
                if (message) env->SetByteArrayRegion(message, 0, static_cast<jsize>(size), reinterpret_cast<const jbyte *>(event->message));
            }
        }
        if (!env->ExceptionCheck()) env->CallVoidMethod(owner->consumer, owner->accept, arguments, message);
        if (env->ExceptionCheck()) {
            env->ExceptionDescribe();
            env->ExceptionClear();
        }
        env->PopLocalFrame(nullptr);
    }

    static void destroy(void *context) { delete static_cast<JvmAudioCallbacks *>(context); }
};

