#include "windows_media.h"
#include <winrt/windows.graphics.directx.direct3d11.h>
#include <Windows.Graphics.DirectX.Direct3D11.interop.h>
#include <d3d11_4.h>
#ifdef RACHEL_VIDEO_GPU
#ifndef _In_count_
#define _In_count_(count)
#endif
#ifndef _In_opt_count_
#define _In_opt_count_(count)
#endif
#pragma push_macro("MIDL_INTERFACE")
#undef MIDL_INTERFACE
#define MIDL_INTERFACE(id) struct __declspec(uuid(id))
#include <d3d12.h>
#include <d3d11on12.h>
#pragma pop_macro("MIDL_INTERFACE")
#pragma push_macro("__uuidof")
#undef __uuidof
template <typename T> const GUID &videoSdkGuid() { return __uuidof(T); }
#pragma pop_macro("__uuidof")
#endif
#include <array>
#include <cstring>

using winrt::Windows::Graphics::DirectX::Direct3D11::IDirect3DSurface;

namespace {
    bool validSize(int width, int height) {
        return width > 0 && height > 0 && width <= 16384 && height <= 16384 && static_cast<int64_t>(width) * height * 4 <= INT32_MAX;
    }

    IDirect3DSurface videoSurface(ID3D11Resource *resource) {
        winrt::com_ptr<IDXGISurface> surface;
        winrt::check_hresult(resource->QueryInterface(__uuidof(IDXGISurface), surface.put_void()));
        winrt::com_ptr<::IInspectable> inspectable;
        static const auto createSurface = reinterpret_cast<decltype(&CreateDirect3D11SurfaceFromDXGISurface)>(GetProcAddress(GetModuleHandleW(L"d3d11.dll"), "CreateDirect3D11SurfaceFromDXGISurface"));
        if (!createSurface) throw RachelAudioError(E_NOINTERFACE, "Direct3D video surfaces are not available");
        winrt::check_hresult(createSurface(surface.get(), inspectable.put()));
        return inspectable.as<IDirect3DSurface>();
    }

    void protectDevice(ID3D11Device *device) {
        winrt::com_ptr<ID3D11Multithread> protection;
        winrt::check_hresult(device->QueryInterface(__uuidof(ID3D11Multithread), protection.put_void()));
        protection->SetMultithreadProtected(TRUE);
    }

    struct CpuVideoSurface {
        winrt::com_ptr<ID3D11Device> device;
        winrt::com_ptr<ID3D11DeviceContext> commands;
        winrt::com_ptr<ID3D11Texture2D> texture;
        winrt::com_ptr<ID3D11Texture2D> staging;
        IDirect3DSurface surface {nullptr};
        int width;
        int height;

        CpuVideoSurface(int w, int h) : width(w), height(h) {
            const auto flags = D3D11_CREATE_DEVICE_BGRA_SUPPORT | D3D11_CREATE_DEVICE_VIDEO_SUPPORT;
            auto result = D3D11CreateDevice(nullptr, D3D_DRIVER_TYPE_HARDWARE, nullptr, flags, nullptr, 0, D3D11_SDK_VERSION, device.put(), nullptr, commands.put());
            if (FAILED(result)) winrt::check_hresult(D3D11CreateDevice(nullptr, D3D_DRIVER_TYPE_WARP, nullptr, D3D11_CREATE_DEVICE_BGRA_SUPPORT, nullptr, 0, D3D11_SDK_VERSION, device.put(), nullptr, commands.put()));
            protectDevice(device.get());
            D3D11_TEXTURE2D_DESC desc {};
            desc.Width = static_cast<UINT>(width);
            desc.Height = static_cast<UINT>(height);
            desc.MipLevels = 1;
            desc.ArraySize = 1;
            desc.Format = DXGI_FORMAT_B8G8R8A8_UNORM;
            desc.SampleDesc.Count = 1;
            desc.Usage = D3D11_USAGE_DEFAULT;
            desc.BindFlags = D3D11_BIND_RENDER_TARGET | D3D11_BIND_SHADER_RESOURCE;
            winrt::check_hresult(device->CreateTexture2D(&desc, nullptr, texture.put()));
            surface = videoSurface(texture.get());
            desc.Usage = D3D11_USAGE_STAGING;
            desc.BindFlags = 0;
            desc.CPUAccessFlags = D3D11_CPU_ACCESS_READ;
            winrt::check_hresult(device->CreateTexture2D(&desc, nullptr, staging.put()));
        }

