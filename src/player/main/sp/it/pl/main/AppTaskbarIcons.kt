package sp.it.pl.main

import com.sun.jna.Callback
import com.sun.jna.Native
import com.sun.jna.Pointer
import com.sun.jna.Structure
import com.sun.jna.platform.win32.BaseTSD
import com.sun.jna.platform.win32.COM.COMUtils
import com.sun.jna.platform.win32.COM.Unknown
import com.sun.jna.platform.win32.Guid
import com.sun.jna.platform.win32.Kernel32
import com.sun.jna.platform.win32.User32
import com.sun.jna.platform.win32.WTypes
import com.sun.jna.platform.win32.WTypes.CLSCTX_SERVER
import com.sun.jna.platform.win32.WinBase
import com.sun.jna.platform.win32.WinDef
import com.sun.jna.platform.win32.WinNT
import com.sun.jna.platform.win32.WinUser
import com.sun.jna.ptr.PointerByReference
import com.sun.jna.win32.StdCallLibrary
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import sp.it.util.dev.fail
import sp.it.util.functional.asIs
import sp.it.util.functional.net

interface Ole32: StdCallLibrary {
   fun CoInitializeEx(pvReserved: Pointer, dwCoInit: Int): WinNT.HRESULT?
   fun CoUninitialize()
   fun CoCreateInstance(rclsid: Guid.GUID, pUnkOuter: Pointer?, dwClsContext: Int, riid: Guid.GUID, ppv: PointerByReference?): WinNT.HRESULT

   companion object {
      val INSTANCE = Native.load(Ole32::class.java)
   }
}

object COM {
   private val lock = ReentrantLock()
   @Volatile var initialized = false
      private set

   init {
      Runtime.getRuntime().addShutdownHook(Thread { dispose() })
   }

   fun init() =
      lock.withLock {
         if (!initialized) COMUtils.checkRC(Ole32.INSTANCE.CoInitializeEx(Pointer.NULL, 0x02))
         initialized = true
      }

   fun dispose() =
      lock.withLock {
         if (initialized) Ole32.INSTANCE.CoUninitialize()
         initialized = false
      }
}

public interface IWindowsTaskbarInternal {
   fun HrInit(): WinNT.HRESULT
   fun AddTab(hWnd: WinDef.HWND, hWnd2: WinDef.HWND): WinNT.HRESULT
   fun SetProgressValue(hWnd: WinDef.HWND, ullCompleted: Int, ullTotal: Int): WinNT.HRESULT
   fun SetProgressState(hWnd: WinDef.HWND, tbpFlags: Int): WinNT.HRESULT
   fun ThumbBarAddButtons(hWnd: WinDef.HWND, cButtons: Int, pButton: Array<THUMBBUTTON>): WinNT.HRESULT
   fun ThumbBarUpdateButtons(hWnd: WinDef.HWND, cButtons: Int, pButton: Array<THUMBBUTTON>): WinNT.HRESULT
   fun SetTabOrder(hWndTab: WinDef.HWND?, hWndInsertBefore: WinDef.HWND): WinNT.HRESULT

   object VTable {
      const val HrInit = 0x03
      const val AddTab = 0x0B
      const val SetProgressValue = 0x09
      const val SetProgressState = 0x0A
      const val ThumbBarAddButtons = 0x0F
      const val ThumbBarUpdateButtons = 0x10
      const val SetTabOrder = 0x0D
   }

   object TBPFLAG {
      const val TBPF_NOPROGRESS = 0x00
      const val TBPF_INDETERMINATE = 0x01
      const val TBPF_NORMAL = 0x02
      const val TBPF_ERROR = 0x04
      const val TBPF_PAUSED = 0x08
   }

   companion object {
      const val MAX_BUTTONS = 7

      fun IWindowsTaskbarInternal.ThumbBarUpdateButtons(hWnd: WinDef.HWND, buttons: Array<THUMBBUTTON>): WinNT.HRESULT =
         buttons.take(MAX_BUTTONS).toTypedArray().net { ThumbBarUpdateButtons(hWnd, it.size, it) }
   }
}

class WindowsTaskbarInternal(private val pComInstance: Pointer): Unknown(pComInstance), IWindowsTaskbarInternal {

   init {
      HrInit()
   }

   override fun HrInit(): WinNT.HRESULT =
      super._invokeNativeObject(IWindowsTaskbarInternal.VTable.HrInit, arrayOf<Any>(pComInstance), WinNT.HRESULT::class.java).checkRC()

