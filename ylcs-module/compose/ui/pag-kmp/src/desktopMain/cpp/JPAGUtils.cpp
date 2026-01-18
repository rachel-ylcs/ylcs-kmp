#include "JPAGUtils.h"
#include "JPAGShapeLayer.h"
#include "JPAGSolidLayer.h"
#include "JPAGFile.h"
#include "JPAGComposition.h"
#include "JPAGTextLayer.h"
#include "JPAGImageLayer.h"
#include "JPAGLayer.h"

using namespace pag;

std::pair<jlong, int> JPAGLayerInstance(std::shared_ptr<PAGLayer> pagLayer) {
    if (pagLayer == nullptr) return std::make_pair(0LL, static_cast<int>(LayerType::Null));
    switch (auto type = pagLayer->layerType()) {
        case LayerType::Shape:
            return std::make_pair(reinterpret_cast<jlong>(new JPAGShapeLayer(std::static_pointer_cast<PAGShapeLayer>(pagLayer))), static_cast<int>(type));
        case LayerType::Solid:
            return std::make_pair(reinterpret_cast<jlong>(new JPAGSolidLayer(std::static_pointer_cast<PAGSolidLayer>(pagLayer))), static_cast<int>(type));
        case LayerType::PreCompose: {
            if (std::static_pointer_cast<PAGComposition>(pagLayer)->isPAGFile()) {
                return std::make_pair(reinterpret_cast<jlong>(new JPAGFile(std::static_pointer_cast<PAGFile>(pagLayer))), 114514);
            }
            else {
                return std::make_pair(reinterpret_cast<jlong>(new JPAGComposition(std::static_pointer_cast<PAGComposition>(pagLayer))), static_cast<int>(type));
            }
        }
        case LayerType::Text:
            return std::make_pair(reinterpret_cast<jlong>(new JPAGTextLayer(std::static_pointer_cast<PAGTextLayer>(pagLayer))), static_cast<int>(type));
        case LayerType::Image:
            return std::make_pair(reinterpret_cast<jlong>(new JPAGImageLayer(std::static_pointer_cast<PAGImageLayer>(pagLayer))), static_cast<int>(type));
        default:
            return std::make_pair(reinterpret_cast<jlong>(new JPAGLayer(pagLayer)), static_cast<int>(LayerType::Unknown));
    }
}

std::shared_ptr<pag::PAGLayer> PAGLayerInstance(jlong layerHandle, jint type) {
    if (layerHandle == 0LL) return nullptr;
    switch (static_cast<LayerType>(type)) {
        case LayerType::Shape: return reinterpret_cast<JPAGShapeLayer*>(layerHandle)->get();
        case LayerType::Solid: return reinterpret_cast<JPAGSolidLayer*>(layerHandle)->get();
        case LayerType::PreCompose: return reinterpret_cast<JPAGComposition*>(layerHandle)->get();
        case static_cast<LayerType>(114514): return reinterpret_cast<JPAGFile*>(layerHandle)->get();
        case LayerType::Text: return reinterpret_cast<JPAGTextLayer*>(layerHandle)->get();
        case LayerType::Image: return reinterpret_cast<JPAGImageLayer*>(layerHandle)->get();
        default: return reinterpret_cast<JPAGLayer*>(layerHandle)->get();
    }
}