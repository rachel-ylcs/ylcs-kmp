package love.yinlin.platform.ffi

object Win32 {
    typealias BOOL = Int
    typealias DWORD = Int
    typealias POINTER = Address
    typealias HANDLE = Address

    const val TRUE: DWORD = 1
    const val FALSE: DWORD = 0

    const val EVENT_ALL_ACCESS: DWORD = 0x1F0003
    const val ERROR_SUCCESS: DWORD = 0
    const val ERROR_ALREADY_EXISTS: DWORD = 183

    const val GWL_EXSTYLE: DWORD = -20

    const val WS_EX_TRANSPARENT: DWORD = 0x00000020
    const val WS_EX_TOOLWINDOW: DWORD = 0x00000080
}

object Win32Type {
    val BOOL: Layout = NativeType.Int
    val DWORD: Layout = NativeType.Int
    val POINTER: Layout = NativeType.Pointer
    val HWND: Layout = NativeType.Pointer
    val HANDLE: Layout = NativeType.Pointer

    val COORD: Layout = NamedStructLayout("COORD") {
        "X" with NativeType.Short
        "Y" with NativeType.Short
    }

    val POINT: Layout = NamedStructLayout("POINT") {
        "x" with NativeType.Int
        "y" with NativeType.Int
    }

    val IORING_CREATE_FLAGS: Layout = NamedStructLayout("IORING_CREATE_FLAGS") {
        "Required" with NativeType.Int
        "Advisory" with NativeType.Int
    }

    val IORING_HANDLE_REF: Layout = NamedStructLayout("IORING_HANDLE_REF") {
        "Kind" with NativeType.Int
        padding(4L)
        union("Handle") {
            "Handle" with HANDLE
            "Index" with NativeType.Int
        }
    }

    val IORING_BUFFER_REF: Layout = NamedStructLayout("IORING_BUFFER_REF") {
        "Kind" with NativeType.Int
        padding(4L)
        union("Buffer") {
            "Address" with POINTER
            struct("IndexAndOffset") {
                "BufferIndex" with NativeType.Int
                "Offset" with NativeType.Int
            }
        }
    }

    val LARGE_INTEGER: Layout = NamedUnionLayout("LARGE_INTEGER") {
        val parts = NamedStructLayout {
            "LowPart" with DWORD
            "HighPart" with NativeType.Int
        }
        add(parts)
        "u" with parts
        "QuadPart" with NativeType.Long
    }
}
