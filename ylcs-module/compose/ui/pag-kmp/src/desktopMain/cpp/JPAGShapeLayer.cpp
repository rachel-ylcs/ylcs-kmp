#include "JPAGShapeLayer.h"

using namespace pag;

extern "C" {
    JNIEXPORT void JNICALL Java_org_libpag_PAGShapeLayer_nativeRelease(JNIEnv* env, jclass, jlong handle) {
        auto jPagLayer = reinterpret_cast<JPAGShapeLayer*>(handle);
        if (jPagLayer) delete jPagLayer;
    }
}