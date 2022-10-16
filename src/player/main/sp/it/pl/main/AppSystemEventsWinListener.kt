/* Copyright (c) 2012 Tobias Wolf, All Rights Reserved
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 */
package sp.it.pl.main

import com.sun.jna.platform.win32.DBT
import com.sun.jna.platform.win32.Kernel32
import com.sun.jna.platform.win32.User32
import com.sun.jna.platform.win32.User32.WS_EX_TOPMOST
import com.sun.jna.platform.win32.WinDef
import com.sun.jna.platform.win32.WinUser
import com.sun.jna.platform.win32.WinUser.WindowProc
import com.sun.jna.platform.win32.Wtsapi32
import java.util.UUID
import mu.KLogging
import sp.it.pl.main.AppSystemEvents.Event.FileVolumeAdded
import sp.it.pl.main.AppSystemEvents.Event.FileVolumeRemoved
import sp.it.pl.main.AppSystemEvents.SysListener
import sp.it.util.async.runNew
import sp.it.util.type.volatile

@Suppress("SpellCheckingInspection", "UNUSED_VARIABLE", "UNUSED_PARAMETER")
class AppSystemEventsWinListener(emitter: (AppSystemEvents.Event) -> Unit): SysListener {

   private val user32 = User32.INSTANCE
   private val wtsapi32 = Wtsapi32.INSTANCE
   private var hWnd: WinDef.HWND? by volatile(null)
   private val emitter: (AppSystemEvents.Event) -> Unit = emitter

   init {
      // start blocking event loop on new thread
      runNew {
         logger.info { "Initializing..." }

         // define new window class
         val hInst = Kernel32.INSTANCE.GetModuleHandle("")
         val wClass = WinUser.WNDCLASSEX()
         wClass.hInstance = hInst
         wClass.lpfnWndProc = WindowProc { hwnd, uMsg, wParam, lParam -> callback(hwnd, uMsg, wParam, lParam) }
         wClass.lpszClassName = UUID.randomUUID().toString()

         // register window class
         user32.RegisterClassEx(wClass)
         getLastError("RegisterClassEx $wClass")

         // create new window
         hWnd = user32.CreateWindowEx(
            WS_EX_TOPMOST, wClass.lpszClassName, "SpitPlayer OS device monitor message queue window",
            0, 0, 0, 0, 0,
            null,  // WM_DEVICECHANGE contradicts parent=WinUser.HWND_MESSAGE
            null, hInst, null
         )
         if (hWnd==null) getLastError("CreateWindowEx")

         wtsapi32.WTSRegisterSessionNotification(hWnd, Wtsapi32.NOTIFY_FOR_THIS_SESSION).ifErrorWarn("WTSRegisterSessionNotification")

         /* filter for all device classes */
         // DEV_BROADCAST_HDR notificationFilter = new DEV_BROADCAST_HDR();
         // notificationFilter.dbch_devicetype = DBT.DBT_DEVTYP_DEVICEINTERFACE;

         // filter for all usb device classes/
         val notificationFilter = DBT.DEV_BROADCAST_DEVICEINTERFACE()
         notificationFilter.dbcc_size = notificationFilter.size()
         notificationFilter.dbcc_devicetype = DBT.DBT_DEVTYP_DEVICEINTERFACE
         notificationFilter.dbcc_classguid = DBT.GUID_DEVINTERFACE_USB_DEVICE

         // use User32.DEVICE_NOTIFY_ALL_INTERFACE_CLASSES instead of DEVICE_NOTIFY_WINDOW_HANDLE to ignore the dbcc_classguid value
         val hDevNotify = user32.RegisterDeviceNotification(hWnd, notificationFilter, User32.DEVICE_NOTIFY_WINDOW_HANDLE)
         if (hDevNotify==null) getLastError("RegisterDeviceNotification")
         logger.info { "Initialized" }

         val msg = WinUser.MSG()
         while (user32.GetMessage(msg, hWnd, 0, 0)!=0) {
            user32.TranslateMessage(msg)
            user32.DispatchMessage(msg)
         }

         logger.info { "Disposing..." }
         user32.UnregisterDeviceNotification(hDevNotify).ifErrorWarn("UnregisterDeviceNotification")
         wtsapi32.WTSUnRegisterSessionNotification(hWnd).ifErrorWarn("WTSUnRegisterSessionNotification")
         user32.DestroyWindow(hWnd).ifErrorWarn("DestroyWindow")
         user32.UnregisterClass(wClass.lpszClassName, hInst).ifErrorWarn("UnregisterClass")
         logger.info { "Disposed" }
      }
   }