        void copy(const winrt::Windows::Media::Playback::MediaPlayer &player, void *pixels) {
            player.CopyFrameToVideoSurface(surface);
            commands->CopyResource(staging.get(), texture.get());
            D3D11_MAPPED_SUBRESOURCE mapped {};
            winrt::check_hresult(commands->Map(staging.get(), 0, D3D11_MAP_READ, 0, &mapped));
            const auto rowBytes = static_cast<size_t>(width) * 4;
            if (mapped.RowPitch < rowBytes) {
                commands->Unmap(staging.get(), 0);
                throw RachelAudioError(E_FAIL, "Invalid mapped video row pitch");
            }
            if (mapped.RowPitch == rowBytes) std::memcpy(pixels, mapped.pData, rowBytes * height);
            else {
                for (int y = 0; y < height; ++y) {
                    std::memcpy(static_cast<uint8_t *>(pixels) + y * rowBytes, static_cast<const uint8_t *>(mapped.pData) + static_cast<size_t>(y) * mapped.RowPitch, rowBytes);
                }
            }
            commands->Unmap(staging.get(), 0);
        }
    };

#ifdef RACHEL_VIDEO_GPU
    extern "C" int8_t skiko_windows_d3d_acquire_context(void *context, void **device, void **queue);
    extern "C" int32_t skiko_windows_d3d_target_state(void *target);

    struct GpuVideoSurface {
        struct Transfer {
            winrt::com_ptr<ID3D12CommandAllocator> allocator;
            winrt::com_ptr<ID3D12GraphicsCommandList> commands;
            uint64_t fence = 0;
        };

        winrt::com_ptr<ID3D12Device> device;
        winrt::com_ptr<ID3D12CommandQueue> queue;
        winrt::com_ptr<ID3D12Resource> texture;
        winrt::com_ptr<ID3D12Fence> fence;
        winrt::handle fenceEvent;
        winrt::com_ptr<ID3D11Device> device11;
        winrt::com_ptr<ID3D11DeviceContext> commands11;
        winrt::com_ptr<ID3D11On12Device> interop;
        winrt::com_ptr<ID3D11Resource> wrapped;
        IDirect3DSurface surface {nullptr};
        std::array<Transfer, 3> transfers;
        uint64_t nextFence = 1;
        size_t transferIndex = 0;
        bool failed = false;
        void *context;
        int width;
        int height;

        GpuVideoSurface(void *ctx, int w, int h) : context(ctx), width(w), height(h) {
            if (!skiko_windows_d3d_acquire_context(context, device.put_void(), queue.put_void())) throw RachelAudioError(E_NOINTERFACE, "The canvas does not use the Native Direct3D renderer");
            IUnknown *queues[] = {queue.get()};
            static const auto createOn12 = reinterpret_cast<PFN_D3D11ON12_CREATE_DEVICE>(GetProcAddress(GetModuleHandleW(L"d3d11.dll"), "D3D11On12CreateDevice"));
            if (!createOn12) throw RachelAudioError(E_NOINTERFACE, "D3D11On12 is not available");
            winrt::check_hresult(createOn12(device.get(), D3D11_CREATE_DEVICE_BGRA_SUPPORT, nullptr, 0, queues, 1, 0, device11.put(), commands11.put(), nullptr));
            protectDevice(device11.get());
            winrt::check_hresult(device11->QueryInterface(videoSdkGuid<ID3D11On12Device>(), interop.put_void()));
            D3D12_HEAP_PROPERTIES heap {};
            heap.Type = D3D12_HEAP_TYPE_DEFAULT;
            heap.CreationNodeMask = 1;
            heap.VisibleNodeMask = 1;
            D3D12_RESOURCE_DESC desc {};
            desc.Dimension = D3D12_RESOURCE_DIMENSION_TEXTURE2D;
            desc.Width = static_cast<UINT64>(width);
            desc.Height = static_cast<UINT>(height);
            desc.DepthOrArraySize = 1;
            desc.MipLevels = 1;
            desc.Format = DXGI_FORMAT_B8G8R8A8_UNORM;
            desc.SampleDesc.Count = 1;
            desc.Flags = D3D12_RESOURCE_FLAG_ALLOW_RENDER_TARGET;
            winrt::check_hresult(device->CreateCommittedResource(&heap, D3D12_HEAP_FLAG_NONE, &desc, D3D12_RESOURCE_STATE_COMMON, nullptr, videoSdkGuid<ID3D12Resource>(), texture.put_void()));
            D3D11_RESOURCE_FLAGS flags {};
            flags.BindFlags = D3D11_BIND_RENDER_TARGET | D3D11_BIND_SHADER_RESOURCE;
            winrt::check_hresult(interop->CreateWrappedResource(texture.get(), &flags, D3D12_RESOURCE_STATE_COMMON, D3D12_RESOURCE_STATE_COMMON, __uuidof(ID3D11Resource), wrapped.put_void()));
            surface = videoSurface(wrapped.get());
            ID3D11Resource *resources[] = {wrapped.get()};
            interop->ReleaseWrappedResources(resources, 1);
            commands11->Flush();
            winrt::check_hresult(device->CreateFence(0, D3D12_FENCE_FLAG_NONE, videoSdkGuid<ID3D12Fence>(), fence.put_void()));
            fenceEvent.attach(CreateEventW(nullptr, FALSE, FALSE, nullptr));
            if (!fenceEvent) winrt::throw_last_error();
        }

