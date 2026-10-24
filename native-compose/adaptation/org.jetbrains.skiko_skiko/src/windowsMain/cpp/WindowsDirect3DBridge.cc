#include "types.h"

#if defined(SK_BUILD_FOR_WIN) && defined(SK_DIRECT3D)

#include <Windows.h>
#include <d3d12.h>
#include <dcomp.h>
#include <dxgi1_6.h>

namespace {

constexpr UINT kMaximumBufferCount = 3;
constexpr DXGI_FORMAT kSwapChainFormat = DXGI_FORMAT_R8G8B8A8_UNORM;

template <typename T> void releaseCom(T *&value) {
  if (value != nullptr) {
    value->Release();
    value = nullptr;
  }
}

struct WindowsDirect3DDevice {
  HWND window = nullptr;
  IDXGIAdapter1 *adapter = nullptr;
  ID3D12Device *device = nullptr;
  ID3D12CommandQueue *queue = nullptr;
  IDXGISwapChain3 *swapChain = nullptr;
  ID3D12Fence *fence = nullptr;
  HANDLE fenceEvent = nullptr;
  HANDLE frameLatencyWaitableObject = nullptr;
  ID3D12Resource *buffers[kMaximumBufferCount] = {};
  UINT64 bufferFenceValues[kMaximumBufferCount] = {};
  UINT64 nextFenceValue = 1;
  UINT bufferCount = 2;
  UINT currentBufferIndex = 0;
  UINT swapChainFlags = 0;
  HRESULT lastError = S_OK;
  bool transparencyRequested = false;
  bool transparencySupported = false;
  IDCompositionDevice *compositionDevice = nullptr;
  IDCompositionTarget *compositionTarget = nullptr;
  IDCompositionVisual *compositionVisual = nullptr;
  char adapterName[512] = {};
  UINT64 adapterMemory = 0;
};

HRESULT gLastCreateError = S_OK;

using DCompositionCreateDeviceFunction = HRESULT(WINAPI *)(IDXGIDevice *,
                                                           REFIID, void **);

DCompositionCreateDeviceFunction loadDCompositionCreateDevice() {
  static DCompositionCreateDeviceFunction function = [] {
    HMODULE module = LoadLibraryW(L"dcomp.dll");
    if (module == nullptr)
      return static_cast<DCompositionCreateDeviceFunction>(nullptr);
    return reinterpret_cast<DCompositionCreateDeviceFunction>(
        GetProcAddress(module, "DCompositionCreateDevice"));
  }();
  return function;
}

bool isDeviceLostResult(HRESULT result) {
  return result == DXGI_ERROR_DEVICE_HUNG ||
         result == DXGI_ERROR_DEVICE_REMOVED ||
         result == DXGI_ERROR_DEVICE_RESET ||
         result == DXGI_ERROR_DRIVER_INTERNAL_ERROR ||
         result == DXGI_ERROR_ACCESS_LOST;
}

HRESULT remember(WindowsDirect3DDevice *state, HRESULT result) {
  if (state != nullptr && FAILED(result))
    state->lastError = result;
  return result;
}

void releaseBuffers(WindowsDirect3DDevice *state) {
  for (UINT index = 0; index < kMaximumBufferCount; ++index) {
    releaseCom(state->buffers[index]);
    state->bufferFenceValues[index] = 0;
  }
}

HRESULT waitForFence(WindowsDirect3DDevice *state, UINT64 value) {
  if (value == 0)
    return S_OK;
  const UINT64 completedValue = state->fence->GetCompletedValue();
  if (completedValue == UINT64_MAX) {
    return remember(state, state->device->GetDeviceRemovedReason());
  }
  if (completedValue >= value)
    return S_OK;
  HRESULT result = state->fence->SetEventOnCompletion(value, state->fenceEvent);
  if (FAILED(result))
    return remember(state, result);
  DWORD waitResult = WaitForSingleObjectEx(state->fenceEvent, 5000, FALSE);
  if (waitResult == WAIT_OBJECT_0)
    return S_OK;
  if (waitResult == WAIT_TIMEOUT)
    return remember(state, DXGI_ERROR_DEVICE_HUNG);
  return remember(state, HRESULT_FROM_WIN32(GetLastError()));
}

HRESULT waitForGpuIdle(WindowsDirect3DDevice *state) {
  if (state->queue == nullptr || state->fence == nullptr)
    return S_OK;
  const UINT64 value = state->nextFenceValue++;
  HRESULT result = state->queue->Signal(state->fence, value);
  if (FAILED(result))
    return remember(state, result);
  return waitForFence(state, value);
}

HRESULT createD3D12Device(IDXGIAdapter1 *adapter, ID3D12Device **result) {
  constexpr D3D_FEATURE_LEVEL featureLevels[] = {
      D3D_FEATURE_LEVEL_12_1,
      D3D_FEATURE_LEVEL_12_0,
      D3D_FEATURE_LEVEL_11_1,
      D3D_FEATURE_LEVEL_11_0,
  };
  HRESULT lastResult = E_FAIL;
  for (D3D_FEATURE_LEVEL level : featureLevels) {
    lastResult = D3D12CreateDevice(adapter, level, IID_PPV_ARGS(result));
    if (SUCCEEDED(lastResult))
      return lastResult;
  }
  return lastResult;
}

HRESULT chooseAdapterAndDevice(IDXGIFactory4 *factory,
                               DXGI_GPU_PREFERENCE preference,
                               IDXGIAdapter1 **selectedAdapter,
                               ID3D12Device **selectedDevice) {
  IDXGIFactory6 *factory6 = nullptr;
  factory->QueryInterface(IID_PPV_ARGS(&factory6));
  HRESULT lastResult = DXGI_ERROR_NOT_FOUND;
  for (UINT index = 0;; ++index) {
    IDXGIAdapter1 *adapter = nullptr;
    HRESULT result = factory6 != nullptr
                         ? factory6->EnumAdapterByGpuPreference(
                               index, preference, IID_PPV_ARGS(&adapter))
                         : factory->EnumAdapters1(index, &adapter);
    if (result == DXGI_ERROR_NOT_FOUND)
      break;
    if (FAILED(result)) {
      lastResult = result;
      break;
    }

    DXGI_ADAPTER_DESC1 description = {};
    result = adapter->GetDesc1(&description);
    if (SUCCEEDED(result) &&
        (description.Flags & DXGI_ADAPTER_FLAG_SOFTWARE) == 0) {
      ID3D12Device *device = nullptr;
      result = createD3D12Device(adapter, &device);
      if (SUCCEEDED(result)) {
        *selectedAdapter = adapter;
        *selectedDevice = device;
        releaseCom(factory6);
        return S_OK;
      }
    }
    lastResult = result;
    releaseCom(adapter);
  }
  releaseCom(factory6);
  return lastResult;
}

void storeAdapterDescription(WindowsDirect3DDevice *state) {
  DXGI_ADAPTER_DESC1 description = {};
  if (FAILED(state->adapter->GetDesc1(&description)))
    return;
  state->adapterMemory = description.DedicatedVideoMemory;
  int result = WideCharToMultiByte(
      CP_UTF8, 0, description.Description, -1, state->adapterName,
      static_cast<int>(sizeof(state->adapterName)), nullptr, nullptr);
  if (result == 0)
    state->adapterName[0] = '\0';
}

void releaseComposition(WindowsDirect3DDevice *state) {
  if (state->compositionTarget != nullptr) {
    state->compositionTarget->SetRoot(nullptr);
    if (state->compositionDevice != nullptr)
      state->compositionDevice->Commit();
  }
  releaseCom(state->compositionVisual);
  releaseCom(state->compositionTarget);
  releaseCom(state->compositionDevice);
  state->transparencySupported = false;
}

HRESULT attachDirectComposition(WindowsDirect3DDevice *state,
                                IDXGISwapChain1 *swapChain) {
  DCompositionCreateDeviceFunction createDevice =
      loadDCompositionCreateDevice();
  if (createDevice == nullptr)
    return HRESULT_FROM_WIN32(ERROR_MOD_NOT_FOUND);

  HRESULT result =
      createDevice(nullptr, __uuidof(IDCompositionDevice),
                   reinterpret_cast<void **>(&state->compositionDevice));
  if (FAILED(result))
    return result;
  result = state->compositionDevice->CreateTargetForHwnd(
      state->window, TRUE, &state->compositionTarget);
  if (FAILED(result))
    return result;
  result = state->compositionDevice->CreateVisual(&state->compositionVisual);
  if (FAILED(result))
    return result;
  result = state->compositionVisual->SetContent(swapChain);
  if (FAILED(result))
    return result;
  result = state->compositionTarget->SetRoot(state->compositionVisual);
  if (FAILED(result))
    return result;
  result = state->compositionDevice->Commit();
  if (SUCCEEDED(result)) {
    // DirectComposition is also used for opaque capacity-backed swap chains. Only advertise a
    // transparent window buffer when the caller explicitly requested one.
    state->transparencySupported = state->transparencyRequested;
  }
  return result;
}

HRESULT createSwapChainAttempt(WindowsDirect3DDevice *state, UINT width,
                               UINT height, bool composition, UINT flags,
                               IDXGISwapChain1 **result) {
  IDXGIFactory4 *factory = nullptr;
  HRESULT status = CreateDXGIFactory2(0, IID_PPV_ARGS(&factory));
  if (FAILED(status))
    return status;

  DXGI_SWAP_CHAIN_DESC1 description = {};
  description.Width = width;
  description.Height = height;
  description.Format = kSwapChainFormat;
  description.Stereo = FALSE;
  description.SampleDesc.Count = 1;
  description.BufferUsage = DXGI_USAGE_RENDER_TARGET_OUTPUT;
  description.BufferCount = state->bufferCount;
  description.Scaling = DXGI_SCALING_STRETCH;
  description.SwapEffect = DXGI_SWAP_EFFECT_FLIP_DISCARD;
  description.AlphaMode =
      composition ? DXGI_ALPHA_MODE_PREMULTIPLIED : DXGI_ALPHA_MODE_UNSPECIFIED;
  description.Flags = flags;

  status = composition
               ? factory->CreateSwapChainForComposition(
                     state->queue, &description, nullptr, result)
               : factory->CreateSwapChainForHwnd(state->queue, state->window,
                                                 &description, nullptr, nullptr,
                                                 result);
  if (SUCCEEDED(status) && !composition) {
    factory->MakeWindowAssociation(state->window, DXGI_MWA_NO_ALT_ENTER);
  }
  releaseCom(factory);
  return status;
}

HRESULT createSwapChain(WindowsDirect3DDevice *state, UINT width, UINT height) {
  constexpr UINT preferredFlags =
      DXGI_SWAP_CHAIN_FLAG_FRAME_LATENCY_WAITABLE_OBJECT;
  IDXGISwapChain1 *swapChain1 = nullptr;
  HRESULT result = E_FAIL;

  // A composition visual preserves the source rectangle's 1:1 coordinate space while the
  // capacity-backed swap chain remains larger. CreateSwapChainForHwnd instead stretches the full
  // backing buffer to the client area, shrinking current-size content whenever spare capacity is
  // present.
  result = createSwapChainAttempt(state, width, height, true, preferredFlags,
                                  &swapChain1);
  if (FAILED(result)) {
    result = createSwapChainAttempt(state, width, height, true, 0, &swapChain1);
  }
  if (SUCCEEDED(result)) {
    result = attachDirectComposition(state, swapChain1);
    if (FAILED(result)) {
      releaseComposition(state);
      releaseCom(swapChain1);
    }
  }

  if (swapChain1 == nullptr) {
    result = createSwapChainAttempt(state, width, height, false, preferredFlags,
                                    &swapChain1);
    if (FAILED(result)) {
      result =
          createSwapChainAttempt(state, width, height, false, 0, &swapChain1);
    }
  }
  if (FAILED(result))
    return remember(state, result);

  result = swapChain1->QueryInterface(IID_PPV_ARGS(&state->swapChain));
  if (FAILED(result)) {
    releaseCom(swapChain1);
    return remember(state, result);
  }
  DXGI_SWAP_CHAIN_DESC1 actualDescription = {};
  state->swapChain->GetDesc1(&actualDescription);
  state->swapChainFlags = actualDescription.Flags;
  IDXGISwapChain2 *swapChain2 = nullptr;
  if (SUCCEEDED(state->swapChain->QueryInterface(IID_PPV_ARGS(&swapChain2)))) {
    swapChain2->SetMaximumFrameLatency(state->bufferCount - 1);
    state->frameLatencyWaitableObject =
        swapChain2->GetFrameLatencyWaitableObject();
  }
  releaseCom(swapChain2);
  releaseCom(swapChain1);
  state->currentBufferIndex = state->swapChain->GetCurrentBackBufferIndex();
  state->lastError = S_OK;
  return S_OK;
}

HRESULT resizeSwapChain(WindowsDirect3DDevice *state, UINT width, UINT height) {
  HRESULT result = waitForGpuIdle(state);
  if (FAILED(result))
    return result;
  releaseBuffers(state);
  state->frameLatencyWaitableObject = nullptr;
  result =
      state->swapChain->ResizeBuffers(state->bufferCount, width, height,
                                      kSwapChainFormat, state->swapChainFlags);
  if (FAILED(result))
    return remember(state, result);
  IDXGISwapChain2 *swapChain2 = nullptr;
  if (SUCCEEDED(state->swapChain->QueryInterface(IID_PPV_ARGS(&swapChain2)))) {
    swapChain2->SetMaximumFrameLatency(state->bufferCount - 1);
    state->frameLatencyWaitableObject =
        swapChain2->GetFrameLatencyWaitableObject();
  }
  releaseCom(swapChain2);
  state->currentBufferIndex = state->swapChain->GetCurrentBackBufferIndex();
  state->lastError = S_OK;
  return S_OK;
}

} // namespace