   override fun AddTab(hWnd: WinDef.HWND, hWnd2: WinDef.HWND): WinNT.HRESULT =
      super._invokeNativeObject(IWindowsTaskbarInternal.VTable.AddTab, arrayOf(pComInstance, hWnd, hWnd2), WinNT.HRESULT::class.java).checkRC()

   override fun SetProgressValue(hWnd: WinDef.HWND, ullCompleted: Int, ullTotal: Int): WinNT.HRESULT =
      super._invokeNativeObject(IWindowsTaskbarInternal.VTable.SetProgressValue, arrayOf(pComInstance, hWnd, ullCompleted, ullTotal), WinNT.HRESULT::class.java).checkRC()

   override fun SetProgressState(hWnd: WinDef.HWND, tbpFlags: Int): WinNT.HRESULT =
      super._invokeNativeObject(IWindowsTaskbarInternal.VTable.SetProgressState, arrayOf(pComInstance, hWnd, tbpFlags), WinNT.HRESULT::class.java).checkRC()

   override fun ThumbBarAddButtons(hWnd: WinDef.HWND, cButtons: Int, pButton: Array<THUMBBUTTON>): WinNT.HRESULT =
      super._invokeNativeObject(IWindowsTaskbarInternal.VTable.ThumbBarAddButtons, arrayOf(pComInstance, hWnd, cButtons, pButton), WinNT.HRESULT::class.java).checkRC()

   override fun ThumbBarUpdateButtons(hWnd: WinDef.HWND, cButtons: Int, pButton: Array<THUMBBUTTON>): WinNT.HRESULT =
      super._invokeNativeObject(IWindowsTaskbarInternal.VTable.ThumbBarUpdateButtons, arrayOf(pComInstance, hWnd, cButtons, pButton), WinNT.HRESULT::class.java).checkRC()

   override fun SetTabOrder(hWndTab: WinDef.HWND?, hWndInsertBefore: WinDef.HWND): WinNT.HRESULT =
      super._invokeNativeObject(IWindowsTaskbarInternal.VTable.SetTabOrder, arrayOf(pComInstance, hWndTab, hWndInsertBefore), WinNT.HRESULT::class.java).checkRC()

   private fun Any.checkRC() = asIs<WinNT.HRESULT>().apply(COMUtils::checkRC)

   companion object {
      val INSTANCE by lazy {
         COM.init()
         if (!COM.initialized) fail { "COM library not initialized" }
         val CLSID_TaskbarList = Guid.GUID("{56FDF344-FD6D-11d0-958A-006097C9A090}")
         val IID_ITaskbarList3 = Guid.GUID("{EA1AFB91-9E28-4B86-90E9-9E9F8A5EEFAF}")
         val pointerRef = PointerByReference()
         val hr = Ole32.INSTANCE.CoCreateInstance(CLSID_TaskbarList, null, CLSCTX_SERVER, IID_ITaskbarList3, pointerRef)
         COMUtils.checkRC(hr)
         WindowsTaskbarInternal(pointerRef.value)
      }
   }

}

class THUMBBUTTON(): Structure() {
   @JvmField var dwMask = 0
   @JvmField var iId = 0
   @JvmField var iBitmap = 0
   @JvmField var hIcon: WinDef.HICON? = null
   @JvmField var szTip = CharArray(MAX_TOOLTIP_LENGTH)
   @JvmField var dwFlags = 0

   constructor(
      dwMask: Int,
      iId: Int,
      iBitmap: Int,
      hIcon: WinDef.HICON?,
      szTip: CharArray,
      dwFlags: Int
   ): this() {
      this.dwMask = dwMask
      this.iId = iId
      this.iBitmap = iBitmap
      this.hIcon = hIcon
      this.szTip = szTip
      this.dwFlags = dwFlags
   }

   override fun getFieldOrder() = listOf("dwMask", "iId", "iBitmap", "hIcon", "szTip", "dwFlags")

   companion object {
      const val MAX_TOOLTIP_LENGTH = 260

      const val THB_BITMAP = 0x01
      const val THB_ICON = 0x02
      const val THB_TOOLTIP = 0x04
      const val THB_FLAGS = 0x08

      const val THBF_ENABLED = 0x00
      const val THBF_DISABLED = 0x01
      const val THBF_DISMISSONCLICK = 0x02
      const val THBF_NOBACKGROUND = 0x04
      const val THBF_HIDDEN = 0x08
      const val THBF_NONINTERACTIVE = 0x10
   }
}