        void wait(uint64_t value) {
            if (!value || fence->GetCompletedValue() >= value) return;
            winrt::check_hresult(fence->SetEventOnCompletion(value, fenceEvent.get()));
            if (WaitForSingleObject(fenceEvent.get(), 5000) != WAIT_OBJECT_0) throw RachelAudioError(DXGI_ERROR_DEVICE_HUNG, "Video GPU synchronization timed out");
        }

        ~GpuVideoSurface() {
            try {
                const auto value = nextFence++;
                winrt::check_hresult(queue->Signal(fence.get(), value));
                wait(value);
            }
            catch (...) { }
        }

        void copy(const winrt::Windows::Media::Playback::MediaPlayer &player) {
            ID3D11Resource *resources[] = {wrapped.get()};
            interop->AcquireWrappedResources(resources, 1);
            try { player.CopyFrameToVideoSurface(surface); }
            catch (...) {
                interop->ReleaseWrappedResources(resources, 1);
                commands11->Flush();
                throw;
            }
            interop->ReleaseWrappedResources(resources, 1);
            commands11->Flush();
        }

        bool complete(void *target) {
            const auto state = skiko_windows_d3d_target_state(target);
            if (state < 0) return false;
            if (state == D3D12_RESOURCE_STATE_COMMON) return true;
            auto &transfer = transfers[transferIndex++ % transfers.size()];
            wait(transfer.fence);
            if (!transfer.allocator) {
                winrt::check_hresult(device->CreateCommandAllocator(D3D12_COMMAND_LIST_TYPE_DIRECT, videoSdkGuid<ID3D12CommandAllocator>(), transfer.allocator.put_void()));
                winrt::check_hresult(device->CreateCommandList(0, D3D12_COMMAND_LIST_TYPE_DIRECT, transfer.allocator.get(), nullptr, videoSdkGuid<ID3D12GraphicsCommandList>(), transfer.commands.put_void()));
            }
            else {
                winrt::check_hresult(transfer.allocator->Reset());
                winrt::check_hresult(transfer.commands->Reset(transfer.allocator.get(), nullptr));
            }
            D3D12_RESOURCE_BARRIER barrier {};
            barrier.Type = D3D12_RESOURCE_BARRIER_TYPE_TRANSITION;
            barrier.Transition.pResource = texture.get();
            barrier.Transition.Subresource = D3D12_RESOURCE_BARRIER_ALL_SUBRESOURCES;
            barrier.Transition.StateBefore = static_cast<D3D12_RESOURCE_STATES>(state);
            barrier.Transition.StateAfter = D3D12_RESOURCE_STATE_COMMON;
            transfer.commands->ResourceBarrier(1, &barrier);
            winrt::check_hresult(transfer.commands->Close());
            ID3D12CommandList *lists[] = {transfer.commands.get()};
            queue->ExecuteCommandLists(1, lists);
            transfer.fence = nextFence++;
            winrt::check_hresult(queue->Signal(fence.get(), transfer.fence));
            return true;
        }
    };

    struct GpuVideoFrame final : RachelVideoFrame {
        std::shared_ptr<GpuVideoSurface> surface;

        explicit GpuVideoFrame(std::shared_ptr<GpuVideoSurface> value, int64_t serial) : surface(std::move(value)) {
            info = {surface->width, surface->height, serial, surface->texture.get()};
        }