extern "C" KNativePointer skiko_windows_d3d_create(KNativePointer window,
                                                   KInt adapterPriority,
                                                   KInt bufferCount,
                                                   KBoolean transparency) {
  gLastCreateError = S_OK;
  HWND hwnd = reinterpret_cast<HWND>(window);
  if (hwnd == nullptr || !IsWindow(hwnd)) {
    gLastCreateError = E_INVALIDARG;
    return nullptr;
  }

  IDXGIFactory4 *factory = nullptr;
  HRESULT result = CreateDXGIFactory2(0, IID_PPV_ARGS(&factory));
  if (FAILED(result)) {
    gLastCreateError = result;
    return nullptr;
  }
  DXGI_GPU_PREFERENCE preference = DXGI_GPU_PREFERENCE_UNSPECIFIED;
  if (adapterPriority == 1)
    preference = DXGI_GPU_PREFERENCE_MINIMUM_POWER;
  if (adapterPriority == 2)
    preference = DXGI_GPU_PREFERENCE_HIGH_PERFORMANCE;

  IDXGIAdapter1 *adapter = nullptr;
  ID3D12Device *device = nullptr;
  result = chooseAdapterAndDevice(factory, preference, &adapter, &device);
  releaseCom(factory);
  if (FAILED(result)) {
    gLastCreateError = result;
    return nullptr;
  }

  D3D12_COMMAND_QUEUE_DESC queueDescription = {};
  queueDescription.Type = D3D12_COMMAND_LIST_TYPE_DIRECT;
  ID3D12CommandQueue *queue = nullptr;
  result = device->CreateCommandQueue(&queueDescription, IID_PPV_ARGS(&queue));
  if (FAILED(result)) {
    releaseCom(device);
    releaseCom(adapter);
    gLastCreateError = result;
    return nullptr;
  }

  auto *state = static_cast<WindowsDirect3DDevice *>(HeapAlloc(
      GetProcessHeap(), HEAP_ZERO_MEMORY, sizeof(WindowsDirect3DDevice)));
  if (state == nullptr) {
    releaseCom(queue);
    releaseCom(device);
    releaseCom(adapter);
    gLastCreateError = E_OUTOFMEMORY;
    return nullptr;
  }
  state->window = hwnd;
  state->adapter = adapter;
  state->device = device;
  state->queue = queue;
  state->bufferCount = bufferCount == 3 ? 3 : 2;
  state->nextFenceValue = 1;
  state->lastError = S_OK;
  state->transparencyRequested = transparency != 0;
  storeAdapterDescription(state);

  result = state->device->CreateFence(0, D3D12_FENCE_FLAG_NONE,
                                      IID_PPV_ARGS(&state->fence));
  if (SUCCEEDED(result)) {
    state->fenceEvent = CreateEventW(nullptr, FALSE, FALSE, nullptr);
    if (state->fenceEvent == nullptr)
      result = HRESULT_FROM_WIN32(GetLastError());
  }
  if (FAILED(result)) {
    releaseCom(state->fence);
    releaseCom(state->queue);
    releaseCom(state->device);
    releaseCom(state->adapter);
    HeapFree(GetProcessHeap(), 0, state);
    gLastCreateError = result;
    return nullptr;
  }
  return state;
}