   override fun dispose() {
      logger.info { "Sending dispose message..." }
      // Sends asynchronously WM_DESTROY message, eventually dispatched on the messaging thread to callback, which stops listening and ends thread
      user32.SendMessage(hWnd, WinUser.WM_DESTROY, WinDef.WPARAM(0), WinDef.LPARAM(0))
   }

   private fun callback(hwnd: WinDef.HWND, uMsg: Int, wParam: WinDef.WPARAM, lParam: WinDef.LPARAM): WinDef.LRESULT {
      return when (uMsg) {
         WinUser.WM_CREATE -> {
            onCreate(wParam, lParam)
            WinDef.LRESULT(0)
         }
         WinUser.WM_DESTROY -> {
            user32.PostQuitMessage(0)
            WinDef.LRESULT(0)
         }
         WinUser.WM_SESSION_CHANGE -> {
            onSessionChange(wParam, lParam)
            WinDef.LRESULT(0)
         }
         WinUser.WM_DEVICECHANGE -> {
            onDeviceChange(wParam, lParam) ?: user32.DefWindowProc(hwnd, uMsg, wParam, lParam)
         }
         else -> user32.DefWindowProc(hwnd, uMsg, wParam, lParam)
      }
   }

   private fun onSessionChange(wParam: WinDef.WPARAM, lParam: WinDef.LPARAM) {
      when (wParam.toInt()) {
         Wtsapi32.WTS_CONSOLE_CONNECT -> Unit
         Wtsapi32.WTS_CONSOLE_DISCONNECT -> Unit
         Wtsapi32.WTS_SESSION_LOGON -> Unit
         Wtsapi32.WTS_SESSION_LOGOFF -> Unit
         Wtsapi32.WTS_SESSION_LOCK -> Unit
         Wtsapi32.WTS_SESSION_UNLOCK -> Unit
      }
   }

   private fun onCreate(wParam: WinDef.WPARAM?, lParam: WinDef.LPARAM?) {
      logger.debug { "onCreate: WM_CREATE" }
   }

   /** @return the result or null if the message is not processed. */
   private fun onDeviceChange(wParam: WinDef.WPARAM, lParam: WinDef.LPARAM): WinDef.LRESULT? {
      return when (wParam.toInt()) {
         DBT.DBT_DEVICEARRIVAL -> onDeviceChangeArrival(lParam)
         DBT.DBT_DEVICEREMOVECOMPLETE -> onDeviceChangeRemoveComplete(lParam)
         DBT.DBT_DEVNODES_CHANGED -> onDeviceChangeNodesChanged()
         else -> { logger.debug { "Message WM_DEVICECHANGE message received, value unhandled" }; null }
      }
   }

   private fun onDeviceChangeArrival(lParam: WinDef.LPARAM): WinDef.LRESULT? = onDeviceChangeArrivalOrRemoveComplete(lParam, "ADD")

   private fun onDeviceChangeRemoveComplete(lParam: WinDef.LPARAM): WinDef.LRESULT? = onDeviceChangeArrivalOrRemoveComplete(lParam, "REM")

   private fun onDeviceChangeNodesChanged(): WinDef.LRESULT {
      logger.debug { "Message DBT_DEVNODES_CHANGED" }

      // return TRUE means processed message for this wParam.
      // see http://msdn.microsoft.com/en-us/library/windows/desktop/aa363211.aspx
      return WinDef.LRESULT(1)
   }