interface User32Custom: StdCallLibrary {
   fun LoadImageA(hInst: WinDef.HINSTANCE, name: String?, type: Int, cx: Int, cy: Int, fuLoad: Int): WinNT.HANDLE?
   fun DestroyIcon(hIcon: WinDef.HICON): WinDef.BOOL?
   fun GetWindowLongPtrA(hWnd: WinDef.HWND, nIndex: Int): BaseTSD.LONG_PTR?
   fun SetWindowLongPtrA(hWnd: WinDef.HWND, nIndex: Int, dwNewLong: Any): BaseTSD.LONG_PTR
   fun CallWindowProcA(lpPrevWndFunc: BaseTSD.LONG_PTR, hWnd: WinDef.HWND?, uMsg: Int, wParam: WinDef.WPARAM?, lParam: WinDef.LPARAM?): WinDef.LRESULT
   fun LookupIconIdFromDirectoryEx(presbits: Pointer, fIcon: Boolean, cxDesired: Int, cyDesired: Int, uFlags: Int): Int
   fun CreateIconFromResourceEx(presbits: Pointer, dwResSize: WinDef.DWORD?, fIcon: Boolean, dwVer: WinDef.DWORD?, cxDesired: Int, cyDesired: Int, uFlags: Int): WinDef.HICON?
   fun FindWindowA(lpClassName: String, lpWindowName: String?): WinDef.HWND?
   fun GetActiveWindow(): WinDef.HWND?
   interface WndProcCallback: Callback {
      fun callback(hWnd: WinDef.HWND, uMsg: Int, wParam: WinDef.WPARAM, lParam: WinDef.LPARAM): WinDef.LRESULT?
   }

   companion object {
      const val IMAGE_ICON = 0x01
      const val LR_LOADFROMFILE = 0x10
      const val LR_DEFAULTSIZE = 0x40
      const val GWLP_WNDPROC = -4
      const val WM_COMMAND = 0x0111
      val INSTANCE = Native.load("user32", User32Custom::class.java)
   }
}

class WndProcCallbackOverride(private val hWnd: WinDef.HWND, private val block: (Int) -> Unit): Callback {
   private val lpPrevWndFunc: BaseTSD.LONG_PTR

   init {
      lpPrevWndFunc = User32Custom.INSTANCE.SetWindowLongPtrA(hWnd, User32Custom.GWLP_WNDPROC, this)
   }

   fun callback(hWnd: WinDef.HWND, uMsg: Int, wParam: WinDef.WPARAM, lParam: WinDef.LPARAM): WinDef.LRESULT {
      if (uMsg==User32Custom.WM_COMMAND) {
         val id = wParam.toInt() and 0xFFFF
         block(id)
      }
      return User32Custom.INSTANCE.CallWindowProcA(lpPrevWndFunc, hWnd, uMsg, wParam, lParam)
   }
}

object CacheHICON {
   private val icons = ConcurrentHashMap<String, WinDef.HICON>()

   operator fun contains(path: String): Boolean = get(path)!=null

   operator fun get(path: String): WinDef.HICON? = icons[path]

   infix fun create(path: String): WinDef.HICON =
      icons.getOrPut(path) {
         val handle = User32.INSTANCE.LoadImage(
            null, path, User32.IMAGE_ICON,
            User32.INSTANCE.GetSystemMetrics(WinUser.SM_CXICON), User32.INSTANCE.GetSystemMetrics(WinUser.SM_CYICON),
            User32.LR_LOADFROMFILE or User32.LR_DEFAULTSIZE
         )
         if (handle==null || handle===WinBase.INVALID_HANDLE_VALUE) {
            throw AssertionError("Failed to create native icon for path=" + path + ": " + Kernel32.INSTANCE.GetLastError())
         }
         WinDef.HICON(handle)
      }

   fun disposeAll() {
      icons.keys.forEach(::dispose)
      icons.clear()
   }

   fun dispose(path: String): Boolean {
      val icon = get(path)
      if (icon==null) return false
      val result = User32.INSTANCE.DestroyIcon(icon)
      if (result) icons.remove(path)
      return result
   }

}

fun List<THUMBBUTTON>.toContiguousMemoryArray(): Array<THUMBBUTTON> {
   var array = first().toArray(size).asIs<Array<THUMBBUTTON>>()
   for (i in indices) {
      array[i].dwMask = this[i].dwMask
      array[i].iId = this[i].iId
      array[i].iBitmap = this[i].iBitmap
      array[i].hIcon = this[i].hIcon
      array[i].szTip = this[i].szTip
   }
   return array.asIs()
}