extern "C" KInt skiko_windows_d3d_last_create_error() {
  return static_cast<KInt>(gLastCreateError);
}

extern "C" KNativePointer skiko_windows_d3d_adapter(KNativePointer handle) {
  auto *state = reinterpret_cast<WindowsDirect3DDevice *>(handle);
  return state == nullptr ? nullptr : state->adapter;
}

extern "C" KNativePointer skiko_windows_d3d_device(KNativePointer handle) {
  auto *state = reinterpret_cast<WindowsDirect3DDevice *>(handle);
  return state == nullptr ? nullptr : state->device;
}

extern "C" KNativePointer skiko_windows_d3d_queue(KNativePointer handle) {
  auto *state = reinterpret_cast<WindowsDirect3DDevice *>(handle);
  return state == nullptr ? nullptr : state->queue;
}

extern "C" KNativePointer
skiko_windows_d3d_adapter_name(KNativePointer handle) {
  auto *state = reinterpret_cast<WindowsDirect3DDevice *>(handle);
  return state == nullptr ? nullptr : state->adapterName;
}

extern "C" KLong skiko_windows_d3d_adapter_memory(KNativePointer handle) {
  auto *state = reinterpret_cast<WindowsDirect3DDevice *>(handle);
  return state == nullptr ? 0 : static_cast<KLong>(state->adapterMemory);
}

