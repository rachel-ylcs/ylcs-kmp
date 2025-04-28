#pragma once
#if defined(_WIN32)

#include <concepts>

#include <windows.h>
#include <shobjidl.h>

struct COM_ENVIRONMENT {
	bool handler{};
	COM_ENVIRONMENT() { handler = SUCCEEDED(CoInitialize(nullptr)); }
	~COM_ENVIRONMENT() { if (handler) CoUninitialize(); }
	explicit operator bool() const { return handler; }
};

template<std::derived_from<IUnknown> T>
struct COM_ITEM {
	T* handler{};
	~COM_ITEM() { if (handler) handler->Release(); }
	inline explicit operator bool() const { return handler; }
	inline T* operator -> () { return handler; }
	inline T* operator -> () const { return handler; }
	inline operator T** () { return &handler; }
	inline operator T** () const { return &handler; }
};

template<std::derived_from<IUnknown> T>
struct COM_OBJECT : COM_ITEM<T> {
	COM_OBJECT(const IID& clsid, const IID& iid) {
		if (!SUCCEEDED(CoCreateInstance(clsid, nullptr, CLSCTX_ALL, iid, reinterpret_cast<void**>(&this->handler)))) this->handler = nullptr;
	}
};

template<typename T>
requires std::is_pointer_v<T>
struct COM_MEM {
	T handler{};
	~COM_MEM() { if (handler) CoTaskMemFree(handler); }
	inline operator T* () { return &handler; }
	inline operator T* () const { return &handler; }
};

#endif