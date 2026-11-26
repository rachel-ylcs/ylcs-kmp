package love.yinlin.platform.ffi

@Suppress("UnusedReceiverParameter")
inline val FFI.cfgmgr32: CfgMgr32Library get() = CfgMgr32Library.Instance

@Suppress("PropertyName", "unused", "SpellCheckingInspection")
class CfgMgr32Library private constructor() : NativeLibrary("cfgmgr32") {
    @PublishedApi
    internal companion object {
        @PublishedApi
        internal val Instance = CfgMgr32Library()

        private val pointer = NativeType.Pointer
        private val bool = Win32Type.BOOL
        private val dword = Win32Type.DWORD
        private val int16 = NativeType.Short
        private val narrowString = NativeType.String
        private val wideString = NativeType.WString
    }

    val CM_Enumerate_Classes by func { declare(dword, pointer, dword, retType = dword) }
    val CM_Get_Child by func { declare(pointer, dword, dword, retType = dword) }
    val CM_Get_Class_NameA by func { declare(pointer, narrowString, pointer, dword, retType = dword) }
    val CM_Get_Class_NameW by func { declare(pointer, wideString, pointer, dword, retType = dword) }
    val CM_Get_Class_Property_Keys by func { declare(pointer, pointer, pointer, dword, retType = dword) }
    val CM_Get_Class_PropertyW by func { declare(pointer, pointer, pointer, pointer, pointer, dword, retType = dword) }
    val CM_Get_Depth by func { declare(pointer, dword, dword, retType = dword) }
    val CM_Get_Device_ID_List_SizeA by func { declare(pointer, narrowString, dword, retType = dword) }
    val CM_Get_Device_ID_List_SizeW by func { declare(pointer, wideString, dword, retType = dword) }
    val CM_Get_Device_ID_ListA by func { declare(narrowString, narrowString, dword, dword, retType = dword) }
    val CM_Get_Device_ID_ListW by func { declare(wideString, wideString, dword, dword, retType = dword) }
    val CM_Get_Device_ID_Size by func { declare(pointer, dword, dword, retType = dword) }
    val CM_Get_Device_IDA by func { declare(dword, narrowString, dword, dword, retType = dword) }
    val CM_Get_Device_IDW by func { declare(dword, wideString, dword, dword, retType = dword) }
    val CM_Get_Device_Interface_List_SizeA by func { declare(pointer, pointer, narrowString, dword, retType = dword) }
    val CM_Get_Device_Interface_List_SizeW by func { declare(pointer, pointer, wideString, dword, retType = dword) }
    val CM_Get_Device_Interface_ListA by func { declare(pointer, narrowString, narrowString, dword, dword, retType = dword) }
    val CM_Get_Device_Interface_ListW by func { declare(pointer, wideString, wideString, dword, dword, retType = dword) }
    val CM_Get_Device_Interface_Property_KeysW by func { declare(wideString, pointer, pointer, dword, retType = dword) }
    val CM_Get_Device_Interface_PropertyW by func { declare(wideString, pointer, pointer, pointer, pointer, dword, retType = dword) }
    val CM_Get_DevNode_Property_Keys by func { declare(dword, pointer, pointer, dword, retType = dword) }
    val CM_Get_DevNode_PropertyW by func { declare(dword, pointer, pointer, pointer, pointer, dword, retType = dword) }
    val CM_Get_DevNode_Registry_PropertyA by func { declare(dword, dword, pointer, pointer, pointer, dword, retType = dword) }
    val CM_Get_DevNode_Registry_PropertyW by func { declare(dword, dword, pointer, pointer, pointer, dword, retType = dword) }
    val CM_Get_DevNode_Status by func { declare(pointer, pointer, dword, dword, retType = dword) }
    val CM_Get_Parent by func { declare(pointer, dword, dword, retType = dword) }
    val CM_Get_Sibling by func { declare(pointer, dword, dword, retType = dword) }
    val CM_Get_Version by func { declare(retType = int16) }
    val CM_Is_Version_Available by func { declare(int16, retType = bool) }
    val CM_Locate_DevNodeA by func { declare(pointer, narrowString, dword, retType = dword) }
    val CM_Locate_DevNodeW by func { declare(pointer, wideString, dword, retType = dword) }
    val CM_MapCrToWin32Err by func { declare(dword, dword, retType = dword) }
    val CM_Open_DevNode_Key by func { declare(dword, dword, dword, dword, pointer, dword, retType = dword) }
    val CM_Register_Notification by func { declare(pointer, pointer, pointer, pointer, retType = dword) }
    val CM_Unregister_Notification by func { declare(pointer, retType = dword) }
}