extern "C" KInt skiko_windows_d3d_ensure_swap_chain(KNativePointer handle,
                                                    KInt width, KInt height,
                                                    KBoolean resize) {
  auto *state = reinterpret_cast<WindowsDirect3DDevice *>(handle);
  if (state == nullptr || width <= 0 || height <= 0)
    return E_INVALIDARG;
  HRESULT result = state->swapChain == nullptr
                       ? createSwapChain(state, static_cast<UINT>(width),
                                         static_cast<UINT>(height))
                   : resize != 0
                       ? resizeSwapChain(state, static_cast<UINT>(width),
                                         static_cast<UINT>(height))
                       : S_OK;
  return static_cast<KInt>(result);
}

extern "C" KInt skiko_windows_d3d_set_source_size(KNativePointer handle,
                                                   KInt width, KInt height,
                                                   KFloat scaleX,
                                                   KFloat scaleY) {
  auto *state = reinterpret_cast<WindowsDirect3DDevice *>(handle);
  if (state == nullptr || state->swapChain == nullptr || width <= 0 ||
      height <= 0 || scaleX <= 0.0f || scaleX > 1.0f || scaleY <= 0.0f ||
      scaleY > 1.0f)
    return E_INVALIDARG;
  HRESULT result = state->swapChain->SetSourceSize(static_cast<UINT>(width),
                                                   static_cast<UINT>(height));
  if (FAILED(result))
    return static_cast<KInt>(remember(state, result));
  // DXGI_SCALING_STRETCH maps the selected source region to the full
  // capacity-backed swap chain. Scale each axis back to the current physical
  // source size. Unlike a DirectComposition visual commit, this transform
  // becomes active with the next Present, atomically with the replacement
  // frame, so live resize cannot briefly display it at the wrong scale.
  const DXGI_MATRIX_3X2_F transform = {scaleX, 0.0f, 0.0f,
                                       scaleY, 0.0f, 0.0f};
  result = state->swapChain->SetMatrixTransform(&transform);
  if (FAILED(result))
    return static_cast<KInt>(remember(state, result));
  state->lastError = S_OK;
  return S_OK;
}

