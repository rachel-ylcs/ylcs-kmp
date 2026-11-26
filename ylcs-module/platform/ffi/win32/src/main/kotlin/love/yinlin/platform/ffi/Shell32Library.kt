package love.yinlin.platform.ffi

@Suppress("UnusedReceiverParameter")
inline val FFI.shell32: Shell32Library get() = Shell32Library.Instance

@Suppress("PropertyName", "unused", "SpellCheckingInspection")
class Shell32Library private constructor() : NativeLibrary("shell32") {
    @PublishedApi
    internal companion object {
        @PublishedApi
        internal val Instance = Shell32Library()

        private val pointer = NativeType.Pointer
        private val bool = Win32Type.BOOL
        private val dword = Win32Type.DWORD
        private val handle = Win32Type.HANDLE
        private val hwnd = Win32Type.HWND
        private val int32 = NativeType.Int
        private val int64 = NativeType.Long
        private val narrowString = NativeType.String
        private val wideString = NativeType.WString
    }

    val CommandLineToArgvW by func { declare(wideString, pointer, retType = pointer) }
    val DragAcceptFiles by func { declare(hwnd, bool) }
    val DragFinish by func { declare(pointer) }
    val DragQueryFileA by func { declare(pointer, dword, narrowString, dword, retType = dword) }
    val DragQueryFileW by func { declare(pointer, dword, wideString, dword, retType = dword) }
    val DragQueryPoint by func { declare(pointer, pointer, retType = bool) }
    val DuplicateIcon by func { declare(pointer, pointer, retType = pointer) }
    val ExtractAssociatedIconA by func { declare(pointer, narrowString, pointer, retType = pointer) }
    val ExtractAssociatedIconW by func { declare(pointer, wideString, pointer, retType = pointer) }
    val ExtractIconA by func { declare(pointer, narrowString, dword, retType = pointer) }
    val ExtractIconExA by func { declare(narrowString, int32, pointer, pointer, dword, retType = dword) }
    val ExtractIconExW by func { declare(wideString, int32, pointer, pointer, dword, retType = dword) }
    val ExtractIconW by func { declare(pointer, wideString, dword, retType = pointer) }
    val FindExecutableA by func { declare(narrowString, narrowString, narrowString, retType = pointer) }
    val FindExecutableW by func { declare(wideString, wideString, wideString, retType = pointer) }
    val GetCurrentProcessExplicitAppUserModelID by func { declare(pointer, retType = int32) }
    val ILClone by func { declare(pointer, retType = pointer) }
    val ILCombine by func { declare(pointer, pointer, retType = pointer) }
    val ILFindLastID by func { declare(pointer, retType = pointer) }
    val ILFree by func { declare(pointer) }
    val ILGetSize by func { declare(pointer, retType = dword) }
    val ILIsEqual by func { declare(pointer, pointer, retType = bool) }
    val ILIsParent by func { declare(pointer, pointer, bool, retType = bool) }
    val ILRemoveLastID by func { declare(pointer, retType = bool) }
    val SetCurrentProcessExplicitAppUserModelID by func { declare(wideString, retType = int32) }
    val SHAddToRecentDocs by func { declare(dword, pointer) }
    val SHAppBarMessage by func { declare(dword, pointer, retType = int64) }
    val SHBrowseForFolderA by func { declare(pointer, retType = pointer) }
    val SHBrowseForFolderW by func { declare(pointer, retType = pointer) }
    val SHChangeNotification_Lock by func { declare(handle, dword, pointer, pointer, retType = handle) }
    val SHChangeNotification_Unlock by func { declare(handle, retType = bool) }
    val SHChangeNotify by func { declare(int32, dword, pointer, pointer) }
    val SHChangeNotifyDeregister by func { declare(dword, retType = bool) }
    val SHChangeNotifyRegister by func { declare(hwnd, int32, int32, dword, int32, pointer, retType = dword) }
    val SHCreateItemFromIDList by func { declare(pointer, pointer, pointer, retType = int32) }
    val SHCreateItemFromParsingName by func { declare(wideString, pointer, pointer, pointer, retType = int32) }
    val SHCreateItemFromRelativeName by func { declare(pointer, wideString, pointer, pointer, pointer, retType = int32) }
    val SHCreateShellItem by func { declare(pointer, pointer, pointer, pointer, retType = int32) }
    val Shell_NotifyIconA by func { declare(dword, pointer, retType = bool) }
    val Shell_NotifyIconGetRect by func { declare(pointer, pointer, retType = int32) }
    val Shell_NotifyIconW by func { declare(dword, pointer, retType = bool) }
    val ShellExecuteA by func { declare(hwnd, narrowString, narrowString, narrowString, narrowString, int32, retType = pointer) }
    val ShellExecuteExA by func { declare(pointer, retType = bool) }
    val ShellExecuteExW by func { declare(pointer, retType = bool) }
    val ShellExecuteW by func { declare(hwnd, wideString, wideString, wideString, wideString, int32, retType = pointer) }
    val SHEmptyRecycleBinA by func { declare(hwnd, narrowString, dword, retType = int32) }
    val SHEmptyRecycleBinW by func { declare(hwnd, wideString, dword, retType = int32) }
    val SHFileOperationA by func { declare(pointer, retType = int32) }
    val SHFileOperationW by func { declare(pointer, retType = int32) }
    val SHFreeNameMappings by func { declare(handle) }
    val SHGetDesktopFolder by func { declare(pointer, retType = int32) }
    val SHGetFileInfoA by func { declare(narrowString, dword, pointer, dword, dword, retType = int64) }
    val SHGetFileInfoW by func { declare(wideString, dword, pointer, dword, dword, retType = int64) }
    val SHGetFolderLocation by func { declare(hwnd, int32, handle, dword, pointer, retType = int32) }
    val SHGetFolderPathA by func { declare(hwnd, int32, handle, dword, narrowString, retType = int32) }
    val SHGetFolderPathW by func { declare(hwnd, int32, handle, dword, wideString, retType = int32) }
    val SHGetIDListFromObject by func { declare(pointer, pointer, retType = int32) }
    val SHGetImageList by func { declare(int32, pointer, pointer, retType = int32) }
    val SHGetKnownFolderIDList by func { declare(pointer, dword, handle, pointer, retType = int32) }
    val SHGetKnownFolderItem by func { declare(pointer, int32, handle, pointer, pointer, retType = int32) }
    val SHGetKnownFolderPath by func { declare(pointer, dword, handle, pointer, retType = int32) }
    val SHGetLocalizedName by func { declare(wideString, wideString, dword, pointer, retType = int32) }
    val SHGetNameFromIDList by func { declare(pointer, int32, pointer, retType = int32) }
    val SHGetPathFromIDListA by func { declare(pointer, narrowString, retType = bool) }
    val SHGetPathFromIDListEx by func { declare(pointer, wideString, dword, dword, retType = bool) }
    val SHGetPathFromIDListW by func { declare(pointer, wideString, retType = bool) }
    val SHGetPropertyStoreFromParsingName by func { declare(wideString, pointer, int32, pointer, pointer, retType = int32) }
    val SHGetSpecialFolderLocation by func { declare(hwnd, int32, pointer, retType = int32) }
    val SHGetSpecialFolderPathA by func { declare(hwnd, narrowString, int32, bool, retType = bool) }
    val SHGetSpecialFolderPathW by func { declare(hwnd, wideString, int32, bool, retType = bool) }
    val SHGetStockIconInfo by func { declare(int32, dword, pointer, retType = int32) }
    val SHOpenFolderAndSelectItems by func { declare(pointer, dword, pointer, dword, retType = int32) }
    val SHParseDisplayName by func { declare(wideString, pointer, pointer, dword, pointer, retType = int32) }
    val SHQueryRecycleBinA by func { declare(narrowString, pointer, retType = int32) }
    val SHQueryRecycleBinW by func { declare(wideString, pointer, retType = int32) }
    val SHQueryUserNotificationState by func { declare(pointer, retType = int32) }
    val SHRemoveLocalizedName by func { declare(wideString, retType = int32) }
    val SHSetKnownFolderPath by func { declare(pointer, dword, handle, wideString, retType = int32) }
    val SHSetLocalizedName by func { declare(wideString, wideString, int32, retType = int32) }
    val SHTestTokenMembership by func { declare(handle, dword, retType = bool) }
}
