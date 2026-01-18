#include <native_jni.h>
#include <pag.h>
#include <vector>

using namespace pag;

extern "C" {
    JNIEXPORT void JNICALL Java_org_libpag_PAGFont_nativeUnregisterFont(JNIEnv* env, jclass, jstring font_family, jstring font_style) {
        PAGFont::UnregisterFont({j2s(env, font_family), j2s(env, font_style)});
    }

    JNIEXPORT void JNICALL Java_org_libpag_PAGFont_nativeSetFallbackFontPaths(JNIEnv* env, jclass, jobjectArray fontNameList, jintArray ttcIndices) {
        std::vector<std::string> fallbackList;
        std::vector<int> ttcList;
        auto length = env->GetArrayLength(fontNameList);
        auto ttcLength = env->GetArrayLength(ttcIndices);
        length = std::min(length, ttcLength);
        auto ttcData = env->GetIntArrayElements(ttcIndices, nullptr);
        for (int index = 0; index < length; index++) {
            auto fontNameObject = (jstring)env->GetObjectArrayElement(fontNameList, index);
            auto fontFamily = j2s(env, fontNameObject);
            fallbackList.push_back(fontFamily);
            ttcList.push_back(ttcData[index]);
        }
        env->ReleaseIntArrayElements(ttcIndices, ttcData, 0);
        PAGFont::SetFallbackFontPaths(fallbackList, ttcList);
    }

    JNIEXPORT jobjectArray JNICALL Java_org_libpag_PAGFont_nativeRegisterFont(JNIEnv* env, jclass, jbyteArray bytes, jint ttcIndex, jstring font_family, jstring font_style) {
        auto length = env->GetArrayLength(bytes);
        auto data = env->GetPrimitiveArrayCritical(bytes, nullptr);
        if (data) {
            auto font = PAGFont::RegisterFont(data, static_cast<size_t>(length), ttcIndex, j2s(env, font_family), j2s(env, font_style));
            env->ReleasePrimitiveArrayCritical(bytes, data, JNI_ABORT);
            if (font.fontFamily.empty()) return nullptr;
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
}