extern "C" KNativePointer
skiko_windows_d3d_acquire_buffer(KNativePointer handle) {
  auto *state = reinterpret_cast<WindowsDirect3DDevice *>(handle);
  if (state == nullptr || state->swapChain == nullptr)
    return nullptr;
  if (state->frameLatencyWaitableObject != nullptr) {
    DWORD result =
        WaitForSingleObjectEx(state->frameLatencyWaitableObject, 5000, FALSE);
    if (result != WAIT_OBJECT_0) {
      state->lastError = result == WAIT_TIMEOUT
                             ? DXGI_ERROR_DEVICE_HUNG
                             : HRESULT_FROM_WIN32(GetLastError());
      return nullptr;
    }
  }
  state->currentBufferIndex = state->swapChain->GetCurrentBackBufferIndex();
  HRESULT result =
      waitForFence(state, state->bufferFenceValues[state->currentBufferIndex]);
  if (FAILED(result))
    return nullptr;
  if (state->buffers[state->currentBufferIndex] == nullptr) {
    result = state->swapChain->GetBuffer(
        state->currentBufferIndex,
        IID_PPV_ARGS(&state->buffers[state->currentBufferIndex]));
    if (FAILED(result)) {
      remember(state, result);
      return nullptr;
    }
  }
  return state->buffers[state->currentBufferIndex];
}

