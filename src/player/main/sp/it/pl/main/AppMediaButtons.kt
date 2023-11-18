package sp.it.pl.main

import com.sun.jna.Pointer
import com.sun.jna.platform.win32.Kernel32
import com.sun.jna.platform.win32.User32
import com.sun.jna.platform.win32.Win32VK
import com.sun.jna.platform.win32.WinDef.LPARAM
import com.sun.jna.platform.win32.WinDef.LRESULT
import com.sun.jna.platform.win32.WinDef.WPARAM
import com.sun.jna.platform.win32.WinUser.HHOOK
import com.sun.jna.platform.win32.WinUser.KBDLLHOOKSTRUCT
import com.sun.jna.platform.win32.WinUser.LowLevelKeyboardProc
import com.sun.jna.platform.win32.WinUser.MSG
import com.sun.jna.platform.win32.WinUser.WH_KEYBOARD_LL
import javafx.scene.input.KeyCode
import mu.KLogging
import sp.it.util.async.NEW
import sp.it.util.async.runFX
import sp.it.util.bool.Bool2
import sp.it.util.system.Os
import sp.it.util.type.volatile

/** Low level keyboard hook. Supports [Os.WINDOWS] only. */
class AppMediaButtons(handlers: (KeyCode?) -> (() -> Boolean)? = APP.actions.registrar.hotkeys::registered) {

   /** [User32.INSTANCE] */
   private val user32 = User32.INSTANCE
   /** Low level keyboard hook */
   private var hhook: HHOOK? by volatile(null)
   /** [hhook] callback. Rquires strong reference else after gc JVM may crashes */
   private val hProc = object: LowLevelKeyboardProc {
      override fun callback(nCode: Int, wParam: WPARAM, lParam: KBDLLHOOKSTRUCT): LRESULT {
         Win32VK.VK_MEDIA_PLAY_PAUSE
         val key = lParam.vkCode.toFx();
         val handler = handlers(key)
         val consume = handler?.invoke() ?: false
         return if (consume) LRESULT(1)
         else user32.CallNextHookEx(hhook, nCode, wParam, LPARAM(Pointer.nativeValue(lParam.pointer)))
      }
   }

   fun init() {
      if (Os.WINDOWS.isCurrent)
         NEW("AppMediaButtons") {
            val hMod = Kernel32.INSTANCE.GetModuleHandle(null)
            if (hMod==null) getLastError("GetModuleHandle")
            val hhk = user32.SetWindowsHookEx(WH_KEYBOARD_LL, hProc, hMod, 0)
            if (hhk==null) getLastError("SetWindowsHookEx")
            hhook = hhk
            var result: Int
            while (true) {
               val msg = MSG()
               result = user32.GetMessage(msg, null, 0, 0)
               if (result==-1) {
                  getLastError("GetMessage")
                  break
               } else if (result==0) {
                  break
               } else {
                  user32.TranslateMessage(msg)
                  user32.DispatchMessage(msg)
               }
            }
            hhook?.unhook()
         }
   }

   fun dispose() =
      hhook?.unhook()

   private fun HHOOK.unhook() {
      hhook = null
      val r = user32.UnhookWindowsHookEx(this)
      if (r==false) getLastError("UnhookWindowsHookEx")
   }

   private companion object: KLogging() {
      const val WM_APPCOMMAND = 0x0319
      const val APPCOMMAND_MEDIA_PLAY_PAUSE = 14

      const val WM_KEYDOWN = 256
      const val WM_KEYUP = 257
      const val WM_SYSKEYDOWN = 260
      const val WM_SYSKEYUP = 261

      fun getLastError(message: String): Int {
         val rc = Kernel32.INSTANCE.GetLastError()
         if (rc!=0) AppSystemEventsWinListener.logger.warn { "Failed to $message error: $rc" }
         return rc
      }

      fun Int.toFx() = when(this) {
         Win32VK.VK_MEDIA_PLAY_PAUSE.code -> KeyCode.PLAY
         Win32VK.VK_MEDIA_STOP.code -> KeyCode.STOP
         Win32VK.VK_MEDIA_NEXT_TRACK.code -> KeyCode.TRACK_NEXT
         Win32VK.VK_MEDIA_PREV_TRACK.code -> KeyCode.TRACK_PREV
         Win32VK.VK_VOLUME_UP.code -> KeyCode.VOLUME_UP
         Win32VK.VK_VOLUME_DOWN.code -> KeyCode.VOLUME_DOWN
         Win32VK.VK_VOLUME_MUTE.code -> KeyCode.MUTE
         else -> null
      }

      fun systemState(code: WPARAM) = when (code.toInt()) {
         WM_SYSKEYDOWN, WM_SYSKEYUP -> SystemState.SYSTEM
         else -> SystemState.STANDARD
      }

      fun pressState(code: WPARAM) = when(code.toInt()) {
         WM_SYSKEYUP, WM_KEYUP -> PressState.UP
         WM_SYSKEYDOWN, WM_KEYDOWN -> PressState.DOWN
         else -> PressState.UNKNOWN
      }

      enum class SystemState { SYSTEM, STANDARD }

      enum class PressState { UP, DOWN, UNKNOWN }
   }

}