        bool complete(void *target) override {
            try {
                const auto completed = surface->complete(target);
                if (!completed) surface->failed = true;
                return completed;
            }
            catch (...) {
                surface->failed = true;
                throw;
            }
        }
    };
#endif

    struct VideoNotifications {
        std::shared_ptr<AudioNotifications> notifications;
        std::atomic_bool closed {false};
        std::atomic_bool pending {false};
        std::atomic<int64_t> serial {0};
    };

    class WindowsVideoPlayer final : public WindowsMediaPlayer {
        std::shared_ptr<VideoNotifications> video = std::make_shared<VideoNotifications>();
        winrt::Windows::Media::Playback::MediaPlayer::VideoFrameAvailable_revoker frameAvailable;
        std::unique_ptr<CpuVideoSurface> cpu;
        std::mutex frameMutex;
#ifdef RACHEL_VIDEO_GPU
        std::shared_ptr<GpuVideoSurface> gpu;
        void *rejectedContext = nullptr;
#endif
    public:
        explicit WindowsVideoPlayer(const std::shared_ptr<AudioNotifications> &notifications) : WindowsMediaPlayer(notifications) {
            WindowsApartment apartment;
            video->notifications = notifications;
            player.AudioCategory(winrt::Windows::Media::Playback::MediaPlayerAudioCategory::Movie);
            player.IsVideoFrameServerEnabled(true);
            std::weak_ptr<VideoNotifications> weak = video;
            frameAvailable = player.VideoFrameAvailable(winrt::auto_revoke, [weak](auto const &, auto const &) {
                if (auto current = weak.lock(); current && !current->closed.load()) {
                    ++current->serial;
                    if (!current->pending.exchange(true)) current->notifications->emit(RACHEL_AUDIO_EVENT_FRAME);
                }
            });
        }

        ~WindowsVideoPlayer() override {
            video->closed.store(true);
            frameAvailable.revoke();
        }

        bool videoInfo(RachelVideoFrameInfo &info) override {
            WindowsApartment apartment;
            std::lock_guard<std::mutex> guard(frameMutex);
            video->pending.store(false);
            info = {static_cast<int>(session.NaturalVideoWidth()), static_cast<int>(session.NaturalVideoHeight()), video->serial.load(), nullptr};
            return info.serial > 0 && validSize(info.width, info.height);
        }

        bool copyVideoFrame(void *pixels, int64_t capacity, int width, int height) override {
            WindowsApartment apartment;
            std::lock_guard<std::mutex> guard(frameMutex);
            if (!validSize(width, height) || session.NaturalVideoWidth() != static_cast<UINT32>(width) || session.NaturalVideoHeight() != static_cast<UINT32>(height) || static_cast<int64_t>(width) * height * 4 > capacity) return false;
            if (!cpu || cpu->width != width || cpu->height != height) cpu = std::make_unique<CpuVideoSurface>(width, height);
            cpu->copy(player, pixels);
            return true;
        }

        std::unique_ptr<RachelVideoFrame> gpuVideoFrame(void *context) override {
#ifdef RACHEL_VIDEO_GPU
            WindowsApartment apartment;
            std::lock_guard<std::mutex> guard(frameMutex);
            const auto width = static_cast<int>(session.NaturalVideoWidth());
            const auto height = static_cast<int>(session.NaturalVideoHeight());
            if (!context || context == rejectedContext || !validSize(width, height)) return nullptr;
            if (gpu && gpu->failed) {
                rejectedContext = gpu->context;
                gpu.reset();
                return nullptr;
            }
            try {
                if (!gpu || gpu->context != context || gpu->width != width || gpu->height != height) gpu = std::make_shared<GpuVideoSurface>(context, width, height);
                gpu->copy(player);
                return std::make_unique<GpuVideoFrame>(gpu, video->serial.load());
            }
            catch (...) {
                rejectedContext = context;
                gpu.reset();
                return nullptr;
            }
#else
            return nullptr;
#endif
        }
    };
}

std::shared_ptr<RachelAudioBackend> rachel_video_open(const char *path, const std::shared_ptr<AudioNotifications> &notifications) {
    try {
        auto result = std::make_shared<WindowsVideoPlayer>(notifications);
        result->load(path);
        return result;
    }
    catch (const winrt::hresult_error &error) { throw RachelAudioError(error.code().value, winrt::to_string(error.message())); }
}