extern "C" KInt skiko_windows_d3d_current_buffer_index(KNativePointer handle) {
  auto *state = reinterpret_cast<WindowsDirect3DDevice *>(handle);
  return state == nullptr ? -1 : static_cast<KInt>(state->currentBufferIndex);
}

extern "C" KInt skiko_windows_d3d_present(KNativePointer handle,
                                          KBoolean waitForVsync) {
  auto *state = reinterpret_cast<WindowsDirect3DDevice *>(handle);
  if (state == nullptr || state->swapChain == nullptr)
    return E_INVALIDARG;
  const UINT renderedBufferIndex = state->currentBufferIndex;
  HRESULT result = state->swapChain->Present(waitForVsync != 0 ? 1 : 0, 0);
  if (FAILED(result))
    return static_cast<KInt>(remember(state, result));
  const UINT64 fenceValue = state->nextFenceValue++;
  result = state->queue->Signal(state->fence, fenceValue);
  if (FAILED(result))
    return static_cast<KInt>(remember(state, result));
  state->bufferFenceValues[renderedBufferIndex] = fenceValue;
  state->currentBufferIndex = state->swapChain->GetCurrentBackBufferIndex();
  state->lastError = S_OK;
  return S_OK;
}

extern "C" KBoolean
skiko_windows_d3d_transparency_supported(KNativePointer handle) {
  auto *state = reinterpret_cast<WindowsDirect3DDevice *>(handle);
  return state != nullptr && state->transparencySupported ? 1 : 0;
}

extern "C" KBoolean skiko_windows_d3d_is_device_lost(KNativePointer handle) {
  auto *state = reinterpret_cast<WindowsDirect3DDevice *>(handle);
  if (state == nullptr || state->device == nullptr)
    return 1;
  if (isDeviceLostResult(state->lastError))
    return 1;
  HRESULT reason = state->device->GetDeviceRemovedReason();
  if (FAILED(reason)) {
    state->lastError = reason;
    return 1;
  }
  return 0;
}

extern "C" KInt skiko_windows_d3d_last_error(KNativePointer handle) {
  auto *state = reinterpret_cast<WindowsDirect3DDevice *>(handle);
  return state == nullptr ? E_INVALIDARG : static_cast<KInt>(state->lastError);
}

extern "C" void skiko_windows_d3d_close(KNativePointer handle,
                                        KBoolean contextLost) {
  auto *state = reinterpret_cast<WindowsDirect3DDevice *>(handle);
  if (state == nullptr)
    return;
  if (contextLost == 0)
    waitForGpuIdle(state);
  releaseBuffers(state);
  releaseComposition(state);
  releaseCom(state->swapChain);
  releaseCom(state->fence);
  if (state->fenceEvent != nullptr)
    CloseHandle(state->fenceEvent);
  releaseCom(state->queue);
  releaseCom(state->device);
  releaseCom(state->adapter);
  HeapFree(GetProcessHeap(), 0, state);
}

#endif

#if defined(SK_BUILD_FOR_WIN)
// Skia's /MT archives register C++ static destructors through atexit. Kotlin/Native
// starts with MinGW's CRT, whose on-exit table is incompatible with that registration.
// These are process-lifetime caches, so their destructors do not need to run on exit.
extern "C" int __wrap_atexit(void (*)(void)) { return 0; }
#endif