   private fun onDeviceChangeArrivalOrRemoveComplete(lParam: WinDef.LPARAM, action: String): WinDef.LRESULT? {
      val bhdr = DBT.DEV_BROADCAST_HDR(lParam.toLong())
      when (bhdr.dbch_devicetype) {
         DBT.DBT_DEVTYP_DEVICEINTERFACE -> {
            // see http://msdn.microsoft.com/en-us/library/windows/desktop/aa363244.aspx
            val bdif = DBT.DEV_BROADCAST_DEVICEINTERFACE(bhdr.pointer)
            logger.debug { "$action BROADCAST_DEVICEINTERFACE:\ndbcc_devicetype: ${bdif.dbcc_devicetype}\ndbcc_name: ${bdif.getDbcc_name()}\ndbcc_classguid: ${bdif.dbcc_classguid.toGuidString()}" }
         }
         DBT.DBT_DEVTYP_HANDLE -> {
            // see http://msdn.microsoft.com/en-us/library/windows/desktop/aa363245.aspx
            val bhd = DBT.DEV_BROADCAST_HANDLE(bhdr.pointer)
            logger.debug { "$action BROADCAST_HANDLE" }
         }
         DBT.DBT_DEVTYP_OEM -> {
            // see http://msdn.microsoft.com/en-us/library/windows/desktop/aa363247.aspx
            val boem = DBT.DEV_BROADCAST_OEM(bhdr.pointer)
            logger.debug { "$action DBT_DEVTYP_OEM" }
         }
         DBT.DBT_DEVTYP_PORT -> {
            // see http://msdn.microsoft.com/en-us/library/windows/desktop/aa363248.aspx
            val bpt = DBT.DEV_BROADCAST_PORT(bhdr.pointer)
            logger.debug { "$action DBT_DEVTYP_PORT" }
         }
         DBT.DBT_DEVTYP_VOLUME -> {
            // see http://msdn.microsoft.com/en-us/library/windows/desktop/aa363249.aspx
            val bvl = DBT.DEV_BROADCAST_VOLUME(bhdr.pointer)
            var logicalDriveAffected = bvl.dbcv_unitmask
            val flag = bvl.dbcv_flags
            val isMediaNotPhysical = 0!=flag.toInt() and DBT.DBTF_MEDIA /*value is 1*/
            val isNet = 0!=flag.toInt() and DBT.DBTF_NET /*value is 2*/
            var driveLetterIndex = 0
            val driveLetters = mutableSetOf<Char>()
            while (logicalDriveAffected!=0) {
               if (0!=logicalDriveAffected and 1) {
                  val letter = ('A'.code + driveLetterIndex).toChar()
                  driveLetters.add(letter)
                  if (action=="ADD") emitter(FileVolumeAdded(letter))
                  if (action=="REM") emitter(FileVolumeRemoved(letter))
               }
               logicalDriveAffected = logicalDriveAffected ushr 1
               driveLetterIndex++
            }
            logger.debug { "$action DBT_DEVTYP_VOLUME\nlogical Drive Letters: $driveLetters\nisMediaNotPhysical:$isMediaNotPhysical\nisNet:$isNet" }
         }
         else -> return null
      }

      // return TRUE means processed message for this wParam.
      // see http://msdn.microsoft.com/en-us/library/windows/desktop/aa363205.aspx
      // see http://msdn.microsoft.com/en-us/library/windows/desktop/aa363208.aspx
      return WinDef.LRESULT(1)
   }

   companion object: KLogging() {

      private fun getLastError(message: String): Int {
         val rc = Kernel32.INSTANCE.GetLastError()
         if (rc!=0) logger.warn { "Failed to $message error: $rc" }
         return rc
      }

      private fun Boolean.ifErrorWarn(message: String) {
         if (!this) getLastError(message)
      }

   }
}